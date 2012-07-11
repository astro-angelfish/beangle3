/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.transfer.importer.listener;

import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.PropertyUtils;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.dao.Entity;
import org.beangle.commons.dao.EntityDao;
import org.beangle.commons.dao.metadata.Model;
import org.beangle.commons.dao.util.EntityUtils;
import org.beangle.commons.lang.Strings;
import org.beangle.commons.transfer.TransferResult;
import org.beangle.commons.transfer.importer.MultiEntityImporter;

/**
 * 导入数据外键监听器<br>
 * 这里尽量使用entityDao，因为在使用entityService加载其他代码时，jpa会保存还未修改外的"半成对象"<br>
 * 从而造成有些外键是空对象的错误<br>
 * 如果外键不存在，则目标中的外键会置成null；<br>
 * 如果外键是空的，那么目标的外键取决于importer.isIgnoreNull取值
 * 
 * @author chaostone
 * @version $Id: $
 */
public class ImporterForeignerListener extends ItemImporterListener {

  protected EntityDao entityDao;

  protected Map<String, Map<String, Object>> foreigersMap = CollectUtils.newHashMap();

  private static final int CACHE_SIZE = 500;

  private String[] foreigerKeys = { "code" };

  private boolean multiEntity = false;

  /**
   * <p>
   * Constructor for ImporterForeignerListener.
   * </p>
   * 
   * @param entityDao a {@link org.beangle.commons.dao.EntityDao} object.
   */
  public ImporterForeignerListener(EntityDao entityDao) {
    this.entityDao = entityDao;
  }

  /** {@inheritDoc} */
  public void onStart(TransferResult tr) {
    if (importer.getClass().equals(MultiEntityImporter.class)) {
      multiEntity = true;
    }
    super.onStart(tr);
  }

  /** {@inheritDoc} */
  public void onItemFinish(TransferResult tr) {
    // 过滤所有外键
    for (int i = 0; i < importer.getAttrs().length; i++) { // getAttrs()得到属性,即表的第二行
      String attr = importer.getAttrs()[i];

      String processed = importer.processAttr(attr);
      int foreigerKeyIndex = 0;
      boolean isforeiger = false;
      for (; foreigerKeyIndex < foreigerKeys.length; foreigerKeyIndex++) {
        if (processed.endsWith("." + foreigerKeys[foreigerKeyIndex])) {// ?
          isforeiger = true;
          break;
        }
      }
      if (!isforeiger) continue;

      String codeValue = (String) importer.getCurData().get(attr);
      try {
        Object foreiger = null;
        // 外键的代码是空的
        if (Strings.isEmpty(codeValue)) continue;
        Object entity = null;
        if (multiEntity) {
          entity = ((MultiEntityImporter) importer).getCurrent(attr);
        } else {
          entity = importer.getCurrent();
        }

        attr = importer.processAttr(attr);
        Object nestedForeigner = PropertyUtils.getProperty(entity,
            Strings.substring(attr, 0, attr.lastIndexOf(".")));

        if (nestedForeigner instanceof Entity<?>) {
          String className = EntityUtils.getEntityClassName(nestedForeigner.getClass());
          Map<String, Object> foreignerMap = foreigersMap.get(className);
          if (null == foreignerMap) {
            foreignerMap = CollectUtils.newHashMap();
            foreigersMap.put(className, foreignerMap);
          }
          if (foreignerMap.size() > CACHE_SIZE) foreignerMap.clear();
          foreiger = foreignerMap.get(codeValue);
          if (foreiger == null) {
            List<?> foreigners = entityDao.get(Class.forName(className), foreigerKeys[foreigerKeyIndex],
                new Object[] { codeValue });
            if (!foreigners.isEmpty()) {
              foreiger = foreigners.iterator().next();
              foreignerMap.put(codeValue, foreiger);
            } else {
              tr.addFailure("error.model.notExist", codeValue);
            }
          }
        }
        String parentAttr = Strings.substring(attr, 0, attr.lastIndexOf("."));
        Model.getPopulator().populateValue(entity, parentAttr, foreiger);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }

  /**
   * <p>
   * addForeigerKey.
   * </p>
   * 
   * @param key a {@link java.lang.String} object.
   */
  public void addForeigerKey(String key) {
    String[] foreigers = new String[foreigerKeys.length + 1];
    int i = 0;
    for (; i < foreigerKeys.length; i++) {
      foreigers[i] = foreigerKeys[i];
    }
    foreigers[i] = key;
    foreigerKeys = foreigers;
  }
}
