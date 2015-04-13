/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;
import java.util.HashSet;


public class TypeBuilder implements Annotatable<TypeBuilder> {

  private final ProjectBuilder parent;
  private final boolean isInterface;
  private final boolean isAbstract;
  private final String name;
  private final Collection<Annotation> annotations = new HashSet<Annotation>();
  private final Collection<Method> methods = new HashSet<Method>();
  private final Collection<String> superTypes = new HashSet<String>();
  private String documentation;

  public TypeBuilder(ProjectBuilder parent, boolean isInterface, boolean isAbstract, String name) {
    this.parent = parent;
    this.isInterface = isInterface;
    this.isAbstract = isAbstract;
    this.name = name;
  }

  public AnnotationBuilder<TypeBuilder> annotatedWith() {
    return new AnnotationBuilder<TypeBuilder>(this);
  }

  @Override
  public TypeBuilder annotateWith(Collection<Annotation> provided) {
    this.annotations.addAll(provided);
    return this;
  }

  public MethodBuilder withMethod(String method) {
    return new MethodBuilder(this, method);
  }

  public ProjectBuilder end() {
    return parent.addType(new Type(isInterface, isAbstract, name, superTypes, documentation, annotations, methods));
  }

  TypeBuilder addMethod(Method method) {
    methods.add(method);
    return this;
  }

  public TypeBuilder derivedFrom(String superType) {
    superTypes.add(superType);
    return this;
  }

  public TypeBuilder documentedWith(String javaDoc) {
    this.documentation = javaDoc;
    return this;
  }

}
