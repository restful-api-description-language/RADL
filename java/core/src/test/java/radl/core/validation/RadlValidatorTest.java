/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Test;

import radl.common.io.IO;
import radl.core.cli.Arguments;
import radl.core.validation.Issue.Level;
import radl.test.RandomData;
import radl.test.TestUtil;


public class RadlValidatorTest {

  private static final RandomData RANDOM = new RandomData();

  private final File dir = TestUtil.randomDir(RadlValidatorTest.class);
  private final RadlValidator radlValidator = new RadlValidator();

  @After
  public void done() {
    IO.delete(dir);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void validatesAllFiles() throws IOException {
    int numFiles = RANDOM.integer(2, 5);
    String[] files = new String[numFiles];
    String[] fileNames = new String[numFiles];
    char ch = 'a';
    for (int i = 0; i < numFiles; i++) {
      files[i] = randomFileName(ch++);
      fileNames[i] = new File(files[i]).getName();
    }
    Arguments args = new Arguments(files);
    Validator validator = mock(Validator.class);
    Map<String, Collection<Issue>> issues = new TreeMap<String, Collection<Issue>>();

    radlValidator.validate(args, validator, issues);

    verify(validator, times(files.length)).validate(any(FileInputStream.class), any(Collection.class));
    TestUtil.assertCollectionEquals("Validated files", Arrays.asList(fileNames), issues.keySet());
  }

  private String randomFileName(char prefix) throws IOException {
    File result = new File(dir, prefix + RANDOM.string() + ".radl");
    PrintWriter writer = new PrintWriter(result, "UTF8");
    try {
      writer.println();
    } finally {
      writer.close();
    }
    return result.getAbsolutePath();
  }

  @Test(expected = IllegalArgumentException.class)
  public void throwsExceptionOnError() {
    Map<String, Collection<Issue>> issues = new HashMap<String, Collection<Issue>>();
    issues.put(RANDOM.string(), Arrays.asList(new Issue(Validator.class, Level.ERROR, RANDOM.integer(),
        RANDOM.integer(), RANDOM.string())));
    radlValidator.reportIssues(issues, new PrintStreamIssueReporter(System.err));
  }

  @Test
  public void printsWarningsAndInfos() throws UnsupportedEncodingException {
    String fileName = RANDOM.string(8) + ".radl";
    String warning = RANDOM.string();
    String info = RANDOM.string();
    int line1 = RANDOM.integer();
    int column1 = RANDOM.integer();
    int line2 = RANDOM.integer();
    int column2 = RANDOM.integer();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    String encoding = "UTF8";
    PrintStream out = new PrintStream(stream, true, encoding);
    try {
      List<Issue> issuesByFile = Arrays.asList(new Issue(Validator.class, Level.WARNING, line1, column1, warning),
          new Issue(Validator.class, Level.INFO, line2, column2, info));
      Map<String, Collection<Issue>> issues = new HashMap<String, Collection<Issue>>();
      issues.put(fileName, issuesByFile);
      radlValidator.reportIssues(issues, new PrintStreamIssueReporter(out));
    } finally {
      out.close();
    }

    assertEquals("Printed issues", String.format("File: %s; %s [%d,%d]: %s%nFile: %s; %s [%d,%d]: %s%n",
        fileName, Level.WARNING, line1, column1, warning, fileName, Level.INFO, line2, column2, info),
        new String(stream.toByteArray(), "UTF8"));
  }

  @Test
  public void reportsIssues() throws Exception {
    String fileName = RANDOM.string(8) + ".radl";
    String warning = RANDOM.string();
    String info = RANDOM.string();
    Issue issue2 = new Issue(Validator.class, Level.INFO, RANDOM.integer(), RANDOM.integer(), info);
    Issue issue1 = new Issue(Validator.class, Level.WARNING, RANDOM.integer(), RANDOM.integer(), warning);
    Map<String, Collection<Issue>> issues = new HashMap<String, Collection<Issue>>();
    issues.put(fileName, Arrays.asList(issue1, issue2));
    IssueReporter reporter = mock(IssueReporter.class);

    radlValidator.reportIssues(issues, reporter);

    verify(reporter).start();
    verify(reporter).file(fileName);
    verify(reporter).issue(issue1);
    verify(reporter).issue(issue2);
    verify(reporter).end();
  }

  @Test
  public void writesIssuesInProvidedLocation() {
    File issuesFile = someIssuesFile();
    String radlFileName = RANDOM.string() + ".radl";

    radlValidator.run(new Arguments(new String[] { issuesFile.getPath(), radlFileName }));

    assertTrue("Issues file not found", issuesFile.exists());
  }

  private File someIssuesFile() {
    return new File(dir, RANDOM.string() + ".xml");
  }

  @Test
  public void returnsNonZeroExitCodeOnFailure() {
    String radlFileName = RANDOM.string() + ".radl";

    int exitCode = radlValidator.run(new Arguments(new String[] { someIssuesFile().getPath(), radlFileName }));

    assertNotEquals("Exit code", 0, exitCode);
  }

  @Test
  public void returnsZeroExitCodeOnSuccess() {
    int exitCode = radlValidator.run(new Arguments(new String[] { someIssuesFile().getPath() }));

    assertEquals("Exit code", 0, exitCode);
  }

}
