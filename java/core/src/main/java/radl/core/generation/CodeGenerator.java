/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

/**
 * Generates code from a RADL document.
 */
public interface CodeGenerator {

  /**
   * Generate code.
   * @param source The input files from which to generate code.
   * @param generated The generated files. These are under control of the code generator and will always be override.
   * @param skeleton The skeleton files. These will be generated once, but not overridden.
   */
  void generate(Module source, Module generated, Module skeleton);

}
