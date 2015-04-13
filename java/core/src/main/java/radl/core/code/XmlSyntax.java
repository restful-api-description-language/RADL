/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;


/**
 * Syntax for XML code.
 */
public class XmlSyntax extends GenericSyntax {

  @Override
  public boolean canSplitOn(char c, boolean commentIsEmpty) {
    return super.canSplitOn(c, commentIsEmpty) && c != '>' && c != '/';
  }

}
