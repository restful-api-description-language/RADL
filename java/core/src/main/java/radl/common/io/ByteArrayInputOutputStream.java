/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.common.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;


/**
 * Combination of {@linkplain ByteArrayInputStream} and {@linkplain ByteArrayOutputStream} that share a byte buffer to
 * avoid copying.
 */
public class ByteArrayInputOutputStream extends ByteArrayOutputStream {

  public InputStream getInputStream() {
    return new ByteArrayInputStream(buf, 0, count);
  }

}
