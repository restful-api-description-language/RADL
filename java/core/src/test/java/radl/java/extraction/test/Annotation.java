/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;


public class Annotation {

  private final String name;
  private final String property;
  private final String value;

  public Annotation(String name) {
    this(name, null);
  }

  public Annotation(String name, String value) {
    this(name, "value", value);
  }

  public Annotation(String name, String property, String value) {
    this.name = name;
    this.property = property;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public String getProperty() {
    return property;
  }

  public String getValue() {
    return value;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Annotation && name.equals(((Annotation)obj).name);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder().append(name);
    if (value != null) {
      result.append('(').append(property).append("=\"").append(value).append("\")");
    }
    return result.toString();
  }

}
