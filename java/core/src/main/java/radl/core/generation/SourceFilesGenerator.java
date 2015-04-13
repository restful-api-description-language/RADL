/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.io.File;

import org.w3c.dom.Document;

import radl.core.code.SourceFile;


/**
 * Generates source files from a RADL document.
 */
public interface SourceFilesGenerator {

  Iterable<SourceFile> generateFrom(Document radl, File baseDir);

}
