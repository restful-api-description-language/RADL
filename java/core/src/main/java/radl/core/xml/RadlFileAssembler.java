/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.core.xml;

import java.io.File;
import java.io.FileWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import radl.core.Log;

public final class RadlFileAssembler {

  public static final String XINCLUDE_FIXUP_BASE_URI = "http://apache.org/xml/features/xinclude/fixup-base-uris";
  public static final String XINCLUDE_FIXUP_LANGUAGE = "http://apache.org/xml/features/xinclude/fixup-language";

  private RadlFileAssembler() {
  }

  public static File assemble(File radlFile, File targetDirectory) {
    File fullRadlFile = createFullRadlFile(radlFile, targetDirectory);
    try {
      // merge RADL files into a single file using transformation
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setNamespaceAware(true);
      factory.setXIncludeAware(true);
      factory.setFeature(XINCLUDE_FIXUP_BASE_URI, false);
      factory.setFeature(XINCLUDE_FIXUP_LANGUAGE, false);
      DocumentBuilder docBuilder = factory.newDocumentBuilder();
      if (!docBuilder.isXIncludeAware()) {
        throw new RuntimeException("The document builder does not support XInclude: " + docBuilder);
      }
      Document doc = docBuilder.parse(radlFile);
      Transformer transformer = TransformerFactory.newInstance().newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      StreamResult result = new StreamResult(new FileWriter(fullRadlFile, false));
      DOMSource source = new DOMSource(doc);
      transformer.transform(source, result);
    } catch (Exception e) {
      Log.error("Failed to assemble RADL file: " + e.getMessage());
      return radlFile;
    }

    return fullRadlFile;
  }

  private static File createFullRadlFile(File radlFile, File targetDirectory) {
    String name = radlFile.getName();
    File dir = targetDirectory;
    if (dir == null) {
      dir = radlFile.getParentFile();
    } else {
      dir.mkdirs();
    }
    File assembled = new File(dir, name + ".out");
    Log.info("-> Assembling the radl file for processing: " + assembled.getAbsolutePath());
    return assembled;
  }
}
