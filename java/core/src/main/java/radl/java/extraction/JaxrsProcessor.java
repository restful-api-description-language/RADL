/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;


/**
 * Annotation processor for JAX-RS annotations.
 * <p>
 * JAX-RS has no support for PATCH. Projects that implement their own PATCH method should set the
 * {@value #PATCH_OPTION} annotation processor option to the fully qualified name of their PATCH annotation.
 */
public class JaxrsProcessor extends AbstractRestAnnotationProcessor {

  public static final String PATCH_OPTION = "jaxrs.patch";
  public static final String LOG_OPTION = "jaxrs.log";
  private static final String BASE_URI = "ApplicationPath";
  private static final String RELATIVE_URI = "Path";
  private static final Collection<String> METHOD_ANNOTATIONS = Arrays.asList("GET", "PUT", "DELETE", "POST");

  private String patchAnnotation;

  public JaxrsProcessor() {
    super("javax.ws.rs.", new String[] { RELATIVE_URI, BASE_URI, "GET", "PUT", "DELETE", "POST", "Consumes",
        "Produces", "QueryParam" });
  }

  @Override
  public Set<String> getSupportedOptions() {
    return new HashSet<String>(Arrays.asList(PATCH_OPTION, LOG_OPTION));
  }

  @Override
  public synchronized void init(ProcessingEnvironment environment) {
    super.init(environment);
    patchAnnotation = environment.getOptions().get(PATCH_OPTION);
    setLoggableClasses(environment.getOptions().get(LOG_OPTION));
  }

  @Override
  public Set<String> getSupportedAnnotationTypes() {
    if (patchAnnotation == null) {
      return super.getSupportedAnnotationTypes();
    }
    Set<String> result = new HashSet<String>();
    result.addAll(super.getSupportedAnnotationTypes());
    result.add(patchAnnotation);
    return result;
  }

  @Override
  protected Collection<String> getUri(Element element, Collection<TypeElement> annotations) {
    StringBuilder result = new StringBuilder();
    appendBaseUri(element, getAnnotation(annotations, BASE_URI), result);
    appendRelativeUri(element, getAnnotation(annotations, RELATIVE_URI), result);
    return result.length() == 0 ? null : Arrays.asList(result.toString());
  }

  private TypeElement getAnnotation(Collection<TypeElement> annotations, String name) {
    for (TypeElement annotation : annotations) {
      if (name.equals(nameOf(annotation))) {
        return annotation;
      }
    }
    return null;
  }

  private void appendBaseUri(Element element, TypeElement applicationPathAnnotation, StringBuilder uri) {
    String result = singleValueOf(applicationPathAnnotation, element);
    if (result == null || result.isEmpty()) {
      return;
    }
    if (result.charAt(0) != '/') {
      uri.append('/');
    }
    uri.append(result);
  }

  private void appendRelativeUri(Element element, TypeElement pathAnnotation, StringBuilder uri) {
    String path = singleValueOf(pathAnnotation, element);
    if (path == null || path.isEmpty()) {
      return;
    }
    if (uri.length() == 0) {
      uri.append(path);
    } else if (uri.charAt(uri.length() - 1) == '/') {
      if (path.charAt(0) == '/') {
        uri.append(path.substring(1));
      } else {
        uri.append(path);
      }
    } else {
      if (path.charAt(0) == '/') {
        uri.append(path);
      } else {
        uri.append('/').append(path);
      }
    }
  }

  @Override
  protected String getMethod(Element element, TypeElement annotation) {
    String result = nameOf(annotation);
    return METHOD_ANNOTATIONS.contains(result) || qualifiedNameOf(annotation).equals(patchAnnotation)
        ? result : null;
  }

  @Override
  protected Collection<String> getConsumes(Element element, TypeElement annotation) {
    return annotationValue(element, annotation, "Consumes");
  }

  private Collection<String> annotationValue(Element element, TypeElement annotation, String annotationName) {
    return annotation.getSimpleName().contentEquals(annotationName) ? valueOf(annotation, element) : null;
  }

  @Override
  protected Collection<String> getProduces(Element element, TypeElement annotation) {
    return annotationValue(element, annotation, "Produces");
  }

}
