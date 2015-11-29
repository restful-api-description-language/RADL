/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.java.code.JavaBeanProperty;
import radl.java.code.JavaCode;


public class ErrorDtoGenerator extends FromRadlErrorsCodeGenerator {

  @Override
  protected void generateFromRadlErrors(RadlCode radl, Iterable<String> errors, Map<String, Object> context,
      Collection<Code> sources) {
    sources.add(generateErrorDto());
  }

  private Code generateErrorDto() {
    Code result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public class %s {", ERROR_DTO_TYPE);
    result.add("");
    addProperties(result, Arrays.asList(new JavaBeanProperty("title"), new JavaBeanProperty("type")));
    result.add("}");
    return result;
  }

}
