/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.io.Serializable;

/**
 * A variable in a URI template, as defined by RFC 6570.
 */
public class UriTemplateVar implements Serializable {

  private final String name;
  private final String documentation;

  public UriTemplateVar(String name, String documentation) {
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
