/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

import radl.test.RandomData;


public class TimerTest {

  private static final RandomData RANDOM = new RandomData();

  private final Clock clock = mock(Clock.class);
  private final Timer timer = new Timer(clock);

  @Test
  public void showsMilliSeconds() {
    long ms = someMilliSeconds();

    assertTime(ms, ms + " ms");
  }

  private int someMilliSeconds() {
    return RANDOM.integer(100, 999);
  }

  private void assertTime(long now, String expected) {
    when(clock.now()).thenReturn(now);
    assertEquals(now + " ms", expected, timer.toString());
  }

  @Test
  public void showsSecondsAndMilliSeconds() {
    long sec = someSeconds();
    int ms = someMilliSeconds() / 100;

    assertTime(1000 * sec + 100 * ms + RANDOM.integer(49), sec + "." + ms + " s");
  }

  private int someSeconds() {
    return RANDOM.integer(1, 59);
  }

  @Test
  public void showsMinutesAndSeconds() {
    long min = someMinutes();
    int sec = someSeconds();
    long now = 1000 * (60 * min + sec) + someMilliSeconds();

    assertTime(now, min + ":" + sec + " min");
  }

  private long someMinutes() {
    return someSeconds();
  }

}
