/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;

import radl.common.io.IO;
import radl.common.xml.DocumentBuilder;
import radl.core.Radl;
import radl.test.RandomData;
import radl.test.TestUtil;


public class StateDiagramGeneratorTest {

  private static final String NL = System.getProperty("line.separator");
  private static final RandomData RANDOM = new RandomData();
  private static final File DIR = TestUtil.randomDir(StateDiagramGeneratorTest.class);

  private final StateDiagramGenerator generator = new StateDiagramGenerator();

  @After
  public void done() {
    IO.delete(DIR);
  }

  @Test
  public void generatesImageFromStateDiagram() throws FileNotFoundException {
    String prefix = "$rel";
    File configuration = new File(someName() + ".properties");
    String linkRelationsUriPrefix = "urn:radl:" + someName();
    try (PrintWriter writer = new PrintWriter(configuration, "UTF8")) {
      writer.println("prefixes = " + prefix + '=' + linkRelationsUriPrefix);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    String service = someName();
    String homeState = "Start";
    String homeTransition = someTransition();
    String state1 = someState();
    String state2 = someState();
    String transition = someTransition();
    String linkRelation = someLinkRelation();
    String homeUri = "/";
    String homeMethod = "GET";
    String method = "POST";
    Document radl = DocumentBuilder.newDocument()
        .namespace(Radl.NAMESPACE_URI)
        .element("service")
            .attribute("name", service)
            .element("states")
                .element("start-state")
                    .element("transitions")
                        .element("transition")
                            .attribute("name", homeTransition)
                            .attribute("to", state1)
                        .end()
                    .end()
                .end()
                .element("state")
                    .attribute("name", state1)
                    .element("transitions")
                        .element("transition")
                            .attribute("name", transition)
                            .attribute("to", state2)
                        .end()
                    .end()
                .end()
                .element("state")
                    .attribute("name", state2)
                .end()
            .end()
            .element("link-relations")
                .element("link-relation")
                    .attribute("name", linkRelationsUriPrefix + linkRelation)
                    .element("transitions")
                        .element("transition")
                            .attribute("name", transition)
                        .end()
                    .end()
                .end()
            .end()
            .element("resources")
                .element("resource")
                    .element("location")
                        .attribute("uri", homeUri)
                    .end()
                    .element("methods")
                        .element("method")
                            .attribute("name", homeMethod)
                            .element("transitions")
                                .element("transition")
                                    .attribute("name", homeTransition)
                                .end()
                            .end()
                        .end()
                        .element("method")
                            .attribute("name", method)
                            .element("transitions")
                                .element("transition")
                                    .attribute("name", transition)
                                .end()
                            .end()
                        .end()
                    .end()
                .end()
            .end()
        .end()
    .build();
    File dir = new File(DIR, someName());
    File image = new File(dir, "states.png");
    StateDiagram diagram = mock(StateDiagram.class);
    try {
      generator.generateFrom(radl, diagram, dir, configuration);

      verify(diagram).addStartState(homeState);
      verify(diagram).addState(state1);
      verify(diagram).addTransition("Start", state1, homeMethod + ' ' + homeUri);
      verify(diagram).addState(state2);
      verify(diagram).addTransition(state1, state2, method + NL + prefix + linkRelation);
      verify(diagram).toImage(eq(image));
    } finally {
      IO.delete(configuration);
    }
  }

  private String someName() {
    return RANDOM.string(8);
  }

  private String someState() {
    return 'S' + someName();
  }

  private String someTransition() {
    return 'T' + someName();
  }

  private String someLinkRelation() {
    return 'L' + someName();
  }

}
