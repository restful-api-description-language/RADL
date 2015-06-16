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
import radl.core.extraction.ResourceModelHolder;
import radl.java.code.Java;
import radl.java.extraction.FromJavaRadlExtractor;
import radl.java.generation.spring.RadlToSpringServer;


@RunWith(Parameterized.class)
public class GenerateAndExtractTest {

  private static final File TESTS_DIR = new File(System.getProperty("radl.dir", "."), "specification/examples");
  private static final String RADL_FILE_EXTENSION = ".radl";
  private static final String CLASSPATH = System.getProperty("classpath", "");

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
    outputDir = new File(String.format("build/integration-tests/%s/%s", getClass().getSimpleName(), example));
    outputDir.mkdirs();
    ResourceModelHolder.setInstance(null);
  }

  @Test
  public void extractedRadlFromGeneratedCodeMatchesOriginalRadl() throws Exception {
    File generatedSpringCodeDir = new File(outputDir, "spring");
    String generatedSpringCodePackagePrefix = String.format("radl.example.%s.rest", Java.toIdentifier(example));
    File generatedRadlFile = new File(outputDir, radlFile.getName());
    File argumentsFile = extractionArgumentsFile(generatedSpringCodeDir, generatedRadlFile);

    generateCodeFromRadl(generatedSpringCodeDir, generatedSpringCodePackagePrefix);
    extractRadlFromCode(argumentsFile);
    compareOriginalWithGeneratedRadl(generatedRadlFile);
  }

  private File extractionArgumentsFile(File generatedSpringCodeDir, File generatedRadlFile) throws IOException {
    File argumentsFile = new File(outputDir, "extract.arguments");
    PrintWriter writer = new PrintWriter(argumentsFile, "UTF-8");
    try {
      writer.println("service.name = " + example);
      writer.println("base.dir = " + fileToPropertiesPath(generatedSpringCodeDir));
      writer.println("radl.file = " + fileToPropertiesPath(generatedRadlFile));
      writer.println("classpath = " + getClassPath());
      writer.println("java.version = " + Java.getVersion());
    } finally {
      writer.close();
    }
    return argumentsFile;
  }

  private String fileToPropertiesPath(File file) {
    return file.getPath().replaceAll("\\" + File.separator, "/");
  }

  private String getClassPath() {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String path : CLASSPATH.split("\\" + File.pathSeparator)) {
      result.append(prefix).append(fileToPropertiesPath(new File(path)));
      prefix = File.pathSeparator;
    }
    return result.toString();
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

  private void compareOriginalWithGeneratedRadl(File generatedRadlFile) throws Exception {
    run(RadlComparer.class, radlFile.getPath(), generatedRadlFile.getPath());
  }

}
