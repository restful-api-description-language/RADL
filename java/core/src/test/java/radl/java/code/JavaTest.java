/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import radl.common.io.IO;


public class JavaTest {

  private static final File TMP_DIR = new File("build/tmp");

  @Test
  public void findsJdk() throws IOException {
    assertJdk("foo");
    assertJdk("bar\\");
    assertJdk("baz/");
    assertJdk("jre");
  }

  private void assertJdk(String dir) throws IOException {
    String path = simulateJdkIn(dir);
    try {
      String saveJavaHome = System.getProperty(Java.JAVA_HOME);
      try {
        System.setProperty(Java.JAVA_HOME, path);
        Java.ensureCorrectJavaHome();

        assertEquals("JDK in '" + dir + "'", path, System.getProperty(Java.JAVA_HOME));
      } finally {
        System.setProperty(Java.JAVA_HOME, saveJavaHome);
      }
    } finally {
      IO.delete(TMP_DIR);
    }
  }

  private String simulateJdkIn(String path) throws IOException {
    File result = new File(TMP_DIR, path);
    File dir = new File(result, "lib");
    if (!dir.mkdirs()) {
      throw new IllegalStateException("Couldn't create directory: " + dir.getPath());
    }
    PrintWriter writer = new PrintWriter(new File(dir, "tools.jar"), "UTF8");
    try {
      writer.println();
    } finally {
      writer.close();
    }
    return result.getAbsolutePath();
  }

  @Test
  public void toJavaString() {
    assertJavaString("ape", "ape");
    assertJavaString("bear\ncheetah", "bear\\ncheetah");
    assertJavaString("dingo\"elephant", "dingo\\\"elephant");
  }

  private void assertJavaString(String input, String expected) {
    assertEquals("toString(" + input + ")", expected, Java.toString(input));
  }
  
}
