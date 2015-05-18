/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;
import java.util.HashSet;


public class AnnotationBuilder<T extends Annotatable<T>> {

  private static final String ANNOTATION_PACKAGE = "javax.ws.rs.";

  private final T parent;
  private final Collection<Annotation> annotations = new HashSet<Annotation>();

  public AnnotationBuilder(T parent) {
    this.parent = parent;
  }

  public AnnotationBuilder<T> path(String uri) {
    annotations.add(new Annotation(ANNOTATION_PACKAGE + "Path", uri));
    return this;
  }

  public AnnotationBuilder<T> applicationPath(String uri) {
    annotations.add(new Annotation(ANNOTATION_PACKAGE + "ApplicationPath", uri));
    return this;
  }

  public AnnotationBuilder<T> method(String method) {
    annotations.add(new Annotation(ANNOTATION_PACKAGE + method));
    return this;
  }

  public AnnotationBuilder<T> consuming(String mediaType) {
    annotations.add(new Annotation(ANNOTATION_PACKAGE + "Consumes", mediaType));
    return this;
  }

  public AnnotationBuilder<T> producing(String mediaType) {
    annotations.add(new Annotation(ANNOTATION_PACKAGE + "Produces", mediaType));
    return this;
  }

  public AnnotationBuilder<T> annotation(String annotation) {
    annotations.add(new Annotation(annotation));
    return this;
  }

  public T end() {
    return parent.annotateWith(annotations);
  }

  public AnnotationBuilder<T> pathParam(String name) {
    annotations.add(new Annotation(ANNOTATION_PACKAGE + "PathParam", name));
    return this;
  }

}
