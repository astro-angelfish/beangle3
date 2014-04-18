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
package org.beangle.commons.dao.query.builder;

import static org.beangle.commons.lang.Strings.concat;
import static org.beangle.commons.lang.Strings.contains;
import static org.beangle.commons.lang.Strings.isEmpty;
import static org.beangle.commons.lang.Strings.isNotEmpty;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.beangle.commons.collection.Order;
import org.beangle.commons.collection.page.PageLimit;
import org.beangle.commons.dao.query.Lang;
import org.beangle.commons.entity.metadata.EntityType;
import org.beangle.commons.entity.metadata.Model;
import org.beangle.commons.entity.util.EntityUtils;
import org.beangle.commons.lang.Assert;
import org.beangle.commons.lang.Strings;

/**
 * 实体类查询 Object Query Language Builder
 * 
 * @author chaostone
 * @version $Id: $
 */
public class OqlBuilder<T> extends AbstractQueryBuilder<T> {

  /** 查询实体类 */
  protected Class<T> entityClass;

  /**
   * Constructor for OqlBuilder.
   */
  protected OqlBuilder() {
    super();
  }

  /**
   * hql.
   * 
   * @param hql a {@link java.lang.String} object.
   * @param <E> a E object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public static <E> OqlBuilder<E> hql(final String hql) {
    OqlBuilder<E> query = new OqlBuilder<E>();
    query.statement = hql;
    return query;
  }

  /**
   * from.
   * 
   * @param from a {@link java.lang.String} object.
   * @param <E> a E object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public static <E> OqlBuilder<E> from(final String from) {
    OqlBuilder<E> query = new OqlBuilder<E>();
    query.newFrom(from);
    return query;
  }

  /**
   * from.
   * 
   * @param entityName a {@link java.lang.String} object.
   * @param alias a {@link java.lang.String} object.
   * @param <E> a E object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  @SuppressWarnings("unchecked")
  public static <E> OqlBuilder<E> from(final String entityName, final String alias) {
    EntityType type = Model.getType(entityName);
    OqlBuilder<E> query = new OqlBuilder<E>();
    if (null != type) query.entityClass = (Class<E>) type.getEntityClass();
    query.alias = alias;
    query.select = "select " + alias;
    query.from = concat("from ", entityName, " ", alias);
    return query;
  }

  /**
   * from.
   * 
   * @param entityClass a {@link java.lang.Class} object.
   * @param <E> a E object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public static <E> OqlBuilder<E> from(final Class<E> entityClass) {
    EntityType type = Model.getType(entityClass.getName());
    if (null == type) type = Model.getType(entityClass);
    return from(entityClass, EntityUtils.getCommandName(type.getEntityName()));
  }

  /**
   * from.
   * 
   * @param entityClass a {@link java.lang.Class} object.
   * @param alias a {@link java.lang.String} object.
   * @param <E> a E object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  @SuppressWarnings("unchecked")
  public static <E> OqlBuilder<E> from(final Class<E> entityClass, final String alias) {
    EntityType type = Model.getType(entityClass.getName());
    if (null == type) type = Model.getType(entityClass);
    OqlBuilder<E> query = new OqlBuilder<E>();
    query.entityClass = (Class<E>) type.getEntityClass();
    query.alias = alias;
    query.select = "select " + alias;
    query.from = concat("from ", type.getEntityName(), " ", alias);
    return query;
  }

  /**
   * alias.
   * 
   * @param alias a {@link java.lang.String} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> alias(final String alias) {
    this.alias = alias;
    return this;
  }

  /**
   * join.
   * 
   * @param path a {@link java.lang.String} object.
   * @param alias a {@link java.lang.String} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> join(final String path, final String alias) {
    from = concat(from, " join ", path, " ", alias);
    return this;
  }

  /**
   * join.
   * 
   * @param joinMode a {@link java.lang.String} object.
   * @param path a {@link java.lang.String} object.
   * @param alias a {@link java.lang.String} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> join(final String joinMode, final String path, final String alias) {
    from = concat(from, " ", joinMode, " join ", path, " ", alias);
    return this;
  }

  public OqlBuilder<T> params(final Map<String, Object> params) {
    this.params.putAll(params);
    return this;
  }

  /**
   * param.
   * 
   * @param name a {@link java.lang.String} object.
   * @param value a {@link java.lang.Object} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> param(String name, Object value) {
    params.put(name, value);
    return this;
  }

  public OqlBuilder<T> limit(final PageLimit limit) {
    this.limit = limit;
    return this;
  }

  /**
   * limit.
   * 
   * @param pageNo a int.
   * @param pageSize a int.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> limit(final int pageNo, final int pageSize) {
    this.limit = new PageLimit(pageNo, pageSize);
    return this;
  }

  /**
   * cacheable.
   * 
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> cacheable() {
    this.cacheable = true;
    return this;
  }

  /**
   * cacheable.
   * 
   * @param cacheable a boolean.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> cacheable(final boolean cacheable) {
    this.cacheable = cacheable;
    return this;
  }

  /**
   * where.
   * 
   * @param condition a {@link org.beangle.commons.dao.query.builder.Condition} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> where(final Condition... conditions) {
    if (isNotEmpty(statement)) throw new RuntimeException("cannot add condition to a exists statement");
    return where(Arrays.asList(conditions));
  }

  /**
   * @param content
   * @return
   */
  public OqlBuilder<T> where(String content) {
    Condition con = new Condition(content);
    return where(con);
  }

