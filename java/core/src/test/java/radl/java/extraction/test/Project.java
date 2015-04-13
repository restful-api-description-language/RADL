/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;


public class Project {

  private final TestRoundEnvironment environment;
  private final Types types;
  private final Elements elements;
  private final Map<String, String> options;

  public Project(TestRoundEnvironment environment, Types types, Elements elements, Map<String, String> options) {
    this.environment = environment;
    this.types = types;
    this.elements = elements;
    this.options = options;
  }

  public boolean apply(Processor processor) {
    ProcessingEnvironment processingEnvironment = mock(ProcessingEnvironment.class);
    when(processingEnvironment.getOptions()).thenReturn(options);
    when(processingEnvironment.getTypeUtils()).thenReturn(types);
    when(processingEnvironment.getElementUtils()).thenReturn(elements);
    processor.init(processingEnvironment);

    return processor.process(environment.getAnnotations(), environment);
  }

  public TypeElement getRootElement(String name) {
    for (Element element : environment.getRootElements()) {
      if (element.getSimpleName().contentEquals(name)) {
        return (TypeElement)element;
      }
    }
    return null;
  }

  public TypeElement getAnnotation(Element element) {
    return environment.getAnnotationsOn(element).iterator().next();
  }

}
