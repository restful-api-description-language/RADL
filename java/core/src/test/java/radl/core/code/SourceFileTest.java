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
import org.junit.Test;

import radl.common.io.IO;
import radl.core.code.radl.RadlCode;
import radl.core.code.xml.XmlCode;
import radl.test.RandomData;
import radl.test.TestUtil;


public class SourceFileTest {

  private static final RandomData RANDOM = new RandomData();

  private String path = TestUtil.randomDir(SourceFileTest.class).getPath();

  @After
  public void done() {
    IO.delete(new File(path));
  }

  @Test
  public void storesFilePath() {
    SourceFile sourceFile = new SourceFile(path);

    Assert.assertEquals("Path", path, sourceFile.path());
  }

  @Test
  public void isEqualWhenSameTypeAndPath() {
    SourceFile sourceFile1 = new SourceFile(path);
    SourceFile sourceFile2 = new SourceFile(path);
    SourceFile sourceFile3 = new SourceFile(RANDOM.string());
    Object other = new Object();

    assertEquals("Equal when same path", true, sourceFile1, sourceFile2); // NOPMD UseAssertTrueInsteadOfAssertEquals
    assertEquals("Equal when different paths", false, sourceFile1, sourceFile3); // NOPMD UseAssertTrueInsteadOfAssertEquals
    assertEquals("Equal when different type", false, sourceFile1, other); // NOPMD UseAssertTrueInsteadOfAssertEquals
  }

  private void assertEquals(String message, boolean expected, Object sourceFile1, Object sourceFile2) {
    Assert.assertEquals(message + ".equals", expected, sourceFile1.equals(sourceFile2)); // NOPMD UseAssertTrueInsteadOfAssertEquals
    Assert.assertEquals(message + ".equals reverse", expected, sourceFile2.equals(sourceFile1)); // NOPMD UseAssertTrueInsteadOfAssertEquals
    Assert.assertEquals(message + ".hashCode", expected, sourceFile1.hashCode() == sourceFile2.hashCode()); // NOPMD UseAssertTrueInsteadOfAssertEquals
  }

  @Test
  public void readsCodeFromFile() throws FileNotFoundException, UnsupportedEncodingException {
    String text = RANDOM.string();
    Code expected = new Code();
    expected.add(text);
    File file = new File(new File(path), RANDOM.string());
    try {
      try (PrintWriter writer = new PrintWriter(file, "UTF8")) {
        writer.println(text);
      }
      SourceFile sourceFile = new SourceFile(file.getPath());

      Code code = sourceFile.code();

      Assert.assertEquals("Code", expected, code);
    } finally {
      IO.delete(file);
    }
  }

  @Test
  public void readsEmptyCodeFromMissingFile() {
    File file = new File(path);
    SourceFile sourceFile = new SourceFile(file.getPath());

    Code code = sourceFile.code();

    Assert.assertEquals("Code", new Code(), code);
  }

  @Test
  public void readsSpecificCodeFromFile() throws FileNotFoundException, UnsupportedEncodingException {
    assertXmlCode("xml", XmlCode.class);
    assertXmlCode("radl", RadlCode.class);
  }

  protected void assertXmlCode(String extension, Class<? extends XmlCode> codeClass)
      throws FileNotFoundException, UnsupportedEncodingException {
    String root = RANDOM.string();
    path = String.format("%s%s%s.%s", path, File.separator, root, extension);
    File file = new File(path);
    file.getParentFile().mkdirs();
    try {
      try (PrintWriter writer = new PrintWriter(file, "UTF8")) {
        writer.println(String.format("<%s/>", root));
      }
      SourceFile sourceFile = new SourceFile(file.getPath());

      Code code = sourceFile.code();

      Assert.assertEquals("Code type for " + extension, codeClass, code.getClass());
      Assert.assertEquals("Document element", root, ((XmlCode)code).asDom().getDocumentElement().getTagName());
    } finally {
      IO.delete(file.getParentFile());
    }
  }

}
