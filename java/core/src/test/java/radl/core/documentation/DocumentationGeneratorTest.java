/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;

import radl.common.io.IO;
import radl.core.Radl;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.test.RandomData;
import radl.test.TestUtil;


public class DocumentationGeneratorTest {

  private static final RandomData RANDOM = new RandomData();

  private final Application generator = new DocumentationGenerator();
  private final File dir = TestUtil.randomDir(DocumentationGeneratorTest.class);

  @After
  public void done() {
    IO.delete(dir);
  }

  @Test
  public void createsProvidedDirectory() throws Exception {
    IO.delete(dir);

    generateClientDocumentation();

    assertTrue("Dir not created", dir.exists());
  }

  private void generateClientDocumentation(String... radlFiles) {
    generator.run(new Arguments(new String[] { dir.getPath() }, radlFiles));
  }

  @Test
  public void createsIndexInProvidedDirectory() throws Exception {
    String serviceName = createRadl();
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  private String createRadl() throws FileNotFoundException {
    String result = RANDOM.string(8);
    File radl = new File(dir, result + ".radl");
    try {
      PrintWriter writer = new PrintWriter(radl, "UTF8");
      try {
        writer.println(String.format("<service xmlns='%s' name='%s'><states/></service>", Radl.NAMESPACE_URI, result));
      } finally {
        writer.close();
      }
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    generateClientDocumentation(radl.getPath());
    return result;
  }

  @Test
  @Ignore("Enable when we move to Java 7")
  public void createsStateDiagramInProvidedDirectory() throws FileNotFoundException {
    String serviceName = createRadl();
    File stateDiagramFile = new File(new File(dir, serviceName), "states.png");

    assertTrue("State diagram image not generated", stateDiagramFile.exists());
  }

}
