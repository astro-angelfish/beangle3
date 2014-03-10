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
package org.beangle.security.blueprint.session.service;

import java.util.Map;

import org.beangle.commons.bean.Initializing;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.dao.query.builder.OqlBuilder;
import org.beangle.commons.event.Event;
import org.beangle.commons.event.EventListener;
import org.beangle.commons.lang.Assert;
import org.beangle.commons.lang.Option;
import org.beangle.security.auth.Principals;
import org.beangle.security.blueprint.session.model.SessionStat;
import org.beangle.security.core.Authentication;
import org.beangle.security.core.session.Sessioninfo;
import org.beangle.security.core.session.SessioninfoBuilder;
import org.beangle.security.core.session.category.CategoryPrincipal;
import org.beangle.security.core.session.category.CategoryProfile;
import org.beangle.security.core.session.category.CategoryProfileProvider;
import org.beangle.security.core.session.category.CategoryProfileUpdateEvent;
import org.beangle.security.core.session.category.CategorySessioninfo;
import org.beangle.security.core.session.category.SimpleCategoryProfileProvider;

/**
 * 基于数据库的集中session控制器
 * 
 * @author chaostone
 * @version $Id: DbSessionController.java Jul 8, 2011 9:08:14 AM chaostone $
 */
public class DbSessionController extends AbstractSessionController implements Initializing,
    EventListener<CategoryProfileUpdateEvent> {

  private CategoryProfileProvider categoryProfileProvider = new SimpleCategoryProfileProvider();

  private SessioninfoBuilder sessioninfoBuilder;

  private Map<String, Integer> categoryStatIds = CollectUtils.newConcurrentHashMap();

  @Override
  protected boolean allocate(Authentication auth, String sessionId) {
    CategoryPrincipal principal = (CategoryPrincipal) auth.getPrincipal();
    String category = principal.getCategory();
    // Check corresponding stat existence
    Integer statId = categoryStatIds.get(category);
    if (null == statId) {
      statId = (Integer) entityDao.uniqueResult(OqlBuilder.from(SessionStat.class.getName(), "ss")
          .where("ss.category=:category", category).select("ss.id"));
      if (null == statId) {
        CategoryProfile cp = categoryProfileProvider.getProfile(principal.getCategory());
        if (null != cp) {
          SessionStat stat = new SessionStat(cp.getCategory(), cp.getCapacity());
          entityDao.saveOrUpdate(stat);
          statId = stat.getId();
        }
      }
      if (null != statId) categoryStatIds.put(category, statId);
    }

    if (Principals.ROOT.equals(principal.getId())) {
      if (null != statId)
        entityDao.executeUpdate("update " + SessionStat.class.getName()
            + " stat set stat.online = stat.online + 1 where  stat.id=?1", statId);
      return true;
    } else {
      int result = 0;
      if (null != statId) {
        result = entityDao.executeUpdate(
            "update " + SessionStat.class.getName() + " stat set stat.online = stat.online + 1 "
                + "where stat.online < stat.capacity and stat.id=?1", statId);
      }
      return result > 0;
    }
  }

  public int getMaxSessions(Authentication auth) {
    CategoryPrincipal principal = (CategoryPrincipal) auth.getPrincipal();
    if (Principals.ROOT.equals(principal.getId())) {
      return -1;
    } else {
      CategoryProfile cp = categoryProfileProvider.getProfile(principal.getCategory());
      if (null == cp) return 1;
      else return cp.getUserMaxSessions();
    }
  }

  /**
   * @param auth
   * @return -1 represent not specified
   */
  public Option<Short> getInactiveInterval(Authentication auth) {
    CategoryPrincipal principal = (CategoryPrincipal) auth.getPrincipal();
    CategoryProfile cp = categoryProfileProvider.getProfile(principal.getCategory());
    if (null == cp) return Option.none();
    else return Option.some(cp.getInactiveInterval());
  }

  public void onLogout(Sessioninfo info) {
    CategorySessioninfo categoryinfo = (CategorySessioninfo) info;
    if (!info.isExpired()) {
      entityDao.executeUpdate("update " + SessionStat.class.getName()
          + " stat set stat.online=stat.online -1 " + "where stat.online>0 and stat.category=?1",
          categoryinfo.getCategory());
    }
  }

  public void init() throws Exception {
    Assert.notNull(categoryProfileProvider);
  }

  public void onEvent(CategoryProfileUpdateEvent event) {
    CategoryProfile profile = (CategoryProfile) event.getSource();
    int cnt = entityDao.executeUpdate("update " + SessionStat.class.getName()
        + " stat set stat.capacity=?1 where stat.category=?2", profile.getCapacity(), profile.getCategory());
    if (cnt == 0) entityDao.saveOrUpdate(new SessionStat(profile.getCategory(), profile.getCapacity()));
  }

  public void stat() {
    entityDao.executeUpdate("update " + SessionStat.class.getName()
        + " stat  set stat.online=(select count(*) from " + sessioninfoBuilder.getSessioninfoType().getName()
        + " info where info.expiredAt is null and info.category=stat.category)");
  }

  public CategoryProfileProvider getCategoryProfileProvider() {
    return categoryProfileProvider;
  }

  public void setCategoryProfileProvider(CategoryProfileProvider categoryProfileProvider) {
    this.categoryProfileProvider = categoryProfileProvider;
  }

  public void setSessioninfoBuilder(SessioninfoBuilder sessioninfoBuilder) {
    this.sessioninfoBuilder = sessioninfoBuilder;
  }

  public boolean supportsEventType(Class<? extends Event> eventType) {
    return eventType.equals(CategoryProfileUpdateEvent.class);
  }

  public boolean supportsSourceType(Class<?> sourceType) {
    return true;
  }

}
