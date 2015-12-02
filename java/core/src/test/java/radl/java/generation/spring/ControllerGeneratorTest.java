/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;


public class ControllerGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  @Test
  public void generatesControllerPerResource() {
    String resource = aName() + '-' + aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(resource)
        .end()
    .build();
  
    Iterable<Code> sources = radlToCode(radl);
  
    assertController(resource, sources);
  }

  protected void assertController(String expectedClassName, Iterable<Code> sources) {
    JavaCode javaSource = getType(sources, controllerName(expectedClassName));
  
    assertFileComments(javaSource);
    assertEquals("Class name", controllerName(expectedClassName), javaSource.typeName());
    assertTrue("Missing @RestController", javaSource.typeAnnotations().contains("@RestController"));
    TestUtil.assertCollectionEquals("Imports", Arrays.asList(
        "org.springframework.beans.factory.annotation.Autowired",
        "org.springframework.web.bind.annotation.RestController",
        packagePrefix + ".api." + TYPE_API,
        packagePrefix + ".impl." + TYPE_URIS), javaSource.imports());
    assertEquals("Package", packagePrefix + '.' + expectedClassName.replaceAll("\\-", ""),
        javaSource.packageName());
  
    String fieldName = "support";
    TestUtil.assertCollectionEquals("Fields", Arrays.asList(fieldName), javaSource.fieldNames());
    assertEquals("Field type", controllerSupportName(expectedClassName), javaSource.fieldType(fieldName));
    assertEquals("Field annotation", Arrays.asList("@Autowired"), javaSource.fieldAnnotations(fieldName));
  }

  @Test
  public void addsRequestMappingForResourceLocation() {
    String name = aName();
    String uri = aLocalUri();
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(name)
            .locatedAt(uri)
        .end()
    .build();
  
    Iterable<Code> sources = radlToCode(radl);
  
    JavaCode uris = getType(sources, TYPE_URIS);
    String constant = getFieldWithValue(uris, quote(uri));
    JavaCode controller = getType(sources, controllerName(name));
  
    String requestMappingAnnotation = String.format("@RequestMapping(%s.%s)", TYPE_URIS, constant);
    assertTrue("Missing @RequestMapping: " + controller.typeAnnotations(),
        controller.typeAnnotations().contains(requestMappingAnnotation));
  }

  @Test
  public void addsControllerMethodsForResourceMethods() {
    String resource = aName();
    String httpMethod = aMethod();
    String mediaType1 = aMediaType();
    String mediaType2 = aMediaType();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(mediaType1, mediaType2)
        .withResource()
            .named(resource)
            .withMethod(httpMethod)
                .consuming(mediaType1)
                .producing(mediaType2)
            .end()
        .end()
    .build();
    String method = javaMethodName(httpMethod);
  
    JavaCode source = generateController(radl, resource);
  
    assertImports(Arrays.asList("org.springframework.web.bind.annotation.RequestMapping",
        "org.springframework.web.bind.annotation.RequestMethod"), source);
    TestUtil.assertCollectionEquals("Methods", Arrays.asList(method), source.methods());
    String methodAnnotation = String.format(
        "@RequestMapping(method = RequestMethod.%s, consumes = { %s }, produces = { %s })",
        httpMethod, mediaTypeToConstant(mediaType1, false), mediaTypeToConstant(mediaType2, false));
    assertEquals("Method annotations", Collections.singleton(methodAnnotation).toString(),
        source.methodAnnotations(method).toString());
    assertEquals("Method arguments", "@RequestBody Object input", source.methodArguments(method));
    assertEquals("Method return type", "ResponseEntity<ResourceSupport>", source.methodReturns(method));
    assertTrue("Doesn't import ResourceSupport", source.imports().contains("org.springframework.hateoas.ResourceSupport"));
    assertTrue("Method body calls support", source.methodBody(method).contains(
        String.format("support.%s(input);", method)));
  }

  @Test
  public void generatesClassNamesWithoutSuccessiveCapitals() {
    String name = "PDP";
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(name)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    Code controller = getType(sources, TestUtil.initCap(javaMethodName(name)) + "Controller");
    assertNotNull("Missing controller", controller);
  }

  @Test
  public void addsRequestMappingForResourceWithUriTemplate() {
    String name = aName();
    String uri = aLocalUri();
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(name)
            .locatedAtTemplate(uri)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode uris = getType(sources, TYPE_URIS);
    String constant = getFieldWithValue(uris, quote(uri));
    JavaCode controller = getType(sources, controllerName(name));

    String requestMappingAnnotation = String.format("@RequestMapping(%s.%s)", TYPE_URIS, constant);
    assertTrue("Missing @RequestMapping: " + controller.typeAnnotations(),
        controller.typeAnnotations().contains(requestMappingAnnotation));
  }

  @Test
  public void generatesValidJavaPackage() throws Exception {
    String name1 = aName();
    String name2 = aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(TestUtil.initCap(name1) + " " + name2 + "-")
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode controller = getType(sources, TestUtil.initCap(name1) + TestUtil.initCap(name2) + "Controller");
    assertNotNull("Missing controller", controller);

    String controllerPackage = controller.packageName();
    assertTrue("Package: " + controllerPackage, controllerPackage.endsWith(name1 + name2));
    assertEquals("Package should be all lowercase", javaMethodName(controllerPackage),
        controllerPackage);
  }

  @Test
  public void generatesControllersInPackageWithSingularName() {
    String resource1 = aName() + 'l';
    String resource2 = resource1 + 's';
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(resource1)
        .end()
        .withResource()
            .named(resource2)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode controller1 = getType(sources, controllerName(resource1));
    JavaCode controller2 = getType(sources, controllerName(resource2));
    assertEquals("Package", controller1.packageName(), controller2.packageName());
  }

  @Test
  public void generatesAndUsesApiConstantForDefaultMediaType() {
    String mediaType = aName();
    String fullMediaType = "application/" + mediaType;
    String resource = aName();
    String method = aMethod();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(true, fullMediaType)
        .withResource()
            .named(resource)
            .withMethod(method)
                .consuming(fullMediaType)
                .producing(fullMediaType)
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode controller = getType(sources, controllerName(resource));
    TestUtil.assertCollectionEquals("Method annotations", Collections.<String>singleton(
        "@RequestMapping(method = RequestMethod." + method + ", consumes = { Api." + DEFAULT_MEDIA_TYPE_CONSTANT
        + " }, produces = { Api." + DEFAULT_MEDIA_TYPE_CONSTANT + " })"),
        controller.methodAnnotations(javaMethodName(method)));
  }

  @Test
  public void generatedControllerUsesGeneratedDtos() {
    String state1 = aName();
    String propertyGroup1 = aName();
    String httpMethod1 = aMethod();
    String transition = aName();
    String state2 = aName();
    String propertyGroup2 = aName();
    String httpMethod2 = aMethod();
    Document radl = RadlBuilder.aRadlDocument()
        .withStates()
            .startingAt(state1)
            .withState(state1)
                .containing(propertyGroup1)
                .withTransition(transition, state2)
                    .withInput(propertyGroup2)
                .end()
            .end()
            .withState(state2)
            .end()
        .end()
        .withPropertyGroup()
            .named(propertyGroup1)
            .withProperty(aName())
            .end()
        .end()
        .withPropertyGroup()
            .named(propertyGroup2)
            .withProperty(aName())
            .end()
        .end()
        .withMediaTypes(true, JSON_LD)
        .withResource()
            .named(state1)
            .withMethod(httpMethod1)
                .transitioningTo("Start")
                .producing()
            .end()
        .end()
        .withResource()
            .named(state2)
            .withMethod(httpMethod2)
                .transitioningTo(transition)
                .consuming()
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode controller1 = getType(sources, controllerName(state1));
    String controllerMethod1 = javaMethodName(httpMethod1);
    assertEquals("Returns #1", "ResponseEntity<" + dtoName(propertyGroup1) + ">",
        controller1.methodReturns(controllerMethod1));
    assertEquals("Args #1", "", controller1.methodArguments(controllerMethod1));

    JavaCode controller2 = getType(sources, controllerName(state2));
    String controllerMethod2 = javaMethodName(httpMethod2);
    assertTrue("Imports #2 contains ResponseEntity",
        controller2.imports().contains("org.springframework.http.ResponseEntity"));
    assertEquals("Returns #2", "ResponseEntity<Void>", controller2.methodReturns(controllerMethod2));
    assertEquals("Args #2", "@RequestBody " + dtoName(propertyGroup2) + " input", controller2.methodArguments(controllerMethod2));
  }

  // #45 Generated controllers should add links/forms to responses
  @Test
  public void generatedControllersAddLinks() {
    String state1 = aName();
    String propertyGroup1 = aName();
    String httpMethod1 = aMethod();
    String transition = aName();
    String state2 = aName();
    String httpMethod2 = aMethod();
    String linkRel = aUri();
    Document radl = RadlBuilder.aRadlDocument()
        .withStates()
            .startingAt(state1)
            .withState(state1)
                .containing(propertyGroup1)
                .withTransition(transition, state2)
                .end()
            .end()
            .withState(state2)
            .end()
        .end()
        .withLinkRelations()
            .withLinkRelation(linkRel, null)
                .implementing(transition)
            .end()
        .end()
        .withPropertyGroup()
            .named(propertyGroup1)
            .withProperty(aName())
            .end()
        .end()
        .withMediaTypes(true, JSON_LD)
        .withResource()
            .named(state1)
            .withMethod(httpMethod1)
                .transitioningTo("Start")
                .producing()
            .end()
        .end()
        .withResource()
            .named(state2)
            .withMethod(httpMethod2)
                .transitioningTo(transition)
                .consuming()
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode dto1 = getType(sources, dtoName(propertyGroup1));
    assertEquals("DTO super class", "ResourceSupport", dto1.superTypeName());

    String controllerMethod1 = javaMethodName(httpMethod1);
    JavaCode controllerSupport1 = getType(sources, controllerSupportName(state1));
    assertTrue("Controller doesn't support method",
        controllerSupport1.methodBody(controllerMethod1).contains("new " + dto1.typeName() + "();"));

    JavaCode actions = getType(sources, TYPE_ACTIONS);
    String transitionConstant = transition.toUpperCase(Locale.getDefault());
    assertTrue("Support doesn't have constant for transition: " + actions.fieldNames(),
        actions.fieldNames().contains(transitionConstant));

    JavaCode controller1 = getType(sources, controllerName(state1));
    String controllerName2 = controllerName(state2);
    JavaCode controller2 = getType(sources, controllerName2);
    assertTrue("Controller #1 doesn't import controller #2",
        controller1.imports().contains(controller2.fullyQualifiedName()));
    assertTrue("Controller #1 doesn't import controller link builder",
        controller1.imports().contains("de.escalon.hypermedia.spring.AffordanceBuilder"));
    
    String methodBody = controller1.methodBody(controllerMethod1);
    assertTrue("Controller #1 doesn't add link", methodBody.contains(
        "methodOn(" + controllerName2 + ".class)." + javaMethodName(httpMethod2) + "("));
    assertTrue("Controller doesn't check transition enabled",
        methodBody.contains(TRANSITION_ENABLED_METHOD + "(" + actions.typeName() + '.' + transitionConstant));
  }

  @Test
  public void generatedControllerAddsLinkOnlyOnce() {
    String state1 = aName();
    String state2 = aName();
    String state3 = aName();
    String transition12 = aName();
    String transition13 = aName();
    String linkRel = aUri();
    String httpMethod = aMethod();
    Document radl = RadlBuilder.aRadlDocument()
        .withStates()
            .startingAt(state1)
            .withState(state1)
                .withTransition(transition12, state2)
                .end()
                .withTransition(transition13, state3)
                .end()
            .end()
            .withState(state2)
            .end()
            .withState(state3)
            .end()
        .end()
        .withLinkRelations()
            .withLinkRelation(linkRel, null)
                .implementing(transition12, transition13)
            .end()
        .end()
        .withMediaTypes(true, JSON_LD)
        .withResource()
            .named(state1)
            .withMethod(httpMethod)
                .transitioningTo("Start")
                .producing()
            .end()
        .end()
        .withResource()
            .named(state2)
            .withMethod(httpMethod)
                .transitioningTo(transition12)
                .producing()
            .end()
        .end()
        .withResource()
            .named(state3)
            .withMethod(httpMethod)
                .transitioningTo(transition13)
                .producing()
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);
    
    JavaCode controller = getType(sources, controllerName(state1));
    int numLinks = 0;
    for (String line : controller.methodBody(javaMethodName(httpMethod)).split("\n")) {
      if (line.trim().startsWith("if (" + TRANSITION_ENABLED_METHOD)) {
        numLinks++;
      }
    }
    assertEquals("#links", 1, numLinks);
  }

  @Test
  public void generatesPathVariableForUriTemplateVariable() {
    String name = aName();
    String param = aName();
    String uriTemplate = aLocalUri() + '{' + param + "}/";
    String httpMethod = aMethod();
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(name)
            .locatedAtTemplate(uriTemplate)
            .withMethod(httpMethod)
            .end()
        .end()
        .build();

    Iterable<Code> sources = radlToCode(radl);
    
    JavaCode controller = getType(sources, controllerName(name));
    String method = javaMethodName(httpMethod);
    assertEquals("Method arguments", "@PathVariable(\"" + param + "\") String " + param,
        controller.methodArguments(method));
  }

  @Test
  public void getMissingControllerMethodVariablesFromResponse() {
    String state1 = aName();
    String httpMethod1 = aMethod();
    String transition = aName();
    String state2 = aName();
    String httpMethod2 = aMethod();
    String linkRel = aUri();
    String param = aName();
    String uriTemplate = aLocalUri() + '{' + param + "}/";
    Document radl = RadlBuilder.aRadlDocument()
        .withStates()
            .startingAt(state1)
            .withState(state1)
                .withTransition(transition, state2)
                .end()
            .end()
            .withState(state2)
            .end()
        .end()
        .withLinkRelations()
            .withLinkRelation(linkRel, null)
                .implementing(transition)
            .end()
        .end()
        .withMediaTypes(true, JSON_LD)
        .withResource()
            .named(state1)
            .withMethod(httpMethod1)
                .transitioningTo("Start")
                .producing()
            .end()
        .end()
        .withResource()
            .named(state2)
            .locatedAtTemplate(uriTemplate)
            .withMethod(httpMethod2)
                .transitioningTo(transition)
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);
    
    JavaCode controller = getType(sources, controllerName(state1));
    String methodCall = javaMethodName(httpMethod2) + "(response.getParameter(\"" + param + "\"))";
    assertTrue("Missing method call: " + methodCall,
        controller.methodBody(javaMethodName(httpMethod1)).contains(methodCall));
  }
  
}
