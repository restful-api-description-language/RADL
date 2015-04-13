/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;


public class Method extends Documentable {

  private final String name;
  private final String returnType;
  private final Collection<Annotation> annotations;

  public Method(String name, String returnType, String documentation, Collection<Annotation> annotations) {
    super(documentation);
    this.name = name;
    this.returnType = returnType;
    this.annotations = annotations;
  }

  public String getName() {
    return name;
  }

  public String getReturnType() {
    return returnType;
  }

  public Collection<Annotation> getAnnotations() {
    return annotations;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Method && name.equals(((Method)obj).name);
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append(returnType == null ? "void" : returnType).append(' ').append(name).append("()")
        .toString();
  }

}
