/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code.radl;

import radl.core.code.xml.NestedXml;

public class PropertyGroups {

  private final NestedXml xml;

  public PropertyGroups(NestedXml xml) {
    this.xml = xml;
  }

  protected NestedXml xml() {
    return xml;
  }
  
  public Iterable<String> names() {
    return xml.items();
  }

  public PropertyGroup item(String name) {
    return new PropertyGroup(xml.item(name));
  }

  @Override
  public String toString() {
    return xml.toString();
  }

}
