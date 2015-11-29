/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.java.code.JavaCode;


public class RestResponseGenerator extends FromRadlCodeGenerator {

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    return Arrays.asList(generateRestResponse());
  }

  private Code generateRestResponse() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import java.util.ArrayList;");
    result.add("import java.util.Collection;");
    result.add("import java.util.HashMap;");
    result.add("import java.util.Map;");
    result.add("");
    result.add("import %s.%s;", STATUS_TYPE_PACKAGE, STATUS_TYPE);
    result.add("");
    result.add("");
    result.add("public class %s<T> {", SUPPORT_RESPONSE_TYPE);
    result.add("");
    result.add("  private final T payload;");
    result.add("  private final Collection<String> excludedActions = new ArrayList<String>();");
    result.add("  private final Map<String, String> parameters = new HashMap<String, String>();");
    result.add("  private %1$s status = %1$s.OK;", STATUS_TYPE);
    result.add("");
    result.add("  public %s(T payload) {", SUPPORT_RESPONSE_TYPE);
    result.add("    this.payload = payload;");
    result.add("  }");
    result.add("");
    result.add("  public T getPayload() {");
    result.add("    return payload;");
    result.add("  }");
    result.add("");
    result.add("  public void %s(String action) {", TRANSITITION_DENY_NAME);
    result.add("    excludedActions.add(action);");
    result.add("  }");
    result.add("");
    result.add("  public boolean %s(String action) {", TRANSITITION_CHECK_NAME);
    result.add("    return !excludedActions.contains(action);");
    result.add("  }");
    result.add("");
    result.add("  public String getParameter(String name) {");
    result.add("    return parameters.get(name);");
    result.add("  }");
    result.add("");
    result.add("  public void setParameter(String name, String value) {");
    result.add("    parameters.put(name, value);");
    result.add("  }");
    result.add("");
    result.add("  public %s getStatus() {", STATUS_TYPE);
    result.add("    return status;");
    result.add("  }");
    result.add("");
    result.add("  public void setStatus(%s status) {", STATUS_TYPE);
    result.add("    this.status = status;");
    result.add("  }");
    result.add("");
    result.add("}");
    return result;
  }

}
