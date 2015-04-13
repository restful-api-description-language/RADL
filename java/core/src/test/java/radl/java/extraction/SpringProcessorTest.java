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

import java.util.Arrays;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;

import org.junit.Test;


public class SpringProcessorTest extends AbstractRestAnnotationProcessorTest {

  private static final String SPRING_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation.";
  private static final String REQUEST_MAPPING_ANNOTATION = "RequestMapping";

  private final SpringProcessor processor = new SpringProcessor();

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
  }

  @Test
  public void extractsUri() throws Exception {
    TypeElement annotation = requestMapping();
    String uri = '/' + aName() + '/';
    Element element = annotatedClass(annotation, "value", uri);

    assertEquals("URI", Arrays.asList(uri), processor.getUri(element, Arrays.asList(annotation)));
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

}
