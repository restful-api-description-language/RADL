/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import org.w3c.dom.Document;


/**
 * Merge a resource model into an existing RADL document.
 */
public interface ResourceModelMerger {

  void setService(String serviceName);

  Document toRadl(ResourceModel resourceModel);

}
