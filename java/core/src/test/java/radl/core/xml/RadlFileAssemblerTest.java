/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.core.xml;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.InputSource;

import radl.common.io.IO;


public class RadlFileAssemblerTest {

  private File assembled;

  @Before
  public void init() {
    assembled = null;
  }

  @After
  public void done() {
    if (assembled != null) {
      IO.delete(assembled);
    }
  }

  @Test
  public void testAssembleCompleteRadlFile() throws Exception {
    URL completeFilePath = this.getClass().getResource("sample-complete.radl");
    File completeFile = new File(completeFilePath.getFile());

    assembled = RadlFileAssembler.assemble(completeFile, null);

    assertXmlEquals(completeFile, assembled);
  }

  @Test
  public void testAssembleRadlFilesWithXinclude() throws Exception {
    URL completeFilePath = this.getClass().getResource("sample-complete.radl");
    File completeFile = new File(completeFilePath.getFile());
    URL part1Path = this.getClass().getResource("sample-part1.radl");
    File part1 = new File(part1Path.getFile());

    assembled = RadlFileAssembler.assemble(part1, null);

    assertXmlEquals(completeFile, assembled);
  }

  @Test
  public void testAssembleRadlFilesWithXincludeAndLocation() throws Exception {
    URL completeFilePath = this.getClass().getResource("sample-complete.radl");
    File completeFile = new File(completeFilePath.getFile());
    URL part1Path = this.getClass().getResource("sample-part1.radl");
    File part1 = new File(part1Path.getFile());
    File dir = new File("build/test-data/" + System.nanoTime());
    assertFalse("Directory does not exist:" + dir, dir.exists());

    assembled = RadlFileAssembler.assemble(part1, dir);
    assertXmlEquals(completeFile, assembled);
    assertTrue("Directory is created:" + dir, dir.exists());
  }

  private void assertXmlEquals(File file1, File file2) throws Exception {
    XMLUnit.setIgnoreWhitespace(true);
    try (FileInputStream ins1 = new FileInputStream(file1)) {
      try (FileInputStream ins2 = new FileInputStream(file2)) {
        XMLAssert.assertXMLEqual("XML Compare", new InputSource(ins1), new InputSource(ins2));
      }
    }
  }

}
