/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;


public class JavaBeanProperty {

  private final String name;
  private final String type;
  private final String annotation;

  public JavaBeanProperty(String name) {
    this(name, String.class.getSimpleName(), null);
  }
  
  public JavaBeanProperty(String name, String type, String annotation) {
    this.name = name;
    this.type = type;
    this.annotation = annotation;
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getAnnotation() {
    return annotation;
  }

}
