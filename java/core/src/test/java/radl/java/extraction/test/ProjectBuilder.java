/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public final class ProjectBuilder {

  public static ProjectBuilder project() {
    return new ProjectBuilder();
  }

  private final TestRoundEnvironment environment;
  private final Types types = mock(Types.class);
  private final Elements elements = mock(Elements.class);
  private final Map<String, DeclaredType> missingTypes = new HashMap<>();
  private final Map<String, String> options = new HashMap<>();

  private ProjectBuilder() {
    environment = new TestRoundEnvironment();
  }

  public TypeBuilder withClass(String name) {
    return withType(false, false, name);
  }

  private TypeBuilder withType(boolean isInterface, boolean isAbstract, String name) {
    return new TypeBuilder(this, isInterface, isAbstract, name);
  }

  public TypeBuilder withInterface(String name) {
    return withType(true, false, name);
  }

  public TypeBuilder withAbstractClass(String name) {
    return withType(false, true, name);
  }

  public Project build() {
    if (!missingTypes.isEmpty()) {
      throw new IllegalStateException("Missing types: " + missingTypes.keySet());
    }
    return new Project(environment, types, elements, options);
  }

  ProjectBuilder addType(Type type) {
    Element typeElement = newType(type.isInterface(), type.getName());
    if (type.isAbstract()) {
      when(typeElement.getModifiers()).thenReturn(Collections.singleton(Modifier.ABSTRACT));
    }
    environment.addRootElement(typeElement);
    annotate(typeElement, type.getAnnotations());
    document(typeElement, type);
    addMethods(type, typeElement);
    if (!type.getSuperTypes().isEmpty()) {
      addSuperTypes(type, typeElement);
    }
    return this;
  }

  private Element newType(boolean isInterface, String name) {
    Element result = newElement(isInterface ? ElementKind.INTERFACE : ElementKind.CLASS, name);
    if (missingTypes.containsKey(name)) {
      DeclaredType type = missingTypes.remove(name);
      when(type.asElement()).thenReturn(result);
    }
    return result;
  }

  private Element newElement(ElementKind kind, String name) {
    Element result;
    switch (kind) {
      case CLASS:
      case INTERFACE:
        result = mock(TypeElement.class, kind + ": " + name);
        when(((TypeElement)result).getQualifiedName()).thenReturn(namedAs(name));
        DeclaredType typeMirror = mock(DeclaredType.class, "type for " + kind + ": " + name);
        when(typeMirror.asElement()).thenReturn(result);
        when(result.asType()).thenReturn(typeMirror);
        break;
      case METHOD:
        result = mock(ExecutableElement.class, "method: " + name);
        break;
      default:
        result = mock(Element.class, "element(" + kind + "): " + name);
        break;
    }
    when(result.getKind()).thenReturn(kind);
    when(result.getSimpleName()).thenReturn(namedAs(name));
    return result;
  }

  private Name namedAs(String name) {
    return new TestName(name);
  }

  private void annotate(Element element, Collection<Annotation> annotations) {
    for (Annotation annotation : annotations) {
      environment.annotateElement(element, annotate(element, annotation));
    }
  }

  private TypeElement annotate(final Element annotated, final Annotation annotation) {
    String name = annotation.getName();
    TypeElement result = mock(TypeElement.class, "annotation: " + annotation);
    when(result.getQualifiedName()).thenReturn(namedAs(name));
    when(result.getSimpleName()).thenReturn(namedAs(name.substring(name.lastIndexOf('.') + 1)));

    final AnnotationMirror annotationMirror = mock(AnnotationMirror.class);
    DeclaredType annotationType = mock(DeclaredType.class);
    when(annotationType.asElement()).thenReturn(result);
    when(annotationMirror.getAnnotationType()).thenReturn(annotationType);
    when(annotationMirror.getElementValues()).thenAnswer(
        new Answer<Map<? extends ExecutableElement, ? extends AnnotationValue>>() {
      @Override
      public Map<? extends ExecutableElement, ? extends AnnotationValue> answer(InvocationOnMock invocation)
          throws Throwable {
        ExecutableElement executableElement = mock(ExecutableElement.class);
        when(executableElement.getSimpleName()).thenReturn(namedAs(annotation.getProperty()));
        AnnotationValue annotationValue = mock(AnnotationValue.class);
        when(annotationValue.getValue()).thenReturn(annotation.getValue());
        return Collections.singletonMap(executableElement, annotationValue);
      }
    });
    final List<? extends AnnotationMirror> previous = annotated.getAnnotationMirrors();
    when(annotated.getAnnotationMirrors()).thenAnswer(new Answer<List<? extends AnnotationMirror>>() {
      @Override
      public List<? extends AnnotationMirror> answer(InvocationOnMock invocation) throws Throwable {
        List<AnnotationMirror> annotationMirrors = new ArrayList<>();
        if (previous != null) {
          annotationMirrors.addAll(previous);
        }
        annotationMirrors.add(annotationMirror);
        return annotationMirrors;
      }
    });

    return result;
  }

  private void document(Element element, Documentable documentable) {
    if (documentable.hasDocumentation()) {
      when(elements.getDocComment(element)).thenReturn(documentable.getDocumentation());
    }
  }

  private void addMethods(Type type, Element classElement) {
    for (Method method : type.getMethods()) {
      Element methodElement = newMethod(method);
      when(methodElement.getEnclosingElement()).thenReturn(classElement);
      document(methodElement, method);
      annotate(methodElement, method.getAnnotations());
    }
  }

  private Element newMethod(Method method) {
    Element result = newElement(ElementKind.METHOD, method.getName());
    if (method.getReturnType() != null) {
      ExecutableElement executableElement = (ExecutableElement)result;
      DeclaredType returnType = mock(DeclaredType.class);
      when(executableElement.getReturnType()).thenReturn(returnType);
      String type = method.getReturnType();
      Element element = findElement(type);
      if (element == null) {
        if (missingTypes.containsKey(type)) {
          // Conceivable, but not yet encountered. When this happens, let [missingTypes] hold collections
          throw new IllegalStateException("Multiple references to type: " + type);
        }
        missingTypes.put(type, returnType);
      } else {
        when(returnType.asElement()).thenReturn(element);
      }
    }
    for (Parameter parameter : method.getParameters()) {
      Element parameterElement = newElement(ElementKind.PARAMETER, parameter.getName());
      when(parameterElement.getEnclosingElement()).thenReturn(result);
      annotate(parameterElement, parameter.getAnnotations());
      document(parameterElement, parameter);
    }
    return result;
  }

  private Element findElement(String name) {
    for (Element rootElement : environment.getRootElements()) {
      if (rootElement.getSimpleName().contentEquals(name)) {
        return rootElement;
      }
    }
    return null;
  }

  private void addSuperTypes(Type type, Element classElement) {
    final List<TypeMirror> superTypes = new ArrayList<>();
    for (String name : type.getSuperTypes()) {
      Element element = findElement(name);
      if (element == null) {
        throw new IllegalArgumentException("Unknown super type: " + name);
      }
      superTypes.add(element.asType());
    }
    TypeMirror typeMirror = classElement.asType();
    when(types.directSupertypes(typeMirror)).thenAnswer(new Answer<Collection<? extends TypeMirror>>() {
      @Override
      public Collection<? extends TypeMirror> answer(InvocationOnMock invocation) throws Throwable {
        return superTypes;
      }
    });
  }

  public ProjectBuilder withAnnotationProcessorOption(String key, String value) {
    options.put(key, value);
    return this;
  }

}
