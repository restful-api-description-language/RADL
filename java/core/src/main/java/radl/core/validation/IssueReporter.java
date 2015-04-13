/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;


/**
 * Report validation issues.
 */
public interface IssueReporter {

  /**
   * Start the report.
   */
  void start();

  /**
   * Start a new section of the report for the given file.
   * @param fileName The name of the RADL file for which issues will be reported
   */
  void file(String fileName);

  /**
   * Report an issue.
   * @param issue The issue to report
   */
  void issue(Issue issue);

  /**
   * End the report.
   */
  void end();

}
