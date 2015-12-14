/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import radl.common.io.IO;
import radl.core.code.Code;
import radl.core.code.GeneratedSourceFile;
import radl.core.code.SourceFile;
import radl.core.enforce.Reality;
import radl.core.scm.SourceCodeManagementSystem;
import radl.test.RandomData;
import radl.test.TestUtil;


public class RealSourceFilesTest {

  private static final RandomData RANDOM = new RandomData();

  private final File baseDir = TestUtil.randomDir(RealSourceFilesTest.class);
  private final String generatedSourceSetDir = "g_" + RANDOM.string();
  private final String mainSourceSetDir = "m_" + RANDOM.string();
  private final String codeDir = "com/company/p" + RANDOM.string();
  private final SourceCodeManagementSystem scm = mock(SourceCodeManagementSystem.class);
  private final Reality<String, SourceFile> reality = new RealSourceFiles(baseDir, generatedSourceSetDir,
      mainSourceSetDir, codeDir, scm);

  @Before
  public void init() {
    baseDir.mkdirs();
  }

  @After
  public void done() {
    IO.delete(baseDir);
  }

  @Test
  public void returnsNoIdsForMissingDirectory() {
    IO.delete(baseDir);

    Collection<String> actual = reality.getIds();

    assertTrue("Extra IDs: " + actual, actual.isEmpty());
  }

  @Test
  public void returnsCanonicalPathsOfExistingFilesAsIds() throws IOException {
    File file1 = ensureRandomFile(mainSourceSetDir, codeDir, "a");
    File file2 = ensureRandomFile(generatedSourceSetDir, codeDir, "z");
    List<String> expected = Arrays.asList(file2.getCanonicalPath(), file1.getCanonicalPath());

    Collection<String> actual = reality.getIds();

    TestUtil.assertCollectionEquals("IDs", expected, actual);
  }

  private File ensureRandomFile(String sourceSetDir, String packageName, String prefix) throws IOException {
    File result = randomFile(sourceSetDir, packageName, prefix);
    result.getParentFile().mkdirs();
    try (PrintWriter writer = new PrintWriter(result, "UTF8")) {
      writer.println(RANDOM.string());
    }
    return result;
  }

  private File randomFile(String sourceSetDir, String packageName, String prefix) {
    StringBuilder path = new StringBuilder();
    path.append(RANDOM.string(8)).append('.').append(RANDOM.string(3));
    for (int chance = 70; RANDOM.integer(100) < chance; chance -= 30) {
      path.insert(0, File.separator).insert(0, RANDOM.string(8));
    }
    path.insert(0,  prefix);
    StringBuilder packageDir = new StringBuilder();
    for (String part : packageName.split("\\.")) {
      packageDir.append(part).append(File.separator);
    }
    path.insert(0, packageDir);
    path.insert(0, sourceSetDir + File.separator);
    return new File(baseDir, path.toString());
  }

  @Test
  public void returnsSourceFileForPathsInMainSourceSet() throws IOException {
    File file = ensureRandomFile(mainSourceSetDir, codeDir, "");
    String path = file.getCanonicalPath();

    SourceFile actual = reality.get(path);

    assertEquals("Source file", new SourceFile(path), actual);
  }

  @Test
  public void returnsGeneratedSourceFileForPathsInGeneratedSourceSet() throws IOException {
    File file = ensureRandomFile(generatedSourceSetDir, codeDir, "");
    String path = file.getCanonicalPath();

    SourceFile actual = reality.get(path);

    assertEquals("Generated source file", GeneratedSourceFile.class, actual.getClass());
    assertEquals("Source file", new GeneratedSourceFile(path), actual);
  }

  @Test
  public void addsFile() throws IOException {
    File file = randomFile(aSourceSetDir(), codeDir, "");
    Code code = new Code();
    code.add("public class C { }");
    SourceFile sourceFile = new SourceFile(file.getCanonicalPath(), code);

    assertFalse("Sanity check", file.exists());
    reality.add(file.getPath(), sourceFile);

    assertTrue("File is not created", file.exists());
    assertEquals("File contents", code.text(), textOf(file));
  }

  private String aSourceSetDir() {
    return RANDOM.logical() ? generatedSourceSetDir : mainSourceSetDir;
  }

  private String textOf(File file) throws IOException {
    StringBuilder text = new StringBuilder();
    for (String line : IO.linesOf(new FileInputStream(file))) {
      text.append(line).append('\n');
    }
    return text.toString();
  }

  @Test
  public void deletesFile() throws IOException {
    File file = ensureRandomFile(aSourceSetDir(), codeDir, "");

    reality.remove(file.getPath());

    assertFalse("File still exists", file.exists());
  }

  @Test
  public void preparesReadOnlyFileForUpdate() throws Exception {
    File file = randomFile(aSourceSetDir(), codeDir, "");
    Code code = new Code();
    code.add("public class C { }");
    SourceFile sourceFile = new SourceFile(file.getCanonicalPath(), code);
    file.setReadOnly();

    reality.add(file.getPath(), sourceFile);

    verify(scm).prepareForUpdate(eq(file));
  }

  @Test
  public void preparesReadOnlyFileForDelete() throws Exception {
    File file = ensureRandomFile(aSourceSetDir(), codeDir, "");
    Code code = new Code();
    code.add("public class C { }");
    file.setReadOnly();

    reality.remove(file.getPath());

    verify(scm).prepareForDelete(eq(file));
  }

  @Test
  public void skipsFilesOutsideSourceSets() throws IOException {
    ensureRandomFile(RANDOM.string(), codeDir, RANDOM.string());

    Collection<String> actual = reality.getIds();

    assertTrue("Extra IDs: " + actual, actual.isEmpty());
  }

  @Test
  public void skipsFilesOutsidePackage() throws IOException {
    ensureRandomFile(mainSourceSetDir, RANDOM.string(), RANDOM.string());

    Collection<String> actual = reality.getIds();

    assertTrue("Extra IDs: " + actual, actual.isEmpty());
  }

}
