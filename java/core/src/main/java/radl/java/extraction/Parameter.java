/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;


/**
 * A parameter to a method.
 */
public class Parameter {

  private final String name;
  private final String documentation;

  public Parameter(String name, String documentation) {
    this.name = name;
    this.documentation = documentation;
  }

  public String getName() {
    return name;
  }

  public String getDocumentation() {
    return documentation;
  }

}
