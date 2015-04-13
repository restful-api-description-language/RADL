/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.InputStream;
import java.util.Collection;


/**
 * Validate a RADL document.
 */
public interface Validator {

  /**
   * Add issues in a given RADL document to a given list of issues.
   * @param radl The RADL document
   * @param issues The list of issues to add to
   */
  void validate(InputStream radl, Collection<Issue> issues);

}
