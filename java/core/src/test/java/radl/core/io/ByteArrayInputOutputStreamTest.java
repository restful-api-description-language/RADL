/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.junit.Assert;
import org.junit.Test;

import radl.common.io.ByteArrayInputOutputStream;
import radl.test.RandomData;


public class ByteArrayInputOutputStreamTest {

  private static final RandomData RANDOM = new RandomData();

  @Test
  public void readsWhatWasWritten() throws IOException {
    String line1 = RANDOM.string();
    String line2 = RANDOM.string();
    ByteArrayInputOutputStream stream = new ByteArrayInputOutputStream();
    try {
      PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, "UTF8"));
      try {
        writer.println(line1);
        writer.println(line2);
      } finally {
        writer.close();
      }
      BufferedReader reader = new BufferedReader(new InputStreamReader(stream.getInputStream(), "UTF8"));
      try {
        Assert.assertEquals("1st line", line1, reader.readLine());
        Assert.assertEquals("2nd line", line2, reader.readLine());
        Assert.assertNull("Extra line", reader.readLine());
      } finally {
        reader.close();
      }
    } finally {
      stream.close();
    }
  }

}
