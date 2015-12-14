/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;

import radl.test.RandomData;


public class CodeTest {

  private static final RandomData RANDOM = new RandomData();

  private Code code = new Code();

  @Test
  public void addsLines() {
    String line1 = RANDOM.string();
    String line2 = RANDOM.string();

    code.add(line1);
    code.add(line2);

    assertEquals("Lines", Arrays.asList(line1, line2), code);
  }

  @Test
  public void addsFormattedLines() {
    String arg1 = RANDOM.string();
    int arg2 = RANDOM.integer();
    String format = "%s_%d";

    code.add(format, arg1, arg2);

    assertEquals("Lines", Arrays.asList(String.format(format, arg1, arg2)), code);
  }

  @Test
  public void combinesLinesToText() {
    String line1 = RANDOM.string();
    String line2 = RANDOM.string();

    code.add(line1);
    code.add(line2);

    assertEquals("Text", String.format("%s\n%s\n", line1, line2), code.text());
  }

  @Test
  public void isEqualWhenSameText() {
    String line1 = RANDOM.string();
    String line2 = RANDOM.string();
    code.add(line1);
    code.add(line2);

    Code other = new Code();
    for (String line : code) {
      other.add(line);
    }

    assertEquals("Equals when same text", code, other);
  }

  @Test
  public void returnsTextForToString() throws Exception {
    String line1 = RANDOM.string();
    String line2 = RANDOM.string();
    code.add(line1);
    code.add(line2);


    assertEquals("toString()", code.text(), code.toString());
  }

  @Test
  public void wrapsLinesAtMaximumWidth() {
    int len = RANDOM.integer(10, 20);
    code = new Code(len, new GenericSyntax());
    String line1 = RANDOM.string(len);
    String line2 = RANDOM.string(len);
    Collection<String> expected = Arrays.asList(line1, "    " + line2);

    code.add(line1 + ' ' + line2);

    Collection<String> actual = new ArrayList<>();
    for (String line : code) {
      actual.add(line);
    }
    assertEquals("Lines", expected.toString(), actual.toString());
  }

}
