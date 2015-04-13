/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;


public class Documentable {

  private final String documentation;

  public Documentable(String documentation) {
    this.documentation = documentation;
  }

  public boolean hasDocumentation() {
    return documentation != null;
  }

  public String getDocumentation() {
    return documentation;
  }

}
