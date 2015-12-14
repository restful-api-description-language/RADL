/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import radl.core.Log;
import radl.core.cli.Arguments;


@RunWith(Parameterized.class)
public class DocumentationTest {

  private static final File TESTS_DIR = new File(System.getProperty("radl.dir", "."),
      "specification/tests/documentation");
  private static final File BUILD_DIR = new File("build/tmp/doc");
  private static final String RADL_FILE = "instance";

  @Parameters(name = "{0}")
  public static Iterable<String[]> tests() {
    Collection<String[]> result = new ArrayList<>();
    for (String dir : TESTS_DIR.list()) {
      result.add(new String[] { dir });
    }
    return result;
  }

  @Parameter
  public String dir;

  @BeforeClass
  public static void init() {
    Log.deactivate();
  }

  @AfterClass
  public static void done() {
    Log.activate();
  }

  @Test
  public void testValidation() throws Exception {
    File testDir = new File(TESTS_DIR, dir);
    File radlFile = new File (testDir, RADL_FILE + ".xml");
    File docDir = new File(BUILD_DIR, dir);
    DocumentationVerifier documentationVerifier = new DocumentationVerifier(new File(testDir, "test.xml"));

    new DocumentationGenerator().run(new Arguments(new String[] {
        docDir.getAbsolutePath(),
        radlFile.getAbsolutePath()
    }));

    Document documentation = Jsoup.parse(new File(docDir, RADL_FILE + "/index.html"), "UTF-8", "");
    documentationVerifier.verify(documentation);
  }

}
