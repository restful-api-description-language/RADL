/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.File;

import org.junit.Test;

import radl.test.RandomData;


public class ArgumentsTest {

  private static final RandomData RANDOM = new RandomData();

  @Test
  public void extractsExistingArgumentsInOrder() {
    String arg1 = RANDOM.string();
    String arg2 = RANDOM.string();

    Arguments arguments = new Arguments(new String[] { arg1, arg2 });

    assertEquals("#1", arg1, arguments.next());
    assertEquals("#2", arg2, arguments.next());
    assertFalse("Has #3", arguments.hasNext());
  }

  @Test
  public void returnsDefaultValueForMissingArgument() {
    String arg = RANDOM.string();
    String defaultValue = RANDOM.string();

    Arguments arguments = new Arguments(new String[] { arg });

    assertEquals("Existing", arg, arguments.next(defaultValue));
    assertEquals("Default", defaultValue, arguments.next(defaultValue));
  }

  @Test
  public void convertsToFile() {
    String fileName = RANDOM.string();
    String defaultValue = RANDOM.string();

    Arguments arguments = new Arguments(new String[] { fileName });

    assertEquals("Existing", new File(fileName), arguments.file());
    assertEquals("Default", new File(defaultValue), arguments.file(defaultValue));
  }

}
