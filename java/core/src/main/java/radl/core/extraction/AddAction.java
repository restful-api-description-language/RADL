/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.extraction;

import java.util.Collection;

class AddAction implements UriTemplateAction {

  private final String value;

  public AddAction(String value) {
    this.value = value;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public void execute(Collection<String> values) {
    values.add(value);
  }

}