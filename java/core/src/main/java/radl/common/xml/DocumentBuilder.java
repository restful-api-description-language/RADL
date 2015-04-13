/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import radl.common.io.StringStream;


/**
 * Builder for XML DOM {@linkplain Document}s.
 */
public class DocumentBuilder {

  public static DocumentBuilder newDocument() {
    return new DocumentBuilder(Xml.newDocument());
  }

  private final Document document;
  private Node current;
  private String namespaceUri;

  public DocumentBuilder(Node node) {
    if (node instanceof Document) {
      this.document = (Document)node;
      this.current = document;
    } else if (node instanceof Element) {
      this.document = node.getOwnerDocument();
      this.current = node;
    } else {
      throw new IllegalArgumentException("Unhandled node type: " + node.getNodeType());
    }
  }

  public Node getCurrent() {
    return current;
  }

  public Document build() {
    return document;
  }

  public DocumentBuilder namespace(String uri) {
    namespaceUri = uri;
    return this;
  }

  public DocumentBuilder element(String name) {
    return element(document.createElementNS(namespaceUri, name));
  }

  private DocumentBuilder element(Element element) {
    current = current.appendChild(element);
    return this;
  }

  public DocumentBuilder end() {
    current = current.getParentNode();
    return this;
  }

  public DocumentBuilder attribute(String name, String value) {
    return attribute(name, value, null);
  }

  public DocumentBuilder attribute(String name, String value, String namespace) {
    ((Element)current).setAttributeNS(namespace, name, value);
    return this;
  }

  public DocumentBuilder text(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Missing text");
    }
    current.appendChild(document.createTextNode(value));
    return this;
  }

  public DocumentBuilder element(String name, String text) {
    if (text == null) {
      throw new IllegalArgumentException("Missing text for element " + name);
    }
    return element(name).text(text).end();
  }

  public DocumentBuilder importXml(String xml) {
    if (xml == null) {
      throw new IllegalArgumentException("Missing XML to import");
    }
    try {
      Document doc = Xml.parse(new StringStream(xml));
      return importXml(removeWhitespace(doc.getDocumentElement()));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Element removeWhitespace(Element element) {
    Node node = element.getFirstChild();
    while (node != null) {
      switch (node.getNodeType()) {
        case Node.TEXT_NODE: {
          if (node.getNodeValue().trim().isEmpty()) {
            Node toRemove = node;
            node = node.getNextSibling();
            element.removeChild(toRemove);
          } else {
            node = node.getNextSibling();
          }
          break;
        }
        case Node.ELEMENT_NODE: {
          removeWhitespace((Element)node);
          node = node.getNextSibling();
          break;
        }
        default: {
          node = node.getNextSibling();
          break;
        }
      }
    }
    return element;
  }

  public DocumentBuilder importXml(Element element) {
    current = current.appendChild(document.importNode(element, true));
    return this;
  }

  public void setCurrent(Node current) {
    this.current = current;
  }

}
