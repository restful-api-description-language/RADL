/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static radl.java.extraction.test.ProjectBuilder.project;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.junit.Before;
import org.junit.Test;

import radl.core.extraction.ResourceModel;
import radl.core.extraction.ResourceModelHolder;
import radl.java.extraction.test.Project;
import radl.java.extraction.test.ProjectBuilder;
import radl.test.TestUtil;


public class JaxrsProcessorTest extends AbstractRestAnnotationProcessorTest {

  private static final String JAXRS_PACKAGE = "javax.ws.rs.";

  private final AbstractRestAnnotationProcessor processor = new JaxrsProcessor();
  private final ResourceModel resourceModel = mock(ResourceModel.class);

  @Before
  public void init() {
    ResourceModelHolder.setInstance(resourceModel);
  }

  @Test
  public void supportsJava6() {
    assertEquals("Java version", SourceVersion.latestSupported(),  processor.getSupportedSourceVersion());
  }

  @Test
  public void supportsJaxrsAnnotations() {
    Set<String> annotations = processor.getSupportedAnnotationTypes();
    assertFalse("No annotations supported", annotations.isEmpty());

    for (String annotation : annotations) {
      assertTrue("Incorrect annotation: " + annotation, annotation.startsWith(JAXRS_PACKAGE));
    }
    assertTrue("Doesn't support PathParam", annotations.contains(JAXRS_PACKAGE + "PathParam"));
  }

  @Test
  public void addsResourceForClassWithPathAnnotation() {
    String className = TestUtil.initCap(aName());

    processAnnotationsIn(project()
        .withClass(className)
            .annotatedWith()
                .path(aUri())
            .end()
        .end()
    );

    verify(resourceModel).addResource(className, null);
  }

  private String aUri() {
    return '/' + aName();
  }

  private boolean processAnnotationsIn(ProjectBuilder builder) { // NOPMD  UnusedPrivateMethod
    return builder.build().apply(processor);
  }

  @Test
  public void addsImplicitResourceForMethodWithPathAnnotation() throws Exception {
    String className = TestUtil.initCap(aName());
    String methodName = aName();
    String httpMethod = "GET";
    String implicitResourceName = className + '.' + methodName;

    processAnnotationsIn(project()
        .withClass(className)
            .withMethod(methodName)
                .annotatedWith()
                    .path(aUri())
                    .method(httpMethod)
                .end()
            .end()
        .end()
    );

    verify(resourceModel).addResource(className, null);
    verify(resourceModel).addResource(implicitResourceName, null);
    verify(resourceModel).addMethod(implicitResourceName, httpMethod, null, null, null);
  }

  @Test
  public void addsMethodForMethodWithMethodAnnotation() throws Exception {
    String className = TestUtil.initCap(aName());
    String methodName = aName();
    String httpMethod = "GET";

    processAnnotationsIn(project()
        .withClass(className)
            .annotatedWith()
                .path(aUri())
            .end()
            .withMethod(methodName)
                .annotatedWith()
                    .method(httpMethod)
                .end()
            .end()
        .end()
    );

    verify(resourceModel, atLeastOnce()).addResource(className, null);
    verify(resourceModel).addMethod(className, httpMethod, null, null, null);
  }

  @Test
  public void extractsConsumes() {
    String className = aName();
    String mediaType = aMediaType();

    Project context = project()
        .withClass(className)
            .annotatedWith()
                .consuming(mediaType)
            .end()
        .end()
    .build();
    Element element = context.getRootElement(className);
    TypeElement annotation = context.getAnnotation(element);

    assertEquals("Consumes", Arrays.asList(mediaType), processor.getConsumes(element, annotation));
  }

  private String aMediaType() {
    return aName() + '/' + aName();
  }

  @Test
  public void extractsProduces() {
    String className = aName();
    String mediaType = aMediaType();

    Project context = project()
        .withClass(className)
            .annotatedWith()
                .producing(mediaType)
            .end()
        .end()
    .build();
    Element element = context.getRootElement(className);
    TypeElement annotation = context.getAnnotation(element);

    assertEquals("Produces", Arrays.asList(mediaType), processor.getProduces(element, annotation));
  }

  @Test
  public void addsConsumingMethod() throws Exception {
    String className = TestUtil.initCap(aName());
    String methodName = aName();
    String mediaType = aMediaType();
    String httpMethod = "POST";

    processAnnotationsIn(project()
        .withClass(className)
            .annotatedWith()
                .path(aUri())
            .end()
            .withMethod(methodName)
                .annotatedWith()
                    .method(httpMethod)
                    .consuming(mediaType)
                .end()
            .end()
        .end()
    );

    verify(resourceModel).addMethod(className, httpMethod, mediaType, null, null);
  }

