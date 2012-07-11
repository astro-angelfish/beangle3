/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.context.spring;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.beangle.commons.bean.Disposable;
import org.beangle.commons.bean.Initializing;
import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.context.event.DefaultEventMulticaster;
import org.beangle.commons.context.inject.BeanConfig;
import org.beangle.commons.context.inject.BeanConfig.Definition;
import org.beangle.commons.context.inject.BeanConfig.ListProperty;
import org.beangle.commons.context.inject.BeanConfig.ReferenceProperty;
import org.beangle.commons.context.inject.BindModule;
import org.beangle.commons.context.inject.BindRegistry;
import org.beangle.commons.context.inject.Resources;
import org.beangle.commons.context.inject.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.core.MethodParameter;
import org.springframework.core.io.UrlResource;
import org.springframework.util.ClassUtils;

/**
 * 完成springbean的自动注册和再配置
 * 
 * @author chaostone
 * @version $Id: $
 */
public class SpringConfigProcessor implements BeanDefinitionRegistryPostProcessor {

  private static final Logger logger = LoggerFactory.getLogger(SpringConfigProcessor.class);

  private Resources resources;

  /** {@inheritDoc} */
  public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry definitionRegistry)
      throws BeansException {
    // 自动注册
    autoconfig(definitionRegistry);
    // 再配置
    reconfig(definitionRegistry);
  }

  /** {@inheritDoc} */
  public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
  }

  private void reconfig(BeanDefinitionRegistry registry) {
    if (null == resources || resources.isEmpty()) return;
    Set<String> beanNames = CollectUtils.newHashSet();
    BeanDefinitionReader reader = new BeanDefinitionReader();
    for (URL url : resources.getAllPaths()) {
      List<ReconfigBeanDefinitionHolder> holders = reader.load(new UrlResource(url));
      for (ReconfigBeanDefinitionHolder holder : holders) {
        if (holder.getConfigType().equals(ReconfigType.REMOVE)) {
        } else {
          BeanDefinition definition = null;
          try {
            definition = registry.getBeanDefinition(holder.getBeanName());
          } catch (NoSuchBeanDefinitionException e) {
            logger.warn("No bean {} to reconfig defined in {}.", holder.getBeanName(), url);
            continue;
          }
          String successName = mergeDefinition(definition, holder);
          if (null != successName) beanNames.add(successName);
        }
      }
    }
    if (!beanNames.isEmpty()) {
      logger.info("Reconfig bean : {}", beanNames);
    }
  }

  private void autoconfig(BeanDefinitionRegistry definitionRegistry) {
    StopWatch watch = new StopWatch();
    watch.start();
    BindRegistry registry = new SpringBindRegistry(definitionRegistry);
    Map<String, BeanDefinition> newDefinitions = findRegistedModules(registry);
    // should register after all beans
    registerBuildins(registry);
    autowire(newDefinitions, registry);
    lifecycle(registry, definitionRegistry);
    logger.debug("Auto register and wire bean [{}]", newDefinitions.keySet());
    logger.info("Auto register and wire {} beans using {} mills", newDefinitions.size(), watch.getTime());
  }

  /**
   * <p>
   * lifecycle.
   * </p>
   * 
   * @param registry a {@link org.beangle.commons.context.inject.BindRegistry} object.
   * @param definitionRegistry a
   *          {@link org.springframework.beans.factory.support.BeanDefinitionRegistry} object.
   */
  protected void lifecycle(BindRegistry registry, BeanDefinitionRegistry definitionRegistry) {
    for (String name : registry.getBeanNames()) {
      String springName = name;
      if (name.startsWith("&")) springName = name.substring(1);
      Class<?> clazz = registry.getBeanType(name);
      AbstractBeanDefinition def = (AbstractBeanDefinition) definitionRegistry.getBeanDefinition(springName);
      if (Initializing.class.isAssignableFrom(clazz) && null == def.getInitMethodName()
          && !def.getPropertyValues().contains("init-method")) {
        def.setInitMethodName("init");
      }
      if (Disposable.class.isAssignableFrom(clazz) && null == def.getDestroyMethodName()
          && !def.getPropertyValues().contains("destroy-method")) {
        def.setDestroyMethodName("destroy");
      }
    }
  }

  /**
   * <p>
   * registerBuildins.
   * </p>
   * 
   * @param registry a {@link org.beangle.commons.context.inject.BindRegistry} object.
   */
  protected void registerBuildins(BindRegistry registry) {
    // FIXME for listeners inject
    registerBean(new Definition(DefaultEventMulticaster.class.getName(), DefaultEventMulticaster.class,
        Scope.SINGLETON.toString()), registry);
  }

  /**
   * 合并bean定义
   * 
   * @param target a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @param source a {@link org.beangle.commons.context.spring.ReconfigBeanDefinitionHolder} object.
   * @return a {@link java.lang.String} object.
   */
  protected String mergeDefinition(BeanDefinition target, ReconfigBeanDefinitionHolder source) {
    if (null == target.getBeanClassName()) {
      logger.warn("ingore bean definition {} for without class", source.getBeanName());
      return null;
    }
    // 当类型变化后,删除原有配置
    if (null != source.getBeanDefinition().getBeanClassName()
        && !source.getBeanDefinition().getBeanClassName().equals(target.getBeanClassName())) {
      target.setBeanClassName(source.getBeanDefinition().getBeanClassName());
      for (PropertyValue pv : target.getPropertyValues().getPropertyValues()) {
        target.getPropertyValues().removePropertyValue(pv);
      }
    }
    MutablePropertyValues pvs = source.getBeanDefinition().getPropertyValues();
    for (PropertyValue pv : pvs.getPropertyValueList()) {
      String name = pv.getName();
      target.getPropertyValues().addPropertyValue(name, pv.getValue());
      logger.debug("config {}.{} = {}", new Object[] { source.getBeanName(), name, pv.getValue() });
    }
    logger.debug("Reconfig bean {} ", source.getBeanName());
    return source.getBeanName();
  }

  /**
   * <p>
   * findRegistedModules.
   * </p>
   * 
   * @param registry a {@link org.beangle.commons.context.inject.BindRegistry} object.
   * @return a {@link java.util.Map} object.
   */
  protected Map<String, BeanDefinition> findRegistedModules(BindRegistry registry) {
    List<String> modules = registry.getBeanNames(BindModule.class);
    Map<String, BeanDefinition> newBeanDefinitions = CollectUtils.newHashMap();
    for (String name : modules) {
      Class<?> beanClass = registry.getBeanType(name);
      BeanConfig config = null;
      try {
        config = ((BindModule) beanClass.newInstance()).getConfig();
      } catch (Exception e) {
        logger.error("class initialization error of  " + beanClass, e);
        continue;
      }
      List<BeanConfig.Definition> definitions = config.getDefinitions();
      for (BeanConfig.Definition definition : definitions) {
        String beanName = definition.beanName;
        if (registry.contains(beanName)) {
          logger.warn("Ingore exists bean definition {}", beanName);
        } else {
          BeanDefinition def = registerBean(definition, registry);
          newBeanDefinitions.put(beanName, def);
        }
      }
    }
    return newBeanDefinitions;
  }

  private BeanDefinition convert(Definition definition) {
    GenericBeanDefinition def = new GenericBeanDefinition();
    def.setBeanClass(definition.clazz);
    def.setScope(definition.scope);
    if (null != definition.initMethod) def.setInitMethodName(definition.initMethod);
    MutablePropertyValues mpv = new MutablePropertyValues();
    for (Map.Entry<String, Object> entry : definition.getProperties().entrySet()) {
      Object value = entry.getValue();
      if (value instanceof ListProperty) {
        // create a spring managed list
        List<Object> list = new ManagedList<Object>();
        for (Object item : ((ListProperty) value).items) {
          if (item instanceof ReferenceProperty) {
            list.add(new RuntimeBeanReference(((ReferenceProperty) item).ref));
          } else {
            list.add(item);
          }
        }
        value = list;
      } else if (value instanceof Definition) {
        value = new BeanDefinitionHolder(convert((Definition) value), ((Definition) value).beanName);
      }
      mpv.add(entry.getKey(), value);
    }
    def.setLazyInit(definition.lazyInit);
    def.setAbstract(definition.isAbstract());
    def.setPropertyValues(mpv);
    return def;
  }

  /**
   * <p>
   * registerBean.
   * </p>
   * 
   * @param beanName a {@link java.lang.String} object.
   * @param registry a {@link org.beangle.commons.context.inject.BindRegistry} object.
   * @return a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   */
  protected BeanDefinition registerBean(Definition definition, BindRegistry registry) {
    BeanDefinition def = convert(definition);
    registry.register(definition.clazz, definition.beanName, def);

    if (FactoryBean.class.isAssignableFrom(definition.clazz)) {
      registry.register(definition.clazz, "&" + definition.beanName);
      try {
        Class<?> artifactClass = ((FactoryBean<?>) definition.clazz.newInstance()).getObjectType();
        if (null != artifactClass) registry.register(artifactClass, definition.beanName);
      } catch (Exception e) {
        logger.error("Cannot get Factory's Object Type {}", definition.clazz);
      }
    }
    logger.debug("Register definition {} for {}", definition.beanName, definition.clazz);
    return def;
  }

  /**
   * <p>
   * autowire.
   * </p>
   * 
   * @param newBeanDefinitions a {@link java.util.Map} object.
   * @param registry a {@link org.beangle.commons.context.inject.BindRegistry} object.
   */
  protected void autowire(Map<String, BeanDefinition> newBeanDefinitions, BindRegistry registry) {
    for (Map.Entry<String, BeanDefinition> entry : newBeanDefinitions.entrySet()) {
      String beanName = entry.getKey();
      BeanDefinition mbd = entry.getValue();
      autowireBean(beanName, mbd, registry);
    }
  }

  /**
   * <p>
   * autowireBean.
   * </p>
   * 
   * @param beanName a {@link java.lang.String} object.
   * @param mbd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @param registry a {@link org.beangle.commons.context.inject.BindRegistry} object.
   */
  protected void autowireBean(String beanName, BeanDefinition mbd, BindRegistry registry) {
    Map<String, PropertyDescriptor> properties = unsatisfiedNonSimpleProperties(mbd);
    for (Map.Entry<String, PropertyDescriptor> entry : properties.entrySet()) {
      String propertyName = entry.getKey();
      PropertyDescriptor pd = entry.getValue();
      if (Object.class.equals(pd.getPropertyType())) continue;
      MethodParameter methodParam = BeanUtils.getWriteMethodParameter(pd);
      List<String> beanNames = registry.getBeanNames(methodParam.getParameterType());
      boolean binded = false;
      if (beanNames.size() == 1) {
        mbd.getPropertyValues().add(propertyName, new RuntimeBeanReference(beanNames.get(0)));
        binded = true;
      } else if (beanNames.size() > 1) {
        for (String name : beanNames) {
          if (name.equals(propertyName)) {
            mbd.getPropertyValues().add(propertyName, new RuntimeBeanReference(propertyName));
            binded = true;
            break;
          }
        }
      }
      if (!binded) {
        if (beanNames.isEmpty()) {
          logger.debug(beanName + "'s " + propertyName + "  cannot  found candidate bean");
        } else {
          logger.warn(beanName + "'s " + propertyName + " expected single bean but found {} : {}",
              beanNames.size(), beanNames);
        }
      }
    }
  }

  private static boolean isExcludedFromDependencyCheck(PropertyDescriptor pd) {
    Method wm = pd.getWriteMethod();
    if (wm == null) { return false; }
    if (!wm.getDeclaringClass().getName().contains("$$")) {
      // Not a CGLIB method so it's OK.
      return false;
    }
    // It was declared by CGLIB, but we might still want to autowire it
    // if it was actually declared by the superclass.
    Class<?> superclass = wm.getDeclaringClass().getSuperclass();
    return !ClassUtils.hasMethod(superclass, wm.getName(), wm.getParameterTypes());
  }

  private Map<String, PropertyDescriptor> unsatisfiedNonSimpleProperties(BeanDefinition mbd) {
    Map<String, PropertyDescriptor> properties = CollectUtils.newHashMap();
    PropertyValues pvs = mbd.getPropertyValues();
    Class<?> clazz = null;
    try {
      clazz = Class.forName(mbd.getBeanClassName());
    } catch (ClassNotFoundException e) {
      logger.error("Class " + mbd.getBeanClassName() + "not found", e);
      return properties;
    }
    PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(clazz);
    for (PropertyDescriptor pd : pds) {
      if (pd.getWriteMethod() != null && !isExcludedFromDependencyCheck(pd) && !pvs.contains(pd.getName())
          && !BeanUtils.isSimpleProperty(pd.getPropertyType())) {
        properties.put(pd.getName(), pd);
      }
    }
    return properties;
  }

  /**
   * <p>
   * Setter for the field <code>resource</code>.
   * </p>
   * 
   * @param resources a {@link org.beangle.commons.context.inject.Resources} object.
   */
  public void setResources(Resources resources) {
    this.resources = resources;
  }

}
