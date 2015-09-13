/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import radl.core.code.Code;


/**
 * Code that follows the Java language syntax.
 */
public class JavaCode extends Code {

  private static final Pattern PACKAGE_PATTERN = Pattern.compile(
      ".*^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;\\s*(//.*)?$.*",
      Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNIX_LINES);
  private static final String SCOPE = "(?:(private|protected|public)\\s+)?+";
  private static final String COMMENT_PATTERN = "(?:\\s*/\\*.*\\*/\\s*)?";
  public static final String TYPE_PATTERN = COMMENT_PATTERN + "(?:(?:[a-zA-Z]+\\s*\\.\\s*)*[a-zA-Z0-9_]+)"
      + COMMENT_PATTERN;
  static final String TYPES_PATTERN = TYPE_PATTERN + "(?:,\\s*" + TYPE_PATTERN + ")*";
  static final String GENERIC_TYPE_PATTERN
      = TYPE_PATTERN + "(?:\\s*<"
      + TYPES_PATTERN + "(?:\\s*<" + TYPES_PATTERN + ">\\s*)?(?:\\s+extends\\s+" + TYPES_PATTERN + "(?:\\s*<"
      + TYPES_PATTERN + "\\s*>)?)?" + "(,\\s*" + TYPES_PATTERN
      + "(?:\\s+extends\\s+" + TYPES_PATTERN + "(?:\\s*<" + TYPES_PATTERN + "\\s*>)?)?)*"
      + "\\s*>)?";
  static final Pattern CLASS_NAME_PATTERN = Pattern.compile("[^@]*(@.*)?^"
      + SCOPE + "(?:final\\s+)?+(?:abstract\\s+)?+class\\s+(" + GENERIC_TYPE_PATTERN
      + ")(?:\\s+extends\\s+(" + GENERIC_TYPE_PATTERN + "))?"
      + "(\\s+implements\\s+(" + GENERIC_TYPE_PATTERN + "(,\\s+" + GENERIC_TYPE_PATTERN + ")*))?"
      + "\\s+\\{(?:\\s+//.+)?$.*",
      Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNIX_LINES);
  static final Pattern INTERFACE_NAME_PATTERN = Pattern.compile(
      "[^@]*(@.*)?^" + SCOPE + "interface\\s+(" + GENERIC_TYPE_PATTERN + ")(?:\\s+extends\\s+("
      + GENERIC_TYPE_PATTERN + "))?\\s+\\{$.*",
      Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNIX_LINES);
  private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+(?:static\\s+)?+(.*);$");
  static final String ANNOTATION_PATTERN = "(?:@" + TYPE_PATTERN + "(?:\\([^)]+\\))?)";
  public static final String NAME_PATTERN = COMMENT_PATTERN + "[a-zA-Z0-9_]+" + COMMENT_PATTERN;
  static final String PARAMETER_PATTERN = "(?:(?:" + ANNOTATION_PATTERN + "\\s+)*" + TYPE_PATTERN + "\\s+"
      + NAME_PATTERN + ")";
  private static final String END_COMMENT = "(?:\\s*//.*)?";
  static final Pattern METHOD_NAME_PATTERN = Pattern.compile(
      "\\s*" + COMMENT_PATTERN + SCOPE
      + "(?:(" + TYPE_PATTERN + "(?:\\s*<" + TYPES_PATTERN + "\\s*>)?)\\s+)()?(" + NAME_PATTERN + ")\\s*\\(\\s*("
      + PARAMETER_PATTERN + "(,\\s+" + PARAMETER_PATTERN + ")*)?" + COMMENT_PATTERN + "\\s*\\)\\s*\\{" + END_COMMENT,
      Pattern.DOTALL | Pattern.MULTILINE | Pattern.UNIX_LINES);
  private static final Pattern FIELD_PATTERN = Pattern.compile(
      SCOPE + "((?:static\\s+)?+(?:final\\s+)?+)(" + TYPE_PATTERN
      + "(?:\\[\\])?)\\s+(" + NAME_PATTERN + ")(?:\\s*=\\s*(.+))?;" + END_COMMENT + "$");


  public JavaCode() {
    super(new JavaSyntax());
  }

