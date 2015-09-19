/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.core.generation.CodeGenerator;
import radl.java.code.Java;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.RandomData;
import radl.test.TestUtil;


public class SpringCodeGeneratorTest {

  private static final RandomData RANDOM = new RandomData();
  private static final int NAME_LENGTH = RANDOM.integer(3, 7);
  private static final String TYPE_API = "Api";
  private static final String TYPE_URIS = "Uris";
  private static final String TYPE_ERROR_DTO = "ErrorDto";

  private final String packagePrefix = 'a' + RANDOM.string(NAME_LENGTH) + '.' + RANDOM.string(NAME_LENGTH);
  private final CodeGenerator generator = new SpringCodeGenerator(packagePrefix);

  @Test(expected = RuntimeException.class)
  public void throwsExceptionOnInvalidRadl() {
    Document radl = RadlBuilder.aRadlDocument().withResource().named("").build();

    generator.generateFrom(radl).iterator();
  }

  @Test
  public void generatesControllerPerResource() {
    String resource = aName() + '-' + aName();
    Document radl = RadlBuilder.aRadlDocument().withResource().named(resource).build();

    Iterator<Code> sources = generator.generateFrom(radl).iterator();

    assertTrue("Missing source", sources.hasNext());
    assertController(resource, sources.next());
  }

  private String aName() {
    return RANDOM.string(NAME_LENGTH) + 'q';
  }

  private void assertController(String expectedClassName, Code source) {
    assertEquals("Source type", JavaCode.class, source.getClass());
    JavaCode javaSource = (JavaCode)source;

    assertFileComments(javaSource);
    assertEquals("Class name", getControllerClassName(expectedClassName), javaSource.typeName());
    assertEquals("Class annotations", Arrays.asList("@RestController"), javaSource.typeAnnotations());
    TestUtil.assertCollectionEquals("Imports", Arrays.asList(packagePrefix + ".api.Api", packagePrefix + ".impl.Uris",
        "org.springframework.beans.factory.annotation.Autowired",
        "org.springframework.web.bind.annotation.RestController"), javaSource.imports());
    assertEquals("Package", packagePrefix + '.' + expectedClassName.replaceAll("\\-", ""),
        javaSource.packageName());

    String fieldName = "service";
    TestUtil.assertCollectionEquals("Fields", Arrays.asList(fieldName), javaSource.fieldNames());
    assertEquals("Field type", Java.toIdentifier(expectedClassName) + "Service", javaSource.fieldType(fieldName));
    assertEquals("Field annotation", Arrays.asList("@Autowired"), javaSource.fieldAnnotations(fieldName));
  }

  private void assertFileComments(JavaCode javaSource) {
    assertEquals("File comments", Arrays.asList("Generated from RADL."), javaSource.fileComments());
  }

  private String getControllerClassName(String expectedClassName) {
    return Java.toIdentifier(expectedClassName) + "Controller";
  }

