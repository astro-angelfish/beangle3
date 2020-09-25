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
package org.beangle.struts2.convention.result;

import static org.beangle.commons.lang.Strings.contains;
import static org.beangle.commons.lang.Strings.isBlank;
import static org.beangle.commons.lang.Strings.isEmpty;
import static org.beangle.commons.lang.Strings.isNotEmpty;
import static org.beangle.commons.lang.Strings.substringAfter;
import static org.beangle.commons.lang.Strings.substringBefore;
import static org.beangle.commons.web.util.RequestUtils.getServletPath;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.result.ServletRedirectResult;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.apache.struts2.views.util.UrlHelper;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.struts2.convention.route.Action;
import org.beangle.struts2.convention.route.ActionBuilder;
import org.beangle.struts2.convention.route.ProfileService;
import org.beangle.struts2.convention.route.ViewMapper;
import org.beangle.struts2.freemarker.TemplateFinderByConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.ActionContext;
import com.opensymphony.xwork2.ObjectFactory;
import com.opensymphony.xwork2.Result;
import com.opensymphony.xwork2.XWorkException;
import com.opensymphony.xwork2.config.Configuration;
import com.opensymphony.xwork2.config.entities.ActionConfig;
import com.opensymphony.xwork2.config.entities.PackageConfig;
import com.opensymphony.xwork2.config.entities.ResultConfig;
import com.opensymphony.xwork2.config.entities.ResultTypeConfig;
import com.opensymphony.xwork2.inject.Inject;

/**
 * 按照各种result的特征进行结果构建<br>
 * 1)chain:/xxx?param1=value1&param2=value2<br>
 * 2)redirectAction:/yyy!method.action?param1=value1&param2=value2<br>
 * 2)redirect:/yyy!methodd?param1=value1&param2=value2<br>
 * 3)path/to/page/page.ftl<br>
 *
 * @author chaostone
 */
public class DefaultResultBuilder implements ResultBuilder {

  private static final Logger logger = LoggerFactory.getLogger(DefaultResultBuilder.class);

  /** [ftl->freemarker,vm->velocity] */
  private final Map<String, ResultTypeConfig> resultTypeConfigs = CollectUtils.newHashMap();

  private final ObjectFactory objectFactory;

  private final ViewMapper viewMapper;

  private final ProfileService profileService;

  private final ActionBuilder actionBuilder;

  private final TemplateFinderByConfig templateFinder;

  private final UrlHelper urlHelper;
  @Inject
  public DefaultResultBuilder(Configuration configuration, ObjectFactory objectFactory,
      FreemarkerManager freemarkerManager, ProfileService profileService, ActionBuilder actionBuilder,
      ViewMapper viewMapper,UrlHelper urlHelper) {
    super();
    this.objectFactory = objectFactory;
    this.profileService = profileService;
    this.actionBuilder = actionBuilder;
    this.viewMapper = viewMapper;
    this.urlHelper = urlHelper;
    this.templateFinder = new TemplateFinderByConfig(freemarkerManager.getConfig(), viewMapper);
    Map<String, String> typeExtensions = CollectUtils.toMap(new String[][] { { "freemarker", "ftl" },
        { "velocity", "vm" }, { "dispatcher", "jsp" } });
    PackageConfig pc = configuration.getPackageConfig("struts-default");
    for (String name : pc.getAllResultTypeConfigs().keySet()) {
      String extension = typeExtensions.get(name);
      ResultTypeConfig rtc = pc.getAllResultTypeConfigs().get(name);
      if (null != extension) resultTypeConfigs.put(extension, rtc);
      resultTypeConfigs.put(name, rtc);
    }
  }

