/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import radl.test.RandomData;


public class LinesTest {

  private static final RandomData RANDOM = new RandomData();

  private int len;
  private Lines lines;
  private Syntax syntax = new GenericSyntax();

  @Before
  public void init() {
    init(RANDOM.integer(20, 30));
  }

  private void init(int lineLength) {
    len = lineLength;
    lines = new Lines(len, syntax);
  }

  @Test
  public void doesntSplitIdentifier() {
    String identifier = RANDOM.string(len + 2);
    assertLines(identifier, identifier);
  }

  private void assertLines(String input, String... expected) {
    Iterator<String> actual = lines.split(input).iterator();
    String indent = "";
    for (String line : expected) {
      assertTrue("Missing line: " + line, actual.hasNext());
      assertEquals("Line", indent + line, actual.next());
      indent = "    ";
    }
    assertFalse("Extra line", actual.hasNext());
  }

  @Test
  public void splitsAtWhitespaceBeforeLength() {
    String part1 = RANDOM.string(RANDOM.integer(2, len - 2));
    String part2 = RANDOM.string(len + 2 - part1.length());
    assertLines(part1 + ' ' + part2, part1, part2);
  }

  @Test
  public void doesntSplitStrings() {
    String part1 = "def foo =";
    String part2 = String.format("'%s %s'", RANDOM.string(len / 2), RANDOM.string(len));
    assertLines(part1 + ' ' + part2, part1, part2);
  }

  @Test
  public void doesntSplitShortLines() {
    String line = RANDOM.string(len - 1) + ';';
    assertLines(line, line);
  }

  @Test
  public void indentsWrappedLinesRelativeToCurrentIndentation() {
    String indent = "  ";
    String line1 = '1' + RANDOM.string(len);
    String line2 = '2' + RANDOM.string();
    assertLines(indent + line1 + ' ' + line2, indent + line1, indent + line2);
  }

  @Test
  public void prefersSplittingOnWhitespaceOverDot() {
    init(120);
    assertLines("@RequestMapping(method = RequestMethod.POST, consumes = { Api.MEDIA_XACML_XML_VERSION_2_0, Api"
        + ".MEDIA_XACML_XML_VERSION_3_0, Api.MEDIA_VND_XACML_JSON }, produces = { Api.MEDIA_XACML_XML_VERSION_2_0, Api"
        + ".MEDIA_XACML_XML_VERSION_3_0, Api.MEDIA_VND_XACML_JSON })",
        "@RequestMapping(method = RequestMethod.POST, consumes = { Api.MEDIA_XACML_XML_VERSION_2_0,",
        "Api.MEDIA_XACML_XML_VERSION_3_0, Api.MEDIA_VND_XACML_JSON }, produces = { Api.MEDIA_XACML_XML_VERSION_2_0,",
        "Api.MEDIA_XACML_XML_VERSION_3_0, Api.MEDIA_VND_XACML_JSON })");
  }

  @Test
  public void doesntTerminateStringsAtEscapedQuote() {
    init(35);
    String line1 = "String foo =";
    String line2 = "\"some string with \\\"quotes\\\" in it\"";
    assertLines(line1 + ' ' + line2, line1, line2);
  }

  @Test
  public void doesntTreatQuotesInCommentsAsStrings() {
    init(16);
    assertLines("String foo; // Qapla'!", "String foo;", "// Qapla'!");
  }

  @Test
  public void handlesLinesThatStartWithComment() {
    init(120);
    String indent = "          ";
    String line1 = indent + "//raise event for the modified trait first, discuss the need for this, as ideally trait "
        + "should not process";
    String line2 = "it's own events.";
    assertLines(line1 + ' ' + line2, line1, indent + "// " + line2);
  }

  @Test
  public void handlesLinesThatStartWithComment2() {
    init(120);
    String indent = "        // ";
    String line1 = indent + "when(mockRequest.getRequestURI()).thenReturn(\"http://localhost:8080/context/xxx/"
        + "com.company.app.foo.bar";
    String line2 = "/comments\");";
    assertLines(line1 + line2, line1, indent + line2);
  }

  @Test
  public void honorsNewLines() throws Exception {
    init(10);
    String line1 = RANDOM.string(RANDOM.integer(1, 4));
    String line2 = RANDOM.string(RANDOM.integer(1, 4));
    assertLines(line1 + '\n' + line2, line1, line2);
  }

  @Test
  public void keepsTooLongLineWhenNoGoodSplitPoint() {
    syntax = new XmlSyntax();
    init(5);
    String line = "\"" + RANDOM.string(7) + "\"/>";
    assertLines(line, line);
  }

}
