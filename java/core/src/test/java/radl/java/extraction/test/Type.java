/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import java.util.Collection;


public class Type extends Documentable {

  private final boolean isInterface;
  private final boolean isAbstract;
  private final String name;
  private final Collection<String> superTypes;
  private final Collection<Annotation> annotations;
  private final Collection<Method> methods;

  public Type(boolean isInterface, boolean isAbstract, String name, Collection<String> superTypes,
      String documentation, Collection<Annotation> annotations, Collection<Method> methods) {
    super(documentation);
    this.isInterface = isInterface;
    this.isAbstract = isAbstract;
    this.name = name;
    this.superTypes = superTypes;
    this.annotations = annotations;
    this.methods = methods;
  }

  public boolean isInterface() {
    return isInterface;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public String getName() {
    return name;
  }

  public Collection<String> getSuperTypes() {
    return superTypes;
  }

  public Collection<Annotation> getAnnotations() {
    return annotations;
  }

  public Collection<Method> getMethods() {
    return methods;
  }

  @Override
  public int hashCode() {
    int factor = isInterface ? 1 : 31;
    return factor * name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof Type)) {
      return false;
    }
    Type other = (Type)obj;
    return isInterface == other.isInterface && name.equals(other.name);
  }

  @Override
  public String toString() {
    return new StringBuilder().append(isInterface ? "interface " : "class ").append(name).toString();
  }

}
