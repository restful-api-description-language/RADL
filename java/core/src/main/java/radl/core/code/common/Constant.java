/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.code.common;


public class Constant {

  private final String name;
  private final String comments;
  private final String value;

  public Constant(String name, String value, String comments) {
    this.name = name;
    this.value = value;
    this.comments = comments;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public String[] getComments() {
    return comments == null ? new String[0] : comments.split("\n");
  }

}
