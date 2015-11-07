/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atteo.evo.inflector.English;
import org.w3c.dom.Document;

import radl.common.StringUtil;
import radl.core.code.Code;
import radl.core.code.MediaType;
import radl.core.code.Property;
import radl.core.code.PropertyGroup;
import radl.core.code.PropertyGroups;
import radl.core.code.RadlCode;
import radl.core.code.RadlCode.ResourceMethod;
import radl.core.generation.CodeGenerator;
import radl.java.code.Constant;
import radl.java.code.Constants;
import radl.java.code.Java;
import radl.java.code.JavaBeanProperty;
import radl.java.code.JavaCode;

/**
 * Generates Java code for the Spring framework from a RADL document.
 */
public class SpringCodeGenerator implements CodeGenerator { // NOPMD ExcessiveClassLength

  private static final String NO_PARAMETER = "null";
  private static final String DTO_SUFFIX = "Resource";
  private static final String UNKNOWN_INPUT_TYPE = "Object";
  private static final String UNKNOWN_OUTPUT_TYPE = "ResourceSupport";
  private static final String UNKNOWN_OUTPUT_TYPE_PACKAGE = "org.springframework.hateoas";
  private static final String RESPONSE_TYPE = "ResponseEntity";
  private static final String RESPONSE_PACKAGE = "org.springframework.http";
  private static final String NO_TYPE = "Void";
  private static final String STATUS_TYPE = "HttpStatus";
  private static final String STATUS_TYPE_PACKAGE = RESPONSE_PACKAGE;
  private static final String DEFAULT_HEADER = "Generated from RADL.";
  private static final String IMPL_PACKAGE = "impl";
  private static final String API_PACKAGE = "api";
  private static final String BILLBOARD_URL = "BILLBOARD";
  private static final String DEFAULT_MEDIA_TYPE = "application/";
  private static final String DEFAULT_MEDIA_TYPE_CONSTANT = "MEDIA_TYPE_DEFAULT";
  private static final String API_TYPE = "Api";
  private static final String URIS_TYPE = "Resources";
  private static final int BAD_REQUEST = 400;
  private static final int DEFAULT_STATUS_CODE = BAD_REQUEST;
  private static final int INTERNAL_SERVER_ERROR = 500;
  private static final String ERROR_DTO_TYPE = "Error" + DTO_SUFFIX;
  private static final String IDENTIFIABLE_TYPE = "Identifiable";
  private static final Map<Integer, String> HTTP_STATUSES = new HashMap<Integer, String>();
  private static final Collection<Integer> FRAMEWORK_HANDLED_STATUSES = Arrays.asList(405, 406);
  private static final String SEMANTIC_ANNOTATION_PACKAGE = "de.escalon.hypermedia.hydra.mapping";
  private static final String SEMANTIC_ANNOTATION = "Expose";
  private static final String RESPONSE_VAR = "response";
  private static final String SUPPORT_RESPONSE_TYPE = "Rest" + StringUtil.initCap(RESPONSE_VAR);
  private static final String CONTROLLER_SUPPORT_VAR = "support";
  private static final String ACTIONS_TYPE = "Actions";
  private static final String TRANSITITION_CHECK_NAME = "allows";
  private static final String TRANSITITION_DENY_NAME = "deny";

  private final String packagePrefix;
  private final Constants errorConstants = new Constants("ERROR", "Error conditions");
  private final Constants linkRelationConstants = new Constants("LINK_REL", "Link relations");
  private final Constants mediaTypeConstants = new Constants("MEDIA_TYPE", "Media types");
  private final Constants transitionConstants = new Constants("", "");
  private final Constants uriConstants = new Constants("URL", "URIs");
  private final String fileHeader;
  private MediaType defaultMediaType;

  public SpringCodeGenerator(String packagePrefix) {
    this(packagePrefix, null);
  }

  public SpringCodeGenerator(String packagePrefix, String header) {
    this.packagePrefix = packagePrefix;
    this.fileHeader = header == null || header.trim().isEmpty() ? DEFAULT_HEADER : header;
    initHttpStatuses();
  }

