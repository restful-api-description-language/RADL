/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Test;

import radl.common.io.IO;
import radl.common.io.StringStream;
import radl.core.Radl;
import radl.test.RandomData;
import radl.test.TestUtil;


public class RelaxNgValidatorTest {

  private static final RandomData RANDOM = new RandomData();

  private final File file = new File(RANDOM.string());
  private final Validator validator = new RelaxNgValidator();
  private final Collection<Issue> issues = new ArrayList<>();

  @After
  public void done() {
    IO.delete(file);
  }

  @Test
  public void acceptsValidRadlFile() {
    validate("<service xmlns='" + Radl.NAMESPACE_URI + "' name='test'/>");

    assertTrue("Unexpected issues added: " + issues, issues.isEmpty());
  }

  private void validate(String contents) {
    validator.validate(new StringStream(contents), issues);
  }

  @Test
  public void addsErrorsForSchemaViolations() {
    validate("<foo/>");

    Issue expectedIssue = new Issue(RelaxNgValidator.class, Issue.Level.ERROR, 1, 7,
        "element \"foo\" not allowed anywhere; expected element \"service\" "
            + "(with xmlns=\"" + Radl.NAMESPACE_URI + "\")");
    TestUtil.assertCollectionEquals("Issues", Arrays.asList(expectedIssue), issues);
  }

}
