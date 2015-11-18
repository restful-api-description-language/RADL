/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import radl.common.io.IO;
import radl.core.Radl;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.test.RandomData;
import radl.test.TestUtil;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class DocumentationGeneratorTest {

  private static final RandomData RANDOM = new RandomData();

  private final Application generator = new DocumentationGenerator();
  private File dir = TestUtil.randomDir(DocumentationGeneratorTest.class);

  @Before
  public void before() {
    dir = TestUtil.randomDir(DocumentationGeneratorTest.class);
  }

  @After
  public void done() {
    IO.delete(dir);
  }

  @Test
  public void createsProvidedDirectory() throws Exception {
    IO.delete(dir);

    generateClientDocumentation(null);
    assertTrue("Dir not created", dir.exists());
  }

  @Test
  public void createsIndexInProvidedDirectory() throws Exception {
    String serviceName = createRadl(null);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
   public void createsIndexWithLocalCSSFile() throws Exception {
    URL cssURL = getClass().getResource("radl-test.css");
    assertTrue("Local CSS file does not exist", new File(cssURL.getFile()).exists());
    String serviceName = createRadl(cssURL);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  public void createsIndexWithRemoteCSSFile() throws Exception {
    URL cssURL = new URL("https://raw.githubusercontent.com/asciidoctor/asciidoctor/master/data/stylesheets/asciidoctor-default.css");
    assertNotNull("Remote CSS file does not exist", cssURL.getContent());
    String serviceName = createRadl(cssURL);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  @Ignore("Enable when we move to Java 7")
  public void createsStateDiagramInProvidedDirectory() throws FileNotFoundException {
    String serviceName = createRadl(null);
    File stateDiagramFile = new File(new File(dir, serviceName), "states.png");

    assertTrue("State diagram image not generated", stateDiagramFile.exists());
  }

  private void generateClientDocumentation(String cssFile, String... radlFiles) {
    String[] args = new String[radlFiles.length + 2];
    args[0] = dir.getPath();
    args[1] = cssFile;
    System.arraycopy(radlFiles, 0, args, 2, radlFiles.length);
    generator.run(new Arguments(args));
  }

  private String createRadl(URL cssFile) throws FileNotFoundException {
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
    generateClientDocumentation(cssFile == null ? null : cssFile.toString(), radl.getPath());
    return result;
  }
}
