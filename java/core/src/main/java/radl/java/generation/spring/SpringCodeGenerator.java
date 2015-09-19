/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import radl.common.xml.ElementProcessor;
import radl.common.xml.Xml;
import radl.core.code.Code;
import radl.core.generation.CodeGenerator;
import radl.java.code.Java;
import radl.java.code.JavaCode;


/**
 * Generates Java code for the Spring framework from a RADL document.
 */
public class SpringCodeGenerator implements CodeGenerator {

  private static final String DEFAULT_HEADER = "Generated from RADL.";
  private static final String IMPL_PACKAGE = "impl";
  private static final String API_PACKAGE = "api";
  private static final String BILLBOARD_URL = "BILLBOARD";
  private static final String CONSTANT_PREFIX_URL = "URL_";
  private static final String MEDIA_TYPE_JSON_LD = "application/ld+json";
  private static final String DEFAULT_MEDIA_TYPE = "application/";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String REF_ATTRIBUTE = "ref";
  private static final String URI_ATTRIBUTE = "uri";
  private static final String MEDIA_TYPES_ELEMENT = "media-types";
  private static final String DEFAULT_ATTRIBUTE = "default";
  private static final String MEDIA_TYPE_ELEMENT = "media-type";
  private static final String MEDIA_TYPE_REF_ATTRIBUTE = "media-type";
  private static final String MEDIA_TYPE_CONSTANT_PREFIX = "MEDIA_TYPE_";
  private static final String DEFAULT_MEDIA_TYPE_CONSTANT = MEDIA_TYPE_CONSTANT_PREFIX + "DEFAULT";
  private static final String RESOURCE_ELEMENT = "resource";
  private static final String LOCATION_ELEMENT = "location";
  private static final String URI_TEMPLATE_ATTRIBUTE = "uri-template";
  private static final String METHODS_ELEMENT = "methods";
  private static final String METHOD_ELEMENT = "method";
  private static final String REQUEST_ELEMENT = "request";
  private static final String RESPONSE_ELEMENT = "response";
  private static final String REPRESENTATIONS_ELEMENT = "representations";
  private static final String REPRESENTATION_ELEMENT = "representation";
  private static final String LINK_RELATION_ELEMENT = "link-relation";
  private static final String STATES_ELEMENT = "states";
  private static final String START_STATE_ELEMENT = "start-state";
  private static final String API_TYPE = "Api";
  private static final String URIS_TYPE = "Uris";
  private static final String TRANSITIONS_ELEMENT = "transitions";
  private static final String TRANSITION_ELEMENT = "transition";
  private static final String ERRORS_ELEMENT = "errors";
  private static final String ERROR_ELEMENT = "error";
  private static final String STATUS_CODE_ATTRIBUTE = "status-code";
  private static final String DEFAULT_STATUS_CODE = "400";
  private static final String ERROR_DTO_TYPE = "ErrorDto";
  private static final String IDENTIFIABLE_TYPE = "Identifiable";
  private static final Map<String, String> HTTP_STATUSES = new HashMap<String, String>();
  private static final Collection<String> FRAMEWORK_HANDLED_STATUSES = Arrays.asList("405", "406");
  private static final String PROPERTY_GROUP_ELEMENT = "property-group";
  private static final String PROPERTY_GROUPS_ELEMENT = PROPERTY_GROUP_ELEMENT + 's';
  private static final String PROPERTY_ELEMENT = "property";
  private static final String TYPE_ATTRIBUTE = "type";
  private static final String REPEATS_ATTRIBUTE = "repeats";
  private static final String SEMANTIC_ANNOTATION_PACKAGE = "de.escalon.hypermedia.hydra.mapping";
  private static final String SEMANTIC_ANNOTATION = "Expose";

  private final String packagePrefix;
  private final Map<String, Constant> errorConstants = new TreeMap<String, Constant>();
  private final Map<String, Constant> mediaTypeConstants = new TreeMap<String, Constant>();
  private final Map<String, Constant> uriConstants = new TreeMap<String, Constant>();
  private final String header;
  private String defaultMediaType;

  public SpringCodeGenerator(String packagePrefix) {
    this(packagePrefix, null);
  }

  public SpringCodeGenerator(String packagePrefix, String header) {
    this.packagePrefix = packagePrefix;
    this.header = header == null || header.trim().isEmpty() ? DEFAULT_HEADER : header;
    initHttpStatuses();
  }

