/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.ems.security.restrict;

import java.util.Set;

import org.beangle.dao.pojo.LongIdEntity;

public interface RestrictionHolder extends LongIdEntity {

	public Set<Restriction> getRestrictions();

	public void setRestrictions(Set<Restriction> restrictions);

}
