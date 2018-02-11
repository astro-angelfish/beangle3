/*
 * Beangle, Agile Development Scaffold and Toolkits.
 *
 * Copyright © 2005, The Beangle Software.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.commons.csv;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.List;

import org.beangle.commons.lang.Throwables;

/**
 * <p>
 * CsvWriter class.
 * </p>
 * 
 * @author chaostone
 * @version $Id: $
 */
public class CsvWriter implements Closeable {

  /** Constant <code>INITIAL_STRING_SIZE=128</code> */
  public static final int INITIAL_STRING_SIZE = 128;

  private Writer rawWriter;

  private PrintWriter pw;

  private CsvFormat format;

  private String lineEnd = "\n";

  /** The quote constant to use when you wish to suppress all quoting. */
  public static final char NO_QUOTE_CHARACTER = '\u0000';

  /** The escape constant to use when you wish to suppress all escaping. */
  public static final char NO_ESCAPE_CHARACTER = '\u0000';

  /** Default line terminator uses platform encoding. */
  public static final String DEFAULT_LINE_END = "\n";

  /**
   * <p>
   * Constructor for CsvWriter.
   * </p>
   * 
   * @param writer a {@link java.io.Writer} object.
   */
  public CsvWriter(Writer writer) {
    this.rawWriter = writer;
    this.pw = new PrintWriter(writer);
    this.format = new CsvFormat.Builder().escape(NO_ESCAPE_CHARACTER).build();
  }

  /**
   * <p>
   * Constructor for CsvWriter.
   * </p>
   * 
   * @param writer a {@link java.io.Writer} object.
   * @param format a {@link org.beangle.commons.csv.CsvFormat} object.
   */
  public CsvWriter(Writer writer, CsvFormat format) {
    this.rawWriter = writer;
    this.pw = new PrintWriter(writer);
    this.format = format;
  }

  /**
   * <p>
   * write.
   * </p>
   * 
   * @param allLines a {@link java.util.List} object.
   */
  public void write(List<String[]> allLines) {
    for (String[] line : allLines) {
      write(line);
    }
  }

  /**
   * <p>
   * write.
   * </p>
   * 
   * @param nextLine an array of {@link java.lang.String} objects.
   */
  public void write(String[] nextLine) {
    if (nextLine == null) return;
    StringBuilder sb = new StringBuilder(INITIAL_STRING_SIZE);
    for (int i = 0; i < nextLine.length; i++) {
      if (i != 0) {
        sb.append(format.defaultSeparator());
      }

      String nextElement = nextLine[i];
      if (nextElement == null) continue;
      if (!format.isDelimiter(NO_QUOTE_CHARACTER)) sb.append(format.getDelimiter());

      sb.append(stringContainsSpecialCharacters(nextElement) ? processLine(nextElement) : nextElement);

      if (!format.isDelimiter(NO_QUOTE_CHARACTER)) sb.append(format.getDelimiter());
    }
    sb.append(lineEnd);
    pw.write(sb.toString());

  }

  private boolean stringContainsSpecialCharacters(String line) {
    return line.indexOf(format.getDelimiter()) != -1 || line.indexOf(format.getDelimiter()) != -1;
  }

  /**
   * <p>
   * processLine.
   * </p>
   * 
   * @param nextElement a {@link java.lang.String} object.
   * @return a {@link java.lang.StringBuilder} object.
   */
  protected StringBuilder processLine(String nextElement) {
    StringBuilder sb = new StringBuilder(INITIAL_STRING_SIZE);
    for (int j = 0; j < nextElement.length(); j++) {
      char nextChar = nextElement.charAt(j);
      if (format.getEscape() != NO_ESCAPE_CHARACTER && nextChar == format.getDelimiter()) {
        sb.append(format.getEscape()).append(nextChar);
      } else if (format.getEscape() != NO_ESCAPE_CHARACTER && nextChar == format.getEscape()) {
        sb.append(format.getEscape()).append(nextChar);
      } else {
        sb.append(nextChar);
      }
    }
    return sb;
  }

  /**
   * <p>
   * flush.
   * </p>
   * 
   * @throws java.io.IOException if any.
   */
  public void flush() throws IOException {
    pw.flush();

  }

  /**
   * <p>
   * close.
   * </p>
   * 
   * @throws java.io.IOException if any.
   */
  public void close() {
    try {
      flush();
      pw.close();
      rawWriter.close();
    } catch (IOException e) {
      Throwables.propagate(e);
    }
  }

  /**
   * <p>
   * checkError.
   * </p>
   * 
   * @return a boolean.
   */
  public boolean checkError() {
    return pw.checkError();
  }

}
