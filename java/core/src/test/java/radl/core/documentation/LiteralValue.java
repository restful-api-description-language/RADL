/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import org.jsoup.nodes.Document;
import org.w3c.dom.Element;


public class LiteralValue implements Value {

  private final String value;

  public LiteralValue(Element element) {
    value = element.getTextContent();
  }

  @Override
  public String get(Document document) {
    return value;
  }

  @Override
  public String toString() {
    return "'" + value + "'";
  }

}
