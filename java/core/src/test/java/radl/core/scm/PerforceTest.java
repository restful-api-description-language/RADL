/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.scm;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import org.junit.Test;

import radl.common.io.IO;
import radl.test.TestUtil;


public class PerforceTest {

  private final OperatingSystem operatingSystem = mock(OperatingSystem.class);
  private final SourceCodeManagementSystem p4 = new Perforce(operatingSystem);

  @Test
  public void returnsId() {
    assertEquals("ID", "p4", p4.getId());
  }

  @Test
  public void editsReadOnlyFile() throws IOException {
    File file = someFile();
    try {
      file.setReadOnly();

      p4.prepareForUpdate(file);

      verify(operatingSystem).run("p4 edit " + file.getAbsolutePath());
    } finally {
      IO.delete(file);
    }
  }

  private File someFile() throws IOException {
    File result = TestUtil.randomFile(getClass(), ".p4");
    PrintWriter writer = new PrintWriter(result, "UTF8");
    try {
      writer.println();
    } finally {
      writer.close();
    }
    return result;
  }

  @Test
  public void doesntEditWritableFile() throws IOException {
    File file = someFile();
    try {
      p4.prepareForUpdate(file);

      verify(operatingSystem, never()).run(anyString());
    } finally {
      IO.delete(file);
    }
  }

  @Test
  public void doesntDeleteWritableFile() throws IOException {
    File file = someFile();
    try {
      p4.prepareForDelete(file);

      verify(operatingSystem, never()).run(anyString());
    } finally {
      IO.delete(file);
    }
  }

  @Test
  public void deletesReadOnlyFile() throws IOException {
    File file = someFile();
    try {
      file.setReadOnly();

      p4.prepareForDelete(file);

      verify(operatingSystem).run("p4 delete " + file.getAbsolutePath());
    } finally {
      IO.delete(file);
    }
  }

}
