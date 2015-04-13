/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.cli;

import radl.core.Log;


/**
 * Support for command-line interfaces.
 */
public final class Cli {

  private Cli() {
    // Utility class
  }

  /**
   * Run a command-line application with the provided arguments. This method is typically called from a
   * <code>main()</code> method. All exceptions are caught and printed on stderr. In case of an exception, the
   * application exists with a negative exit code.
   * @param applicationClass The type of application to run
   * @param arguments The arguments to run the application with
   */
  public static void run(Class<? extends Application> applicationClass, String[] arguments) {
    int exitCode;
    try {
      exitCode = applicationClass.newInstance().run(new Arguments(arguments));
    } catch (Exception e) {
      Log.error(e.getMessage());
      exitCode = -1;
    }
    System.exit(exitCode); // NOPMD DoNotCallSystemExit
  }

}
