/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.core.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import radl.common.io.IO;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.code.SourceFile;
import radl.core.extraction.ResourceModelHolder;
import radl.core.extraction.ResourceModelImpl;
import radl.java.code.Java;
import radl.java.code.JavaCode;
import radl.java.extraction.FromJavaRadlExtractor;
import radl.java.generation.spring.RadlToSpringServer;
import radl.test.TestUtil;


@RunWith(Parameterized.class)
public class GenerateAndExtractTest {

  private static final File TESTS_DIR = new File(System.getProperty("radl.dir", "."), "specification/examples");
  private static final String RADL_FILE_EXTENSION = ".radl";

  @Parameters(name = "{0}")
  public static Iterable<String[]> tests() {
    Collection<String[]> result = new ArrayList<String[]>();
    for (String fileName : TESTS_DIR.list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(RADL_FILE_EXTENSION);
      }
    })) {
      result.add(new String[] { fileName.substring(0, fileName.length() - RADL_FILE_EXTENSION.length()) });
    }
    return result;
  }

  @Parameter
  public String example;
  private File radlFile;
  private File outputDir;
  private String testName;

  @Rule
  public TestRule watcher = new TestWatcher() {
    @Override
    protected void starting(Description description) {
      testName = description.getMethodName();
    }
  };

  @Before
  public void init() {
    radlFile = new File(TESTS_DIR, example + RADL_FILE_EXTENSION);
    outputDir = new File(String.format("build/integration-tests/%s/%s/%s", getClass().getSimpleName(), example,
        testName));
    IO.delete(outputDir);
    outputDir.mkdirs();
    ResourceModelHolder.INSTANCE.set(new ResourceModelImpl());
  }

  @Test
  public void extractedRadlFromGeneratedCodeMatchesOriginalRadl() throws Exception {
    File generatedSpringCodeDir = new File(outputDir, "spring");
    String generatedSpringCodePackagePrefix = String.format("radl.example.%s.rest",
        Java.toIdentifier(example).toLowerCase(Locale.getDefault()));
    File generatedRadlFile = new File(outputDir, radlFile.getName());
    File argumentsFile = extractionArgumentsFile(generatedSpringCodeDir, generatedRadlFile);

    generateCodeFromRadl(generatedSpringCodeDir, generatedSpringCodePackagePrefix, "");
    extractRadlFromCode(argumentsFile.getPath());
    compareOriginalWithGeneratedRadl(generatedRadlFile);
  }

  private File extractionArgumentsFile(File generatedSpringCodeDir, File generatedRadlFile) throws IOException {
    File argumentsFile = new File(outputDir, "extract.arguments");
    PrintWriter writer = new PrintWriter(argumentsFile, "UTF-8");
    try {
      writer.println("service.name = " + example);
      writer.println("base.dir = " + fileToPropertiesPath(generatedSpringCodeDir.getPath()));
      writer.println("radl.file = " + fileToPropertiesPath(generatedRadlFile.getPath()));
      writer.println("classpath = " + getClassPath());
      writer.println("java.version = " + Java.getVersion());
    } finally {
      writer.close();
    }
    return argumentsFile;
  }

  private String fileToPropertiesPath(String file) {
    return file.replaceAll("\\" + File.separator, "/");
  }

  private String getClassPath() {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    ClassLoader cl = ClassLoader.getSystemClassLoader();
    URL[] urls = ((URLClassLoader)cl).getURLs();
    for(URL url: urls){
      result.append(prefix).append(fileToPropertiesPath(url.getFile()));
      prefix = File.pathSeparator;
    }
    return result.toString();
  }

  private void generateCodeFromRadl(File generatedSpringCodeDir, String generatedSpringCodePackagePrefix,
      String header) throws Exception {
    run(RadlToSpringServer.class, radlFile.getPath(), generatedSpringCodeDir.getPath(),
        generatedSpringCodePackagePrefix, "build/src/java", "src/main/java", "default", header);
  }

  private void run(Class<? extends Application> applicationClass, String... arguments) throws Exception {
    int exitCode = applicationClass.newInstance().run(new Arguments(arguments));
    assertEquals("Exit code", 0, exitCode);
  }

  private void extractRadlFromCode(String argumentsPath) throws Exception {
    run(JavaCompiler.class, argumentsPath);
    run(FromJavaRadlExtractor.class, "@" + argumentsPath);
  }

  private void compareOriginalWithGeneratedRadl(File generatedRadlFile) throws Exception {
    run(RadlComparer.class, radlFile.getPath(), generatedRadlFile.getPath());
  }

  @Test
  public void generatesCodeWithConfiguredHeader() throws Exception {
    File generatedSpringCodeDir = new File(outputDir, "spring");
    String generatedSpringCodePackagePrefix = String.format("radl.example.%s.rest", Java.toIdentifier(example));
    String header = "First header line.\nSecond line.";

    generateCodeFromRadl(generatedSpringCodeDir, generatedSpringCodePackagePrefix, header);

    for (File file : collectFilesIn(generatedSpringCodeDir)) {
      JavaCode code = (JavaCode)new SourceFile(file.getPath()).code();
      TestUtil.assertCollectionEquals("Header for " + file.getPath(), Arrays.asList(header.split("\n")),
          code.fileComments());
    }
  }

  private Iterable<File> collectFilesIn(File dir) {
    Collection<File> result = new ArrayList<File>();
    collectFilesIn(dir, result);
    return result;
  }

  private void collectFilesIn(File dir, Collection<File> files) {
    for (File child : dir.listFiles()) {
      if (child.isDirectory()) {
        collectFilesIn(child, files);
      } else if (child.isFile()) {
        files.add(child);
      }
    }
  }

}
