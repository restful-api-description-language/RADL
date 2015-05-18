/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
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
import javax.lang.model.util.Types;

import radl.core.Log;
import radl.core.extraction.StringUtil;


/**
 * Base class for processing REST-specific annotations.
 */
public abstract class AbstractRestAnnotationProcessor extends AbstractRestProcessor {

  protected abstract Collection<String> getUri(Element element, Collection<TypeElement> annotations);

  protected abstract String getMethod(Element element, TypeElement annotation);

  protected abstract Collection<String> getConsumes(Element element, TypeElement annotation);

  protected abstract Collection<String> getProduces(Element element, TypeElement annotation);

  protected abstract Parameter getParameter(Element element, TypeElement annotation);

  private final Set<String> supportedAnnotations = new LinkedHashSet<String>();
  private String[] loggableClasses = new String[0];

  public AbstractRestAnnotationProcessor(String packageName, String[] annotationNames) {
    for (String annotation : annotationNames) {
      supportedAnnotations.add(packageName + annotation);
    }
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    return supportedAnnotations;
  }

  protected void setLoggableClasses(String classNames) {
    loggableClasses = classNames == null ? new String[0] : classNames.split(",");
  }

  @Override
  protected void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment environment) {
    Set<? extends Element> allElements = environment.getRootElements();
    for (Entry<Element, Collection<TypeElement>> entry : annotationsByElement(annotations, environment).entrySet()) {
      processAnnotations(allElements, entry.getKey(), entry.getValue());
    }
  }

  private Map<Element, Collection<TypeElement>> annotationsByElement(Set<? extends TypeElement> annotations,
      RoundEnvironment environment) {
    Map<Element, Collection<TypeElement>> result = new HashMap<Element, Collection<TypeElement>>();
    for (TypeElement annotation : annotations) {
      for (Element element : environment.getElementsAnnotatedWith(annotation)) {
        if (shouldIgnore(element.getKind())) {
          continue;
        }
        Collection<TypeElement> elementAnnotations = result.get(element);
        if (elementAnnotations == null) {
          elementAnnotations = new ArrayList<TypeElement>();
          result.put(element, elementAnnotations);
        }
        elementAnnotations.add(annotation);
      }
    }
    return result;
  }

  private boolean shouldIgnore(ElementKind kind) {
    return kind != ElementKind.CLASS && kind != ElementKind.METHOD && kind != ElementKind.PARAMETER;
  }

  private void processAnnotations(Set<? extends Element> allElements, Element element,
      Collection<TypeElement> annotations) {
    Collection<String> uri = null;
    String method = null;
    Collection<String> consumes = null;
    Collection<String> produces = null;
    Collection<Parameter> parameters = new ArrayList<Parameter>();
    uri = getUri(element, annotations);
    for (TypeElement annotation : annotations) {
      method = update(getMethod(element, annotation), method);
      consumes = update(getConsumes(element, annotation), consumes);
      produces = update(getProduces(element, annotation), produces);
      addParameters(getParameter(element, annotation), parameters);
    }
    processElement(allElements, element, uri, method, consumes, produces, parameters);
  }

  private void addParameters(Parameter parameter, Collection<Parameter> parameters) {
    if (parameter != null) {
      parameters.add(parameter);
    }
  }

  private String update(String newValue, String oldValue) {
    return newValue == null ? oldValue : newValue;
  }

  Collection<String> update(Collection<String> newValue, Collection<String> oldValue) {
    return newValue == null ? oldValue : newValue;
  }

  private void processElement(Set<? extends Element> allElements, Element element, Collection<String> uris,
      String method, Collection<String> consumes, Collection<String> produces, Collection<Parameter> parameters) {
    Element classElement = element;
    while (classElement.getKind() != ElementKind.CLASS) {
      classElement = classElement.getEnclosingElement();
    }
    for (Element concreteSubClassElement : getConcreteSubClassesOf(allElements, classElement)) {
      processElement(allElements, element, concreteSubClassElement, uris, method, consumes, produces, parameters);
    }
  }

  private Collection<Element> getConcreteSubClassesOf(Set<? extends Element> allTypes, Element classElement) {
    Collection<Element> result = new HashSet<Element>();
    for (Element subClassElement : getTypesExtendingOrImplementing(allTypes, classElement)) {
      if (!subClassElement.getModifiers().contains(Modifier.ABSTRACT)) {
        result.add(subClassElement);
      }
    }
    return result;
  }

  private void processElement(Set<? extends Element> allTypes, Element element, Element classElement,
      Collection<String> uris, String method, Collection<String> consumes, Collection<String> produces,
      Collection<Parameter> parameters) {
    String resourceName = qualifiedNameOf(classElement);
    addResource(resourceName, getDocumentationFor(classElement));

    if (!parameters.isEmpty()) {
      for (Parameter parameter : parameters) {
        addParameter(resourceName, parameter);
      }
    } else if (uris == null) {
      addMethod(resourceName, method, consumes, produces, getDocumentationFor(element));
    } else {
      if (element.getKind() == ElementKind.METHOD) {
        for (String childResourceName : getChildResourceNames(allTypes, resourceName, element)) {
          addResource(childResourceName, null);
          getResourceModel().addParentResource(childResourceName, resourceName);
          getResourceModel().addLocations(childResourceName, uris);
          addMethod(childResourceName, method, consumes, produces, getDocumentationFor(element));
        }
      } else {
        getResourceModel().addLocations(resourceName, uris);
        addMethod(resourceName, method, consumes, produces, getDocumentationFor(element));
      }
    }
  }

  private void addParameter(String className, Parameter parameter) {
    logClass("Added parameter " + parameter.getName(), className);
    getResourceModel().addLocationVar(className, parameter.getName(), parameter.getDocumentation());
  }

  private void logClass(String message, String className) {
    for (String loggableClass : loggableClasses) {
      if (className.contains(loggableClass)) {
        Log.info(className + " - " + message);
        return;
      }
    }
  }

  private void addResource(String className, String documentation) {
    logClass("Added", className);
    getResourceModel().addResource(className, documentation);
  }

  private void addMethod(String resource, String method, Collection<String> consumes, Collection<String> produces,
      String documentation) {
    if (method != null) {
      getResourceModel().addMethod(resource, method, collectionToString(consumes), collectionToString(produces),
          documentation);
    }
  }

  protected String collectionToString(Collection<String> values) {
    if (values == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String value : values) {
      result.append(prefix).append(value);
      prefix = ",";
    }
    return result.toString();
  }

  private Iterable<String> getChildResourceNames(Set<? extends Element> allTypes, String classResourceName,
      Element methodElement) {
    Element returnType = getReturnType(methodElement);
    if (allTypes.contains(returnType)) {
      Collection<String> result = new HashSet<String>();
      for (Element type : getTypesExtendingOrImplementing(allTypes, returnType)) {
        result.add(qualifiedNameOf(type));
      }
      return result;
    }
    return Collections.singleton(classResourceName + '.' + nameOf(methodElement));
  }

  private Collection<Element> getTypesExtendingOrImplementing(Set<? extends Element> allTypes, Element baseType) {
    Collection<Element> result = new LinkedHashSet<Element>();
    result.add(baseType);
    addTypesExtendingOrImplementing(allTypes, baseType.asType(), result);
    logClass("Sub types: " + result, qualifiedNameOf(baseType));
    return result;
  }

  private void addTypesExtendingOrImplementing(Set<? extends Element> allElements, TypeMirror superType,
      Collection<Element> subElements) {
    Types typeUtils = processingEnv.getTypeUtils();
    for (Element element : allElements) {
      if (subElements.contains(element)) {
        continue;
      }
      TypeMirror type = element.asType();
      if (typeUtils.directSupertypes(type).contains(superType)) {
        subElements.add(element);
        addTypesExtendingOrImplementing(allElements, type, subElements);
      }
    }
  }

  private Element getReturnType(Element methodElement) {
    Element result = null;
    if (methodElement instanceof ExecutableElement) {
      TypeMirror returnType = ((ExecutableElement)methodElement).getReturnType();
      if (returnType instanceof DeclaredType) {
        result = ((DeclaredType)returnType).asElement();
      }
    }
    return result;
  }

  protected Collection<String> valueOf(TypeElement annotation, Element element) {
    return valueOf(annotation, element, "value");
  }

  protected Collection<String> valueOf(TypeElement annotation, Element element, String property) {
    if (annotation == null) {
      return null;
    }
    Name annotationClassName = annotation.getQualifiedName();
    for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      TypeElement annotationElement = (TypeElement)annotationMirror.getAnnotationType().asElement();
      if (annotationElement.getQualifiedName().contentEquals(annotationClassName)) {
        return valueOf(annotationMirror, property);
      }
    }
    return null;
  }

  private Collection<String> valueOf(AnnotationMirror annotation, String property) {
    for (Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : annotation.getElementValues().entrySet()) {
      if (entry.getKey().getSimpleName().contentEquals(property)) {
        Object values = entry.getValue().getValue();
        if (values instanceof Iterable) {
          Collection<String> result = new ArrayList<String>();
          for (Object value : (Iterable<?>)values) {
            result.add(StringUtil.stripQuotes(value.toString()));
          }
          return result;
        }
        if (values instanceof String) {
          return Arrays.asList(StringUtil.stripQuotes(values.toString()));
        }
        throw new IllegalStateException("Unhandled annotation value type: " + values.getClass().getName());
      }
    }
    return null;
  }

  protected String singleValueOf(TypeElement annotation, Element element) {
    Collection<String> result = valueOf(annotation, element);
    return result == null ? null : result.iterator().next();
  }

}
