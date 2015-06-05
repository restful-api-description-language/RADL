/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;


/**
 * Utility methods for working with XML.
 */
public final class Xml {

  public static final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

  /*
   * Avoid expensive DocumentBuilderFactory provider lookup by reusing a static instance. The
   * documentation says nothing about the thread safety of a DocumentBuilderFactory, but in practice
   * newDocumentBuilder is safe.
   */
  private static final DocumentBuilderFactory VALIDATING_DOCUMENT_BUILDER_FACTORY =
      newSecureDocumentBuilderFactory(true);
  private static final DocumentBuilderFactory NON_VALIDATING_DOCUMENT_BUILDER_FACTORY =
      newSecureDocumentBuilderFactory(false);
  private static final TransformerFactory TRANSFORMER_FACTORY = newSecureTransformerFactory();

  private static final ThreadLocal<DocumentBuilder> VALIDATING_DOCUMENT_BUILDER = new ThreadLocal<DocumentBuilder>() {
    @Override
    protected DocumentBuilder initialValue() {
      try {
        return VALIDATING_DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        throw new IllegalStateException(e);
      }
    }
  };

  private static final ThreadLocal<DocumentBuilder> NON_VALIDATING_DOCUMENT_BUILDER =
    new ThreadLocal<DocumentBuilder>() {
      @Override
      protected DocumentBuilder initialValue() {
        try {
          return NON_VALIDATING_DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
          throw new IllegalStateException(e);
        }
      }
    };

  private static final ThreadLocal<Transformer> TRANSFORMER = new ThreadLocal<Transformer>() {
    @Override
    protected Transformer initialValue() {
      try {
        return TRANSFORMER_FACTORY.newTransformer();
      } catch (TransformerConfigurationException e) {
        throw new IllegalStateException(e);
      }
    }
  };

  private static final Escaper XML_ESCAPER;
  private static final int MAX_INDENTATION = 10;
  private static final String[] INDENTATION = new String[MAX_INDENTATION];

  static {
    Escapers.Builder builder = Escapers.builder();
    // The char values \uFFFE and \uFFFF are explicitly not allowed in XML
    // (Unicode code points above \uFFFF are represented via surrogate pairs
    // which means they are treated as pairs of safe characters).

    builder.setSafeRange(Character.MIN_VALUE, '\uFFFF');

    // Unsafe characters are removed.
    builder.setUnsafeReplacement("");

    // Build the content escaper first and then add quote escaping for the general escaper.
    builder.addEscape('&', "&amp;");
    builder.addEscape('<', "&lt;");
    builder.addEscape('>', "&gt;");
    builder.addEscape('\'', "&apos;");
    builder.addEscape('"', "&quot;");
    XML_ESCAPER = builder.build();

    StringBuilder indentation = new StringBuilder();
    for (int i = 0; i < MAX_INDENTATION; i++) {
      INDENTATION[i] = indentation.toString();
      indentation.append("  ");
    }
  }

  private Xml() {
    // Utility class
  }

  public static Document parse(File file) {
    return parse(file, true);
  }

