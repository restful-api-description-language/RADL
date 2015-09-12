/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Pattern;

import org.junit.Test;

import radl.core.code.Code;
import radl.test.RandomData;
import radl.test.TestUtil;


public class JavaCodeTest {

  private static final RandomData RANDOM = new RandomData();

  private final JavaCode code = new JavaCode();

  @Test(expected = IllegalStateException.class)
  public void throwsExceptionOnInvalidJavaCode() {
    code.add(someValue());

    code.typeName();
  }

  private String someValue() {
    return RANDOM.string(5);
  }

  @Test
  public void extractsClassName() {
    String className = randomJavaId();

    code.add("public class %s {", className);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  private String randomJavaId() {
    return Java.toIdentifier(someValue());
  }

  @Test
  public void extractsEmptyClassAnnotations() {
    code.add("public class %s {", randomJavaId());
    code.add("}");

    assertEquals("Class annotations", Collections.emptyList(), code.typeAnnotations());
  }

  @Test
  public void extractsClassAnnotations() {
    String annotation1 = "@Z" + someValue();
    String annotation2 = "@A" + someValue();

    code.add(annotation1);
    code.add(annotation2);
    code.add("public class %s {", randomJavaId());
    code.add("}");

    assertEquals("Class annotations", Arrays.asList(annotation2, annotation1), code.typeAnnotations());
  }

  @Test
  public void extractsEmptyImports() {
    code.add("public class %s {", randomJavaId());
    code.add("}");

    TestUtil.assertCollectionEquals("Imports", Collections.<String>emptyList(), code.imports());
  }

  @Test
  public void extractsImports() {
    String import1 = 'Z' + someValue();
    String import2 = 'A' + someValue();

    code.add("import %s;", import1);
    code.add("import %s;", import2);
    code.add("public class %s {", randomJavaId());
    code.add("}");

    TestUtil.assertCollectionEquals("Imports", Arrays.asList(import2, import1), code.imports());
  }

  @Test
  public void extractsEmptyMethods() {
    code.add("public class %s {", randomJavaId());
    code.add("}");

    TestUtil.assertCollectionEquals("Methods", Collections.<String>emptyList(), code.methods());
  }

  @Test
  public void extractsMethods() {
    String method1 = 'z' + someValue();
    String method2 = 'm' + someValue();
    String method3 = 'a' + someValue();
    String method4 = 'b' + someValue();
    code.add("public class %s {", randomJavaId());
    code.add("  public void %s() {", method1);
    code.add("  }");
    code.add("  public int %s(int arg) {", method2);
    code.add("    return 42;");
    code.add("  }");
    code.add("  public String %s(@RequestBody String arg) {", method3);
    code.add("    return \"foo\";");
    code.add("  }");
    code.add("  public void %s", method4);
    code.add("      () {");
    code.add("  }");
    code.add("}");

    TestUtil.assertCollectionEquals("Methods", Arrays.asList(method3, method4, method2, method1), code.methods());
  }

  @Test
  public void extractsEmptyMethodAnnotations() {
    String method = someValue();
    code.add("public class %s {", randomJavaId());
    code.add("  public void %s() {", method);
    code.add("  }");
    code.add("}");

    assertEquals("Method annotations", Collections.emptyList(), code.methodAnnotations(method));
  }

  @Test
  public void extractsMethodAnnotations() {
    String method1 = "m" + someValue();
    String annotation1 = "@Z" + someValue();
    String annotation2 = "@A" + someValue();
    String method2 = "m" + someValue();
    String annotation3 = "@" + someValue();
    String method3 = "foo";
    code.add("public class C {");
    code.add("  @Q%s", someValue());
    code.add("  public void q%s() {", someValue());
    code.add("  }");
    code.add("  %s", annotation1);
    code.add("  %s", annotation2);
    code.add("  public void %s(@%s %s %s) {", method1, someValue(), someValue(), someValue());
    code.add("  }");
    code.add("  public void %s() {", method2);
    code.add("  }");
    code.add("  %s", annotation3);
    code.add("  public void %s", method3);
    code.add("      () {");
    code.add("  }");
    code.add("}");

    assertEquals("Method annotations", Arrays.asList(annotation2, annotation1), code.methodAnnotations(method1));
    assertEquals("No annotations", Collections.emptyList(), code.methodAnnotations(method2));
    assertEquals("Unknown method", Collections.emptyList(), code.methodAnnotations(someValue()));
    assertEquals("Annotations for multi-line method", Arrays.asList(annotation3), code.methodAnnotations(method3));
  }

  @Test
  public void extractsMethodReturnType() {
    String method = "m" + someValue();
    String returns = someValue();
    code.add("public class C {");
    code.add("  public %s %s(int foo) {", returns, method);
    code.add("  }");
    code.add("}");

    assertEquals("Returns", returns, code.methodReturns(method));
    assertEquals("Returns of non-existing method", "", code.methodReturns(someValue()));
  }

  @Test
  public void extractsMethodArguments() {
    String method = "m1" + someValue();
    String arguments = String.format("int %s", someValue());
    String methodWithAnnotation = "m2" + someValue();
    String argumentsWithAnnotation = String.format("@%s String %s", someValue(), someValue());
    String methodWithoutArguments = "m3" + someValue();
    code.add("public class C {");
    code.add("  public void %s(%s) {", method, arguments);
    code.add("  }");
    code.add("  public void %s(%s) {", methodWithAnnotation, argumentsWithAnnotation);
    code.add("  }");
    code.add("  public void %s() {", methodWithoutArguments);
    code.add("  }");
    code.add("}");

    assertEquals("Arguments", arguments, code.methodArguments(method));
    assertEquals("Arguments annotations", argumentsWithAnnotation, code.methodArguments(methodWithAnnotation));
    assertEquals("No arguments", "", code.methodArguments(methodWithoutArguments));
    assertEquals("Unknow method", "", code.methodArguments(someValue()));
  }

  @Test
  public void extractsMethodBody() {
    String singleLineMethod = "m1" + someValue();
    String singleLine = "return 42;";
    String complexMethod = "m2" + someValue();
    Code complexBody = new Code();
    complexBody.add("if (false) {");
    complexBody.add("return 666;");
    complexBody.add("}");
    complexBody.add("return 313;");
    String complexText = complexBody.text().trim();
    code.add("public class C {");
    code.add("  public int %s() {", singleLineMethod);
    code.add("  %s", singleLine);
    code.add("  }");
    code.add("  public int %s() {", complexMethod);
    for (String line : complexBody) {
      code.add("    %s", line);
    }
    code.add("  }");
    code.add("}");

    assertEquals("Single line body", singleLine, code.methodBody(singleLineMethod));
    assertEquals("Empty body", "", code.methodBody(someValue()));
    assertEquals("Complex body", complexText, code.methodBody(complexMethod));
  }

  @Test
  public void extractsPackage() throws Exception {
    String packageName = String.format("%s.%s", someValue(), someValue());
    code.add("package %s;", packageName);
    code.add("public class %s {", someValue());
    code.add("}");

    assertEquals("Package", packageName, code.packageName());
  }

  @Test
  public void extractsDefaultPackage() throws Exception {
    code.add("public class %s {", someValue());
    code.add("}");

    assertEquals("Package", "", code.packageName());
  }

  @Test
  public void extractsPackageWhenLineEndsWithComment() throws Exception {
    String packageName = String.format("%s.%s", someValue(), someValue());
    code.add("package %s; // NOPMD", packageName);
    code.add("public class %s {", someValue());
    code.add("}");

    assertEquals("Package", packageName, code.packageName());
  }

  @Test
  public void extractsFields() {
    String field1 = 'z' + someValue();
    String field2 = 'a' + someValue();
    String field3 = 'b' + someValue();
    String field4 = "MEDIA_HAG_RWT";
    String field5 = 'N' + someValue();
    String field6 = 'O' + someValue();
    String type1 = "Object";
    String type2 = "int";
    String type3 = "boolean";
    String type4 = "String";
    String type5 = "String";
    String type6 = "String";
    String value2 = "42";
    String value4 = '"' + someValue() + '/' + someValue() + "; foo\"";
    String value5 = '"' + someValue() + '"';
    String value6 = '"' + someValue() + '"';
    String annotation1 = '@' + someValue();
    String comment = someValue();
    code.add("public class %s {", someValue());
    code.add("  %s", annotation1);
    code.add("  private %s %s;", type1, field1);
    code.add("  /**");
    code.add("   * %s", comment);
    code.add("   */");
    code.add("  private static final %s %s = %s;", type2, field2, value2);
    code.add("  %s %s;", type3, field3);
    code.add("  %s %s = %s;", type4, field4, value4);
    code.add("  static final %s %s = %s;", type5, field5, value5);
    code.add("  %s %s = %s; // %s", type6, field6, value6, someValue());
    code.add("}");

    TestUtil.assertCollectionEquals("Field names", Arrays.asList(field4, field5, field6, field2, field3, field1),
        code.fieldNames());
    assertEquals("Field type #1", type1, code.fieldType(field1));
    assertEquals("Field type #2", type2, code.fieldType(field2));
    assertEquals("Field type #3", type3, code.fieldType(field3));
    assertEquals("Field type #4", type4, code.fieldType(field4));
    assertEquals("Field type #5", type5, code.fieldType(field5));
    assertEquals("Field type #6", type6, code.fieldType(field6));
    assertEquals("Field annotations #1", Arrays.asList(annotation1), code.fieldAnnotations(field1));
    assertEquals("Field annotations #2", Collections.emptyList(), code.fieldAnnotations(field2));
    assertEquals("Field annotations #3", Collections.emptyList(), code.fieldAnnotations(field3));
    assertEquals("Field annotations #4", Collections.emptyList(), code.fieldAnnotations(field4));
    assertEquals("Field annotations #5", Collections.emptyList(), code.fieldAnnotations(field5));
    assertEquals("Field annotations #6", Collections.emptyList(), code.fieldAnnotations(field6));
    assertNull("Field value #1", code.fieldValue(field1));
    assertNull("Field value #3", code.fieldValue(field3));
    assertEquals("Field value #2", value2, code.fieldValue(field2));
    assertEquals("Field value #4", value4, code.fieldValue(field4));
    assertEquals("Field value #5", value5, code.fieldValue(field5));
    assertEquals("Field value #6", value6, code.fieldValue(field6));
    assertFalse("Is constant #1", code.fieldIsContant(field1));
    assertTrue("Is constant #2", code.fieldIsContant(field2));
    assertFalse("Is constant #3", code.fieldIsContant(field3));
    assertFalse("Is constant #4", code.fieldIsContant(field4));
    assertTrue("Is constant #5", code.fieldIsContant(field5));
    assertEquals("Field comments #1", Collections.emptyList(), code.fieldComments(field1));
    assertEquals("Field comments #2", Arrays.asList(comment), code.fieldComments(field2));
    assertEquals("Field comments #3", Collections.emptyList(), code.fieldComments(field3));
    assertEquals("Field comments #4", Collections.emptyList(), code.fieldComments(field4));
    assertEquals("Field comments #5", Collections.emptyList(), code.fieldComments(field5));
    assertEquals("Field comments #6", Collections.emptyList(), code.fieldComments(field6));
  }

  @Test
  public void extractsFileComments() throws Exception {
    String comment1 = RANDOM.string();
    String comment2 = RANDOM.string();
    String commentPrefix = " * ";
    code.add("/*");
    code.add(commentPrefix + comment1);
    code.add(commentPrefix + comment2);
    code.add("*/");

    assertEquals("Comments", Arrays.asList(comment1, comment2), code.fileComments());
  }

  @Test
  public void extractsStaticImports() {
    String className = randomJavaId();
    String importedClassName = randomJavaId();

    code.add("import static %s;", importedClassName);
    code.add("public class %s {", className);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
    TestUtil.assertCollectionEquals("Imports", Arrays.asList(importedClassName), code.imports());
  }

  @Test
  public void extractsSuperClass() {
    String superClassName = randomJavaId();
    String className = randomJavaId();

    code.add("public class %s extends %s {", className, superClassName);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
    assertEquals("Super class name", superClassName, code.superTypeName());
  }

  @Test
  public void extractsInterfaces() {
    String interfaceName1 = 'Z' + randomJavaId();
    String interfaceName2 = 'A' + randomJavaId();
    String className = randomJavaId();

    code.add("public class %s implements %s, %s {", className, interfaceName1, interfaceName2);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
    TestUtil.assertCollectionEquals("Interfaces", Arrays.asList(interfaceName2, interfaceName1), code.implementedInterfaces());
  }

  @Test
  public void extractsClassNameFromFinalClass() {
    String className = randomJavaId();

    code.add("final class %s {", className);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  @Test
  public void extractsClassNameFromGenericClass() {
    String className = randomJavaId() + '<' + randomJavaId() + '>';

    code.add("final class %s {", className);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  @Test
  public void extractsClassNameFromAbstractClass() {
    String className = randomJavaId();

    code.add("public abstract class %s {", className);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  @Test
  public void extractsClassNameFromMultiGenericClass() {
    String className = randomJavaId();
    String superClass = randomJavaId() + '<' + randomJavaId() + ", " + randomJavaId().toLowerCase(Locale.getDefault())
        + '.' + randomJavaId() + '>';

    code.add("public class %s extends %s {", className, superClass);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  @Test
  public void extractsClassNameFromExtendedGenericClass() {
    String className = randomJavaId() + '<' + randomJavaId() + " extends " + randomJavaId() + '>';

    code.add("class %s {", className);
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  @Test
  public void extractsClassNameWithComment() {
    String className = randomJavaId();

    code.add("public class %s { // %s", className, RANDOM.string());
    code.add("}");

    assertEquals("Class name", className, code.typeName());
  }

  @Test
  public void extractsClassNameFromGenericGenericClass() {
    String className = randomJavaId();
    String genericClass1 = randomJavaId();
    String genericClass2 = randomJavaId();
    String fullClass = className + "<T extends " + genericClass1 + "<" + genericClass2 + ">>";

    code.add("public abstract class %s {", fullClass);
    code.add("}");

    assertEquals("Class name", fullClass, code.typeName());
  }

  @Test
  public void matchesTypeNames() {
    assertMatches("Ape", JavaCode.TYPE_PATTERN);
    assertMatches("bear.cheetah.Dingo", JavaCode.TYPE_PATTERN);
    assertMatches("Elephant2", JavaCode.TYPE_PATTERN);
    assertMatches("fox.giraffe\n    .hyena.Iguana", JavaCode.TYPE_PATTERN);
    assertMatches("Jaguar.Koala", JavaCode.TYPE_PATTERN);
  }

  private void assertMatches(String input, String regExp) {
    assertTrue("Not recognized: " + input, input.matches(regExp));
  }

  @Test
  public void matchesGenericTypeNames() {
    assertMatches("Ape", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Bear<Cheetah>", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Dingo<Elephant extends Fox>", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Giraffe<Hyena extends Iguana<Jaguar>>", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Koala<Leopard, Mule>", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Nightingale<O>", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("quetzal.Rhino", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Snake<Tapir<Unicorn>>", JavaCode.GENERIC_TYPE_PATTERN);
    assertMatches("Velociraptor <Whale>", JavaCode.GENERIC_TYPE_PATTERN);
  }

  @Test
  public void matchesClassNames() {
    assertMatchesClass("class Ape {");
    assertMatchesClass("final class Bear {");
    assertMatchesClass("public abstract class Cheetah {");
    assertMatchesClass("class Dingo<Elephant> {");
    assertMatchesClass("class Fox<Giraffe extends Hyena> {");
    assertMatchesClass("public abstract class Iguana<Jaguar extends Koala<Leopard>> {");
    assertMatchesClass("public abstract class Mule<N extends Opossum<P>, P extends Quetzal, R extends com.company.Snake> {");
    assertMatchesClass("public class Tapir implements Unicorn<Whale<Yak>> {");
    assertMatchesClass("public abstract class Zebra<A extends Bat> extends Cougar\n    <D> {");
  }

  private void assertMatchesClass(String input) {
    assertMatchesPattern(input, JavaCode.CLASS_NAME_PATTERN);
  }

  private void assertMatchesPattern(String input, Pattern typePattern) {
    assertMatchesPattern(true, input, typePattern);
  }

  private void assertMatchesPattern(boolean expected, String input, Pattern pattern) {
    assertEquals("Recognized: " + input, expected, pattern.matcher(input).matches());
  }

  @Test
  public void matchesInterfaceNames() {
    assertMatchesInterface("public interface Ape {");
    assertMatchesInterface("public interface Bear extends Cheetah {");
  }

  private void assertMatchesInterface(String input) {
    assertMatchesPattern(input, JavaCode.INTERFACE_NAME_PATTERN);
  }

  @Test
  public void comparesTypeNames() {
    assertType("Ape", true, "Ape");
    assertType("Bear", false, "Cheetah");
    assertType("Dingo<Elephant>", true, "Dingo");
    assertType("Fox<Hyena>", true, "Fox<Hyena>");
  }

  private void assertType(String className, boolean expected, String type) {
    code.add("public class %s {", className);
    code.add("}");

    assertEquals(className, expected, code.isType(type));
  }

  @Test
  public void matchesAnnotations() {
    assertMatchesAnnotation("@Ape");
    assertMatchesAnnotation("@Bear(\"cheetah\")");
  }

  private void assertMatchesAnnotation(String input) {
    assertMatches(input, JavaCode.ANNOTATION_PATTERN);
  }

  @Test
  public void matchesNames() {
    assertMatchesName("ape");
    assertMatchesName("bear2");
    assertMatchesName("cheetahDingo");
  }

  private void assertMatchesName(String input) {
    assertMatches(input, JavaCode.NAME_PATTERN);
  }

  @Test
  public void matchesParameters() {
    assertMatchesParameter("int ape");
    assertMatchesParameter("String bear");
    assertMatchesParameter("@Cheetah boolean dingo");
    assertMatchesParameter("@Elephant @Fox(\"giraffe\") Hyena iguana");
    assertMatchesParameter("/* jaguar */ int koala");
    assertMatchesParameter("int /* leopard */ mule");
    assertMatchesParameter("int nightingale /* opossum */");
  }

  private void assertMatchesParameter(String input) {
    assertMatches(input, JavaCode.PARAMETER_PATTERN);
  }

  @Test
  public void matchesMethods() {
    assertMatchesMethod("public void ape() {");
    assertMatchesMethod("void bear(\nint cheetah\n) {");
    assertMatchesMethod("private int dingo(@Elephant(\"fox\") String giraffe) {");
    assertMatchesMethod("protected Hyena iguana(Jaguar jaguar, boolean koala) {");
    assertMatchesMethod("protected void leopard(@Mule @Nightingale(\"false\") Opossum parrot) {");
    assertMatchesMethod("public void quetzal(/* rhino */) {");
    assertMatchesMethod("public void tiger() { // unicorn");
    assertMatchesMethod("public Whale() {", false);
    assertMatchesMethod("/*public Yak() { }*/", false);
    assertMatchesMethod("public A<B> c() {");
  }

  private void assertMatchesMethod(String input) {
    assertMatchesPattern(input, JavaCode.METHOD_NAME_PATTERN);
  }

  private void assertMatchesMethod(String input, boolean expected) {
    assertMatchesPattern(expected, input, JavaCode.METHOD_NAME_PATTERN);
  }

  @Test
  public void extractsTypeNameFromInterface() {
    String interfaceName = randomJavaId();
    code.add("public interface %s {", interfaceName);
    code.add("}");

    assertEquals("Type name", interfaceName, code.typeName());
  }

}
