/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core;


/**
 * Extremely simple logging.
 */
public final class Log {

  private Log() {
    // Utility class
  }

  public static void info(Object message) {
    System.out.println(message); // NOPMD SystemPrintln
  }

  public static void error(Object message) {
    System.err.println(message); // NOPMD SystemPrintln
  }

}