  @Test
  public void addsRequestMappingForResourceLocation() {
    String name = aName();
    String uri = aLocalUri();
    Document radl = RadlBuilder.aRadlDocument().withResource().named(name).locatedAt(uri).build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode uris = getJavaCode(sources, TYPE_URIS);
    String constant = getFieldWithValue(uris, quote(uri));
    JavaCode controller = getJavaCode(sources, getControllerClassName(name));

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

  private JavaCode getJavaCode(Iterable<Code> sources, String typeName) {
    for (Code source : sources) {
      if (source instanceof JavaCode) {
        JavaCode code = (JavaCode)source;
        if (typeName.equals(code.typeName())) {
          return code;
        }
      }
    }
    fail("Missing type: " + typeName);
    return null; // NOTREACHED
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
    String httpMethod = aMethod();
    String mediaType1 = aMediaType();
    String mediaType2 = aMediaType();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(mediaType1, mediaType2)
        .withResource()
            .withMethod(httpMethod)
                .consuming(mediaType1)
                .producing(mediaType2)
        .build();
    String method = httpMethod.toLowerCase(Locale.getDefault());

    JavaCode source = generateController(radl);

    assertImports(Arrays.asList("org.springframework.web.bind.annotation.RequestBody",
        "org.springframework.web.bind.annotation.RequestMapping",
        "org.springframework.web.bind.annotation.RequestMethod"), source);
    TestUtil.assertCollectionEquals("Methods", Arrays.asList(method), source.methods());
    String methodAnnotation = String.format(
        "@RequestMapping(method = RequestMethod.%s, consumes = { %s }, produces = { %s })",
        httpMethod, mediaTypeToConstant(mediaType1, false), mediaTypeToConstant(mediaType2, false));
    assertEquals("Method annotations", Collections.singleton(methodAnnotation).toString(),
        source.methodAnnotations(method).toString());
    assertEquals("Method arguments", "@RequestBody String input", source.methodArguments(method));
    assertEquals("Method return type", "Object", source.methodReturns(method));
    assertEquals("Method body", String.format("return service.%s(input);", method), source.methodBody(method));
  }

  private JavaCode generateController(Document radl) {
    return (JavaCode)generator.generateFrom(radl).iterator().next();
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
  public void generatesServicePerResource() {
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
            .and().withMethod(httpMethod2)
                .consuming(mediaType)
        .build();
    String method1 = httpMethod1.toLowerCase(Locale.getDefault());
    String method2 = httpMethod2.toLowerCase(Locale.getDefault());

    Iterator<Code> sources = generator.generateFrom(radl).iterator();

    assertTrue("Missing controller", sources.hasNext());
    sources.next();
    assertTrue("Missing service", sources.hasNext());
    assertService(resource, sources.next(), method1, method2);
    assertType(TYPE_API, sources);
    assertType(TYPE_URIS, sources);
    assertFalse("Extra source", sources.hasNext());
  }

  private void assertService(String expectedClassName, Code source, String... methods) {
    assertEquals("Source type", JavaCode.class, source.getClass());
    JavaCode javaSource = (JavaCode)source;

    assertFileComments(javaSource);
    assertEquals("Class name", Java.toIdentifier(expectedClassName) + "Service", javaSource.typeName());
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
    String ret = "GET".equalsIgnoreCase(method) ? "return null; " : "";

    assertEquals("Method arguments for " + method, arguments, javaSource.methodArguments(method));
    // Make sure the comment is not viewed as a to-do in this code base
    assertEquals("Method body for " + method, ret + "// TO" + "DO: Implement", javaSource.methodBody(method));
  }

  private void assertType(String expectedName, Iterator<Code> actualSources) {
    assertTrue("Missing " + expectedName, actualSources.hasNext());
    assertEquals("Type name", expectedName, ((JavaCode)actualSources.next()).typeName());
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
    for (Code source : sources) {
      JavaCode javaSource = (JavaCode)source;
      if (type.equals(javaSource.typeName())) {
        return javaSource;
      }
    }
    fail("Missing type: " + type);
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

    String field = "LINK_FOO_BAR";
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
        .withResource().named(name).locatedAt(billboardUri).withMethod("GET").transitioningTo("Start")
        .and().withResource().locatedAt(otherUri)
        .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    String field = "URL_BILLBOARD";
    JavaCode api = getJavaCode(sources, TYPE_API);
    assertFileComments(api);
    assertEquals("Fields", Arrays.asList(field).toString(), api.fieldNames().toString());
    assertEquals("Field value #1", quote(billboardUri), api.fieldValue(field));

    JavaCode uris = getJavaCode(sources, TYPE_URIS);
    assertFileComments(uris);
    assertEquals("# Implementation URIs", 1, uris.fieldNames().size());
    String constant = uris.fieldNames().iterator().next();
    assertFalse("URI constant doesn't fit naming pattern: " + constant, constant.startsWith("URL_"));
    assertEquals("URI value", quote(otherUri), uris.fieldValue(constant));

    JavaCode controller = getJavaCode(sources, TestUtil.initCap(name) + "Controller");
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
        .withResource().named(TestUtil.initCap(name1) + " " + name2 + "-")
        .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode controller = getJavaCode(sources, TestUtil.initCap(name1) + TestUtil.initCap(name2) + "Controller");
    assertNotNull("Missing controller", controller);

    String controllerPackage = controller.packageName();
    assertTrue("Package: " + controllerPackage, controllerPackage.endsWith(name1 + name2));
    assertEquals("Package should be all lowercase", controllerPackage.toLowerCase(Locale.getDefault()),
        controllerPackage);
  }

  @Test
  public void generatesClassNamesWithoutSuccessiveCapitals() {
    String name = "PDP";
    Document radl = RadlBuilder.aRadlDocument()
        .withResource().named(name)
        .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode controller = getJavaCode(sources, TestUtil.initCap(name.toLowerCase(Locale.getDefault())) + "Controller");
    assertNotNull("Missing controller", controller);
  }

  @Test
  public void addsRequestMappingForResourceWithUriTemplate() {
    String name = aName();
    String uri = aLocalUri();
    Document radl = RadlBuilder.aRadlDocument().withResource().named(name).locatedAtTemplate(uri).build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode uris = getJavaCode(sources, TYPE_URIS);
    String constant = getFieldWithValue(uris, quote(uri));
    JavaCode controller = getJavaCode(sources, getControllerClassName(name));

    String requestMappingAnnotation = String.format("@RequestMapping(Uris.%s)", constant);
    assertTrue("Missing @RequestMapping: " + controller.typeAnnotations(),
        controller.typeAnnotations().contains(requestMappingAnnotation));
  }

  @Test
  public void doesntAddProducesForInvalidMediaTypeReference() {
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(aMediaType())
        .withResource()
            .named(aName())
            .locatedAt(aLocalUri())
            .withMethod("GET")
                .producing(aMediaType())
            .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    for (Code source : sources) {
      JavaCode code = (JavaCode)source;
      if (code.typeName().endsWith("Controller")) {
        for (String method : code.methods()) {
          for (String annotation : code.methodAnnotations(method)) {
            assertEquals("Annotation", "@RequestMapping(method = RequestMethod.GET)", annotation);
          }
        }
      }
    }
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
            .error(name2, null)
            .error(uri, null)
        .end()
    .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode api = getJavaCode(sources, TYPE_API);
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
            .linkRelation(linkRelation, linkRelationSpecificationUri)
        .end()
    .build();

    JavaCode api = generateType(radl, TYPE_API);

    String field = "LINK_FOO_BAR";
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
            // This should be mapped unto IllegalStateException
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
    TestUtil.assertCollectionEquals("Error handler methods",
        Arrays.asList("error", "illegalArgument", "illegalState", name3),
        errorHandler.methods());
    TestUtil.assertCollectionEquals("Error handler annotations",
        Collections.<String>singleton("@ExceptionHandler({ IllegalArgumentException.class })"),
        errorHandler.methodAnnotations("illegalArgument"));
    TestUtil.assertCollectionEquals("Error handler annotations",
        Collections.<String>singleton("@ExceptionHandler({ " + Java.toIdentifier(name3) + "Exception.class })"),
        errorHandler.methodAnnotations(name3));
  }

  private void assertExceptionType(String name, String documentation, String baseType,
      JavaCode implementedInterface, Iterable<Code> sources) {
    JavaCode exceptionType = getType(sources, Java.toIdentifier(name) + "Exception");
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
    
    getType(sources, Java.toIdentifier(name) + "Exception");
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
        .build();

    Iterable<Code> sources = generator.generateFrom(radl);
    
    JavaCode api = getType(sources, TYPE_API);
    String defaultMediaTypeConstant = "MEDIA_TYPE_DEFAULT";
    String mediaTypeToConstant = mediaTypeToConstant(mediaType, true);
    assertEquals("Default media type", mediaTypeToConstant, api.fieldValue(defaultMediaTypeConstant));
    
    JavaCode controller = getType(sources, Java.toIdentifier(resource) + "Controller");
    TestUtil.assertCollectionEquals("Method annotations", Collections.<String>singleton(
        "@RequestMapping(method = RequestMethod." + method + ", consumes = { Api." + defaultMediaTypeConstant
        + " }, produces = { Api." + defaultMediaTypeConstant + " })"),
        controller.methodAnnotations(method.toLowerCase(Locale.getDefault())));
  }

  @Test
  public void generatesControllersInPackageWithSingularName() {
    String resource1 = aName() + 'l';
    String resource2 = resource1 + 's';
    Document radl = RadlBuilder.aRadlDocument()
        .withResource().named(resource1).and()
        .withResource().named(resource2)
        .build();

    Iterable<Code> sources = generator.generateFrom(radl);

    JavaCode controller1 = getType(sources, Java.toIdentifier(resource1) + "Controller");
    JavaCode controller2 = getType(sources, Java.toIdentifier(resource2) + "Controller");
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
        .withMediaTypes(true, "application/ld+json")
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
    
    JavaCode dto1 = getType(sources, Java.toIdentifier(name1) + "Dto");
    assertEquals("Package", packagePrefix + '.' + name1, dto1.packageName());
    assertTrue("Annotations #1", dto1.typeAnnotations().isEmpty());
    TestUtil.assertCollectionEquals("Fields #1", Arrays.asList(property1, property2, property3), dto1.fieldNames());
    assertEquals("Field #1 type", "String", dto1.fieldType(property1));
    assertEquals("Field #2 type", property2Type, dto1.fieldType(property2));
    assertEquals("Field #3 type", "String[]", dto1.fieldType(property3));

    JavaCode dto2 = getType(sources, Java.toIdentifier(name2) + "Dto");
    assertTrue("Missing import on Expose", dto2.imports().contains("de.escalon.hypermedia.hydra.mapping.Expose"));
    TestUtil.assertCollectionEquals("Annotations #2", Collections.<String>singleton("@Expose(\"" + uri1 + "\")"),
        dto2.typeAnnotations());
    TestUtil.assertCollectionEquals("Annotations property #4", Collections.<String>singleton("@Expose(\"" + uri2 + "\")"),
        dto2.fieldAnnotations(property4));

    JavaCode dto3 = getType(sources, Java.toIdentifier(name3) + "Dto");
    TestUtil.assertCollectionEquals("Fields #3", Arrays.asList(property5, property6), dto3.fieldNames());
    assertTrue("Imports #3 doesn't contain " + dto1.fullyQualifiedName(), dto3.imports().contains(dto1.fullyQualifiedName()));
    assertEquals("Field #5 type", dto1.typeName(), dto3.fieldType(property5));
    String nestedDtoName = Java.toIdentifier(property6) + "Dto";
    assertEquals("Field #6 type", nestedDtoName, dto3.fieldType(property6));
    
    JavaCode nestedDto = getType(sources, nestedDtoName);
    assertTrue("Imports #3 doesn't contain " + nestedDto.fullyQualifiedName(),
        dto3.imports().contains(nestedDto.fullyQualifiedName()));

    JavaCode dto4 = getType(sources, Java.toIdentifier(name4) + "Dto");
    assertTrue("Missing import on Expose #4", dto4.imports().contains("de.escalon.hypermedia.hydra.mapping.Expose"));
  }
  
}
