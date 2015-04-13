/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;


/**
 * Time the duration of an operation. Create the <code>Timer</code>, perform the operation, then use the
 * <code>Timer</code>'s {@linkplain #toString()} method to acquire a human readable representation of the duration.
 */
public class Timer {

  private final Clock clock;
  private final long start;

  public Timer() {
    this(new SystemClock());
  }

  public Timer(Clock clock) {
    this.clock = clock;
    this.start = clock.now();
  }

  @Override
  public String toString() {
    long value = clock.now() - start;
    long remainder = 0;
    String separator = ".";
    String unit = "ms";
    if (value >= 1000) {
      remainder = (value % 1000) / 100;
      value /= 1000;
      unit = "s";
      if (value >= 60) {
        separator = ":";
        remainder = value % 60;
        value /= 60;
        unit = "min";
      }
    }
    StringBuilder result = new StringBuilder();
    result.append(value);
    if (remainder > 0) {
      result.append(separator).append(remainder);
    }
    return result.append(' ').append(unit).toString();
  }

}
