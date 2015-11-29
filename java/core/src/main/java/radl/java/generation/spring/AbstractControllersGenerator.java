/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.common.Constants;
import radl.core.code.radl.MediaType;
import radl.core.code.radl.RadlCode;
import radl.java.code.Java;
import radl.java.code.JavaCode;


public abstract class AbstractControllersGenerator extends FromRadlCodeGenerator {

  private Constants mediaTypeConstants;
  private MediaType defaultMediaType;

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    Collection<Code> result = new ArrayList<Code>();
    mediaTypeConstants = (Constants)context.get(FromRadlCodeGenerator.MEDIA_TYPE_CONSTANTS);
    defaultMediaType = (MediaType)context.get(FromRadlCodeGenerationInitializer.DEFAULT_MEDIA_TYPE);
    for (String resource : radl.resourceNames()) {
      result.add(generateController(radl, resource));
    }
    return result;
  }

  protected Code generateController(RadlCode radl, String resource) {
    JavaCode result = new JavaCode();
    addPackage(resource, result);
    result.add("");
    initController(radl, resource, result);
    result.add("public class %s {", getControllerClassName(resource));
    result.add("");
    generateMethods(radl, resource, result);
    result.add("}");
    return result;
  }
  
  protected String getClassName(String name) {
    return toJavaIdentifier(name);
  }

  public String toJavaIdentifier(String text) {
    StringBuilder result = new StringBuilder(text);
    while (!Character.isJavaIdentifierStart(result.charAt(0))) {
      result.delete(0, 1);
    }
    upcase(result, 0);
    int index = 1;
    while (index < result.length()) {
      char ch = result.charAt(index);
      if (Character.isUpperCase(ch)) {
        result.setCharAt(index, Character.toLowerCase(ch));
      } else if (!Character.isJavaIdentifierPart(ch)) {
        while (index < result.length() && !Character.isJavaIdentifierPart(result.charAt(index))) {
          result.delete(index, index + 1);
        }
        if (index < result.length()) {
          upcase(result, index);
        }
      }
      index++;
    }
    return result.toString();
  }

  private static void upcase(StringBuilder builder, int index) {
    builder.setCharAt(index, Character.toUpperCase(builder.charAt(index)));
  }
  protected void generateMethods(RadlCode radl, String resource, JavaCode result) {
    for (String method : radl.methodNames(resource)) {
      addControllerMethod(radl, resource, method, result);
    }
  }

  protected String getControllerClassName(String resource) {
    return getControllerClassName(resource, getClassNameSuffix());
  }

  protected String getControllerClassName(String resource, String suffix) {
    return getClassName(resource) + suffix;
  }
  
  protected String getConsumes(RadlCode radl, String resource, String method) {
    return getMediaTypes(radl.methodRequestRepresentations(resource, method), "consumes");
  }

  private String getMediaTypes(Iterable<String> mediaTypes, String prefix) {
    Iterator<String> iterator = mediaTypes.iterator();
    if (!iterator.hasNext()) {
      return "";
    }
    String mediaType = iterator.next();
    if (!iterator.hasNext() && defaultMediaType != null && mediaType.equals(defaultMediaType.name())) {
      // Explicit use of default media type
      mediaType = API_TYPE + '.' + DEFAULT_MEDIA_TYPE_CONSTANT;
    } else {
      mediaType = getMediaTypeConstant(mediaTypeConstants, mediaType);
    }
    StringBuilder result = new StringBuilder();
    result.append(", ").append(prefix).append(" = { ").append(mediaType);
    while (iterator.hasNext()) {
      result.append(", ").append(getMediaTypeConstant(mediaTypeConstants, iterator.next()));
    }
    result.append(" }");
    return result.toString();
  }

  protected String getProduces(RadlCode radl, String resource, String method) {
    return getMediaTypes(radl.methodResponseRepresentations(resource, method), "produces");
  }

  protected String parameterName(String consumes) {
    return consumes.isEmpty() ? "" : "input";
  }

  protected String returnType(String produces, RadlCode radl, String resource, String method) {
    final String noType = produces.isEmpty() ? NO_TYPE : UNKNOWN_OUTPUT_TYPE;
    String result = noType;
    for (String transition : radl.methodTransitions(resource, method)) {
      String propertyGroup = getOutputPropertyGroup(radl, transition);
      if (propertyGroup.isEmpty()) {
        result = noType;
      } else {
        String dto = getDtoClass(propertyGroup);
        if (noType.equals(result)) {
          result = dto;
        } else if (!result.equals(dto)) {
          result = UNKNOWN_OUTPUT_TYPE;
        }
      }
    }
    return result;
  }

  private String getOutputPropertyGroup(RadlCode radl, String transition) {
    String result = "";
    for (String state : radl.transitionEnds(transition)) {
      result = radl.statePropertyGroup(state);
      if (!result.isEmpty()) {
        return result;
      }
    }
    return "";
  }

  protected void addReturnTypeImport(String type, boolean importNoType, JavaCode code) {
    if (type.endsWith(DTO_SUFFIX)) {
      code.ensureImport(dtoPackage(type), type);
    } else if (NO_TYPE.equals(type) && importNoType) {
      code.ensureImport(RESPONSE_PACKAGE, RESPONSE_TYPE);
    } else if (UNKNOWN_OUTPUT_TYPE.equals(type)) {
      code.ensureImport(UNKNOWN_OUTPUT_TYPE_PACKAGE, UNKNOWN_OUTPUT_TYPE);
    } else if (SUPPORT_RESPONSE_TYPE.equals(type)) {
      code.ensureImport(getPackagePrefix() + '.' + IMPL_PACKAGE, SUPPORT_RESPONSE_TYPE);
    }
  }

  private String dtoPackage(String dtoType) {
    return join(getPackagePrefix(), toPackage(dtoType.substring(0, dtoType.length() - DTO_SUFFIX.length())));
  }

  protected String httpToJavaMethod(String httpMethod) {
    return httpMethod.toLowerCase(Locale.getDefault());
  }

  protected String parameters(String consumes, RadlCode radl, String resource, String method, String argName,
      boolean addAnnotations) {
    StringBuilder result = new StringBuilder();
    String prefix = appendPath(radl, resource, "", addAnnotations, result);
    if (!consumes.isEmpty()) {
      result.append(prefix);
      if (addAnnotations) {
        result.append("@RequestBody ");
      }
      result.append(parameterType(consumes, radl, resource, method)).append(' ').append(argName);
    }
    return result.toString();
  }

  private String appendPath(RadlCode radl, String resource, String prefix, boolean addAnnotations,
      StringBuilder builder) {
    String result = prefix;
    String location = radl.resourceLocation(resource);
    for (String segment : location.split("/")) {
      if (segment.startsWith("{") && segment.endsWith("}")) {
        builder.append(result);
        String templateVariableName = segment.substring(1, segment.length() - 1);
        if (addAnnotations) {
          builder.append("@PathVariable(\"").append(templateVariableName).append("\") ");
        }
        builder.append("String ").append(Java.toIdentifier(templateVariableName, false));
        result = ", ";
      }
    }
    return result;
  }

  private String parameterType(String consumes, RadlCode radl, String resource, String method) {
    String noType = consumes.isEmpty() ? FromRadlCodeGenerator.NO_TYPE : FromRadlCodeGenerator.UNKNOWN_INPUT_TYPE;
    String result = noType;
    for (String transition : radl.methodTransitions(resource, method)) {
      String propertyGroup = radl.transitionPropertyGroup(transition);
      if (propertyGroup.isEmpty()) {
        result = noType;
      } else {
        String dto = FromRadlCodeGenerator.getDtoClass(propertyGroup);
        if (noType.equals(result)) {
          result = dto;
        } else if (!result.equals(dto)) {
          result = FromRadlCodeGenerator.UNKNOWN_INPUT_TYPE;
        }
      }
    }
    return result;
  }

  protected abstract void addControllerMethod(RadlCode radl, String resource, String method, JavaCode result);
  protected abstract String getClassNameSuffix();
  protected abstract void initController(RadlCode radl, String resource, JavaCode controller);

}
