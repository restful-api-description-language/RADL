/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.xml;


public class XmlException extends Exception {

  public XmlException(Exception cause) {
    super(cause);
  }

  @Override
  public String getMessage() {
    return getCause().getMessage();
  }

}