  public OqlBuilder<T> where(String content, Object param1) {
    Condition con = new Condition(content);
    con.param(param1);
    return where(con);
  }

  public OqlBuilder<T> where(String content, Object param1, Object param2) {
    Condition con = new Condition(content);
    con.param(param1);
    con.param(param2);
    return where(con);
  }

  /**
   * 为了接受数组作为参数，防止java将数组展开为可变参数特定以下几个重致where函数，
   * 
   * @param content
   * @param param1
   * @param param2
   * @param param3
   * @param varparams
   * @see https://github.com/beangle/library/issues/231
   * @return
   */
  public OqlBuilder<T> where(String content, Object param1, Object param2, Object param3, Object... varparams) {
    Condition con = new Condition(content);
    con.param(param1);
    con.param(param2);
    con.param(param3);
    if (varparams != null && varparams.length > 0) {
      for (Object p : varparams)
        con.param(p);
    }
    return where(con);
  }

  /**
   * 添加一组条件<br>
   * query中不能添加条件集合作为一个条件,因此这里命名没有采用有区别性的addAll
   * 
   * @param cons a {@link java.util.Collection} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> where(final Collection<Condition> cons) {
    conditions.addAll(cons);
    return params(Conditions.getParamMap(cons));
  }

  /**
   * 声明排序字符串
   * 
   * @param orderBy 排序字符串
   * @return 查询构建器
   */
  public OqlBuilder<T> orderBy(final String orderBy) {
    return orderBy(Order.parse(orderBy));
  }

  /**
   * 指定排序字符串的位置
   * 
   * @param index 从0开始
   * @param orderBy 排序字符串
   * @return 查询构建器
   */
  public OqlBuilder<T> orderBy(final int index, final String orderBy) {
    if (null != orders) {
      if (isNotEmpty(statement)) { throw new RuntimeException("cannot add order by to a exists statement."); }
      this.orders.addAll(index, Order.parse(orderBy));
    }
    return this;
  }

