/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.code.common;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import radl.java.code.Java;


public class Constants {

  private static final char WORD_SEPARATOR = '_';
  
  private final Map<String, Constant> itemsByValue = new TreeMap<String, Constant>();
  private final String prefix;
  private final String description;

  public Constants(String prefix, String description) {
    this.prefix = prefix.isEmpty() ? "" : prefix + WORD_SEPARATOR;
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public Constant add(String baseName, String value, String comments) {
    Constant result = new Constant(constantName(baseName), value, comments);
    add(result);
    return result;
  }
  
  private String constantName(String name) {
    return prefix + Java.toName(name.replace('/', WORD_SEPARATOR).toUpperCase(Locale.getDefault()));
  }
  
  private void add(Constant constant) {
    itemsByValue.put(constant.getValue(), constant);
  }
  
  public Constant byValue(String value) {
    return itemsByValue.get(value);
  }

  public Iterable<Constant> all() {
    return itemsByValue.values();
  }

  public Constants filter(String baseName, boolean include) {
    Constants result = new Constants(prefix, description);
    String name = constantName(baseName);
    for (Constant candidate : all()) {
      if (include == candidate.getName().equals(name)) {
        result.add(candidate);
      }
    }
    return result;
  }
  
}
