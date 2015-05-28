/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.util.Iterator;
import java.util.ServiceLoader;

import radl.core.scm.SourceCodeManagementSystem;


/**
 * Factory for creating {@linkplain SourceCodeManagementSystem} instances.
 */
public final class IssueReporterFactory {

  private IssueReporterFactory() {
    // Utility class
  }

  public static IssueReporter newInstance(String id) {
    ServiceLoader<IssueReporter> serviceLoader = ServiceLoader.load(IssueReporter.class);
    Iterator<IssueReporter> services = serviceLoader.iterator();
    while (services.hasNext()) {
      IssueReporter reporter = services.next();
      if (reporter.getId().equals(id)) {
        return reporter;
      }
    }
    throw new IllegalArgumentException("Unknown issue reporter: " + id);
  }

}
