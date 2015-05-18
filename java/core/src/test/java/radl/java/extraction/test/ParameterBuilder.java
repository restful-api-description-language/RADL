/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;
import java.util.HashSet;


public class ParameterBuilder implements Annotatable<ParameterBuilder> {

  private final MethodBuilder parent;
  private final String name;
  private final Collection<Annotation> annotations = new HashSet<Annotation>();
  private String documentation;

  public ParameterBuilder(MethodBuilder parent, String name) {
    this.name = name;
    this.parent = parent;
  }

  @Override
  public ParameterBuilder annotateWith(Collection<Annotation> ann) {
    annotations.addAll(ann);
    return this;
  }

  public AnnotationBuilder<ParameterBuilder> annotatedWith() {
    return new AnnotationBuilder<ParameterBuilder>(this);
  }

  public ParameterBuilder documentedWith(String provided) {
    this.documentation = provided;
    return this;
  }

  public MethodBuilder end() {
    return parent.addParameter(new Parameter(name, documentation, annotations));
  }

}
