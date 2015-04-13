/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;


/**
 * Report validation issues in CheckStyle format, for easy integration into Continuous Integration builds.
 */
public class CheckStyleIssueReporter implements IssueReporter {

  private String currentFileName;
  private final String outputFileName;
  private final DocumentBuilder output = DocumentBuilder.newDocument()
      .element("checkstyle")
          .attribute("version", "5.6");

  public CheckStyleIssueReporter(String outputFileName) {
    this.outputFileName = outputFileName;
  }

  @Override
  public void start() {
    currentFileName = null;
  }

  @Override
  public void file(String fileName) {
    if (currentFileName != null) {
      output.end();
    }
    currentFileName = fileName;
    output.element("file")
        .attribute("name", fileName);
  }

  @Override
  public void issue(Issue issue) {
    output.element("error")
        .attribute("line", Integer.toString(issue.getLine()))
        .attribute("column", Integer.toString(issue.getColumn()))
        .attribute("source", issue.getSource())
        .attribute("severity", issue.getLevel().toString().toLowerCase(Locale.getDefault()))
        .attribute("message", encodeXml(issue.getMessage()))
    .end();
  }

  private String encodeXml(String value) {
    return value.replace("&", "&amp;").replace("\"", "&quot;").replace("'", "&apos;").replace("<", "&lt;")
        .replace(">", "&gt;");
  }

  @Override
  public void end() {
    File outputFile = new File(outputFileName);
    if (outputFile.getParentFile() != null) {
      outputFile.getParentFile().mkdirs();
    }
    try {
      PrintWriter writer = new PrintWriter(outputFile, "UTF8");
      try {
        writer.print(Xml.toString(output.build()));
      } finally {
        writer.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
