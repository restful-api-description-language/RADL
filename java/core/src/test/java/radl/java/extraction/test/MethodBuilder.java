/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;
import java.util.HashSet;


public class MethodBuilder implements Annotatable<MethodBuilder> {

  private final TypeBuilder parent;
  private final String name;
  private final Collection<Annotation> annotations = new HashSet<Annotation>();
  private String returnType;
  private String documentation;

  public MethodBuilder(TypeBuilder parent, String name) {
    this.parent = parent;
    this.name = name;
  }

  public AnnotationBuilder<MethodBuilder> annotatedWith() {
    return new AnnotationBuilder<MethodBuilder>(this);
  }

  @Override
  public MethodBuilder annotateWith(Collection<Annotation> provided) {
    annotations.addAll(provided);
    return this;
  }

  public MethodBuilder returning(String type) {
    returnType = type;
    return this;
  }

  public TypeBuilder end() {
    return parent.addMethod(new Method(name, returnType, documentation, annotations));
  }

  public MethodBuilder documentedWith(String provided) {
    this.documentation = provided;
    return this;
  }

}