  /**
   * @return The simple name of the top-level type (class or interface) defined in this code
   * @throws IllegalStateException when the code doesn't contain a Java type
   */
  public String typeName() {
    return getTypeNameMatcher().group(3);
  }

  private Matcher getTypeNameMatcher() {
    Matcher result = CLASS_NAME_PATTERN.matcher(text());
    if (!result.matches()) {
      result = INTERFACE_NAME_PATTERN.matcher(text());
    }
    if (!result.matches()) {
      throw new IllegalStateException("Invalid Java class:\n" + text());
    }
    return result;
  }

  /**
   * @return The annotations on the class defined in this code, or an empty collection if there are none
   */
  public Collection<String> typeAnnotations() {
    Matcher matcher = getTypeNameMatcher();
    List<String> result = new ArrayList<String>();
    String annotations = matcher.group(1);
    if (annotations != null) {
      for (String annotation : annotations.split("\\n")) {
        result.add(annotation.trim());
      }
      Collections.sort(result);
    }
    return result;
  }

  /**
   * @return The packages that this code imports, or an empty collection if there are none
   */
  public Collection<String> imports() {
    return collectPatterns(IMPORT_PATTERN, 1);
  }

  private Collection<String> collectPatterns(Pattern pattern, int group) {
    Collection<String> result = new TreeSet<String>();
    if (isSingleLinePattern(pattern)) {
      collectPatternsByLine(pattern, group, result);
    } else {
      collectionPatternsByText(pattern, group, result);
    }
    return result;
  }

  private boolean isSingleLinePattern(Pattern pattern) {
    return (pattern.flags() & Pattern.MULTILINE) == 0;
  }

  private void collectPatternsByLine(Pattern pattern, int group, Collection<String> patterns) {
    for (String line : this) {
      String trimmedLine = line.trim();
      if (trimmedLine.isEmpty()) {
        continue;
      }
      Matcher matcher = pattern.matcher(trimmedLine);
      if (matcher.matches()) {
        patterns.add(matcher.group(group));
      }
    }
  }

  private void collectionPatternsByText(Pattern pattern, int group, Collection<String> patterns) {
    Matcher matcher = pattern.matcher(text());
    while (matcher.find()) {
      patterns.add(matcher.group(group));
    }
  }

  /**
   * @return The names of the methods that the class defined in this code contains, or an empty collection if there
   * are none
   */
  public Collection<String> methods() {
    return collectPatterns(METHOD_NAME_PATTERN, 4);
  }

  /**
   * @param method The name of the required method
   * @return The annotations defined on the given method, or an empty collection if there are none
   */
  public Collection<String> methodAnnotations(String method) {
    return getAnnotations(METHOD_NAME_PATTERN, 4, method);
  }

  private Collection<String> getAnnotations(Pattern pattern, int namePart, String name) {
    List<String> result = new ArrayList<String>();
    if (isSingleLinePattern(pattern)) {
      getAnnotationsByLine(pattern, namePart, name, result);
    } else {
      getAnnotationsByText(pattern, namePart, name, result);
    }
    Collections.sort(result);
    return result;
  }

  private void getAnnotationsByLine(Pattern pattern, int namePart, String name, List<String> annotations) {
    boolean inAnnotation = false;
    for (String line : this) {
      String trimmedLine = line.trim();
      if (inAnnotation) {
        annotations.set(annotations.size() - 1, annotations.get(annotations.size() - 1) + ' ' + trimmedLine);
        inAnnotation = !trimmedLine.contains(")");
      } else if (trimmedLine.startsWith("@")) {
        annotations.add(trimmedLine);
        inAnnotation = trimmedLine.contains("(") && !trimmedLine.contains(")");
      } else {
        Matcher matcher = pattern.matcher(trimmedLine);
        if (matcher.matches() && name.equals(matcher.group(namePart))) {
          break;
        } else {
          annotations.clear();
        }
      }
    }
  }

