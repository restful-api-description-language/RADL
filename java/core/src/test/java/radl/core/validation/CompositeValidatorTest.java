/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import radl.common.io.IO;
import radl.common.io.StringStream;
import radl.test.RandomData;


public class CompositeValidatorTest {

  private static final RandomData RANDOM = new RandomData();

  @Test
  public void callsAllComponents() throws IOException {
    Validator v1 = mock(Validator.class);
    Validator v2 = mock(Validator.class);
    Validator composite = new CompositeValidator(v1, v2);
    Collection<Issue> issues = new ArrayList<Issue>();
    AtomicReference<byte[]> bytes1 = captureValidatedBytes(v1, issues);
    AtomicReference<byte[]> bytes2 = captureValidatedBytes(v2, issues);
    String contents = RANDOM.string();

    try (InputStream stream = new StringStream(contents)) {
      composite.validate(stream, issues);
    }

    assertArrayEquals("Bytes supplied to child 1", contents.getBytes("UTF8"), bytes1.get());
    assertArrayEquals("Bytes supplied to child 2", bytes1.get(), bytes2.get());
  }

  private AtomicReference<byte[]> captureValidatedBytes(Validator validator, Collection<Issue> issues) {
    final AtomicReference<byte[]> result = new AtomicReference<byte[]>();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        result.set(readBytesFrom(invocation));
        return null;
      }
    }).when(validator).validate(any(InputStream.class), eq(issues));
    return result;
  }

  private byte[] readBytesFrom(InvocationOnMock invocation) throws IOException {
    byte[] bytes;
    InputStream found = (InputStream)invocation.getArguments()[0];
    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      IO.copy(found, out);
      bytes = out.toByteArray();
    }
    return bytes;
  }

}
