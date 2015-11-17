/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;


public class CompositeCodeGenerator implements CodeGenerator {
  
  private final CodeGenerator[] generators;

  public CompositeCodeGenerator(CodeGenerator... generators) {
    this.generators = generators;
  }

  @Override
  public void generate(Module source, Module generated, Module skeleton) {
    for (CodeGenerator generator : generators) {
      generator.generate(source, generated, skeleton);
    }
  }

}
