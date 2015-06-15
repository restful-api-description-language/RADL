/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import radl.java.code.JavaCode;


/**
 * A source file that contains code. Two source files are considered equal if they are stored at the same path.
 */
public class SourceFile {

  private final String path;
  private Code code;

  public SourceFile(String path) {
    this(path, null);
  }

  public SourceFile(String path, Code code) {
    this.path = path;
    this.code = code;
  }

  /**
   * @return The path where this source file is stored on the file system
   */
  public String path() {
    return path;
  }

  /**
   * @return The code stored in this source file. This is a factory method that may return specialized classes for
   * certain recognized types of code, like Java code
   */
  public Code code() {
    if (code == null) {
      code = newCode();
      try {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF8"));
        try {
          addText(reader);
        } finally {
          reader.close();
        }
      } catch (IOException e) { // NOPMD EmptyCatchBlock
        // Ignore
      }
    }
    return code;
  }

  private Code newCode() {
    if (path.endsWith(".radl")) {
      return new RadlCode();
    }
    if (path.endsWith(".xml")) {
      return new XmlCode();
    }
    if (path.endsWith(".java")) {
      return new JavaCode();
    }
    return new Code();
  }

  private void addText(BufferedReader reader) throws IOException {
    String line = reader.readLine();
    while (line != null) {
      code.add(line);
      line = reader.readLine();
    }
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + path.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SourceFile)) {
      return false;
    }
    SourceFile other = (SourceFile)obj;
    if (!path.equals(other.path)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return path;
  }

}