  public static Document parse(File file, boolean validating) {
    try {
      InputStream stream = new FileInputStream(file);
      try {
        return parse(stream, validating);
      } finally {
        stream.close();
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to parse " + file.getAbsolutePath(), e);
    }
  }

  public static Document parse(InputStream stream) {
    return parse(stream, true);
  }

  public static Document parse(InputStream stream, boolean validating) {
    DocumentBuilder documentBuilder = getDocumentBuilder(validating);
    try {
      return documentBuilder.parse(stream);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      documentBuilder.reset();
    }
  }

  private static DocumentBuilder getDocumentBuilder(boolean validating) {
    DocumentBuilder result = validating ? VALIDATING_DOCUMENT_BUILDER.get() : NON_VALIDATING_DOCUMENT_BUILDER.get();
    if (validating) {
      result.setErrorHandler(new DefaultErrorHandler());
    }
    return result;
  }

  public static void transform(Source source, Result destination) {
    Transformer transformer = getTransformer();
    try {
      transformer.transform(source, destination);
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      transformer.reset();
    }
  }

  private static Transformer getTransformer() {
    return TRANSFORMER.get();
  }

  public static Document newDocument() {
    return newDocument(true);
  }

  public static Document newDocument(boolean validating) {
    DocumentBuilder documentBuilder = getDocumentBuilder(validating);
    try {
      return documentBuilder.newDocument();
    } finally {
      documentBuilder.reset();
    }
  }

  public static DocumentBuilderFactory newSecureDocumentBuilderFactory(boolean validating) {
    try {
      DocumentBuilderFactory result = DocumentBuilderFactory.newInstance();
      result.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
      result.setFeature("http://xml.org/sax/features/external-general-entities", false);
      result.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      result.setNamespaceAware(true);
      result.setValidating(validating);
      return result;
    } catch (ParserConfigurationException e) {
      throw new IllegalStateException(e);
    }
  }

  public static Element getFirstChildElement(Element parent, String name) {
    if (parent == null) {
      return null;
    }
    Node current = parent.getFirstChild();
    while (current != null) {
      if (current.getNodeType() == Node.ELEMENT_NODE && (name == null || name.equals(current.getLocalName()))) {
        return (Element)current;
      }
      current = current.getNextSibling();
    }
    return null;
  }

  public static Element getChildElementByAttribute(Element parent, String childName, final String attributeName,
      final String attributeValue) throws Exception {
    final AtomicReference<Element> result = new AtomicReference<Element>();
    processChildElements(parent, new ElementProcessor() {

      @Override
      public void process(Element element) throws Exception {
        if (attributeValue.equals(element.getAttributeNS(null, attributeName))) {
          result.set(element);
        }
      }
    }, childName);
    return result.get();
  }

  public static void processChildElements(Node parent, ElementProcessor elementProcessor, String... childElementNames)
      throws Exception {
    processElements(parent.getFirstChild(), elementProcessor, childElementNames);
  }

  private static void processElements(Node firstNode, ElementProcessor elementProcessor, String... names)
      throws Exception {
    List<String> elementNames = Arrays.asList(names);
    Node current = firstNode;
    while (current != null) {
      if (current.getNodeType() == Node.ELEMENT_NODE
          && (elementNames.isEmpty() || elementNames.contains(current.getLocalName()))) {
        elementProcessor.process((Element)current);
      }
      current = current.getNextSibling();
    }
  }

  public static boolean hasChildElements(Element parent) {
    Node child = parent.getFirstChild();
    while (child != null) {
      if (child.getNodeType() == Node.ELEMENT_NODE) {
        return true;
      }
      child = child.getNextSibling();
    }
    return false;
  }

  public static void processNestedElements(Node parentNode, ElementProcessor elementProcessor,
      String... elementNames) throws Exception {
    if (parentNode == null) {
      return;
    }
    processElements(parentNode.getFirstChild(), newHierarchyElementProcessor(elementProcessor, elementNames, 1),
        elementNames[0]);
  }

  private static ElementProcessor newHierarchyElementProcessor(final ElementProcessor elementProcessor,
      final String[] elementNames, final int index) {
    if (index == elementNames.length - 1) {
      return new ElementProcessor() {

        @Override
        public void process(Element element) throws Exception {
          processChildElements(element, elementProcessor, elementNames[index]);
        }
      };
    }
    return new ElementProcessor() {

      @Override
      public void process(Element element) throws Exception {
        processChildElements(element, newHierarchyElementProcessor(elementProcessor, elementNames, index + 1),
            elementNames[index]);
      }
    };
  }

  public static void processDecendantElements(Node parentNode, ElementProcessor elementProcessor, String elementName)
      throws Exception {
    if (parentNode == null) {
      return;
    }
    Node current = parentNode.getFirstChild();
    while (current != null) {
      if (current.getNodeType() == Node.ELEMENT_NODE) {
        if (elementName.equals(current.getLocalName())) {
          elementProcessor.process((Element)current);
        } else {
          processDecendantElements(current, elementProcessor, elementName);
        }
      }
      current = current.getNextSibling();
    }
  }

  public static void processChildElementsWithAttribute(Node parentNode, ElementProcessor elementProcessor,
      String attributeName) throws Exception {
    if (parentNode == null) {
      return;
    }
    Node current = parentNode.getFirstChild();
    while (current != null) {
      if (current.getNodeType() == Node.ELEMENT_NODE
          && current.getAttributes().getNamedItemNS(null, attributeName) != null) {
        elementProcessor.process((Element)current);
      }
      current = current.getNextSibling();
    }
  }

  public static String toString(Node node) {
    StringBuilder result = new StringBuilder();
    if (node != null) {
      Map<String, String> namespaces = new HashMap<String, String>();
      namespaces.put("http://www.w3.org/2001/XMLSchema-instance", "xsi");
      namespaces.put("http://www.w3.org/XML/1998/namespace", "xml");
      append(node, 0, namespaces, result);
    }
    return result.toString();
  }

  private static void append(Node node, int indentationLevel, Map<String, String> namespaces, StringBuilder builder) {
    switch (node.getNodeType()) {
      case Node.DOCUMENT_NODE:
        appendDocument(node, indentationLevel, namespaces, builder);
        break;
      case Node.ELEMENT_NODE:
        appendElement(node, indentationLevel, namespaces, builder);
        break;
      case Node.ATTRIBUTE_NODE:
        appendAttribute(node, namespaces, builder);
        break;
      case Node.TEXT_NODE:
        appendText(node, builder);
        break;
      case Node.COMMENT_NODE:
        appendComment(node, indentationLevel, builder);
        break;
      default:
        throw new UnsupportedOperationException("Unhandled node type: " + node.getNodeType());
    }
  }

  private static void appendComment(Node node, int indentationLevel, StringBuilder builder) {
    builder.append(getIndentation(indentationLevel)).append("<!-- ").append(node.getNodeValue()).append(" -->\n");
  }

  private static String getIndentation(int level) {
    return INDENTATION[level];
  }

  private static void appendDocument(Node node, int indentationLevel, Map<String, String> namespaces,
      StringBuilder builder) {
    builder.append(XML_PROLOG);
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      append(child, indentationLevel, namespaces, builder);
    }
  }

