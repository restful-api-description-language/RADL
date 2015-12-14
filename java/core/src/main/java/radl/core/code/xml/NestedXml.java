/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code.xml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.w3c.dom.Element;


public class NestedXml extends XmlCode {

  private final String elementName;
  private final String idAttributeName;

  public NestedXml(Element root, String elementName, String idAttributeName, Map<String, String> namespaces) {
    super(root, namespaces);
    this.elementName = elementName;
    this.idAttributeName = idAttributeName;
  }

  public Iterable<String> items() {
    Collection<String> result = new ArrayList<>();
    for (Element element : multiple(elementName, Element.class)) {
      result.add(attr(element, idAttributeName));
    }
    return result;
  }

  public NestedXml item(String name) {
    Element root = one(String.format("%s[@%s='%s']", elementName, idAttributeName, name), Element.class);
    return new NestedXml(root, elementName, idAttributeName, namespaces());
  }

}
