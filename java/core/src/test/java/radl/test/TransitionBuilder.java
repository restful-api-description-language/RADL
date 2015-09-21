package radl.test;

import radl.common.xml.DocumentBuilder;

public class TransitionBuilder {

  private final StateBuilder parent;

  public TransitionBuilder(StateBuilder parent, String name, String state) {
    this.parent = parent;
    builder().element("transitions")
      .element("transition")
          .attribute("name", name)
          .attribute("to", state);
  }

  private DocumentBuilder builder() {
    return parent.builder();
  }

  public StateBuilder end() {
    builder().end().end();
    return parent;
  }

  public TransitionBuilder withInput(String propertyGroup) {
    builder().element("input").attribute("property-group", propertyGroup).end();
    return this;
  }

}