  @Test
  public void addsProducingMethod() throws Exception {
    String className = TestUtil.initCap(aName());
    String methodName = aName();
    String mediaType = aMediaType();
    String httpMethod = "GET";

    processAnnotationsIn(project()
        .withClass(className)
            .annotatedWith()
                .path(aUri())
            .end()
            .withMethod(methodName)
                .annotatedWith()
                    .method(httpMethod)
                    .producing(mediaType)
                .end()
            .end()
        .end()
    );

    verify(resourceModel).addMethod(className, httpMethod, null, mediaType, null);
  }

  @Test
  public void addsPathToApplicationPath() {
    String applicationPathUri = 'a' + aName();
    String pathUri = 'p' + aName();
    String className1 = "C1" + aName();
    String className2 = "C2" + aName();
    String className3 = "C3" + aName();
    String className4 = "C4" + aName();
    String className5 = "C5" + aName();
    Collection<String> uris = Arrays.asList('/' + applicationPathUri + '/' + pathUri);
    Collection<String> pathUris = Arrays.asList('/' + pathUri);

    processAnnotationsIn(project()
        .withClass(className1)
            .annotatedWith()
                .path(pathUri)
                .applicationPath(applicationPathUri)
            .end()
        .end()
        .withClass(className2)
            .annotatedWith()
                .applicationPath(applicationPathUri)
                .path(pathUri)
            .end()
        .end()
        .withClass(className3)
            .annotatedWith()
                .applicationPath(applicationPathUri)
                .path('/' + pathUri)
            .end()
        .end()
        .withClass(className4)
            .annotatedWith()
                .applicationPath("/")
                .path(pathUri)
            .end()
        .end()
        .withClass(className5)
            .annotatedWith()
                .applicationPath("")
                .path('/' + pathUri)
            .end()
        .end()
    );

    verify(resourceModel).addLocations(className1, uris);
    verify(resourceModel).addLocations(className2, uris);
    verify(resourceModel).addLocations(className3, uris);
    verify(resourceModel).addLocations(className4, pathUris);
    verify(resourceModel).addLocations(className5, pathUris);
  }

  @Test
  public void recordsAClassReturnedByAPathMethodAsChildResource() {
    String parentClassName = 'P' + aName();
    String childClassName = 'C' + aName();
    String methodName = aName();
    String httpMethod = "GET";

    processAnnotationsIn(project()
        .withClass(parentClassName)
            .withMethod(methodName)
                .annotatedWith()
                    .path(aUri())
                    .method(httpMethod)
                .end()
                .returning(childClassName)
            .end()
        .end()
        .withClass(childClassName)
        .end()
    );

    verify(resourceModel).addResource(parentClassName, null);
    verify(resourceModel).addResource(childClassName, null);
    verify(resourceModel).addParentResource(childClassName, parentClassName);
  }

  @Test
  public void ignoresAbstractClasses() throws Exception {
    String className = TestUtil.initCap(aName());

    processAnnotationsIn(project()
        .withAbstractClass(className)
            .annotatedWith()
                .path(aUri())
            .end()
        .end()
    );

    verify(resourceModel, never()).addResource(anyString(), anyString());
  }

  @Test
  public void recordsAnImplementationOfATypeReturnedByAPathMethodAsChildResource() {
    String className1 = 'P' + aName();
    String interfaceName = 'I' + aName();
    String className2 = 'C' + aName();
    String methodName = aName();
    String httpMethod = "GET";

    processAnnotationsIn(project()
        .withInterface(interfaceName)
        .end()
        .withClass(className1)
            .withMethod(methodName)
                .annotatedWith()
                    .path(aUri())
                    .method(httpMethod)
                .end()
                .returning(interfaceName)
            .end()
        .end()
        .withClass(className2)
            .derivedFrom(interfaceName)
        .end()
    );

    verify(resourceModel).addResource(className1, null);
    verify(resourceModel).addResource(className2, null);
    verify(resourceModel).addParentResource(className2, className1);
  }