  private void getAnnotationsByText(Pattern pattern, int namePart, String name, List<String> annotations) {
    Matcher matcher = pattern.matcher(text());
    int start = 0;
    while (matcher.find()) {
      String actual = matcher.group(namePart);
      if (name.equals(actual)) {
        boolean inAnnotation = false;
        for (String line : text().substring(start, matcher.start()).split("\\n")) {
          String trimmedLine = line.trim();
          if (inAnnotation) {
            annotations.set(annotations.size() - 1, annotations.get(annotations.size() - 1) + ' ' + trimmedLine);
            inAnnotation = !trimmedLine.contains(")");
          } else if (trimmedLine.startsWith("@")) {
            annotations.add(trimmedLine);
            inAnnotation = trimmedLine.contains("(") && !trimmedLine.contains(")");
          } else {
            annotations.clear();
          }
        }
        break;
      } else {
        start = matcher.end();
      }
    }
  }

  /**
   * @param method The name of the required method
   * @return The return clause for the given method, or an empty string if the code doesn't contain the method
   */
  public String methodReturns(String method) {
    return getMethodPart(method, 2);
  }

  private String getMethodPart(String method, int part) {
    return getPatternPart(METHOD_NAME_PATTERN, 4, method, part);
  }

  private String getPatternPart(Pattern pattern, int namePart, String name, int valuePart) {
    for (String line : this) {
      String trimmedLine = line.trim();
      Matcher matcher = pattern.matcher(trimmedLine);
      if (matcher.matches() && name.equals(matcher.group(namePart))) {
        String result = matcher.group(valuePart);
        return result == null ? "" : result;
      }
    }
    return "";
  }

  /**
   * @param method The name of the required method
   * @return The arguments for the given method, or an empty string if the code doesn't contain the method or doesn't
   * take any arguments
   */
  public String methodArguments(String method) {
    return getMethodPart(method, 5);
  }

  /**
   * @return The package that contains this code
   */
  public String packageName() {
    Matcher matcher = PACKAGE_PATTERN.matcher(text());
    if (matcher.matches()) {
      return matcher.group(1);
    }
    return "";
  }

  /**
   * @param method The name of the required method
   * @return The method's body, or an empty string if the code doesn't contain the method or doesn't contain any code
   */
  public String methodBody(String method) {
    StringBuilder result = new StringBuilder();
    int depth = 0;
    for (String line : this) {
      String trimmedLine = line.trim();
      if (depth > 0) {
        if ("}".equals(trimmedLine) && --depth == 0) {
          break;
        } else {
          if (result.length() > 0) {
            result.append('\n');
          }
          result.append(trimmedLine);
          if (trimmedLine.endsWith("{")) {
            depth++;
          }
        }
      }
      Matcher methodMatcher = METHOD_NAME_PATTERN.matcher(trimmedLine);
      if (methodMatcher.matches() && method.equals(methodMatcher.group(4))) {
        depth = 1;
      }
    }
    return result.toString();
  }

  /**
   * @return The names of the fields that the class defined in this code contains, or an empty collection if there
   * are none
   */
  public Collection<String> fieldNames() {
    return collectPatterns(FIELD_PATTERN, 4);
  }

  /**
   * @param fieldName The name of the required field
   * @return The field's type, or an empty string if the code doesn't contain the field
   */
  public String fieldType(String fieldName) {
    return getPatternPart(FIELD_PATTERN, 4, fieldName, 3);
  }

  /**
   * @param fieldName The name of the required field
   * @return The annotations defined on the given field, or an empty collection if there are none
   */
  public Iterable<String> fieldAnnotations(String fieldName) {
    return getAnnotations(FIELD_PATTERN, 4, fieldName);
  }

  /**
   * Returns the value of a given field.
   * @param fieldName The name of the field
   * @return The value of the given field
   */
  public String fieldValue(String fieldName) {
    String result = getPatternPart(FIELD_PATTERN, 4, fieldName, 5);
    return result.isEmpty() ? null : result;
  }

  /**
   * @return The comments at the start of the file containing this code
   */
  public Iterable<String> fileComments() {
    Collection<String> result = new ArrayList<String>();
    int line = 0;
    if (get(line).trim().startsWith("/*")) {
      while (!get(++line).trim().endsWith("*/")) {
        result.add(removeCommentPrefix(get(line)));
      }
    }
    return result;
  }

