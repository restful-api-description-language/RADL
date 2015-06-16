package radl.core.integration;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.java.code.Java;
import radl.java.extraction.FromJavaRadlExtractor;
import radl.java.generation.spring.RadlToSpringServer;


@RunWith(Parameterized.class)
public class GenerateAndExtractTest {

  private static final File TESTS_DIR = new File(System.getProperty("radl.dir", "."), "specification/examples");
  private static final String RADL_FILE_EXTENSION = ".radl";
  private static final String CLASSPATH = System.getProperty("classpath");

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

  @Before
  public void init() {
    radlFile = new File(TESTS_DIR, example + RADL_FILE_EXTENSION);
    outputDir = new File(String.format("build/integrationTest/%s/%s", getClass().getSimpleName(), example));
  }

  @Test
  public void extractedRadlFromGeneratedCodeMatchesOriginalRadl() throws Exception {
    File generatedSpringCodeDir = new File(outputDir, "spring");
    String generatedSpringCodePackagePrefix = String.format("radl.example.%s.rest", Java.toIdentifier(example));
    File generatedRadlFile = new File(outputDir, radlFile.getName());
    File argumentsFile = extractionArgumentsFile(generatedSpringCodeDir, generatedRadlFile);

    generateCodeFromRadl(generatedSpringCodeDir, generatedSpringCodePackagePrefix);
    extractRadlFromCode(argumentsFile);
    // TODO: Make this work: compareOriginalWithGeneratedRadl(generatedRadlFile);
  }

  private File extractionArgumentsFile(File generatedSpringCodeDir, File generatedRadlFile) throws IOException {
    File argumentsFile = new File(outputDir, "extract.arguments");
    PrintWriter writer = new PrintWriter(argumentsFile, "UTF-8");
    try {
      writer.println("service.name = " + example);
      writer.println("base.dir = " + generatedSpringCodeDir.getPath());
      writer.println("radl.file = " + generatedRadlFile.getPath().replaceAll("\\" + File.separator, "/"));
      writer.println("classpath = " + CLASSPATH);
      writer.println("java.version = " + Java.getVersion());
    } finally {
      writer.close();
    }
    return argumentsFile;
  }

  private void generateCodeFromRadl(File generatedSpringCodeDir, String generatedSpringCodePackagePrefix)
      throws Exception {
    run(RadlToSpringServer.class, radlFile.getPath(), generatedSpringCodeDir.getPath(),
        generatedSpringCodePackagePrefix);
  }

  private void run(Class<? extends Application> applicationClass, String... arguments) throws Exception {
    int exitCode = applicationClass.newInstance().run(new Arguments(arguments));
    assertEquals("Exit code", 0, exitCode);
  }

  private void extractRadlFromCode(File argumentsFile) throws Exception {
    run(FromJavaRadlExtractor.class, "@" + argumentsFile.getPath());
  }

  /*
  private void compareOriginalWithGeneratedRadl(File generatedRadlFile) throws Exception {
    run(RadlDiff.class, radlFile.getPath(), generatedRadlFile.getPath());
  }
  */

}
