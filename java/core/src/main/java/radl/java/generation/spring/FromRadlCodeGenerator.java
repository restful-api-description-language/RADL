/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import radl.common.StringUtil;
import radl.core.code.Code;
import radl.core.code.common.Constant;
import radl.core.code.common.Constants;
import radl.core.code.radl.RadlCode;
import radl.core.generation.CodeGenerator;
import radl.core.generation.Module;
import radl.java.code.Java;
import radl.java.code.JavaBeanProperty;
import radl.java.code.JavaCode;


public abstract class FromRadlCodeGenerator implements CodeGenerator {

  static final String DEFAULT_MEDIA_TYPE = "mediatypes.default";
  static final String HAS_HYPERMEDIA = "mediatypes.has.hypermedia";
  static final String MEDIA_TYPE_CONSTANTS = "constants.media.types";
  static final String TRANSITION_CONSTANTS = "constants.transitions";
  static final String LINK_RELATION_CONSTANTS = "constants.link.relations";
  static final String ERROR_CONSTANTS = "constants.errors";
  static final String URI_CONSTANTS = "constants.uris";
  static final String SPRING_HTTP_STATUSES = "spring.http.statuses";
  static final String DTO_SUFFIX = "Resource";
  static final String API_TYPE = "Api";
  static final String API_PACKAGE = "api";
  static final String IMPL_PACKAGE = "impl";
  static final String NO_PARAMETER = "null";
  static final String UNKNOWN_INPUT_TYPE = "Object";
  static final String UNKNOWN_OUTPUT_TYPE = "ResourceSupport";
  static final String UNKNOWN_OUTPUT_TYPE_PACKAGE = "org.springframework.hateoas";
  static final String RESPONSE_TYPE = "ResponseEntity";
  static final String RESPONSE_PACKAGE = "org.springframework.http";
  static final String NO_TYPE = "Void";
  static final String STATUS_TYPE = "HttpStatus";
  static final String STATUS_TYPE_PACKAGE = RESPONSE_PACKAGE;
  static final String BILLBOARD_URL = "BILLBOARD";
  static final String DEFAULT_MEDIA_TYPE_CONSTANT = "MEDIA_TYPE_DEFAULT";
  static final String URIS_TYPE = "Resources";
  static final Collection<Integer> FRAMEWORK_HANDLED_STATUSES = Arrays.asList(405, 406);
  static final String RESPONSE_VAR = "response";
  static final String SUPPORT_RESPONSE_TYPE = "Rest" + StringUtil.initCap(RESPONSE_VAR);
  static final String CONTROLLER_SUPPORT_VAR = "support";
  static final String ACTIONS_TYPE = "Actions";
  static final String TRANSITITION_CHECK_NAME = "allows";
  static final String TRANSITITION_DENY_NAME = "deny";
  private static final String STANDARD_MEDIA_TYPE = "application/";

  private String fileHeader;
  private String packagePrefix;
  
  protected String getPackagePrefix() {
    return packagePrefix;
  }

  @Override
  public Collection<Code> generateFrom(List<Module> input, Map<String, Object> context) {
    fileHeader = (String)context.get(FILE_HEADER);
    packagePrefix = (String)context.get(PACKAGE_PREFIX);
    RadlCode radl = (RadlCode)input.get(0).get(0);
    return generateFromRadl(radl, context);
  }

  protected abstract Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context);

  protected void addPackage(String name, Code code) {
    code.add("/*");
    for (String line : fileHeader.split("\n")) {
      code.add(" * %s", line);
    }
    code.add(" */");
    code.add("package %s.%s;", packagePrefix, toPackage(name));
  }

  protected String toPackage(String name) {
    StringBuilder result = new StringBuilder(name);
    int i = 0;
    while (i < result.length()) {
      char c = result.charAt(i);
      if (Character.isUpperCase(c)) {
        result.setCharAt(i, Character.toLowerCase(c));
      } else if (!Character.isJavaIdentifierPart(c)) {
        int j = i + 1;
        while (j < result.length() && !Character.isJavaIdentifierPart(result.charAt(j))) {
          j++;
        }
        result.delete(i, j);
      }
      i++;
    }
    if (result.charAt(result.length() - 1) == 's') {
      result.setLength(result.length() - 1);
    }
    return result.toString();
  }

  protected void addProperties(Code code, Iterable<JavaBeanProperty> properties) {
    if (!properties.iterator().hasNext()) {
      return;
    }
    for (JavaBeanProperty property : properties) {
      if (property.getAnnotation() != null) {
        code.add("%s", property.getAnnotation());
      }
      code.add("  public %s %s;", property.getType(), property.getName());
    }
    code.add("");
  }

  static String getDtoClass(String name) {
    return Java.toIdentifier(name) + DTO_SUFFIX;
  }

  protected String apiType() {
    return join(packagePrefix, API_PACKAGE, API_TYPE);
  }

  protected String urisType() {
    return join(packagePrefix, IMPL_PACKAGE, URIS_TYPE);
  }

  protected String join(String... parts) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String part : parts) {
      result.append(prefix).append(part);
      prefix = ".";
    }
    return result.toString();
  }

  protected String getErrorName(String name) {
    URI uri;
    try {
      uri = new URI(name);
    } catch (URISyntaxException e) {
      return name;
    }
    if (uri.getScheme() == null || !uri.getScheme().startsWith("http")) {
      return name;
    }
    String path = uri.getPath();
    if (path.endsWith("/")) {
      path = path.substring(0, path.length() - 1);
    }
    return path.substring(path.lastIndexOf('/') + 1);
  }

  protected void addConstants(Constants constants, JavaCode code) {
    Iterator<Constant> iterator = constants.all().iterator();
    if (iterator.hasNext()) {
      String scope = code.isClass() ? "public static " : "";
      addConstantsHeading(constants.getDescription(), code);
      while (iterator.hasNext()) {
        Constant constant = iterator.next();
        if (constant.getComments().length > 0) {
          code.add("  /**");
          for (String comment : constant.getComments()) {
            code.add("   * %s", comment);
          }
          code.add("   */");
        }
        code.add("  %sString %s = \"%s\";", scope, constant.getName(), constant.getValue());
      }
    }
  }

  protected void addConstantsHeading(String heading, Code code) {
    code.add("");
    if (heading.isEmpty()) {
      return;
    }
    code.add("");
    code.add("  // %s", heading);
    code.add("");
  }

  protected String getMediaTypeConstant(Constants mediaTypeConstants, String mediaType) {
    return API_TYPE + '.' + getLocalMediaTypeConstant(mediaTypeConstants, mediaType);
  }

  protected String getLocalMediaTypeConstant(Constants mediaTypeConstants, String mediaType) {
    String name = mediaType.startsWith(STANDARD_MEDIA_TYPE)
        ? mediaType.substring(STANDARD_MEDIA_TYPE.length()) : mediaType;
    return mediaTypeConstants.add(name, mediaType, null).getName();
  }

}
