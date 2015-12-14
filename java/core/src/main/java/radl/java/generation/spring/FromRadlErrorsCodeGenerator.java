/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.java.code.Java;


public abstract class FromRadlErrorsCodeGenerator extends FromRadlCodeGenerator {

  protected static final String ERROR_DTO_TYPE = "Error" + FromRadlCodeGenerator.DTO_SUFFIX;
  protected static final String IDENTIFIABLE_TYPE = "Identifiable";
  protected static final int BAD_REQUEST = 400;
  protected static final int INTERNAL_SERVER_ERROR = 500;

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    Iterable<String> errors = radl.errors();
    if (!errors.iterator().hasNext()) {
      return Collections.emptyList();
    }
    Collection<Code> result = new ArrayList<>();
    generateFromRadlErrors(radl, errors, context, result);
    return result;
  }

  protected abstract void generateFromRadlErrors(RadlCode radl, Iterable<String> errors, Map<String, Object> context,
      Collection<Code> sources);

  protected String toExceptionTypeName(String name) {
    return Java.toIdentifier(name + "Exception");
  }

}
