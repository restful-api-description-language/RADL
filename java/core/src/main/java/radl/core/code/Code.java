/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import radl.common.io.IO;


/**
 * Generic piece of code.
 */
public class Code extends ArrayList<String> {

  private static final int DEFAULT_LINE_LENGTH = 120;

  private final transient int lineLength;
  private final transient Syntax syntax;

  /**
   * Create a piece of code with generic syntax processing.
   */
  public Code() {
    this(new GenericSyntax());
  }

  /**
   * Create a piece of code with a given syntax.
   * @param syntax The syntax to use for processing the code
   */
  public Code(Syntax syntax) {
    this(DEFAULT_LINE_LENGTH, syntax);
  }

  /**
   * Create a piece of code with a given syntax.
   * @param syntax The syntax to use for processing the code
   * @param len The maximum preferred line length. Lines longer than this will be wrapped, if possible
   */
  public Code(int len, Syntax syntax) {
    lineLength = len;
    this.syntax = syntax;
  }

  /**
   * Add the given contents to the code.
   * @param contents The contents to add
   */
  public void add(InputStream contents) {
    for (String line : IO.linesOf(contents)) {
      add(line);
    }
  }

  /**
   * Add a line of code.
   * @param format The format of the line
   * @param args The arguments to the format
   */
  public void add(String format, Object... args) {
    add(String.format(format, args));
  }

  @Override
  public boolean add(String text) {
    boolean result = false;
    for (String line : new Lines(lineLength, syntax).split(text)) {
      result = super.add(line);
    }
    return result;
  }

  /**
   * @return All the lines of code, separated by newlines
   */
  public String text() {
    StringBuilder result = new StringBuilder();
    for (String line : this) {
      result.append(line).append('\n');
    }
    return result.toString();
  }

  @Override
  public String toString() {
    return text();
  }

  public void writeTo(File file) {
    try {
      PrintWriter writer = new PrintWriter(file, "UTF8");
      try {
        writer.println(text());
      } finally {
        writer.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
