/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import radl.common.io.IO;
import radl.test.RandomData;
import radl.test.TestUtil;


public class GeneratedSourceFileTest {

  private static final RandomData RANDOM = new RandomData();

  private final File dir = TestUtil.randomDir(GeneratedSourceFileTest.class);
  private final String path = new File(dir, RANDOM.string()).getPath();
  private final String code = RANDOM.string();

  @Before
  public void init() throws FileNotFoundException {
    try (PrintWriter writer = new PrintWriter(path, "UTF8")) {
      writer.println(RANDOM.string());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @After
  public void done() {
    IO.delete(dir);
  }

  @Test
  public void isEqualWhenSamePathAndCode() {
    SourceFile sourceFile1 = new GeneratedSourceFile(path);
    sourceFile1.code().add(code);
    SourceFile sourceFile2 = new GeneratedSourceFile(path);
    sourceFile2.code().add(code);
    SourceFile sourceFile3 = new GeneratedSourceFile(RANDOM.string());
    sourceFile3.code().add(code);
    SourceFile sourceFile4 = new GeneratedSourceFile(path);
    sourceFile4.code().add(RANDOM.string());

    assertEquals("Equal when same path and code", true, sourceFile1, sourceFile2); // NOPMD UseAssertTrueInsteadOfAssertEquals
    assertEquals("Equal when different path", false, sourceFile1, sourceFile3); // NOPMD UseAssertTrueInsteadOfAssertEquals
    assertEquals("Equal when different code", false, sourceFile1, sourceFile4); // NOPMD UseAssertTrueInsteadOfAssertEquals
  }

  private void assertEquals(String message, boolean expected, SourceFile sourceFile1, SourceFile sourceFile2) {
    Assert.assertEquals(message + " equals", expected, sourceFile1.equals(sourceFile2)); // NOPMD UseAssertTrueInsteadOfAssertEquals
    Assert.assertEquals(message + " equals reverse", expected, sourceFile2.equals(sourceFile1)); // NOPMD UseAssertTrueInsteadOfAssertEquals
    Assert.assertEquals(message + " hashCode", expected, sourceFile1.hashCode() == sourceFile2.hashCode()); // NOPMD UseAssertTrueInsteadOfAssertEquals
  }

}
