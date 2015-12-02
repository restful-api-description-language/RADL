/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.core.code.radl.RadlCode;
import radl.core.extraction.ExtractOptions;
import radl.core.extraction.ResourceModelHolder;
import radl.core.extraction.ResourceModelImpl;
import radl.java.code.Java;
import radl.java.code.JavaCode;


public class SpringProcessorTest extends AbstractRestAnnotationProcessorTest {

  private static final String SPRING_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation.";
  private static final String REQUEST_MAPPING_ANNOTATION = "RequestMapping";
  private static final String REQUEST_PARAM_ANNOTATION = "RequestParam";
  private static final String REQUEST_METHOD = "RequestMethod";

  @Rule
  public final TemporaryFolder folder = new TemporaryFolder();

  private final SpringProcessor processor = new SpringProcessor();
  private File baseDir;

  @Before
  public void init() throws IOException {
    ProcessingEnvironment processingEnv = mock(ProcessingEnvironment.class);
    Elements elementUtils = mock(Elements.class);
    when(processingEnv.getElementUtils()).thenReturn(elementUtils);
    processor.init(processingEnv);

    baseDir = folder.newFolder();
  }

  @Test
  public void extendsAbstractRestAnnotationProcessor() {
    assertTrue("Does not extend " + AbstractRestAnnotationProcessor.class.getName(),
        AbstractRestAnnotationProcessor.class.isAssignableFrom(processor.getClass()));
  }

  @Test
  public void returnsSpringAnnotations() throws Exception {
    Set<String> annotations = processor.getSupportedAnnotationTypes();
    assertNotNull("Missing annotations", annotations);
    assertFalse("No annotations", annotations.isEmpty());

    for (String annotation : annotations) {
      assertTrue("Invalid annotation: " + annotation, annotation.startsWith(SPRING_ANNOTATION_PACKAGE));
    }
    assertTrue("Doesn't support RequestParam", annotations.contains(SPRING_ANNOTATION_PACKAGE + "RequestParam"));
  }

  @Test
  public void extractsUri() throws Exception {
    String location = '/' + aName() + '/';
    String resource = aResource();
    JavaCode code = startControllerClass(resource, location);
    addControllerMethod(aMethod(), code);
    endClass(code);

    RadlCode radl = extractRadlFrom(code);

    assertEquals("Resources", Collections.singletonList(resource), radl.resourceNames());
    assertEquals("Resource location", location, radl.resourceLocation(resource));
  }

  private String aResource() {
    return StringUtils.capitalize(aName());
  }

  private JavaCode startControllerClass(String resource, String location) {
    JavaCode result = new JavaCode();
    result.add("import %s%s;", SPRING_ANNOTATION_PACKAGE, REQUEST_MAPPING_ANNOTATION);
    result.add("import %s%s;", SPRING_ANNOTATION_PACKAGE, REQUEST_METHOD);
    result.add("");
    result.add("@%s(\"%s\")", REQUEST_MAPPING_ANNOTATION, location);
    result.add("public class %sController {", Java.toIdentifier(resource));
    return result;
  }

  private String aMethod() {
    return "GET";
  }

  private void addControllerMethod(String method, JavaCode code) {
    code.add("  @%s(method=%s.%s)", REQUEST_MAPPING_ANNOTATION, REQUEST_METHOD, method);
    code.add("  public void %s() {", method);
    code.add("  }");
    code.add("");
  }

  private void endClass(Code code) {
    code.add("}");
  }

  private RadlCode extractRadlFrom(JavaCode code) throws IOException {
    ResourceModelHolder.INSTANCE.set(new ResourceModelImpl());
    code.writeTo(new File(baseDir, code.typeName() + ".java"));
    ExtractOptions options = new FromJavaExtractOptions(Collections.<File>emptyList(), Collections.<File>emptyList(),
        "", "1.6", null, false);
    Document radl = new FromJavaRadlExtractor().extractFrom(aName(), baseDir, options);
    return new RadlCode(radl);
  }

  private TypeElement requestMapping() {
    String value = SPRING_ANNOTATION_PACKAGE + REQUEST_MAPPING_ANNOTATION;
    TypeElement result = mock(TypeElement.class);
    when(result.getQualifiedName()).thenReturn(name(value));
    when(result.getSimpleName()).thenReturn(name(REQUEST_MAPPING_ANNOTATION));
    return result;
  }

  private Element annotatedClass(TypeElement annotation, String property, String value) {
    return annotatedElement(ElementKind.CLASS, aName(), annotation, property, value);
  }

  @Test
  public void extractsMethod() throws Exception {
    TypeElement annotation = requestMapping();
    String method = "GET";
    Element element = annotatedClass(annotation, "method", SPRING_ANNOTATION_PACKAGE + "RequestMethod." + method);

    assertEquals("URI", method, processor.getMethod(element, annotation));
  }

  @Test
  public void extractsConsumes() {
    TypeElement annotation = requestMapping();
    String consumes = aMediaType();
    Element element = annotatedClass(annotation, "consumes", consumes);

    assertEquals("Consumes", Arrays.asList(consumes), processor.getConsumes(element, annotation));
  }

  private String aMediaType() {
    return aName() + '/' + aName();
  }

  @Test
  public void extractsProduces() {
    TypeElement annotation = requestMapping();
    String produces = aMediaType();
    Element element = annotatedClass(annotation, "produces", produces);

    assertEquals("Produces", Arrays.asList(produces), processor.getProduces(element, annotation));
  }

  @Test
  public void extractsParameter() throws Exception {
    TypeElement annotation = requestParam();
    String parameter = aName();
    Element element = annotatedClass(annotation, "value", parameter);

    assertEquals("RequestParam", parameter, processor.getParameter(element, annotation).getName());
  }

  private TypeElement requestParam() {
    String value = SPRING_ANNOTATION_PACKAGE + REQUEST_PARAM_ANNOTATION;
    TypeElement result = mock(TypeElement.class);
    when(result.getQualifiedName()).thenReturn(name(value));
    when(result.getSimpleName()).thenReturn(name(REQUEST_PARAM_ANNOTATION));
    return result;
  }

}
