/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.core.generation.CodeGenerator;
import radl.java.code.Java;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.RandomData;
import radl.test.TestUtil;


public class SpringCodeGeneratorTest { // NOPMD ExcessiveClassLength

  private static final RandomData RANDOM = new RandomData();
  private static final int NAME_LENGTH = RANDOM.integer(3, 7);
  private static final String JSON_LD = "application/ld+json";
  private static final String TYPE_API = "Api";
  private static final String TYPE_URIS = "Resources";
  private static final String TYPE_ERROR_DTO = "ErrorResource";
  private static final String TYPE_ACTIONS = "Actions";
  private static final String TRANSITION_ENABLED_METHOD = "response.allows";

  private final String packagePrefix = 'a' + RANDOM.string(NAME_LENGTH) + '.' + RANDOM.string(NAME_LENGTH);
  private final CodeGenerator generator = new SpringCodeGenerator(packagePrefix);

  @Test
  public void generatesControllerPerResource() {
    String resource = aName() + '-' + aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(resource)
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    assertController(resource, sources);
  }

  private String aName() {
    return RANDOM.string(NAME_LENGTH) + 'q';
  }

  private void assertController(String expectedClassName, Iterable<Code> sources) {
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

  private void assertFileComments(JavaCode javaSource) {
    assertEquals("File comments", Arrays.asList("Generated from RADL."), javaSource.fileComments());
  }

  private String controllerName(String resourceName) {
    return typeName(resourceName, "Controller");
  }

  private String typeName(String name, String suffix) {
    return Java.toIdentifier(name) + suffix;
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

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode uris = getType(sources, TYPE_URIS);
    String constant = getFieldWithValue(uris, quote(uri));
    JavaCode controller = getType(sources, controllerName(name));

    String requestMappingAnnotation = String.format("@RequestMapping(%s.%s)", TYPE_URIS, constant);
    assertTrue("Missing @RequestMapping: " + controller.typeAnnotations(),
        controller.typeAnnotations().contains(requestMappingAnnotation));
  }

  private String aLocalUri() {
    return String.format("/%s/", someValue());
  }

  private String someValue() {
    return RANDOM.string(10);
  }

  private String quote(String value) {
    return '"' + value + '"';
  }

  private String getFieldWithValue(JavaCode code, String value) {
    for (String field : code.fieldNames()) {
      if (value.equals(code.fieldValue(field))) {
        return field;
      }
    }
    fail("Missing field with value " + value + " in\n" + code);
    return null; // NOTREACHED
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

  private String javaMethodName(String httpMethod) {
    return httpMethod.toLowerCase(Locale.getDefault());
  }

  private JavaCode generateController(Document radl, String resourceName) {
    Iterable<Code> sources = generator.generateFrom(radl);
    return getType(sources, controllerName(resourceName));
  }

  private String mediaTypeToConstant(String mediaType, boolean local) {
    String result = String.format("MEDIA_TYPE_%s", mediaType.replace('/', '_').toUpperCase(Locale.getDefault()));
    return local ? result : "Api." + result;
  }

  private void assertImports(Iterable<String> expectedImports, JavaCode actual) {
    Collection<String> actualImports = actual.imports();
    for (String expected : expectedImports) {
      assertTrue("Missing import: " + expected, actualImports.contains(expected));
    }
  }

  private String aMethod() {
    switch (RANDOM.integer(4)) {
      case 0: return "GET";
      case 1: return "PUT";
      case 2: return "POST";
      case 3: return "DELETE";
      default: throw new IllegalStateException("Should not happen");
    }
  }

  private String aMediaType() {
    return String.format("%s/%s", aName(), aName());
  }

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

    Iterable<Code> sources = generator.generateFrom(radl);

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

  private String controllerSupportName(String resourceName) {
    return typeName(resourceName, "ControllerSupport");
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
  public void generatesApiWithMediaTypes() {
    String httpMethod = aMethod();
    String mediaType1 = 'a' + aName();
    String mediaType2 = 'z' + aMediaType();
    String ignorablePrefix = "application/";
    String fullMediaType1 = ignorablePrefix + mediaType1 + "+xml";
    String fullMediaType2 = mediaType2 + "; version=3.0";
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(fullMediaType1, fullMediaType2)
        .withResource()
            .withMethod(httpMethod)
                .consuming(fullMediaType1)
                .producing(fullMediaType2)
            .end()
        .end()
    .build();

    JavaCode api = generateType(radl, TYPE_API);

    String field1 = mediaTypeToConstant(mediaType1, true) + "_XML";
    String field2 = mediaTypeToConstant(mediaType2, true) + "_VERSION_3_0";
    assertEquals("Fields", Arrays.asList(field1, field2).toString(), api.fieldNames().toString());
    assertEquals("Field value #1", quote(fullMediaType1), api.fieldValue(field1));
    assertEquals("Field value #2", quote(fullMediaType2), api.fieldValue(field2));
  }

  private JavaCode generateType(Document radl, String type) {
    return getType(generator.generateFrom(radl), type);
  }

  private JavaCode getType(Iterable<Code> sources, String type) {
    Collection<String> types = new ArrayList<String>();
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

  @Test
  public void generatesApiWithLinkRelations() {
    String linkRelationName = "foo-bar";
    String linkRelation = aUri() + linkRelationName;
    Document radl = RadlBuilder.aRadlDocument()
        .withLinkRelations(linkRelation)
        .build();

    JavaCode api = generateType(radl, TYPE_API);
    assertFileComments(api);

    String field = "LINK_REL_FOO_BAR";
    TestUtil.assertCollectionEquals("Fields", Arrays.asList(field), api.fieldNames());
    assertEquals("Field value #1", quote(linkRelation), api.fieldValue(field));
  }

  private String aUri() {
    return String.format("http://%s.com:%d%s", someValue(), somePort(), aLocalUri());
  }

  private int somePort() {
    return RANDOM.integer(1025, 65535);
  }

  @Test
  public void generatesConstantsForUris() {
    String name = aName();
    String billboardUri = aLocalUri();
    String otherUri = aLocalUri();
    Document radl = RadlBuilder.aRadlDocument()
        .startingAt(name)
        .withResource()
            .named(name)
            .locatedAt(billboardUri)
            .withMethod("GET")
                .transitioningTo("Start")
            .end()
        .end()
        .withResource()
            .locatedAt(otherUri)
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    String field = "URL_BILLBOARD";
    JavaCode api = getType(sources, TYPE_API);
    assertFileComments(api);
    assertEquals("Fields", Arrays.asList(field).toString(), api.fieldNames().toString());
    assertEquals("Field value #1", quote(billboardUri), api.fieldValue(field));

    JavaCode uris = getType(sources, TYPE_URIS);
    assertFileComments(uris);
    assertEquals("# Implementation URIs", 1, uris.fieldNames().size());
    String constant = uris.fieldNames().iterator().next();
    assertTrue("URI constant doesn't fit naming pattern: " + constant, constant.startsWith("URL_"));
    assertEquals("URI value", quote(otherUri), uris.fieldValue(constant));

    JavaCode controller = getType(sources, TestUtil.initCap(name) + "Controller");
    assertFileComments(controller);
    assertNotNull("Missing controller", controller);
    for (String type : controller.imports()) {
      assertFalse("Should not import " + type, type.endsWith("." + TYPE_URIS));
    }
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

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode controller = getType(sources, TestUtil.initCap(name1) + TestUtil.initCap(name2) + "Controller");
    assertNotNull("Missing controller", controller);

    String controllerPackage = controller.packageName();
    assertTrue("Package: " + controllerPackage, controllerPackage.endsWith(name1 + name2));
    assertEquals("Package should be all lowercase", javaMethodName(controllerPackage),
        controllerPackage);
  }

  @Test
  public void generatesClassNamesWithoutSuccessiveCapitals() {
    String name = "PDP";
    Document radl = RadlBuilder.aRadlDocument()
        .withResource()
            .named(name)
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode controller = getType(sources, TestUtil.initCap(javaMethodName(name)) + "Controller");
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

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode uris = getType(sources, TYPE_URIS);
    String constant = getFieldWithValue(uris, quote(uri));
    JavaCode controller = getType(sources, controllerName(name));

    String requestMappingAnnotation = String.format("@RequestMapping(%s.%s)", TYPE_URIS, constant);
    assertTrue("Missing @RequestMapping: " + controller.typeAnnotations(),
        controller.typeAnnotations().contains(requestMappingAnnotation));
  }

  // #39 - Add error conditions to generated API
  @Test
  public void addsErrorConditionsToApi() {
    String name1 = 'a' + RANDOM.string(7);
    String name2 = 'm' + RANDOM.string(3) + ':' + RANDOM.string(7);
    String name3 = 'z' + RANDOM.string(7);
    String docPart1 = RANDOM.string(12);
    String docPart2 = RANDOM.string(12);
    String uri = aUri() + name3;
    Document radl = RadlBuilder.aRadlDocument()
        .withErrors()
            .error(name1, docPart1 + "\n\t " + docPart2)
            .error(name2)
            .error(uri)
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode api = getType(sources, TYPE_API);
    assertNotNull("Missing API", api);

    String error1 = "ERROR_" + name1.toUpperCase(Locale.getDefault());
    assertTrue("Missing field " + error1, api.fieldNames().contains(error1));
    assertEquals("JavaDoc for error #1", Arrays.asList(docPart1 + ' ' + docPart2), api.fieldComments(error1));

    String error3 = "ERROR_" + name3.toUpperCase(Locale.getDefault());
    assertTrue("Missing field " + error3, api.fieldNames().contains(error3));
    assertEquals("Error #3", '"' + uri + '"', api.fieldValue(error3));
  }

  // #40 Generate JavaDoc for <specification> in link relations
  @Test
  public void generateJavaDocForLinkRelationSpecification() {
    String linkRelationName = "foo-bar";
    String linkRelation = aUri() + linkRelationName;
    String linkRelationSpecificationUri = aUri();
    Document radl = RadlBuilder.aRadlDocument()
        .withLinkRelations()
            .withLinkRelation(linkRelation, linkRelationSpecificationUri)
        .end()
    .build();

    JavaCode api = generateType(radl, TYPE_API);

    String field = "LINK_REL_FOO_BAR";
    TestUtil.assertCollectionEquals("Fields", Arrays.asList(field), api.fieldNames());
    assertEquals("Field comment", Arrays.asList("See " + linkRelationSpecificationUri), api.fieldComments(field));
  }

  // #39 Add error conditions to generated API
  @Test
  public void generatesErrorDtoWhenErrors() {
    Document radl = RadlBuilder.aRadlDocument()
        .withErrors()
            .error(RANDOM.string(12), RANDOM.string(42))
        .end()
    .build();

    JavaCode errorDto = generateType(radl, TYPE_ERROR_DTO);

    TestUtil.assertCollectionEquals("Fields", Arrays.asList("title", "type"), errorDto.fieldNames());
  }

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

    Iterable<Code> sources = generator.generateFrom(radl);
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

    Iterable<Code> sources = generator.generateFrom(radl);

    getType(sources, exceptionName(name));
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

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode api = getType(sources, TYPE_API);
    String defaultMediaTypeConstant = "MEDIA_TYPE_DEFAULT";
    String mediaTypeToConstant = mediaTypeToConstant(mediaType, true);
    assertEquals("Default media type", mediaTypeToConstant, api.fieldValue(defaultMediaTypeConstant));

    JavaCode controller = getType(sources, controllerName(resource));
    TestUtil.assertCollectionEquals("Method annotations", Collections.<String>singleton(
        "@RequestMapping(method = RequestMethod." + method + ", consumes = { Api." + defaultMediaTypeConstant
        + " }, produces = { Api." + defaultMediaTypeConstant + " })"),
        controller.methodAnnotations(javaMethodName(method)));
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

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode controller1 = getType(sources, controllerName(resource1));
    JavaCode controller2 = getType(sources, controllerName(resource2));
    assertEquals("Package", controller1.packageName(), controller2.packageName());
  }

  // #9 Generate data transfer objects for property groups
  @Test
  public void generatesDtosForPropertySources() {
    String name1 = 'a' + aName();
    String property1 = 'a' + aName();
    String property2 = 'b' + aName();
    String property2Type = "int";
    String property3 = 'c' + aName();
    String name2 = 'b' + aName();
    String uri1 = aUri();
    String property4 = aName();
    String uri2 = aUri();
    String name3 = 'c' + aName();
    String property5 = 'a' + aName();
    String property6 = 'b' + aName();
    String name4 = 'd' + aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(true, JSON_LD)
        .withPropertyGroup()
            .named(name1)
            .withProperty(property1)
            .end()
            .withProperty(property2)
                .as(property2Type)
            .end()
            .withProperty(property3)
                .repeating()
            .end()
        .end()
        .withPropertyGroup()
            .named(name2)
            .meaning(uri1)
            .withProperty(property4)
                .meaning(uri2)
            .end()
        .end()
        .withPropertyGroup()
            .named(name3)
            .withPropertyGroup()
                .named(property5)
                .referencing(name1)
            .endNested()
            .withPropertyGroup()
                .named(property6)
                .withProperty(property1)
                .end()
            .endNested()
        .end()
        .withPropertyGroup()
            .named(name4)
            .meaning(uri1)
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode dto1 = getType(sources, dtoName(name1));
    assertEquals("Package", packagePrefix + '.' + name1, dto1.packageName());
    assertTrue("Annotations #1", dto1.typeAnnotations().isEmpty());
    TestUtil.assertCollectionEquals("Fields #1", Arrays.asList(property1, property2, property3 + 's'), dto1.fieldNames());
    assertEquals("Field #1 type", "String", dto1.fieldType(property1));
    assertEquals("Field #2 type", property2Type, dto1.fieldType(property2));
    assertEquals("Field #3 type", "String[]", dto1.fieldType(property3 + 's'));

    JavaCode dto2 = getType(sources, dtoName(name2));
    assertTrue("Missing import on Expose", dto2.imports().contains("de.escalon.hypermedia.hydra.mapping.Expose"));
    TestUtil.assertCollectionEquals("Annotations #2", Collections.<String>singleton("@Expose(\"" + uri1 + "\")"),
        dto2.typeAnnotations());
    TestUtil.assertCollectionEquals("Annotations property #4", Collections.<String>singleton("@Expose(\"" + uri2 + "\")"),
        dto2.fieldAnnotations(property4));

    JavaCode dto3 = getType(sources, dtoName(name3));
    TestUtil.assertCollectionEquals("Fields #3", Arrays.asList(property5, property6), dto3.fieldNames());
    assertTrue("Imports #3 doesn't contain " + dto1.fullyQualifiedName(), dto3.imports().contains(dto1.fullyQualifiedName()));
    assertEquals("Field #5 type", dto1.typeName(), dto3.fieldType(property5));
    String nestedDtoName = dtoName(property6);
    assertEquals("Field #6 type", nestedDtoName, dto3.fieldType(property6));

    JavaCode nestedDto = getType(sources, nestedDtoName);
    assertTrue("Imports #3 doesn't contain " + nestedDto.fullyQualifiedName(),
        dto3.imports().contains(nestedDto.fullyQualifiedName()));

    JavaCode dto4 = getType(sources, dtoName(name4));
    assertTrue("Missing import on Expose #4", dto4.imports().contains("de.escalon.hypermedia.hydra.mapping.Expose"));
  }

  private String dtoName(String name) {
    return typeName(name, "Resource");
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

    Iterable<Code> sources = generator.generateFrom(radl);

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

    JavaCode controllerSupport2 = getType(sources, controllerSupportName(state2));
    assertTrue("Missing method #2: " + controllerSupport2.methods(),
        controllerSupport2.methods().contains(controllerMethod2));
    assertTrue("Return support #2", controllerSupport2.methodBody(controllerMethod2).contains(
        "new RestResponse<Void>(null)"));
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

    Iterable<Code> sources = generator.generateFrom(radl);

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
  public void generatesDtoWithDateTimeField() {
    String data = aName();
    String property = aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withPropertyGroup()
            .named(data)
            .withProperty(property)
                .as("xsd:dateTime")
            .end()
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode dto = getType(sources, dtoName(data));
    assertEquals("Field type", XMLGregorianCalendar.class.getSimpleName(), dto.fieldType(property));
    assertTrue("Imports", dto.imports().contains(XMLGregorianCalendar.class.getName()));
  }

  @Test
  public void generatesDtoWithNumberField() {
    String data = aName();
    String property = aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withPropertyGroup()
            .named(data)
            .withProperty(property)
                .as("number")
            .end()
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode dto = getType(sources, dtoName(data));
    assertEquals("Field type", "double", dto.fieldType(property));
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

    Iterable<Code> sources = generator.generateFrom(radl);
    
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

    Iterable<Code> sources = generator.generateFrom(radl);
    
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

    Iterable<Code> sources = generator.generateFrom(radl);
    
    JavaCode controller = getType(sources, controllerName(state1));
    String methodCall = javaMethodName(httpMethod2) + "(response.getParameter(\"" + param + "\"))";
    assertTrue("Missing method call: " + methodCall,
        controller.methodBody(javaMethodName(httpMethod1)).contains(methodCall));
  }
  
}
