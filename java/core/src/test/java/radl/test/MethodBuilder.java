/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import radl.common.xml.DocumentBuilder;


/**
 * Data test builder for RADL methods.
 */
public class MethodBuilder {

  private final ResourceBuilder parent;

  public MethodBuilder(ResourceBuilder parent, String method) {
    this.parent = parent;
    builder().element("method").attribute("name", method);
  }

  public MethodBuilder consuming() {
    return message("request");
  }

  private MethodBuilder message(String type) {
    builder().element(type).end();
    return this;
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

  public MethodBuilder producing() {
    return message("response");
  }

  public MethodBuilder producing(String mediaTypeId) {
    message("response", mediaTypeId);
    return this;
  }

  public MethodBuilder and(String method) {
    builder().end();
    return new MethodBuilder(parent, method);
  }

  public ResourceBuilder end() {
    builder().end().end();
    return parent;
  }

  public MethodBuilder transitioningTo(String name) {
    builder().element("transitions")
        .element("transition")
            .attribute("ref", name)
        .end()
    .end();
    return this;
  }

}
