/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.extraction;

import java.util.Collection;

class IgnoreAction implements UriTemplateAction {

  @Override
  public boolean isFinal() {
    return true;
  }

  @Override
  public void execute(Collection<String> values) {
    // Nothing to do
  }

}