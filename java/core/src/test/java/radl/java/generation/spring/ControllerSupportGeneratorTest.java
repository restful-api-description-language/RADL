/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;


public class ControllerSupportGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  @Test
  public void generatesControllerSupportPerResource() {
    String resource = aName();
    String httpMethod1 = "GET";
    String httpMethod2 = "POST";
    String mediaType = aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(mediaType)
        .withResource()
            .named(resource)
            .withMethod(httpMethod1)
                .producing(mediaType)
            .end()
            .withMethod(httpMethod2)
                .consuming(mediaType)
            .end()
        .end()
    .build();
    String method1 = javaMethodName(httpMethod1);
    String method2 = javaMethodName(httpMethod2);

    Iterable<Code> sources = radlToCode(radl);

    getType(sources, TYPE_API);
    getType(sources, TYPE_ACTIONS);
    getType(sources, controllerName(resource));
    assertControllerSupport(resource, sources, method1, method2);
    getType(sources, TYPE_URIS);
  }

  private void assertControllerSupport(String expectedClassName, Iterable<Code> sources, String... methods) {
    JavaCode javaSource = getType(sources, controllerSupportName(expectedClassName));

    assertFileComments(javaSource);
    assertEquals("Class annotations", Arrays.asList("@Service"), javaSource.typeAnnotations());
    assertImports(Arrays.asList("org.springframework.stereotype.Service"), javaSource);
    assertEquals("Package", packagePrefix + '.' + expectedClassName, javaSource.packageName());
    TestUtil.assertCollectionEquals("Methods", Arrays.asList(methods), javaSource.methods());
    for (String method : methods) {
      assertMethod(javaSource, method);
    }
  }

  private void assertMethod(JavaCode javaSource, String method) {
    String arguments = "GET".equalsIgnoreCase(method) ? "" : "Object input";
    String ret = "GET".equalsIgnoreCase(method) ? "new ResourceSupport()" : "HttpStatus.NO_CONTENT";

    assertEquals("Method arguments for " + method, arguments, javaSource.methodArguments(method));
    // Make sure the comment is not viewed as a to-do in this code base
    String methodBody = javaSource.methodBody(method);
    assertTrue("Method body for " + method + ":\n" + methodBody, methodBody.contains(ret));
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

    String controllerMethod2 = javaMethodName(httpMethod2);
    JavaCode controllerSupport2 = getType(sources, controllerSupportName(state2));
    assertTrue("Missing method #2: " + controllerSupport2.methods(),
        controllerSupport2.methods().contains(controllerMethod2));
    assertTrue("Return support #2", controllerSupport2.methodBody(controllerMethod2).contains(
        "new RestResponse<Void>(null)"));
  }

}
