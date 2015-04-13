/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.test;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;
import org.w3c.dom.Document;

import radl.common.xml.Xml;


/**
 * Utility methods for use in tests.
 */
public final class TestUtil {

  private static final RandomData RANDOM = new RandomData();

  private TestUtil() {
    // Utility class
  }

  public static <T> void assertCollectionEquals(String message, Iterable<T> expected, Iterable<T> actual) {
    Iterator<T> expectedItems = expected.iterator();
    Iterator<T> actualItems = actual.iterator();
    while (expectedItems.hasNext()) {
      T expectedItem = expectedItems.next();
      assertTrue(message + ": missing item '" + expectedItem + "', got " + actual, actualItems.hasNext());

      T actualItem = actualItems.next();
      if (!expectedItem.equals(actualItem)) {
        Assert.assertEquals(message + ": incorrect item", expectedItem.toString(), actualItem.toString());
      }
    }
    if (actualItems.hasNext()) {
      Collection<T> extraItems = new ArrayList<T>();
      while (actualItems.hasNext()) {
        extraItems.add(actualItems.next());
      }
      Assert.assertEquals("Extra item(s)", "", extraItems.toString());
    }
  }

  public static void assertXmlEquals(String message, Document expected, Document actual) {
    Assert.assertEquals(message, Xml.toString(expected), Xml.toString(actual));
  }

  public static String initCap(String text) {
    return Character.toUpperCase(text.charAt(0)) + text.substring(1);
  }

  public static File randomDir(Class<?> caller) {
    String name = caller.getSimpleName();
    if (name.endsWith("Test")) {
      name = name.substring(0, name.length() - 4);
    }
    File result = new File("build/" + name);
    result.mkdirs();
    return result;
  }

  public static File randomFile(Class<?> caller, String extension) {
    return new File(randomDir(caller), RANDOM.string(8) + extension);
  }

}
