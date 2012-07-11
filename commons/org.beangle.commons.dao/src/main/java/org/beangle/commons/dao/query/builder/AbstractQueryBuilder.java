/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.dao.query.builder;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.collection.Order;
import org.beangle.commons.collection.page.PageLimit;
import org.beangle.commons.dao.query.Lang;
import org.beangle.commons.dao.query.Query;
import org.beangle.commons.dao.query.QueryBuilder;
import org.beangle.commons.lang.Strings;

/**
 * <p>
 * Abstract AbstractQueryBuilder class.
 * </p>
 * 
 * @author chaostone
 * @version $Id: $
 */
public abstract class AbstractQueryBuilder<T> implements QueryBuilder<T> {

  /** Constant <code>INNER_JOIN=" left join "</code> */
  public static final String INNER_JOIN = " left join ";

  /** Constant <code>OUTER_JOIN=" outer join "</code> */
  public static final String OUTER_JOIN = " outer join ";

  /** Constant <code>LEFT_OUTER_JOIN=" left outer join "</code> */
  public static final String LEFT_OUTER_JOIN = " left outer join ";

  /** Constant <code>RIGHT_OUTER_JOIN=" right outer join "</code> */
  public static final String RIGHT_OUTER_JOIN = " right outer join ";

  /** query 查询语句 */
  protected String statement;

  /** 分页 */
  protected PageLimit limit;

  /** 参数 */
  protected Map<String, Object> params;

  protected String select;

  protected String from;

  /** 别名 */
  protected String alias;

  protected List<Condition> conditions = CollectUtils.newArrayList();

  protected List<Order> orders = CollectUtils.newArrayList();

  protected List<String> groups = CollectUtils.newArrayList();

  /** 缓存查询结果 */
  protected boolean cacheable = false;

  /**
   * <p>
   * Getter for the field <code>params</code>.
   * </p>
   * 
   * @param <T> a T object.
   * @return a {@link java.util.Map} object.
   */
  public Map<String, Object> getParams() {
    return (null == params) ? ConditionUtils.getParamMap(conditions) : CollectUtils.newHashMap(params);
  }

  /**
   * <p>
   * Getter for the field <code>alias</code>.
   * </p>
   * 
   * @return a {@link java.lang.String} object.
   */
  public String getAlias() {
    return alias;
  }

  /**
   * <p>
   * isCacheable.
   * </p>
   * 
   * @return a boolean.
   */
  public boolean isCacheable() {
    return cacheable;
  }

  /**
   * <p>
   * Getter for the field <code>limit</code>.
   * </p>
   * 
   * @return a {@link org.beangle.commons.collection.page.PageLimit} object.
   */
  public PageLimit getLimit() {
    return limit;
  }

  /**
   * <p>
   * build.
   * </p>
   * 
   * @return a {@link org.beangle.commons.dao.query.Query} object.
   */
  public Query<T> build() {
    QueryBean<T> queryBean = new QueryBean<T>();
    queryBean.setStatement(genStatement());
    queryBean.setParams(getParams());
    if (null != limit) {
      queryBean.setLimit(new PageLimit(limit.getPageNo(), limit.getPageSize()));
    }
    queryBean.setCountStatement(genCountStatement());
    queryBean.setCacheable(cacheable);
    queryBean.setLang(getLang());
    return queryBean;
  }

  /**
   * <p>
   * getLang.
   * </p>
   * 
   * @return a {@link org.beangle.commons.dao.query.Lang} object.
   */
  abstract protected Lang getLang();

  /**
   * {@inheritDoc}
   * <p>
   * 生成查询语句（如果查询语句已经存在则不进行生成）
   * 
   * @return a {@link java.lang.String} object.
   */
  protected String genStatement() {
    if (Strings.isNotEmpty(statement)) {
      return statement;
    } else {
      return genQueryStatement(true);
    }
  }

  /**
   * <p>
   * genCountStatement.
   * </p>
   * 
   * @return a {@link java.lang.String} object.
   */
  abstract protected String genCountStatement();

  /**
   * <p>
   * genQueryStatement.
   * </p>
   * 
   * @param hasOrder a boolean.
   * @return a {@link java.lang.String} object.
   */
  protected String genQueryStatement(final boolean hasOrder) {
    if (null == from) { return statement; }
    final StringBuilder buf = new StringBuilder(50);
    buf.append((select == null) ? "" : select).append(' ').append(from);
    if (!conditions.isEmpty()) {
      buf.append(" where ").append(ConditionUtils.toQueryString(conditions));
    }
    if (!groups.isEmpty()) {
      buf.append(" group by ");
      for (final String groupBy : groups) {
        buf.append(groupBy).append(',');
      }
      buf.deleteCharAt(buf.length() - 1);
    }
    if (hasOrder && !CollectionUtils.isEmpty(orders)) {
      buf.append(' ').append(Order.toSortString(orders));
    }
    return buf.toString();
  }

}
