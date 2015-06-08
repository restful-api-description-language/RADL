/*
 * Copyright (c) 2015 EMC Corporation. All Rights Reserved.
 * EMC Confidential: Restricted Internal Distribution
 */
package radl.core.documentation;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.w3c.dom.Element;


public class SelectorValue implements Value {

  private final String element;
  private final String attribute;

  public SelectorValue(Element element) {
    this.element = element.getAttributeNS(null, "element");
    this.attribute = element.getAttributeNS(null, "attribute");
  }

  @Override
  public String get(Document document) {
    Elements elements = document.select(element);
    if (elements.isEmpty()) {
      return null;
    }
    if (attribute.isEmpty()) {
      return elements.first().text();
    }
    return elements.first().attr(attribute);
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    result.append("select(").append(element).append(')');
    if (attribute != null) {
      result.append("/@").append(attribute);
    }
    return result.toString();
  }

}
