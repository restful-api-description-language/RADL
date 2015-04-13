/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Test;

import radl.common.io.IO;
import radl.common.io.StringStream;
import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.Radl;
import radl.core.validation.Issue.Level;
import radl.test.RandomData;
import radl.test.TestUtil;


public class LintValidatorTest {

  private static final RandomData RANDOM = new RandomData();

  private final Validator validator = new LintValidator();
  private final Collection<Issue> issues = new ArrayList<Issue>();
  private final File file = TestUtil.randomFile(LintValidatorTest.class, ".radl");
  private final String service = RANDOM.string(3);
  private final DocumentBuilder radl = DocumentBuilder.newDocument()
      .namespace(Radl.NAMESPACE_URI)
      .element("service")
          .attribute("name", service);

  @After
  public void done() {
    IO.delete(file.getParentFile());
  }

  @Test
  public void warnsOnMissingStartState() throws FileNotFoundException {
    assertIssues(warning("Missing start-state"));

    issues.clear();
    radl.element("states");
    assertIssues(warning("Missing start-state"));
  }

  private Issue warning(String message) {
    return issue(Level.WARNING, message);
  }

  private Issue issue(Level level, String message) {
    return new Issue(LintValidator.class, level, 0, 0, message);
  }

  private void assertIssues(Issue... expected) {
    validate();
    for (Issue issue : expected) {
      assertTrue("Missing issue: " + issue + "\nGot: " + issues, issues.contains(issue));
    }
  }

  private void validate() {
    validator.validate(new StringStream(Xml.toString(radl.build())), issues);
  }

  @Test
  public void warnsOnDisconnectedState() {
    String state = someName();
    radl.element("states")
        .element("state")
            .attribute("name", state);

    assertIssues(warning("State '" + state + "' is not connected to any other state"));
  }

  private String someName() {
    return RANDOM.string(8);
  }

  @Test
  public void warnsOnTransitionToUndefinedState() {
    String state = someName();
    String transition = someName();
    String missingState = someName();
    radl.element("states")
        .element("state")
            .attribute("name", state)
            .element("transitions")
                .element("transition")
                    .attribute("name", transition)
                    .attribute("to", missingState);

    assertIssues(warning("Transition '" + transition + "' in state '" + state + "' points to undefined state '"
        + missingState + "'"));
  }

