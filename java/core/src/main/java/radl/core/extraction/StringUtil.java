/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;


/**
 * Utility methods for working with quoted strings.
 */
public final class StringUtil {

  private StringUtil() {
    // Utility class
  }

  public static boolean isQuoted(String value) {
    return value.matches("\\\"[^\\\"]+\\\"");
  }

  public static String stripQuotes(String value) {
    if (isQuoted(value)) {
      return value.substring(1, value.length() - 1);
    }
    return value;
  }

}
