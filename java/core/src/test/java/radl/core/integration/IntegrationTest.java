package radl.core.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import radl.core.cli.Arguments;
import radl.java.generation.spring.RadlToSpringServer;


@RunWith(Parameterized.class)
public class IntegrationTest {

  private static final File TESTS_DIR = new File(System.getProperty("radl.dir", "."),
      "specification/examples");

  @Parameters(name = "{0}")
  public static Iterable<File[]> tests() {
    Collection<File[]> result = new ArrayList<File[]>();
    for (File file : TESTS_DIR.listFiles()) {
      result.add(new File[] { file });
    }
    return result;
  }

  @Parameter
  public File radlFile;

  @Test
  public void extractedRadlFromGeneratedCodeMatchesOriginal() {
    String example = radlFile.getName();
    example = example.substring(0, example.lastIndexOf('.'));
    File exampleDir = new File("build/integrationTest", example);
    File baseDir = new File(exampleDir, "spring");
    new RadlToSpringServer().run(new Arguments(new String[] {
        radlFile.getPath(),
        baseDir.getPath(),
        "radl." + example.replaceAll("[^a-zA-Z0-9_]+", "_") + ".rest"
    }));
    // TODO: Move other code over from build.gradle - integrationTest
  }

}
