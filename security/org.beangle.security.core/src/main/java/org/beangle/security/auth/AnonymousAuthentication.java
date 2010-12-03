/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.security.auth;

import org.beangle.security.core.GrantedAuthority;

public class AnonymousAuthentication extends AbstractAuthentication {

	private Object principal;

	public AnonymousAuthentication(Object principal, GrantedAuthority[] authorities) {
		super(authorities);
		this.principal = principal;
		setAuthenticated(true);
	}

	public Object getCredentials() {
		return "";
	}

	public Object getPrincipal() {
		return principal;
	}

}