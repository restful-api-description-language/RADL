/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;


/**
 * {@linkplain DocumentProcessor} that merges an XML DOM {@linkplain Document} into an existing document.
 */
public class XmlMerger implements DocumentProcessor {

  private final Document document;

  public XmlMerger() {
    this(DocumentBuilder.newDocument().build());
  }

  public XmlMerger(Document source) {
    document = DocumentBuilder.newDocument().build();
    if (source.getDocumentElement() != null) {
      document.appendChild(this.document.importNode(source.getDocumentElement(), true));
    }
  }

  @Override
  public void process(Document fragment) {
    process(fragment.getDocumentElement(), document);
  }

  private void process(Element node, Node parent) {
    FindResult result = find(node, parent);
    if (result.isExactMatch()) {
      processDecendants(node, result.getElement());
    } else if (result.isMismatch()) {
      update(node, result.getElement());
      processDecendants(node, result.getElement());
    } else {
      duplicate(node, parent);
    }
  }

  private void processDecendants(Element node, Element element) {
    for (Node current = node.getFirstChild(); current != null; current = current.getNextSibling()) {
      if (current instanceof Element) {
        process((Element)current, element);
      }
    }
  }

  private void update(Element source, Element destination) {
    NamedNodeMap attributes = destination.getAttributes();
    for (Node attribute : Xml.getAttributes(destination)) {
      attributes.removeNamedItem(attribute.getNodeName());
    }
    for (Node  attribute : Xml.getAttributes(source)) {
      destination.setAttribute(attribute.getNodeName(), attribute.getNodeValue());
    }
  }

  private FindResult find(Element node, Node parent) {
    for (Node current = parent.getFirstChild(); current != null; current = current.getNextSibling()) {
      if (current.getNodeType() == node.getNodeType()
          && isEqual(node.getNamespaceURI(), current.getNamespaceURI())
          && isEqual(node.getLocalName(), current.getLocalName())) {
        Element result = (Element)current;
        boolean exactMatch = isSame(node, result);
        String listName = node.getLocalName() + 's';
        boolean isListItem = listName.equals(parent.getLocalName());
        if (exactMatch || !isListItem) {
          return new FindResult(result, exactMatch);
        }
      }
    }
    return FindResult.notFound();
  }

  private boolean isEqual(String value1, String value2) {
    return value1 == null ? value2 == null : value1.equals(value2);
  }

  private boolean isSame(Element element1, Element element2) {
    if (element1.getAttributes().getLength() > 0) {
      return hasSameAttributes(element1, element2);
    }
    return hasSameText(element1, element2);
  }

  private boolean hasSameAttributes(Element element1, Element element2) {
    for (Node attribute : Xml.getAttributes(element1)) {
      if (!attribute.getNodeValue().equals(element2.getAttribute(attribute.getNodeName()))) {
        return false;
      }
    }
    return true;
  }

  private boolean hasSameText(Element element1, Element element2) {
    return getText(element1).equals(getText(element2));
  }

  private String getText(Element element) {
    return element.getTextContent().trim();
  }

  private void duplicate(Element node, Node parent) {
    if (parent instanceof Document && parent.getFirstChild() != null) {
      parent.removeChild(parent.getFirstChild());
    }
    Node importedNode = getDocument(parent).importNode(node, true);
    // TODO: documentation should be first element -> introduce XmlMergeRules interface
    parent.appendChild(importedNode);
  }

  private Document getDocument(Node parent) {
    if (parent instanceof Document) {
      return (Document)parent;
    }
    return parent.getOwnerDocument();
  }

  @Override
  public Document result() {
    return document;
  }


  private static class FindResult {

    private final Element element;
    private final boolean exactMatch;

    public static FindResult notFound() {
      return new FindResult(null, false);
    }

    public FindResult(Element element, boolean exactMatch) {
      this.element = element;
      this.exactMatch = exactMatch;
    }

    public boolean isMismatch() {
      return element != null && !exactMatch;
    }

    public boolean isExactMatch() {
      return element != null && exactMatch;
    }

    public Element getElement() {
      return element;
    }

  }

}
