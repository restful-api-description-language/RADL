/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import radl.common.io.IO;
import radl.test.RandomData;


public class IOTest {

  private static final RandomData RANDOM = new RandomData();

  private final File dir = new File("build/" + RANDOM.string());

  @Before
  public void init() {
    dir.mkdirs();
  }

  @After
  public void done() {
    deleteAll(dir);
  }

  private void deleteAll(File root) {
    if (!root.exists()) {
      return;
    }
    if (root.isDirectory()) {
      for (File file : root.listFiles()) {
        deleteAll(file);
      }
    }
    if (!root.delete()) {
      root.deleteOnExit();
    }
  }

  @Test
  public void copiesStreams() throws IOException {
    final String content = RANDOM.string();
    String charSet = "UTF8";
    try (ByteArrayOutputStream destination = new ByteArrayOutputStream()) {
      try (InputStream source = new ByteArrayInputStream(content.getBytes(charSet))) {
        IO.copy(source, destination);
        assertEquals("Content", content, new String(destination.toByteArray(), charSet));
      }
    }
  }

  @Test
  public void recursivelyDeletesDirectories() {
    File parent = new File(dir, "root");
    File child = new File(parent, RANDOM.string(8));
    assertTrue("Failed to create directory " + child.getPath(), child.mkdirs());
    assertTrue("Sanity check: root", parent.exists());
    assertTrue("Sanity check: dir", child.exists());

    IO.delete(parent);
    assertFalse("After delete: dir", child.exists());
    assertFalse("After delete: root", parent.exists());
  }

  @Test
  public void readsLinesFromStreams() throws IOException {
    String line1 = RANDOM.string();
    String line2 = RANDOM.string();
    File file = new File(dir, RANDOM.string(8));
    try (PrintWriter writer = new PrintWriter(file, "UTF8")) {
      writer.println(line1);
      writer.println(line2);
    }

    try (InputStream stream = new FileInputStream(file)) {
      Iterable<String> lines = IO.linesOf(stream);
      assertEquals("Lines", Arrays.asList(line1, line2), lines);
    }
  }

}
