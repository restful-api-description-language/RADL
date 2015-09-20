/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import radl.core.Radl;


/**
 * Code that follows the XML syntax and RADL schema.
 */
public class RadlCode extends XmlCode {

  private static final String STATES_PATH = "//radl:states/radl:state";
  private static final String TRANSITION_PATH = "//radl:states/radl:*/radl:transitions/radl:transition";
  private static final String LINK_RELATIONS_PATH = "//radl:link-relations/radl:link-relation";
  private static final String ERRORS_PATH = "//radl:errors/radl:error";
  
  private transient Iterable<String> states;
  private final transient Map<String, Iterable<String>> methodsByResource = new HashMap<String, Iterable<String>>();

  public RadlCode() {
    super();
    init();
  }

  private void init() {
    addNamespace("radl", Radl.NAMESPACE_URI);
  }
  
  public RadlCode(Document document) {
    super(document);
    init();
  }

  public String service() {
    return one("xs:string(/radl:service/@name)", String.class);
  }

  public Iterable<String> resourceNames() {
    return getElementsName("//radl:resource");
  }

  private Iterable<String> getElementsName(String elementXPath, Object... args) {
    return elementsAttribute("name", elementXPath, args);
  }

  private Iterable<String> getElementsRef(String elementXPath, Object... args) {
    return elementsAttribute("ref", elementXPath, args);
  }

  public Iterable<String> methodNames(String resource) {
    Iterable<String> result = methodsByResource.get(resource);
    if (result == null) {
      result = getElementsName("//radl:resource[@name='%s']//radl:method", resource);
      methodsByResource.put(resource, result);
    }
    return result;
  }

  public Iterable<String> stateNames() {
    Iterable<String> result = states;
    if (result == null) {
      result = getElementsName(STATES_PATH);
      if (multiple("//radl:states/radl:start-state", Element.class).iterator().hasNext()) {
        result = combine(result, "");
      }
      states = result;
    }
    return result;
  }

  private Iterable<String> combine(Iterable<String> first, String second) {
    List<String> collection = (List<String>)first;
    collection.add(second);
    Collections.sort(collection);
    return collection;
  }

  public Iterable<String> stateTransitionNames(String state) {
    String xpath = state.isEmpty() ? "//radl:states/radl:start-state/radl:transitions/radl:transition"
        : statePath(state) + "/radl:transitions/radl:transition";
    return getElementsName(xpath, state);
  }
  
  public String statePath(String name) {
    return STATES_PATH + namePath(name);
  }

  public String statePropertyGroup(String state) {
    return optional(elementsAttribute("property-group", statePath(state)));
  }
  
  private String optional(Iterable<String> values) {
    Iterator<String> result = values.iterator();
    return result.hasNext() ? result.next() : "";
  }

  public Iterable<String> transitionEnds(String transition) {
    return elementsAttribute("to", transitionPath(transition), transition);
  }

  private String transitionPath(String name) {
    return TRANSITION_PATH + namePath(name);
  }

  public String transitionPropertyGroup(String transition) {
    return optional(elementsAttribute("property-group", transitionPath(transition) + "/radl:input"));
  }

  public Iterable<String> methodTransitions(String resource, String method) {
    String xpath = "//radl:resource[@name='%s']//radl:method[@name='%s']//radl:transition";
    return getElementsRef(xpath, resource, method);
  }

  public String resourceLocation(String resource) {
    String xpath = "//radl:resource[@name='%s']/radl:location";
    Iterator<String> found = elementsAttribute("uri", xpath, resource).iterator();
    if (found.hasNext()) {
      String result = found.next();
      if (!result.isEmpty()) {
        return result;
      }
    }
    found = elementsAttribute("uri-template", xpath, resource).iterator();
    return found.hasNext() ? found.next() : "";
  }

  public Iterable<String> methodRequestRepresentations(String resource, String method) {
    return methodRepresentations(resource, method, "request");
  }

  private Iterable<String> methodRepresentations(String resource, String method, String type) {
    String xpath = String.format("//radl:resource[@name='%s']//radl:method[@name='%s']/radl:%s", resource, method, type);
    if (!multiple(xpath, Element.class).iterator().hasNext()) {
      // No representations of this type
      return Collections.emptyList();
    }
    Iterable<String> result = elementsAttribute("media-type", xpath + "//radl:representation");
    if (result.iterator().hasNext()) {
      // Explicit representations
      return result;
    }
    // Implicit representation of default media type
    String mediaType = defaultMediaTypeName();
    return mediaType == null ? Collections.<String>emptyList() : Collections.singletonList(mediaType);
  }

  public String defaultMediaTypeName() {
    Iterator<String> result = elementsAttribute("default", "//radl:media-types").iterator();
    return result.hasNext() ? result.next() : null;
  }

  public MediaType defaultMediaType() {
    String result = defaultMediaTypeName();
    return result == null ? null : new MediaType(result);
  }

  public Iterable<String> methodResponseRepresentations(String resource, String method) {
    return methodRepresentations(resource, method, "response");
  }

  public Iterable<String> mediaTypeNames() {
    return getElementsName("//radl:media-types/radl:media-type");
  }

  public Iterable<String> linkRelationNames() {
    return getElementsName(LINK_RELATIONS_PATH);
  }

  public String linkRelationDocumentation(String name) {
    return documentation(linkRelationPath(name));
  }

  private String linkRelationPath(String name) {
    return LINK_RELATIONS_PATH + namePath(name);
  }

  private String namePath(String name) {
    return "[@name='" + name + "']";
  }
  
  private String documentation(String path) {
    Iterator<Element> specification = multiple(path + "/radl:specification", Element.class).iterator();
    if (specification.hasNext()) {
      return "See " + specification.next().getAttributeNS(null, "href");
    }
    Iterator<Element> documentation = multiple(path + "/radl:documentation", Element.class).iterator();
    if (!documentation.hasNext()) {
      return null;
    }
    return documentation.next().getTextContent().replaceAll("\\s+", " ");
  }

  public Iterable<String> linkRelationTransitions(String linkRelation) {
    return getElementsRef(linkRelationPath(linkRelation) + "//radl:transition");
  }

  public boolean hasHyperMediaTypes() {
    for (MediaType mediaType : mediaTypes()) {
      if (mediaType.isHyperMediaType()) {
        return true;
      }
    }
    return false;
  }

  public Iterable<MediaType> mediaTypes() {
    Collection<MediaType> result = new ArrayList<MediaType>();
    for (String name : mediaTypeNames()) {
      result.add(new MediaType(name));
    }
    return result;
  }

  public PropertyGroups propertyGroups() {
    NestedXml result = nested("//radl:property-groups", "radl:property-group", "name");
    return result == null ? null : new PropertyGroups(result);
  }

  public Iterable<String> errors() {
    return getElementsName(ERRORS_PATH);
  }

  public String errorStatus(String name) {
    Element errorElement = one(errorPath(name), Element.class);
    return errorElement.getAttributeNS(null, "status-code");
  }

  private String errorPath(String name) {
    return ERRORS_PATH + namePath(name);
  }

  public String errorDocumentation(String name) {
    return documentation(errorPath(name));
  }

}
