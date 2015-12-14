/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Default implementation of {@linkplain CodeBaseGenerator} that defers to a suite of {@linkplain CodeGenerator}s.
 */
public class CodeBaseGeneratorImpl implements CodeBaseGenerator {

  private final CodeGenerator[] generators;
  private final String packagePrefix;
  private final String fileHeader;

  public CodeBaseGeneratorImpl(String packagePrefix, String fileHeader, CodeGenerator... generators) {
    this.packagePrefix = packagePrefix;
    this.fileHeader = fileHeader;
    this.generators = generators;
  }

  @Override
  public void generate(List<Module> source, List<Module> destination) {
    Map<String, Object> context = newContext();
    context.put(CodeGenerator.OUTPUT_MODULES, destination);
    for (CodeGenerator generator : generators) {
      moduleFor(destination, generator).addAll(generator.generateFrom(source, context));
    }
  }

  protected Map<String, Object> newContext() {
    Map<String, Object> result = new HashMap<String, Object>();
    result.put(CodeGenerator.PACKAGE_PREFIX, packagePrefix);
    result.put(CodeGenerator.FILE_HEADER, fileHeader);
    return result;
  }

  /**
   * Determine what module should contain generated code.
   * @param modules The available output modules
   * @param generator The generator
   * @return The module to store the generated code into
   */
  protected Module moduleFor(List<Module> modules, CodeGenerator generator) {
    return modules.get(0);
  }

}
