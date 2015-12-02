/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.common.Constants;
import radl.core.code.radl.MediaType;
import radl.core.code.radl.RadlCode;
import radl.java.code.JavaCode;


public class ApiGenerator extends FromRadlCodeGenerator {

  private Constants errorConstants;
  private Constants uriConstants;
  private Constants linkRelationConstants;
  private Constants mediaTypeConstants;
  private MediaType defaultMediaType;

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    errorConstants = (Constants)context.get(FromRadlCodeGenerator.ERROR_CONSTANTS);
    uriConstants = (Constants)context.get(FromRadlCodeGenerator.URI_CONSTANTS);
    linkRelationConstants = (Constants)context.get(FromRadlCodeGenerator.LINK_RELATION_CONSTANTS);
    mediaTypeConstants = (Constants)context.get(FromRadlCodeGenerator.MEDIA_TYPE_CONSTANTS);
    defaultMediaType = (MediaType)context.get(FromRadlCodeGenerationInitializer.DEFAULT_MEDIA_TYPE);
    return Arrays.asList(generateApi());
  }

  private Code generateApi() {
    JavaCode result = new JavaCode();
    addPackage(API_PACKAGE, result);
    result.add("");
    result.add("");
    result.add("public interface %s {", API_TYPE);
    addConstants(uriConstants.filter(BILLBOARD_URL, true), result);
    addConstants(linkRelationConstants, result);
    addConstants(errorConstants, result);
    addConstants(mediaTypeConstants, result);
    addDefaultMediaType(result);
    result.add("");
    result.add("}");
    return result;
  }

  private void addDefaultMediaType(Code code) {
    if (defaultMediaType != null) {
      if (!mediaTypeConstants.all().iterator().hasNext()) {
        addConstantsHeading(mediaTypeConstants.getDescription(), code);
      }
      String defaultMediaTypeConstant = getLocalMediaTypeConstant(mediaTypeConstants, defaultMediaType.name());
      code.add("  String %s = \"%s\";", defaultMediaTypeConstant, defaultMediaType.name());
      code.add("  String %s = %s;", DEFAULT_MEDIA_TYPE_CONSTANT, defaultMediaTypeConstant);
    }
  }

}