  private static void appendElement(Node node, int indentationLevel, Map<String, String> namespaces,
      StringBuilder builder) {
    Element element = (Element)node;
    openElement(indentationLevel, builder, element);
    appendAttributes(indentationLevel, element, namespaces, builder);
    if (node.getFirstChild() == null) {
      builder.append("/>\n");
    } else if (startsWithNonWhitespaceText(node)) {
      builder.append('>');
      appendChildren(node, indentationLevel, namespaces, builder);
      closeElement(builder, element);
    } else {
      builder.append(">\n");
      appendChildren(node, indentationLevel, namespaces, builder);
      builder.append(getIndentation(indentationLevel));
      closeElement(builder, element);
    }
  }

  private static StringBuilder openElement(int indentationLevel, StringBuilder builder, Element element) {
    String tag = element.getTagName();
    builder.append(getIndentation(indentationLevel)).append('<');
    builder.append(tag);
    if (hasDifferentNamespaceThanParent(element)) {
      int index = tag.indexOf(':');
      if (index < 0) {
        builder.append(" xmlns=\"").append(element.getNamespaceURI()).append('"');
      } else {
        builder.append(" xmlns:").append(tag.substring(0, index)).append("=\"").append(element.getNamespaceURI())
            .append('"');
      }
    }
    return builder;
  }

