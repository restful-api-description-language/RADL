/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import java.io.File;

import org.jsoup.nodes.Document;


public class DocumentationVerifier {

  private final Iterable<Assertion> assertions;

  public DocumentationVerifier(File testFile) throws Exception {
    assertions = new TestParser().parse(testFile);
  }

  public void verify(Document document) {
    for (Assertion assertion : assertions) {
      assertion.verify(document);
    }
  }

}
