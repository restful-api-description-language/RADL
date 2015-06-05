/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import radl.common.xml.ElementProcessor;
import radl.common.xml.Xml;


public class TestParser {

  public Iterable<Assertion> parse(File test) throws Exception {
    final Collection<Assertion> result = new ArrayList<Assertion>();
    Document assertions = Xml.parse(test);
    Xml.processChildElements(assertions.getDocumentElement(), new ElementProcessor() {
      @Override
      public void process(Element element) throws Exception {
        String message = element.getAttributeNS(null, "message");
        if ("equals".equals(element.getLocalName())) {
          result.add(parseEquals(element, message));
        }
      }
    });
    return result;
  }

  private Assertion parseEquals(Element element, String message) {
    Element child = Xml.getFirstChildElement(element, null);
    Value expected = parseValue(child);
    Value actual = parseValue(Xml.nextElement(child));
    return new AssertEquals(message, expected, actual);
  }

  private Value parseValue(Element element) {
    String tag = element.getLocalName();
    if ("literal".equals(tag)) {
      return new LiteralValue(element);
    }
    if ("select".equals(tag)) {
      return new SelectorValue(element);
    }
    throw new UnsupportedOperationException("Unknown value type: " + tag);
  }

}
