/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Lines of code. Code lines are split at some maximum line length and indented. Comments start with // and continue
 * until the end of the line.
 */
public class Lines {

  private static final String INDENT = "    ";
  private static final int MAX_DISTANCE_TO_PREFER_BETTER_SEPARATOR_CLASS = 5;
  private static final String COMMENT_START = "//";

  private final int maxLength;
  private final Syntax syntax;
  private boolean inMultiLineComment;

  public Lines(int length, Syntax syntax) {
    this.maxLength = length;
    this.syntax = syntax;
  }

  public Iterable<String> split(String text) {
    Collection<String> result = new ArrayList<>();
    String remainder = text;
    String indent = getIndent(remainder);
    String prefix = "";
    int index = splitPoint(remainder, indent.length());
    while (index > 0) {
      String line = rightTrim(remainder.substring(0, index));
      if (line.trim().endsWith(COMMENT_START)) {
        line = indent + line.substring(0, line.lastIndexOf(COMMENT_START)).trim();
        prefix = COMMENT_START + ' ';
      } else if (line.trim().startsWith(COMMENT_START)) {
        prefix = COMMENT_START + ' ';
      }
      result.add(line);
      remainder = indent + INDENT + prefix + remainder.substring(index).trim();
      index = splitPoint(remainder, INDENT.length() + indent.length());
    }
    result.add(remainder);
    return result;
  }

  private String rightTrim(String text) {
    return text.replaceAll("\\s+$", "");
  }

  private String getIndent(String text) {
    int result = 0;
    while (text.length() > result && Character.isWhitespace(text.charAt(result))) {
      result++;
    }
    return text.substring(0, result);
  }

  private int splitPoint(String text, int start) {
    int index = text.indexOf('\n');
    if (index > 0) {
      String line = text.substring(0, index);
      if (inMultiLineComment) {
        if (syntax.endsMultiLineComment(line)) {
          inMultiLineComment = false;
          return index;
        }
      } else if (syntax.startsMultiLineComment(line)) {
        inMultiLineComment = true;
        return index;
      }
    }
    SplitPoint result = new RegularSplitPoint(text, start, start, maxLength, -1, inMultiLineComment, syntax);
    while (result.canAdvance()) {
      result = result.advance();
    }
    return result.getIndex();
  }

  interface SplitPoint {

    boolean canAdvance();

    SplitPoint advance();

    int getIndex();

  }

  private static class FinalSplitPoint implements SplitPoint {

    private final int index;

    public FinalSplitPoint(int index) {
      this.index = index;
    }

    @Override
    public boolean canAdvance() {
      return false;
    }

    @Override
    public SplitPoint advance() {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getIndex() {
      return index;
    }

  }

  private abstract static class IntermediateSplitPoint implements SplitPoint {

    private final String text;
    private final int max;
    private final int best;
    private final int len;
    private final int start;
    private final Syntax syntax;
    private int current;

    public IntermediateSplitPoint(String text, int start, int current, int max, int best, Syntax syntax) {
      this.text = text;
      this.start = start;
      this.current = current;
      this.max = max;
      this.best = best;
      this.syntax = syntax;
      this.len = text.length();
    }

    @Override
    public boolean canAdvance() {
      return true;
    }

    @Override
    public int getIndex() {
      throw new UnsupportedOperationException();
    }

    protected boolean hasChars() {
      return current < len && (len > max || text.contains("\n"));
    }

    protected char currentChar() {
      if (current >= text.length()) {
        throw new IllegalStateException("At end of text: " + text);
      }
      return text.charAt(current);
    }

    protected void nextChar() {
      current++;
    }

    protected char bestChar() {
      return text.charAt(best);
    }

    protected SplitPoint inString() {
      return new InStringSplitPoint(text, start, current, max, best, syntax);
    }

    protected SplitPoint better(boolean inComment) {
      return new RegularSplitPoint(text, start, current + 1, max, current, inComment, syntax);
    }

    protected SplitPoint same() {
      return new RegularSplitPoint(text, start, current + 1, max, best, false, syntax);
    }

    protected boolean isBeyondLineLength() {
      return best > start && current >= max;
    }

    protected int improvement() {
      return current - best;
    }

    protected boolean notSplit() {
      return best < 0;
    }

    protected SplitPoint result() {
      return new FinalSplitPoint(best);
    }

    protected boolean isStringStart(char c) {
      return syntax.isStringStart(c);
    }

    protected boolean canSplitCommentOn(char c) {
      return syntax.canSplitCommentOn(c);
    }

    protected boolean canSplitOn(char c, boolean commentIsEmpty) {
      return syntax.canSplitOn(c, commentIsEmpty);
    }

  }

  private static class RegularSplitPoint extends IntermediateSplitPoint {

    private String comment = "";
    private boolean inComment;

    public RegularSplitPoint(String text, int start, int current, int max, int best, boolean inComment, Syntax syntax) {
      super(text, start, current, max, best, syntax);
      this.inComment = inComment;
    }

    @Override
    public SplitPoint advance() {
      while (hasChars()) {
        char c = currentChar();
        updateIsInComment(c);
        if (!inComment && isStringStart(c)) {
          return inString();
        }
        if (isSplittableOn(c) && isBetter(c)) {
          return better(inComment);
        }
        if (isBeyondLineLength()) {
          break;
        }
        nextChar();
      }
      return result();
    }

    private void updateIsInComment(char c) {
      if (!inComment) {
        if (COMMENT_START.startsWith(comment + c)) {
          comment += c;
        } else {
          comment = "";
        }
        if (COMMENT_START.equals(comment)) {
          inComment = true;
        }
      }
    }

    private boolean isSplittableOn(char c) {
      return inComment ? canSplitCommentOn(c) : canSplitOn(c, comment.isEmpty());
    }

    private boolean isBetter(char candidate) {
      if (notSplit()) {
        return true;
      }
      return separatorClass(candidate) >= separatorClass(bestChar())
          || improvement() > MAX_DISTANCE_TO_PREFER_BETTER_SEPARATOR_CLASS;
    }

    private int separatorClass(char c) {
      if (Character.getType(c) == Character.LINE_SEPARATOR) {
        return 3;
      }
      switch (c) {
        case ' ':
          return 2;
        case '.':
          return 0;
        default:
          return 1;
      }
    }

  }

  private static class InStringSplitPoint extends IntermediateSplitPoint {

    private final char quote;

    public InStringSplitPoint(String text, int start, int current, int max, int best, Syntax syntax) {
      super(text, start, current, max, best, syntax);
      this.quote = currentChar();
      nextChar();
    }

    @Override
    public SplitPoint advance() {
      boolean escape = false;
      while (escape || currentChar() != quote) {
        escape = currentChar() == '\\';
        nextChar();
      }
      return same();
    }

  }

}
