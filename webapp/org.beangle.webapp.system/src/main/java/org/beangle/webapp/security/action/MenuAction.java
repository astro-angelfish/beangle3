/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.webapp.security.action;

import java.util.List;
import java.util.Set;

import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.lang.SeqStrUtils;
import org.beangle.model.Entity;
import org.beangle.model.query.builder.OqlBuilder;
import org.beangle.security.blueprint.Authority;
import org.beangle.security.blueprint.Menu;
import org.beangle.security.blueprint.MenuProfile;
import org.beangle.security.blueprint.Resource;

/**
 * 系统模块(菜单)管理响应类
 * 
 * @author 鄂州蚊子 2008-8-4
 */
public class MenuAction extends SecurityActionSupport {

	protected void indexSetting() {
		put("profiles", entityDao.getAll(MenuProfile.class));
	}

	protected void editSetting(Entity<?> entity) {
		put("profiles", entityDao.getAll(MenuProfile.class));
		Menu menu = (Menu) entity;
		OqlBuilder<Resource> builder = OqlBuilder.from(Resource.class, "r");
		if (null != menu.getProfile() && null != menu.getProfile().getId()) {
			MenuProfile profile = entityDao.get(MenuProfile.class, menu.getProfile().getId());
			builder.where("exists(from r.categories as rc where rc=:category)",
					profile.getCategory());
		}
		List<Resource> resurces = entityDao.search(builder);
		Set<Resource> existResources = menu.getResources();
		if (null != resurces) {
			resurces.removeAll(existResources);
		}
		put("resources", resurces);
	}

	protected String saveAndForward(Entity<?> entity) {
		Menu menu = (Menu) entity;
		try {
			List<Resource> resources = CollectUtils.newArrayList();
			String resourceIdSeq = get("resourceIds");
			if (null != resourceIdSeq && resourceIdSeq.length() > 0) {
				resources = entityDao.get(Resource.class,
						SeqStrUtils.transformToLong(resourceIdSeq));
			}
			menu.getResources().clear();
			menu.getResources().addAll(resources);
			entityDao.saveOrUpdate(menu);
		} catch (Exception e) {
			return forward(ERROR);
		}
		return redirect("search", "info.save.success");
	}

	/**
	 * 禁用或激活一个或多个模块
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 */
	public String activate() {
		String menuIdSeq = get("menuIds");
		Long[] menuIds = SeqStrUtils.transformToLong(menuIdSeq);
		Boolean enabled = getBoolean("isActivate");
		if (null == enabled) {
			enabled = Boolean.TRUE;
		}
		List<Menu> menus = entityDao.get(Menu.class, menuIds);
		for (Menu menu : menus) {
			menu.setEnabled(enabled);
		}
		entityDao.saveOrUpdate(menus);
		return redirect("search", "info.save.success");
	}

	/**
	 * 打印预览功能列表
	 * 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
	public String preview() {
		OqlBuilder<Menu> query = OqlBuilder.from(Menu.class, "menu");
		populateConditions(query);
		query.orderBy("menu.code asc");
		put("menus", entityDao.search(query));

		query.cleanOrders();
		query.select("max(length(menu.code)/2)");
		List<?> rs = entityDao.search(query);
		put("depth", rs.get(0));
		return forward();
	}

	@Override
	public String info() throws Exception {
		Long entityId = getEntityId(getShortName());
		if (null == entityId) {
			logger.warn("cannot get paremeter {}Id or {}.id", getShortName(), getShortName());
		}
		Menu menu = (Menu) getModel(getEntityName(), entityId);
		put(getShortName(), menu);
		if (!menu.getResources().isEmpty()) {
			OqlBuilder<Authority> groupQuery = OqlBuilder.from(Authority.class, "auth");
			groupQuery.where("auth.resource in(:resources)", menu.getResources()).select(
					"distinct auth.group");
			put("groups", entityDao.search(groupQuery));
		}
		return forward();
	}

}