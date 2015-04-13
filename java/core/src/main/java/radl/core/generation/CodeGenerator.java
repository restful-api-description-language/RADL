/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import org.w3c.dom.Document;

import radl.core.code.Code;


/**
 * Generates code from a RADL document.
 */
public interface CodeGenerator {

  Iterable<Code> generateFrom(Document radl);

}
