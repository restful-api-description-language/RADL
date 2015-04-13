/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;


/**
 * Syntax for code in some language.
 */
public interface Syntax {

  boolean isStringStart(char c);

  boolean canSplitCommentOn(char c);

  boolean canSplitOn(char c, boolean commentIsEmpty);

}
