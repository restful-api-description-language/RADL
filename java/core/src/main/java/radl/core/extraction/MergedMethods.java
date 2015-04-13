/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.extraction;

import java.util.Collection;

class MergedMethods {

  private final Collection<String> resources;
  private final Collection<Method> methods;

  public MergedMethods(Collection<String> resources, Collection<Method> methods) {
    this.resources = resources;
    this.methods = methods;
  }

  public Collection<String> getResources() {
    return resources;
  }

  public Collection<Method> getMethods() {
    return methods;
  }

}