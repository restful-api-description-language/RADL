/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import org.jsoup.nodes.Document;
import org.junit.Assert;


public class AssertEquals implements Assertion {

  private final String message;
  private final Value expectation;
  private final Value actualization;

  public AssertEquals(String message, Value expected, Value actual) {
    this.message = message;
    this.expectation = expected;
    this.actualization = actual;
  }

  @Override
  public void verify(Document document) {
    String expected = expectation.get(document);
    String actual = actualization.get(document);
    Assert.assertEquals(message,  expected, actual);
  }

  @Override
  public String toString() {
    return message + ": " + expectation + " == " + actualization;
  }

}
