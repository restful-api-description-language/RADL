/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code.xml;


/**
 * Indentation for XML files.
 */
public class XmlIndent /* TODO: Java 7 implements AutoCloseable */ {

  private static final int INDENTATION = 2;

  private final XmlCode xml;

  public XmlIndent(XmlCode xml) {
    this.xml = xml;
    xml.indent(INDENTATION);
  }

  // TODO: Java 7 @Override
  public void close() {
    xml.unindent(INDENTATION);
  }

}
