/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.context.testbean;

public class TestService {

  TestDao testDao;

  public TestDao getTestDao() {
    return testDao;
  }

  public void setTestDao(TestDao testDao) {
    this.testDao = testDao;
  }

}
