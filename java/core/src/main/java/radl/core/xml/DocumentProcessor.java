/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.xml;

import org.w3c.dom.Document;


/**
 * Process XML DOM {@linkplain Document}s.
 */
public interface DocumentProcessor {

  /**
   * Process a given XML DOM Document. May be called multiple times.
   * @param document The document to process
   */
  void process(Document document);

  /**
   * @return The result of all the processing
   */
  Document result();

}
