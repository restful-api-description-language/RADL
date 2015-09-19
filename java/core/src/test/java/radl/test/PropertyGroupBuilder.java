/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import radl.common.xml.DocumentBuilder;


public class PropertyGroupBuilder implements PropertGroupContainer {

  private final PropertGroupContainer parent;

  public PropertyGroupBuilder(PropertGroupContainer parent) {
    this.parent = parent;
    if (parent instanceof RadlBuilder && !"property-groups".equals(builder().getCurrent().getLocalName())) {
      builder().element("property-groups");
    }
    builder().element("property-group");
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
