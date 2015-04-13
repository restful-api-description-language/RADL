/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.test.RandomData;


public class DocumentBuilderTest {

  private static final RandomData RANDOM = new RandomData();

  @Test
  public void buildsDocument() {
    Document document = DocumentBuilder.newDocument().build();

    assertDocument(document);
    assertNull("Extra document element", document.getDocumentElement());
  }

  private void assertDocument(Document document) {
    assertNotNull("Missing document", document);
  }

  @Test
  public void buildsDocumentElement() {
    String element = RANDOM.string();

    Document document = DocumentBuilder.newDocument().element(element).build();

    assertDocument(document);
    Element ape = assertDocumentElement(document);
    assertEquals("Element", element, ape.getTagName());
  }

  private Element assertDocumentElement(Document document) {
    Element result = document.getDocumentElement();
    assertNotNull("Missing document element", result);

    return result;
  }

  @Test
  public void buildsNestedElements() {
    String parent = RANDOM.string();
    String child = RANDOM.string();
    String grandChild1 = RANDOM.string();
    String grandChild2 = RANDOM.string();

    Document document = newDocumentWithChildAndTwoGrandChildren(parent, child, grandChild1, grandChild2);

    assertDocumentWithChildAndTwoGrandChildren(parent, child, grandChild1, grandChild2, document);
  }

  private Document newDocumentWithChildAndTwoGrandChildren(String parent, String child, String grandChild1,
      String grandChild2) {
    return DocumentBuilder.newDocument()
        .element(parent)
            .element(child)
                .element(grandChild1)
                .end()
                .element(grandChild2, RANDOM.string())
    .build();
  }

  private void assertDocumentWithChildAndTwoGrandChildren(String parent, String child, String grandChild1,
      String grandChild2, Document actual) {
    assertDocument(actual);

    Element parentElement = assertDocumentElement(actual);
    assertElement("Parent", parent, parentElement);
    assertEquals("# children", 1, parentElement.getChildNodes().getLength());

    Node childElement = parentElement.getChildNodes().item(0);
    assertElement("Child", child, childElement);

    NodeList grandChildren = childElement.getChildNodes();
    assertEquals("# grand children", 2, grandChildren.getLength());
    assertElement("Grandchild #1", grandChild1, grandChildren.item(0));
    assertElement("Grandchild #2", grandChild2, grandChildren.item(1));
    assertFalse("Missing text for grandchild #2", ((Element)grandChildren.item(1)).getTextContent().isEmpty());
  }

  private void assertElement(String message, String expected, Node actual) {
    assertEquals(message, expected, ((Element)actual).getTagName());
  }

  @Test
  public void buildsAttribute() {
    String attribute = RANDOM.string();
    String value = RANDOM.string();

    Document document = DocumentBuilder.newDocument()
        .element(RANDOM.string())
            .attribute(attribute, value)
        .build();

    assertDocument(document);
    Element element = assertDocumentElement(document);
    assertEquals(attribute, value, element.getAttributeNS(null, attribute));
  }

  @Test
  public void buildsText() {
    String text = RANDOM.string();

    Document document = DocumentBuilder.newDocument()
        .element(RANDOM.string())
            .text(text)
        .build();

    assertDocument(document);
    Element element = assertDocumentElement(document);
    assertEquals("Text", text, element.getTextContent());
  }

  @Test
  public void buildsElementWithTextAndAttribute() {
    String name = RANDOM.string();
    String text = RANDOM.string();
    String attribute = RANDOM.string();
    String value = RANDOM.string();

    Document document = DocumentBuilder.newDocument()
        .element(RANDOM.string())
            .element(name, text)
            .attribute(attribute, value)
        .build();

    assertDocument(document);
    Element documentElement = assertDocumentElement(document);
    assertEquals(attribute, value, documentElement.getAttributeNS(null, attribute));

    Element element = (Element)documentElement.getElementsByTagNameNS(null, name).item(0);
    assertNotNull("Missing element", element);
    assertEquals("Text", text, element.getTextContent());
  }

  @Test
  public void buildsElementInNamespace() {
    String namespace = RANDOM.string();
    String name = RANDOM.string();

    Document document = DocumentBuilder.newDocument().namespace(namespace).element(name).build();

    Element element = assertDocumentElement(document);
    assertEquals("Namespace URI", namespace, element.getNamespaceURI());
    assertEquals("Local name", name, element.getLocalName());
  }

  @Test
  public void buildAttributeInNamespace() {
    String namespace = RANDOM.string();
    String elementName = RANDOM.string();
    String attributeName1 = RANDOM.string();
    String attributeValue1 = RANDOM.string();
    String attributeName2 = RANDOM.string();
    String attributeValue2 = RANDOM.string();

    Document document = DocumentBuilder.newDocument()
        .namespace(namespace)
        .element(elementName)
            .attribute(attributeName1, attributeValue1)
            .attribute(attributeName2, attributeValue2, namespace)
        .build();

    Element element = assertDocumentElement(document);
    assertEquals("Attribute value 1", attributeValue1, element.getAttributeNS(null, attributeName1));
    assertEquals("Attribute value 2", attributeValue2, element.getAttributeNS(namespace, attributeName2));
  }

  @Test
  public void buildsAttributeInDifferentNamespaceThanElement() {
    String elementNamespace = RANDOM.string();
    String elementName = RANDOM.string();
    String attributeNamespace = RANDOM.string();
    String attributeName = RANDOM.string();
    String attributeValue = RANDOM.string();

    Document document = DocumentBuilder.newDocument()
        .namespace(elementNamespace)
        .element(elementName)
            .attribute(attributeName, attributeValue, attributeNamespace)
        .build();

    Element element = assertDocumentElement(document);
    String value = element.getAttributeNS(attributeNamespace, attributeName);
    assertEquals("Attribute value", attributeValue, value);
  }

  @Test
  public void importsXmlDocument() {
    String parent = RANDOM.string();
    String child = RANDOM.string();
    String grandChild1 = RANDOM.string();
    String grandChild2 = RANDOM.string();
    Element element = newDocumentWithChildAndTwoGrandChildren(parent, child,
        grandChild1, grandChild2).getDocumentElement();

    Document document = DocumentBuilder.newDocument().importXml(element).build();

    assertDocumentWithChildAndTwoGrandChildren(parent, child, grandChild1, grandChild2, document);
  }

  @Test
  public void importsXmlString() {
    String parent = RANDOM.string();
    String child = RANDOM.string();
    String grandChild1 = RANDOM.string();
    String grandChild2 = RANDOM.string();
    String element = Xml.toString(newDocumentWithChildAndTwoGrandChildren(parent, child,
        grandChild1, grandChild2).getDocumentElement());

    Document document = DocumentBuilder.newDocument().importXml(element).build();

    assertDocumentWithChildAndTwoGrandChildren(parent, child, grandChild1, grandChild2, document);
  }

  @Test
  public void providesRandomAccessToNodes() {
    String root = RANDOM.string();
    String child1 = RANDOM.string();
    String child2 = RANDOM.string();
    DocumentBuilder builder = DocumentBuilder.newDocument().element(root);
    Node current = builder.getCurrent();
    builder.element(child1);

    builder.setCurrent(current);
    builder.element(child2);

    assertEquals("XML", Xml.toString(DocumentBuilder.newDocument()
        .element(root)
            .element(child1)
            .end()
            .element(child2)
        .build()), Xml.toString(builder.build()));
  }

}
