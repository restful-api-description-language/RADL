/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import radl.core.Radl;


/**
 * Code that follows the XML syntax and RADL schema.
 */
public class RadlCode extends XmlCode {

  private transient Iterable<String> states;
  private final transient Map<String, Iterable<String>> methodsByResource = new HashMap<String, Iterable<String>>();

  public RadlCode() {
    super();
    addNamespace("radl", Radl.NAMESPACE_URI);
  }

  public String service() {
    return one("xs:string(/radl:service/@name)", String.class);
  }

  public Iterable<String> resourceNames() {
    return getElementsName("//radl:resource");
  }

  private Iterable<String> getElementsName(String elementXPath, Object... args) {
    return getElementsAttribute("name", elementXPath, args);
  }

  private Iterable<String> getElementsAttribute(String attribute, String elementXPath, Object... args) {
    List<String> result = new ArrayList<String>();
    for (Element element : multiple(String.format(elementXPath, args), Element.class)) {
      result.add(attr(element, attribute));
    }
    Collections.sort(result);
    return result;
  }

  private Iterable<String> getElementsRef(String elementXPath, Object... args) {
    return getElementsAttribute("ref", elementXPath, args);
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
      result = getElementsName("//radl:states/radl:state");
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
        : "//radl:states/radl:state[@name='%s']/radl:transitions/radl:transition";
    return getElementsName(xpath, state);
  }

  public Iterable<String> transitionEnds(String transition) {
    return getElementsAttribute("to", "//radl:states/radl:*/radl:transitions/radl:transition[@name='%s']",
        transition);
  }

  public Iterable<String> methodTransitions(String resource, String method) {
    String xpath = "//radl:resource[@name='%s']//radl:method[@name='%s']//radl:transition";
    return getElementsRef(xpath, resource, method);
  }

  public String resourceLocation(String resource) {
    String xpath = "//radl:resource[@name='%s']/radl:location";
    Iterator<String> found = getElementsAttribute("uri", xpath, resource).iterator();
    if (found.hasNext()) {
      String result = found.next();
      if (!result.isEmpty()) {
        return result;
      }
    }
    found = getElementsAttribute("uri-template", xpath, resource).iterator();
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
    Iterable<String> result = getElementsAttribute("media-type", xpath + "//radl:representation");
    if (result.iterator().hasNext()) {
      // Explicit representations
      return result;
    }
    // Implicit representation of default media type
    String mediaType = defaultMediaType();
    return mediaType == null ? Collections.<String>emptyList() : Collections.singletonList(mediaType);
  }

  public String defaultMediaType() {
    Iterator<String> result = getElementsAttribute("default", "//radl:media-types").iterator();
    return result.hasNext() ? result.next() : null;
  }

  public Iterable<String> methodResponseRepresentations(String resource, String method) {
    return methodRepresentations(resource, method, "response");
  }

  public Iterable<String> mediaTypeNames() {
    return getElementsName("//radl:media-types/radl:media-type");
  }

  public Iterable<String> linkRelationNames() {
    return getElementsName("//radl:link-relations/radl:link-relation");
  }

  public Iterable<String> linkRelationTransitions(String linkRelation) {
    return getElementsRef("//radl:link-relations/radl:link-relation[@name='%s']//radl:transition", linkRelation);
  }

}
