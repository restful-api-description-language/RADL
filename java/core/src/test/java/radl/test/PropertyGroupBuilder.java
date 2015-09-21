/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import org.w3c.dom.Element;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;


public class PropertyGroupBuilder implements PropertGroupContainer {

  private final PropertGroupContainer parent;

  public PropertyGroupBuilder(PropertGroupContainer parent) {
    this.parent = parent;
    if (parent instanceof RadlBuilder) {
      ensurePropertyGroups();
    }
    builder().element("property-group");
  }

  private void ensurePropertyGroups() {
    String currentNodeName = builder().getCurrent().getLocalName();
    if ("service".equals(currentNodeName)) {
      Element propertyGroupsElement = Xml.getFirstChildElement((Element)builder().getCurrent(), "property-groups");
      if (propertyGroupsElement != null) {
        builder().setCurrent(propertyGroupsElement);
        currentNodeName = propertyGroupsElement.getLocalName();
      }
    }
    if (!"property-groups".equals(currentNodeName)) {
      builder().element("property-groups");
    }
  }

  @Override
  public DocumentBuilder builder() {
    return parent.builder();
  }

  public PropertyGroupBuilder named(String name) {
    builder().attribute("name", name);
    return this;
  }

  public RadlBuilder end() {
    builder().end().end();
    return (RadlBuilder)parent;
  }

  public PropertyGroupBuilder endNested() {
    builder().end();
    return (PropertyGroupBuilder)parent;
  }

  public PropertyGroupBuilder meaning(String uri) {
    builder().attribute("uri", uri);
    return this;
  }

  public PropertyBuilder withProperty(String name) {
    return new PropertyBuilder(this, name);
  }

  private DocumentBuilder startProperty(String name) {
    return builder().element("property")
        .attribute("name", name);
  }

  public PropertyGroupBuilder withProperty(String name, String type) {
    startProperty(name).attribute("type", type).end();
    return this;
  }

  public PropertyGroupBuilder withPropertyGroup() {
    return new PropertyGroupBuilder(this);
  }

  public PropertyGroupBuilder referencing(String name) {
    builder().attribute("ref", name);
    return this;
  }

}
