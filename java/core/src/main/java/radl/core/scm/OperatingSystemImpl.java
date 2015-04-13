/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;


public class OperatingSystemImpl implements OperatingSystem {

  @Override
  public void run(String command) {
    try {
      Process process = Runtime.getRuntime().exec(command);
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new RuntimeException("Command " + command + " exited with value " + exitCode);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
