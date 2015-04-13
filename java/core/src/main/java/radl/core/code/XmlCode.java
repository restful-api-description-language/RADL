/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.jaxen.JaxenException;
import org.jaxen.XPath;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import radl.common.io.StringStream;
import radl.common.xml.Xml;


/**
 * Code that follows the XML syntax.
 */
public class XmlCode extends Code {

  private final Map<String, String> namespaces = new HashMap<String, String>();
  private final StringBuilder indentation = new StringBuilder(); // NOPMD AvoidStringBufferField
  private transient Document document;

  public XmlCode() {
    super(new XmlSyntax());
  }

  @Override
  public boolean add(String text) {
    document = null;
    return super.add(indentation + text);
  }

  public Document asDom() {
    return getDocument();
  }

  private Document getDocument() {
    if (document == null) {
      try {
        document = Xml.parse(new StringStream(text()));
      } catch (Exception e) {
        throw new IllegalStateException("Not valid XML:\n" + text(), e);
      }
    }
    return document;
  }

  protected <T> Iterable<T> multiple(String path, Class<T> returnType) {
    Collection<T> result = new ArrayList<T>();
    try {
      XPath xpath = new DOMXPath(path);
      for (Entry<String, String> entry : namespaces.entrySet()) {
        xpath.addNamespace(entry.getKey(), entry.getValue());
      }
      for (Object found : xpath.selectNodes(getDocument())) {
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

}
