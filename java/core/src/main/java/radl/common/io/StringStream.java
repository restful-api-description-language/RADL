/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.io;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;


/**
 * Use a {@linkplain String} as an {@linkplain InputStream}.
 */
public class StringStream extends ByteArrayInputStream {

  public StringStream(String text) {
    this(text, "UTF8");
  }

  public StringStream(String text, String charSetName) {
    super(getBytes(text, charSetName));
  }

  private static byte[] getBytes(String text, String charSetName) {
    try {
      return text.getBytes(charSetName);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}
