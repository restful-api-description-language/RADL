/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import java.io.File;


/**
 * The Perforce {@linkplain SourceCodeManagementSystem}.
 */
public class Perforce implements SourceCodeManagementSystem {

  private final OperatingSystem operatingSystem;

  public Perforce() {
    this(new OperatingSystemImpl());
  }

  public Perforce(OperatingSystem operatingSystem) {
    this.operatingSystem = operatingSystem;
  }

  @Override
  public String getId() {
    return "p4";
  }

  @Override
  public void prepareForUpdate(File file) {
    runPerforceCommandIfNeeded("edit", file);
  }

  private void runPerforceCommandIfNeeded(String command, File file) {
    if (file.canWrite()) {
      return;
    }
    operatingSystem.run(String.format("p4 %s %s", command, file.getAbsolutePath()));
  }

  @Override
  public void prepareForDelete(File file) {
    runPerforceCommandIfNeeded("delete", file);
  }

}