  @Test
  public void connectsMethodWithLocationsWhenIntermediateClassHasNoLocation() {
    String className1 = "C1" + aName();
    String className2 = "C2" + aName();
    String methodName1 = "m1" + aName();
    String methodName2 = "m2" + aName();
    String pathClass1 = aUri();
    String pathMethod1 = "p1" + aName();
    String pathMethod2 = "p2" + aName();

    processAnnotationsIn(project()
        .withClass(className1)
            .annotatedWith()
                .path(pathClass1)
            .end()
            .withMethod(methodName1)
                .annotatedWith()
                    .path(pathMethod1)
                .end()
                .returning(className2)
            .end()
        .end()
        .withClass(className2)
            .withMethod(methodName2)
                .annotatedWith()
                    .path(pathMethod2)
                .end()
            .end()
        .end()
    );

    String grandChildResource = className2 + '.' + methodName2;

    verify(resourceModel, atLeastOnce()).addResource(className1, null);
    verify(resourceModel, atLeastOnce()).addResource(className2, null);
    verify(resourceModel, atLeastOnce()).addResource(grandChildResource, null);

    verify(resourceModel).addParentResource(className2, className1);
    verify(resourceModel).addParentResource(grandChildResource, className2);

    verify(resourceModel).addLocations(className1, Arrays.asList(pathClass1));
    verify(resourceModel).addLocations(className2, Arrays.asList(pathMethod1));
    verify(resourceModel).addLocations(grandChildResource, Arrays.asList(pathMethod2));
  }

  @Test
  public void addsMethodsInSuperTypeToSubTypes() {
    String baseClassName = 'B' + aName();
    String methodName = aName();
    String httpMethod = "DELETE";
    String derivedClassName = 'D' + aName();

    processAnnotationsIn(project()
        .withAbstractClass(baseClassName)
            .withMethod(methodName)
                .annotatedWith()
                    .method(httpMethod)
                .end()
            .end()
        .end()
        .withClass(derivedClassName)
            .derivedFrom(baseClassName)
        .end()
    );

    verify(resourceModel).addMethod(derivedClassName, httpMethod, null, null, null);
  }

  @Test
  public void supportsCustomPatchAnnotation() throws Exception {
    String className = aName();
    String methodName = aName();
    String customPatchClassName = "PATCH";
    String customPatchFullyQualifiedName = aName() + '.' + customPatchClassName;

    processAnnotationsIn(project()
        .withAnnotationProcessorOption("jaxrs.patch", customPatchFullyQualifiedName)
        .withClass(className)
            .withMethod(methodName)
                .annotatedWith()
                    .annotation(customPatchFullyQualifiedName)
                .end()
            .end()
        .end()
    );

    verify(resourceModel).addMethod(className, customPatchClassName, null, null, null);
  }

  @Test
  public void addsClassJavaDocAsResourceDocumentation() throws Exception {
    String className = TestUtil.initCap(aName());
    String documentation = aName() + ' ' + aName();

    processAnnotationsIn(project()
        .withClass(className)
            .documentedWith(documentation)
            .annotatedWith()
                .path(aUri())
            .end()
        .end()
    );

    verify(resourceModel).addResource(className, documentation);
  }

  @Test
  public void addsMethodJavaDocAsMethodDocumentation() throws Exception {
    String className = TestUtil.initCap(aName());
    String methodName = aName();
    String httpMethod = "GET";
    String documentation = aName() + ' ' + aName();

    processAnnotationsIn(project()
        .withClass(className)
            .annotatedWith()
                .path(aUri())
            .end()
            .withMethod(methodName)
                .documentedWith(documentation)
                .annotatedWith()
                    .method(httpMethod)
                .end()
            .end()
        .end()
    );

    verify(resourceModel, atLeastOnce()).addResource(className, null);
    verify(resourceModel).addMethod(className, httpMethod, null, null, documentation);
  }

  @Test
  public void removesParametersAndReturnsFromJavaDoc() throws Exception {
    String className = TestUtil.initCap(aName());
    String documentation1 = aName() + ' ' + aName();
    String documentation2 = aName() + ' ' + aName();

    processAnnotationsIn(project()
        .withClass(className)
            .documentedWith(documentation1 + "\n\t " + documentation2
                + "\n @param foo The Foo that is bar\n @returns The one and only\n baz")
            .annotatedWith()
                .path(aUri())
            .end()
        .end()
    );

    verify(resourceModel).addResource(className, documentation1 + ' ' + documentation2);
  }

  @Test
  public void ignoresEmptyJavaDoc() throws Exception {
    String className = TestUtil.initCap(aName());

    processAnnotationsIn(project()
        .withClass(className)
            .documentedWith("@returns Foo")
            .annotatedWith()
                .path(aUri())
            .end()
        .end()
    );

    verify(resourceModel).addResource(className, null);
  }

  // Issue 22
  @Test
  public void addsLocationVarForMethodParameterWithPathAnnotation() throws Exception {
    String className = TestUtil.initCap(aName());
    String varName = aName();
    String documentation = aName();

    processAnnotationsIn(project()
        .withClass(className)
            .withMethod(aName())
                .annotatedWith()
                    .path(aUri())
                    .method("GET")
                .end()
                .withParameter(aName())
                    .annotatedWith()
                        .pathParam(varName)
                    .end()
                    .documentedWith(documentation)
                .end()
            .end()
        .end()
    );

    verify(resourceModel).addLocationVar(className, varName, documentation);
  }

}
