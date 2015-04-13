/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.Radl;
import radl.test.RandomData;

import com.google.common.collect.Iterables;


public class XmlCodeTest {

  private static final RandomData RANDOM = new RandomData();

  private final XmlCode code = new XmlCode();

  @Test(expected = IllegalStateException.class)
  public void throwsExceptionOnInvalidXmlCode() {
    code.add(someValue());

    code.asDom();
  }

  private String someValue() {
    return RANDOM.string(10);
  }

  @Test
  public void extractsDom() {
    String root = someValue();
    String child = someValue();
    code.add("<%s>", root);
    code.add("  <%s/>", child);
    code.add("</%s>", root);
    Document expected = DocumentBuilder.newDocument().element(root).element(child).build();

    Document actual = code.asDom();

    assertEquals("Document", Xml.toString(expected), Xml.toString(actual));
  }

  @Test
  public void extractsAttributeValue() {
    String element = RANDOM.string();
    String attribute = RANDOM.string();
    String value = RANDOM.string();
    Document document = DocumentBuilder.newDocument()
        .element(element)
            .attribute(attribute, value)
        .end()
    .build();

    assertEquals("Attribute value", value, code.attr(document.getDocumentElement(), attribute));
  }

  @Test
  public void extractsElementsFromXPath() {
    assertXpath("", "");
  }

  private void assertXpath(String prefix, String uri) {
    String root = someValue();
    String element = someValue();
    String intermediate = someValue();
    code.add("<%s%s>", root, namespaceDeclaration(uri));
    code.add("  <%s/>", someValue());
    code.add("  <%s/>", element);
    code.add("  <%s/>", element);
    code.add("  <%s><%s/></%s>", intermediate, element, intermediate);
    code.add("</%s>", root);
    if (!uri.isEmpty()) {
      code.addNamespace(prefix, uri);
    }
    String path = "//" + namespacePrefix(prefix) + element;

    Iterable<Element> found = code.multiple(path, Element.class);

    assertEquals("# elements", 3, Iterables.size(found));
    for (Element e : found) {
      assertEquals("Tag", element, e.getTagName());
    }
  }

  private String namespacePrefix(String prefix) {
    if (prefix.isEmpty()) {
      return "";
    }
    return prefix + ':';
  }

  private String namespaceDeclaration(String uri) {
    if (uri.isEmpty()) {
      return "";
    }
    return " xmlns='" + uri + "'";
  }

  @Test
  public void extractsNamespacedElementsFromXPath() {
    assertXpath("radl", Radl.NAMESPACE_URI);
  }

  @Test
  public void indentsTwoSpaces() {
    String parent = someValue();
    String child1 = someValue();
    String child2 = someValue();
    code.add("<%s>", parent);
    XmlIndent indent = code.indent();
    try {
      code.add("<%s/>", child1);
      code.add("<%s/>", child2);
    } finally {
      indent.close();
    }
    code.add("</%s>", parent);

    assertEquals("XML", String.format("<%s>\n  <%s/>\n  <%s/>\n</%s>\n", parent, child1, child2, parent), code.text());
  }

}
