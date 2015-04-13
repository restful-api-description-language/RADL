/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.w3c.dom.Document;

import radl.core.code.SourceFile;
import radl.core.enforce.Desired;


/**
 * Describes the source files that would implement a RADL document.
 */
public class DesiredSourceFiles implements Desired<String, SourceFile> {

  private final Document radl;
  private final SourceFilesGenerator generator;
  private final File baseDir;
  private Map<String, SourceFile> sourceFiles;

  /**
   * @param radl The RESTful API description that represents the desired state
   * @param generator A generator that can generate source files from a RESTful API description
   * @param baseDir Directory under which to generate source files
   */
  public DesiredSourceFiles(Document radl, SourceFilesGenerator generator, File baseDir) {
    this.radl = radl;
    this.generator = generator;
    this.baseDir = baseDir;
  }

  @Override
  public Collection<String> getIds() {
    return Collections.unmodifiableCollection(sourceFiles().keySet());
  }

  private Map<String, SourceFile> sourceFiles() {
    if (sourceFiles == null) {
      sourceFiles = new TreeMap<String, SourceFile>();
      for (SourceFile sourceFile : generator.generateFrom(radl, baseDir)) {
        sourceFiles.put(sourceFile.path(), sourceFile);
      }
    }
    return sourceFiles;
  }

  @Override
  public SourceFile get(String id) {
    return sourceFiles().get(id);
  }

}
