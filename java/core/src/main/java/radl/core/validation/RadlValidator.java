/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import radl.common.io.IO;
import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;
import radl.core.validation.Issue.Level;
import radl.core.xml.RadlFileAssembler;

/**
 * {@linkplain Application} that validates RADL documents and reports issues.
 */
public final class RadlValidator implements Application {

  private static final String DEFAULT_ISSUE_REPORTER = CheckStyleIssueReporter.ID;
  private static final String DEFAULT_REPORT_FILE_NAME = "build/radl-issues.xml";

  public static void main(String[] args) {
    Cli.run(RadlValidator.class, args);
  }

  /**
   * The following arguments are supported.<ul>
   * <li>[Optional] The output report file name. Default is "build/radl-issues.xml"</li>
   * <li>[Optional] The output report format. Default is "checkstyle"</li>
   * <li> [Required] The file names of one or more RADL documents to validate</li>
   * </ul>
   */
  @Override
  public int run(Arguments arguments) {
    Map<String, Collection<Issue>> issues = new TreeMap<String, Collection<Issue>>();
    String reportFileName = arguments.hasNext() ? arguments.next() : DEFAULT_REPORT_FILE_NAME;
    String issueReporterId = DEFAULT_ISSUE_REPORTER;
    if (reportFileName.endsWith(".radl")) {
      reportFileName = DEFAULT_REPORT_FILE_NAME;
      arguments.prev();
    } else if (arguments.hasNext()) {
      issueReporterId = arguments.next();
      if (issueReporterId.endsWith(".radl")) {
        issueReporterId = DEFAULT_ISSUE_REPORTER;
        arguments.prev();
      }
    }
    File reportDir = new File(reportFileName).getParentFile();
    validate(arguments, issues, reportDir);
    IssueReporter reporter = IssueReporterFactory.newInstance(issueReporterId);
    reporter.setReportFileName(reportFileName);
    reportIssues(issues, reporter);
    return numErrors(issues);
  }

  public void validate(Arguments arguments, Map<String, Collection<Issue>> issues, File reportDir) {
    validate(arguments, newValidator(), issues, reportDir);
  }

  Validator newValidator() {
    return new CompositeValidator(new RelaxNgValidator(), new LintValidator());
  }

  void validate(Arguments arguments, Validator validator, Map<String, Collection<Issue>> issues, File reportDir) {
    while (arguments.hasNext()) {
      File radlFile = arguments.file();
      File assembledRadl = RadlFileAssembler.assemble(radlFile, reportDir);
      try {
        Log.info("-> Validating " + radlFile.getName() + " using " + validator);
        validate(assembledRadl, validator, issues);
      } finally {
        IO.delete(assembledRadl);
      }
    }
  }

  private void validate(File radl, Validator validator, Map<String, Collection<Issue>> issues) {
    Collection<Issue> issuesByFile = new ArrayList<Issue>();
    issues.put(radl.getName(), issuesByFile);
    try (InputStream stream = new FileInputStream(radl)) {
      validator.validate(stream, issuesByFile);
    } catch (IOException e) {
      issuesByFile.add(new Issue(Validator.class, Level.ERROR, 0, 0, e.toString()));
    }
  }

  void reportIssues(Map<String, Collection<Issue>> issues, IssueReporter reporter) {
    reporter.start();
    for (Entry<String, Collection<Issue>> entry : issues.entrySet()) {
      reporter.file(entry.getKey());
      for (Issue issue : entry.getValue()) {
        reporter.issue(issue);
      }
    }
    reporter.end();
  }

  private int numErrors(Map<String, Collection<Issue>> issues) {
    int result = 0;
    for (Collection<Issue> issuesByFile : issues.values()) {
      for (Issue issue : issuesByFile) {
        if (issue.getLevel() == Level.ERROR) {
          result++;
        }
      }
    }
    return result;
  }

}
