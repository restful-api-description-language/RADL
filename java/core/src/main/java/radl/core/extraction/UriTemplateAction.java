/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.extraction;

import java.util.Collection;

interface UriTemplateAction {

  boolean isFinal();

  void execute(Collection<String> values);

}