/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static radl.common.xml.DocumentBuilder.newDocument;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import radl.common.io.ByteArrayInputOutputStream;
import radl.common.io.StringStream;
import radl.common.xml.ElementProcessor;
import radl.common.xml.Xml;


public class XmlTest {

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionOnMissingFile() {
    File file = new File("ape.bear");
    if (file.exists()) {
      assertTrue("Failed to delete file", file.delete());
    }

    Xml.parse(file);
  }

  @Test
  public void parsesXml()  {
    String xml = "<cheetah/>";

    Document document = parse(xml);

    assertNotNull("Missing document", document);
    Element documentElement = document.getDocumentElement();
    assertNotNull("Missing document element", documentElement);
    assertEquals("Document element", "cheetah", documentElement.getTagName());
  }

  private Document parse(String xml) {
    return Xml.parse(new StringStream(xml));
  }

  @Test
  public void getsFirstChildElement()  {
    Document document = parse("<dingo><!-- elephant -->elephant<hyena/><elephant/></dingo>");
    Element documentElement = document.getDocumentElement();

    Element firstChild = Xml.getFirstChildElement(documentElement, "elephant");

    assertNotNull("Missing first child", firstChild);
    assertEquals("First child", "elephant", firstChild.getTagName());
    assertNull("Missing parent", Xml.getFirstChildElement(null, ""));
  }

  @Test
  public void getsChildElementByAttribute() throws Exception {
    Document document = parse("<iguano><jaguar/><jaguar koala='leopard'/><jaguar koala='mule'/></iguano>");
    Element documentElement = document.getDocumentElement();

    Element firstChild = Xml.getChildElementByAttribute(documentElement, "jaguar", "koala", "mule");

    assertNotNull("Missing first child", firstChild);
    assertEquals("First child", "jaguar", firstChild.getTagName());
    assertEquals("Attribute", "mule", firstChild.getAttributeNS(null, "koala"));
  }

  @Test
  public void processesNestedElements() throws Exception {
    Document document = newDocument()
        .element("velociraptor")
            .element("whale")
                .element("zebra", "aardvark")
            .end()
            .element("whale")
                .element("zebra", "bee")
            .end()
    .build();

    final Collection<String> animals = new ArrayList<String>();
    Xml.processNestedElements(document, new ElementProcessor() {
      @Override
      public void process(Element zebraElement) {
        animals.add(zebraElement.getTextContent());
      }
    }, "velociraptor", "whale", "zebra");

    assertEquals("Animals", Arrays.asList("aardvark", "bee"), animals);
    Xml.processNestedElements(null, null, ""); // Check for NPE
  }

  @Test
  public void processesDecendants() throws Exception {
    Document document = newDocument()
        .element("cobra")
            .element("dolphin")
                .element("eagle", "flamingo")
            .end()
            .element("gazelle")
                .element("eagle", "hawk")
            .end()
    .build();

    final Collection<String> animals = new ArrayList<String>();
    Xml.processDecendantElements(document, new ElementProcessor() {
      @Override
      public void process(Element eagleElement) {
        animals.add(eagleElement.getTextContent());
      }
    }, "eagle");

    assertEquals("Animals", Arrays.asList("flamingo", "hawk"), animals);
    Xml.processDecendantElements(null, null, ""); // Check for NPE
  }

  @Test
  public void serializesNullToEmptyString() {
    assertEquals("toString()", "", Xml.toString(null));
  }

  @Test
  public void serializesDocumentWithoutNamespaces() {
    Document document = newDocument()
        .element("tapir")
            .element("vicuna", "wasp")
            .attribute("ant", "bison")
        .build();
    assertEquals("toString()",
        Xml.XML_PROLOG + "<tapir ant=\"bison\">\n  <vicuna>wasp</vicuna>\n</tapir>\n",
        Xml.toString(document));
  }

