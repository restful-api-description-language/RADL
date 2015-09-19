package radl.test;

import radl.common.xml.DocumentBuilder;


public class PropertyBuilder {

  private final PropertyGroupBuilder parent;

  public PropertyBuilder(PropertyGroupBuilder parent, String name) {
    this.parent = parent;
    builder().element("property").attribute("name", name);
  }

  DocumentBuilder builder() {
    return parent.builder();
  }

  public PropertyGroupBuilder end() {
    builder().end();
    return parent;
  }

  public PropertyBuilder as(String type) {
    builder().attribute("type", type);
    return this;
  }

  public PropertyBuilder repeating() {
    builder().attribute("repeats", "true");
    return this;
  }

  public PropertyBuilder meaning(String uri) {
    builder().attribute("uri", uri);
    return this;
  }

}

