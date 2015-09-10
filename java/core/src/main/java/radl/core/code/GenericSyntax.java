/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.Arrays;
import java.util.Collection;


/**
 * Default code syntax. Strings are surrounded by either single or double quotes.
 */
public class GenericSyntax implements Syntax {

  private static final Collection<Character> QUOTES = Arrays.asList('"', '\'');

  @Override
  public boolean isStringStart(char c) {
    return QUOTES.contains(c);
  }

  @Override
  public boolean canSplitCommentOn(char c) {
    return !Character.isLetter(c) && c != '\'';
  }

  @Override
  public boolean canSplitOn(char c, boolean commentIsEmpty) {
    return Character.isWhitespace(c) && commentIsEmpty;
  }

  @Override
  public boolean startsMultiLineComment(String line) {
    return false;
  }

  @Override
  public boolean endsMultiLineComment(String line) {
    return false;
  }

}
