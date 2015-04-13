/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import org.w3c.dom.Document;

import radl.common.xml.DocumentBuilder;


/**
 * Data test builder for RADL methods.
 */
public class MethodBuilder {

  private final ResourceBuilder parent;

  public MethodBuilder(ResourceBuilder parent) {
    this.parent = parent;
  }

  public MethodBuilder consuming(String mediaTypeId) {
    message("request", mediaTypeId);
    return this;
  }

  private void message(String type, String mediaTypeId) {
    builder().element(type)
        .element("representations")
            .element("representation")
                .attribute("media-type", mediaTypeId)
            .end()
        .end()
    .end();
  }

  private DocumentBuilder builder() {
    return parent.builder();
  }

  public MethodBuilder producing(String mediaTypeId) {
    message("response", mediaTypeId);
    return this;
  }

  public MethodBuilder add(String method) {
    builder().element("method").attribute("name", method);
    return this;
  }

  public Document build() {
    return end().build();
  }

  public RadlBuilder end() {
    return parent.parent();
  }

  public ResourceBuilder and() {
    builder().end();
    return parent;
  }

  public ResourceBuilder transitioningTo(String name) {
    builder().element("transitions")
        .element("transition")
            .attribute("name", name)
        .end()
    .end()
    .end().end();
    return parent;
  }

}