  /**
   * orderBy.
   * 
   * @param order a {@link org.beangle.commons.collection.Order} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> orderBy(final Order order) {
    if (null != order) { return orderBy(Collections.singletonList(order)); }
    return this;
  }

  /**
   * cleanOrders.
   * 
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> clearOrders() {
    this.orders.clear();
    return this;
  }

  /**
   * orderBy.
   * 
   * @param orders a {@link java.util.List} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> orderBy(final List<Order> orders) {
    if (null != orders) {
      if (isNotEmpty(statement)) { throw new RuntimeException("cannot add order by to a exists statement."); }
      this.orders.addAll(orders);
    }
    return this;
  }

  /**
   * <p>
   * select.
   * </p>
   * 
   * @param what a {@link java.lang.String} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> select(final String what) {
    if (null == what) {
      this.select = null;
    } else {
      if (what.toLowerCase().trim().startsWith("select")) {
        this.select = what;
      } else {
        this.select = "select " + what;
      }
    }
    return this;
  }

  /**
   * newFrom.
   * 
   * @param from a {@link java.lang.String} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> newFrom(final String from) {
    if (null == from) {
      this.from = null;
    } else {
      if (Strings.contains(from.toLowerCase(), "from")) {
        this.from = from;
      } else {
        this.from = "from " + from;
      }
    }
    return this;
  }

  /**
   * groupBy.
   * 
   * @param what a {@link java.lang.String} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> groupBy(final String what) {
    if (Strings.isNotEmpty(what)) {
      groups.add(what);
    }
    return this;
  }

  /**
   * <p>
   * Having subclause.
   * </p>
   * 
   * @param what having subclause
   * @return this
   */
  public OqlBuilder<T> having(final String what) {
    Assert.isTrue(null != groups && !groups.isEmpty());
    if (Strings.isNotEmpty(what)) having = what;
    return this;
  }

  /**
   * 形成计数查询语句，如果不能形成，则返回""
   * 
   * @return a {@link java.lang.String} object.
   */
  protected String genCountStatement() {
    StringBuilder countString = new StringBuilder("select count(*) ");
    // 原始查询语句
    final String genQueryStr = genQueryStatement(false);
    if (isEmpty(genQueryStr)) { return ""; }
    final String lowerCaseQueryStr = genQueryStr.toLowerCase();

    if (contains(lowerCaseQueryStr, " group ")) { return ""; }
    if (contains(lowerCaseQueryStr, " union ")) { return ""; }

    final int indexOfFrom = findIndexOfFrom(lowerCaseQueryStr);
    final String selectWhat = lowerCaseQueryStr.substring(0, indexOfFrom);
    final int indexOfDistinct = selectWhat.indexOf("distinct");
    // select distinct a from table;
    if (-1 != indexOfDistinct) {
      if (contains(selectWhat, ",")) {
        return "";
      } else {
        countString = new StringBuilder("select count(");
        countString.append(genQueryStr.substring(indexOfDistinct, indexOfFrom)).append(") ");
      }
    }

    int orderIdx = genQueryStr.lastIndexOf(" order ");
    if (-1 == orderIdx) orderIdx = genQueryStr.length();
    countString.append(genQueryStr.substring(indexOfFrom, orderIdx));
    return countString.toString();
  }

  /**
   * Find index of from
   * 
   * @param query
   * @return -1 or from index
   */
  private int findIndexOfFrom(String query) {
    if (query.startsWith("from")) return 0;
    int fromIdx = query.indexOf(" from ");
    if (-1 == fromIdx) return -1;
    final int first = query.substring(0, fromIdx).indexOf("(");
    if (first > 0) {
      int leftCnt = 1;
      int i = first + 1;
      while (leftCnt != 0 && i < query.length()) {
        if (query.charAt(i) == '(') leftCnt++;
        else if (query.charAt(i) == ')') leftCnt--;
        i++;
      }
      if (leftCnt > 0) return -1;
      else {
        fromIdx = query.indexOf(" from ", i);
        return (fromIdx == -1) ? -1 : fromIdx + 1;
      }
    } else {
      return fromIdx + 1;
    }
  }

  /**
   * forEntity.
   * 
   * @param entityClass a {@link java.lang.Class} object.
   * @return a {@link org.beangle.commons.dao.query.builder.OqlBuilder} object.
   */
  public OqlBuilder<T> forEntity(final Class<T> entityClass) {
    this.entityClass = entityClass;
    return this;
  }

  @Override
  protected Lang getLang() {
    return Lang.HQL;
  }

  /**
   * Getter for the field <code>entityClass</code>.
   * 
   * @return a {@link java.lang.Class} object.
   */
  public Class<T> getEntityClass() {
    return entityClass;
  }
}
