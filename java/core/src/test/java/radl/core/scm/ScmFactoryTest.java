/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import radl.test.RandomData;


public class ScmFactoryTest {

  private static final RandomData RANDOM = new RandomData();

  private final String id = RANDOM.string();

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionOnUnknownId() {
    ScmFactory.newInstance(id);
  }

  @Test
  public void returnsPerforceScm() throws Exception {
    assertScm(Perforce.class, "p4");
  }

  private void assertScm(Class<? extends SourceCodeManagementSystem> scmClass, String scmId) {
    assertEquals("SCM", scmClass, ScmFactory.newInstance(scmId).getClass());
  }

  @Test
  public void returnsDefaultScm() throws Exception {
    assertScm(DefaultSourceCodeManagementSystem.class, "default");
  }

}
