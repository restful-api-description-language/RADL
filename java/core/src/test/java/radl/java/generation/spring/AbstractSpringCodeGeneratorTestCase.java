/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.core.generation.CodeBaseGenerator;
import radl.core.generation.Module;
import radl.java.code.Java;
import radl.java.code.JavaCode;
import radl.test.RandomData;


public abstract class AbstractSpringCodeGeneratorTestCase {

  protected static final RandomData RANDOM = new RandomData();
  protected static final String JSON_LD = "application/ld+json";
  protected static final String TYPE_API = "Api";
  protected static final String TYPE_URIS = "Resources";
  protected static final String TYPE_ERROR_DTO = "ErrorResource";
  protected static final String TYPE_ACTIONS = "Actions";
  protected static final String TRANSITION_ENABLED_METHOD = "response.allows";
  protected static final String DEFAULT_MEDIA_TYPE_CONSTANT = "MEDIA_TYPE_DEFAULT";
  private static final int NAME_LENGTH = RANDOM.integer(3, 7);

  protected final String packagePrefix = 'a' + RANDOM.string(NAME_LENGTH) + '.' + RANDOM.string(NAME_LENGTH);
  private final CodeBaseGenerator generator = new SpringCodeBaseGenerator(packagePrefix);

  protected Iterable<Code> radlToCode(Document radl) {
    Module input = new Module(new RadlCode(radl));
    Module generated = new Module();
    Module skeleton = new Module();
    generator.generate(Arrays.asList(input), Arrays.asList(generated, skeleton));
    Collection<Code> result = new ArrayList<>();
    result.addAll(generated);
    result.addAll(skeleton);
    return result;
  }

  protected String aName() {
    return RANDOM.string(NAME_LENGTH) + 'q';
  }

  protected void assertFileComments(JavaCode javaSource) {
    assertEquals("File comments", Arrays.asList("Generated from RADL."), javaSource.fileComments());
  }

  protected String controllerName(String resourceName) {
    return typeName(resourceName, "Controller");
  }

  protected String typeName(String name, String suffix) {
    return Java.toIdentifier(name) + suffix;
  }

  protected String aLocalUri() {
    return String.format("/%s/", someValue());
  }

  private String someValue() {
    return RANDOM.string(10);
  }

  protected String quote(String value) {
    return '"' + value + '"';
  }

  protected String getFieldWithValue(JavaCode code, String value) {
    for (String field : code.fieldNames()) {
      if (value.equals(code.fieldValue(field))) {
        return field;
      }
    }
    fail("Missing field with value " + value + " in\n" + code);
    return null; // NOTREACHED
  }

  protected String javaMethodName(String httpMethod) {
    return httpMethod.toLowerCase(Locale.getDefault());
  }

  protected JavaCode generateController(Document radl, String resourceName) {
    Iterable<Code> sources = radlToCode(radl);
    return getType(sources, controllerName(resourceName));
  }

  protected String mediaTypeToConstant(String mediaType, boolean local) {
    String result = String.format("MEDIA_TYPE_%s", mediaType.replace('/', '_').toUpperCase(Locale.getDefault()));
    return local ? result : "Api." + result;
  }

  protected void assertImports(Iterable<String> expectedImports, JavaCode actual) {
    Collection<String> actualImports = actual.imports();
    for (String expected : expectedImports) {
      assertTrue("Missing import: " + expected, actualImports.contains(expected));
    }
  }

  protected String aMethod() {
    switch (RANDOM.integer(4)) {
      case 0: return "GET";
      case 1: return "PUT";
      case 2: return "POST";
      case 3: return "DELETE";
      default: throw new IllegalStateException("Should not happen");
    }
  }

  protected String aMediaType() {
    return String.format("%s/%s", aName(), aName());
  }

  protected String controllerSupportName(String resourceName) {
    return typeName(resourceName, "ControllerSupport");
  }

  protected JavaCode generateType(Document radl, String type) {
    return getType(radlToCode(radl), type);
  }

  protected JavaCode getType(Iterable<Code> sources, String type) {
    Collection<String> types = new ArrayList<>();
    for (Code source : sources) {
      JavaCode javaSource = (JavaCode)source;
      if (type.equals(javaSource.typeName())) {
        return javaSource;
      }
      types.add(javaSource.typeName());
    }
    fail("Missing type: " + type + " in\n" + types);
    return null; // NOTREACHED
  }

  protected String aUri() {
    return String.format("http://%s.com:%d%s", someValue(), somePort(), aLocalUri());
  }

  private int somePort() {
    return RANDOM.integer(1025, 65535);
  }

  protected String dtoName(String name) {
    return typeName(name, "Resource");
  }

}
