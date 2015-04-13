/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import java.io.File;


/**
 * Default implementation of {@linkplain SourceCodeManagementSystem}.
 */
public class DefaultSourceCodeManagementSystem implements SourceCodeManagementSystem {

  @Override
  public String getId() {
    return "default";
  }

  @Override
  public void prepareForUpdate(File file) {
    // Nothing to do
  }

  @Override
  public void prepareForDelete(File file) {
    // Nothing to do
  }

}