  private void initHttpStatuses() {
    HTTP_STATUSES.put("400", "BAD_REQUEST");
    HTTP_STATUSES.put("401", "UNAUTHORIZED");
    HTTP_STATUSES.put("402", "PAYMENT_REQUIRED");
    HTTP_STATUSES.put("403", "FORBIDDEN");
    HTTP_STATUSES.put("404", "NOT_FOUND");
    HTTP_STATUSES.put("405", "METHOD_NOT_ALLOWED");
    HTTP_STATUSES.put("406", "NOT_ACCEPTABLE");
    HTTP_STATUSES.put("407", "PROXY_AUTHENTICATION_REQUIRED");
    HTTP_STATUSES.put("408", "REQUEST_TIMEOUT");
    HTTP_STATUSES.put("409", "CONFLICT");
    HTTP_STATUSES.put("410", "GONE");
    HTTP_STATUSES.put("411", "LENGTH_REQUIRED");
    HTTP_STATUSES.put("412", "PRECONDITION_FAILED");
    HTTP_STATUSES.put("413", "PAYLOAD_TOO_LARGE");
    HTTP_STATUSES.put("414", "URI_TOO_LONG");
    HTTP_STATUSES.put("415", "UNSUPPORTED_MEDIA_TYPE");
    HTTP_STATUSES.put("416", "REQUESTED_RANGE_NOT_SATISFIABLE");
    HTTP_STATUSES.put("417", "EXPECTATION_FAILED");
    HTTP_STATUSES.put("422", "UNPROCESSABLE_ENTITY");
    HTTP_STATUSES.put("423", "LOCKED");
    HTTP_STATUSES.put("424", "FAILED_DEPENDENCY");
    HTTP_STATUSES.put("426", "UPGRADE_REQUIRED");
    HTTP_STATUSES.put("428", "PRECONDITION_REQUIRED");
    HTTP_STATUSES.put("429", "TOO_MANY_REQUESTS");
    HTTP_STATUSES.put("431", "REQUEST_HEADER_FIELDS_TOO_LARGE");
    HTTP_STATUSES.put("500", "INTERNAL_SERVER_ERROR");
    HTTP_STATUSES.put("501", "NOT_IMPLEMENTED");
    HTTP_STATUSES.put("502", "BAD_GATEWAY");
    HTTP_STATUSES.put("503", "SERVICE_UNAVAILABLE");
    HTTP_STATUSES.put("504", "GATEWAY_TIMEOUT");
    HTTP_STATUSES.put("505", "HTTP_VERSION_NOT_SUPPORTED");
    HTTP_STATUSES.put("506", "VARIANT_ALSO_NEGOTIATES");
    HTTP_STATUSES.put("507", "INSUFFICIENT_STORAGE");
    HTTP_STATUSES.put("508", "LOOP_DETECTED");
    HTTP_STATUSES.put("509", "BANDWIDTH_LIMIT_EXCEEDED");
    HTTP_STATUSES.put("510", "NOT_EXTENDED");
    HTTP_STATUSES.put("511", "NETWORK_AUTHENTICATION_REQUIRED");
  }

