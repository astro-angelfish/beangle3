/*
 * Beangle, Agile Java/Scala Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2012, Beangle Software.
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
package org.beangle.security.blueprint.data.service.internal;

import java.util.Collection;
import java.util.List;

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.dao.EntityDao;
import org.beangle.commons.dao.query.builder.OqlBuilder;
import org.beangle.commons.entity.metadata.EntityType;
import org.beangle.commons.entity.metadata.Model;
import org.beangle.commons.lang.Strings;
import org.beangle.commons.lang.reflect.Reflections;
import org.beangle.security.blueprint.data.ProfileField;
import org.beangle.security.blueprint.data.service.UserDataResolver;

public class IdentifierDataResolver implements UserDataResolver {

  protected EntityDao entityDao;

  public String marshal(ProfileField field, Collection<?> items) {
    StringBuilder sb = new StringBuilder();
    for (Object obj : items) {
      try {
        Object value = obj;
        if (null != field.getType().getKeyName()) {
          value = PropertyUtils.getProperty(obj, field.getType().getKeyName());
        }
        sb.append(String.valueOf(value)).append(',');
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  public <T> List<T> unmarshal(ProfileField field, String text) {
    if (null == field.getType().getTypeName()) {
      return (List<T>) CollectUtils.newArrayList(Strings.split(text, ","));
    } else {
      Class<?> clazz = null;
      try {
        clazz = Class.forName(field.getType().getTypeName());
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
        throw new RuntimeException(e);
      }
      EntityType myType = Model.getType(clazz);
      OqlBuilder<T> builder = OqlBuilder.from(myType.getEntityName(), "field");

      String[] ids = Strings.split(text, ",");
       Class<?> propertyType = Reflections.getPropertyType(clazz, field.getType().getKeyName());
      List<Object> realIds = CollectUtils.newArrayList(ids.length);
      for (String id : ids) {
        Object realId = ConvertUtils.convert(id, propertyType);
        realIds.add(realId);
      }
      builder.where("field." + field.getType().getKeyName() + " in (:ids)", realIds).cacheable();
      return entityDao.search(builder);
    }
  }

  public void setEntityDao(EntityDao entityDao) {
    this.entityDao = entityDao;
  }

}
