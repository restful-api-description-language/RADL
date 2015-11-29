/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Collection;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.java.code.JavaCode;


public class IdentifiableGenerator extends FromRadlErrorsCodeGenerator {

  @Override
  protected void generateFromRadlErrors(RadlCode radl, Iterable<String> errors, Map<String, Object> context,
      Collection<Code> sources) {
    sources.add(generateIdentifiable());
  }

  private Code generateIdentifiable() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", IDENTIFIABLE_TYPE);
    result.add("");
    result.add("  String getId();");
    result.add("");
    result.add("}");
    return result;
  }

}
