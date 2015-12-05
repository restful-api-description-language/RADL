/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.core.xml;

import java.io.File;

import radl.common.xml.Xml;
import radl.common.xml.XmlException;
import radl.core.Log;


public final class RadlFileAssembler {

  public static final String XINCLUDE_FIXUP_BASE_URI = "http://apache.org/xml/features/xinclude/fixup-base-uris";
  public static final String XINCLUDE_FIXUP_LANGUAGE = "http://apache.org/xml/features/xinclude/fixup-language";

  private RadlFileAssembler() {
    // Utility class
  }

  public static File assemble(File radlFile, File targetDirectory) {
    File result = createOutputFile(radlFile, targetDirectory);
    try {
      // Merge potentially several RADL files into a single file using identity transformation and XInclude-aware parser
      Xml.identityTransform(Xml.parseWithIncludes(radlFile), result);
    } catch (XmlException e) {
      Log.error("Failed to assemble RADL file: " + e.getMessage());
      return radlFile;
    }

    return result;
  }

  private static File createOutputFile(File radlFile, File targetDirectory) {
    String name = radlFile.getName();
    File dir = targetDirectory;
    if (dir == null) {
      dir = radlFile.getParentFile();
    } else {
      dir.mkdirs();
    }
    return new File(dir, name + ".out");
  }

}
