/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;


/**
 * A source file that was generated by some process rather than hand-coded. Two generated source files are considered
 * equal if they are stored at the same path and contain the same code.
 */
public class GeneratedSourceFile extends SourceFile {

  public GeneratedSourceFile(String path) {
    this(path, null);
  }

  public GeneratedSourceFile(String path, Code code) {
    super(path, code);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + code().hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (!super.equals(obj)) {
      return false;
    }
    return code().equals(((SourceFile)obj).code());
  }

}
