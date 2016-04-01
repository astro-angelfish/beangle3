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
package org.beangle.struts2.view.template;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.ServletActionContext;
import org.apache.struts2.views.freemarker.FreemarkerManager;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.lang.Throwables;
import org.beangle.struts2.freemarker.BeangleClassTemplateLoader;
import org.beangle.struts2.view.component.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opensymphony.xwork2.inject.Inject;
import com.opensymphony.xwork2.util.ValueStack;

import freemarker.cache.StrongCacheStorage;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.SimpleHash;
import freemarker.template.Template;

/**
 * Freemarker Template Engine
 * <ul>
 * <li>User hashmodel store in request</li>
 * <li>Load hierarchical templates</li>
 * <li>Disabled freemarker localized lookup in template loading</li>
 * </ul>
 * 
 * @author chaostone
 */
public class FreemarkerTemplateEngine extends AbstractTemplateEngine {
  private static final Logger logger = LoggerFactory.getLogger(FreemarkerTemplateEngine.class);

  protected FreemarkerManager freemarkerManager;
  protected Configuration config;

  public void render(String template, ValueStack stack, Writer writer, Component component) throws Exception {
    SimpleHash model = buildModel(stack, component);
    Object prevTag = model.get("tag");
    model.put("tag", component);
    getTemplate(template).process(model, writer);
    if (null != prevTag) model.put("tag", prevTag);
  }

  /**
   * Clone configuration from FreemarkerManager,but custmize in
   * <ul>
   * <li>Disable freemarker localized lookup
   * <li>Cache two hour(7200s) and Strong cache
   * <li>Disable auto imports and includes
   * </ul>
   */
  @Inject
  public void setFreemarkerManager(FreemarkerManager mgr) {
    this.freemarkerManager = mgr;
    if (null != freemarkerManager) {
      Configuration old = freemarkerManager.getConfig();
      if (null != old) {
        config = (Configuration) freemarkerManager.getConfig().clone();
        config.setTemplateLoader(new HierarchicalTemplateLoader(this, config.getTemplateLoader()));
      } else {
        config = new Configuration();
        config.setTemplateLoader(new HierarchicalTemplateLoader(this, new BeangleClassTemplateLoader(null)));
      }
      // Disable freemarker localized lookup
      config.setLocalizedLookup(false);
      config.setEncoding(config.getLocale(), "UTF-8");

      // Cache one hour(7200s) and Strong cache
      config.setTemplateUpdateDelay(7200);
      // config.setCacheStorage(new MruCacheStorage(100,250));
      config.setCacheStorage(new StrongCacheStorage());

      // Disable auto imports and includes
      config.setAutoImports(CollectUtils.newHashMap());
      config.setAutoIncludes(CollectUtils.newArrayList(0));
    }
  }

  /**
   * Load template in hierarchical path
   * 
   * @param templateName
   * @throws Exception
   */
  private Template getTemplate(String templateName) throws ParseException {
    try {
      return config.getTemplate(templateName, "UTF-8");
    } catch (ParseException e) {
      throw e;
    } catch (IOException e) {
      logger.error("Couldn't load template '{}',loader is {}", templateName, config.getTemplateLoader()
          .getClass());
      throw Throwables.propagate(e);
    }
  }

  /**
   * componentless model(one per request)
   * 
   * @param stack
   * @param component
   */
  private SimpleHash buildModel(ValueStack stack, Component component) {
    Map<?, ?> context = stack.getContext();
    HttpServletRequest req = (HttpServletRequest) context.get(ServletActionContext.HTTP_REQUEST);
    // build hash
    SimpleHash model = (SimpleHash) req.getAttribute(FreemarkerManager.ATTR_TEMPLATE_MODEL);
    if (null == model) {
      model = freemarkerManager.buildTemplateModel(stack, null,
          (ServletContext) context.get(ServletActionContext.SERVLET_CONTEXT), req,
          (HttpServletResponse) context.get(ServletActionContext.HTTP_RESPONSE), config.getObjectWrapper());
      req.setAttribute(FreemarkerManager.ATTR_TEMPLATE_MODEL, model);
    }
    return model;
  }

  public final String getSuffix() {
    return ".ftl";
  }
}
