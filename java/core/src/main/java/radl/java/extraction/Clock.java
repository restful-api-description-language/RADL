/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;


/**
 * An instrument for measuring time.
 */
public interface Clock {

  /**
   * @return The current time, in milliseconds passed since midnight, January 1, 1970 UTC
   */
  long now();

}
