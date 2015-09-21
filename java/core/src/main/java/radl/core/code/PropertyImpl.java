/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.Map;

import org.w3c.dom.Element;


public class PropertyImpl extends XmlCode implements Property {

  public PropertyImpl(Element propertyElement, Map<String, String> namespaces) {
    super(propertyElement, namespaces);
  }

  @Override
  public String uri() {
    return attr("uri");
  }

  private String attr(String name) {
    return attr(asElement(), name);
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
