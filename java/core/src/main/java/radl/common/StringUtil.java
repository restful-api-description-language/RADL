/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common;


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

  public static String initCap(String text) {
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

}
