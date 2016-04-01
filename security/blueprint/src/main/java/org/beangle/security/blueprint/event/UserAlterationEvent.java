/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2016, Beangle Software.
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
package org.beangle.security.blueprint.event;

import java.util.List;

import org.beangle.security.Securities;
import org.beangle.security.blueprint.User;

/**
 * @author chaostone
 * @version $Id: UserAlterationEvent.java Jul 27, 2011 10:18:55 AM chaostone $
 */
public class UserAlterationEvent extends UserEvent {

  private static final long serialVersionUID = 8988908030203145117L;

  public UserAlterationEvent(List<User> users) {
    super(users);
    setSubject(Securities.getUsername() + " 修改了" + getUserNames() + "的用户信息");
  }

}
