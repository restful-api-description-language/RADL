/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import radl.common.xml.DocumentBuilder;


public class StateBuilder {

  private final StatesBuilder parent;

  public StateBuilder(StatesBuilder parent) {
    this.parent = parent;
  }
  
  DocumentBuilder builder() {
    return parent.builder();
  }

  public StatesBuilder end() {
    builder().end();
    return parent;
  }

  public StateBuilder transitioningTo(String state) {
    builder().element("transitions")
        .element("transition")
            .attribute("name", "Start")
            .attribute("to", state)
        .end()
    .end();
    return this;
  }

  public StateBuilder containing(String propertyGroup) {
    builder().attribute("property-group", propertyGroup);
    return this;
  }
}
