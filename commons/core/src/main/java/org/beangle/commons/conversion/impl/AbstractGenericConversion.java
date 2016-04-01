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
package org.beangle.commons.conversion.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.beangle.commons.collection.CollectUtils;
import org.beangle.commons.lang.Objects;
import org.beangle.commons.lang.Primitives;
import org.beangle.commons.conversion.Conversion;
import org.beangle.commons.conversion.Converter;
import org.beangle.commons.conversion.ConverterRegistry;
import org.beangle.commons.lang.tuple.Pair;

/**
 * Generic Conversion Super class
 * It provider converter registry and converter search machanism.
 * 
 * @author chaostone
 * @since 3.2.0
 */
public abstract class AbstractGenericConversion implements Conversion, ConverterRegistry {

  Map<Class<?>, Map<Class<?>, GenericConverter>> converters = CollectUtils.newHashMap();

  Map<Pair<Class<?>, Class<?>>, GenericConverter> cache = CollectUtils.newConcurrentHashMap();

  protected void addConverter(GenericConverter converter) {
    Pair<?, ?> key = converter.getTypeinfo();
    getOrCreateConverters((Class<?>) key.getLeft()).put((Class<?>) key.getRight(), converter);
    cache.clear();
  }

  @Override
  public void addConverter(Converter<?, ?> converter) {
    Pair<Class<?>, Class<?>> key = null;
    Pair<Class<Object>, Class<Object>> defaultKey = Pair.of(Object.class, Object.class);
    for (Method m : converter.getClass().getMethods()) {
      if (m.getName().equals("apply") && Modifier.isPublic(m.getModifiers())) {
        key = Pair.<Class<?>, Class<?>> of(m.getParameterTypes()[0], m.getReturnType());
        if (!key.equals(defaultKey)) break;
      }
    }
    if (null == key) throw new IllegalArgumentException("Cannot find convert type pair "
        + converter.getClass());

    getOrCreateConverters((Class<?>) key.getLeft()).put((Class<?>) key.getRight(),
        new ConverterAdapter(converter, key));
    cache.clear();
  }

  private Map<Class<?>, GenericConverter> getOrCreateConverters(Class<?> sourceType) {
    Map<Class<?>, GenericConverter> exists = converters.get(sourceType);
    if (null == exists) {
      exists = CollectUtils.newHashMap();
      converters.put(sourceType, exists);
    }
    return exists;
  }

  private Map<Class<?>, GenericConverter> getConverters(Class<?> sourceType) {
    Map<Class<?>, GenericConverter> exists = converters.get(sourceType);
    if (null == exists) return Collections.emptyMap();
    else return exists;
  }

  private GenericConverter getConverter(Class<?> targetType, Map<Class<?>, GenericConverter> converters) {
    Set<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
    LinkedList<Class<?>> queue = new LinkedList<Class<?>>();
    queue.addFirst(targetType);
    while (!queue.isEmpty()) {
      Class<?> cur = queue.removeLast();
      GenericConverter converter = converters.get(cur);
      if (converter != null) return converter;

      Class<?> superClass = cur.getSuperclass();
      if (superClass != null && superClass != Object.class) queue.addFirst(superClass);

      for (Class<?> interfaceType : cur.getInterfaces())
        addInterfaces(interfaceType, interfaces);
    }

    for (Class<?> interfaceType : interfaces) {
      GenericConverter converter = converters.get(interfaceType);
      if (converter != null) return converter;
    }

    return null;
  }

  private void addInterfaces(Class<?> interfaceType, Set<Class<?>> interfaces) {
    interfaces.add(interfaceType);
    for (Class<?> inheritedInterface : interfaceType.getInterfaces())
      addInterfaces(inheritedInterface, interfaces);
  }

  @SuppressWarnings("unchecked")
  protected GenericConverter findConverter(Class<?> sourceType, Class<?> targetType) {
    // Get cache
    Pair<?, ?> key = Pair.of(sourceType, targetType);
    GenericConverter converter = cache.get(key);
    if (null == converter) converter = searchConverter(sourceType, targetType);
    else return converter;

    if (null == converter) converter = NoneConverter.Instance;
    else cache.put((Pair<Class<?>, Class<?>>) key, converter);
    return converter;
  }

  protected GenericConverter searchConverter(Class<?> sourceType, Class<?> targetType) {
    HashSet<Class<?>> interfaces = new LinkedHashSet<Class<?>>();
    LinkedList<Class<?>> classQueue = new LinkedList<Class<?>>();
    classQueue.addFirst(sourceType);
    // Search Class hierarchy
    while (!classQueue.isEmpty()) {
      Class<?> currentClass = classQueue.removeLast();
      GenericConverter converter = getConverter(targetType, getConverters(currentClass));
      if (converter != null) return converter;

      Class<?> superClass = currentClass.getSuperclass();
      if (superClass != null && superClass != Object.class) classQueue.addFirst(superClass);

      for (Class<?> interfaceType : currentClass.getInterfaces())
        addInterfaces(interfaceType, interfaces);
    }
    // Search interface
    for (Class<?> interfaceType : interfaces) {
      GenericConverter converter = getConverter(targetType, getConverters(interfaceType));
      if (converter != null) return converter;
    }
    return getConverter(targetType, getConverters(Object.class));
  }

  /**
   * Convert to target type.
   */
  @SuppressWarnings("unchecked")
  @Override
  public <T> T convert(Object source, Class<T> targetType) {
    if (null == source) return (T) Objects.defaultValue(targetType);

    Class<?> sourceType = Primitives.wrap(source.getClass());
    Class<?> targetClazz = Primitives.wrap(targetType);

    if (targetClazz.isAssignableFrom(sourceType)) return (T) source;

    if (sourceType.isArray() && targetClazz.isArray()) {
      Class<?> sourceObjType = Primitives.wrap(sourceType.getComponentType());
      Class<?> targetObjType = Primitives.wrap(targetClazz.getComponentType());
      GenericConverter converter = findConverter(sourceObjType, targetObjType);
      if (null == converter) return (T) Array.newInstance(targetClazz.getComponentType(), 0);
      else {
        int length = Array.getLength(source);
        T result = (T) Array.newInstance(targetClazz.getComponentType(), length);
        for (int i = 0; i < length; i++)
          Array.set(result, i, converter.convert(Array.get(source, i), sourceObjType, targetObjType));
        return result;
      }
    } else {
      Object rs = null;
      GenericConverter converter = findConverter(sourceType, targetClazz);
      if (null != converter) rs = converter.convert(source, sourceType, targetClazz);
      if (null == rs && targetType.isPrimitive()) rs = Objects.defaultValue(targetType);
      return (T) rs;
    }
  }

}