  public Result build(String resultCode, ActionConfig actionConfig, ActionContext context) {
    String path = null;
    ResultTypeConfig cfg = null;
    logger.debug("result code:{} for actionConfig:{}", resultCode, actionConfig);
    // first route by common result
    if (!contains(resultCode, ':')) {
      Class<?> actionClass = context.getActionInvocation().getProxy().getAction().getClass();
      String className = actionClass.getName();
      String methodName = context.getActionInvocation().getProxy().getMethod();
      if (isEmpty(resultCode)) resultCode = "index";
      String extention = profileService.getProfile(className).getViewExtension();
      if (extention.equals("ftl")) {
        path = templateFinder.find(actionClass, methodName, resultCode, extention);
        if (null == path) {
          StringBuilder buf = new StringBuilder();
          buf.append(viewMapper.getViewPath(className, methodName, resultCode));
          buf.append('.').append(extention);
          path = buf.toString();
        }
      } else {
        StringBuilder buf = new StringBuilder();
        buf.append(viewMapper.getViewPath(className, methodName, resultCode));
        buf.append('.').append(extention);
        path = buf.toString();
      }
      cfg = resultTypeConfigs.get(extention);
      return buildResult(resultCode, cfg, context, buildResultParams(path, cfg));
    } else {
      // by prefix
      String prefix = substringBefore(resultCode, ":");
      cfg = (ResultTypeConfig) resultTypeConfigs.get(prefix);
      if (prefix.startsWith("chain")) {
        Action action = buildAction(substringAfter(resultCode, ":"));
        Map<String, String> params = buildResultParams(path, cfg);
        addNamespaceAction(action, params);
        if (isNotEmpty(action.getMethod())) params.put("method", action.getMethod());

        return buildResult(resultCode, cfg, context, params);
      } else if (prefix.startsWith("redirect")) {
        String targetResource = substringAfter(resultCode, ":");
        if (contains(targetResource, ':')) {
          ServletRedirectResult srr = new ServletRedirectResult(targetResource);
          srr.setUrlHelper(urlHelper);
          return srr;
        }
        Action action = buildAction(targetResource);

        // add special param and ajax tag for redirect
        HttpServletRequest request = ServletActionContext.getRequest();
        String[] redirectParamStrs = request.getParameterValues("_params");
        if (null != redirectParamStrs) {
          for (String redirectParamStr : redirectParamStrs)
            action.params(redirectParamStr);
        }

        // x-requested-with->XMLHttpRequest
        if (null != request.getHeader("x-requested-with")) action.param("x-requested-with", "1");

        Map<String, String> params = buildResultParams(path, cfg);
        if (null != action.getParams().get("method")) {
          params.put("method", (String) action.getParams().get("method"));
          action.getParams().remove("method");
        }

        if (isNotEmpty(action.getMethod())) params.put("method", action.getMethod());
        addNamespaceAction(action, params);

        ServletRedirectResult result = (ServletRedirectResult) buildResult(resultCode, cfg, context, params);
        for (Map.Entry<String, String> param : action.getParams().entrySet()) {
          String property = param.getKey();
          result.addParameter(property, param.getValue());
        }
        return result;
      } else {
        // 从结果中抽取路径和返回值
        path = substringAfter(resultCode, ":");
        resultCode = "success";
        return buildResult(resultCode, cfg, context, buildResultParams(path, cfg));
      }
    }
  }

  /**
   * 依据跳转路径进行构建
   *
   * @param path
   * @param param
   * @param redirectParamStr
   */
  private Action buildAction(String path) {
    Action action = (Action) ActionContext.getContext().getContextMap().get("dispatch_action");
    if (null == action) {
      action = new Action();
      String newPath = path;
      if (path.startsWith("?")) {
        newPath = getServletPath(ServletActionContext.getRequest()) + path;
      }
      action.path(newPath);
    } else {
      if (null != action.getClazz()) {
        Action newAction = actionBuilder.build(action.getClazz());
        action.name(newAction.getName()).namespace(newAction.getNamespace());
      }
      if (isBlank(action.getName())) {
        action.path(getServletPath(ServletActionContext.getRequest()));
      }
    }
    return action;
  }

  private void addNamespaceAction(Action action, Map<String, String> params) {
    params.put("namespace", action.getNamespace());
    params.put("actionName", action.getName());
  }

  protected Map<String, String> buildResultParams(String defaultParam, ResultTypeConfig resultTypeConfig) {
    Map<String, String> params = new LinkedHashMap<String, String>();
    if (resultTypeConfig.getParams() != null) {
      params.putAll(resultTypeConfig.getParams());
    }
    params.put(resultTypeConfig.getDefaultResultParam(), defaultParam);
    return params;
  }

  /**
   * 构建结果
   *
   * @param resultCode
   * @param resultTypeConfig
   * @param context
   * @param params
   */
  protected Result buildResult(String resultCode, ResultTypeConfig resultTypeConfig, ActionContext context,
      Map<String, String> params) {
    ResultConfig resultConfig = new ResultConfig.Builder(resultCode, resultTypeConfig.getClassName())
        .addParams(params).build();
    try {
      return objectFactory.buildResult(resultConfig, context.getContextMap());
    } catch (Exception e) {
      throw new XWorkException("Unable to build convention result", e, resultConfig);
    }
  }
}