  private void initHttpStatuses() {
    HTTP_STATUSES.put(400, "BAD_REQUEST");
    HTTP_STATUSES.put(401, "UNAUTHORIZED");
    HTTP_STATUSES.put(402, "PAYMENT_REQUIRED");
    HTTP_STATUSES.put(403, "FORBIDDEN");
    HTTP_STATUSES.put(404, "NOT_FOUND");
    HTTP_STATUSES.put(405, "METHOD_NOT_ALLOWED");
    HTTP_STATUSES.put(406, "NOT_ACCEPTABLE");
    HTTP_STATUSES.put(407, "PROXY_AUTHENTICATION_REQUIRED");
    HTTP_STATUSES.put(408, "REQUEST_TIMEOUT");
    HTTP_STATUSES.put(409, "CONFLICT");
    HTTP_STATUSES.put(410, "GONE");
    HTTP_STATUSES.put(411, "LENGTH_REQUIRED");
    HTTP_STATUSES.put(412, "PRECONDITION_FAILED");
    HTTP_STATUSES.put(413, "PAYLOAD_TOO_LARGE");
    HTTP_STATUSES.put(414, "URI_TOO_LONG");
    HTTP_STATUSES.put(415, "UNSUPPORTED_MEDIA_TYPE");
    HTTP_STATUSES.put(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
    HTTP_STATUSES.put(417, "EXPECTATION_FAILED");
    HTTP_STATUSES.put(422, "UNPROCESSABLE_ENTITY");
    HTTP_STATUSES.put(423, "LOCKED");
    HTTP_STATUSES.put(424, "FAILED_DEPENDENCY");
    HTTP_STATUSES.put(426, "UPGRADE_REQUIRED");
    HTTP_STATUSES.put(428, "PRECONDITION_REQUIRED");
    HTTP_STATUSES.put(429, "TOO_MANY_REQUESTS");
    HTTP_STATUSES.put(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");
    HTTP_STATUSES.put(500, "INTERNAL_SERVER_ERROR");
    HTTP_STATUSES.put(501, "NOT_IMPLEMENTED");
    HTTP_STATUSES.put(502, "BAD_GATEWAY");
    HTTP_STATUSES.put(503, "SERVICE_UNAVAILABLE");
    HTTP_STATUSES.put(504, "GATEWAY_TIMEOUT");
    HTTP_STATUSES.put(505, "HTTP_VERSION_NOT_SUPPORTED");
    HTTP_STATUSES.put(506, "VARIANT_ALSO_NEGOTIATES");
    HTTP_STATUSES.put(507, "INSUFFICIENT_STORAGE");
    HTTP_STATUSES.put(508, "LOOP_DETECTED");
    HTTP_STATUSES.put(509, "BANDWIDTH_LIMIT_EXCEEDED");
    HTTP_STATUSES.put(510, "NOT_EXTENDED");
    HTTP_STATUSES.put(511, "NETWORK_AUTHENTICATION_REQUIRED");
  }

  @Override
  public Iterable<Code> generateFrom(Document radl) {
    Collection<Code> result = new ArrayList<Code>();
    generate(new RadlCode(radl), result);
    return result;
  }

  private void generate(RadlCode radl, Collection<Code> result) {
    defaultMediaType = radl.defaultMediaType();
    boolean hasHyperMediaTypes = radl.hasHyperMediaTypes();
    addLinkRelationConstants(radl);
    generateSourcesForPropertyGroups(radl.propertyGroups(), hasHyperMediaTypes, result);
    generateSourcesForResources(radl, hasHyperMediaTypes, result);
    generateSourcesForErrors(radl, result);
  }

  private void generateSourcesForPropertyGroups(PropertyGroups propertyGroups, final boolean hasHyperMediaTypes,
      final Collection<Code> sources) {
    if (propertyGroups == null) {
      return;
    }
    for (String propertyGroup : propertyGroups.names()) {
      addDtosFor(propertyGroups.item(propertyGroup), hasHyperMediaTypes, sources);
    }
  }

  protected String addDtosFor(PropertyGroup propertyGroup, boolean hasHyperMediaTypes, Collection<Code> sources) {
    final JavaCode code = new JavaCode();
    addPackage(propertyGroup.name(), code);
    code.add("");
    String superType;
    if (hasHyperMediaTypes) {
      code.add("import org.springframework.hateoas.ResourceSupport;");
      code.add("");
      superType = "extends ResourceSupport ";
    } else {
      superType = "";
    }
    addSemanticAnnotationImport(propertyGroup, code);
    addDtoImports(propertyGroup, code);
    code.add("");
    String annotation = getSemanticAnnotation(propertyGroup, "");
    if (annotation != null) {
      code.add(annotation);
    }
    String result = getDtoClass(propertyGroup.name());
    code.add("public class %s %s{", result, superType);
    code.add("");
    addDtoFields(hasHyperMediaTypes, propertyGroup, code, sources);
    code.add("}");
    sources.add(code);
    return result;
  }

  private static String getDtoClass(String name) {
    return Java.toIdentifier(name) + DTO_SUFFIX;
  }

  private void addDtoImports(PropertyGroups propertyGroup, final Code code) {
    boolean added = false;
    for (String name : propertyGroup.names()) {
      String ref = propertyGroup.item(name).reference();
      if (ref.isEmpty()) {
        ref = name;
      }
      code.add("import %s.%s.%s;", packagePrefix, toPackage(ref), getDtoClass(ref));
      added = true;
    }
    if (added) {
      code.add("");
    }
  }

  private void addSemanticAnnotationImport(PropertyGroup propertyGroup, Code code) {
    if (defaultMediaType != null && defaultMediaType.isSemanticMediaType() && propertyGroup.hasSemantics()) {
      code.add("import %s.%s;", SEMANTIC_ANNOTATION_PACKAGE, SEMANTIC_ANNOTATION);
      code.add("");
    }
  }

  private String getSemanticAnnotation(Property property, String indent) {
    String result = null;
    if (defaultMediaType != null && defaultMediaType.isSemanticMediaType()) {
      String uri = property.uri();
      if (!uri.isEmpty()) {
        result = String.format("%s@%s(\"%s\")", indent, SEMANTIC_ANNOTATION, Java.toString(uri));
      }
    }
    return result;
  }

  private void addDtoFields(boolean hasHyperMediaTypes, PropertyGroup propertyGroup, JavaCode dto,
      Collection<Code> sources) {
    Collection<JavaBeanProperty> properties = new ArrayList<JavaBeanProperty>();
    for (String propertyName : propertyGroup.propertyNames()) {
      Property property = propertyGroup.property(propertyName);
      String annotation = getSemanticAnnotation(property, "  ");
      String type = getType(property, hasHyperMediaTypes, sources);
      int index = type.lastIndexOf('.');
      if (index > 0) {
        String simpleType = type.substring(index + 1);
        dto.ensureImport(type.substring(0, index), simpleType);
        type = simpleType;
      }
      String fieldName = property.repeats() ? English.plural(propertyName) : propertyName;
      properties.add(new JavaBeanProperty(fieldName, type, annotation));
    }
    addProperties(dto, properties);
  }

  protected String getType(Property property, boolean hasHyperMediaTypes, Collection<Code> sources) {
    String result = null;
    if (property instanceof PropertyGroup) {
      PropertyGroup propertyGroup = (PropertyGroup)property;
      String ref = propertyGroup.reference();
      if (ref.isEmpty()) {
        result = addDtosFor(propertyGroup, hasHyperMediaTypes, sources);
      } else {
        result = getDtoClass(ref);
      }
    }
    if (result == null) {
      String type = property.type();
      result = type.isEmpty() ? "String" : radlTypeToJavaType(type);
    }
    if (property.repeats()) {
      return result + "[]";
    }
    return result;
  }

  private String radlTypeToJavaType(String type) {
    if ("xsd:dateTime".equals(type)) {
      return XMLGregorianCalendar.class.getName();
    }
    if ("number".equals(type)) {
      return "double";
    }
    return type;
  }

  private void generateSourcesForErrors(RadlCode radl, final Collection<Code> sources) {
    Iterator<String> errors = radl.errors().iterator();
    if (!errors.hasNext()) {
      return;
    }
    sources.add(generateErrorDto());
    sources.add(generateIdentifiable());
    final JavaCode errorHandler = startErrorHandler();
    final Collection<String> errorHandlingMethods = new ArrayList<String>();
    do {
      String name = errors.next();
      int statusCode = radl.errorStatus(name);
      if (statusCode < 0) {
        statusCode = DEFAULT_STATUS_CODE;
      }
      String documentation = radl.errorDocumentation(name);
      JavaCode exceptionType = generateException(name, statusCode, documentation);
      sources.add(exceptionType);
      handleException(exceptionType, statusCode, errorHandlingMethods, errorHandler);
    } while (errors.hasNext());
    sources.add(endErrorHandler(errorHandler));
  }

  private Code generateErrorDto() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public class %s {", ERROR_DTO_TYPE);
    result.add("");
    addProperties(result, Arrays.asList(new JavaBeanProperty("title"), new JavaBeanProperty("type")));
    result.add("}");
    return result;
  }

  private void addProperties(Code code, Iterable<JavaBeanProperty> properties) {
    if (!properties.iterator().hasNext()) {
      return;
    }
    for (JavaBeanProperty property : properties) {
      if (property.getAnnotation() != null) {
        code.add("%s", property.getAnnotation());
      }
      code.add("  public %s %s;", property.getType(), property.getName());
    }
    code.add("");
  }

  private Code generateIdentifiable() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", IDENTIFIABLE_TYPE);
    result.add("");
    result.add("  String getId();");
    result.add("");
    result.add("}");
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

  private String getErrorName(String name) {
    URI uri;
    try {
      uri = new URI(name);
    } catch (URISyntaxException e) {
      return name;
    }
    if (uri.getScheme() == null || !uri.getScheme().startsWith("http")) {
      return name;
    }
    String path = uri.getPath();
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private String toExceptionTypeName(String name) {
    return Java.toIdentifier(name + "Exception");
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
    errorHandler.add("    return error(e, %s.%s);", STATUS_TYPE, HTTP_STATUSES.get(statusCode));
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

  private Code endErrorHandler(JavaCode errorHandler) {
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

  private void generateSourcesForResources(RadlCode radl, boolean hasHyperMediaTypes, Collection<Code> sources) {
    Iterator<String> startTransitions = radl.stateTransitionNames("").iterator();
    String startTransition = startTransitions.hasNext() ? startTransitions.next() : null;
    sources.add(generatePermittedActions());
    sources.add(generateActions(radl, hasHyperMediaTypes));
    for (String resource : radl.resourceNames()) {
      sources.add(generateController(radl, resource, hasHyperMediaTypes, startTransition));
      sources.add(generateControllerSupport(radl, resource));
    }
    sources.add(generateApi(radl));
    sources.add(generateUris());
  }

  private Code generatePermittedActions() {
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

  private Code generateActions(RadlCode radl, boolean hasHyperMediaTypes) {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", ACTIONS_TYPE);
    if (hasHyperMediaTypes) {
      addTransitionConstants(radl, result);
      result.add("");
    }
    result.add("}");
    return result;
  }

  private void addTransitionConstants(RadlCode radl, JavaCode code) {
    for (String transition : getTransitions(radl)) {
      transitionConstants.add(transition, transition, null);
    }
    addConstants(transitionConstants, code);
  }

  private Iterable<String> getTransitions(RadlCode radl) {
    Collection<String> result = new TreeSet<String>();
    for (String state : radl.stateNames()) {
      if (radl.isStartState(state)) {
        continue;
      }
      for (String transition : radl.stateTransitionNames(state)) {
        result.add(transition);
      }
    }
    return result;
  }

  private Code generateUris() {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", URIS_TYPE);
    addUris(result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addUris(JavaCode code) {
    addConstants(uriConstants.filter(BILLBOARD_URL, false), code);
  }

  private Code generateApi(RadlCode radl) {
    JavaCode result = new JavaCode();
    addPackage(API_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", API_TYPE);
    addBillboardUri(result);
    addMediaTypes(result);
    addLinkRelations(result);
    addErrors(radl, result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addErrors(RadlCode radl, JavaCode code) {
    addErrorConstants(radl);
    addConstants(errorConstants, code);
  }

  private void addErrorConstants(RadlCode radl) {
    for (String value : radl.errors()) {
      errorConstants.add(getErrorName(value), value, radl.errorDocumentation(value));
    }
  }

  private void addBillboardUri(JavaCode code) {
    addConstants(uriConstants.filter(BILLBOARD_URL, true), code);
  }

  private void addLinkRelations(JavaCode code) {
    addConstants(linkRelationConstants, code);
  }

  private void addLinkRelationConstants(RadlCode radl) {
    for (String value : radl.linkRelationNames()) {
      String[] segments = value.split("/");
      String name = segments[segments.length - 1];
      String documentation = radl.linkRelationDocumentation(value);
      linkRelationConstants.add(name, value, documentation);
    }
  }

  private void addMediaTypes(JavaCode code) {
    addConstants(mediaTypeConstants, code);
    if (defaultMediaType != null) {
      if (!mediaTypeConstants.all().iterator().hasNext()) {
        addConstantsHeading(mediaTypeConstants.getDescription(), code);
      }
      code.add("  String %s = \"%s\";", getLocalMediaTypeConstant(defaultMediaType.name()), defaultMediaType.name());
      code.add("  String %s = %s;", DEFAULT_MEDIA_TYPE_CONSTANT, getLocalMediaTypeConstant(defaultMediaType.name()));
    }
  }

  private void addConstants(Constants constants, JavaCode code) {
    Iterator<Constant> iterator = constants.all().iterator();
    if (iterator.hasNext()) {
      String scope = code.isClass() ? "public static " : "";
      addConstantsHeading(constants.getDescription(), code);
      while (iterator.hasNext()) {
        Constant constant = iterator.next();
        if (constant.getComments().length > 0) {
          code.add("  /**");
          for (String comment : constant.getComments()) {
            code.add("   * %s", comment);
          }
          code.add("   */");
        }
        code.add("  %sString %s = \"%s\";", scope, constant.getName(), constant.getValue());
      }
    }
  }

  private void addConstantsHeading(String heading, Code code) {
    code.add("");
    if (heading.isEmpty()) {
      return;
    }
    code.add("");
    code.add("  // %s", heading);
    code.add("");
  }

  private void addPackage(String name, Code code) {
    code.add("/*");
    for (String line : fileHeader.split("\n")) {
      code.add(" * %s", line);
    }
    code.add(" */");
    code.add("package %s.%s;", packagePrefix, toPackage(name));
  }

  private String toPackage(String name) {
    StringBuilder result = new StringBuilder(name);
    int i = 0;
    while (i < result.length()) {
      char c = result.charAt(i);
      if (Character.isUpperCase(c)) {
        result.setCharAt(i, Character.toLowerCase(c));
      } else if (!Character.isJavaIdentifierPart(c)) {
        int j = i + 1;
        while (j < result.length() && !Character.isJavaIdentifierPart(result.charAt(j))) {
          j++;
        }
        result.delete(i, j);
      }
      i++;
    }
    if (result.charAt(result.length() - 1) == 's') {
      result.setLength(result.length() - 1);
    }
    return result.toString();
  }

  private String apiType() {
    return join(packagePrefix, API_PACKAGE, API_TYPE);
  }

  private String join(String... parts) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String part : parts) {
      result.append(prefix).append(part);
      prefix = ".";
    }
    return result.toString();
  }

  private String urisType() {
    return join(packagePrefix, IMPL_PACKAGE, URIS_TYPE);
  }

  private Code generateController(RadlCode radl, String resource, final boolean hasHyperMediaTypes,
      String startTransition) {
    final JavaCode result = new JavaCode();
    String name = resource;
    addPackage(name, result);
    result.add("");
    String uri = radl.resourceLocation(resource);
    boolean addUris;
    String constantName;
    String type;
    if (transitionsToStart(radl, startTransition, resource)) {
      constantName = BILLBOARD_URL;
      type = API_TYPE;
      addUris = false;
    } else {
      constantName = name;
      type = URIS_TYPE;
      addUris = true;
    }
    addControllerImports(radl, resource, addUris, result);
    result.add("@RestController");
    if (uri != null) {
      Constant constant = uriConstants.add(constantName, uri, null);
      result.add(String.format("@RequestMapping(%s.%s)", type, constant.getName()));
    }
    result.add("public class %s {", getControllerClassName(resource));
    result.add("");
    result.add("  @Autowired");
    result.add("  private %s %s;", getControllerSupportClassName(resource), CONTROLLER_SUPPORT_VAR);
    result.add("");
    for (String method : radl.methodNames(resource)) {
      addControllerMethod(radl, resource, method, hasHyperMediaTypes, result);
    }
    result.add("}");
    return result;
  }

  private boolean transitionsToStart(RadlCode radl, final String startTransition, String resource) {
    for (String method : radl.methodNames(resource)) {
      for (String transition : radl.methodTransitions(resource, method)) {
        if (transition.equals(startTransition)) {
          return true;
        }
      }
    }
    return false;
  }

  private void addControllerMethod(RadlCode radl, String resource, String method, boolean hasHyperMediaTypes,
      JavaCode code) {
    String consumes = getConsumes(radl, resource, method);
    String produces = getProduces(radl, resource, method);
    String argName = parameterName(consumes);
    code.add("  @RequestMapping(method = RequestMethod.%s%s%s)", method.toUpperCase(Locale.getDefault()), consumes,
        produces);
    String type = returnType(produces, radl, resource, method);
    boolean hasReturn = !NO_TYPE.equals(type);
    addReturnTypeImport(type, true, code);
    code.ensureImport(RESPONSE_PACKAGE, RESPONSE_TYPE);
    String javaMethod = httpToJavaMethod(method);
    String parameters = ParametersType.CONTROLLER.parameters(consumes, radl, resource, method, argName);
    if (parameters.contains("PathVariable")) {
      code.ensureImport("org.springframework.web.bind.annotation", "PathVariable");
    }
    if (!argName.isEmpty()) {
      code.ensureImport("org.springframework.web.bind.annotation", "RequestBody");
    }
    code.add("  public %s<%s> %s(%s) {", RESPONSE_TYPE, type, javaMethod, parameters);
    parameters = stripParameterTypes(parameters);
    code.add("    %s<%s> %s = %s.%s(%s);", SUPPORT_RESPONSE_TYPE, type, RESPONSE_VAR,
        CONTROLLER_SUPPORT_VAR, javaMethod, parameters);
    if (hasReturn) {
      code.add("    %s result = %s.getPayload();", type, RESPONSE_VAR);
      if (hasHyperMediaTypes) {
        addLinks(radl, resource, method, code, parameters, argName);
      }
    }
    code.add("    return new %s<%s>(%s%s.getStatus());", RESPONSE_TYPE, type, hasReturn ? "result, " : "",
        RESPONSE_VAR);
    code.add("  }");
    code.add("");
  }

  private String stripParameterTypes(String parameters) {
    if (parameters.isEmpty()) {
      return parameters;
    }
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String parameter : parameters.split(",")) {
      result.append(prefix).append(parameter.substring(parameter.lastIndexOf(' ')).trim());
      prefix = ", ";
    }
    return result.toString();
  }

  private String httpToJavaMethod(String httpMethod) {
    return httpMethod.toLowerCase(Locale.getDefault());
  }

  private void addLinks(RadlCode radl, String resource, String method, JavaCode code, String parameters,
      String argName) {
    Collection<String> parameterList = toCollection(parameters);
    if (!argName.isEmpty()) {
      parameterList.add(argName);
    }
    for (String transition : radl.methodTransitions(resource, method)) {
      for (String state : radl.transitionEnds(transition)) {
        addLinks(radl, state, code, parameterList);
      }
    }
  }

  private Collection<String> toCollection(String parameters) {
    Collection<String> result = new ArrayList<String>();
    for (String parameter : parameters.split(",")) {
      result.add(parameter.trim());
    }
    return result;
  }

  private void addLinks(RadlCode radl, String state, JavaCode code, Collection<String> callerParameters) {
    Collection<String> addedLinkRelations = new HashSet<String>();
    for (String transition : radl.stateTransitionNames(state)) {
      ResourceMethod resourceMethod = radl.transitionMethod(transition);
      String controller = getControllerClassName(resourceMethod.getResource());
      String method = httpToJavaMethod(resourceMethod.getMethod());
      String consumes = getConsumes(radl, resourceMethod.getResource(), resourceMethod.getMethod());
      String arguments = getArguments(consumes, radl, resourceMethod.getResource(), resourceMethod.getMethod(),
          callerParameters);
      code.ensureImport(packagePrefix + '.' + toPackage(resourceMethod.getResource()), controller);
      code.ensureImport("de.escalon.hypermedia.spring", "AffordanceBuilder");
      for (String linkRelation : radl.transitionImplementations(transition)) {
        if (addedLinkRelations.add(linkRelation)) {
          String linkConstant = API_TYPE + '.' + linkRelationConstants.byValue(linkRelation).getName();
          code.add("    if (%s.%s(%s.%s)) {", RESPONSE_VAR, TRANSITITION_CHECK_NAME, ACTIONS_TYPE,
              transitionConstants.byValue(transition).getName());
          code.add("      result.add(AffordanceBuilder");
          code.add("        .linkTo(AffordanceBuilder.methodOn(%s.class).%s(%s))", controller, method, arguments);
          code.add("        .withRel(%s));", linkConstant);
          code.add("    }");
          code.ensureImport(packagePrefix + '.' + IMPL_PACKAGE, ACTIONS_TYPE);
        }
      }
    }
  }

  private String getArguments(String consumes, RadlCode radl, String resource, String method,
      Collection<String> availableParameters) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    String requiredParameters = stripParameterTypes(ParametersType.SUPPORT.parameters(consumes, radl, resource, method,
        NO_PARAMETER));
    for (String param : requiredParameters.split(",")) {
      String parameter = param.trim();
      if (parameter.isEmpty()) {
        continue;
      }
      result.append(prefix);
      prefix = ", ";
      if (availableParameters.contains(parameter)) {
        result.append(parameter);
      } else if (NO_PARAMETER.equals(parameter)) {
        result.append("null");
      } else {
        result.append(String.format("%s.getParameter(\"%s\")", RESPONSE_VAR, parameter));
      }
    }
    return result.toString();
  }

  private void addReturnTypeImport(String type, boolean importNoType, JavaCode code) {
    if (type.endsWith(DTO_SUFFIX)) {
      code.ensureImport(dtoPackage(type), type);
    } else if (NO_TYPE.equals(type) && importNoType) {
      code.ensureImport(RESPONSE_PACKAGE, RESPONSE_TYPE);
    } else if (UNKNOWN_OUTPUT_TYPE.equals(type)) {
      code.ensureImport(UNKNOWN_OUTPUT_TYPE_PACKAGE, UNKNOWN_OUTPUT_TYPE);
    } else if (SUPPORT_RESPONSE_TYPE.equals(type)) {
      code.ensureImport(packagePrefix + '.' + IMPL_PACKAGE, SUPPORT_RESPONSE_TYPE);
    }
  }

  private String dtoPackage(String dtoType) {
    return join(packagePrefix, toPackage(dtoType.substring(0, dtoType.length() - DTO_SUFFIX.length())));
  }

  private String returnType(String produces, RadlCode radl, String resource, String method) {
    final String noType = produces.isEmpty() ? NO_TYPE : UNKNOWN_OUTPUT_TYPE;
    String result = noType;
    for (String transition : radl.methodTransitions(resource, method)) {
      String propertyGroup = getOutputPropertyGroup(radl, transition);
      if (propertyGroup.isEmpty()) {
        result = noType;
      } else {
        String dto = getDtoClass(propertyGroup);
        if (noType.equals(result)) {
          result = dto;
        } else if (!result.equals(dto)) {
          result = UNKNOWN_OUTPUT_TYPE;
        }
      }
    }
    return result;
  }

  protected String getOutputPropertyGroup(RadlCode radl, String transition) {
    String result = "";
    for (String state : radl.transitionEnds(transition)) {
      result = radl.statePropertyGroup(state);
      if (!result.isEmpty()) {
        return result;
      }
    }
    return "";
  }

  private String parameterName(String consumes) {
    return consumes.isEmpty() ? "" : "input";
  }

  private String getMediaTypeConstant(String mediaType) {
    return API_TYPE + '.' + getLocalMediaTypeConstant(mediaType);
  }

  private String getLocalMediaTypeConstant(String mediaType) {
    String name = mediaType.startsWith(DEFAULT_MEDIA_TYPE)
        ? mediaType.substring(DEFAULT_MEDIA_TYPE.length()) : mediaType;
    return mediaTypeConstants.add(name, mediaType, null).getName();
  }

  private void addControllerImports(RadlCode radl, String resource, boolean addUris, Code controllerClass) {
    controllerClass.add("import org.springframework.beans.factory.annotation.Autowired;");
    boolean hasMethod = radl.methodNames(resource).iterator().hasNext();
    if (hasMethod || !radl.resourceLocation(resource).isEmpty()) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestMapping;");
    }
    if (hasMethod) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestMethod;");
    }
    controllerClass.add("import org.springframework.web.bind.annotation.RestController;");
    controllerClass.add("");
    controllerClass.add("import %s;", apiType());
    if (addUris) {
      controllerClass.add("import %s;", urisType());
    }
    controllerClass.add("");
    if (hasMethod) {
      controllerClass.add("import %s.%s.%s;", packagePrefix, IMPL_PACKAGE, SUPPORT_RESPONSE_TYPE);
    }
    controllerClass.add("");
    controllerClass.add("");
  }

  private String getControllerClassName(String resource) {
    return getClassName(resource) + "Controller";
  }

  private String getClassName(String name) {
    return toJavaIdentifier(name);
  }

  public String toJavaIdentifier(String text) {
    StringBuilder result = new StringBuilder(text);
    while (!Character.isJavaIdentifierStart(result.charAt(0))) {
      result.delete(0, 1);
    }
    upcase(result, 0);
    int index = 1;
    while (index < result.length()) {
      char ch = result.charAt(index);
      if (Character.isUpperCase(ch)) {
        result.setCharAt(index, Character.toLowerCase(ch));
      } else if (!Character.isJavaIdentifierPart(ch)) {
        while (index < result.length() && !Character.isJavaIdentifierPart(result.charAt(index))) {
          result.delete(index, index + 1);
        }
        if (index < result.length()) {
          upcase(result, index);
        }
      }
      index++;
    }
    return result.toString();
  }

  private static void upcase(StringBuilder builder, int index) {
    builder.setCharAt(index, Character.toUpperCase(builder.charAt(index)));
  }

  private String getConsumes(RadlCode radl, String resource, String method) {
    return getMediaTypes(radl.methodRequestRepresentations(resource, method), "consumes");
  }

  private String getMediaTypes(Iterable<String> mediaTypes, String prefix) {
    Iterator<String> iterator = mediaTypes.iterator();
    if (!iterator.hasNext()) {
      return "";
    }
    String mediaType = iterator.next();
    if (!iterator.hasNext() && defaultMediaType != null && mediaType.equals(defaultMediaType.name())) {
      // Explicit use of default media type
      mediaType = API_TYPE + '.' + DEFAULT_MEDIA_TYPE_CONSTANT;
    } else {
      mediaType = getMediaTypeConstant(mediaType);
    }
    StringBuilder result = new StringBuilder();
    result.append(", ").append(prefix).append(" = { ").append(mediaType);
    while (iterator.hasNext()) {
      result.append(", ").append(getMediaTypeConstant(iterator.next()));
    }
    result.append(" }");
    return result.toString();
  }

  private String getProduces(RadlCode radl, String resource, String method) {
    return getMediaTypes(radl.methodResponseRepresentations(resource, method), "produces");
  }

  private Code generateControllerSupport(RadlCode radl, String resource) {
    final JavaCode result = new JavaCode();
    addPackage(resource, result);
    result.add("");
    result.add("import org.springframework.stereotype.Service;");
    result.add("");
    result.add("");
    result.add("@Service");
    result.add("public class %s {", getControllerSupportClassName(resource));
    result.add("");
    for (String method : radl.methodNames(resource)) {
      addControllerSupportMethod(radl, resource, method, result);
    }
    result.add("}");
    return result;
  }

  private String getControllerSupportClassName(String resource) {
    return getControllerClassName(resource) + "Support";
  }

  private void addControllerSupportMethod(RadlCode radl, String resource, String method, JavaCode code) {
    String consumes = getConsumes(radl, resource, method);
    String produces = getProduces(radl, resource, method);
    String argName = parameterName(consumes);
    String args = ParametersType.SUPPORT.parameters(consumes, radl, resource, method, argName);
    String type = returnType(produces, radl, resource, method);
    boolean hasReturn = !NO_TYPE.equals(type);
    addReturnTypeImport(type, false, code);
    if (hasReturn) {
      addReturnTypeImport(SUPPORT_RESPONSE_TYPE, false, code);
    }
    code.add("  public %s<%s> %s(%s) {", SUPPORT_RESPONSE_TYPE, type, httpToJavaMethod(method), args);
    if (hasReturn) {
      code.add("    %s result = %s", type, getDummyReturnStatement(type, code));
      code.add("    // result.xxx = ...;");
      code.add("    %1$s<%2$s> %3$s = new %1$s<%2$s>(result);", SUPPORT_RESPONSE_TYPE, type, RESPONSE_VAR);
      code.add("    // %s.deny(%s.YYY);", RESPONSE_VAR, ACTIONS_TYPE);
      code.add("    // %s.setStatus(%s.ZZZ);", RESPONSE_VAR, STATUS_TYPE);
    } else {
      code.add("    %1$s<%2$s> %3$s = new %1$s<%2$s>(null);", SUPPORT_RESPONSE_TYPE, NO_TYPE, RESPONSE_VAR);
      code.add("    %s.setStatus(%s.NO_CONTENT);", RESPONSE_VAR, STATUS_TYPE);
      code.ensureImport(STATUS_TYPE_PACKAGE, STATUS_TYPE);
    }
    code.add("    return %s;", RESPONSE_VAR);
    code.add("  }");
    code.add("");
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

  private enum ParametersType {
    
    CONTROLLER(true, true), SUPPORT(true, false), NONE(false, false);
  
    private final boolean addPath;
    private final boolean addAnnotations;

    private ParametersType(boolean addPath, boolean addAnnotations) {
      this.addPath = addPath;
      this.addAnnotations = addAnnotations;
    }
    
    public String parameters(String consumes, RadlCode radl, String resource, String method, String argName) {
      StringBuilder result = new StringBuilder();
      String prefix = "";
      if (addPath) {
        String location = radl.resourceLocation(resource);
        if (location.contains("{")) {
          for (String segment : location.split("/")) {
            if (segment.startsWith("{") && segment.endsWith("}")) {
              result.append(prefix);
              String templateVariableName = segment.substring(1, segment.length() - 1);
              if (addAnnotations) {
                result.append("@PathVariable(\"").append(templateVariableName).append("\") ");
              }
              result.append("String ").append(Java.toIdentifier(templateVariableName, false));
              prefix = ", ";
            }
          }
        }
      }
      if (!consumes.isEmpty()) {
        result.append(prefix);
        if (addAnnotations) {
          result.append("@RequestBody ");
        }
        result.append(parameterType(consumes, radl, resource, method)).append(' ').append(argName);
      }
      return result.toString();
    }
  
    private String parameterType(String consumes, RadlCode radl, String resource, String method) {
      final String noType = consumes.isEmpty() ? NO_TYPE : UNKNOWN_INPUT_TYPE;
      String result = noType;
      for (String transition : radl.methodTransitions(resource, method)) {
        String propertyGroup = radl.transitionPropertyGroup(transition);
        if (propertyGroup.isEmpty()) {
          result = noType;
        } else {
          String dto = getDtoClass(propertyGroup);
          if (noType.equals(result)) {
            result = dto;
          } else if (!result.equals(dto)) {
            result = UNKNOWN_INPUT_TYPE;
          }
        }
      }
      return result;
    }

  }

}
