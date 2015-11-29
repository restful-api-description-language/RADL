/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Collection;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.common.Constants;
import radl.core.code.radl.RadlCode;
import radl.java.code.Java;
import radl.java.code.JavaCode;


public class ExceptionsGenerator extends FromRadlErrorsCodeGenerator {

  private static final int DEFAULT_STATUS_CODE = BAD_REQUEST;

  private Constants errorConstants;

  @Override
  protected void generateFromRadlErrors(RadlCode radl, Iterable<String> errors, Map<String, Object> context,
      Collection<Code> sources) {
    errorConstants = (Constants)context.get(ERROR_CONSTANTS);
    for (String error : errors) {
      sources.add(generateException(error, getStatusCode(radl, error), radl.errorDocumentation(error)));
    }
  }

  private int getStatusCode(RadlCode radl, String error) {
    int result = radl.errorStatus(error);
    if (result < 0) {
      result = DEFAULT_STATUS_CODE;
    }
    return result;
  }

  private JavaCode generateException(String error, int statusCode, String documentation) {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import %s;", apiType());
    result.add("");
    result.add("");
    String type = toExceptionTypeName(getErrorName(error));
    result.add("public class %s extends %s implements %s {", type, getBaseException(statusCode), IDENTIFIABLE_TYPE);
    result.add("");
    result.add("  public %s() {", type);
    result.add("    super(\"%s\");", getExceptionMessage(error, documentation));
    result.add("  }");
    result.add("");
    result.add("  public String getId() {");
    result.add("    return %s.%s;", API_TYPE, errorConstants.byValue(error).getName());
    result.add("  }");
    result.add("");
    result.add("}");
    return result;
  }

  private String getExceptionMessage(String error, String documentation) {
    if (documentation == null || documentation.trim().isEmpty()) {
      return errorConstants.byValue(error).getName();
    }
    return Java.toString(documentation.trim());
  }

  private String getBaseException(int statusCode) {
    switch (statusCode) {
      case BAD_REQUEST: return IllegalArgumentException.class.getSimpleName();
      case INTERNAL_SERVER_ERROR: return IllegalStateException.class.getSimpleName();
      default: return RuntimeException.class.getSimpleName();
    }
  }

}
