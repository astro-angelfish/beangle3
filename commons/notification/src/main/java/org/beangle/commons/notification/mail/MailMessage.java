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
package org.beangle.commons.notification.mail;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.mail.internet.InternetAddress;

import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.lang.Assert;
import org.beangle.commons.lang.Strings;
import org.beangle.commons.notification.AbstractMessage;

public class MailMessage extends AbstractMessage {

  private InternetAddress from = null;

  private List<InternetAddress> to = CollectUtils.newArrayList();

  private List<InternetAddress> cc = CollectUtils.newArrayList();

  private List<InternetAddress> bcc = CollectUtils.newArrayList();

  private Date sentAt;

  public MailMessage() {
    super();
  }

  public String getEncoding() {
    return Strings.substringAfter(getContentType(), "charset=");
  }

  public List<String> getRecipients() {
    List<String> recipients = new ArrayList<String>();
    for (InternetAddress address : to)
      recipients.add(address.toString());
    for (InternetAddress address : cc)
      recipients.add(address.toString());
    for (InternetAddress address : bcc)
      recipients.add(address.toString());
    return recipients;
  }

  public MailMessage(String subject, String text, String sendTo) {
    to = MimeUtils.parseAddress(sendTo, getEncoding());
    setSubject(subject);
    setText(text);
  }

  public MailMessage(String subject, String text, String sendTo, String sendCc, String sendBcc) {
    to = MimeUtils.parseAddress(sendTo, getEncoding());
    cc = MimeUtils.parseAddress(sendCc, getEncoding());
    bcc = MimeUtils.parseAddress(sendBcc, getEncoding());
    setSubject(subject);
    setText(text);
  }

  public List<InternetAddress> getTo() {
    return to;
  }

  public List<InternetAddress> getCc() {
    return cc;
  }

  public List<InternetAddress> getBcc() {
    return bcc;
  }

  public MailMessage from(String from) {
    List<InternetAddress> froms = MimeUtils.parseAddress(from, getEncoding());
    if (froms.size() > 0) this.from = froms.get(0);
    return this;
  }

  public InternetAddress getFrom() {
    return from;
  }

  public void addTo(String sendTo) {
    Assert.notNull(sendTo);
    this.to.addAll(MimeUtils.parseAddress(sendTo, getEncoding()));
  }

  public void addCc(String sendCc) {
    Assert.notNull(sendCc);
    this.cc.addAll(MimeUtils.parseAddress(sendCc, getEncoding()));
  }

  public void addBcc(String sendBcc) {
    Assert.notNull(sendBcc);
    this.bcc.addAll(MimeUtils.parseAddress(sendBcc, getEncoding()));
  }

  public Date getSentAt() {
    return sentAt;
  }

  public void setSentAt(Date sendAt) {
    this.sentAt = sendAt;
  }

}
