/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2016, Beangle Software.
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
package org.beangle.commons.entity.metadata;

import java.sql.Date;

import org.beangle.commons.entity.pojo.IntegerIdObject;

public class ManagerEmployer extends IntegerIdObject {

  private static final long serialVersionUID = 1371741254315503984L;

  ContractInfo contractInfo;

  Date birthday;

  public ContractInfo getContractInfo() {
    return contractInfo;
  }

  public void setContractInfo(ContractInfo contractInfo) {
    this.contractInfo = contractInfo;
  }

  public Date getBirthday() {
    return birthday;
  }

  public void setBirthday(Date birthday) {
    this.birthday = birthday;
  }

}
