/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;


/**
 * Tell time based on the system clock.
 */
public class SystemClock implements Clock {

  @Override
  public long now() {
    return System.currentTimeMillis();
  }

}
