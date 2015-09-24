/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import radl.common.io.IO;
import radl.common.xml.Xml;
import radl.core.code.SourceFile;
import radl.core.scm.SourceCodeManagementSystem;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.RandomData;
import radl.test.TestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;


public class RadlToSpringServerTest {

  private static final RandomData RANDOM = new RandomData();

  private final RadlToSpringServer generator = new RadlToSpringServer();
  private final File radlFile = new File(TestUtil.randomDir(RadlToSpringServerTest.class), RANDOM.string() + ".radl");
  private final File baseDir = TestUtil.randomDir(RadlToSpringServerTest.class);

  @Before
  public void init() {
    IO.delete(baseDir);
    radlFile.getParentFile().mkdirs();
  }

  @After
  public void done() {
    IO.delete(radlFile);
    IO.delete(baseDir);
  }

  @Test
  public void generatesSpringServerSourceFilesFromRadl() throws IOException {
    String lower = "f";
    String upper = lower.toUpperCase(Locale.getDefault());
    String name = RANDOM.string();
    Document radlDocument = RadlBuilder.aRadlDocument()
        .withResource()
            .named(lower + name)
        .end()
    .build();
    PrintWriter writer = new PrintWriter(radlFile, "UTF8");
    try {
      writer.print(Xml.toString(radlDocument));
    } finally {
      writer.close();
    }
    String packagePrefix = somePackage();
    String generatedSourceSetDir = someSourceSetDir();
    String mainSourceSetDir = someSourceSetDir();
    SourceCodeManagementSystem scm = mock(SourceCodeManagementSystem.class);
    String header = RANDOM.string();

    generator.generate(radlFile, baseDir, packagePrefix, generatedSourceSetDir, mainSourceSetDir, scm, header);

    Collection<File> files = generatedFiles();
    File controller = find(files, upper + name + "Controller.java");
    assertNotNull("Missing controller: " + files, controller);
    String expectedPath = expectedFilePath(generatedSourceSetDir, packagePrefix, lower + name, controller);
    assertEquals("Path: " + expectedPath + " vs. " + controller.getPath(), expectedPath, controller.getPath());
    JavaCode javaCode = toJava(controller);
    TestUtil.assertCollectionEquals("Header for " + controller.getName(), Arrays.asList(header),
        javaCode.fileComments());

    File controllerHelper = find(files, upper + name + "ControllerHelper.java");
    assertNotNull("Missing controller helper: " + files, controllerHelper);
    expectedPath = expectedFilePath(mainSourceSetDir, packagePrefix, lower + name, controllerHelper);
    assertEquals("Path: " + expectedPath + " vs. " + controllerHelper.getPath(), expectedPath, controllerHelper.getPath());
  }

  private JavaCode toJava(File file) {
    return (JavaCode)new SourceFile(file.getPath()).code();
  }

  private String somePackage() {
    return "com." + RANDOM.string(3);
  }

  private String someSourceSetDir() {
    return RANDOM.string(8) + File.separator + RANDOM.string(8) + File.separator + RANDOM.string(8);
  }

  private File find(Collection<File> files, String name) {
    for (File file : files) {
      if (file.getName().equals(name)) {
        return file;
      }
    }
    return null;
  }

  private Collection<File> generatedFiles() {
    Collection<File> result = new ArrayList<File>();
    addFilesIn(baseDir, result);
    return result;
  }

  private void addFilesIn(File file, Collection<File> files) {
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        addFilesIn(child, files);
      }
    } else if (file.isFile()) {
      files.add(file);
    }
  }

  private String expectedFilePath(String sourceSetDir, String packagePrefix, String packageName, File type) {
    return baseDir.getPath() + File.separator + sourceSetDir + File.separator
        + packagePrefix.replaceAll("\\.", '\\' + File.separator) + File.separator + packageName + File.separator
        + type.getName();
  }

}
