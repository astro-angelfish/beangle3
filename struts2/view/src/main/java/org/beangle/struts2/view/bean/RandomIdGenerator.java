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
package org.beangle.struts2.view.bean;

import java.util.Random;

import org.beangle.commons.lang.Strings;

/**
 * 随机UI's id产生器
 *
 * @author chaostone
 * @since 2.4
 */
public class RandomIdGenerator implements UIIdGenerator {
  final protected Random seed = new Random();

  public String generate(Class<?> clazz) {
    int nextInt = seed.nextInt();
    nextInt = (nextInt == Integer.MIN_VALUE) ? Integer.MAX_VALUE : Math.abs(nextInt);
    return Strings.uncapitalize(clazz.getSimpleName()) + String.valueOf(nextInt);
  }

}
