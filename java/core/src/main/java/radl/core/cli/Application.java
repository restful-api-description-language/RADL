/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.cli;


/**
 * An application with a command-line interface.
 */
public interface Application {

  /**
   * Run the application with the provided arguments.
   * @param arguments The command-line arguments to the application
   * @return The program exit code. Use zero for success and non-zero for failure
   */
  int run(Arguments arguments);

}
