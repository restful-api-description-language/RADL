/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code.radl;

import org.w3c.dom.Element;

import radl.core.code.xml.NestedXml;


public class PropertyGroup extends PropertyGroups implements Property {

  public PropertyGroup(NestedXml xml) {
    super(xml);
  }

  public String name() {
    return attr("name");
  }

  private String attr(String name) {
    return xml().attr(xml().asElement(), name);
  }

  public boolean hasSemantics() {
    if (hasUri()) {
      return true;
    }
    for (String name : names()) {
      if (item(name).hasSemantics()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasUri() {
    return !uri().isEmpty();
  }

  @Override
  public String uri() {
    return attr("uri");
  }
  
  public String reference() {
    return attr("ref");
  }

  public Iterable<String> propertyNames() {
    return xml().elementsAttribute("name", "radl:*[local-name(.) = 'property' or local-name(.) = 'property-group']");
  }

  public Property property(String name) {
    Element propertyElement = xml().one("radl:*[@name='" + name + "']", Element.class);
    if (propertyElement.getLocalName().equals("property-group")) {
      return item(name);
    }
    return new PropertyImpl(propertyElement, xml().namespaces());
  }

  @Override
  public String type() {
    return attr("type");
  }

  @Override
  public boolean repeats() {
    return Boolean.parseBoolean(attr("repeats"));
  }

}
