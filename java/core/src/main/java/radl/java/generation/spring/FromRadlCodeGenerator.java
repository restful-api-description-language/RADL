/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.core.generation.CodeGenerator;
import radl.core.generation.Module;


public abstract class FromRadlCodeGenerator implements CodeGenerator {

  static final String FILE_HEADER = "file.header";
  static final String PACKAGE_PREFIX = "package.prefix";
  static final String DEFAULT_MEDIA_TYPE = "mediatypes.default";
  static final String HAS_HYPERMEDIA = "mediatypes.has.hypermedia";
  static final String CONSTANTS_LINK_RELATIONS = "constants.link.relations";
  static final String SPRING_HTTP_STATUSES = "spring.http.statuses";

  @Override
  public Collection<Code> generateFrom(List<Module> input, Map<String, Object> context) {
    RadlCode radl = (RadlCode)input.get(0).get(0);
    return generateFromRadl(radl, context);
  }

  protected abstract Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context);

}