  private String removeCommentPrefix(String comment) {
    return comment.trim().replaceAll("^\\*+", "").trim();
  }

  /**
   * @return The name of the super type of this type
   */
  public String superTypeName() {
    return getTypeNameMatcher().group(5);
  }

  /**
   * @return The interfaces implemented by this type
   */
  public Iterable<String> implementedInterfaces() {
    Collection<String> result = new TreeSet<String>();
    String interfaces = getTypeNameMatcher().group(8);
    if (interfaces == null) {
      return result;
    }
    for (String implementedInterface : interfaces.split(",")) {
      result.add(implementedInterface.trim());
    }
    return result;
  }

  /**
   * @param type A Java type
   * @return Whether the type in this code is the same as the given type
   */
  public boolean isType(String type) {
    return stripGenerics(typeName()).equals(stripGenerics(type));
  }

  private String stripGenerics(String typeName) {
    int index = typeName.indexOf('<');
    if (index >= 0) {
      return typeName.substring(0, index);
    }
    return typeName;
  }

  /**
   * @param fieldName The name of a field in this type
   * @return Whether the field with the given name is a constant (is declared using <code>static final</code>)
   */
  public boolean fieldIsContant(String fieldName) {
    return "static final".equals(getPatternPart(FIELD_PATTERN, 4, fieldName, 2).trim());
  }

  public Iterable<String> fieldComments(String fieldName) {
    return getComments(FIELD_PATTERN, 4, fieldName);
  }

  private Collection<String> getComments(Pattern pattern, int namePart, String name) {
    List<String> result = new ArrayList<String>();
    if (isSingleLinePattern(pattern)) {
      getCommentsByLine(pattern, namePart, name, result);
    } else {
      getCommentsByText(pattern, namePart, name, result);
    }
    return result;
  }

  private void getCommentsByLine(Pattern pattern, int namePart, String name, List<String> comments) {
    boolean inComment = false;
    for (String line : this) {
      String trimmedLine = line.trim();
      if (inComment) {
        inComment = !"*/".equals(trimmedLine);
        if (inComment) {
          comments.add(trimmedLine.substring(trimmedLine.indexOf(' ') + 1));
        }
      } else if (trimmedLine.startsWith("/**")) {
        int index = trimmedLine.indexOf(' ');
        if (index > 0) {
          comments.add(trimmedLine.substring(index + 1));
        }
        inComment = !trimmedLine.contains("*/");
      } else {
        Matcher matcher = pattern.matcher(trimmedLine);
        if (matcher.matches() && name.equals(matcher.group(namePart))) {
          break;
        } else {
          comments.clear();
        }
      }
    }
  }

  private void getCommentsByText(Pattern pattern, int namePart, String name, List<String> comments) {
    Matcher matcher = pattern.matcher(text());
    int start = 0;
    while (matcher.find()) {
      String actual = matcher.group(namePart);
      if (name.equals(actual)) {
        boolean inComment = false;
        for (String line : text().substring(start, matcher.start()).split("\\n")) {
          String trimmedLine = line.trim();
          if (inComment) {
            comments.set(comments.size() - 1,
                comments.get(comments.size() - 1) + ' ' + trimmedLine.substring(trimmedLine.indexOf(' ') + 1));
            inComment = !trimmedLine.contains("*/");
          } else if (trimmedLine.startsWith("/**")) {
            comments.add(trimmedLine.substring(trimmedLine.indexOf(' ') + 1));
            inComment = !trimmedLine.contains("*/");
          } else {
            comments.clear();
          }
        }
        break;
      } else {
        start = matcher.end();
      }
    }
  }

  public String constructorBody() {
    String result = text();
    int start = result.indexOf(typeName() + "(");
    if (start < 0) {
      return null;
    }
    start = result.indexOf('{', start) + 1;
    int count = 1;
    int len = result.length();
    int end = start;
    while (end < len) {
      char c = result.charAt(end);
      if (c == '{') {
        count++;
      } else if (c == '}') {
        count--;
        if (count == 0) {
          break;
        }
      }
      end++;
    }
    return result.substring(start, end).trim();
  }

}
