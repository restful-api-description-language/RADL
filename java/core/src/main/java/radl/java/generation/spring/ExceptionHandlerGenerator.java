/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.core.generation.Module;
import radl.java.code.JavaCode;


public class ExceptionHandlerGenerator extends FromRadlErrorsCodeGenerator {

  private Map<Integer, String> httpStatuses;

  @SuppressWarnings("unchecked")
  @Override
  protected void generateFromRadlErrors(RadlCode radl, Iterable<String> errors, Map<String, Object> context,
      Collection<Code> sources) {
    httpStatuses = (Map<Integer, String>)context.get(SPRING_HTTP_STATUSES);
    Collection<Code> generatedSources = ((List<Module>)context.get(OUTPUT_MODULES)).get(0);
    sources.add(generateExceptionHandler(radl, errors, generatedSources));
  }

  private Code generateExceptionHandler(RadlCode radl, Iterable<String> errors, Collection<Code> sources) {
    final JavaCode errorHandler = startErrorHandler();
    final Collection<String> errorHandlingMethods = new ArrayList<>();
    for (String error : errors) {
      int statusCode = radl.errorStatus(error);
      JavaCode exceptionType = getException(error, sources);
      handleException(exceptionType, statusCode, errorHandlingMethods, errorHandler);
    }
    return endErrorHandler(errorHandler);
  }

  private JavaCode startErrorHandler() {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import %s.%s;", STATUS_TYPE_PACKAGE, STATUS_TYPE);
    result.add("import %s.%s;", RESPONSE_PACKAGE, RESPONSE_TYPE);
    result.add("import org.springframework.web.bind.annotation.ControllerAdvice;");
    result.add("import org.springframework.web.bind.annotation.ExceptionHandler;");
    result.add("");
    result.add("");
    result.add("@ControllerAdvice");
    result.add("public class CentralErrorHandler {");
    result.add("");
    return result;
  }

  private JavaCode getException(String error, Collection<Code> sources) {
    String typeName = toExceptionTypeName(getErrorName(error));
    for (Code source : sources) {
      JavaCode result = (JavaCode)source;
      if (result.isType(typeName)) {
        return result;
      }
    }
    throw new IllegalArgumentException(String.format("Class %s not found for error: %s", typeName, error));
  }

  private void handleException(JavaCode exceptionType, int statusCode, Collection<String> errorHandlingMethods,
      JavaCode errorHandler) {
    if (FRAMEWORK_HANDLED_STATUSES.contains(statusCode)) {
      return;
    }
    String handledType;
    String method;
    if (statusCode == INTERNAL_SERVER_ERROR) {
      handledType = Throwable.class.getSimpleName();
      method = "internalError";
    } else {
      handledType = handledExceptionType(exceptionType);
      method = exceptionTypeToMethod(handledType);
    }
    if (errorHandlingMethods.contains(method)) {
      return;
    }
    errorHandlingMethods.add(method);

    errorHandler.add("  @ExceptionHandler({ %s.class })", handledType);
    errorHandler.add("  public ResponseEntity<%s> %s(%s e) {", ERROR_DTO_TYPE, method, handledType);
    errorHandler.add("    return error(e, %s.%s);", STATUS_TYPE, httpStatuses.get(statusCode));
    errorHandler.add("  }");
    errorHandler.add("");
    errorHandler.ensureImport(RESPONSE_PACKAGE, "ResponseEntity");
  }

  private String handledExceptionType(JavaCode exceptionType) {
    String result = exceptionType.superTypeName();
    if (RuntimeException.class.getSimpleName().equals(result)) {
      result = exceptionType.typeName();
    }
    return result;
  }

  private String exceptionTypeToMethod(String exceptionType) {
    StringBuilder result = new StringBuilder(exceptionType);
    result.setLength(result.length() - "Exception".length());
    result.setCharAt(0, Character.toLowerCase(result.charAt(0)));
    return result.toString();
  }

  private Code endErrorHandler(Code errorHandler) {
    errorHandler.add("  private ResponseEntity<%s> error(Throwable t, %s statusCode) {", ERROR_DTO_TYPE, STATUS_TYPE);
    errorHandler.add("    %s error = new %s();", ERROR_DTO_TYPE, ERROR_DTO_TYPE);
    errorHandler.add("    if (t instanceof %s) {", IDENTIFIABLE_TYPE);
    errorHandler.add("      error.type = ((%s)t).getId();", IDENTIFIABLE_TYPE);
    errorHandler.add("    }");
    errorHandler.add("    error.title = getNonRevealingMessage(t);");
    errorHandler.add("    return new ResponseEntity<%s>(error, statusCode);", ERROR_DTO_TYPE);
    errorHandler.add("  }");
    errorHandler.add("");
    errorHandler.add("  private String getNonRevealingMessage(Throwable t) {");
    errorHandler.add("    StringBuilder result = new StringBuilder(64);");
    errorHandler.add("    result.append(t.getMessage());");
    errorHandler.add("    int index = result.indexOf(\"Exception\");");
    errorHandler.add("    while (index >= 0) {");
    errorHandler.add("      int start = findIdentifierEnd(result, index, -1);");
    errorHandler.add("      int end = findIdentifierEnd(result, index, +1);");
    errorHandler.add("      result.delete(start + 1, end);");
    errorHandler.add("      index = result.indexOf(\"Exception\", start + 1);");
    errorHandler.add("    }");
    errorHandler.add("    return result.toString();");
    errorHandler.add("  }");
    errorHandler.add("");
    errorHandler.add("  private int findIdentifierEnd(StringBuilder text, int start, int delta) {");
    errorHandler.add("    int index = start;");
    errorHandler.add("    while (!isAtEnd(text, index, delta)");
    errorHandler.add("        && (Character.isJavaIdentifierPart(text.charAt(index)) || text.charAt(index) == '.')) {");
    errorHandler.add("      index += delta;");
    errorHandler.add("    }");
    errorHandler.add("    while (!isAtEnd(text, index, delta) && isNonWord(text.charAt(index))) {");
    errorHandler.add("      index += delta;");
    errorHandler.add("    }");
    errorHandler.add("    return index;");
    errorHandler.add("  }");
    errorHandler.add("  ");
    errorHandler.add("  private boolean isAtEnd(StringBuilder text, int index, int delta) {");
    errorHandler.add("    return delta < 0 ? index < 0 : index == text.length();");
    errorHandler.add("  }");
    errorHandler.add("  ");
    errorHandler.add("  private boolean isNonWord(char ch) {");
    errorHandler.add("    return Character.isWhitespace(ch) || isPunctuation(ch);");
    errorHandler.add("  }");
    errorHandler.add("  ");
    errorHandler.add("  private boolean isPunctuation(char ch) {");
    errorHandler.add("    return ch == '.' || ch == ';' || ch == ':' || ch == '-';");
    errorHandler.add("  }");
    errorHandler.add("  ");
    errorHandler.add("}");
    return errorHandler;
  }

}
