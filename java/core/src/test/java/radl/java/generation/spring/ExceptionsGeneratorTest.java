/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;


public class ExceptionsGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  // #39 Add error conditions to generated API
  @Test
  public void generatesExceptionsForErrors() {
    String name2 = 'i' + RANDOM.string(12);
    String documentation2 = RANDOM.string(42);
    String name3 = 'n' + RANDOM.string(12);
    String documentation3 = RANDOM.string(42);
    String name4 = 's' + RANDOM.string(12);
    String documentation4 = RANDOM.string(42);
    String name5 = 'z' + RANDOM.string(12);
    String documentation5 = RANDOM.string(42);
    Document radl = RadlBuilder.aRadlDocument()
        .withErrors()
            .error(name2, documentation2, 400)
            .error(name3, documentation3, 503)
            .error(name4, documentation4)
            .error(name5, '\n' + documentation5 + '\n')
            // These should be ignored
            .error('t' + RANDOM.string(5), "", 405)
            .error('u' + RANDOM.string(5), "", 406)
            // This should not be mapped
            .error('v' + RANDOM.string(5), "", 500)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);
    JavaCode identifiable = getType(sources, "Identifiable");

    assertExceptionType(name2, documentation2, "IllegalArgumentException", identifiable, sources);
    assertExceptionType(name3, documentation3, "RuntimeException", identifiable, sources);
    assertExceptionType(name4, documentation4, "IllegalArgumentException", identifiable, sources);
    assertExceptionType(name5, documentation5, "IllegalArgumentException", identifiable, sources);

    JavaCode errorHandler = getType(sources, "CentralErrorHandler");
    TestUtil.assertCollectionEquals("Error handler annotations", Collections.singleton("@ControllerAdvice"),
        errorHandler.typeAnnotations());
    TestUtil.assertCollectionEquals("Error handler imports", Arrays.asList(
        "org.springframework.http.HttpStatus",
        "org.springframework.http.ResponseEntity",
        "org.springframework.web.bind.annotation.ControllerAdvice",
        "org.springframework.web.bind.annotation.ExceptionHandler"), errorHandler.imports());
    Collection<String> methods = errorHandler.methods();
    for (String method : Arrays.asList("error", "illegalArgument", "internalError", name3)) {
      assertTrue("Missing method: " + method, methods.contains(method));
    }
    TestUtil.assertCollectionEquals("Error handler annotations",
        Collections.<String>singleton("@ExceptionHandler({ IllegalArgumentException.class })"),
        errorHandler.methodAnnotations("illegalArgument"));
    TestUtil.assertCollectionEquals("Error handler annotations",
        Collections.<String>singleton("@ExceptionHandler({ " + exceptionName(name3) + ".class })"),
        errorHandler.methodAnnotations(name3));
  }

  private String exceptionName(String name) {
    return typeName(name, "Exception");
  }

  private void assertExceptionType(String name, String documentation, String baseType,
      JavaCode implementedInterface, Iterable<Code> sources) {
    JavaCode exceptionType = getType(sources, exceptionName(name));
    assertEquals("Base type", baseType, exceptionType.superTypeName());
    TestUtil.assertCollectionEquals("Implements", Collections.singleton(implementedInterface.typeName()),
        exceptionType.implementedInterfaces());
    assertEquals("ID", "return Api.ERROR_" + name.toUpperCase(Locale.getDefault()) + ";",
        exceptionType.methodBody("getId"));
    assertEquals("Constructor", "super(\"" + documentation + "\");", exceptionType.constructorBody());
    assertEquals("Imports", Collections.<String>singleton(packagePrefix + ".api.Api"), exceptionType.imports());
  }

  // #39 Add error conditions to generated API
  @Test
  public void stripsCommonPrefixFromExceptionClasses() {
    String prefix = aUri();
    String name = 'a' + RANDOM.string(8);
    Document radl = RadlBuilder.aRadlDocument()
        .withErrors()
            .error(prefix + name, "")
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    getType(sources, exceptionName(name));
  }

}
