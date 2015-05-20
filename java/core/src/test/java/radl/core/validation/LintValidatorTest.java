/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Element;

import radl.common.xml.Xml;
import radl.core.validation.Issue.Level;


@RunWith(Parameterized.class)
public class LintValidatorTest {

  private static final File TESTS_DIR = new File("specification/tests/invalid");

  @Parameters(name = "{0}")
  public static Iterable<String[]> tests() {
    Collection<String[]> result = new ArrayList<String[]>();
    for (String dir : TESTS_DIR.list()) {
      result.add(new String[] { dir });
    }
    return result;
  }

  @Parameter
  public String dir;

  @Test
  public void testValidation() throws IOException {
    File testDir = new File(TESTS_DIR, dir);
    Issue expected = newIssue(new File(testDir, "issue.xml"));

    Collection<Issue> actual = validate(new File(testDir, "instance.xml"));

    assertTrue("\nMissing issue:\n- " + expected + "\nFound:\n" + toString(actual), actual.contains(expected));
  }

  private String toString(Collection<Issue> issues) {
    StringBuilder result = new StringBuilder();
    for (Issue issue : issues) {
      result.append("- ").append(issue).append('\n');
    }
    return result.toString();
  }

  private Collection<Issue> validate(File radlFile) throws IOException {
    Collection<Issue> result = new ArrayList<Issue>();
    Validator validator = new LintValidator();
    InputStream radl = new FileInputStream(radlFile);
    try {
      validator.validate(radl, result);
    } finally {
      radl.close();
    }
    return result;
  }

  private Issue newIssue(File issueFile) throws IOException {
    InputStream issue = new FileInputStream(issueFile);
    try {
      Element issueElement = Xml.parse(issue).getDocumentElement();
      return new Issue(LintValidator.class, getLevel(issueElement), getIntAttr(issueElement, "line"),
          getIntAttr(issueElement, "column"), issueElement.getTextContent());
    } finally {
      issue.close();
    }
  }

  private Level getLevel(Element issueElement) {
    return Level.valueOf(issueElement.getAttributeNS(null, "level"));
  }

  private int getIntAttr(Element element, String attr) {
    return Integer.parseInt(element.getAttributeNS(null, attr));
  }

}
