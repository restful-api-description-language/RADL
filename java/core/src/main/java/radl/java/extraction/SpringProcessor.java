/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import java.util.Collection;

import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;


/**
 * Annotation processor for Spring REST annotations.
 */
public class SpringProcessor extends AbstractRestAnnotationProcessor {

  private static final String SPRING_ANNOTATION_PACKAGE = "org.springframework.web.bind.annotation.";
  private static final String METHOD_PREFIX = SPRING_ANNOTATION_PACKAGE + "RequestMethod.";

  public SpringProcessor() {
    super(SPRING_ANNOTATION_PACKAGE, new String[] { "RequestMapping" });
  }

  @Override
  protected Collection<String> getUri(Element element, Collection<TypeElement> annotations) {
    return valueOf(annotations.iterator().next(), element);
  }

  @Override
  protected String getMethod(Element element, TypeElement annotation) {
    Collection<String> values = valueOf(annotation, element, "method");
    if (values == null) {
      return null;
    }
    String result = values.iterator().next();
    if (result != null && result.startsWith(METHOD_PREFIX)) {
      result = result.substring(METHOD_PREFIX.length());
    }
    return result;
  }

  @Override
  protected Collection<String> getConsumes(Element element, TypeElement annotation) {
    return valueOf(annotation, element, "consumes");
  }

  @Override
  protected Collection<String> getProduces(Element element, TypeElement annotation) {
    return valueOf(annotation, element, "produces");
  }

}
