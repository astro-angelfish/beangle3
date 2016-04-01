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
package org.beangle.orm.hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import javax.sql.DataSource;

import org.beangle.commons.bean.Disposable;
import org.beangle.commons.bean.Initializing;
import org.beangle.commons.entity.orm.AbstractPersistModule;
import org.beangle.commons.entity.orm.EntityPersistConfig;
import org.beangle.commons.io.IOs;
import org.beangle.commons.lang.ClassLoaders;
import org.beangle.commons.lang.reflect.Reflections;
import org.beangle.commons.lang.time.Stopwatch;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

/**
 * @author chaostone
 * @version $Id: SessionFactoryBean.java Feb 27, 2012 10:52:27 PM chaostone $
 */
public class SessionFactoryBean implements FactoryBean<SessionFactory>, Initializing, Disposable,
    BeanClassLoaderAware {

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private Class<? extends Configuration> configurationClass = Configuration.class;

  private DataSource dataSource;

  /** static and global hbm mapping without namingstrategy */
  private Resource staticHbm;

  private Resource[] configLocations;

  private Resource[] persistLocations;

  private NamingStrategy namingStrategy;

  private Properties hibernateProperties;

  private SessionFactory sessionFactory;

  private ClassLoader beanClassLoader = ClassLoaders.getDefaultClassLoader();

  private Configuration configuration;

  /**
   * Set the locations of multiple Hibernate XML config files, for example as
   * classpath resources "classpath:hibernate.cfg.xml,classpath:extension.cfg.xml".
   * <p>
   * Note: Can be omitted when all necessary properties and mapping resources are specified locally
   * via this bean.
   * 
   * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
   */
  public void setConfigLocations(Resource[] configLocations) {
    this.configLocations = configLocations;
  }

  public void setPersistLocations(Resource[] persistLocations) {
    this.persistLocations = persistLocations;
  }

  /**
   * Set Hibernate properties, such as "hibernate.dialect".
   * <p>
   * Can be used to override values in a Hibernate XML config file, or to specify all necessary
   * properties locally.
   * <p>
   * Note: Do not specify a transaction provider here when using Spring-driven transactions. It is
   * also advisable to omit connection provider settings and use a Spring-set DataSource instead.
   * 
   * @see #setDataSource
   */
  public void setHibernateProperties(Properties hibernateProperties) {
    this.hibernateProperties = hibernateProperties;
  }

  /**
   * Return the Hibernate properties, if any. Mainly available for
   * configuration through property paths that specify individual keys.
   */
  public Properties getHibernateProperties() {
    if (this.hibernateProperties == null) this.hibernateProperties = new Properties();
    return this.hibernateProperties;
  }

  /**
   * Set a Hibernate NamingStrategy for the SessionFactory, determining the
   * physical column and table names given the info in the mapping document.
   * 
   * @see org.hibernate.cfg.Configuration#setNamingStrategy
   */
  public void setNamingStrategy(NamingStrategy namingStrategy) {
    this.namingStrategy = namingStrategy;
  }

  private void staticInit() throws Exception {
    configuration.addCacheableFile(staticHbm.getFile());
  }

  @SuppressWarnings("unchecked")
  private void dynamicInit() throws Exception {
    if (this.namingStrategy != null) configuration.setNamingStrategy(this.namingStrategy);
    try {
      if (null != configLocations) {
        for (Resource resource : configLocations)
          configuration.configure(resource.getURL());
      }
      if (null != persistLocations) {
        for (Resource resource : persistLocations) {
          InputStream is = resource.getURL().openStream();
          Properties props = new Properties();
          if (null != is) props.load(is);

          Object module = props.remove("module");
          if (null == module) {
            logger.warn("Cannot find module in {}", resource.getURL());
            continue;
          }

          Class<? extends AbstractPersistModule> moduleClass = (Class<? extends AbstractPersistModule>) ClassLoaders
              .loadClass(module.toString());
          addPersistInfo(moduleClass.newInstance().getConfig());
          Enumeration<String> enumer = (Enumeration<String>) props.propertyNames();
          while (enumer.hasMoreElements()) {
            String propertyName = enumer.nextElement();
            configuration.setProperty(propertyName, props.getProperty(propertyName));
          }
          IOs.close(is);
        }
      }
    } finally {
    }
  }

  protected void processProperties() {
    if (hibernateProperties == null) hibernateProperties = new Properties();

    // populate system properties
    Properties sysProps = System.getProperties();
    Enumeration<?> keys = sysProps.propertyNames();
    while (keys.hasMoreElements()) {
      String key = (String) keys.nextElement();
      if (key.startsWith("hibernate.")) {
        String value = sysProps.getProperty(key);
        boolean override = hibernateProperties.containsKey(key);
        hibernateProperties.put(key, value);
        if (override) logger.info("Override hibernate property {}={}", key, value);
      }
    }

    configuration.getProperties().put("hibernate.classLoader.application", beanClassLoader);
    // Set Hibernate 4.0+ CurrentSessionContext implementation,
    // provide the Beangle-managed Session as current Session.
    configuration.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS,
        BeangleSessionContext.class.getName());
    if (dataSource != null) configuration.getProperties().put(Environment.DATASOURCE, dataSource);

    // Disable JdbcServicesImpl magic behaviour except declare explicitly.
    // for it will slow startup performance. and just consult medata's ddl semantic, which is
    // seldom used.
    final String useJdbcMetaName = "hibernate.temp.use_jdbc_metadata_defaults";
    if (hibernateProperties.containsKey(Environment.DIALECT)
        && !hibernateProperties.containsKey(useJdbcMetaName)) {
      hibernateProperties.put(useJdbcMetaName, "false");
    }
    configuration.addProperties(this.hibernateProperties);
  }

  public void init() throws Exception {
    boolean staticInit = (null != staticHbm);
    try {
      staticHbm.getFile();
    } catch (IOException e) {
      staticInit = false;
    }

    if (staticInit) configuration = new Configuration();
    else configuration = newConfiguration();

    processProperties();

    if (staticInit) staticInit();
    else dynamicInit();

    // do session factory build.
    Stopwatch watch = new Stopwatch(true);

    final ServiceRegistry serviceRegistry = new ServiceRegistryBuilder().applySettings(
        configuration.getProperties()).buildServiceRegistry();

    configuration.setSessionFactoryObserver(new SessionFactoryObserver() {
      private static final long serialVersionUID = 1L;

      @Override
      public void sessionFactoryCreated(SessionFactory factory) {
      }

      @Override
      public void sessionFactoryClosed(SessionFactory factory) {
        ServiceRegistryBuilder.destroy(serviceRegistry);
      }
    });
    this.sessionFactory = configuration.buildSessionFactory(serviceRegistry);
    logger.info("Building Hibernate SessionFactory in {}", watch);

  }

  /**
   * Add annotation class from persist configuration
   * 
   * @param epconfig
   */
  private void addPersistInfo(EntityPersistConfig epconfig) {
    for (EntityPersistConfig.EntityDefinition definition : epconfig.getEntites()) {
      configuration.addAnnotatedClass(definition.getClazz());
      logger.debug("Add annotation {}", definition.getClazz());
      if (null != definition.getCacheUsage()) {
        String region = (null == definition.getCacheRegion()) ? definition.getEntityName() : definition
            .getCacheRegion();
        configuration.setCacheConcurrencyStrategy(definition.getEntityName(), definition.getCacheUsage(),
            region, true);
      }
    }
    for (EntityPersistConfig.CollectionDefinition definition : epconfig.getCollections()) {
      if (null == definition.getCacheUsage()) continue;
      String role = epconfig.getEntity(definition.getClazz()).getEntityName() + "."
          + definition.getProperty();
      String region = (null == definition.getCacheRegion()) ? role : definition.getCacheRegion();
      configuration.setCollectionCacheConcurrencyStrategy(role, definition.getCacheUsage(), region);
    }
  }

  /**
   * Subclasses can override this method to perform custom initialization
   * of the Configuration instance used for SessionFactory creation.
   * The properties of this LocalSessionFactoryBean will be applied to
   * the Configuration object that gets returned here.
   * <p>
   * The default implementation creates a new Configuration instance. A custom implementation could
   * prepare the instance in a specific way, or use a custom Configuration subclass.
   * 
   * @return the Configuration instance
   * @throws HibernateException in case of Hibernate initialization errors
   * @see org.hibernate.cfg.Configuration#Configuration()
   */
  protected Configuration newConfiguration() throws HibernateException {
    return Reflections.newInstance(this.configurationClass);
  }

  public void destroy() throws HibernateException {
    logger.info("Closing Hibernate SessionFactory");
    this.sessionFactory.close();
  }

  public void setBeanClassLoader(ClassLoader classLoader) {
    this.beanClassLoader = classLoader;
  }

  public SessionFactory getObject() {
    return this.sessionFactory;
  }

  public Class<? extends SessionFactory> getObjectType() {
    return (this.sessionFactory != null ? this.sessionFactory.getClass() : SessionFactory.class);
  }

  public boolean isSingleton() {
    return true;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public SessionFactory getSessionFactory() {
    return sessionFactory;
  }

  public void setConfigurationClass(Class<? extends Configuration> configurationClass) {
    this.configurationClass = configurationClass;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public void setStaticHbm(Resource staticHbm) {
    this.staticHbm = staticHbm;
  }
}
