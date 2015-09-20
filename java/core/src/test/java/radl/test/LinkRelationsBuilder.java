/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import org.w3c.dom.Document;

import radl.common.xml.DocumentBuilder;


public class LinkRelationsBuilder {

  private final RadlBuilder parent;

  public LinkRelationsBuilder(RadlBuilder parent) {
    this.parent = parent;
    builder().element("link-relations");
  }

  DocumentBuilder builder() {
    return parent.builder();
  }
  
  public LinkRelationBuilder withLinkRelation(String name, String specificationUri) {
    builder().element("link-relation").attribute("name", name);
    if (specificationUri != null) {
      builder().element("specification")
          .attribute("href", specificationUri)
      .end();
    }
    return new LinkRelationBuilder(this);
  }

  public RadlBuilder end() {
    builder().end();
    return parent;
  }

  public Document build() {
    return end().build();
  }

}
