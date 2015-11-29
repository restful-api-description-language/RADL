/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import radl.core.code.radl.RadlCode;
import radl.java.code.JavaCode;


public class ControllerSupportsGenerator extends AbstractControllersGenerator {

  static final String CLASS_NAME_SUFFIX = "ControllerSupport";

  @Override
  protected void initController(RadlCode radl, String resource, JavaCode controllerSupport) {
    controllerSupport.add("import org.springframework.stereotype.Service;");
    controllerSupport.add("");
    controllerSupport.add("");
    controllerSupport.add("@Service");
  }
  
  @Override
  protected String getClassNameSuffix() {
    return CLASS_NAME_SUFFIX;
  }
  
  @Override
  protected void addControllerMethod(RadlCode radl, String resource, String method, JavaCode controllerSupport) {
    String consumes = getConsumes(radl, resource, method);
    String produces = getProduces(radl, resource, method);
    String argName = parameterName(consumes);
    String args = parameters(consumes, radl, resource, method, argName, false);
    String type = returnType(produces, radl, resource, method);
    boolean hasReturn = !NO_TYPE.equals(type);
    addReturnTypeImport(type, false, controllerSupport);
    if (hasReturn) {
      addReturnTypeImport(SUPPORT_RESPONSE_TYPE, false, controllerSupport);
    }
    controllerSupport.add("  public %s<%s> %s(%s) {", SUPPORT_RESPONSE_TYPE, type, httpToJavaMethod(method), args);
    if (hasReturn) {
      controllerSupport.add("    %s result = %s", type, getDummyReturnStatement(type, controllerSupport));
      controllerSupport.add("    // result.xxx = ...;");
      controllerSupport.add("    %1$s<%2$s> %3$s = new %1$s<%2$s>(result);", SUPPORT_RESPONSE_TYPE, type, RESPONSE_VAR);
      controllerSupport.add("    // %s.deny(%s.YYY);", RESPONSE_VAR, ACTIONS_TYPE);
      controllerSupport.add("    // %s.setStatus(%s.ZZZ);", RESPONSE_VAR, STATUS_TYPE);
    } else {
      controllerSupport.add("    %1$s<%2$s> %3$s = new %1$s<%2$s>(null);", SUPPORT_RESPONSE_TYPE, NO_TYPE, RESPONSE_VAR);
      controllerSupport.add("    %s.setStatus(%s.NO_CONTENT);", RESPONSE_VAR, STATUS_TYPE);
      controllerSupport.ensureImport(STATUS_TYPE_PACKAGE, STATUS_TYPE);
    }
    controllerSupport.add("    return %s;", RESPONSE_VAR);
    controllerSupport.add("  }");
    controllerSupport.add("");
  }

  private String getDummyReturnStatement(String type, JavaCode code) {
    String result;
    if (Boolean.TRUE.toString().equals(type)) {
      result = type + ";";
    } else if (NO_TYPE.equals(type)) {
      result = "";
      code.ensureImport(STATUS_TYPE_PACKAGE, STATUS_TYPE);
    } else if (UNKNOWN_OUTPUT_TYPE.equals(type)) {
      result = "new " + type + "();";
      code.ensureImport(UNKNOWN_OUTPUT_TYPE_PACKAGE, UNKNOWN_OUTPUT_TYPE);
    } else {
      result = "new " + type + "();";
    }
    return result;
  }

}
