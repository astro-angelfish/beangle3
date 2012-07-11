/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.bean.converters;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.converters.BooleanConverter;
import org.apache.commons.beanutils.converters.DoubleConverter;
import org.apache.commons.beanutils.converters.FloatConverter;
import org.apache.commons.beanutils.converters.IntegerConverter;
import org.apache.commons.beanutils.converters.LongConverter;

/**
 * <p>
 * Converter class.
 * </p>
 * 
 * @author chaostone
 * @version $Id: $
 */
public final class Converter {

  /**
   * <p>
   * getDefault.
   * </p>
   * 
   * @return a {@link org.apache.commons.beanutils.ConvertUtilsBean} object.
   */
  public static final ConvertUtilsBean getDefault() {
    ConvertUtilsBean convertUtils = new ConvertUtilsBean();
    convertUtils.register(new SqlDateConverter(), java.sql.Date.class);
    convertUtils.register(new DateConverter(), java.util.Date.class);
    convertUtils.register(new BooleanConverter(null), Boolean.class);
    convertUtils.register(new IntegerConverter(null), Integer.class);
    convertUtils.register(new LongConverter(null), Long.class);
    convertUtils.register(new FloatConverter(null), Float.class);
    convertUtils.register(new DoubleConverter(null), Double.class);
    return convertUtils;
  }
}
