/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.transfer.importer;

import java.util.HashMap;
import java.util.Map;

import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.lang.Strings;
import org.beangle.commons.transfer.ItemTransfer;
import org.beangle.commons.transfer.io.ItemReader;
import org.beangle.commons.transfer.io.Reader;

/**
 * 线性导入实现
 * 
 * @author chaostone
 * @version $Id: $
 */
public abstract class AbstractItemImporter extends AbstractImporter implements Importer, ItemTransfer {

  /** 属性说明[attr,description] */
  protected Map<String, String> descriptions = new HashMap<String, String>();

  /** 导入属性 */
  protected String[] attrs;

  /** 当前导入值[attr,value] */
  protected Map<String, Object> values = CollectUtils.newHashMap();

  /**
   * <p>
   * Constructor for AbstractItemImporter.
   * </p>
   */
  public AbstractItemImporter() {
    super();
    this.prepare = new DescriptionAttrPrepare();
  }

  /** {@inheritDoc} */
  @Override
  public ItemReader getReader() {
    return (ItemReader) super.getReader();
  }

  /**
   * {@inheritDoc} 设置数据读取对象
   */
  public void setReader(Reader reader) {
    if (reader instanceof ItemReader) {
      this.reader = (ItemReader) reader;
    } else {
      throw new RuntimeException("Expected LineReader but：" + reader.getClass().getName());
    }
  }

  /**
   * 改变现有某个属性的值
   * 
   * @param attr a {@link java.lang.String} object.
   * @param value a {@link java.lang.Object} object.
   */
  public void changeCurValue(String attr, Object value) {
    values.put(attr, value);
  }

  /**
   * <p>
   * read.
   * </p>
   * 
   * @return a boolean.
   */
  public boolean read() {
    Object[] curData = (Object[]) reader.read();
    if (null == curData) {
      setCurrent(null);
      setCurData(null);
      return false;
    } else {
      for (int i = 0; i < curData.length; i++) {
        values.put(attrs[i], curData[i]);
      }
      return true;
    }
  }

  /**
   * <p>
   * isDataValid.
   * </p>
   * 
   * @return a boolean.
   */
  public boolean isDataValid() {
    boolean valid = false;
    for (Object value : values.values()) {
      if (value instanceof String) {
        String tt = (String) value;
        if (Strings.isNotBlank(tt)) {
          valid = true;
          break;
        }
      } else {
        if (null != value) {
          valid = true;
          break;
        }
      }
    }
    return valid;
  }

  /**
   * <p>
   * getCurData.
   * </p>
   * 
   * @return a {@link java.util.Map} object.
   */
  public Map<String, Object> getCurData() {
    return values;
  }

  /** {@inheritDoc} */
  public void setCurData(Map<String, Object> curData) {
    this.values = curData;
  }

  /**
   * <p>
   * Getter for the field <code>attrs</code>.
   * </p>
   * 
   * @return an array of {@link java.lang.String} objects.
   */
  public String[] getAttrs() {
    return attrs;
  }

  /**
   * <p>
   * Setter for the field <code>attrs</code>.
   * </p>
   * 
   * @param attrs an array of {@link java.lang.String} objects.
   */
  public void setAttrs(String[] attrs) {
    this.attrs = attrs;
  }

  /**
   * <p>
   * Getter for the field <code>descriptions</code>.
   * </p>
   * 
   * @return a {@link java.util.Map} object.
   */
  public Map<String, String> getDescriptions() {
    return descriptions;
  }

  /**
   * <p>
   * Setter for the field <code>descriptions</code>.
   * </p>
   * 
   * @param descriptions a {@link java.util.Map} object.
   */
  public void setDescriptions(Map<String, String> descriptions) {
    this.descriptions = descriptions;
  }

  /**
   * <p>
   * processAttr.
   * </p>
   * 
   * @param attr a {@link java.lang.String} object.
   * @return a {@link java.lang.String} object.
   */
  public String processAttr(String attr) {
    return attr;
  }
}
