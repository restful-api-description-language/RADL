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

  public CodeBaseGeneratorImpl(CodeGenerator... generators) {
    this.generators = generators;
  }
  
  @Override
  public void generate(List<Module> source, List<Module> destination) {
    Map<String, Object> context = newContext();
    for (CodeGenerator generator : generators) {
      moduleFor(destination, generator).addAll(generator.generateFrom(source, context));
    }
  }

  protected Map<String, Object> newContext() {
    return new HashMap<String, Object>();
  }

  protected Module moduleFor(List<Module> modules, CodeGenerator generator) {
    return modules.get(0);
  }

}
