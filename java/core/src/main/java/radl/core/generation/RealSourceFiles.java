/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import radl.common.io.IO;
import radl.core.code.GeneratedSourceFile;
import radl.core.code.SourceFile;
import radl.core.enforce.Reality;
import radl.core.scm.SourceCodeManagementSystem;


/**
 * Describes the actual source files that supposedly implement a RADL document.
 */
public class RealSourceFiles implements Reality<String, SourceFile> {

  private final File baseDir;
  private final SourceCodeManagementSystem scm;
  private final String generatedSourceDir;
  private final String mainSourceSetDir;
  private final String codeDir;

  /**
   * @param baseDir
   *          The root of the file system where the files are stored
   * @param generatedSourceDir
   *          The sub-directory of <code>baseDir</code> where generated files are stored,
   *          e.g. <code>build/src/java</code>
   * @param manualSourceDir
   *          The sub-directory of <code>baseDir</code> where manually written source files are stored,
   *          e.g. <code>src/main/java</code>
   * @param codeDir
   *          The sub-directory of <code>generatedSourceDir</code> and <code>manualSourceDir</code> where file are
   *          stored, e.g. <code>com/mycompany/myapp</code>
   * @param scm
   *          The source code management system that manages the files
   */
  public RealSourceFiles(File baseDir, String generatedSourceDir, String manualSourceDir,
      String codeDir, SourceCodeManagementSystem scm) {
    this.baseDir = baseDir;
    this.generatedSourceDir = generatedSourceDir + File.separator;
    this.mainSourceSetDir = manualSourceDir + File.separator;
    this.codeDir = codeDir;
    this.scm = scm;
  }

  @Override
  public Collection<String> getIds() {
    List<String> result = new ArrayList<String>();
    addPaths(new File(baseDir, generatedSourceDir + codeDir), result);
    addPaths(new File(baseDir, mainSourceSetDir + codeDir), result);
    Collections.sort(result);
    return result;
  }

  private void addPaths(File file, Collection<String> paths) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        addPaths(child, paths);
      }
    } else if (file.isFile()) {
      try {
        paths.add(file.getCanonicalPath());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  public SourceFile get(String id) {
    return id.contains(File.separator + generatedSourceDir) ? new GeneratedSourceFile(id)
        : new SourceFile(id);
  }

  @Override
  public void add(String path, SourceFile sourceFile) {
    File file = getUpdatableFile(path);
    try {
      PrintWriter writer = new PrintWriter(file, "UTF8");
      try {
        writer.print(sourceFile.code().text());
      } finally {
        writer.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private File getUpdatableFile(String path) {
    File result = toFile(path);
    result.getParentFile().mkdirs();
    scm.prepareForUpdate(result);
    return result;
  }

  private File toFile(String path) {
    return new File(path.replace(File.separatorChar, '/'));
  }

  @Override
  public void remove(String path) {
    File result = toFile(path);
    scm.prepareForDelete(result);
    IO.delete(result);
  }

  @Override
  public void update(String path, SourceFile oldSourceFile, SourceFile newSourceFile) {
    remove(path);
    add(path, newSourceFile);
  }

}
