/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import java.util.Iterator;
import java.util.ServiceLoader;


/**
 * Factory for creating {@linkplain SourceCodeManagementSystem} instances.
 */
public final class ScmFactory {

  private ScmFactory() {
    // Utility class
  }

  public static SourceCodeManagementSystem newInstance(String id) {
    ServiceLoader<SourceCodeManagementSystem> serviceLoader = ServiceLoader.load(SourceCodeManagementSystem.class);
    Iterator<SourceCodeManagementSystem> services = serviceLoader.iterator();
    while (services.hasNext()) {
      SourceCodeManagementSystem scm = services.next();
      if (scm.getId().equals(id)) {
        return scm;
      }
    }
    throw new IllegalArgumentException("Unknown SCM: " + id);
  }

}
