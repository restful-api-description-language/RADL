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
import java.util.Map.Entry;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import radl.common.io.StringStream;
import radl.common.xml.Xml;


/**
 * Code that follows the XML syntax.
 */
public class XmlCode extends Code {

  private final Map<String, String> namespaces = new HashMap<String, String>();
  private final StringBuilder indentation = new StringBuilder(); // NOPMD AvoidStringBufferField
  private transient Node root;

  public XmlCode() {
    super(new XmlSyntax());
  }

  public XmlCode(Node root) {
    this();
    this.root = root;
  }

  public XmlCode(Element root, Map<String, String> namespaces) {
    this(root);
    this.namespaces.putAll(namespaces);
  }

  public Map<String, String> namespaces() {
    return Collections.<String, String>unmodifiableMap(namespaces);
  }

  @Override
  public boolean add(String text) {
    root = null;
    return super.add(indentation + text);
  }

  public Document asDom() {
    return (Document)getRoot();
  }

  public Element asElement() {
    return (Element)getRoot();
  }

  public Node asNode() {
    return getRoot();
  }

  private Node getRoot() {
    if (root == null) {
      try {
        root = Xml.parse(new StringStream(text()));
      } catch (Exception e) {
        throw new IllegalStateException("Not valid XML:\n" + text(), e);
      }
    }
    return root;
  }

  protected <T> Iterable<T> multiple(String path, Class<T> returnType) {
    Collection<T> result = new ArrayList<T>();
    try {
      XPath xpath = new DOMXPath(path);
      for (Entry<String, String> entry : namespaces.entrySet()) {
        xpath.addNamespace(entry.getKey(), entry.getValue());
      }
      for (Object found : xpath.selectNodes(getRoot())) {
        result.add(returnType.cast(found));
      }
    } catch (JaxenException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  public void addNamespace(String prefix, String uri) {
    namespaces.put(prefix, uri);
  }

  protected String attr(Element element, String name) {
    return element.getAttributeNS(null, name);
  }

  protected <T> T one(String path, Class<T> returnType) {
    Iterator<T> results = multiple(path, returnType).iterator();
    if (!results.hasNext()) {
      throw new IllegalArgumentException("No results for: " + path);
    }
    T result = results.next(); // NOPMD PrematureDeclaration
    if (results.hasNext()) {
      throw new IllegalArgumentException("More that one result for: " + path);
    }
    return result;
  }

  public XmlIndent indent() {
    return new XmlIndent(this);
  }

  void indent(int toAdd) {
    for (int i = 0; i < toAdd; i++) {
      indentation.append(' ');
    }
  }

  void unindent(int toRemove) {
    indentation.setLength(indentation.length() - toRemove);
  }

  public NestedXml nested(String containerPath, String elementName, String idAttributeName) {
    Iterator<Element> result = multiple(containerPath, Element.class).iterator();
    return result.hasNext() ? new NestedXml(result.next(), elementName, idAttributeName, namespaces) : null;
  }

  public Iterable<String> elementsAttribute(String attribute, String elementXPath, Object... args) {
    List<String> result = new ArrayList<String>();
    for (Element element : multiple(String.format(elementXPath, args), Element.class)) {
      String value = attr(element, attribute);
      if (!value.isEmpty()) {
        result.add(value);
      }
    }
    Collections.sort(result);
    return result;
  }

  @Override
  public String toString() {
    return root == null ? super.toString() : Xml.toString(root);
  }

}
