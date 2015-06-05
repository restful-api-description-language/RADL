/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core;

import java.io.PrintStream;


/**
 * Extremely simple logging.
 */
public final class Log {

  private static boolean active = true;

  private Log() {
    // Utility class
  }

  public static void activate() {
    active = true;
  }

  public static void deactivate() {
    active = false;
  }

  public static void info(Object message) {
    log(System.out, message);
  }

  private static void log(PrintStream out, Object message) {
    if (active) {
      out.println(message); // NOPMD SystemPrintln
    }
  }

  public static void error(Object message) {
    log(System.err, message);
  }

}
