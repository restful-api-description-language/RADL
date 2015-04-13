/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import radl.common.io.IO;
import radl.test.RandomData;
import radl.test.TestUtil;


public class JGraphStateDiagramTest {

  private static final RandomData RANDOM = new RandomData();

  private final String title = aName();
  private final StateDiagram std = new JGraphStateDiagram();
  private final File dir = TestUtil.randomDir(JGraphStateDiagramTest.class);

  public String aName() {
    return RANDOM.string(8);
  }

  @Before
  public void init() {
    std.setTitle(title);
  }

  @After
  public void done() {
    IO.delete(dir);
  }

  @Test
  @Ignore("Enable when we move to Java 7")
  public void convertsGraphToImageFile() {
    String from = aName();
    String intermediate = aName();
    String to = aName();
    String transition1 = aName();
    String transition2 = aName();
    std.addStartState(from);
    std.addState(intermediate);
    std.addState(to);
    std.addTransition(from, intermediate, transition1);
    std.addTransition(intermediate, to, transition2);
    File image = TestUtil.randomFile(JGraphStateDiagramTest.class, "png");

    std.toImage(image);

    assertTrue("Image not created", image.exists());
  }

}
