/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import java.util.Map;
import java.util.Map.Entry;

import org.w3c.dom.Document;

import radl.common.xml.DocumentBuilder;
import radl.test.ErrorBuilder.Error;


/**
 * Data test builder for RADL documents.
 */
public class RadlBuilder {

  private static final RandomData RANDOM = new RandomData();
  private static final int NAME_LENGTH = 5;

  private final DocumentBuilder builder = DocumentBuilder.newDocument()
      .namespace("urn:radl:service")
      .element("service")
          .attribute("name", aName());

  String aName() {
    return RANDOM.string(NAME_LENGTH);
  }

  DocumentBuilder builder() {
    return builder;
  }

  public ResourceBuilder withResource() {
    return new ResourceBuilder(this);
  }

  public RadlBuilder withMediaTypes(String... names) {
    return withCollection("media-types", "media-type", names);
  }

  private RadlBuilder withCollection(String collectionTag, String itemTag, String... names) {
    builder.element(collectionTag);
    for (String name : names) {
      builder.element(itemTag)
          .attribute("name", name)
      .end();
    }
    builder.end();
    return this;
  }

  public Document build() {
    return builder.build();
  }

  public static RadlBuilder aRadlDocument() {
    return new RadlBuilder();
  }

  public LinkRelationsBuilder withLinkRelations() {
    return new LinkRelationsBuilder(this);
  }

  public RadlBuilder withLinkRelations(String... names) {
    LinkRelationsBuilder linkBuilder = withLinkRelations();
    for (String name : names) {
      linkBuilder = linkBuilder.linkRelation(name, null);
    }
    return linkBuilder.end();
  }

  public RadlBuilder startingAt(String state) {
    builder.element("states")
        .element("start-state")
            .element("transitions")
                .element("transition")
                    .attribute("name", "Start")
                    .attribute("to", state)
            .end()
        .end()
        .element("state")
            .attribute("name", state)
        .end()
    .end();
    return this;
  }

  public ErrorBuilder withErrors() {
    return new ErrorBuilder(this);
  }

  void setErrors(Map<String, Error> errors) {
    builder.element("errors");
    for (Entry<String, Error> entry : errors.entrySet()) {
      builder.element("error").attribute("name", entry.getKey());
      Error error = entry.getValue();
      if (error.hasStatusCode()) {
        builder.attribute("status-code", Integer.toString(error.getStatusCode()));
      }
      if (error.getDocumentation() != null) {
        builder.element("documentation", error.getDocumentation());
      }
      builder.end();
    }
    builder.end();
  }

  public void addLinkRelations(Map<String, String> linkRelations) {
    builder.element("link-relations");
    for (Entry<String, String> entry : linkRelations.entrySet()) {
      builder.element("link-relation")
          .attribute("name", entry.getKey());
      if (entry.getValue() != null) {
        builder.element("specification")
            .attribute("href", entry.getValue())
        .end();
      }
      builder.end();
    }
    builder.end();
  }

}