  private static boolean hasDifferentNamespaceThanParent(Node node) {
    if (node.getNamespaceURI() == null || isNamespaceNode(node)) {
      return false;
    }
    Node parent;
    if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
      parent = ((Attr)node).getOwnerElement();
    } else {
      parent = node.getParentNode();
    }
    if (parent == null) {
      return true;
    }
    return !node.getNamespaceURI().equals(parent.getNamespaceURI());
  }

  private static boolean isNamespaceNode(Node node) {
    return node.getNodeType() == Node.ATTRIBUTE_NODE && node.getNodeName().startsWith("xmlns")
        && "http://www.w3.org/2000/xmlns/".equals(node.getNamespaceURI());
  }

  private static void appendAttributes(int indentationLevel, Element element, Map<String, String> namespaces,
      StringBuilder builder) {
    NamedNodeMap attributes = element.getAttributes();
    for (int i = 0; i < attributes.getLength(); i++) {
      append(attributes.item(i), indentationLevel + 1, namespaces, builder);
    }
  }

  private static boolean startsWithNonWhitespaceText(Node node) {
    StringBuilder text = new StringBuilder();
    Node child = node.getFirstChild();
    while (child != null) {
      if (child.getNodeType() != Node.TEXT_NODE) {
        break;
      }
      text.append(child.getNodeValue());
      child = child.getNextSibling();
    }
    return text.length() > 0 && !text.toString().trim().isEmpty();
  }

  private static void appendChildren(Node node, int indentationLevel, Map<String, String> namespaces,
      StringBuilder builder) {
    for (Node child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
      if (child.getNodeType() != Node.ATTRIBUTE_NODE) {
        append(child, indentationLevel + 1, namespaces, builder);
      }
    }
  }

  private static StringBuilder closeElement(StringBuilder builder, Element element) {
    return builder.append("</").append(element.getTagName()).append(">\n");
  }

  private static void appendAttribute(Node node, Map<String, String> namespaces, StringBuilder builder) {
    if (isNamespaceNode(node)) {
      return;
    }
    builder.append(' ');
    if (hasDifferentNamespaceThanParent(node)) {
      String uri = node.getNamespaceURI();
      String prefix = getPrefix(uri, namespaces);
      if (!"xml".equals(prefix)) {
        builder.append("xmlns:").append(prefix).append("=\"").append(uri).append("\" ");
      }
      builder.append(prefix).append(':');
    }
    builder.append(getAttributeName(node)).append("=\"").append(node.getNodeValue()).append('\"');
  }

  private static String getAttributeName(Node node) {
    String result = node.getLocalName();
    if (result == null) {
      // No namespace support while parsing
      result = node.getNodeName();
    }
    return result;
  }

  public static String getPrefix(String uri, Map<String, String> namespaces) {
    String result = namespaces.get(uri);
    if (result == null) {
      result = "ns" + (namespaces.size() + 1);
      namespaces.put(uri, result);
    }
    return result;
  }

  private static void appendText(Node node, StringBuilder builder) {
    builder.append(format(node.getNodeValue()));
  }

  private static String format(String text) {
    return text == null ? "" : escape(text.trim());
  }

  public static String escape(String xml) {
    return XML_ESCAPER.escape(xml);
  }

  public static TransformerFactory newSecureTransformerFactory() {
    try {
      TransformerFactory result = TransformerFactory.newInstance();
      result.setFeature("http://javax.xml.XMLConstants/feature/secure-processing", true);
      return result;
    } catch (TransformerConfigurationException e) {
      throw new IllegalStateException(e);
    }
  }


  private static final class DefaultErrorHandler implements ErrorHandler {

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      // Do nothing
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      if (!isMissingSchemaError(exception)) {
        throw exception;
      }
    }

    private boolean isMissingSchemaError(SAXParseException exception) {
      String message = exception.getMessage();
      if (message == null) {
        return false;
      }
      return message.contains("no grammar found") || message.contains("must match DOCTYPE root \"null\"");
    }
  }


  public static Element nextElement(Element element) {
    Node current = element.getNextSibling();
    while (current != null) {
      if (current.getNodeType() == Node.ELEMENT_NODE) {
        return (Element)current;
      }
      current = current.getNextSibling();
    }
    return null;
  }

}
