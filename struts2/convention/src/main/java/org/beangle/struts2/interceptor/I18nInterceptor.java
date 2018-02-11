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
package org.beangle.struts2.interceptor;

import java.util.Locale;
import java.util.Map;

import org.apache.struts2.dispatcher.HttpParameters;
import org.apache.struts2.dispatcher.Parameter;
import org.beangle.commons.lang.Locales;

import com.opensymphony.xwork2.ActionInvocation;
import com.opensymphony.xwork2.interceptor.AbstractInterceptor;

/**
 * Simplify I18nInterceptor
 * 
 * @author chaostone
 * @since 2.4
 */
public class I18nInterceptor extends AbstractInterceptor {

  private static final long serialVersionUID = 3154197236572163145L;

  public static final String SessionAttribute = "WW_TRANS_I18N_LOCALE";
  public static final String SessionParameter = "session_locale";
  public static final String RequestParameter = "request_locale";

  @Override
  public String intercept(ActionInvocation invocation) throws Exception {
    HttpParameters params = invocation.getInvocationContext().getParameters();
    Locale locale = null;
    // get session locale
    Map<String, Object> session = invocation.getInvocationContext().getSession();
    if (null != session) {
      String session_locale = findLocaleParameter(params, SessionParameter);
      if (null == session_locale) {
        locale = (Locale) session.get(SessionAttribute);
      } else {
        locale = Locales.toLocale(session_locale);
        // save it in session
        session.put(SessionAttribute, locale);
      }
    }
    // get request locale
    String request_locale = findLocaleParameter(params, RequestParameter);
    if (null != request_locale) locale = Locales.toLocale(request_locale);

    if (null != locale) invocation.getInvocationContext().setLocale(locale);
    return invocation.invoke();
  }

  private String findLocaleParameter(HttpParameters params, String parameterName) {
    Parameter localParam = params.get(parameterName);
    params.remove(parameterName);
    String localValue = null;
    if (localParam != null) {
      if (localParam.isMultiple()) {
        localValue = localParam.getMultipleValues()[0];
      } else {
        localValue = localParam.getValue();
      }
    }
    return localValue;
  }
}
