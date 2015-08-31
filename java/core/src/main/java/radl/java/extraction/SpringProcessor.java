/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import java.util.Collection;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * Annotation processor for Spring REST annotations.
 */
@SupportedOptions(ProcessorOptions.RESOURCE_MODEL_FILE)
public class SpringProcessor extends AbstractRestAnnotationProcessor {

  private static final String ANN_MAPPING = "RequestMapping";
  private static final String ANN_PARAMETER = "RequestParam";
  private static final String SPRING_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation.";
  private static final String METHOD_PREFIX = SPRING_ANNOTATION_PACKAGE + "RequestMethod.";

  public SpringProcessor() {
    super(SPRING_ANNOTATION_PACKAGE, new String[] { ANN_MAPPING, ANN_PARAMETER });
  }

  @Override
  protected Collection<String> getUri(Element element, Collection<TypeElement> annotations) {
    for (TypeElement annotation : annotations) {
      Collection<String> result = valueOf(ANN_MAPPING, annotation, element, "value");
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  @Override
  protected String getMethod(Element element, TypeElement annotation) {
    Collection<String> values = valueOf(ANN_MAPPING, annotation, element, "method");
    if (values == null) {
      return null;
    }
    String result = values.iterator().next();
    if (result != null && result.startsWith(METHOD_PREFIX)) {
      result = result.substring(METHOD_PREFIX.length());
    }
    return result;
  }

  private Collection<String> valueOf(String annotationName, TypeElement annotation, Element element, String property) {
    if (annotation.getSimpleName().contentEquals(annotationName)) {
      return valueOf(annotation, element, property);
    }
    return null;
  }

  @Override
  protected Collection<String> getConsumes(Element element, TypeElement annotation) {
    return valueOf(ANN_MAPPING, annotation, element, "consumes");
  }

  @Override
  protected Collection<String> getProduces(Element element, TypeElement annotation) {
    return valueOf(ANN_MAPPING, annotation, element, "produces");
  }

  @Override
  protected Parameter getParameter(Element element, TypeElement annotation) {
    Collection<String> names = valueOf(ANN_PARAMETER, annotation, element, "value");
    return names == null ? null : new Parameter(names.iterator().next(), getDocumentationFor(element));
  }

}
