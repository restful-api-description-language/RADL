/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import radl.common.io.IO;
import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.validation.Issue.Level;
import radl.test.RandomData;
import radl.test.TestUtil;


public class CheckStyleIssueReporterTest {

  private static final RandomData RANDOM = new RandomData();
  private static final String OUTPUT_FILE = "build/checkstyle/radl-issues.xml";

  private final IssueReporter reporter = new CheckStyleIssueReporter();

  @Before
  public void init() {
    reporter.setReportFileName(OUTPUT_FILE);
  }

  @Test
  public void writesCheckStyleCompatibleOutput() {
    String fileName = RANDOM.string(8) + ".radl";
    Level level = RANDOM.logical() ? Level.WARNING : Level.ERROR;
    int line = RANDOM.integer();
    int column = RANDOM.integer();
    String message = RANDOM.string() + '"' + RANDOM.string(3) + '"' + RANDOM.string();

    reporter.start();
    reporter.file(fileName);
    reporter.issue(new Issue(Validator.class, level, line, column, message));
    reporter.end();

    File checkStyleFile = new File(OUTPUT_FILE);
    assertTrue("Missing CheckStyle file", checkStyleFile.exists());

    try {
      TestUtil.assertXmlEquals("CheckStyle output", DocumentBuilder.newDocument()
          .element("checkstyle")
              .attribute("version", "5.6")
              .element("file")
                  .attribute("name", fileName)
                  .element("error")
                      .attribute("line", Integer.toString(line))
                      .attribute("column", Integer.toString(column))
                      .attribute("source", Validator.class.getSimpleName())
                      .attribute("severity", level.toString().toLowerCase(Locale.getDefault()))
                      .attribute("message", message)
          .build(), Xml.parse(checkStyleFile));
    } finally {
      IO.delete(checkStyleFile);
    }
  }

  @Test
  public void identifiedAsCheckstyle() throws Exception {
    assertEquals("ID", "checkstyle", reporter.getId());
  }

}
