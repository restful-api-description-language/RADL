/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.maven.util;

import java.io.File;
import java.io.FilenameFilter;

import org.apache.maven.plugin.MojoFailureException;

/**
 * An utility to process RADL files.
 */
public final class RadlFileUtil {
  private static final String RADL_EXT = ".radl";

  private RadlFileUtil() {
  }

  public static File findRadlFile(File dir, String suggestedName)
      throws MojoFailureException {
    if (dir == null || !dir.isDirectory()) {
      throw new MojoFailureException("The RADL directory is incorrect: " + dir);
    }
    File radlFile = new File(dir, suggestedName + RADL_EXT);
    if (!radlFile.exists()) {
      //iterate to find a first .radl file
      File[] files = dir.listFiles(new FilenameFilter() {
        public boolean accept(File dir, String name) {
          return name.endsWith(RADL_EXT);
        }
      });
      if (files != null && files.length > 0) {
        radlFile = files[0];
      }
    }
    if (!radlFile.exists()) {
      throw new MojoFailureException("No RADL files (.radl) are found under directory: " + dir);
    }
    return radlFile;
  }
}
