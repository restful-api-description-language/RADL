/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.lang.model.SourceVersion;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.apache.commons.lang.StringUtils;


/**
 * Support for working with the Java programming language.
 */
public final class Java {

  static final String JAVA_HOME = "java.home";

  private static final Logger LOGGER = Logger.getLogger(Java.class.getName());

  private static JavaCompiler javaCompiler;
  private static boolean lookedForJavaCompiler;

  private Java() {
    // Utility class
  }

  /**
   * @return The Java compiler (requires a JDK)
   */
  public static JavaCompiler getCompiler() {
    if (!lookedForJavaCompiler) {
      lookedForJavaCompiler = true;
      javaCompiler = lookForCompiler();
    }
    return javaCompiler;
  }

  private static JavaCompiler lookForCompiler() {
    JavaCompiler result;
    String saveJavaHome = System.getProperty(JAVA_HOME);
    try {
      ensureCorrectJavaHome();
      result = ToolProvider.getSystemJavaCompiler();
    } finally {
      System.setProperty(JAVA_HOME, saveJavaHome);
    }
    if (result == null) {
      throw new IllegalStateException("Cannot find Java compiler, please use a JDK as JAVA_HOME\nLooked in: "
          + getCandidateDirs());
    }
    return result;
  }

  static void ensureCorrectJavaHome() {
    String jdkDirectory = getJdkDirectory();
    if (jdkDirectory == null) {
      LOGGER.severe("Could not find JDK");
    } else {
      System.setProperty(JAVA_HOME, jdkDirectory);
    }
  }

  private static String getJdkDirectory() {
    for (String dir : getCandidateDirs()) {
      String result = checkJdk(dir);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private static Iterable<String> getCandidateDirs() {
    Collection<String> result = new ArrayList<String>();
    String dir = System.getProperty(JAVA_HOME);
    result.add(dir);
    if (dir.endsWith("jre")) {
      result.add(dir.substring(0, dir.length() - 3));
    }
    result.add(System.getenv("JAVA_HOME"));
    return result;
  }

  private static String checkJdk(String candidateJdkDir) {
    if (StringUtils.isEmpty(candidateJdkDir)) {
      return null;
    }
    File dir = new File(candidateJdkDir);
    if (isJdkDir(dir)) {
      return candidateJdkDir;
    }
    dir = new File(dir, "bin");
    if (isJdkDir(dir)) {
      return dir.getAbsolutePath();
    }
    return null;
  }

  private static boolean isJdkDir(File dir) {
    try {
      LOGGER.fine(String.format("Checking directory %s for lib/tools.jar", dir.getCanonicalPath()));
    } catch (IOException e) {
      LOGGER.log(Level.SEVERE, e.getMessage(), e);
    }
    if (!dir.exists()) {
      return false;
    }
    File[] jdkFile = dir.listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File parent, String name) {
        return "lib".equals(name) && new File(parent, name).listFiles(new FilenameFilter() {
          @Override
          public boolean accept(File ignored, String file) {
            return "tools.jar".equals(file);
          }
        }).length > 0;
      }
    });
    return jdkFile.length > 0;
  }

  /**
   * Convert a given value to something that is acceptable as a Java identifier.
   * @param value The value to convert
   * @return The converted Java identifier
   */
  public static String toIdentifier(String value) {
    StringBuilder result = new StringBuilder(value);
    while (!Character.isJavaIdentifierStart(result.charAt(0))) {
      result.delete(0, 1);
    }
    upcase(result, 0);
    int index = 1;
    while (index < result.length()) {
      if (!Character.isJavaIdentifierPart(result.charAt(index))) {
        while (index < result.length() && !Character.isJavaIdentifierPart(result.charAt(index))) {
          result.delete(index, index + 1);
        }
        if (index < result.length()) {
          upcase(result, index);
        }
      }
      index++;
    }
    return result.toString();
  }

  private static void upcase(StringBuilder builder, int index) {
    builder.setCharAt(index, Character.toUpperCase(builder.charAt(index)));
  }

  /**
   * Convert a given Java package to a directory on the file system in which the package should be stored.
   * @param packageName The package to convert
   * @return The directory path in which to store the package
   */
  public static String packageToDir(String packageName) {
    StringBuilder result = new StringBuilder();
    for (String part : packageName.split("\\.")) {
      result.append(part).append(File.separator);
    }
    return result.toString();
  }

  public static String getVersion() {
    Iterator<SourceVersion> versions = getCompiler().getSourceVersions().iterator();
    SourceVersion result = versions.next();
    while (versions.hasNext()) {
      SourceVersion version = versions.next();
      if (version.compareTo(result) > 0) {
        result = version;
      }
    }
    return "1." + result.toString().substring("RELEASE_".length());
  }

}
