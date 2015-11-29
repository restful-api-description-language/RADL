/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import radl.core.code.Code;


public interface CodeGenerator {

  String FILE_HEADER = "file.header";
  String PACKAGE_PREFIX = "package.prefix";
  String OUTPUT_MODULES = "modules.output";

  /**
   * Generate code from input code.
   * @param input The input modules from which to generate code
   * @param context Context that is shared between code generators
   * @return The generated code
   */
  Collection<Code> generateFrom(List<Module> input, Map<String, Object> context);

}
