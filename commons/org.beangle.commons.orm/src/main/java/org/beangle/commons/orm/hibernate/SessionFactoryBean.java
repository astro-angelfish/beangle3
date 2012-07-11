/* Copyright c 2005-2012.
 * Licensed under GNU  LESSER General Public License, Version 3.
 * http://www.gnu.org/licenses
 */
package org.beangle.commons.orm.hibernate;

import java.io.File;
import java.util.Properties;

import javax.sql.DataSource;

import org.beangle.commons.bean.Disposable;
import org.beangle.commons.bean.Initializing;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.cfg.NamingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ClassUtils;

/**
 * @author chaostone
 * @version $Id: SessionFactoryBean.java Feb 27, 2012 10:52:27 PM chaostone $
 */
public class SessionFactoryBean implements FactoryBean<SessionFactory>, Initializing, Disposable,
    BeanClassLoaderAware {

  private static final ThreadLocal<DataSource> configTimeDataSourceHolder = new ThreadLocal<DataSource>();

  protected Logger logger = LoggerFactory.getLogger(getClass());

  private Class<? extends Configuration> configurationClass = Configuration.class;

  private DataSource dataSource;

  private Resource[] configLocations;

  private String[] mappingResources;

  private Resource[] mappingLocations;

  private Resource[] cacheableMappingLocations;

  private Resource[] mappingJarLocations;

  private Resource[] mappingDirectoryLocations;

  private NamingStrategy namingStrategy;

  private Properties hibernateProperties;

  private SessionFactory sessionFactory;

  private ClassLoader beanClassLoader = ClassUtils.getDefaultClassLoader();

  private Configuration configuration;

  /**
   * Set the location of a single Hibernate XML config file, for example as
   * classpath resource "classpath:hibernate.cfg.xml".
   * <p>
   * Note: Can be omitted when all necessary properties and mapping resources are specified locally
   * via this bean.
   * 
   * @see org.hibernate.cfg.Configuration#configure(java.net.URL)
   */
  public void setConfigLocation(Resource configLocation) {
    this.configLocations = new Resource[] { configLocation };
  }

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

  /**
   * Set Hibernate mapping resources to be found in the class path,
   * like "example.hbm.xml" or "mypackage/example.hbm.xml".
   * Analogous to mapping entries in a Hibernate XML config file.
   * Alternative to the more generic setMappingLocations method.
   * <p>
   * Can be used to add to mappings from a Hibernate XML config file, or to specify all mappings
   * locally.
   * 
   * @see #setMappingLocations
   * @see org.hibernate.cfg.Configuration#addResource
   */
  public void setMappingResources(String[] mappingResources) {
    this.mappingResources = mappingResources;
  }

  /**
   * Set locations of Hibernate mapping files, for example as classpath
   * resource "classpath:example.hbm.xml". Supports any resource location
   * via Spring's resource abstraction, for example relative paths like
   * "WEB-INF/mappings/example.hbm.xml" when running in an application context.
   * <p>
   * Can be used to add to mappings from a Hibernate XML config file, or to specify all mappings
   * locally.
   * 
   * @see org.hibernate.cfg.Configuration#addInputStream
   */
  public void setMappingLocations(Resource[] mappingLocations) {
    this.mappingLocations = mappingLocations;
  }

  /**
   * Set locations of cacheable Hibernate mapping files, for example as web app
   * resource "/WEB-INF/mapping/example.hbm.xml". Supports any resource location
   * via Spring's resource abstraction, as long as the resource can be resolved
   * in the file system.
   * <p>
   * Can be used to add to mappings from a Hibernate XML config file, or to specify all mappings
   * locally.
   * 
   * @see org.hibernate.cfg.Configuration#addCacheableFile(java.io.File)
   */
  public void setCacheableMappingLocations(Resource[] cacheableMappingLocations) {
    this.cacheableMappingLocations = cacheableMappingLocations;
  }

  /**
   * Set locations of jar files that contain Hibernate mapping resources,
   * like "WEB-INF/lib/example.hbm.jar".
   * <p>
   * Can be used to add to mappings from a Hibernate XML config file, or to specify all mappings
   * locally.
   * 
   * @see org.hibernate.cfg.Configuration#addJar(java.io.File)
   */
  public void setMappingJarLocations(Resource[] mappingJarLocations) {
    this.mappingJarLocations = mappingJarLocations;
  }

  /**
   * Set locations of directories that contain Hibernate mapping resources,
   * like "WEB-INF/mappings".
   * <p>
   * Can be used to add to mappings from a Hibernate XML config file, or to specify all mappings
   * locally.
   * 
   * @see org.hibernate.cfg.Configuration#addDirectory(java.io.File)
   */
  public void setMappingDirectoryLocations(Resource[] mappingDirectoryLocations) {
    this.mappingDirectoryLocations = mappingDirectoryLocations;
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
    if (this.hibernateProperties == null) {
      this.hibernateProperties = new Properties();
    }
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

  public void init() throws Exception {
    if (dataSource != null) {
      configTimeDataSourceHolder.set(dataSource);
    }
    configuration = newConfiguration();
    //
    // if (this.lobHandler != null) {
    // // Make given LobHandler available for SessionFactory configuration.
    // // Do early because because mapping resource might refer to custom types.
    // configTimeLobHandlerHolder.set(this.lobHandler);
    // }

    // Analogous to Hibernate EntityManager's Ejb3Configuration:
    // Hibernate doesn't allow setting the bean ClassLoader explicitly,
    // so we need to expose it as thread context ClassLoader accordingly.
    Thread currentThread = Thread.currentThread();
    ClassLoader threadContextClassLoader = currentThread.getContextClassLoader();
    boolean overrideClassLoader = (this.beanClassLoader != null && !this.beanClassLoader
        .equals(threadContextClassLoader));
    if (overrideClassLoader) {
      currentThread.setContextClassLoader(this.beanClassLoader);
    }

    try {
      // Set Hibernate 3.1+ CurrentSessionContext implementation,
      // providing the Beangle-managed Session as current Session.
      // Can be overridden by a custom value for the corresponding Hibernate property.
      configuration.setProperty(Environment.CURRENT_SESSION_CONTEXT_CLASS,
          BeangleSessionContext.class.getName());
      configuration.setProperty(Environment.TRANSACTION_STRATEGY, BeangleTransactionFactory.class.getName());

      if (this.namingStrategy != null) configuration.setNamingStrategy(this.namingStrategy);

      if (this.configLocations != null) {
        for (Resource resource : this.configLocations)
          configuration.configure(resource.getURL());
      }

      if (this.hibernateProperties != null) configuration.addProperties(this.hibernateProperties);

      if (dataSource != null) {
        configuration.setProperty(Environment.CONNECTION_PROVIDER,
            DataSourceConnectionProvider.class.getName());
      }

      if (this.mappingResources != null) {
        for (String mapping : this.mappingResources) {
          Resource resource = new ClassPathResource(mapping.trim(), this.beanClassLoader);
          configuration.addInputStream(resource.getInputStream());
        }
      }

      if (this.mappingLocations != null) {
        for (Resource resource : this.mappingLocations)
          configuration.addInputStream(resource.getInputStream());
      }

      if (this.cacheableMappingLocations != null) {
        for (Resource resource : this.cacheableMappingLocations)
          configuration.addCacheableFile(resource.getFile());
      }

      if (this.mappingJarLocations != null) {
        for (Resource resource : this.mappingJarLocations)
          configuration.addJar(resource.getFile());
      }

      if (this.mappingDirectoryLocations != null) {
        for (Resource resource : this.mappingDirectoryLocations) {
          File file = resource.getFile();
          if (!file.isDirectory()) { throw new IllegalArgumentException("Mapping directory location ["
              + resource + "] does not denote a directory"); }
          configuration.addDirectory(file);
        }
      }
      logger.info("Building new Hibernate SessionFactory");
      this.sessionFactory = configuration.buildSessionFactory();
    }

    finally {
      if (dataSource != null) {
        configTimeDataSourceHolder.remove();
      }
      if (overrideClassLoader) {
        // Reset original thread context ClassLoader.
        currentThread.setContextClassLoader(threadContextClassLoader);
      }
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
    return BeanUtils.instantiateClass(this.configurationClass);
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

  public static DataSource getConfigTimeDataSource() {
    return configTimeDataSourceHolder.get();
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

}
