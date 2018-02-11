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
package org.beangle.commons.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;

import org.beangle.commons.lang.Assert;

/**
 * File Operation Utility
 * 
 * @author chaostone
 * @since 3.1
 */
public class Files {

  public static final long CopyBufferSize = 1024 * 1024 * 30;// 30MB

  /**
   * Reads the contents of a file into a String using the default encoding for the VM.
   * The file is always closed.
   */
  public static String readFileToString(File file) throws IOException {
    return readFileToString(file, null);
  }

  /**
   * Reads the contents of a file into a String.
   * The file is always closed.
   */
  public static String readFileToString(File file, Charset charset) throws IOException {
    InputStream in = null;
    try {
      in = new FileInputStream(file);
      StringBuilderWriter sw = new StringBuilderWriter();

      if (null == charset) IOs.copy(new InputStreamReader(in), sw);
      else IOs.copy(new InputStreamReader(in, charset.name()), sw);

      return sw.toString();
    } finally {
      IOs.close(in);
    }
  }

  /**
   * Reads the contents of a file line by line to a List of Strings.
   * The file is always closed.
   */
  public static List<String> readLines(File file, Charset charset) throws IOException {
    InputStream in = null;
    try {
      in = new FileInputStream(file);
      if (null == charset) {
        return IOs.readLines(new InputStreamReader(in));
      } else {
        InputStreamReader reader = new InputStreamReader(in, charset.name());
        return IOs.readLines(reader);
      }
    } finally {
      IOs.close(in);
    }
  }

  public static List<String> readLines(File file) throws IOException {
    return readLines(file, null);
  }

  /**
   * Copies a file to a new location preserving the file date.
   * <p>
   * This method copies the contents of the specified source file to the specified destination file.
   * The directory holding the destination file is created if it does not exist. If the destination
   * file exists, then this method will overwrite it.
   * <p>
   * <strong>Note:</strong> This method tries to preserve the file's last modified date/times using
   * {@link File#setLastModified(long)}, however it is not guaranteed that the operation will
   * succeed. If the modification operation fails, no indication is provided.
   * 
   * @param srcFile an existing file to copy, must not be <code>null</code>
   * @param destFile the new file, must not be <code>null</code>
   * @throws NullPointerException if source or destination is <code>null</code>
   * @throws IOException if source or destination is invalid
   * @throws IOException if an IO error occurs during copying
   * @see #copyFileToDirectory(File, File)
   */
  public static void copyFile(File srcFile, File destFile) throws IOException {
    Assert.isTrue(null != srcFile, "Source must not be null");
    Assert.isTrue(null != destFile, "Destination must not be null");
    if (srcFile.exists() == false) { throw new FileNotFoundException("Source '" + srcFile
        + "' does not exist"); }
    if (srcFile.isDirectory()) { throw new IOException("Source '" + srcFile + "' exists but is a directory"); }
    if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) { throw new IOException("Source '"
        + srcFile + "' and destination '" + destFile + "' are the same"); }
    File parentFile = destFile.getParentFile();
    if (parentFile != null) {
      if (!parentFile.mkdirs() && !parentFile.isDirectory()) { throw new IOException("Destination '"
          + parentFile + "' directory cannot be created"); }
    }
    if (destFile.exists()) {
      if (destFile.isDirectory()) { throw new IOException("Destination '" + destFile
          + "' exists but is a directory"); }
      if (!destFile.canWrite()) throw new IOException("Destination '" + destFile
          + "' exists but is read-only");
    }
    doCopyFile(srcFile, destFile, true);
  }

  private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {
    FileInputStream fis = null;
    FileOutputStream fos = null;
    FileChannel input = null;
    FileChannel output = null;
    try {
      fis = new FileInputStream(srcFile);
      fos = new FileOutputStream(destFile);
      input = fis.getChannel();
      output = fos.getChannel();
      long size = input.size();
      long pos = 0;
      long count = 0;
      while (pos < size) {
        count = size - pos > CopyBufferSize ? CopyBufferSize : size - pos;
        pos += output.transferFrom(input, pos, count);
      }
    } finally {
      IOs.close(output);
      IOs.close(fos);
      IOs.close(input);
      IOs.close(fis);
    }

    if (srcFile.length() != destFile.length()) { throw new IOException("Failed to copy full contents from '"
        + srcFile + "' to '" + destFile + "'"); }
    if (preserveFileDate) {
      destFile.setLastModified(srcFile.lastModified());
    }
  }

  public static void deleteDirectory(File directory) throws IOException {
    if (!directory.exists()) return;
    if (!isSymlink(directory)) cleanDirectory(directory);
    if (!directory.delete()) throw new IOException("Unable to delete directory " + directory + ".");
  }

  public static void cleanDirectory(File directory) throws IOException {
    if (!directory.exists()) throw new IllegalArgumentException(directory + " does not exist");
    if (!directory.isDirectory()) throw new IllegalArgumentException(directory + " is not a directory");

    File[] files = directory.listFiles();
    if (files == null) { // null if security restricted
      throw new IOException("Failed to list contents of " + directory);
    }
    IOException exception = null;
    for (File file : files) {
      try {
        forceDelete(file);
      } catch (IOException ioe) {
        exception = ioe;
      }
    }
    if (null != exception) throw exception;
  }

  public static void forceDelete(File file) throws IOException {
    if (file.isDirectory()) {
      deleteDirectory(file);
    } else {
      if (file.exists()) {
        if (!file.delete()) throw new IOException("Unable to delete file: " + file);
      }
    }
  }

  public static boolean isSymlink(File file) throws IOException {
    if (file == null) throw new NullPointerException("File must not be null");
    // windows
    if (File.separatorChar == '\\') { return false; }
    File fileInCanonicalDir = null;
    if (file.getParent() == null) {
      fileInCanonicalDir = file;
    } else {
      File canonicalDir = file.getParentFile().getCanonicalFile();
      fileInCanonicalDir = new File(canonicalDir, file.getName());
    }

    if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
      return false;
    } else {
      return true;
    }
  }

  public static void writeStringToFile(File file, String data, Charset charset) throws IOException {
    writeStringToFile(file, data, charset, false);
  }

  public static void writeStringToFile(File file, String data, Charset charset, boolean append)
      throws IOException {
    OutputStream out = null;
    try {
      out = new FileOutputStream(file, append);
      IOs.write(data, out, charset);
      out.close();
    } finally {
      IOs.close(out);
    }
  }

}
