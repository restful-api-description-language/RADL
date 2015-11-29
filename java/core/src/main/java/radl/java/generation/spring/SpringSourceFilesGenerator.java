/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.core.code.GeneratedSourceFile;
import radl.core.code.SourceFile;
import radl.core.code.radl.RadlCode;
import radl.core.generation.CodeBaseGenerator;
import radl.core.generation.Module;
import radl.core.generation.SourceFilesGenerator;
import radl.java.code.JavaCode;


/**
 * * Generates Java source files for the Spring framework from a RADL document.
 */
public class SpringSourceFilesGenerator implements SourceFilesGenerator {

  private final CodeBaseGenerator codeGenerator;
  private final String generatedSourceSetDir;
  private final String mainSourceSetDir;

  public SpringSourceFilesGenerator(String packagePrefix, String generatedSourceSetDir, String mainSourceSetDir,
      String header) {
    this(new SpringCodeBaseGenerator(packagePrefix, header), generatedSourceSetDir, mainSourceSetDir);
  }

  SpringSourceFilesGenerator(CodeBaseGenerator codeGenerator, String generatedSourceSetDir, String mainSourceSetDir) {
    this.codeGenerator = codeGenerator;
    this.generatedSourceSetDir = toDir(generatedSourceSetDir);
    this.mainSourceSetDir = toDir(mainSourceSetDir);
  }

  private String toDir(String path) {
    return path.endsWith(File.separator) ? path : path + File.separator;
  }

  @Override
  public Iterable<SourceFile> generateFrom(Document radl, File baseDir) {
    Collection<SourceFile> result = new ArrayList<SourceFile>();
    Module input = new Module();
    input.add(new RadlCode(radl));
    Module generated = new Module();
    Module skeleton = new Module();
    codeGenerator.generate(Arrays.asList(input), Arrays.asList(generated, skeleton));
    for (Code code : generated) {
      String path = codeToPath(baseDir, generatedSourceSetDir, (JavaCode)code);
      result.add(new GeneratedSourceFile(path, code));
    }
    for (Code code : skeleton) {
      String path = codeToPath(baseDir, mainSourceSetDir, (JavaCode)code);
      result.add(new SourceFile(path, code));
    }
    return result;
  }

  private String codeToPath(File baseDir, String sourceSetDir, JavaCode code) {
    try {
      return new File(baseDir, sourceSetDir + directoryFor(code) + File.separator + fileFor(code)).getCanonicalPath();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String directoryFor(JavaCode code) {
    String packageName = code.packageName();
    return packageName.isEmpty() ? "." : packageName.replaceAll("\\.", "\\" + File.separator);
  }

  private String fileFor(JavaCode code) {
    return String.format("%s.java", code.simpleTypeName());
  }

}
