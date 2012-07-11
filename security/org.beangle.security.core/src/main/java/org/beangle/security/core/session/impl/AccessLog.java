/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.security.core.session.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.validation.constraints.NotNull;

import org.beangle.commons.orm.pojo.LongIdObject;

/**
 * 访问日志
 * 
 * @author chaostone
 */
@Entity(name = "org.beangle.security.core.session.impl.AccessLog")
public class AccessLog extends LongIdObject {
  private static final long serialVersionUID = 1L;

  /** 会话ID */
  @NotNull
  private String sessionid;

  /** 资源 */
  @NotNull
  @Column(name = "resrc")
  private String resource;

  /** 开始时间 */
  @NotNull
  private Date beginAt = new Date();

  /** 结束时间 */
  @NotNull
  private Date endAt;

  /** 用户名 */
  @NotNull
  private String username;

  public AccessLog() {
    super();
  }

  public AccessLog(String sessionid, String username, String resource, long beginAt, long endAt) {
    super();
    this.sessionid = sessionid;
    this.username = username;
    this.resource = resource;
    this.beginAt = new Date(beginAt);
    this.endAt = new Date(endAt);
  }

  public long getDuration() {
    if (null == endAt) {
      return System.currentTimeMillis() - beginAt.getTime();
    } else {
      return endAt.getTime() - beginAt.getTime();
    }
  }

  public String getSessionid() {
    return sessionid;
  }

  public void setSessionid(String sessionid) {
    this.sessionid = sessionid;
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public Date getBeginAt() {
    return beginAt;
  }

  public void setBeginAt(Date beginAt) {
    this.beginAt = beginAt;
  }

  public Date getEndAt() {
    return endAt;
  }

  public void setEndAt(Date endAt) {
    this.endAt = endAt;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder(resource);
    sb.append('(');
    DateFormat f = new SimpleDateFormat("HH:mm:ss");
    sb.append(f.format(beginAt));
    sb.append('-');
    if (null != endAt) {
      sb.append(f.format(endAt));
      sb.append(" duration ").append((endAt.getTime() - beginAt.getTime()) / 1000).append(" s");
    } else {
      sb.append(" not ended");
    }
    sb.append(')');
    return sb.toString();
  }

}
