/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import radl.test.RandomData;


public class IssueReporterFactoryTest {

  private static final RandomData RANDOM = new RandomData();

  private final String id = RANDOM.string();

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionOnUnknownId() {
    IssueReporterFactory.newInstance(id);
  }

  @Test
  public void checkStyle() throws Exception {
    assertIssueReporter(CheckStyleIssueReporter.class, "checkstyle");
  }

  private void assertIssueReporter(Class<? extends IssueReporter> scmClass, String scmId) {
    assertEquals("SCM", scmClass, IssueReporterFactory.newInstance(scmId).getClass());
  }

}
