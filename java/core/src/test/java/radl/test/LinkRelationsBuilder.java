/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Document;

public class LinkRelationsBuilder {

  private final RadlBuilder parent;
  private final Map<String, String> linkRelations = new LinkedHashMap<String, String>();

  public LinkRelationsBuilder(RadlBuilder parent) {
    this.parent = parent;
  }

  public LinkRelationsBuilder linkRelation(String name, String specificationUri) {
    linkRelations.put(name, specificationUri);
    return this;
  }

  public RadlBuilder end() {
    parent.addLinkRelations(linkRelations);
    return parent;
  }

  public Document build() {
    return end().build();
  }

}
