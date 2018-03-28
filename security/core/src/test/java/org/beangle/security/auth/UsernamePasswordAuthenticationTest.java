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
package org.beangle.security.auth;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;

import org.beangle.security.authc.UsernamePasswordAuthentication;
import org.testng.annotations.Test;

@Test
public class UsernamePasswordAuthenticationTest {
  public void testAuthenticated() {
    UsernamePasswordAuthentication token = new UsernamePasswordAuthentication("Test", "Password", null);

    // check default given we passed some GrantedAuthorty[]s (well, we
    // passed null)
    assertTrue(token.isAuthenticated());

    // check explicit set to untrusted (we can safely go from trusted to
    // untrusted, but not the reverse)
    token.setAuthenticated(false);
    assertTrue(!token.isAuthenticated());

    // Now let's create a UsernamePasswordAuthentication without any
    // GrantedAuthorty[]s (different constructor)
    token = new UsernamePasswordAuthentication("Test", "Password");

    assertTrue(!token.isAuthenticated());

    // check we're allowed to still set it to untrusted
    token.setAuthenticated(false);
    assertTrue(!token.isAuthenticated());

    // check denied changing it to trusted
    try {
      token.setAuthenticated(true);
      fail("Should have prohibited setAuthenticated(true)");
    } catch (IllegalArgumentException expected) {
      assertTrue(true);
    }
  }

  public void testGetters() {
    UsernamePasswordAuthentication token = new UsernamePasswordAuthentication("Test", "Password",
        Arrays.asList("ROLE_ONE", "ROLE_TWO"));
    assertEquals("Test", token.getPrincipal());
    assertEquals("Password", token.getCredentials());
    // ensure authority order
    Iterator<?> iter = token.getAuthorities().iterator();
    for (int i = 0; i < 2; i++) {
      if (i == 0) {
        assertEquals(iter.next(), "ROLE_ONE");
      } else {
        assertEquals(iter.next(), "ROLE_TWO");
      }
    }
  }

  public void testNoArgConstructorDoesntExist() {
    Class<?> clazz = UsernamePasswordAuthentication.class;
    try {
      clazz.getDeclaredConstructor((Class[]) null);
      fail("Should have thrown NoSuchMethodException");
    } catch (NoSuchMethodException expected) {
      assertTrue(true);
    }
  }
}
