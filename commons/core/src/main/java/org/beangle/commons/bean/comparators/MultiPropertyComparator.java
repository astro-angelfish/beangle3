/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2014, Beangle Software.
 *
 * Beangle is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Beangle is distributed in the hope that it will be useful.
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Beangle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.beangle.commons.bean.comparators;

import org.beangle.commons.lang.Strings;

/**
 * 多个属性的比较
 * 
 * @author chaostone
 * @version $Id: $
 */
public class MultiPropertyComparator extends ChainComparator<Object> {

  /**
   * <p>
   * Constructor for MultiPropertyComparator.
   * </p>
   * 
   * @param propertyStr a {@link java.lang.String} object.
   */
  public MultiPropertyComparator(final String propertyStr) {
    super();
    final String[] properties = Strings.split(propertyStr, ',');
    for (int i = 0; i < properties.length; i++) {
      addComparator(new PropertyComparator(properties[i].trim()));
    }
  }

}
