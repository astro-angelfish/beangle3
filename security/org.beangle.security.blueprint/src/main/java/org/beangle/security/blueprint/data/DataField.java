/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.security.blueprint.data;

import org.beangle.commons.entity.pojo.LongIdEntity;

/**
 * @author chaostone
 * @since 3.0.0
 */
public interface DataField extends LongIdEntity{

  String getName();

  String getTitle();

  DataType getType();

  DataResource getResource();

}
