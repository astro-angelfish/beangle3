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
package org.beangle.security.blueprint.service.impl;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.dao.Operation;
import org.beangle.commons.dao.impl.BaseServiceImpl;
import org.beangle.commons.dao.query.builder.OqlBuilder;
import org.beangle.security.blueprint.model.FuncPermission;
import org.beangle.security.blueprint.model.FuncResource;
import org.beangle.security.blueprint.model.Role;
import org.beangle.security.blueprint.model.User;
import org.beangle.security.blueprint.service.FuncPermissionService;
import org.beangle.security.blueprint.service.UserService;

/**
 * 授权信息的服务实现类
 *
 * @author dell,chaostone 2005-9-26
 */
public class FuncPermissionServiceImpl extends BaseServiceImpl implements FuncPermissionService {

  protected UserService userService;

  public FuncResource getResource(String name) {
    OqlBuilder<FuncResource> query = OqlBuilder.from(FuncResource.class, "r");
    query.where("r.name=:name", name).cacheable();
    return entityDao.uniqueResult(query);
  }

  public List<FuncPermission> getPermissions(User user) {
    if (null == user) return Collections.emptyList();
    List<FuncPermission> permissions = CollectUtils.newArrayList();
    for (final Role role : user.getRoles())
      permissions.addAll(getPermissions(role));
    return permissions;
  }

  public List<FuncResource> getResources(User user) {
    Set<FuncResource> resources = CollectUtils.newHashSet();
    Map<String, Object> params = CollectUtils.newHashMap();
    String hql = "select distinct fp.resource from " + FuncPermission.class.getName()
        + " fp where fp.role.id = :roleId";
    params.clear();
    for (final Role role : user.getRoles()) {
      params.put("roleId", role.getId());
      List<FuncResource> roleResources = entityDao.search(hql, params);
      resources.addAll(roleResources);
    }
    return CollectUtils.newArrayList(resources);
  }

  /** 找到该组内激活的资源id */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<String> getResourceNamesByRole(Integer roleId) {
    String hql = "select a.resource.name from " + FuncPermission.class.getName()
        + " as a where a.role.id= :roleId and a.resource.enabled = true";
    OqlBuilder query = OqlBuilder.hql(hql).param("roleId", roleId).cacheable();
    return (Set<String>) new HashSet(entityDao.search(query));
  }

  /** 找到该组内激活的资源id */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public Set<String> getResourceNamesByScope(FuncResource.Scope scope) {
    OqlBuilder builder = OqlBuilder.from(FuncResource.class, "r").where("r.scope=:scope", scope)
        .select("r.name").cacheable();
    return (Set<String>) new HashSet(entityDao.search(builder));
  }

  public void authorize(Role role, Set<FuncResource> resources) {
    Set<FuncPermission> removed = CollectUtils.newHashSet();
    List<FuncPermission> permissions = getPermissions(role);
    for (final FuncPermission au : permissions) {
      if (!resources.contains(au.getResource())) removed.add(au);
      else resources.remove(au.getResource());
    }
    permissions.removeAll(removed);
    for (FuncResource resource : resources) {
      FuncPermission authority = new FuncPermission(role, resource, null);
      permissions.add(authority);
    }
    entityDao.execute(Operation.remove(removed).saveOrUpdate(permissions).saveOrUpdate(role));
  }

  public void updateState(Integer[] resourceIds, boolean isEnabled) {
    OqlBuilder<FuncResource> query = OqlBuilder.from(FuncResource.class, "resource");
    query.where("resource.id in (:ids)", resourceIds);
    List<FuncResource> resources = entityDao.search(query);
    for (FuncResource resource : resources) {
      resource.setEnabled(isEnabled);
    }
    entityDao.saveOrUpdate(resources);
  }

  public List<FuncPermission> getPermissions(Role role) {
    return entityDao.search(OqlBuilder.from(FuncPermission.class, "fp").where("fp.role=:role", role));
  }

  /** 查询角色对应的模块 */
  public List<FuncResource> getResources(Role role) {
    String hql = "select distinct m from " + Role.class.getName() + " as r join r.permissions as a"
        + " join a.resource as m where  r.id = :roleId and m.enabled = true";
    OqlBuilder<FuncResource> query = OqlBuilder.hql(hql);
    query.param("roleId", role.getId()).cacheable();
    return entityDao.search(query);
  }

  public String extractResource(String uri) {
    int lastDot = -1;
    for (int i = 0; i < uri.length(); i++) {
      char a = uri.charAt(i);
      if (a == '.' || a == '!') {
        lastDot = i;
        break;
      }
    }
    if (lastDot < 0) {
      lastDot = uri.length();
    }
    return uri.substring(0, lastDot);
  }

  public void setUserService(UserService userService) {
    this.userService = userService;
  }

  public UserService getUserService() {
    return userService;
  }
}
