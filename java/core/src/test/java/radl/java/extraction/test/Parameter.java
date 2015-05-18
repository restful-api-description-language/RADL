/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;


public class Parameter extends Documentable {

  private final String name;
  private final Collection<Annotation> annotations;

  public Parameter(String name, String documentation, Collection<Annotation> annotations) {
    super(documentation);
    this.name = name;
    this.annotations = annotations;
  }

  public Collection<Annotation> getAnnotations() {
    return annotations;
  }

  public String getName() {
    return name;
  }

}
