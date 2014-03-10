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
package org.beangle.commons.dao.impl;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.entity.HierarchyEntity;
import org.beangle.commons.entity.pojo.NumberIdHierarchyObject;
import org.beangle.commons.lang.Numbers;
import org.beangle.commons.lang.Objects;
import org.beangle.commons.lang.Strings;

/**
 * @author chaostone
 * @version $Id: AbstractHierarchyService.java Jul 29, 2011 1:34:01 AM chaostone $
 */
@SuppressWarnings({ "unchecked" })
public abstract class AbstractHierarchyService<T extends NumberIdHierarchyObject<? super T, ?>> extends
    BaseServiceImpl {

  public void move(T node, T location, int index) {
    if (Objects.equals(node.getParent(), location)) {
      if (Numbers.toInt(((T) node).getIndexno()) != index) {
        shiftCode(node, location, index);
      }
    } else {
      if (null != node.getParent()) {
        ((HierarchyEntity<? super T, ?>) node.getParent()).getChildren().remove(node);
      }
      node.setParent(location);
      shiftCode(node, location, index);
    }
  }

  private void shiftCode(T node, T newParent, int index) {
    @SuppressWarnings("rawtypes")
    List sibling = null;
    if (null != newParent) sibling = newParent.getChildren();
    else {
      sibling = CollectUtils.newArrayList();
      for (T m : getTopNodes(node)) {
        if (null == m.getParent()) sibling.add(m);
      }
    }
    Collections.sort(sibling);
    sibling.remove(node);
    index--;
    if (index > sibling.size()) {
      index = sibling.size();
    }
    sibling.add(index, node);
    int nolength = String.valueOf(sibling.size()).length();
    Set<T> nodes = CollectUtils.newHashSet();
    for (int seqno = 1; seqno <= sibling.size(); seqno++) {
      T one = (T) sibling.get(seqno - 1);
      generateCode(one, Strings.leftPad(String.valueOf(seqno), nolength, '0'), nodes);
    }
    entityDao.saveOrUpdate(nodes);
  }

  protected abstract List<T> getTopNodes(T m);

  private void generateCode(T node, String indexno, Set<T> nodes) {
    nodes.add(node);
    if (null != indexno) {
      ((T) node).genIndexno(indexno);
    } else {
      ((T) node).genIndexno();
    }
    if (null != node.getChildren()) {
      for (Object m : node.getChildren()) {
        generateCode((T) m, null, nodes);
      }
    }
  }
}
