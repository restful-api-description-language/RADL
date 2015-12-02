/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.common.Constant;
import radl.core.code.common.Constants;
import radl.core.code.radl.RadlCode;
import radl.core.code.radl.RadlCode.ResourceMethod;
import radl.java.code.JavaCode;


public class ControllersGenerator extends AbstractControllersGenerator {

  private boolean hasHyperMediaTypes;
  private String startTransition;
  private Constants uriConstants;
  private Constants linkRelationConstants;
  private Constants transitionConstants;

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    Iterator<String> startTransitions = radl.stateTransitionNames("").iterator();
    startTransition = startTransitions.hasNext() ? startTransitions.next() : null;
    hasHyperMediaTypes = (Boolean)context.get(FromRadlCodeGenerator.HAS_HYPERMEDIA);
    uriConstants = (Constants)context.get(URI_CONSTANTS);
    linkRelationConstants = (Constants)context.get(FromRadlCodeGenerator.LINK_RELATION_CONSTANTS);
    transitionConstants = (Constants)context.get(FromRadlCodeGenerator.TRANSITION_CONSTANTS);
    return super.generateFromRadl(radl, context);
  }

  @Override
  protected void generateMethods(RadlCode radl, String resource, JavaCode result) {
    result.add("  @Autowired");
    result.add("  private %s %s;", getControllerSupportClassName(resource), CONTROLLER_SUPPORT_VAR);
    result.add("");
    super.generateMethods(radl, resource, result);
  }

  private String getControllerSupportClassName(String resource) {
    return getControllerClassName(resource, ControllerSupportsGenerator.CLASS_NAME_SUFFIX);
  }

  @Override
  protected void initController(RadlCode radl, String resource, Code controller) {
    String uri = radl.resourceLocation(resource);
    boolean addUris;
    String constantName;
    String type;
    if (transitionsToStart(radl, resource, startTransition)) {
      constantName = BILLBOARD_URL;
      type = API_TYPE;
      addUris = false;
    } else {
      constantName = resource;
      type = URIS_TYPE;
      addUris = true;
    }
    addControllerImports(radl, resource, addUris, controller);
    controller.add("@RestController");
    if (uri != null) {
      Constant constant = uriConstants.add(constantName, uri, null);
      controller.add(String.format("@RequestMapping(%s.%s)", type, constant.getName()));
    }
  }

  private boolean transitionsToStart(RadlCode radl, String resource, String startTransition) {
    for (String method : radl.methodNames(resource)) {
      for (String transition : radl.methodTransitions(resource, method)) {
        if (transition.equals(startTransition)) {
          return true;
        }
      }
    }
    return false;
  }

  private void addControllerImports(RadlCode radl, String resource, boolean addUris, Code controller) {
    controller.add("import org.springframework.beans.factory.annotation.Autowired;");
    boolean hasMethod = radl.methodNames(resource).iterator().hasNext();
    if (hasMethod || !radl.resourceLocation(resource).isEmpty()) {
      controller.add("import org.springframework.web.bind.annotation.RequestMapping;");
    }
    if (hasMethod) {
      controller.add("import org.springframework.web.bind.annotation.RequestMethod;");
    }
    controller.add("import org.springframework.web.bind.annotation.RestController;");
    controller.add("");
    controller.add("import %s;", apiType());
    if (addUris) {
      controller.add("import %s;", urisType());
    }
    controller.add("");
    if (hasMethod) {
      controller.add("import %s.%s.%s;", getPackagePrefix(), IMPL_PACKAGE, SUPPORT_RESPONSE_TYPE);
    }
    controller.add("");
    controller.add("");
  }

  @Override
  protected String getClassNameSuffix() {
    return "Controller";
  }

  @Override
  protected void addControllerMethod(RadlCode radl, String resource, String method, JavaCode controller) {
    String consumes = getConsumes(radl, resource, method);
    String produces = getProduces(radl, resource, method);
    String argName = parameterName(consumes);
    controller.add("  @RequestMapping(method = RequestMethod.%s%s%s)", method.toUpperCase(Locale.getDefault()), consumes,
        produces);
    String type = returnType(produces, radl, resource, method);
    boolean hasReturn = !NO_TYPE.equals(type);
    addReturnTypeImport(type, true, controller);
    controller.ensureImport(RESPONSE_PACKAGE, RESPONSE_TYPE);
    String javaMethod = httpToJavaMethod(method);
    String parameters = parameters(consumes, radl, resource, method, argName, true);
    if (parameters.contains("PathVariable")) {
      controller.ensureImport("org.springframework.web.bind.annotation", "PathVariable");
    }
    if (!argName.isEmpty()) {
      controller.ensureImport("org.springframework.web.bind.annotation", "RequestBody");
    }
    controller.add("  public %s<%s> %s(%s) {", RESPONSE_TYPE, type, javaMethod, parameters);
    parameters = stripParameterTypes(parameters);
    controller.add("    %s<%s> %s = %s.%s(%s);", SUPPORT_RESPONSE_TYPE, type, RESPONSE_VAR,
        CONTROLLER_SUPPORT_VAR, javaMethod, parameters);
    if (hasReturn) {
      controller.add("    %s result = %s.getPayload();", type, RESPONSE_VAR);
      if (hasHyperMediaTypes) {
        addLinks(radl, resource, method, controller, parameters, argName);
      }
    }
    controller.add("    return new %s<%s>(%s%s.getStatus());", RESPONSE_TYPE, type, hasReturn ? "result, " : "",
        RESPONSE_VAR);
    controller.add("  }");
    controller.add("");
  }

  private String stripParameterTypes(String parameters) {
    if (parameters.isEmpty()) {
      return parameters;
    }
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String parameter : parameters.split(",")) {
      result.append(prefix).append(parameter.substring(parameter.lastIndexOf(' ')).trim());
      prefix = ", ";
    }
    return result.toString();
  }

  private void addLinks(RadlCode radl, String resource, String method, JavaCode code, String parameters,
      String argName) {
    Collection<String> parameterList = toCollection(parameters);
    if (!argName.isEmpty()) {
      parameterList.add(argName);
    }
    for (String transition : radl.methodTransitions(resource, method)) {
      for (String state : radl.transitionEnds(transition)) {
        addLinks(radl, state, code, parameterList);
      }
    }
  }

  private Collection<String> toCollection(String parameters) {
    Collection<String> result = new ArrayList<String>();
    for (String parameter : parameters.split(",")) {
      result.add(parameter.trim());
    }
    return result;
  }

  private void addLinks(RadlCode radl, String state, JavaCode code, Collection<String> callerParameters) {
    Collection<String> addedLinkRelations = new HashSet<String>();
    for (String transition : radl.stateTransitionNames(state)) {
      ResourceMethod resourceMethod = radl.transitionMethod(transition);
      String controller = getControllerClassName(resourceMethod.getResource());
      String method = httpToJavaMethod(resourceMethod.getMethod());
      String consumes = getConsumes(radl, resourceMethod.getResource(), resourceMethod.getMethod());
      String arguments = getArguments(consumes, radl, resourceMethod.getResource(), resourceMethod.getMethod(),
          callerParameters);
      code.ensureImport(getPackagePrefix() + '.' + toPackage(resourceMethod.getResource()), controller);
      code.ensureImport("de.escalon.hypermedia.spring", "AffordanceBuilder");
      for (String linkRelation : radl.transitionImplementations(transition)) {
        if (addedLinkRelations.add(linkRelation)) {
          String linkConstant = API_TYPE + '.' + linkRelationConstants.byValue(linkRelation).getName();
          code.add("    if (%s.%s(%s.%s)) {", RESPONSE_VAR, TRANSITITION_CHECK_NAME, ACTIONS_TYPE,
              transitionConstants.byValue(transition).getName());
          code.add("      result.add(AffordanceBuilder");
          code.add("        .linkTo(AffordanceBuilder.methodOn(%s.class).%s(%s))", controller, method, arguments);
          code.add("        .withRel(%s));", linkConstant);
          code.add("    }");
          code.ensureImport(getPackagePrefix() + '.' + IMPL_PACKAGE, ACTIONS_TYPE);
        }
      }
    }
  }

  private String getArguments(String consumes, RadlCode radl, String resource, String method,
      Collection<String> availableParameters) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    String requiredParameters = stripParameterTypes(parameters(consumes, radl, resource, method, NO_PARAMETER, false));
    for (String param : requiredParameters.split(",")) {
      String parameter = param.trim();
      if (parameter.isEmpty()) {
        continue;
      }
      result.append(prefix);
      prefix = ", ";
      if (availableParameters.contains(parameter)) {
        result.append(parameter);
      } else if (NO_PARAMETER.equals(parameter)) {
        result.append("null");
      } else {
        result.append(String.format("%s.getParameter(\"%s\")", RESPONSE_VAR, parameter));
      }
    }
    return result.toString();
  }

}
