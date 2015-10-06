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
    while (!"states".equals(builder().getCurrent().getLocalName())) {
      builder().end();
    }
    return parent;
  }

  public StateBuilder transitioningTo(String name, String state) {
    return withTransition(name, state).end();
  }

  public TransitionBuilder withTransition(String name, String state) {
    if (!"transitions".equals(builder().getCurrent().getLocalName())) {
      builder().element("transitions");
    }
    return new TransitionBuilder(this, name, state);
  }

  public StateBuilder containing(String propertyGroup) {
    builder().attribute("property-group", propertyGroup);
    return this;
  }
}
