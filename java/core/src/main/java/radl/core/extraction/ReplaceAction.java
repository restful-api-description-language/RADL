/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.extraction;

import java.util.Collection;

class ReplaceAction implements UriTemplateAction {

  private final String oldValue;
  private final String newValue;

  public ReplaceAction(String oldValue, String newValue) {
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  @Override
  public boolean isFinal() {
    return false;
  }

  @Override
  public void execute(Collection<String> values) {
    values.remove(oldValue);
    values.add(newValue);
  }

}