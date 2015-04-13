/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;


/**
 * An issue found in a RADL document.
 */
public final class Issue {

  public enum Level { INFO, WARNING, ERROR }

  private final String source;
  private final Level level;
  private final int line;
  private final int column;
  private final String message;

  public Issue(Class<? extends Validator> source, Level level, int line, int column, String message) {
    if (source == null || level == null || message == null) {
      throw new IllegalArgumentException("Source, level, and message are required");
    }
    this.source = source.getSimpleName();
    this.level = level;
    this.line = line;
    this.column = column;
    this.message = message;
  }

  public String getSource() {
    return source;
  }

  public Level getLevel() {
    return level;
  }

  public int getLine() {
    return line;
  }

  public int getColumn() {
    return column;
  }

  public String getMessage() {
    return message;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + level.hashCode();
    result = prime * result + line;
    result = prime * result + column;
    result = prime * result + message.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Issue)) {
      return false;
    }
    Issue other = (Issue)obj;
    if (level != other.level) {
      return false;
    }
    if (line != other.line) {
      return false;
    }
    if (column != other.column) {
      return false;
    }
    if (!message.equals(other.message)) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return String.format("%s [%d,%d]: %s", getLevel(), getLine(), getColumn(), getMessage());
  }

}
