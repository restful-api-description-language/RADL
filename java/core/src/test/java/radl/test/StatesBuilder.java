/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import radl.common.xml.DocumentBuilder;


public class StatesBuilder {

  private final RadlBuilder parent;

  public StatesBuilder(RadlBuilder parent) {
    this.parent = parent;
    builder().element("states");
  }
  
  DocumentBuilder builder() {
    return parent.builder();
  }

  public StatesBuilder startingAt(String state) {
    builder().element("start-state");
    return new StateBuilder(this)
        .transitioningTo("Start", state)
    .end();
  }

  public StateBuilder withState(String name) {
    builder().element("state").attribute("name", name);
    return new StateBuilder(this);
  }

  public RadlBuilder end() {
    builder().end();
    return parent;
  }

}
