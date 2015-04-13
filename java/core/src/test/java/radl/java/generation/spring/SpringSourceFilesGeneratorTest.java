/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;

import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import radl.common.io.IO;
import radl.common.xml.DocumentBuilder;
import radl.core.code.Code;
import radl.core.code.GeneratedSourceFile;
import radl.core.code.SourceFile;
import radl.core.generation.CodeGenerator;
import radl.core.generation.SourceFilesGenerator;
import radl.java.code.JavaCode;
import radl.test.RandomData;
import radl.test.TestUtil;


public class SpringSourceFilesGeneratorTest {

  private static final RandomData RANDOM = new RandomData();

  private final File baseDir = TestUtil.randomDir(SpringSourceFilesGeneratorTest.class);

  @After
  public void done() {
    IO.delete(baseDir);
  }

  @Test
  public void generatesSourceFilesFromCode() {
    String basePath = RANDOM.string();
    String package1 = aPackage();
    String class1 = aClass();
    String package2 = aPackage();
    String class2 = aClass();
    Code code1 = newCode(package1, class1);
    Code code2 = newCode(package2, class2);
    CodeGenerator codeGenerator = mock(CodeGenerator.class);
    Document radl = DocumentBuilder.newDocument().build();
    when(codeGenerator.generateFrom(radl)).thenReturn(Arrays.asList(code1, code2));
    SourceFilesGenerator sourceFilesGenerator = new SpringSourceFilesGenerator(codeGenerator, "", "");

    Iterable<SourceFile> actual = sourceFilesGenerator.generateFrom(radl, new File(basePath));

    assertEquals("Source files", Arrays.asList(expectedSourceFile(basePath, package1, class1),
        expectedSourceFile(basePath, package2, class2)), actual);
  }

  private String aPackage() {
    return String.format("%s.%s", RANDOM.string(), RANDOM.string());
  }

  private String aClass() {
    return String.format("C%s", RANDOM.string());
  }

  private Code newCode(String packageName, String className) {
    JavaCode result = mock(JavaCode.class);
    when(result.packageName()).thenReturn(packageName);
    when(result.typeName()).thenReturn(className);
    return result;
  }

  private SourceFile expectedSourceFile(String basePath, String packageName, String className) {
    String path = basePath + File.separator + packageName.replaceAll("\\.", "\\" + File.separator)
        + File.separator + className + ".java";
    return new SourceFile(new File(path).getAbsolutePath());
  }

  @Test
  public void generatesGeneratedSourceFileForController() throws Exception {
    String packageName = aPackage();
    String controllerClass = aClass() + "Controller";
    Code controllerCode = newCode(packageName, controllerClass);
    CodeGenerator codeGenerator = mock(CodeGenerator.class);
    Document radl = DocumentBuilder.newDocument().build();
    when(codeGenerator.generateFrom(radl)).thenReturn(Arrays.asList(controllerCode));
    SourceFilesGenerator sourceFilesGenerator = new SpringSourceFilesGenerator(codeGenerator, "", "");

    Iterable<SourceFile> actual = sourceFilesGenerator.generateFrom(radl, new File(RANDOM.string()));

    assertEquals("Generated source file", GeneratedSourceFile.class, actual.iterator().next().getClass());
  }

  @Test
  public void generatesGeneratedSourceFileForApi() throws Exception {
    assertGeneratedSourceFile("Api");
  }

  private void assertGeneratedSourceFile(String typeName) {
    String packageName = aPackage();
    Code code = newCode(packageName, typeName);
    CodeGenerator codeGenerator = mock(CodeGenerator.class);
    Document radl = DocumentBuilder.newDocument().build();
    when(codeGenerator.generateFrom(radl)).thenReturn(Arrays.asList(code));
    SourceFilesGenerator sourceFilesGenerator = new SpringSourceFilesGenerator(codeGenerator, "", "");

    Iterable<SourceFile> actual = sourceFilesGenerator.generateFrom(radl, baseDir);

    assertEquals("Generated source file", GeneratedSourceFile.class, actual.iterator().next().getClass());
  }

  @Test
  public void generatesGeneratedSourceFileForUris() throws Exception {
    assertGeneratedSourceFile("Uris");
  }

}
