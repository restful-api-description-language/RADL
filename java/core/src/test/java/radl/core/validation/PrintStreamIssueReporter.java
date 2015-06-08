/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import radl.core.validation.Issue.Level;


/*
 * Print validation issues to a stream.
 */
public class PrintStreamIssueReporter implements IssueReporter {

  private static final String NL = System.getProperty("line.separator");

  private PrintStream stream;
  private StringBuilder errors; // NOPMD AvoidStringBufferField
  private String currentFile;

  public PrintStreamIssueReporter() {
    this(System.out);
  }

  public PrintStreamIssueReporter(PrintStream stream) {
    this.stream = stream;
  }

  @Override
  public void setReportFileName(String reportFileName) {
    try {
      this.stream = new PrintStream(reportFileName);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
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

  @Override
  public String getId() {
    return "stdout";
  }

}
