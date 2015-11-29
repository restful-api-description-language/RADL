/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;

import net.sf.saxon.TransformerFactoryImpl;
import radl.common.io.IO;
import radl.common.xml.Xml;
import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;
import radl.core.xml.RadlFileAssembler;

/**
 * Generate documentation from a RADL file.
 */
public final class DocumentationGenerator implements Application {

  private static final String CLIENT_DOCUMENTATION_STYLESHEET = "/xslt/radl2html.xsl";
  private static final String CLIENT_DOCUMENTATION_DEFAULT_CSS = "/xslt/radl-default.css";
  private static final String CLIENT_DOCUMENTATION_FILE = "index.html";

  public static void main(String[] args) {
    Cli.run(DocumentationGenerator.class, args);
  }

  /**
   * The following arguments are supported.<ul>
   * <li>[Required] Directory in which to generate the documentation</li>
   * <li>[Optional] The name of a configuration file</li>
   * <li>[Optional] The name of a css file</li>
   * <li>[Required] The names of the RADL files to generate documentation for</li>
   * </ul>
   */
  @Override
  public int run(Arguments arguments) {
    File docDir = parseDocDir(arguments);
    File configuration = parseConfigurationFile(arguments);
    String cssSource = parseCssFile(arguments);
    return iterativelyHandleRadlFiles(arguments, docDir, configuration, cssSource) ? -1 : 0;
  }

  private File parseDocDir(Arguments arguments) {
    File docDir = arguments.file();
    docDir.mkdirs();
    return docDir;
  }

  private File parseConfigurationFile(Arguments arguments) {
    File configuration = arguments.hasNext() ? arguments.file() : null;
    if (notConfigurationFile(configuration)) {
      arguments.prev();
      configuration = null;
    }
    return configuration;
  }

  private boolean notConfigurationFile(File configuration) {
    return configuration != null &&
        (!configuration.getName().endsWith(".properties") || isRadlFile(configuration));
  }

  private String parseCssFile(Arguments arguments) {
    String cssFile = arguments.hasNext() ? arguments.next() : null;
    if (notCssFile(cssFile)) {
      arguments.prev();
      cssFile = null;
    }
    return cssFile;
  }

  private boolean notCssFile(String cssFile) {
    return cssFile != null &&
        (!cssFile.endsWith(".css") || isRadlFile(new File(cssFile)));
  }

  private boolean iterativelyHandleRadlFiles(Arguments arguments, File docDir, File configuration, String cssSource) {
    if (!arguments.hasNext()) {
      Log.error("Missing RADL files");
      return true;
    }
    while (arguments.hasNext()) {
      File radlFile = arguments.file();
      Log.info("-> Generating client documentation for " + radlFile.getName());
      try {
        generateClientDocumentation(radlFile, docDir, configuration, cssSource);
      } catch (RuntimeException e) {
        throw new RuntimeException("Error generating documentation for " + radlFile.getName(), e);
      }
    }
    return false;
  }

  private boolean isRadlFile(File configuration) {
    return configuration.getName().endsWith(".radl") || configuration.getName().endsWith(".xml");
  }

  private void generateClientDocumentation(File radlFile, File docDir, File configuration, String cssSource) {
    File serviceDir = getServiceDir(radlFile, docDir);
    File assembledRadl = RadlFileAssembler.assemble(radlFile, docDir);
    Document radlDocument = Xml.parse(assembledRadl);
    new StateDiagramGenerator().generateFrom(radlDocument, serviceDir, configuration);
    File localCssFile = normalizeCSSFile(docDir, cssSource);
    try {
      generateClientDocumentation(radlDocument, getIndexFile(serviceDir), localCssFile.toURI().toString());
    } finally {
      IO.delete(localCssFile);
    }
  }

  private File getServiceDir(File radl, File docDir) {
    File result = new File(docDir, radl.getName().substring(0, radl.getName().lastIndexOf('.')));
    result.mkdirs();
    return result;
  }

  private File getIndexFile(File serviceDir) {
    return new File(serviceDir, CLIENT_DOCUMENTATION_FILE);
  }

  private void generateClientDocumentation(Document radl, File destination, String cssFile) {
    try {
      InputStream stylesheet = getClass().getResourceAsStream(CLIENT_DOCUMENTATION_STYLESHEET);
      if (stylesheet == null) {
        throw new IllegalStateException("Missing stylesheet: " + CLIENT_DOCUMENTATION_STYLESHEET);
      }
      try {
        generateClientDocumentation(radl, stylesheet, cssFile, destination);
      } finally {
        stylesheet.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private File normalizeCSSFile(File docDir, String cssSource) {
    if (StringUtils.isNotBlank(cssSource)) {
      Log.info("Provided CSS URL is: " + cssSource);
    }
    try {
      InputStream inputStream = StringUtils.isEmpty(cssSource) ?
          getClass().getResourceAsStream(CLIENT_DOCUMENTATION_DEFAULT_CSS) :
          new URL(cssSource).openStream();
      File localCss = new File(docDir, "radl-use.css");
      IO.copy(inputStream, new FileOutputStream(localCss));
      return localCss;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void generateClientDocumentation(Document radl, InputStream stylesheet, String cssFile, File destination)
      throws Exception {
    Transformer transformer = newTransformerFactory().newTransformer(new StreamSource(stylesheet));
    transformer.setParameter("dir", destination);
    transformer.setParameter("css-file", cssFile);
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
