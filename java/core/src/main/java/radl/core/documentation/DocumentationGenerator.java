/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.sf.saxon.TransformerFactoryImpl;

import org.w3c.dom.Document;

import radl.common.xml.Xml;
import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;


/**
 * Generate documentation from a RADL file.
 */
public final class DocumentationGenerator implements Application {

  private static final String CLIENT_DOCUMENTATION_STYLESHEET = "/xslt/radl2html.xsl";
  private static final String CLIENT_DOCUMENTATION_FILE = "index.html";

  public static void main(String[] args) {
    Cli.run(DocumentationGenerator.class, args);
  }

  /**
   * The following arguments are supported.<ul>
   * <li>[Required] Directory in which to generate the documentation</li>
   * <li>[Optional] The name of a configuration file</li>
   * <li>[Required] The names of the RADL files to generate documentation for</li>
   * </ul>
   */
  @Override
  public int run(Arguments arguments) {
    File docDir = arguments.file();
    docDir.mkdirs();
    File configuration = arguments.hasNext() ? arguments.file() : null;
    if (configuration != null && configuration.getName().endsWith(".radl")) {
      arguments.prev();
      configuration = null;
    }
    if (!arguments.hasNext()) {
      Log.error("Missing RADL files");
      return -1;
    }
    while (arguments.hasNext()) {
      File radlFile = arguments.file();
      Log.info("-> Generating client documentation for " + radlFile.getName());
      try {
        generateClientDocumentation(radlFile, docDir, configuration);
      } catch (RuntimeException e) {
        throw new RuntimeException("Error generating documentation for " + radlFile.getName(), e);
      }
    }
    return 0;
  }

  private void generateClientDocumentation(File radlFile, File docDir, File configuration) {
    File serviceDir = getServiceDir(radlFile, docDir);
    Document radlDocument = Xml.parse(radlFile);
    new StateDiagramGenerator().generateFrom(radlDocument, serviceDir, configuration);
    generateClientDocumentation(radlDocument, getIndexFile(serviceDir));
  }

  private File getServiceDir(File radl, File docDir) {
    File result = new File(docDir, radl.getName().substring(0, radl.getName().lastIndexOf('.')));
    result.mkdirs();
    return result;
  }

  private File getIndexFile(File serviceDir) {
    return new File(serviceDir, CLIENT_DOCUMENTATION_FILE);
  }

  private void generateClientDocumentation(Document radl, File destination) {
    try {
      InputStream stylesheet = getClass().getResourceAsStream(CLIENT_DOCUMENTATION_STYLESHEET);
      try {
        generateClientDocumentation(radl, stylesheet, destination);
      } finally {
        stylesheet.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void generateClientDocumentation(Document radl, InputStream stylesheet, File destination)
      throws Exception {
    Transformer transformer = newTransformerFactory().newTransformer(new StreamSource(stylesheet));
    transformer.setParameter("dir", destination);
    OutputStream output = new FileOutputStream(destination);
    try {
      transformer.transform(new DOMSource(radl), new StreamResult(output));
    } finally {
      output.close();
    }
  }

  private TransformerFactory newTransformerFactory() {
    return new TransformerFactoryImpl();
  }

}
