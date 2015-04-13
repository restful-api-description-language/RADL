/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import radl.java.extraction.test.TestName;
import radl.test.RandomData;


/**
 * Base class for tests on REST annotation processors.
 */
public class AbstractRestAnnotationProcessorTest {

  private static final RandomData RANDOM = new RandomData();

  protected Element annotatedElement(ElementKind kind, String name, TypeElement annotation, String property,
      String value) {
    return setAnnotationValue(element(kind, name), annotation, property, value);
  }

  protected Element setAnnotationValue(Element element, TypeElement annotation, final String property,
      final String value) {
    final AnnotationMirror annotationMirror = mock(AnnotationMirror.class);
    DeclaredType annotationType = mock(DeclaredType.class);
    when(annotationType.asElement()).thenReturn(annotation);
    when(annotationMirror.getAnnotationType()).thenReturn(annotationType);
    when(annotationMirror.getElementValues()).thenAnswer(
        new Answer<Map<? extends ExecutableElement, ? extends AnnotationValue>>() {
      @Override
      public Map<? extends ExecutableElement, ? extends AnnotationValue> answer(InvocationOnMock invocation)
          throws Throwable {
        ExecutableElement executableElement = mock(ExecutableElement.class);
        when(executableElement.getSimpleName()).thenReturn(name(property));
        AnnotationValue annotationValue = mock(AnnotationValue.class);
        when(annotationValue.getValue()).thenReturn(value);
        return Collections.singletonMap(executableElement, annotationValue);
      }
    });
    when(element.getAnnotationMirrors()).thenAnswer(new Answer<List<? extends AnnotationMirror>>() {
      @Override
      public List<? extends AnnotationMirror> answer(InvocationOnMock invocation) throws Throwable {
        return Arrays.asList(annotationMirror);
      }
    });
    return element;
  }

  protected String aName() {
    return RANDOM.string(5);
  }

  protected Element element(ElementKind kind, String name) {
    Element result;
    switch (kind) {
      case CLASS:
        result = mock(TypeElement.class, "class " + name);
        when(((TypeElement)result).getQualifiedName()).thenReturn(name(name));
        break;
      case METHOD:
        result = mock(ExecutableElement.class, "method " + name);
        break;
      default:
        result = mock(Element.class, "element(" + kind + ") " + name);
        break;
    }
    when(result.getKind()).thenReturn(kind);
    when(result.getSimpleName()).thenReturn(name(name));
    return result;
  }

  protected Name name(String value) {
    return new TestName(value);
  }

}
