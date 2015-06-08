/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import org.jsoup.nodes.Document;


public interface Value {

  String get(Document document);

}
