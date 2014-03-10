/*
 * Beangle, Agile Development Scaffold and Toolkit
 *
 * Copyright (c) 2005-2014, Beangle Software.
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
package org.beangle.inject.spring.config;

import static org.springframework.beans.factory.xml.BeanDefinitionParserDelegate.*;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanMetadataAttribute;
import org.springframework.beans.BeanMetadataAttributeAccessor;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanNameReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.parsing.BeanEntry;
import org.springframework.beans.factory.parsing.ConstructorArgumentEntry;
import org.springframework.beans.factory.parsing.ParseState;
import org.springframework.beans.factory.parsing.PropertyEntry;
import org.springframework.beans.factory.parsing.QualifierEntry;
import org.springframework.beans.factory.support.*;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * <p>
 * BeanDefinitionParser class.
 * </p>
 * 
 * @author chaostone
 * @version $Id: $
 */
class BeanDefinitionParser {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final ParseState parseState = new ParseState();

  /**
   * Stores all used bean names so we can enforce uniqueness on a per file
   * basis.
   */
  private final Set<String> usedNames = new HashSet<String>();

  /**
   * <p>
   * extractSource.
   * </p>
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @return a {@link java.lang.Object} object.
   */
  protected Object extractSource(Element ele) {
    return null;
  }

  /**
   * Report an error with the given message for the given source element.
   * 
   * @param message a {@link java.lang.String} object.
   * @param source a {@link org.w3c.dom.Node} object.
   */
  protected void error(String message, Node source) {
    logger.error(message);
  }

  /**
   * Report an error with the given message for the given source element.
   * 
   * @param message a {@link java.lang.String} object.
   * @param source a {@link org.w3c.dom.Element} object.
   */
  protected void error(String message, Element source) {
    logger.error(message);
  }

  /**
   * Report an error with the given message for the given source element.
   * 
   * @param message a {@link java.lang.String} object.
   * @param source a {@link org.w3c.dom.Element} object.
   * @param cause a {@link java.lang.Throwable} object.
   */
  protected void error(String message, Element source, Throwable cause) {
    logger.error(message);
  }

  /**
   * Parses the supplied <code>&lt;bean&gt;</code> element. May return <code>null</code> if there
   * were errors during parse. Errors are reported
   * to the {@link org.springframework.beans.factory.parsing.ProblemReporter}.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @return a {@link org.beangle.inject.spring.config.context.spring.ReconfigBeanDefinitionHolder}
   *         object.
   */
  public ReconfigBeanDefinitionHolder parseBeanDefinitionElement(Element ele) {
    return parseBeanDefinitionElement(ele, null);
  }

  /**
   * Parses the supplied <code>&lt;bean&gt;</code> element. May return <code>null</code> if there
   * were errors during parse. Errors are reported
   * to the {@link org.springframework.beans.factory.parsing.ProblemReporter}.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param containingBean a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link org.beangle.inject.spring.config.context.spring.ReconfigBeanDefinitionHolder}
   *         object.
   */
  public ReconfigBeanDefinitionHolder parseBeanDefinitionElement(Element ele, BeanDefinition containingBean) {
    String id = ele.getAttribute(ID_ATTRIBUTE);
    String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);

    List<String> aliases = new ArrayList<String>();
    if (StringUtils.hasLength(nameAttr)) {
      String[] nameArr = StringUtils.tokenizeToStringArray(nameAttr, MULTI_VALUE_ATTRIBUTE_DELIMITERS);
      aliases.addAll(Arrays.asList(nameArr));
    }

    String beanName = id;
    if (!StringUtils.hasText(beanName) && !aliases.isEmpty()) {
      beanName = aliases.remove(0);
      logger.debug("No XML 'id' specified - using '{}' as bean name and {} as aliases", beanName, aliases);
    }

    if (containingBean == null) checkNameUniqueness(beanName, aliases, ele);

    AbstractBeanDefinition beanDefinition = parseBeanDefinitionElement(ele, beanName, containingBean);
    if (beanDefinition != null) {
      String[] aliasesArray = StringUtils.toStringArray(aliases);
      ReconfigBeanDefinitionHolder bdh = new ReconfigBeanDefinitionHolder(beanDefinition, beanName,
          aliasesArray);
      String override = ele.getAttribute("override");
      if (null != override && override.equals("remove")) bdh.setConfigType(ReconfigType.REMOVE);
      String primary = ele.getAttribute("primary");
      if (null != primary && primary.equals("true")) bdh.setConfigType(ReconfigType.PRIMARY);
      return bdh;
    }

