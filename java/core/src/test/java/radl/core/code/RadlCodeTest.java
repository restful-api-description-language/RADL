/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import radl.core.Radl;
import radl.core.code.radl.RadlCode;
import radl.test.RandomData;
import radl.test.TestUtil;


public class RadlCodeTest {

  private static final RandomData RANDOM = new RandomData();

  private final RadlCode radl = new RadlCode();
  private final String service = someName();

  private String someName() {
    return RANDOM.string(8);
  }

  @Test
  public void extractsService() {
    startService();
    endService();

    assertEquals("Service", service, radl.service());
  }

  private void startService() {
    radl.add("<service xmlns='%s' name='%s'>", Radl.NAMESPACE_URI, service);
  }

  private boolean endService() {
    return radl.add("</service>");
  }

  @Test
  public void extractsResourceNames() {
    String name1 = 'z' + someName();
    String name2 = 'a' + someName();
    startService();
    radl.add("  <resources>");
    radl.add("    <resource name='%s'/>", name1);
    radl.add("    <resource name='%s'/>", name2);
    radl.add("  </resources>");
    endService();

    TestUtil.assertCollectionEquals("Resource names", Arrays.asList(name2, name1), radl.resourceNames());
  }

  @Test
  public void extractsResourceMethods() throws Exception {
    String resource = someName();
    String method1 = 'z' + someName();
    String method2 = 'a' + someName();
    startService();
    radl.add("  <resources>");
    radl.add("    <resource name='%s'>", resource);
    radl.add("      <interface>");
    radl.add("        <methods>");
    radl.add("          <method name='%s'/>", method1);
    radl.add("          <method name='%s'/>", method2);
    radl.add("        </methods>");
    radl.add("      </interface>");
    radl.add("    </resource>");
    radl.add("  </resources>");
    endService();

    TestUtil.assertCollectionEquals("Method names", Arrays.asList(method2, method1), radl.methodNames(resource));
  }

  @Test
  public void extractsStateNames() {
    String state1 = 'z' + someName();
    String state2 = 'a' + someName();
    startService();
    radl.add("  <states>");
    radl.add("    <state name='%s'/>", state1);
    radl.add("    <state name='%s'/>", state2);
    radl.add("  </states>");
    endService();

    TestUtil.assertCollectionEquals("State names", Arrays.asList(state2, state1), radl.stateNames());
  }

  @Test
  public void extractsStateTransitions() {
    String state1 = 'z' + someName();
    String state2 = 'a' + someName();
    String transition = 'z' + someName();
    startService();
    radl.add("<states><state name='%s'><transitions>", state1);
    radl.add("<transition name='%s' to='%s'/>", transition, state2);
    radl.add("</transitions></state>");
    radl.add("<state name='%s'/></states>", state2);
    endService();

    TestUtil.assertCollectionEquals("State transitions", Arrays.asList(transition), radl.stateTransitionNames(state1));
  }

  @Test
  public void extractsOutgoingStatesFromTransitions() {
    String state1 = 'z' + someName();
    String state2 = 'a' + someName();
    String transition = 'z' + someName();
    startService();
    radl.add("<states><state name='%s'><transitions>", state1);
    radl.add("<transition name='%s' to='%s'/>", transition, state2);
    radl.add("</transitions></state>");
    radl.add("<state name='%s'/></states>", state2);
    endService();

    TestUtil.assertCollectionEquals("Transition ends", Arrays.asList(state2), radl.transitionEnds(transition));
  }

  @Test
  public void treatsTheStartStateAsAStateWithNoName() {
    String state = 'a' + someName();
    String transition = 'z' + someName();
    startService();
    radl.add("<states><start-state><transitions>");
    radl.add("<transition name='%s' to='%s'/>", transition, state);
    radl.add("</transitions></start-state>");
    radl.add("<state name='%s'/></states>", state);
    endService();

    TestUtil.assertCollectionEquals("States", Arrays.asList("", state), radl.stateNames());
    TestUtil.assertCollectionEquals("Start transitions", Arrays.asList(transition), radl.stateTransitionNames(""));
    TestUtil.assertCollectionEquals("Start transition end", Arrays.asList(state), radl.transitionEnds(transition));
  }

