/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.xml;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.test.RandomData;


public class XmlMergerTest {

  private static final RandomData RANDOM = new RandomData();

  private final String namespace = String.format("http://%s.com/%s", RANDOM.string(), RANDOM.string());
  private final DocumentBuilder builder = DocumentBuilder.newDocument().namespace(namespace);
  private XmlMerger merger;

  @Test
  public void addsMissingElement() {
    String root = aName();
    merger = new XmlMerger(builder.element(root).build());
    String name = aName();

    merger.process(builder.element(name).build());

    assertMerged();
  }

  private String aName() {
    return RANDOM.string(5);
  }

  private void assertMerged() {
    assertEquals("Merged document", Xml.toString(builder.build()), Xml.toString(merger.result()));
  }

  @Test
  public void addsMissingElementAsDocumentElement() {
    merger = new XmlMerger();
    String name = aName();

    merger.process(builder.element(name).build());

    assertMerged();
  }

  @Test
  public void skipsExistingElement() throws Exception {
    String name = aName();
    builder.element(name);
    merger = new XmlMerger(builder.build());

    merger.process(builder.build());

    assertMerged();
  }

  @Test
  public void addsMissingAttribute() {
    String root = aName();
    builder.element(root);
    merger = new XmlMerger(builder.build());
    String name = aName();
    String value = aName();

    merger.process(builder.attribute(name, value).build());

    assertMerged();
  }

  @Test
  public void skipsExistingAttribute() {
    String root = aName();
    String name = aName();
    String value = aName();
    builder.element(root).attribute(name, value);
    merger = new XmlMerger(builder.build());

    merger.process(builder.build());

    assertMerged();
  }

  @Test
  public void addsSiblingElement() {
    String element = aName();
    builder.element(element + 's');
    merger = new XmlMerger(builder.build());
    String attribute = aName();
    String value1 = aName();
    String value2 = aName();
    builder.element(element)
        .attribute(attribute, value1)
    .end()
    .element(element)
        .attribute(attribute, value2);

    merger.process(builder.build());

    assertMerged();
  }

}
