/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;


public class TestRoundEnvironment implements RoundEnvironment {

  private final Set<Element> rootElements = new HashSet<Element>();
  private final Map<TypeElement, Element> elementsByAnnotation = new HashMap<TypeElement, Element>();

  public void addRootElement(Element element) {
    rootElements.add(element);
  }

  public void annotateElement(Element element, TypeElement annotation) {
    elementsByAnnotation.put(annotation, element);
    if (element.getKind() == ElementKind.CLASS) {
      addRootElement(element);
    } else if (element instanceof ExecutableElement) {
      addReturnTypeAsRootElement(element);
    }
  }

  private void addReturnTypeAsRootElement(Element element) {
    TypeMirror returnType = ((ExecutableElement)element).getReturnType();
    if (returnType instanceof DeclaredType) {
      Element returnElement = ((DeclaredType)returnType).asElement();
      if (returnElement != null) {
        rootElements.add(returnElement);
      }
    }
  }

  public Set<TypeElement> getAnnotations() {
    return elementsByAnnotation.keySet();
  }

  public Iterable<TypeElement> getAnnotationsOn(Element element) {
    Collection<TypeElement> result = new ArrayList<TypeElement>();
    for (Entry<TypeElement, Element> entry : elementsByAnnotation.entrySet()) {
      if (element.getSimpleName().contentEquals(entry.getValue().getSimpleName())) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  @Override
  public Set<? extends Element> getElementsAnnotatedWith(TypeElement annotation) {
    return Collections.singleton(elementsByAnnotation.get(annotation));
  }

  @Override
  public Set<? extends Element> getRootElements() {
    return rootElements;
  }

  @Override
  public boolean processingOver() {
    return false;
  }

  @Override
  public boolean errorRaised() {
    return false;
  }

  @Override
  public Set<? extends Element> getElementsAnnotatedWith(Class<? extends Annotation> a) {
    return null;
  }

}