  @Test
  public void doesntReportIssuesOnValidRadl() {
    String state1 = someName();
    String state2 = someName();
    String transition = someName();
    String initialTransition = "initialT" + someName();
    String mediaType = someName() + '/' + someName();
    String linkRelation = someName();
    String baseUri = someUri();
    String part = someName();
    String variable = '{' + someName() + '}';
    String resource1 = 'a' + someName();
    String uri1 = baseUri + part;
    String resource2 = 'z' + someName();
    String uri2 = baseUri + variable;
    String method1 = someName();
    String method2 = someName();
    radl.element("states")
        .element("start-state")
            .element("transitions")
                .element("transition")
                    .attribute("name", initialTransition)
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
            .attribute("name", linkRelation)
            .element("transitions")
                .element("transition")
                    .attribute("ref", transition)
                .end()
            .end()
        .end()
    .end()
    .element("media-types")
        .element("media-type")
            .attribute("name", mediaType)
        .end()
    .end()
    .element("resources")
        .element("resource")
            .attribute("name", resource1)
            .element("location")
                .attribute("uri", uri1)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method1)
                    .element("transitions")
                        .element("transition")
                            .attribute("ref", transition)
                        .end()
                    .end()
                    .element("response")
                        .element("representations")
                            .element("representation")
                                .attribute("media-type", mediaType)
                            .end()
                        .end()
                    .end()
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", resource2)
            .element("location")
                .attribute("uri-template", uri2)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method2)
                    .element("transitions")
                        .element("transition")
                            .attribute("ref", initialTransition)
                        .end()
                    .end()
                    .element("response")
                        .element("representations")
                            .element("representation")
                                .attribute("media-type", mediaType)
                            .end()
                        .end()
                    .end();

    assertNoIssues();
  }

  private String someUri() {
    return '/' + someName() + '/' + someName() + '/';
  }

  private void assertNoIssues() {
    validate();
    assertTrue("Unexpected issues: " + issues, issues.isEmpty());
  }

  @Test
  public void warnsOnUnimplementedTransition() throws Exception {
    String transition = someName();
    radl.element("states")
        .element("start-state")
            .element("transitions")
                .element("transition")
                    .attribute("name", transition);

    assertIssues(warning("Transition '" + transition + "' is not implemented by a method"));
  }

  @Test
  public void warnsOnMissingLocation() throws Exception {
    String resource = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource);

    assertIssues(warning("Resource '" + resource + "' has no location"));
  }

  @Test
  public void warnsOnResourceWithoutMethod() throws Exception {
    String resource = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource);

    assertIssues(warning("Resource '" + resource + "' has no methods"));
  }

  @Test
  public void warnsOnMethodThatDoesntImplementATransition() throws Exception {
    String resource = someName();
    String method = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("methods")
                .element("method")
                    .attribute("name", method);

    assertIssues(warning("Method '" + method + "' in resource '" + resource + "' implements no transitions"));
  }

  @Test
  public void warnsOnMethodThatImplementsUndefinedTransition() throws Exception {
    String resource = someName();
    String method = someName();
    String transition = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("methods")
                .element("method")
                    .attribute("name", method)
                    .element("transitions")
                        .element("transition")
                            .attribute("ref", transition);

    assertIssues(warning("Method '" + method + "' in resource '" + resource + "' implements undefined transition '"
        + transition + "'"));
  }

  @Test
  public void warnsOnMethodWithoutRequestAndResponse() throws Exception {
    String resource = someName();
    String method = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("methods")
                .element("method")
                    .attribute("name", method);

    assertIssues(warning("Method '" + method + "' in resource '" + resource
        + "' has neither a request nor a response representation"));
  }

  @Test
  public void doesntWarnOnDeleteWithoutRequestAndResponse() throws Exception {
    String state = someName();
    String transition = someName();
    String resource = someName();
    String method = "DELETE";
    radl.element("states")
        .element("start-state")
            .attribute("name", RANDOM.string())
            .element("transitions")
                .element("transition")
                    .attribute("name", transition)
                    .attribute("to", state)
                .end()
            .end()
        .end()
        .element("state")
            .attribute("name", state)
        .end()
    .end()
    .element("link-relations")
        .element("link-relation")
            .attribute("name", someName())
            .element("transitions")
                .element("transition")
                    .attribute("ref", transition)
                .end()
            .end()
        .end()
    .end()
    .element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("location")
                .attribute("uri", someUri())
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method)
                    .element("transitions")
                        .element("transition")
                            .attribute("ref", transition);

    assertNoIssues();
  }

  @Test
  public void warnsOnUndefinedConsumedMediaType() throws Exception {
    assertWarnsOnUndefinedMediaTypeInMessage("request", "consumes");
  }

  private void assertWarnsOnUndefinedMediaTypeInMessage(String messageType, String messageAction) {
    String resource = someName();
    String method = someName();
    String mediaType = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("methods")
                .element("method")
                    .attribute("name", method)
                    .element(messageType)
                        .element("representations")
                            .element("representation")
                                .attribute("media-type", mediaType);

    assertIssues(warning("Method '" + method + "' in resource '" + resource + "' " + messageAction
        + " undefined media type '" + mediaType + "'"));
  }

  @Test
  public void warnsOnUndefinedProducedMediaType() throws Exception {
    assertWarnsOnUndefinedMediaTypeInMessage("response", "produces");
  }

  @Test
  public void warnsOnUndiscoverableTransition() throws Exception {
    String transition = someName();
    radl.element("states")
        .element("state")
            .attribute("name", someName())
            .element("transitions")
                .element("transition")
                    .attribute("name", transition);

    assertIssues(warning("Transition '" + transition + "' is not discoverable by a link relation"));
  }

  @Test
  public void warnsOnUndefinedTransition() throws Exception {
    String transition = someName();
    String linkRelation = someName();
    radl.element("link-relations")
        .element("link-relation")
            .attribute("name", linkRelation)
            .element("transitions")
                .element("transition")
                    .attribute("ref", transition);

    assertIssues(warning("Link relation '" + linkRelation + "' makes undefined transition '" + transition
        + "' discoverable"));
  }

  @Test
  public void warnsOnUnconnectedStartState() {
    radl.element("states")
        .element("start-state")
            .element("transitions");

    assertIssues(warning("Start state has no transitions"));
  }

  @Test
  public void warnsOnInitialTransitionToUndefinedState() {
    String transition = someName();
    String missingState = someName();
    radl.element("states")
        .element("start-state")
            .element("transitions")
                .element("transition")
                    .attribute("name", transition)
                    .attribute("to", missingState);

    assertIssues(warning("Transition '" + transition + "' in start state points to undefined state '"
        + missingState + "'"));
  }

  @Test
  public void reportsErrorOnResourcesWithTheSameLocation() throws Exception {
    String resource1 = 'a' + someName();
    String resource2 = 'z' + someName();
    String uri = someUri();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource1)
            .element("location")
                .attribute("uri", uri)
            .end()
        .end()
        .element("resource")
        .attribute("name", resource2)
        .element("location")
            .attribute("uri", uri);

    assertIssues(error("Resources '" + resource1 + "' and '" + resource2 + "' have the same location: " + uri));
  }

  private Issue error(String message) {
    return issue(Level.ERROR, message);
  }

  @Test
  public void warnsOnActionUri() {
    String resource = 'a' + someName();
    String uri = someUri() + "execute/" + someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("location")
                .attribute("uri", uri);

    assertIssues(warning("Location of '" + resource + "' contains action: " + uri));
  }

  @Test
  public void warnsOnDuplicateVariableInUriTemplate() {
    String resource = someName();
    String variable = someName();
    String uri = someUri() + "{" + variable + "}/" + someName() + "/{" + variable + "}";
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("location")
                .attribute("uri", uri);

    assertIssues(warning("URI Template of '" + resource + "' contains duplicate variable '" + variable + "': " + uri));
  }

  @Test
  public void warnsOnRepeatedPartInUri() {
    String resource = someName();
    String part = someName();
    String uri = someUri() + part + "/" + part;
    radl.element("resources")
        .element("resource")
            .attribute("name", resource)
            .element("location")
                .attribute("uri", uri);

    assertIssues(warning("Location of '" + resource + "' contains duplicate part '" + part + "': " + uri));
  }

  @Test
  public void warnsOnOverlappingUriAndUriTemplate() {
    String baseUri = someUri();
    String part = someName();
    String variable = '{' + someName() + '}';
    String resource1 = 'a' + someName();
    String uri1 = baseUri + part;
    String resource2 = 'z' + someName();
    String uri2 = baseUri + variable;
    String method = someName();
    radl.element("resources")
        .element("resource")
            .attribute("name", resource1)
            .element("location")
                .attribute("uri", uri1)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", resource2)
            .element("location")
                .attribute("uri-template", uri2)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method);

    assertIssues(warning("Locations of '" + resource1 + "' and '" + resource2 + "' overlap with fixed part '" + part
        + "' and variable part '" + variable + "':\n" + uri1 + "\n" + uri2));
  }

  @Test
  public void reportsErrorOnDuplicateResource() {
    assertErrorOnDuplicateItem("resource");
  }

  protected void assertErrorOnDuplicateItem(String type) {
    String name = someName();
    radl.element(type + "s")
        .element(type)
            .attribute("name", name)
        .end()
        .element(type)
            .attribute("name", name);

    assertIssues(error("Duplicate " + type + ": '" + name + "'"));
  }

  @Test
  public void reportsErrorOnDuplicateLinkRelation() {
    assertErrorOnDuplicateItem("link-relation");
  }

  @Test
  public void reportsErrorOnDuplicateMediaType() {
    assertErrorOnDuplicateItem("media-type");
  }

  @Test
  public void reportsErrorOnDuplicateState() {
    assertErrorOnDuplicateItem("state");
  }

}
