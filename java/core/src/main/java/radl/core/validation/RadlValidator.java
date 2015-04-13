/*
 * Copyright © EMC Corporation. All rights reserved.
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

import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;
import radl.core.validation.Issue.Level;


/**
 * {@linkplain Application} that validates RADL documents and reports issues.
 */
public final class RadlValidator implements Application {

  private static final String DEFAULT_REPORT_FILE_NAME = "build/checkstyle/radl-issues.xml";

  public static void main(String[] args) {
    Cli.run(RadlValidator.class, args);
  }

  /**
   * The following arguments are supported.<ul>
   * <li>[Optional] The output report file name</li>
   * <li> [Required] The file names of RADL documents to validate</li>
   * </ul>
   */
  @Override
  public int run(Arguments arguments) {
    Map<String, Collection<Issue>> issues = new TreeMap<String, Collection<Issue>>();
    String reportFileName = arguments.hasNext() ? arguments.next() : DEFAULT_REPORT_FILE_NAME;
    if (reportFileName.endsWith(".radl")) {
      reportFileName = DEFAULT_REPORT_FILE_NAME;
      arguments.prev();
    }
    validate(arguments, issues);
    reportIssues(issues, new CheckStyleIssueReporter(reportFileName));
    return numErrors(issues);
  }

  public void validate(Arguments arguments, Map<String, Collection<Issue>> issues) {
    validate(arguments, newValidator(), issues);
  }

  Validator newValidator() {
    return new CompositeValidator(new RelaxNgValidator(), new LintValidator());
  }

  void validate(Arguments arguments, Validator validator, Map<String, Collection<Issue>> issues) {
    while (arguments.hasNext()) {
      File file = arguments.file();
      Log.info("-> Validating " + file.getName() + " using " + validator);
      Collection<Issue> issuesByFile = new ArrayList<Issue>();
      issues.put(file.getName(), issuesByFile);
      try {
        InputStream stream = new FileInputStream(file);
        try {
          validator.validate(stream, issuesByFile);
        } finally {
          stream.close();
        }
      } catch (IOException e) {
        issuesByFile.add(new Issue(Validator.class, Level.ERROR, 0, 0, e.toString()));
      }
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
