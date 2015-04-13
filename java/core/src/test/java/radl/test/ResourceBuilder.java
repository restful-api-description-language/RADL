/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import org.w3c.dom.Document;

import radl.common.xml.DocumentBuilder;


/**
 * Data test builder for RADL resources.
 */
public class ResourceBuilder {

  private final RadlBuilder parent;

  public ResourceBuilder(RadlBuilder parent) {
    this.parent = parent;
    if (!"resources".equals(builder().getCurrent().getLocalName())) {
      builder().element("resources");
    }
    builder().element("resource");
  }

  DocumentBuilder builder() {
    return parent.builder();
  }

  public RadlBuilder parent() {
    return parent;
  }

  public MethodBuilder withMethod(String method) {
    ensureName();
    ensureMethods();
    return new MethodBuilder(this).add(method);
  }

  private void ensureName() {
    if ("resource".equals(builder().getCurrent().getLocalName())
        && builder().getCurrent().getAttributes().getNamedItemNS(null, "name") == null) {
      named(parent.aName());
    }
  }

  public ResourceBuilder named(String name) {
    builder().attribute("name", name);
    return this;
  }

  private DocumentBuilder ensureMethods() {
    if (!"methods".equals(builder().getCurrent().getLocalName())) {
      return builder().element("methods");
    }
    return builder();
  }

  public ResourceBuilder locatedAt(String uri) {
    return located("uri", uri);
  }

  private ResourceBuilder located(String attribute, String uri) {
    ensureName();
    builder().element("location").attribute(attribute, uri).end();
    return this;
  }

  public ResourceBuilder locatedAtTemplate(String uriTemplate) {
    return located("uri-template", uriTemplate);
  }

  public Document build() {
    return and().build();
  }

  public RadlBuilder and() {
    ensureName();
    builder().end();
    return parent;
  }

}
