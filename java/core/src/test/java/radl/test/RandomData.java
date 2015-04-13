/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import java.security.SecureRandom;


/**
 * Random pieces of data. Useful for testing, for instance.
 */
public final class RandomData {

  private static final int MIN_STRING_LENGTH = 3;
  private static final int MAX_STRING_LENGTH = 64;
  private static final int MIN_INTEGER = 0;
  private static final int MAX_INTEGER = 1000;

  private final SecureRandom random;

  public RandomData() {
    this(new SecureRandom());
  }

  public RandomData(SecureRandom random) {
    this.random = random;
  }

  public String string() {
    return string(integer(MIN_STRING_LENGTH, MAX_STRING_LENGTH));
  }

  public String string(int length) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < length; i++) {
      result.append(lowercaseLetter());
    }
    return result.toString();
  }

  public char lowercaseLetter() {
    return (char)('a' + integer('z' - 'a' + 1));
  }

  public int integer() {
    return integer(MAX_INTEGER);
  }

  public int integer(int max) {
    return integer(MIN_INTEGER, max);
  }

  public int integer(int min, int max) {
    ensureMinMax(min, max);
    return min + random.nextInt(max - min);
  }

  private void ensureMinMax(int min, int max) {
    if (min >= max) {
      throw new IllegalArgumentException(String.format("Min (%d) must be less than max (%d)", min, max));
    }
  }

  public boolean logical() {
    return logical(50);
  }

  public boolean logical(int percent) {
    ensureMinMax(0, percent);
    ensureMinMax(percent, 101);
    return integer(100) < percent;
  }

}
