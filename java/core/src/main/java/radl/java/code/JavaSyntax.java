/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;

import radl.core.code.GenericSyntax;


/**
 * Syntax for the Java programming language.
 */
public class JavaSyntax extends GenericSyntax {

  @Override
  public boolean canSplitOn(char c, boolean commentIsEmpty) {
    return !Character.isJavaIdentifierPart(c) && commentIsEmpty;
  }

}
