/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.ems.security.service;

import org.beangle.ems.event.BusinessEvent;

/**
 * @author chaostone
 * @version $Id: AccountStatusEvent.java Jun 22, 2011 8:58:14 AM chaostone $
 */
public class AccountStatusEvent extends BusinessEvent {

	private static final long serialVersionUID = -8120260840834127793L;
	private boolean enabled;

	public AccountStatusEvent(Object source) {
		super(source);
	}

	public AccountStatusEvent(Object source, boolean enabled) {
		super(source);
		this.enabled = enabled;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public boolean isEnabled() {
		return enabled;
	}

}