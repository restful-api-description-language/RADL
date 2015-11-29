/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.common.Constants;
import radl.core.code.radl.RadlCode;
import radl.java.code.JavaCode;


public class UrisGenerator extends FromRadlCodeGenerator {

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    Constants uriConstants = (Constants)context.get(URI_CONSTANTS);
    return Arrays.asList(generateUris(uriConstants));
  }

  private Code generateUris(Constants uriConstants) {
    JavaCode result = new JavaCode();
    addPackage(IMPL_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", URIS_TYPE);
    addUris(uriConstants, result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addUris(Constants uriConstants, JavaCode code) {
    addConstants(uriConstants.filter(BILLBOARD_URL, false), code);
  }

}
