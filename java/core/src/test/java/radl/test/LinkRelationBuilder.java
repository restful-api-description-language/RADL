/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import radl.common.xml.DocumentBuilder;


public class LinkRelationBuilder {

  private final LinkRelationsBuilder parent;

  public LinkRelationBuilder(LinkRelationsBuilder parent) {
    this.parent = parent;
  }

  private DocumentBuilder builder() {
    return parent.builder();
  }
  
  public LinkRelationsBuilder end() {
    builder().end();
    return parent;
  }

  public LinkRelationBuilder implementing(String transition) {
    builder().element("transitions")
        .element("transition")
            .attribute("ref", transition)
        .end()
    .end();
    return this;
  }

}
