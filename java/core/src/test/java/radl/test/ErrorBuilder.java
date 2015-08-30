/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

public class ErrorBuilder {

  private final RadlBuilder parent;
  private final Map<String, String> errors = new LinkedHashMap<String, String>();

  public ErrorBuilder(RadlBuilder parent) {
    this.parent = parent;
  }

  public ErrorBuilder error(String name, String documentation) {
    errors.put(name, documentation);
    return this;
  }

  public RadlBuilder end() {
    parent.setErrors(errors);
    return parent;
  }

  public Document build() {
    return end().build();
  }

}
