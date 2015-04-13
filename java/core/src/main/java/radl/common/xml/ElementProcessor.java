/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.xml;

import org.w3c.dom.Element;


/**
 * Process an XML DOM {@linkplain Element}.
 */
public interface ElementProcessor {

  void process(Element element) throws Exception;

}
