/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.io.File;

import org.w3c.dom.Document;


/**
 * Extracts a (partial) RADL file from a directory of source files.
 */
public interface RadlExtractor {

  Document extractFrom(String serviceName, File baseDir, ExtractOptions options);

}
