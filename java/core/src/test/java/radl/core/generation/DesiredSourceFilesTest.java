/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;

import radl.common.io.IO;
import radl.common.xml.DocumentBuilder;
import radl.core.code.SourceFile;
import radl.core.enforce.Desired;
import radl.test.RandomData;
import radl.test.TestUtil;


public class DesiredSourceFilesTest {

  private static final RandomData RANDOM = new RandomData();

  private final Document radl = DocumentBuilder.newDocument().build();
  private final SourceFilesGenerator generator = mock(SourceFilesGenerator.class);
  private final String basePath = TestUtil.randomDir(DesiredSourceFilesTest.class).getPath() + File.separator;
  private final Desired<String, SourceFile> desired = new DesiredSourceFiles(radl, generator, new File(basePath));

  @After
  public void done() {
    IO.delete(new File(basePath));
  }

  @Test
  public void returnsPathsOfSourceFilesAsIds() throws Exception {
    String path1 = "z" + RANDOM.string();
    String path2 = "a" + RANDOM.string();
    when(generator.generateFrom(radl, new File(basePath))).thenReturn(
        Arrays.asList(new SourceFile(path1), new SourceFile(path2)));
    List<String> expected = Arrays.asList(path2, path1);

    Collection<String> actual = desired.getIds();

    TestUtil.assertCollectionEquals("IDs", expected, actual);
  }

  @Test
  public void returnsSourceFile() throws Exception {
    SourceFile expected = new SourceFile(basePath + RANDOM.string());
    when(generator.generateFrom(radl, new File(basePath))).thenReturn(Arrays.asList(expected));

    SourceFile actual = desired.get(expected.path());

    Assert.assertEquals("Source file", expected, actual);
  }

}
