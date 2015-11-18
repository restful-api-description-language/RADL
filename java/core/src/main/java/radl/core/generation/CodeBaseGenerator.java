/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.util.List;

/**
 * Generates code from a RADL document.
 */
public interface CodeBaseGenerator {

  /**
   * Generate a code base.
   * @param source The input modules from which to generate code
   * @param destination The output modules in which code gets generated
   */
  void generate(List<Module> source, List<Module>destination);

}
