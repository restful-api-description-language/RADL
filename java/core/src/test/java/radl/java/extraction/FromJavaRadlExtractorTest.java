/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;

import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import radl.common.io.IO;
import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.Radl;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.extraction.ResourceModel;
import radl.core.extraction.ResourceModelMerger;
import radl.test.RandomData;
import radl.test.TestUtil;


public class FromJavaRadlExtractorTest {

  private static final RandomData RANDOM = new RandomData();

  private final File dir = TestUtil.randomDir(FromJavaRadlExtractorTest.class);
  private final ResourceModelMerger merger = mock(ResourceModelMerger.class);
  private final ResourceModel resourceModel = mock(ResourceModel.class);
  private final Application radlExtractor = new FromJavaRadlExtractor(merger, resourceModel);

  @After
  public void done() {
    IO.delete(dir);
  }

  @Test
  public void appliesConfiguration() throws IOException {
    Properties properties = randomProperties();
    File radlFile = TestUtil.randomFile(FromJavaRadlExtractorTest.class, ".radl");
    File configurationFile = TestUtil.randomFile(FromJavaRadlExtractorTest.class, ".properties");
    try (PrintWriter writer = new PrintWriter(configurationFile, "UTF8")) {
      properties.store(writer, "");
    }

    radlExtractor.run(new Arguments(new String[] { "", dir.getPath(), radlFile.getAbsolutePath(),
        configurationFile.getAbsolutePath() }));

    verify(resourceModel).configure(eq(properties));
  }

  private Properties randomProperties() {
    Properties result = new Properties();
    int numProperties = RANDOM.integer(2, 10);
    for (int i = 0; i < numProperties; i++) {
      result.put(RANDOM.string(), RANDOM.string());
    }
    return result;
  }

  @Test
  public void appliesConfigurationFromArgumentsFile() throws IOException {
    Properties properties = randomProperties();
    File radlFile = TestUtil.randomFile(FromJavaRadlExtractorTest.class, ".radl");
    File configurationFile = TestUtil.randomFile(FromJavaRadlExtractorTest.class, ".properties");
    try (PrintWriter writer = new PrintWriter(configurationFile, "UTF8")) {
      properties.store(writer, "");
    }
    File argumentsFile = TestUtil.randomFile(FromJavaRadlExtractorTest.class, ".arguments");
    try (PrintWriter writer = new PrintWriter(argumentsFile, "UTF8")) {
      writer.println("base.dir = " + encodePathForPropertiesFile(dir));
      writer.println("radl.file = " + encodePathForPropertiesFile(radlFile));
      writer.println("configuration.file = " + encodePathForPropertiesFile(configurationFile));
    }

    try {
      radlExtractor.run(new Arguments(new String[] { '@' + argumentsFile.getAbsolutePath() }));
    } catch (IllegalArgumentException e) {
      assertEquals("Exception", "Compilation failed", e.getMessage());
    }

    verify(resourceModel).configure(eq(properties));
  }

  private String encodePathForPropertiesFile(File file) {
    return file.getAbsolutePath().replace('\\', '/');
  }

  @Test
  public void keepsManuallyAddedInformation() throws Exception {
    String oldServiceName = aName();
    String newServiceName = aName();
    File radlFile = TestUtil.randomFile(FromJavaRadlExtractorTest.class, ".radl");
    try (PrintWriter writer = new PrintWriter(radlFile, "UTF8")) {
      writer.println(Xml.toString(simpleRadlDocument(oldServiceName)));
    }
    when(merger.toRadl(resourceModel)).thenReturn(simpleRadlDocument(newServiceName));

    radlExtractor.run(new Arguments(new String[] { newServiceName, dir.getPath(), radlFile.getAbsolutePath() }));

    TestUtil.assertXmlEquals("RADL", simpleRadlDocument(newServiceName), Xml.parse(radlFile));
  }

  private String aName() {
    return RANDOM.string(5);
  }

  private Document simpleRadlDocument(String serviceName) {
    return DocumentBuilder.newDocument()
        .namespace(Radl.NAMESPACE_URI)
        .element("service")
            .attribute("name", serviceName)
            .element("states")
        .build();
  }

}