  @Override
  public Iterable<Code> generateFrom(Document radl) {
    Collection<Code> result = new ArrayList<Code>();
    try {
      defaultMediaType = getDefaultMediaType(radl.getDocumentElement());
      generateSourcesForPropertyGroups(radl, result);
      generateSourcesForResources(radl, result);
      generateSourcesForErrors(radl, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void generateSourcesForPropertyGroups(Document radl, final Collection<Code> sources) throws Exception {
    Xml.processNestedElements(radl.getDocumentElement(), new ElementProcessor() {
      @Override
      public void process(Element propertySourceElement) throws Exception {
        addDtosFor(propertySourceElement, sources);
      }
    }, PROPERTY_GROUPS_ELEMENT, PROPERTY_GROUP_ELEMENT);
  }

  protected String addDtosFor(Element propertySourceElement, Collection<Code> sources) throws Exception {
    final Code code = new JavaCode();
    String name = getName(propertySourceElement);
    addPackage(name, code);
    code.add("");
    addSemanticAnnotationImport(propertySourceElement, code);
    addDtoImports(propertySourceElement, code);
    addSemanticAnnotation(propertySourceElement, "", code);
    String result = getDtoClass(name);
    code.add("public class %s {", result);
    code.add("");
    addDtoFields(propertySourceElement, code, sources);
    code.add("");
    code.add("}");
    sources.add(code);
    return result;
  }

  private String getDtoClass(String name) {
    return Java.toIdentifier(name) + "Dto";
  }

  private void addDtoImports(Element propertySourceElement, final Code code) throws Exception {
    final AtomicBoolean added = new AtomicBoolean();
    Xml.processDecendantElements(propertySourceElement, new ElementProcessor() {
      @Override
      public void process(Element propertyGroupElement) throws Exception {
        String name = propertyGroupElement.getAttributeNS(null, REF_ATTRIBUTE);
        if (name.isEmpty()) {
          name = getName(propertyGroupElement);
        }
        code.add("import %s.%s.%s;", packagePrefix, toPackage(name), getDtoClass(name));
        added.set(true);
      }
    }, PROPERTY_GROUP_ELEMENT);
    code.add("");
    if (added.get()) {
      code.add("");
    }
  }

  private void addSemanticAnnotationImport(Element propertySourceElement, Code code) throws Exception {
    if (isSemanticMediaType() && hasSemantics(propertySourceElement)) {
      code.add("import %s.%s;", SEMANTIC_ANNOTATION_PACKAGE, SEMANTIC_ANNOTATION);
      code.add("");
    }
  }

  private boolean isSemanticMediaType() {
    return MEDIA_TYPE_JSON_LD.equals(defaultMediaType);
  }

  private boolean hasSemantics(Element propertySourceElement) throws Exception {
    final AtomicBoolean result = new AtomicBoolean(doHasSemantics(propertySourceElement));
    Xml.processChildElements(propertySourceElement, new ElementProcessor() {
      @Override
      public void process(Element element) throws Exception {
        if (doHasSemantics(element)) {
          result.set(true);
        }
      }
    }, PROPERTY_ELEMENT, PROPERTY_GROUP_ELEMENT);
    return result.get();
  }

  private boolean doHasSemantics(Element element) {
    return !element.getAttributeNS(null, URI_ATTRIBUTE).isEmpty();
  }

  private void addSemanticAnnotation(Element element, String indent, final Code result) {
    if (isSemanticMediaType()) {
      String uri = element.getAttributeNS(null, URI_ATTRIBUTE);
      if (!uri.isEmpty()) {
        result.add("%s@%s(\"%s\")", indent, SEMANTIC_ANNOTATION, Java.toString(uri));
      }
    }
  }

  private void addDtoFields(Element propertySourceElement, final Code dto, final Collection<Code> sources)
      throws Exception {
    Xml.processChildElements(propertySourceElement, new ElementProcessor() {
      @Override
      public void process(Element propertyElement) throws Exception {
        addSemanticAnnotation(propertyElement, "  ", dto);
        dto.add("  public %s %s;", getType(propertyElement, sources), getName(propertyElement));
      }
    }, PROPERTY_ELEMENT, PROPERTY_GROUP_ELEMENT);
  }

  protected String getType(Element propertyElement, Collection<Code> sources) throws Exception {
    String result = null;
    if (PROPERTY_GROUP_ELEMENT.equals(propertyElement.getLocalName())) {
      String ref = propertyElement.getAttributeNS(null, REF_ATTRIBUTE);
      if (ref.isEmpty()) {
        result = addDtosFor(propertyElement, sources);
      } else {
        result = getDtoClass(ref);
      }
    }
    if (result == null) {
      String type = propertyElement.getAttributeNS(null, TYPE_ATTRIBUTE);
      result = type.isEmpty() ? "String" : type;
    }
    if (Boolean.parseBoolean(propertyElement.getAttributeNS(null, REPEATS_ATTRIBUTE))) {
      return result + "[]";
    }
    return result;
  }

  private void generateSourcesForErrors(Document radl, final Collection<Code> sources) throws Exception {
    Element errorsElement = Xml.getFirstChildElement(radl.getDocumentElement(), ERRORS_ELEMENT);
    if (errorsElement == null) {
      return;
    }
    sources.add(generateErrorDto());
    sources.add(generateIdentifiable());
    final JavaCode errorHandler = startErrorHandler();
    final Collection<String> errorHandlingMethods = new ArrayList<String>();
    Xml.processChildElements(errorsElement, new ElementProcessor() {
      @Override
      public void process(Element errorElement) throws Exception {
        String name = errorElement.getAttributeNS(null, NAME_ATTRIBUTE);
        String statusCode = errorElement.getAttributeNS(null, STATUS_CODE_ATTRIBUTE);
        if (statusCode.isEmpty()) {
          statusCode = DEFAULT_STATUS_CODE;
        }
        String documentation = getDocumentation(errorElement);
        JavaCode exceptionType = generateException(name, statusCode, documentation);
        sources.add(exceptionType);
        handleException(exceptionType, statusCode, errorHandlingMethods, errorHandler);
      }
    }, ERROR_ELEMENT);
    sources.add(endErrorHandler(errorHandler));
  }

  private Code generateErrorDto() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public class %s {", ERROR_DTO_TYPE);
    result.add("");
    result.add("  public String title;");
    result.add("  public String type;");
    result.add("");
    result.add("}");
    return result;
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

  protected JavaCode generateException(String name, String statusCode, String documentation) {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import %s;", apiType());
    result.add("");
    result.add("");
    String type = toExceptionTypeName(getErrorName(name));
    result.add("public class %s extends %s implements %s {", type, getBaseException(statusCode), IDENTIFIABLE_TYPE);
    result.add("");
    result.add("  public %s() {", type);
    result.add("    super(\"%s\");", getMessage(name, documentation));
    result.add("  }");
    result.add("");
    result.add("  public String getId() {");
    result.add("    return %s.%s;", API_TYPE, errorConstants.get(name).getName());
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
      path = path.substring(0,  path.length() - 1);
    }
    return path.substring(path.lastIndexOf('/') + 1);
  }

  private String toExceptionTypeName(String name) {
    return Java.toIdentifier(name + "Exception");
  }

  private String getMessage(String name, String documentation) {
    if (documentation == null || documentation.trim().isEmpty()) {
      return errorConstants.get(name).getName();
    }
    return Java.toString(documentation.trim());
  }

  private String getBaseException(String statusCode) {
    if ("400".equals(statusCode)) {
      return IllegalArgumentException.class.getSimpleName();
    }
    if ("500".equals(statusCode)) {
      return IllegalStateException.class.getSimpleName();
    }
    return RuntimeException.class.getSimpleName();
  }

  private JavaCode startErrorHandler() {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("import org.springframework.http.HttpStatus;");
    result.add("import org.springframework.http.ResponseEntity;");
    result.add("import org.springframework.web.bind.annotation.ControllerAdvice;");
    result.add("import org.springframework.web.bind.annotation.ExceptionHandler;");
    result.add("");
    result.add("");
    result.add("@ControllerAdvice");
    result.add("public class CentralErrorHandler {");
    result.add("");
    return result;
  }
  
  private void handleException(JavaCode exceptionType, String statusCode, Collection<String> errorHandlingMethods,
      JavaCode errorHandler) {
    if (FRAMEWORK_HANDLED_STATUSES.contains(statusCode)) {
      return;
    }
    String handledType = handledExceptionType(exceptionType);
    String method = exceptionTypeToMethod(handledType);
    if (errorHandlingMethods.contains(method)) {
      return;
    }
    errorHandlingMethods.add(method);
    errorHandler.add("  @ExceptionHandler({ %s.class })", handledType);
    errorHandler.add("  public ResponseEntity<ErrorDto> %s(%s e) {", method, handledType);
    errorHandler.add("    return error(e, HttpStatus.%s);", HTTP_STATUSES.get(statusCode));
    errorHandler.add("  }");
    errorHandler.add("");
  }

  private String handledExceptionType(JavaCode exceptionType) {
    String result = exceptionType.superTypeName();
    if ("RuntimeException".equals(result)) {
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
    errorHandler.add("  private ResponseEntity<ErrorDto> error(Exception e, HttpStatus statusCode) {");
    errorHandler.add("    ErrorDto error = new ErrorDto();");
    errorHandler.add("    error.type = ((%s)e).getId();", IDENTIFIABLE_TYPE);
    errorHandler.add("    error.title = e.getMessage();");
    errorHandler.add("    return new ResponseEntity<ErrorDto>(error, statusCode);");
    errorHandler.add("  }");
    errorHandler.add("");
    errorHandler.add("}");
    return errorHandler;
  }

  private void generateSourcesForResources(Document radl, final Collection<Code> sources) throws Exception {
    final String startTransition = getStartTransition(radl);
    Xml.processDecendantElements(radl.getDocumentElement(), new ElementProcessor() {
      @Override
      public void process(Element resourceElement) throws Exception {
        sources.add(generateController(resourceElement, startTransition));
        sources.add(generateService(resourceElement));
      }
    }, RESOURCE_ELEMENT);
    sources.add(generateApi(radl));
    sources.add(generateUris());
  }

  private String getDefaultMediaType(Element documentElement) {
    Element mediaTypesElement = Xml.getFirstChildElement(documentElement, MEDIA_TYPES_ELEMENT);
    if (mediaTypesElement == null) {
      return null;
    }
    String result = mediaTypesElement.getAttributeNS(null, DEFAULT_ATTRIBUTE);
    return result.isEmpty() ? null : result;
  }

  private Code generateUris() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", URIS_TYPE);
    addUris(result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addUris(Code code) {
    addConstants(filter(uriConstants, CONSTANT_PREFIX_URL + BILLBOARD_URL, false), "Resource locations", code);
  }

  private String getStartTransition(Document radl) {
    Element statesElement = Xml.getFirstChildElement(radl.getDocumentElement(), STATES_ELEMENT);
    Element startElement = Xml.getFirstChildElement(statesElement, START_STATE_ELEMENT);
    Element transitionsElement = Xml.getFirstChildElement(startElement, TRANSITIONS_ELEMENT);
    Element transitionElement = Xml.getFirstChildElement(transitionsElement, TRANSITION_ELEMENT);
    return transitionElement == null ? "" : transitionElement.getAttributeNS(null, NAME_ATTRIBUTE);
  }

  private Code generateApi(Document radl) throws Exception {
    Code result = new JavaCode();
    addPackage(API_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", API_TYPE);
    addBillboardUri(result);
    addMediaTypes(result);
    addLinkRelations(radl, result);
    addErrors(radl, result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addErrors(Document radl, Code code) throws Exception {
    addErrorConstants(radl, errorConstants);
    addConstants(errorConstants, "Error conditions", code);
  }

  private void addErrorConstants(Document radl, final Map<String, Constant> errorConstants) throws Exception {
    Xml.processDecendantElements(radl.getDocumentElement(), new ElementProcessor() {
      @Override
      public void process(Element errorElement) throws Exception {
        String value = errorElement.getAttributeNS(null, "name");
        String documentation = getDocumentation(errorElement);
        errorConstants.put(value, ensureConstant("ERROR_", getErrorName(value), value, documentation, errorConstants));
      }
    }, ERROR_ELEMENT);
  }

  private String getDocumentation(Element errorElement) {
    Element documentationElement = Xml.getFirstChildElement(errorElement, "documentation");
    if (documentationElement == null) {
      Element specificationElement = Xml.getFirstChildElement(errorElement, "specification");
      if (specificationElement == null) {
        return null;
      }
      return "See " + specificationElement.getAttributeNS(null, "href");
    }
    return documentationElement.getTextContent().replaceAll("\\s+", " ");
  }

  private void addBillboardUri(Code code) {
    addConstants(filter(uriConstants, CONSTANT_PREFIX_URL + BILLBOARD_URL, true), "Entry point", code);
  }

  private Map<String, Constant> filter(Map<String, Constant> values, String value, boolean include) {
    Map<String, Constant> result = new HashMap<String, Constant>();
    for (Entry<String, Constant> entry : values.entrySet()) {
      if (value.equals(entry.getValue().getName()) == include) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private void addLinkRelations(Document radl, Code code) throws Exception {
    Map<String, Constant> linkRelationConstants = new TreeMap<String, Constant>();
    addLinkRelationConstants(radl, linkRelationConstants);
    addConstants(linkRelationConstants, "Link relations", code);
  }

  private void addLinkRelationConstants(Document radl, final Map<String, Constant> linkRelationConstants)
      throws Exception {
    Xml.processDecendantElements(radl.getDocumentElement(), new ElementProcessor() {
      @Override
      public void process(Element linkRelationElement) throws Exception {
        String value = linkRelationElement.getAttributeNS(null, "name");
        String name = value.contains("/") ? value.substring(value.lastIndexOf('/') + 1) : value;
        String documentation = getDocumentation(linkRelationElement);
        ensureConstant("LINK_", name, value, documentation, linkRelationConstants);
      }
    }, LINK_RELATION_ELEMENT);
  }

  private void addMediaTypes(Code code) {
    addConstants(mediaTypeConstants, "Media types", code);
    if (defaultMediaType != null) {
      code.add("  String %s = %s;", DEFAULT_MEDIA_TYPE_CONSTANT, getLocalMediaTypeConstant(defaultMediaType));
    }
  }

  private void addConstants(Map<String, Constant> constants, String heading, Code code) {
    if (!constants.isEmpty()) {
      code.add("");
      code.add("");
      code.add("  // %s", heading);
      code.add("");
      for (Entry<String, Constant> entry : constants.entrySet()) {
        Constant constant = entry.getValue();
        if (constant.getComments().length > 0) {
          code.add("  /**");
          for (String comment : constant.getComments()) {
            code.add("   * %s", comment);
          }
          code.add("   */");
        }
        code.add("  String %s = \"%s\";", constant.getName(), entry.getKey());
      }
    }
  }

  private void addPackage(String name, Code code) {
    code.add("/*");
    for (String line : header.split("\n")) {
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

  private Code generateController(Element resourceElement, String startTransition) throws Exception {
    final Code result = new JavaCode();
    String name = getName(resourceElement);
    addPackage(name, result);
    result.add("");
    String uri = getUri(resourceElement);
    boolean addUris;
    String namePrefix;
    String constantName;
    String type;
    if (transitionsToStart(startTransition, resourceElement)) {
      namePrefix = CONSTANT_PREFIX_URL;
      constantName = BILLBOARD_URL;
      type = API_TYPE;
      addUris = false;
    } else {
      namePrefix = "";
      constantName = name;
      type = URIS_TYPE;
      addUris = true;
    }
    addControllerImports(resourceElement, addUris, result);
    result.add("@RestController");
    if (uri != null) {
      Constant constant = ensureConstant(namePrefix, constantName, uri, null, uriConstants);
      result.add(String.format("@RequestMapping(%s.%s)", type,  constant.getName()));
    }
    result.add("public class %s {", getControllerClassName(resourceElement));
    result.add("");
    result.add("  @Autowired");
    result.add("  private %s service;", getServiceClassName(resourceElement));
    result.add("");
    addMethods(resourceElement, new MethodAdder() {
      @Override
      public void addMethod(Element methodElement) throws Exception {
        addControllerMethod(methodElement, result);
      }
    });
    result.add("}");
    return result;
  }

  private boolean transitionsToStart(final String startState, Element resourceElement) throws Exception {
    final AtomicBoolean result = new AtomicBoolean();
    Xml.processNestedElements(resourceElement, new ElementProcessor() {
      @Override
      public void process(Element startElement) throws Exception {
        if (startElement.getAttributeNS(null, REF_ATTRIBUTE).equals(startState)) {
          result.set(true);
        }
      }
    }, METHODS_ELEMENT, METHOD_ELEMENT, TRANSITIONS_ELEMENT, TRANSITION_ELEMENT);
    return result.get();
  }

  private void addControllerMethod(Element methodElement, Code code) throws Exception {
    String method = getMethodName(methodElement);
    String consumes = getConsumes(methodElement);
    String produces = getProduces(methodElement);
    String argName = parameterName(consumes);
    code.add("  @RequestMapping(method = RequestMethod.%s%s%s)", method.toUpperCase(Locale.getDefault()),
        consumes, produces);
    code.add("  public %s %s(%s) {", returnValue(produces), method, parameters(consumes, argName));
    code.add("    %sservice.%s(%s);", returnStatement(produces), method, argName);
    code.add("  }");
    code.add("");
  }

  private String returnStatement(String produces) {
    return produces.isEmpty() ? "" : "return ";
  }

  private String returnValue(String produces) {
    return produces.isEmpty() ? "void" : "Object";
  }

  private String parameters(String consumes, String argName) {
    return consumes.isEmpty() ? "" : "@RequestBody String " + argName;
  }

  private String parameterName(String consumes) {
    return consumes.isEmpty() ? "" : "input";
  }

  private String getMediaTypeConstant(String mediaType) {
    return API_TYPE + '.' + getLocalMediaTypeConstant(mediaType);
  }

  private String getLocalMediaTypeConstant(String mediaType) {
    String name = mediaType.startsWith(DEFAULT_MEDIA_TYPE) ? mediaType.substring(DEFAULT_MEDIA_TYPE.length())
        : mediaType;
    return ensureConstant(MEDIA_TYPE_CONSTANT_PREFIX, name, mediaType, null, mediaTypeConstants).getName();
  }

  private Constant ensureConstant(String namePrefix, String name, String value, String documentation,
      Map<String, Constant> constants) {
    Constant result = constants.get(value);
    if (result == null) {
      String contantName = namePrefix + toJava(name.replace('/', '_').toUpperCase(Locale.getDefault()));
      result = new Constant(contantName, documentation);
      constants.put(value, result);
    }
    return result;
  }

  private String toJava(String value) {
    StringBuilder result = new StringBuilder(value);
    int i = 0;
    boolean nonJava = false;
    while (i < result.length()) {
      if (Character.isJavaIdentifierPart(result.charAt(i))) {
        nonJava = false;
        i++;
      } else {
        if (nonJava) {
          result.delete(i, i + 1);
        } else {
          result.setCharAt(i, '_');
          nonJava = true;
          i++;
        }
      }
    }
    return result.toString();
  }

  private void addControllerImports(Element resourceElement, boolean addUris, Code controllerClass) throws Exception {
    controllerClass.add("import %s;", apiType());
    if (addUris) {
      controllerClass.add("import %s;", urisType());
    }
    controllerClass.add("import org.springframework.beans.factory.annotation.Autowired;");
    controllerClass.add("import org.springframework.web.bind.annotation.RestController;");
    boolean hasMethod = hasMethod(resourceElement);
    if (hasMethod && hasMethod(resourceElement, "request")) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestBody;");
    }
    if (hasMethod || hasLocation(resourceElement)) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestMapping;");
    }
    if (hasMethod) {
      controllerClass.add("import org.springframework.web.bind.annotation.RequestMethod;");
    }
    controllerClass.add("");
    controllerClass.add("");
  }

  private boolean hasLocation(Element resourceElement) {
    return getUri(resourceElement) != null;
  }

  private boolean hasMethod(Element resourceElement) {
    Element methodsElement = Xml.getFirstChildElement(resourceElement, METHODS_ELEMENT);
    if (methodsElement == null) {
      return false;
    }
    return Xml.getFirstChildElement(methodsElement, METHOD_ELEMENT) != null;
  }

  private boolean hasMethod(Element resourceElement, String type) throws Exception {
    final AtomicBoolean result = new AtomicBoolean();
    Xml.processNestedElements(resourceElement, new ElementProcessor() {
      @Override
      public void process(Element typeElement) throws Exception {
        result.set(true);
      }
    }, METHODS_ELEMENT, METHOD_ELEMENT, type);
    return result.get();
  }

  private String getUri(Element resourceElement) {
    Element locationElement = Xml.getFirstChildElement(resourceElement, LOCATION_ELEMENT);
    if (locationElement == null) {
      return null;
    }
    String uri = locationElement.getAttributeNS(null, URI_ATTRIBUTE);
    if (!uri.isEmpty()) {
      return uri;
    }
    return locationElement.getAttributeNS(null, URI_TEMPLATE_ATTRIBUTE);
  }

  private String getControllerClassName(Element resourceElement) {
    return getClassName(resourceElement) + "Controller";
  }

  private String getClassName(Element resourceElement) {
    return toJavaIdentifier(getName(resourceElement));
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

  private String getName(Element resourceElement) {
    return resourceElement.getAttributeNS(null, NAME_ATTRIBUTE);
  }

  private void addMethods(Element resourceElement, final MethodAdder methodAdder) throws Exception {
    Xml.processNestedElements(resourceElement, new ElementProcessor() {
      @Override
      public void process(Element methodElement) throws Exception {
        methodAdder.addMethod(methodElement);
      }
    }, METHODS_ELEMENT, METHOD_ELEMENT);
  }

  private String getMethodName(Element methodElement) {
    return methodElement.getAttributeNS(null, NAME_ATTRIBUTE).toLowerCase(Locale.getDefault());
  }

  private String getConsumes(Element methodElement) throws Exception {
    return getMediaTypes(methodElement, REQUEST_ELEMENT, "consumes");
  }

  private String getMediaTypes(final Element methodElement, String messageType, String prefix) throws Exception {
    final Collection<String> mediaTypes = getMediaTypes(methodElement, messageType);
    if (mediaTypes.isEmpty()) {
      return "";
    }
    if (mediaTypes.size() == 1 && defaultMediaType != null
        && mediaTypes.iterator().next().equals(getMediaTypeConstant(defaultMediaType))) {
      // Explicit use of default media type
      mediaTypes.clear();
      mediaTypes.add(API_TYPE + '.' + DEFAULT_MEDIA_TYPE_CONSTANT);
    }
    StringBuilder result = new StringBuilder();
    result.append(", ").append(prefix).append(" = { ");
    String separator = "";
    for (String mediaType : mediaTypes) {
      result.append(separator).append(mediaType);
      separator = ", ";
    }
    result.append(" }");
    return result.toString();
  }

  private Collection<String> getMediaTypes(final Element methodElement, String messageType) throws Exception {
    final Collection<String> result = new LinkedHashSet<String>();
    Element messageElement = Xml.getFirstChildElement(methodElement, messageType);
    Xml.processNestedElements(messageElement, new ElementProcessor() {
      @Override
      public void process(Element representationElement) throws Exception {
        String mediaTypeName = representationElement.getAttributeNS(null, MEDIA_TYPE_REF_ATTRIBUTE);
        if (!mediaTypeName.isEmpty()) {
          String mediaType = getMediaTypeConstant(methodElement.getOwnerDocument().getDocumentElement(), mediaTypeName);
          if (mediaType != null) {
            result.add(mediaType);
          }
        }
      }
    }, REPRESENTATIONS_ELEMENT, REPRESENTATION_ELEMENT);
    if (result.isEmpty() && messageElement != null && defaultMediaType != null) {
      // No explicit representations defined, use default media type
      result.add(API_TYPE + '.' + DEFAULT_MEDIA_TYPE_CONSTANT);
    }
    return result;
  }

  private String getMediaTypeConstant(Element serviceElement, String mediaTypeName) throws Exception {
    Element mediaTypesElement = Xml.getFirstChildElement(serviceElement, MEDIA_TYPES_ELEMENT);
    if (mediaTypesElement == null) {
      return null;
    }
    Element mediaTypeElement = Xml.getChildElementByAttribute(mediaTypesElement,
        MEDIA_TYPE_ELEMENT, NAME_ATTRIBUTE, mediaTypeName);
    if (mediaTypeElement == null) {
      return null;
    }
    String mediaType = mediaTypeElement.getAttributeNS(null, NAME_ATTRIBUTE);
    return getMediaTypeConstant(mediaType);
  }

  private String getProduces(Element methodElement) throws Exception {
    return getMediaTypes(methodElement, RESPONSE_ELEMENT, "produces");
  }

  private Code generateService(Element resourceElement) throws Exception {
    final Code result = new JavaCode();
    addPackage(getName(resourceElement), result);
    result.add("");
    result.add("import org.springframework.stereotype.Service;");
    result.add("");
    result.add("");
    result.add("@Service");
    result.add("public class %s {", getServiceClassName(resourceElement));
    result.add("");
    addMethods(resourceElement, new MethodAdder() {
      @Override
      public void addMethod(Element methodElement) throws Exception {
        addServiceMethod(methodElement, result);
      }
    });
    result.add("}");
    return result;
  }

  private String getServiceClassName(Element resourceElement) {
    return getClassName(resourceElement) + "Service";
  }

  private void addServiceMethod(Element methodElement, Code code) throws Exception {
    String method = getMethodName(methodElement);
    String consumes = getConsumes(methodElement);
    String produces = getProduces(methodElement);
    String args = consumes.isEmpty() ? "" : "Object input";
    String returns = produces.isEmpty() ? "void" : "Object";
    String returnStatement = produces.isEmpty() ? "" : "return null; ";
    code.add("  public %s %s(%s) {", returns, method, args);
    // Make sure the comment is not viewed as a to-do in *this* code base
    code.add("    %s// TO%s: Implement", returnStatement, "DO");
    code.add("  }");
    code.add("");
  }


  private interface MethodAdder {

    void addMethod(Element methodElement) throws Exception;

  }


  private static class Constant {

    private final String name;
    private final String comments;

    public Constant(String name, String comments) {
      this.name = name;
      this.comments = comments;
    }

    public String getName() {
      return name;
    }

    public String[] getComments() {
      return comments == null ? new String[0] : comments.split("\n");
    }

  }


}
