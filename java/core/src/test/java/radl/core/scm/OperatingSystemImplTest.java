/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;

import org.junit.Test;


public class OperatingSystemImplTest {

  @Test
  public void executesCommand() {
    String command = UUID.randomUUID().toString();

    try {
      new OperatingSystemImpl().run(command);
      fail("Missing exception");
    } catch (RuntimeException e) {
      assertNotNull("Missing cause", e.getCause());
      assertEquals("Exception", IOException.class, e.getCause().getClass());
      assertTrue("Exception message", e.getCause().getMessage().contains(command));
    }
  }

}
