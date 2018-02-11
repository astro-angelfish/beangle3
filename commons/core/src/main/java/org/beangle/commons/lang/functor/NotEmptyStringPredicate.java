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
package org.beangle.commons.lang.functor;

import org.beangle.commons.lang.Strings;

/**
 * <p>
 * NotEmptyStringPredicate class.
 * </p>
 *
 * @author chaostone
 * @version $Id: $
 */
public class NotEmptyStringPredicate implements Predicate<String> {
  /** Constant <code>INSTANCE</code> */
  public static final NotEmptyStringPredicate Instance = new NotEmptyStringPredicate();

  public Boolean apply(final String value) {
    return (null != value) && (value instanceof String) && Strings.isNotEmpty((String) value);
  }

}
