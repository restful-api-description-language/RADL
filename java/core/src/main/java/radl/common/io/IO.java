/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;


/**
 * Utility functions for working with files and streams.
 */
public final class IO {

  public static final int BUFFER_SIZE = 4096;

  private IO() {
    // Utility class
  }

  public static void delete(File root) {
    if (root == null || !root.exists()) {
      return;
    }
    if (root.isDirectory()) {
      for (File file : root.listFiles()) {
        delete(file);
      }
    }
    if (!root.delete()) {
      root.deleteOnExit();
    }
  }

  public static void copy(InputStream source, OutputStream destination) throws IOException {
    byte[] buffer = new byte[BUFFER_SIZE];
    int numRead = source.read(buffer);
    while (numRead > 0) {
      destination.write(buffer, 0, numRead);
      numRead = source.read(buffer);
    }
    source.close();
    destination.close();
  }

  public static Iterable<String> linesOf(InputStream stream) {
    Collection<String> result = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF8"))) {
      String line = reader.readLine();
      while (line != null) {
        result.add(line);
        line = reader.readLine();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

}
