/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    generateClientDocumentation();
    assertTrue("Dir not created", dir.exists());
  }

  @Test
  public void createsIndexInProvidedDirectory() throws Exception {
    String serviceName = createRadl(null, null);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  public void createsIndexInProvidedDirectoryAndHideLocation() throws Exception {
    String serviceName = createRadl(null, true);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
   public void createsIndexWithLocalCSSFile() throws Exception {
    URL cssURL = getClass().getResource("radl-test.css");
    assertTrue("Local CSS file does not exist", new File(cssURL.getFile()).exists());
    String serviceName = createRadl(cssURL, null);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  public void createsIndexWithLocalCSSFileAndHideLocation() throws Exception {
    URL cssURL = getClass().getResource("radl-test.css");
    assertTrue("Local CSS file does not exist", new File(cssURL.getFile()).exists());
    String serviceName = createRadl(cssURL, true);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  public void createsIndexWithLocalCSSFileAndShowLocation() throws Exception {
    URL cssURL = getClass().getResource("radl-test.css");
    assertTrue("Local CSS file does not exist", new File(cssURL.getFile()).exists());
    String serviceName = createRadl(cssURL, false);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  public void createsIndexWithRemoteCSSFile() throws Exception {
    URL cssURL = new URL("https://raw.githubusercontent.com/asciidoctor/asciidoctor/master/data/stylesheets/asciidoctor-default.css");
    assertNotNull("Remote CSS file does not exist", cssURL.getContent());
    String serviceName = createRadl(cssURL, null);
    File indexFile = new File(new File(dir, serviceName), "index.html");

    assertTrue("Documentation not generated", indexFile.exists());
  }

  @Test
  @Ignore("Enable when we move to Java 7")
  public void createsStateDiagramInProvidedDirectory() throws FileNotFoundException {
    String serviceName = createRadl(null, null);
    File stateDiagramFile = new File(new File(dir, serviceName), "states.png");

    assertTrue("State diagram image not generated", stateDiagramFile.exists());
  }

  private String createRadl(URL cssFile, Boolean hideLocation) throws FileNotFoundException {
    String result = RANDOM.string(8);
    File radl = new File(dir, result + ".radl");
    try (PrintWriter writer = new PrintWriter(radl, "UTF8")) {
      writer.println(String.format("<service xmlns='%s' name='%s'><states/></service>", Radl.NAMESPACE_URI, result));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    generateClientDocumentation(cssFile, hideLocation, radl.getPath());
    return result;
  }

  private void generateClientDocumentation(URL cssFile, Boolean hideLocation, String... radlFiles) {
    List<String> args = new ArrayList<>();
    args.add(dir.toString());
    if (cssFile != null) {
      args.add(cssFile.toString());
    }
    if (hideLocation != null && hideLocation) {
      args.add("hide-location");
    }
    if (radlFiles != null) {
      args.addAll(Arrays.asList(radlFiles));
    }
    generator.run(new Arguments(args.toArray(new String[args.size()])));
  }

  private void generateClientDocumentation(String... radlFiles) {
    generateClientDocumentation(null, false, radlFiles);
  }
}
