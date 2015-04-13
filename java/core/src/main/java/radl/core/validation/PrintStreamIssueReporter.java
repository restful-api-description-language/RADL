/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.PrintStream;

import radl.core.validation.Issue.Level;


/*
 * Print validation issues to a stream.
 */
public class PrintStreamIssueReporter implements IssueReporter {

  private static final String NL = System.getProperty("line.separator");

  private final PrintStream stream;
  private StringBuilder errors; // NOPMD AvoidStringBufferField
  private String currentFile;

  public PrintStreamIssueReporter(PrintStream stream) {
    this.stream = stream;
  }

  @Override
  public void start() {
    errors = new StringBuilder();
  }

  @Override
  public void file(String file) {
    currentFile = file;
  }

  @Override
  public void issue(Issue issue) {
    if (issue.getLevel() == Level.ERROR) {
      errors.append("File: ").append(currentFile).append("; ").append(issue).append(NL);
    } else {
      stream.print("File: ");
      stream.print(currentFile);
      stream.print("; ");
      stream.println(issue);
    }
  }

  @Override
  public void end() {
    if (errors.length() > 0) {
      throw new IllegalArgumentException(errors.toString());
    }
  }

}
