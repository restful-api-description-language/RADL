/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import radl.common.xml.ElementProcessor;
import radl.common.xml.Xml;


/**
 * Generate a state diagram from RADL.
 */
public class StateDiagramGenerator {

  private static final String STATE_DIAGRAM_IMAGE_FILE_NAME = "states.png";

  public File generateFrom(Document radl, File dir, File configuration) {
    return generateFrom(radl, new JGraphStateDiagram(), dir, configuration);
  }

  File generateFrom(Document radl, StateDiagram diagram, File dir, File configuration) {
    String service = getService(radl);
    diagram.setTitle(service);
    Properties properties = new Properties();
    if (configuration != null && configuration.exists()) {
      try (InputStream stream = new FileInputStream(configuration)) {
        properties.load(stream);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    addNodes(radl, diagram);
    addEdges(radl, diagram, properties);
    File result = new File(dir, STATE_DIAGRAM_IMAGE_FILE_NAME);
    diagram.toImage(result);
    return result;
  }

  private String getService(Document radl) {
    return radl.getDocumentElement().getAttribute("name");
  }

  private void addNodes(Document radl, final StateDiagram diagram) {
    try {
      diagram.addStartState("Start");
      Xml.processNestedElements(radl.getDocumentElement(), new ElementProcessor() {
        @Override
        public void process(Element stateElement) throws Exception {
          final String state = stateElement.getAttributeNS(null, "name");
          diagram.addState(state);
        }
      }, "states", "state");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addEdges(Document radl, final StateDiagram diagram, Properties properties) {
    String rawPrefixes = properties.getProperty("prefixes", "").trim();
    final String[] prefixes = rawPrefixes.isEmpty() ? new String[0] : rawPrefixes.split("=");
    try {
      Element statesElement = Xml.getFirstChildElement(radl.getDocumentElement(), "states");
      if (statesElement == null) {
        return;
      }
      Element startStateElement = Xml.getFirstChildElement(statesElement, "start-state");
      addTransitionsFrom(diagram, startStateElement, "Start", prefixes);
      Xml.processChildElements(statesElement, new ElementProcessor() {
        @Override
        public void process(Element stateElement) throws Exception {
          final String from = stateElement.getAttributeNS(null, "name");
          addTransitionsFrom(diagram, stateElement, from, prefixes);
        }
      }, "state");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void addTransitionsFrom(final StateDiagram diagram, final Element stateElement, final String from,
      final String[] prefixes) throws Exception {
    Xml.processNestedElements(stateElement, new ElementProcessor() {
      @Override
      public void process(Element transitionElement) throws Exception {
        String to = transitionElement.getAttributeNS(null, "to");
        String transition = transitionElement.getAttributeNS(null, "name");
        String linkRelation = getLinkRelationFor(stateElement.getOwnerDocument().getDocumentElement(), transition,
            prefixes);
        if (linkRelation == null) {
          transition = getUriFor(stateElement.getOwnerDocument().getDocumentElement(), transition);
        } else {
          transition = linkRelation;
        }
        diagram.addTransition(from, to, transition);
      }
    }, "transitions", "transition");
  }

  private String replacePrefixes(String[] prefixes, String value) {
    String result = value;
    for (int i = 0; i < prefixes.length; i += 2) {
      if (result.startsWith(prefixes[i + 1])) {
        result = prefixes[i] + result.substring(prefixes[i + 1].length());
      }
    }
    return result;
  }

  private String getUriFor(Node node, final String transition) throws Exception {
    final AtomicReference<String> result = new AtomicReference<>();
    Xml.processNestedElements(node, new ElementProcessor() {
      @Override
      public void process(Element resourceElement) throws Exception {
        Element locationElement = Xml.getFirstChildElement(resourceElement, "location");
        final String uri = locationElement.hasAttribute("uri") ? locationElement.getAttributeNS(null, "uri")
            : locationElement.getAttributeNS(null, "uri-template");
        Xml.processNestedElements(resourceElement, new ElementProcessor() {
          @Override
          public void process(Element methodElement) throws Exception {
            final String method = methodElement.getAttributeNS(null, "name");
            Xml.processNestedElements(methodElement, new ElementProcessor() {
              @Override
              public void process(Element transitionElement) throws Exception {
                if (transition.equals(transitionElement.getAttributeNS(null, "name"))) {
                  result.set(method + ' ' + uri);
                }
              }
            }, "transitions", "transition");
          }
        }, "methods", "method");
      }
    }, "resources", "resource");
    return result.get();
  }

  private String getLinkRelationFor(Node node, final String transition, final String[] prefixes) throws Exception {
    AtomicReference<String> result = new AtomicReference<>();
    setTransitionLinkRelation(node, transition, prefixes, result);
    if (result.get() != null) {
      addTransitionMethod(node, transition, result);
    }
    return result.get();
  }

  private void setTransitionLinkRelation(Node node, final String transition, final String[] prefixes,
      final AtomicReference<String> result) throws Exception {
    Xml.processNestedElements(node, new ElementProcessor() {
      @Override
      public void process(Element linkRelationElement) throws Exception {
        final String linkRelation = linkRelationElement.getAttributeNS(null, "name");
        Xml.processNestedElements(linkRelationElement, new ElementProcessor() {
          @Override
          public void process(Element transitionElement) throws Exception {
            if (transition.equals(transitionElement.getAttributeNS(null, "name"))) {
              result.set(replacePrefixes(prefixes, linkRelation));
            }
          }
        }, "transitions", "transition");
      }
    }, "link-relations", "link-relation");
  }

  private void addTransitionMethod(Node node, final String transition, final AtomicReference<String> result)
      throws Exception {
    final AtomicBoolean found = new AtomicBoolean();
    Xml.processNestedElements(node, new ElementProcessor() {
      @Override
      public void process(Element resourceElement) throws Exception {
        Xml.processNestedElements(resourceElement, new ElementProcessor() {
          @Override
          public void process(Element methodElement) throws Exception {
            final String method = methodElement.getAttributeNS(null, "name");
            Xml.processNestedElements(methodElement, new ElementProcessor() {
              @Override
              public void process(Element transitionElement) throws Exception {
                if (!found.get() && transition.equals(transitionElement.getAttributeNS(null, "name"))) {
                  found.set(true);
                  result.set(String.format("%s%n%s", method, result.get()));
                }
              }
            }, "transitions", "transition");
          }
        }, "methods", "method");
      }
    }, "resources", "resource");
  }

}
