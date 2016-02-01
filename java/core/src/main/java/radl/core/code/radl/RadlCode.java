/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code.radl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import radl.core.Radl;
import radl.core.code.xml.NestedXml;
import radl.core.code.xml.XmlCode;

/**
 * Code that follows the XML syntax and RADL schema. This class is not thread safe.
 */
public class RadlCode extends XmlCode {

  private static final String START_STATE = "";
  private static final String STATES_PATH = "//radl:states/radl:state";
  private static final String TRANSITION_PATH = "//radl:states/radl:*/radl:transitions/radl:transition";
  private static final String LINK_RELATIONS_PATH = "//radl:link-relations/radl:link-relation";
  private static final String ERRORS_PATH = "//radl:errors/radl:error";
  
  private Iterable<String> states;
  private Iterable<String> resources;
  private Iterable<String> linkRelations;
  private final Map<String, Iterable<String>> methodsByResource = new HashMap<String, Iterable<String>>();
  private final Map<String, Iterable<String>> transitionsByState = new HashMap<String, Iterable<String>>();
  private final Map<String, Iterable<String>> transitionEndsByName = new HashMap<String, Iterable<String>>();
  private final Map<String, Iterable<String>> transitionsByLink = new HashMap<String, Iterable<String>>();
  private final Table<String, String, Iterable<String>> transitionsByMethod = HashBasedTable.create();

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
    Iterable<String> resources = this.resources;
    if (resources == null) {
      resources = elementsName("//radl:resource");
      this.resources = resources;
    }
    return resources;
  }

  public Iterable<String> methodNames(String resource) {
    Iterable<String> result = methodsByResource.get(resource);
    if (result == null) {
      result = elementsName("//radl:resource[@name='%s']//radl:method", resource);
      methodsByResource.put(resource, result);
    }
    return result;
  }

  public Iterable<String> stateNames() {
    Iterable<String> result = states;
    if (result == null) {
      result = elementsName(STATES_PATH);
      if (multiple("//radl:states/radl:start-state", Element.class).iterator().hasNext()) {
        result = combine(result, START_STATE);
      }
      states = result;
    }
    return result;
  }

  public boolean isStartState(String state) {
    return START_STATE.equals(state);
  }

  public Iterable<String> stateTransitionNames(String state) {
    Iterable<String> result = transitionsByState.get(state);
    if (result == null) {
      String xpath = state.isEmpty() ? "//radl:states/radl:start-state/radl:transitions/radl:transition"
          : statePath(state) + "/radl:transitions/radl:transition";
      result = elementsName(xpath, state);
      transitionsByState.put(state, result);
    }
    return result;
  }

  public String statePropertyGroup(String state) {
    return optional(elementsAttribute("property-group", statePath(state)));
  }

  public Iterable<String> transitionEnds(String transition) {
    Iterable<String> ends = transitionEndsByName.get(transition);
    if (ends == null) {
      ends = elementsAttribute("to", transitionPath(transition), transition);
      transitionEndsByName.put(transition, ends);
    }
    return ends;
  }

  public String transitionPropertyGroup(String transition) {
    return optional(elementsAttribute("property-group", transitionPath(transition) + "/radl:input"));
  }

  public Iterable<String> transitionImplementations(String transition) {
    return elementsName(LINK_RELATIONS_PATH + "[radl:transitions/radl:transition[@ref='" + transition + "']" + "]");
  }

  public Iterable<String> methodTransitions(String resource, String method) {
    Iterable<String> result = transitionsByMethod.get(resource, method);
    if (result == null) {
      String xpath = "//radl:resource[@name='%s']//radl:method[@name='%s']//radl:transition";
      result = elementsRef(xpath, resource, method);
      transitionsByMethod.put(resource, method, result);
    }

    return result;
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
    return elementsName("//radl:media-types/radl:media-type");
  }

  public Iterable<String> linkRelationNames() {
    Iterable<String> links = linkRelations;
    if (links == null) {
      links = elementsName(LINK_RELATIONS_PATH);
      linkRelations = links;
    }
    return links;
  }

  public String linkRelationDocumentation(String name) {
    return documentation(linkRelationPath(name));
  }

  public Iterable<String> linkRelationTransitions(String linkRelation) {
    Iterable<String> transitions = transitionsByLink.get(linkRelation);
    if (transitions == null) {
      transitions = elementsRef(linkRelationPath(linkRelation) + "//radl:transition");
      transitionsByLink.put(linkRelation, transitions);
    }
    return transitions;
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
    Collection<MediaType> result = new ArrayList<>();
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
    return elementsName(ERRORS_PATH);
  }

  public int errorStatus(String name) {
    Element errorElement = one(errorPath(name), Element.class);
    String result = errorElement.getAttributeNS(null, "status-code");
    return result.isEmpty() ? -1 : Integer.parseInt(result);
  }

  public String errorDocumentation(String name) {
    return documentation(errorPath(name));
  }

  public ResourceMethod transitionMethod(String transition) {
    String resource = optional(elementsName("//radl:resource[.//radl:transition[@ref='%s']]", transition));
    for (String method : methodNames(resource)) {
      if (((Collection<String>)methodTransitions(resource, method)).contains(transition)) {
        return new ResourceMethod(resource, method);
      }
    }
    return null;
  }

  private Iterable<String> elementsName(String elementXPath, Object... args) {
    return elementsAttribute("name", elementXPath, args);
  }

  private Iterable<String> elementsRef(String elementXPath, Object... args) {
    return elementsAttribute("ref", elementXPath, args);
  }

  private Iterable<String> combine(Iterable<String> first, String second) {
    List<String> collection = (List<String>)first;
    collection.add(second);
    Collections.sort(collection);
    return collection;
  }

  private String statePath(String name) {
    return STATES_PATH + namePath(name);
  }

  private String optional(Iterable<String> values) {
    Iterator<String> result = values.iterator();
    return result.hasNext() ? result.next() : START_STATE;
  }

  private String transitionPath(String name) {
    return TRANSITION_PATH + namePath(name);
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

  private String errorPath(String name) {
    return ERRORS_PATH + namePath(name);
  }

  public static class ResourceMethod {

    private final String resource;
    private final String method;

    public ResourceMethod(String resource, String method) {
      this.resource = resource;
      this.method = method;
    }

    public String getResource() {
      return resource;
    }

    public String getMethod() {
      return method;
    }

    @Override
    public String toString() {
      return String.format("%s.%s", resource, method);
    }

  }


  public boolean hasErrors() {
    return errors().iterator().hasNext();
  }

}