  @Test
  public void extractsMethodTransitions() throws Exception {
    String resource = someName();
    String method = someName();
    String transition1 = 'z' + someName();
    String transition2 = 'a' + someName();
    startService();
    radl.add("<resources><resource name='%s'>", resource);
    radl.add("<methods><method name='%s'><transitions>", method);
    radl.add("<transition ref='%s'/><transition ref='%s'/>", transition1, transition2);
    radl.add("</transitions></method></methods></resource></resources>");
    endService();

    TestUtil.assertCollectionEquals("Method transitions", Arrays.asList(transition2, transition1), radl.methodTransitions(resource, method));
  }

  @Test
  public void extractsResourceLocationFromUri() throws Exception {
    String resource = someName();
    String uri = someName();
    startService();
    radl.add("<resources><resource name='%s'><location uri='%s'/></resource></resources>", resource, uri);
    endService();

    assertEquals("Location", uri, radl.resourceLocation(resource));
  }

  @Test
  public void extractsResourceLocationFromUriTemplate() throws Exception {
    String resource = someName();
    String uriTemplate = someName();
    startService();
    radl.add("<resources><resource name='%s'><location uri-template='%s'/></resource></resources>", resource,
        uriTemplate);
    endService();

    assertEquals("Location", uriTemplate, radl.resourceLocation(resource));
  }

  @Test
  public void extractsMethodRequestRepresentations() throws Exception {
    String resource = someName();
    String method = someName();
    String mediaType1 = 'z' + someName();
    String mediaType2 = 'a' + someName();
    startService();
    radl.add("<resources><resource name='%s'><methods>", resource);
    radl.add("<method name='%s'>", method);
    radl.add("<request><representations>");
    radl.add("<representation media-type='%s'/>", mediaType1);
    radl.add("<representation media-type='%s'/>", mediaType2);
    radl.add("</representations></request></method>");
    radl.add("</methods></resource></resources>");
    endService();

    TestUtil.assertCollectionEquals("Representations", Arrays.asList(mediaType2, mediaType1), radl.methodRequestRepresentations(resource, method));
  }

  @Test
  public void extractsMethodResponseRepresentations() throws Exception {
    String resource = someName();
    String method = someName();
    String mediaType1 = 'z' + someName();
    String mediaType2 = 'a' + someName();
    startService();
    radl.add("<resources><resource name='%s'><methods>", resource);
    radl.add("<method name='%s'>", method);
    radl.add("<response><representations>");
    radl.add("<representation media-type='%s'/>", mediaType1);
    radl.add("<representation media-type='%s'/>", mediaType2);
    radl.add("</representations></response></method>");
    radl.add("</methods></resource></resources>");
    endService();

    TestUtil.assertCollectionEquals("Representations", Arrays.asList(mediaType2, mediaType1), radl.methodResponseRepresentations(resource, method));
  }

  @Test
  public void extractsMediaTypeNames() throws Exception {
    String mediaType1 = 'z' + someName();
    String mediaType2 = 'a' + someName();
    startService();
    radl.add("<media-types>");
    radl.add("<media-type name='%s'/>", mediaType1);
    radl.add("<media-type name='%s'/>", mediaType2);
    radl.add("</media-types>");
    endService();

    TestUtil.assertCollectionEquals("Media types", Arrays.asList(mediaType2, mediaType1), radl.mediaTypeNames());
  }

  @Test
  public void extractsLinkRelationNames() throws Exception {
    String linkRelation1 = 'z' + someName();
    String linkRelation2 = 'a' + someName();
    startService();
    radl.add("<link-relations>");
    radl.add("<link-relation name='%s'/>", linkRelation1);
    radl.add("<link-relation name='%s'/>", linkRelation2);
    radl.add("</link-relations>");
    endService();

    TestUtil.assertCollectionEquals("Link relations", Arrays.asList(linkRelation2, linkRelation1), radl.linkRelationNames());
  }

  @Test
  public void extractsLinkRelationTransitions() throws Exception {
    String linkRelation = someName();
    String transition1 = 'z' + someName();
    String transition2 = 'a' + someName();
    startService();
    radl.add("<link-relations><link-relation name='%s'><transitions>", linkRelation);
    radl.add("<transition ref='%s'/>", transition1);
    radl.add("<transition ref='%s'/>", transition2);
    radl.add("</transitions></link-relation></link-relations>");
    endService();

    TestUtil.assertCollectionEquals("Link relation transitions", Arrays.asList(transition2, transition1), radl.linkRelationTransitions(linkRelation));
  }

}
