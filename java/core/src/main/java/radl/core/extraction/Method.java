/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import radl.common.StringUtil;


/**
 * Information about a method in RADL.
 */
public class Method implements Comparable<Method>, Serializable {

  private final String name;
  private final String consumes;
  private final String produces;
  private final String documentation;

  public Method(String name, String documentation, String consumes, String produces) {
    if (name == null || name.trim().isEmpty()) {
      throw new IllegalArgumentException("Missing name for method");
    }
    this.name = name;
    this.documentation = documentation;
    this.consumes = getMediaType(consumes);
    this.produces = getMediaType(produces);
  }

  private String getMediaType(String mediaType) {
    if (mediaType == null) {
      return null;
    }
    if ("*/*".equals(mediaType)) {
      return null;
    }
    return mediaType;
  }

  public String getName() {
    return name;
  }

  public boolean hasConsumes() {
    return hasValue(consumes);
  }

  private boolean hasValue(String value) {
    return value != null && !value.isEmpty();
  }

  public Collection<String> getConsumes() {
    return getValues(consumes);
  }

  private Collection<String> getValues(String values) {
    if (!hasValue(values)) {
      return Collections.emptyList();
    }
    if (values.contains(",")) {
      Collection<String> result = new ArrayList<>();
      for (String value : values.split(",")) {
        result.add(StringUtil.stripQuotes(value));
      }
      return result;
    }
    return Collections.singleton(values);
  }

  public boolean hasProduces() {
    return hasValue(produces);
  }

  public Collection<String> getProduces() {
    return getValues(produces);
  }

  public String getDocumentation() {
    return documentation;
  }

  public Method combineWith(Method other) {
    if (!name.equals(other.name)) {
      throw new IllegalArgumentException();
    }
    String doc = documentation == null ? other.documentation : documentation;
    String cons = combineMediaType(consumes, other.consumes);
    String prods = combineMediaType(produces, other.produces);
    return new Method(name, doc, cons, prods);
  }

  private String combineMediaType(String mediaType1, String mediaType2) {
    if (mediaType1 == null) {
      return mediaType2;
    }
    if (mediaType2 == null) {
      return mediaType1;
    }
    return mediaType1 + ',' + mediaType2;
  }

  @Override
  public int compareTo(Method other) {
    return name.compareTo(other.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Method) {
      return name.equals(((Method)obj).name);
    }
    return false;
  }

  @Override
  public String toString() {
    return name;
  }

}