  @Test
  public void serializesDocumentWithNamespaces() {
    Document document = newDocument()
        .namespace("http://ape.com")
        .element("bear")
            .element("cheetah")
            .end()
            .namespace("http://dingo.com")
            .element("elephant")
                .attribute("fox", "giraffe", "http://hyena.com")
                .attribute("schemaLocation", "iguana", "http://www.w3.org/2001/XMLSchema-instance")
        .build();
    assertEquals("toString()", Xml.XML_PROLOG
        + "<bear xmlns=\"http://ape.com\">\n  <cheetah/>\n  "
        + "<elephant xmlns=\"http://dingo.com\" xmlns:ns3=\"http://hyena.com\" ns3:fox=\"giraffe\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"iguana\"/>\n</bear>\n",
        Xml.toString(document));
  }

  @Test
  public void serializationOfParseIsIdentityOperation() throws IOException {
    String xml = "<bear xmlns=\"http://ape.com\">\n  <cheetah/>\n  "
        + "<elephant xmlns=\"http://dingo.com\" xmlns:ns3=\"http://hyena.com\" ns3:fox=\"giraffe\" "
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"iguana\"/>\n</bear>\n";
    ByteArrayInputOutputStream stream = new ByteArrayInputOutputStream();
    try {
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, "UTF8"));
      try {
        writer.print(xml);
        writer.flush();

        Document document = Xml.parse(stream.getInputStream());

        assertEquals("toString()", Xml.XML_PROLOG + xml, Xml.toString(document));
      } finally {
        writer.close();
      }
    } finally {
      stream.close();
    }
  }

  @Test
  public void parsingAndSerializationProperlyHandleTheXmlNamespace() throws IOException {
    String xml = "<elephant xml:lang=\"en\">fox</elephant>\n";
    InputStream stream = new StringStream(xml);
    try {
      Document document = Xml.parse(stream);
      assertEquals("XML", xml, Xml.toString(document.getDocumentElement()));
    } finally {
      stream.close();
    }
  }

  @Test
  public void serializationMaintainsNamespacePrefix() throws IOException {
    String xml = "<zoo:giraffe xmlns:zoo=\"http://zoo.fakedomain.com\">\n  <zoo:hyena/>\n</zoo:giraffe>\n";
    InputStream stream = new ByteArrayInputStream(xml.getBytes("UTF-8"));
    try {
      Document document = Xml.parse(stream);
      assertEquals("XML", xml, Xml.toString(document.getDocumentElement()));
    } finally {
      stream.close();
    }
  }

  @Test
  public void escapesReservedCharacters() {
    assertEquals("Simple text", "cat", Xml.escape("cat"));
    assertEquals("Quote", "&quot;duck", Xml.escape("\"duck"));
    assertEquals("Angle bracket", "&lt;elk", Xml.escape("<elk"));
  }

  @Test
  public void filtersNamespaceAttributesFromAttributes() {
    NamedNodeMap map = mock(NamedNodeMap.class);
    when(map.getLength()).thenReturn(3);
    when(map.item(any(Integer.class))).thenAnswer(new Answer<Node>() {
      @Override
      public Node answer(InvocationOnMock invocation) throws Throwable {
        int index = (Integer)invocation.getArguments()[0];
        switch (index) {
          case 0: return nodeOf("xmlns");
          case 1: return nodeOf("xmlns:falcon");
          default: return nodeOf("grizzly");
        }
      }
    });
    Node node = mock(Node.class);
    when(node.getAttributes()).thenReturn(map);

    Iterator<Node> nodes = Xml.getAttributes(node).iterator();

    assertTrue("Missing node", nodes.hasNext());
    assertEquals("Node", "grizzly", nodes.next().getNodeName());
    assertFalse("Extra node", nodes.hasNext());
  }

  private Node nodeOf(String name) {
    Node result = mock(Node.class);
    when(result.getNodeName()).thenReturn(name);
    return result;
  }

}
