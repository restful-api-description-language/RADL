/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import radl.java.code.JavaCode;


/**
 * * Generates Java code for the Spring framework from a RADL document.
 */
public class SpringCodeGenerator implements CodeGenerator {

  private static final String DEFAULT_HEADER = "Generated from RADL.";
  private static final String IMPL_PACKAGE = "impl";
  private static final String API_PACKAGE = "api";
  private static final String BILLBOARD_URL = "BILLBOARD";
  private static final String CONSTANT_PREFIX_URL = "URL_";
  private static final String DEFAULT_MEDIA_TYPE = "application/";
  private static final String NAME_ATTRIBUTE = "name";
  private static final String REF_ATTRIBUTE = "ref";
  private static final String MEDIA_TYPES_ELEMENT = "media-types";
  private static final String DEFAULT_ATTRIBUTE = "default";
  private static final String MEDIA_TYPE_ELEMENT = "media-type";
  private static final String MEDIA_TYPE_REF_ATTRIBUTE = "media-type";
  private static final String RESOURCE_ELEMENT = "resource";
  private static final String LOCATION_ELEMENT = "location";
  private static final String LOCATION_URI_ATTRIBUTE = "uri";
  private static final String LOCATION_URI_TEMPLATE_ATTRIBUTE = "uri-template";
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

  private final String packagePrefix;
  private final Map<String, String> mediaTypeConstants = new TreeMap<String, String>();
  private final Map<String, String> uriConstants = new TreeMap<String, String>();
  private final String header;

  public SpringCodeGenerator(String packagePrefix) {
    this(packagePrefix, null);
  }

  public SpringCodeGenerator(String packagePrefix, String header) {
    this.packagePrefix = packagePrefix;
    this.header = header == null || header.trim().isEmpty() ? DEFAULT_HEADER : header;
  }

  @Override
  public Iterable<Code> generateFrom(Document radl) {
    Collection<Code> result = new ArrayList<Code>();
    try {
      generateSourcesForResources(radl, result);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
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
    addLinkRelations(radl, result);
    addMediaTypes(result);
    addBillboardUri(result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addBillboardUri(Code code) {
    addConstants(filter(uriConstants, CONSTANT_PREFIX_URL + BILLBOARD_URL, true), "Entry point", code);
  }

  private Map<String, String> filter(Map<String, String> values, String value, boolean include) {
    Map<String, String> result = new HashMap<String, String>();
    for (Entry<String, String> entry : values.entrySet()) {
      if (value.equals(entry.getValue()) == include) {
        result.put(entry.getKey(), entry.getValue());
      }
    }
    return result;
  }

  private void addLinkRelations(Document radl, final Code code) throws Exception {
    Map<String, String> linkRelationConstants = new TreeMap<String, String>();
    addLinkRelationConstants(radl, linkRelationConstants);
    addConstants(linkRelationConstants, "Link relations", code);
  }

  private void addLinkRelationConstants(Document radl, final Map<String, String> linkRelationConstants)
      throws Exception {
    Xml.processDecendantElements(radl.getDocumentElement(), new ElementProcessor() {
      @Override
      public void process(Element linkRelationElement) throws Exception {
        String value = linkRelationElement.getAttributeNS(null, "name");
        String name = value.contains("/") ? value.substring(value.lastIndexOf('/') + 1) : value;
        ensureConstant("LINK_", name, value, linkRelationConstants);
      }
    }, LINK_RELATION_ELEMENT);
  }

  private void addMediaTypes(Code code) {
    addConstants(mediaTypeConstants, "Media types", code);
  }

  private void addConstants(Map<String, String> constants, String heading, Code code) {
    if (!constants.isEmpty()) {
      code.add("");
      code.add("  // %s", heading);
      for (Entry<String, String> entry : constants.entrySet()) {
        code.add("  String %s = \"%s\";", entry.getValue(), entry.getKey());
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
    String name = getResourceName(resourceElement);
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
    result.add("@Controller");
    if (uri != null) {
      String constant = ensureConstant(namePrefix, constantName, uri, uriConstants);
      result.add(String.format("@RequestMapping(%s.%s)", type,  constant));
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
    if (!produces.isEmpty()) {
      code.add("  @ResponseBody");
    }
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
    String name = mediaType.startsWith(DEFAULT_MEDIA_TYPE) ? mediaType.substring(DEFAULT_MEDIA_TYPE.length())
        : mediaType;
    return API_TYPE + '.' + ensureConstant("MEDIA_", name, mediaType, mediaTypeConstants);
  }

  private String ensureConstant(String namePrefix, String name, String value, Map<String, String> constants) {
    String result = constants.get(value);
    if (result == null) {
      result = namePrefix + toJava(name.replace('/', '_').toUpperCase(Locale.getDefault()));
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
    controllerClass.add("import org.springframework.stereotype.Controller;");
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
    if (hasMethod && hasMethod(resourceElement, "response")) {
      controllerClass.add("import org.springframework.web.bind.annotation.ResponseBody;");
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
    String uri = locationElement.getAttributeNS(null, LOCATION_URI_ATTRIBUTE);
    if (!uri.isEmpty()) {
      return uri;
    }
    return locationElement.getAttributeNS(null, LOCATION_URI_TEMPLATE_ATTRIBUTE);
  }

  private String getControllerClassName(Element resourceElement) {
    return getClassName(resourceElement) + "Controller";
  }

  private String getClassName(Element resourceElement) {
    return toJavaIdentifier(getResourceName(resourceElement));
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

  private String getResourceName(Element resourceElement) {
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
    final Collection<String> mediaTypes = new ArrayList<String>();
    Element messageElement = Xml.getFirstChildElement(methodElement, messageType);
    Xml.processNestedElements(messageElement, new ElementProcessor() {
      @Override
      public void process(Element representationElement) throws Exception {
        String mediaTypeName = representationElement.getAttributeNS(null, MEDIA_TYPE_REF_ATTRIBUTE);
        if (!mediaTypeName.isEmpty()) {
          String mediaType = getMediaType(methodElement.getOwnerDocument().getDocumentElement(), mediaTypeName);
          if (mediaType != null) {
            mediaTypes.add(mediaType);
          }
        }
      }
    }, REPRESENTATIONS_ELEMENT, REPRESENTATION_ELEMENT);
    if (mediaTypes.isEmpty() && messageElement != null) {
      // No explicit representations defined, look for default media type
      Element mediaTypesElement = Xml.getFirstChildElement(methodElement.getOwnerDocument().getDocumentElement(),
          MEDIA_TYPES_ELEMENT);
      String defaultMediaType = mediaTypesElement.getAttributeNS(null, DEFAULT_ATTRIBUTE);
      if (!defaultMediaType.isEmpty()) {
        mediaTypes.add(getMediaTypeConstant(defaultMediaType));
      }
    }
    if (mediaTypes.isEmpty()) {
      return "";
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

  private String getMediaType(Element serviceElement, String mediaTypeName) throws Exception {
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
    addPackage(getResourceName(resourceElement), result);
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
    // Make sure the comment is not viewed as a to-do in this code base
    code.add("    %s// TO%s: Implement", returnStatement, "DO");
    code.add("  }");
    code.add("");
  }


  private interface MethodAdder {

    void addMethod(Element methodElement) throws Exception;

  }

}
