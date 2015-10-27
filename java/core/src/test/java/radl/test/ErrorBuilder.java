/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;


public class ErrorBuilder {

  private final RadlBuilder parent;
  private final Map<String, Error> errors = new LinkedHashMap<String, Error>();

  public ErrorBuilder(RadlBuilder parent) {
    this.parent = parent;
  }

  public ErrorBuilder error(String name) {
    return error(name, null);
  }

  public ErrorBuilder error(String name, String documentation) {
    return error(name, documentation, Error.UNDEFINED_STATUS_CODE);
  }

  public ErrorBuilder error(String name, String documentation, int statusCode) {
    errors.put(name, new Error(documentation, statusCode));
    return this;
  }

  public ErrorBuilder error(String name, int statusCode) {
    return error(name, null, statusCode);
  }
  
  public RadlBuilder end() {
    parent.setErrors(errors);
    return parent;
  }

  public Document build() {
    return end().build();
  }

  
  public class Error {

    public static final int UNDEFINED_STATUS_CODE = -1;
    
    private final String documentation;
    private final int statusCode;

    public Error(String documentation, int statusCode) {
      this.documentation = documentation;
      this.statusCode = statusCode;
    }

    public String getDocumentation() {
      return documentation;
    }

    public int getStatusCode() {
      return statusCode;
    }

    public boolean hasStatusCode() {
      return statusCode != UNDEFINED_STATUS_CODE;
    }
    
  }
  
}
