/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import java.io.File;


/**
 * Facade to source code management systems.
 */
public interface SourceCodeManagementSystem {

  /**
   * @return The ID of this type of source code management system.
   */
  String getId();

  /**
   * Do whatever is needed to update a given file.
   * @param file The file that is about to be updated
   */
  void prepareForUpdate(File file);

  /**
   * Do whatever is needed to delete a given file.
   * @param file The file that is about to be deleted
   */
  void prepareForDelete(File file);

}