    return null;
  }

  /**
   * Validate that the specified bean name and aliases have not been used
   * already.
   * 
   * @param beanName a {@link java.lang.String} object.
   * @param aliases a {@link java.util.List} object.
   * @param beanElement a {@link org.w3c.dom.Element} object.
   */
  protected void checkNameUniqueness(String beanName, List<String> aliases, Element beanElement) {
    String foundName = null;

    if (StringUtils.hasText(beanName) && this.usedNames.contains(beanName)) foundName = beanName;

    if (foundName == null) foundName = (String) CollectionUtils.findFirstMatch(this.usedNames, aliases);

    if (foundName != null) error("Bean name '" + foundName + "' is already used in this file", beanElement);

    this.usedNames.add(beanName);
    this.usedNames.addAll(aliases);
  }

  /**
   * Parse the bean definition itself, without regard to name or aliases. May
   * return <code>null</code> if problems occured during the parse of the bean
   * definition.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param beanName a {@link java.lang.String} object.
   * @param containingBean a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link org.springframework.beans.factory.support.AbstractBeanDefinition} object.
   */
  public AbstractBeanDefinition parseBeanDefinitionElement(Element ele, String beanName,
      BeanDefinition containingBean) {

    this.parseState.push(new BeanEntry(beanName));

    String className = null;
    if (ele.hasAttribute(CLASS_ATTRIBUTE)) {
      className = ele.getAttribute(CLASS_ATTRIBUTE).trim();
    }

    try {
      String parent = null;
      if (ele.hasAttribute(PARENT_ATTRIBUTE)) {
        parent = ele.getAttribute(PARENT_ATTRIBUTE);
      }
      AbstractBeanDefinition bd = createBeanDefinition(className, parent);

      parseBeanDefinitionAttributes(ele, beanName, containingBean, bd);
      bd.setDescription(DomUtils.getChildElementValueByTagName(ele, DESCRIPTION_ELEMENT));

      parseMetaElements(ele, bd);
      parseLookupOverrideSubElements(ele, bd.getMethodOverrides());
      parseReplacedMethodSubElements(ele, bd.getMethodOverrides());

      parseConstructorArgElements(ele, bd);
      parsePropertyElements(ele, bd);
      parseQualifierElements(ele, bd);

      // bd.setResource(this.readerContext.getResource());
      bd.setSource(extractSource(ele));

      return bd;
    } catch (ClassNotFoundException ex) {
      error("Bean class [" + className + "] not found", ele, ex);
    } catch (NoClassDefFoundError err) {
      error("Class that bean class [" + className + "] depends on not found", ele, err);
    } catch (Throwable ex) {
      error("Unexpected failure during bean definition parsing", ele, ex);
    } finally {
      this.parseState.pop();
    }

    return null;
  }

  /**
   * Apply the attributes of the given bean element to the given bean *
   * definition.
   * 
   * @param ele bean declaration element
   * @param beanName bean name
   * @param containingBean containing bean definition
   * @return a bean definition initialized according to the bean element attributes
   * @param bd a {@link org.springframework.beans.factory.support.AbstractBeanDefinition} object.
   */
  public AbstractBeanDefinition parseBeanDefinitionAttributes(Element ele, String beanName,
      BeanDefinition containingBean, AbstractBeanDefinition bd) {

    if (ele.hasAttribute(SCOPE_ATTRIBUTE)) {
      // Spring 2.x "scope" attribute
      bd.setScope(ele.getAttribute(SCOPE_ATTRIBUTE));
      if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) error("Specify either 'scope' or 'singleton', not both", ele);

    } else if (ele.hasAttribute(SINGLETON_ATTRIBUTE)) {
      // Spring 1.x "singleton" attribute
      bd.setScope(TRUE_VALUE.equals(ele.getAttribute(SINGLETON_ATTRIBUTE)) ? BeanDefinition.SCOPE_SINGLETON
          : BeanDefinition.SCOPE_PROTOTYPE);
    } else if (containingBean != null) {
      // Take default from containing bean in case of an inner bean
      // definition.
      bd.setScope(containingBean.getScope());
    }

    if (ele.hasAttribute(ABSTRACT_ATTRIBUTE)) bd.setAbstract(TRUE_VALUE.equals(ele
        .getAttribute(ABSTRACT_ATTRIBUTE)));

    String lazyInit = ele.getAttribute(LAZY_INIT_ATTRIBUTE);
    bd.setLazyInit(TRUE_VALUE.equals(lazyInit));

    String autowire = ele.getAttribute(AUTOWIRE_ATTRIBUTE);
    bd.setAutowireMode(getAutowireMode(autowire));

    if (ele.hasAttribute(DEPENDS_ON_ATTRIBUTE)) {
      String dependsOn = ele.getAttribute(DEPENDS_ON_ATTRIBUTE);
      bd.setDependsOn(StringUtils.tokenizeToStringArray(dependsOn, MULTI_VALUE_ATTRIBUTE_DELIMITERS));
    }

    if (ele.hasAttribute(PRIMARY_ATTRIBUTE)) bd.setPrimary(TRUE_VALUE.equals(ele
        .getAttribute(PRIMARY_ATTRIBUTE)));

    if (ele.hasAttribute(INIT_METHOD_ATTRIBUTE)) {
      String initMethodName = ele.getAttribute(INIT_METHOD_ATTRIBUTE);
      if (!"".equals(initMethodName)) bd.setInitMethodName(initMethodName);
    }

    if (ele.hasAttribute(DESTROY_METHOD_ATTRIBUTE)) {
      String destroyMethodName = ele.getAttribute(DESTROY_METHOD_ATTRIBUTE);
      if (!"".equals(destroyMethodName)) bd.setDestroyMethodName(destroyMethodName);
    }

    if (ele.hasAttribute(FACTORY_METHOD_ATTRIBUTE)) bd.setFactoryMethodName(ele
        .getAttribute(FACTORY_METHOD_ATTRIBUTE));
    if (ele.hasAttribute(FACTORY_BEAN_ATTRIBUTE)) bd.setFactoryBeanName(ele
        .getAttribute(FACTORY_BEAN_ATTRIBUTE));

    return bd;
  }

  /**
   * Create a bean definition for the given class name and parent name.
   * 
   * @param className the name of the bean class
   * @param parentName the name of the bean's parent bean
   * @return the newly created bean definition
   * @throws java.lang.ClassNotFoundException
   *           if bean class resolution was attempted but failed
   */
  protected AbstractBeanDefinition createBeanDefinition(String className, String parentName)
      throws ClassNotFoundException {
    return BeanDefinitionReaderUtils.createBeanDefinition(parentName, className, null);
  }

  /**
   * <p>
   * parseMetaElements.
   * </p>
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param attributeAccessor a {@link org.springframework.beans.BeanMetadataAttributeAccessor}
   *          object.
   */
  public void parseMetaElements(Element ele, BeanMetadataAttributeAccessor attributeAccessor) {
    NodeList nl = ele.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, META_ELEMENT)) {
        Element metaElement = (Element) node;
        String key = metaElement.getAttribute(KEY_ATTRIBUTE);
        String value = metaElement.getAttribute(VALUE_ATTRIBUTE);
        BeanMetadataAttribute attribute = new BeanMetadataAttribute(key, value);
        attribute.setSource(extractSource(metaElement));
        attributeAccessor.addMetadataAttribute(attribute);
      }
    }
  }

  /**
   * <p>
   * getAutowireMode.
   * </p>
   * 
   * @param attValue a {@link java.lang.String} object.
   * @return a int.
   */
  public int getAutowireMode(String attValue) {
    String att = attValue;
    int autowire = AbstractBeanDefinition.AUTOWIRE_NO;
    if (AUTOWIRE_BY_NAME_VALUE.equals(att)) {
      autowire = AbstractBeanDefinition.AUTOWIRE_BY_NAME;
    } else if (AUTOWIRE_BY_TYPE_VALUE.equals(att)) {
      autowire = AbstractBeanDefinition.AUTOWIRE_BY_TYPE;
    } else if (AUTOWIRE_CONSTRUCTOR_VALUE.equals(att)) {
      autowire = AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR;
    }
    // Else leave default value.
    return autowire;
  }

  /**
   * Parse constructor-arg sub-elements of the given bean element.
   * 
   * @param beanEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   */
  public void parseConstructorArgElements(Element beanEle, BeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, CONSTRUCTOR_ARG_ELEMENT)) parseConstructorArgElement(
          (Element) node, bd);
    }
  }

  /**
   * Parse property sub-elements of the given bean element.
   * 
   * @param beanEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   */
  public void parsePropertyElements(Element beanEle, BeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, PROPERTY_ELEMENT)) parsePropertyElement(
          (Element) node, bd);
    }
  }

  /**
   * Parse qualifier sub-elements of the given bean element.
   * 
   * @param beanEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.support.AbstractBeanDefinition} object.
   */
  public void parseQualifierElements(Element beanEle, AbstractBeanDefinition bd) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, QUALIFIER_ELEMENT)) parseQualifierElement(
          (Element) node, bd);
    }
  }

  /**
   * Parse lookup-override sub-elements of the given bean element.
   * 
   * @param beanEle a {@link org.w3c.dom.Element} object.
   * @param overrides a {@link org.springframework.beans.factory.support.MethodOverrides} object.
   */
  public void parseLookupOverrideSubElements(Element beanEle, MethodOverrides overrides) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, LOOKUP_METHOD_ELEMENT)) {
        Element ele = (Element) node;
        String methodName = ele.getAttribute(NAME_ATTRIBUTE);
        String beanRef = ele.getAttribute(BEAN_ELEMENT);
        LookupOverride override = new LookupOverride(methodName, beanRef);
        override.setSource(extractSource(ele));
        overrides.addOverride(override);
      }
    }
  }

  /**
   * Parse replaced-method sub-elements of the given bean element.
   * 
   * @param beanEle a {@link org.w3c.dom.Element} object.
   * @param overrides a {@link org.springframework.beans.factory.support.MethodOverrides} object.
   */
  public void parseReplacedMethodSubElements(Element beanEle, MethodOverrides overrides) {
    NodeList nl = beanEle.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && nodeNameEquals(node, REPLACED_METHOD_ELEMENT)) {
        Element replacedMethodEle = (Element) node;
        String name = replacedMethodEle.getAttribute(NAME_ATTRIBUTE);
        String callback = replacedMethodEle.getAttribute(REPLACER_ATTRIBUTE);
        ReplaceOverride replaceOverride = new ReplaceOverride(name, callback);
        // Look for arg-type match elements.
        List<Element> argTypeEles = DomUtils.getChildElementsByTagName(replacedMethodEle, ARG_TYPE_ELEMENT);
        for (Element argTypeEle : argTypeEles) {
          replaceOverride.addTypeIdentifier(argTypeEle.getAttribute(ARG_TYPE_MATCH_ATTRIBUTE));
        }
        replaceOverride.setSource(extractSource(replacedMethodEle));
        overrides.addOverride(replaceOverride);
      }
    }
  }

  /**
   * Parse a constructor-arg element.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   */
  public void parseConstructorArgElement(Element ele, BeanDefinition bd) {
    String indexAttr = ele.getAttribute(INDEX_ATTRIBUTE);
    String typeAttr = ele.getAttribute(TYPE_ATTRIBUTE);
    String nameAttr = ele.getAttribute(NAME_ATTRIBUTE);
    if (StringUtils.hasLength(indexAttr)) {
      try {
        int index = Integer.parseInt(indexAttr);
        if (index < 0) {
          error("'index' cannot be lower than 0", ele);
        } else {
          try {
            this.parseState.push(new ConstructorArgumentEntry(index));
            Object value = parsePropertyValue(ele, bd, null);
            ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(
                value);
            if (StringUtils.hasLength(typeAttr)) valueHolder.setType(typeAttr);
            if (StringUtils.hasLength(nameAttr)) valueHolder.setName(nameAttr);
            valueHolder.setSource(extractSource(ele));
            if (bd.getConstructorArgumentValues().hasIndexedArgumentValue(index)) {
              error("Ambiguous constructor-arg entries for index " + index, ele);
            } else {
              bd.getConstructorArgumentValues().addIndexedArgumentValue(index, valueHolder);
            }
          } finally {
            this.parseState.pop();
          }
        }
      } catch (NumberFormatException ex) {
        error("Attribute 'index' of tag 'constructor-arg' must be an integer", ele);
      }
    } else {
      try {
        this.parseState.push(new ConstructorArgumentEntry());
        Object value = parsePropertyValue(ele, bd, null);
        ConstructorArgumentValues.ValueHolder valueHolder = new ConstructorArgumentValues.ValueHolder(value);
        if (StringUtils.hasLength(typeAttr)) valueHolder.setType(typeAttr);
        if (StringUtils.hasLength(nameAttr)) valueHolder.setName(nameAttr);
        valueHolder.setSource(extractSource(ele));
        bd.getConstructorArgumentValues().addGenericArgumentValue(valueHolder);
      } finally {
        this.parseState.pop();
      }
    }
  }

  /**
   * Parse a property element.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   */
  public void parsePropertyElement(Element ele, BeanDefinition bd) {
    String propertyName = ele.getAttribute(NAME_ATTRIBUTE);
    if (!StringUtils.hasLength(propertyName)) {
      error("Tag 'property' must have a 'name' attribute", ele);
      return;
    }
    this.parseState.push(new PropertyEntry(propertyName));
    try {
      if (bd.getPropertyValues().contains(propertyName)) {
        error("Multiple 'property' definitions for property '" + propertyName + "'", ele);
        return;
      }
      Object val = parsePropertyValue(ele, bd, propertyName);
      PropertyValue pv = new PropertyValue(propertyName, val);
      parseMetaElements(ele, pv);
      pv.setSource(extractSource(ele));
      bd.getPropertyValues().addPropertyValue(pv);
    } finally {
      this.parseState.pop();
    }
  }

  /**
   * Parse a qualifier element.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.support.AbstractBeanDefinition} object.
   */
  public void parseQualifierElement(Element ele, AbstractBeanDefinition bd) {
    String typeName = ele.getAttribute(TYPE_ATTRIBUTE);
    if (!StringUtils.hasLength(typeName)) {
      error("Tag 'qualifier' must have a 'type' attribute", ele);
      return;
    }
    this.parseState.push(new QualifierEntry(typeName));
    try {
      AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(typeName);
      qualifier.setSource(extractSource(ele));
      String value = ele.getAttribute(VALUE_ATTRIBUTE);
      if (StringUtils.hasLength(value)) {
        qualifier.setAttribute(AutowireCandidateQualifier.VALUE_KEY, value);
      }
      NodeList nl = ele.getChildNodes();
      for (int i = 0; i < nl.getLength(); i++) {
        Node node = nl.item(i);
        if (node instanceof Element && nodeNameEquals(node, QUALIFIER_ATTRIBUTE_ELEMENT)) {
          Element attributeEle = (Element) node;
          String attributeName = attributeEle.getAttribute(KEY_ATTRIBUTE);
          String attributeValue = attributeEle.getAttribute(VALUE_ATTRIBUTE);
          if (StringUtils.hasLength(attributeName) && StringUtils.hasLength(attributeValue)) {
            BeanMetadataAttribute attribute = new BeanMetadataAttribute(attributeName, attributeValue);
            attribute.setSource(extractSource(attributeEle));
            qualifier.addMetadataAttribute(attribute);
          } else {
            error("Qualifier 'attribute' tag must have a 'name' and 'value'", attributeEle);
            return;
          }
        }
      }
      bd.addQualifier(qualifier);
    } finally {
      this.parseState.pop();
    }
  }

  /**
   * Get the value of a property element. May be a list etc. Also used for
   * constructor arguments, "propertyName" being null in this case.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @param propertyName a {@link java.lang.String} object.
   * @return a {@link java.lang.Object} object.
   */
  public Object parsePropertyValue(Element ele, BeanDefinition bd, String propertyName) {
    String elementName = (propertyName != null) ? "<property> element for property '" + propertyName + "'"
        : "<constructor-arg> element";

    // Should only have one child element: ref, value, list, etc.
    NodeList nl = ele.getChildNodes();
    Element subElement = null;
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)
          && !nodeNameEquals(node, META_ELEMENT)) {
        // Child element is what we're looking for.
        if (subElement != null) error(elementName + " must not contain more than one sub-element", ele);
        else subElement = (Element) node;
      }
    }

    boolean hasRefAttribute = ele.hasAttribute(REF_ATTRIBUTE);
    boolean hasValueAttribute = ele.hasAttribute(VALUE_ATTRIBUTE);
    if ((hasRefAttribute && hasValueAttribute)
        || ((hasRefAttribute || hasValueAttribute) && subElement != null)) {
      error(elementName
          + " is only allowed to contain either 'ref' attribute OR 'value' attribute OR sub-element", ele);
    }

    if (hasRefAttribute) {
      String refName = ele.getAttribute(REF_ATTRIBUTE);
      if (!StringUtils.hasText(refName)) error(elementName + " contains empty 'ref' attribute", ele);

      RuntimeBeanReference ref = new RuntimeBeanReference(refName);
      ref.setSource(extractSource(ele));
      return ref;
    } else if (hasValueAttribute) {
      TypedStringValue valueHolder = new TypedStringValue(ele.getAttribute(VALUE_ATTRIBUTE));
      valueHolder.setSource(extractSource(ele));
      return valueHolder;
    } else if (subElement != null) {
      return parsePropertySubElement(subElement, bd);
    } else {
      // Neither child element nor "ref" or "value" attribute found.
      error(elementName + " must specify a ref or value", ele);
      return null;
    }
  }

  /**
   * <p>
   * parsePropertySubElement.
   * </p>
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link java.lang.Object} object.
   */
  public Object parsePropertySubElement(Element ele, BeanDefinition bd) {
    return parsePropertySubElement(ele, bd, null);
  }

  /**
   * Parse a value, ref or collection sub-element of a property or
   * constructor-arg element.
   * 
   * @param ele subelement of property element; we don't know which yet
   * @param defaultValueType the default type (class name) for any <code>&lt;value&gt;</code> tag
   *          that might be created
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link java.lang.Object} object.
   */
  public Object parsePropertySubElement(Element ele, BeanDefinition bd, String defaultValueType) {
    if (!isDefaultNamespace(getNamespaceURI(ele))) {
      error("Cannot support nested element .", ele);
      return null;
    } else if (nodeNameEquals(ele, BEAN_ELEMENT)) {
      BeanDefinitionHolder nestedBd = parseBeanDefinitionElement(ele, bd);
      if (nestedBd != null) nestedBd = decorateBeanDefinitionIfRequired(ele, nestedBd, bd);
      return nestedBd;
    } else if (nodeNameEquals(ele, REF_ELEMENT)) {
      // A generic reference to any name of any bean.
      String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
      boolean toParent = false;
      if (!StringUtils.hasLength(refName)) {
        // A reference to the id of another bean in the same XML file.
        refName = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
        if (!StringUtils.hasLength(refName)) {
          // A reference to the id of another bean in a parent context.
          refName = ele.getAttribute(PARENT_REF_ATTRIBUTE);
          toParent = true;
          if (!StringUtils.hasLength(refName)) {
            error("'bean', 'local' or 'parent' is required for <ref> element", ele);
            return null;
          }
        }
      }
      if (!StringUtils.hasText(refName)) {
        error("<ref> element contains empty target attribute", ele);
        return null;
      }
      RuntimeBeanReference ref = new RuntimeBeanReference(refName, toParent);
      ref.setSource(extractSource(ele));
      return ref;
    } else if (nodeNameEquals(ele, IDREF_ELEMENT)) {
      return parseIdRefElement(ele);
    } else if (nodeNameEquals(ele, VALUE_ELEMENT)) {
      return parseValueElement(ele, defaultValueType);
    } else if (nodeNameEquals(ele, NULL_ELEMENT)) {
      // It's a distinguished null value. Let's wrap it in a
      // TypedStringValue
      // object in order to preserve the source location.
      TypedStringValue nullHolder = new TypedStringValue(null);
      nullHolder.setSource(extractSource(ele));
      return nullHolder;
    } else if (nodeNameEquals(ele, ARRAY_ELEMENT)) {
      return parseArrayElement(ele, bd);
    } else if (nodeNameEquals(ele, LIST_ELEMENT)) {
      return parseListElement(ele, bd);
    } else if (nodeNameEquals(ele, SET_ELEMENT)) {
      return parseSetElement(ele, bd);
    } else if (nodeNameEquals(ele, MAP_ELEMENT)) {
      return parseMapElement(ele, bd);
    } else if (nodeNameEquals(ele, PROPS_ELEMENT)) {
      return parsePropsElement(ele);
    } else {
      error("Unknown property sub-element: [" + ele.getNodeName() + "]", ele);
      return null;
    }
  }

  /**
   * Return a typed String value Object for the given 'idref' element.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @return a {@link java.lang.Object} object.
   */
  public Object parseIdRefElement(Element ele) {
    // A generic reference to any name of any bean.
    String refName = ele.getAttribute(BEAN_REF_ATTRIBUTE);
    if (!StringUtils.hasLength(refName)) {
      // A reference to the id of another bean in the same XML file.
      refName = ele.getAttribute(LOCAL_REF_ATTRIBUTE);
      if (!StringUtils.hasLength(refName)) {
        error("Either 'bean' or 'local' is required for <idref> element", ele);
        return null;
      }
    }
    if (!StringUtils.hasText(refName)) {
      error("<idref> element contains empty target attribute", ele);
      return null;
    }
    RuntimeBeanNameReference ref = new RuntimeBeanNameReference(refName);
    ref.setSource(extractSource(ele));
    return ref;
  }

  /**
   * Return a typed String value Object for the given value element.
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param defaultTypeName a {@link java.lang.String} object.
   * @return a {@link java.lang.Object} object.
   */
  public Object parseValueElement(Element ele, String defaultTypeName) {
    // It's a literal value.
    String value = DomUtils.getTextValue(ele);
    String specifiedTypeName = ele.getAttribute(TYPE_ATTRIBUTE);
    String typeName = specifiedTypeName;
    if (!StringUtils.hasText(typeName)) typeName = defaultTypeName;
    try {
      TypedStringValue typedValue = buildTypedStringValue(value, typeName);
      typedValue.setSource(extractSource(ele));
      typedValue.setSpecifiedTypeName(specifiedTypeName);
      return typedValue;
    } catch (ClassNotFoundException ex) {
      error("Type class [" + typeName + "] not found for <value> element", ele, ex);
      return value;
    }
  }

  /**
   * Build a typed String value Object for the given raw value.
   * 
   * @see org.springframework.beans.factory.config.TypedStringValue
   * @param value a {@link java.lang.String} object.
   * @param targetTypeName a {@link java.lang.String} object.
   * @return a {@link org.springframework.beans.factory.config.TypedStringValue} object.
   * @throws java.lang.ClassNotFoundException if any.
   */
  protected TypedStringValue buildTypedStringValue(String value, String targetTypeName)
      throws ClassNotFoundException {
    TypedStringValue typedValue;
    if (!StringUtils.hasText(targetTypeName)) typedValue = new TypedStringValue(value);
    else typedValue = new TypedStringValue(value, targetTypeName);
    return typedValue;
  }

  /**
   * Parse an array element.
   * 
   * @param arrayEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link java.lang.Object} object.
   */
  public Object parseArrayElement(Element arrayEle, BeanDefinition bd) {
    String elementType = arrayEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
    NodeList nl = arrayEle.getChildNodes();
    ManagedArray target = new ManagedArray(elementType, nl.getLength());
    target.setSource(extractSource(arrayEle));
    target.setElementTypeName(elementType);
    target.setMergeEnabled(parseMergeAttribute(arrayEle));
    parseCollectionElements(nl, target, bd, elementType);
    return target;
  }

  /**
   * Parse a list element.
   * 
   * @param collectionEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link java.util.List} object.
   */
  public List<Object> parseListElement(Element collectionEle, BeanDefinition bd) {
    String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
    NodeList nl = collectionEle.getChildNodes();
    ManagedList<Object> target = new ManagedList<Object>(nl.getLength());
    target.setSource(extractSource(collectionEle));
    target.setElementTypeName(defaultElementType);
    target.setMergeEnabled(parseMergeAttribute(collectionEle));
    parseCollectionElements(nl, target, bd, defaultElementType);
    return target;
  }

  /**
   * Parse a set element.
   * 
   * @param collectionEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link java.util.Set} object.
   */
  public Set<Object> parseSetElement(Element collectionEle, BeanDefinition bd) {
    String defaultElementType = collectionEle.getAttribute(VALUE_TYPE_ATTRIBUTE);
    NodeList nl = collectionEle.getChildNodes();
    ManagedSet<Object> target = new ManagedSet<Object>(nl.getLength());
    target.setSource(extractSource(collectionEle));
    target.setElementTypeName(defaultElementType);
    target.setMergeEnabled(parseMergeAttribute(collectionEle));
    parseCollectionElements(nl, target, bd, defaultElementType);
    return target;
  }

  /**
   * <p>
   * parseCollectionElements.
   * </p>
   * 
   * @param elementNodes a {@link org.w3c.dom.NodeList} object.
   * @param target a {@link java.util.Collection} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @param defaultElementType a {@link java.lang.String} object.
   */
  protected void parseCollectionElements(NodeList elementNodes, Collection<Object> target, BeanDefinition bd,
      String defaultElementType) {
    for (int i = 0; i < elementNodes.getLength(); i++) {
      Node node = elementNodes.item(i);
      if (node instanceof Element && !nodeNameEquals(node, DESCRIPTION_ELEMENT)) target
          .add(parsePropertySubElement((Element) node, bd, defaultElementType));
    }
  }

  /**
   * Parse a map element.
   * 
   * @param mapEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link java.util.Map} object.
   */
  public Map<Object, Object> parseMapElement(Element mapEle, BeanDefinition bd) {
    String defaultKeyType = mapEle.getAttribute(KEY_TYPE_ATTRIBUTE);
    String defaultValueType = mapEle.getAttribute(VALUE_TYPE_ATTRIBUTE);

    List<Element> entryEles = DomUtils.getChildElementsByTagName(mapEle, ENTRY_ELEMENT);
    ManagedMap<Object, Object> map = new ManagedMap<Object, Object>(entryEles.size());
    map.setSource(extractSource(mapEle));
    map.setKeyTypeName(defaultKeyType);
    map.setValueTypeName(defaultValueType);
    map.setMergeEnabled(parseMergeAttribute(mapEle));

    for (Element entryEle : entryEles) {
      // Should only have one value child element: ref, value, list, etc.
      // Optionally, there might be a key child element.
      NodeList entrySubNodes = entryEle.getChildNodes();
      Element keyEle = null;
      Element valueEle = null;
      for (int j = 0; j < entrySubNodes.getLength(); j++) {
        Node node = entrySubNodes.item(j);
        if (node instanceof Element) {
          Element candidateEle = (Element) node;
          if (nodeNameEquals(candidateEle, KEY_ELEMENT)) {
            if (keyEle != null) error("<entry> element is only allowed to contain one <key> sub-element",
                entryEle);
            else keyEle = candidateEle;
          } else {
            // Child element is what we're looking for.
            if (valueEle != null) error("<entry> element must not contain more than one value sub-element",
                entryEle);
            else valueEle = candidateEle;
          }
        }
      }

      // Extract key from attribute or sub-element.
      Object key = null;
      boolean hasKeyAttribute = entryEle.hasAttribute(KEY_ATTRIBUTE);
      boolean hasKeyRefAttribute = entryEle.hasAttribute(KEY_REF_ATTRIBUTE);
      if ((hasKeyAttribute && hasKeyRefAttribute) || ((hasKeyAttribute || hasKeyRefAttribute))
          && keyEle != null) {
        error("<entry> element is only allowed to contain either "
            + "a 'key' attribute OR a 'key-ref' attribute OR a <key> sub-element", entryEle);
      }
      if (hasKeyAttribute) {
        key = buildTypedStringValueForMap(entryEle.getAttribute(KEY_ATTRIBUTE), defaultKeyType, entryEle);
      } else if (hasKeyRefAttribute) {
        String refName = entryEle.getAttribute(KEY_REF_ATTRIBUTE);
        if (!StringUtils.hasText(refName)) error("<entry> element contains empty 'key-ref' attribute",
            entryEle);

        RuntimeBeanReference ref = new RuntimeBeanReference(refName);
        ref.setSource(extractSource(entryEle));
        key = ref;
      } else if (keyEle != null) {
        key = parseKeyElement(keyEle, bd, defaultKeyType);
      } else {
        error("<entry> element must specify a key", entryEle);
      }

      // Extract value from attribute or sub-element.
      Object value = null;
      boolean hasValueAttribute = entryEle.hasAttribute(VALUE_ATTRIBUTE);
      boolean hasValueRefAttribute = entryEle.hasAttribute(VALUE_REF_ATTRIBUTE);
      if ((hasValueAttribute && hasValueRefAttribute) || ((hasValueAttribute || hasValueRefAttribute))
          && valueEle != null) {
        error("<entry> element is only allowed to contain either "
            + "'value' attribute OR 'value-ref' attribute OR <value> sub-element", entryEle);
      }
      if (hasValueAttribute) {
        value = buildTypedStringValueForMap(entryEle.getAttribute(VALUE_ATTRIBUTE), defaultValueType,
            entryEle);
      } else if (hasValueRefAttribute) {
        String refName = entryEle.getAttribute(VALUE_REF_ATTRIBUTE);
        if (!StringUtils.hasText(refName)) {
          error("<entry> element contains empty 'value-ref' attribute", entryEle);
        }
        RuntimeBeanReference ref = new RuntimeBeanReference(refName);
        ref.setSource(extractSource(entryEle));
        value = ref;
      } else if (valueEle != null) {
        value = parsePropertySubElement(valueEle, bd, defaultValueType);
      } else {
        error("<entry> element must specify a value", entryEle);
      }

      // Add final key and value to the Map.
      map.put(key, value);
    }

    return map;
  }

  /**
   * Build a typed String value Object for the given raw value.
   * 
   * @see org.springframework.beans.factory.config.TypedStringValue
   * @param value a {@link java.lang.String} object.
   * @param defaultTypeName a {@link java.lang.String} object.
   * @param entryEle a {@link org.w3c.dom.Element} object.
   * @return a {@link java.lang.Object} object.
   */
  protected final Object buildTypedStringValueForMap(String value, String defaultTypeName, Element entryEle) {
    try {
      TypedStringValue typedValue = buildTypedStringValue(value, defaultTypeName);
      typedValue.setSource(extractSource(entryEle));
      return typedValue;
    } catch (ClassNotFoundException ex) {
      error("Type class [" + defaultTypeName + "] not found for Map key/value type", entryEle, ex);
      return value;
    }
  }

  /**
   * Parse a key sub-element of a map element.
   * 
   * @param keyEle a {@link org.w3c.dom.Element} object.
   * @param bd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @param defaultKeyTypeName a {@link java.lang.String} object.
   * @return a {@link java.lang.Object} object.
   */
  protected Object parseKeyElement(Element keyEle, BeanDefinition bd, String defaultKeyTypeName) {
    NodeList nl = keyEle.getChildNodes();
    Element subElement = null;
    for (int i = 0; i < nl.getLength(); i++) {
      Node node = nl.item(i);
      if (node instanceof Element) {
        // Child element is what we're looking for.
        if (subElement != null) error("<key> element must not contain more than one value sub-element",
            keyEle);
        else subElement = (Element) node;
      }
    }
    return parsePropertySubElement(subElement, bd, defaultKeyTypeName);
  }

  /**
   * Parse a props element.
   * 
   * @param propsEle a {@link org.w3c.dom.Element} object.
   * @return a {@link java.util.Properties} object.
   */
  public Properties parsePropsElement(Element propsEle) {
    ManagedProperties props = new ManagedProperties();
    props.setSource(extractSource(propsEle));
    props.setMergeEnabled(parseMergeAttribute(propsEle));

    List<Element> propEles = DomUtils.getChildElementsByTagName(propsEle, PROP_ELEMENT);
    for (Element propEle : propEles) {
      String key = propEle.getAttribute(KEY_ATTRIBUTE);
      // Trim the text value to avoid unwanted whitespace
      // caused by typical XML formatting.
      String value = DomUtils.getTextValue(propEle).trim();
      TypedStringValue keyHolder = new TypedStringValue(key);
      keyHolder.setSource(extractSource(propEle));
      TypedStringValue valueHolder = new TypedStringValue(value);
      valueHolder.setSource(extractSource(propEle));
      props.put(keyHolder, valueHolder);
    }

    return props;
  }

  /**
   * Parse the merge attribute of a collection element, if any.
   * 
   * @param collectionElement a {@link org.w3c.dom.Element} object.
   * @return a boolean.
   */
  public boolean parseMergeAttribute(Element collectionElement) {
    String value = collectionElement.getAttribute(MERGE_ATTRIBUTE);
    return TRUE_VALUE.equals(value);
  }

  /**
   * <p>
   * decorateBeanDefinitionIfRequired.
   * </p>
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param definitionHolder a {@link org.springframework.beans.factory.config.BeanDefinitionHolder}
   *          object.
   * @return a {@link org.springframework.beans.factory.config.BeanDefinitionHolder} object.
   */
  public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele,
      BeanDefinitionHolder definitionHolder) {
    return decorateBeanDefinitionIfRequired(ele, definitionHolder, null);
  }

  /**
   * <p>
   * decorateBeanDefinitionIfRequired.
   * </p>
   * 
   * @param ele a {@link org.w3c.dom.Element} object.
   * @param definitionHolder a {@link org.springframework.beans.factory.config.BeanDefinitionHolder}
   *          object.
   * @param containingBd a {@link org.springframework.beans.factory.config.BeanDefinition} object.
   * @return a {@link org.springframework.beans.factory.config.BeanDefinitionHolder} object.
   */
  public BeanDefinitionHolder decorateBeanDefinitionIfRequired(Element ele,
      BeanDefinitionHolder definitionHolder, BeanDefinition containingBd) {

    BeanDefinitionHolder finalDefinition = definitionHolder;

    // Decorate based on custom attributes first.
    NamedNodeMap attributes = ele.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      Node node = attributes.item(i);
      finalDefinition = decorateIfRequired(node, finalDefinition, containingBd);
    }

    // Decorate based on custom nested elements.
    NodeList children = ele.getChildNodes();
    for (int i = 0; i < children.getLength(); i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) finalDefinition = decorateIfRequired(node,
          finalDefinition, containingBd);
    }
    return finalDefinition;
  }

  private BeanDefinitionHolder decorateIfRequired(Node node, BeanDefinitionHolder originalDef,
      BeanDefinition containingBd) {
    return originalDef;
  }

  public boolean isDefaultNamespace(String namespaceUri) {
    return (!StringUtils.hasLength(namespaceUri) || BEANS_NAMESPACE_URI.equals(namespaceUri));
  }

  private String getNamespaceURI(Node node) {
    return node.getNamespaceURI();
  }

  private boolean nodeNameEquals(Node node, String desiredName) {
    return desiredName.equals(node.getNodeName()) || desiredName.equals(getLocalName(node));
  }

  public String getLocalName(Node node) {
    return node.getLocalName();
  }
}
