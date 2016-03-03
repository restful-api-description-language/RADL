/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.maven;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import radl.core.cli.Arguments;
import radl.core.documentation.DocumentationGenerator;
import radl.maven.util.RadlFileUtil;


/**
 * Maven plugin for generating HTML documentation from the RADL file.
 */
@Mojo(name = "radl2docs", defaultPhase = LifecyclePhase.PACKAGE)
public class Radl2HtmlDocPlugin extends AbstractMojo implements MavenConfig {

  private static final String MSG = "HTML documentation is generated at: %s";

  /**
   * Location of RADL files. Defaults to <pre>src/main/radl</pre>.
   */
  @Parameter(property = RADL_DIR_NAME, defaultValue = RADL_DIR_NAME_DEFAULT)
  private File radlDirName;

  /**
   * The name of the service that the RADL files describe. Defaults to the project's name.
   */
  @Parameter(property = SERVICE_NAME, defaultValue = SERVICE_NAME_DEFAULT)
  private String serviceName;

  /**
   * The css URL to produce the HTML documentation. Defaults to RADL default css.
   */
  @Parameter(property = CSS_URL_NAME, defaultValue = CSS_URL_DEFAULT)
  private URL cssURL;

  /**
   * The css URL to produce the HTML documentation. Defaults to RADL default css.
   */
  @Parameter(property = HIDE_LOCATION_NAME, defaultValue = HIDE_LOCATION_DEFAULT)
  private boolean hideLocation;

  /**
   * The sub-directory of the project build directory into which documentation is generated.
   * Defaults to <pre>target/radl</pre>.
   */
  @Parameter(property = DOCS_DIR, defaultValue = DOCS_DIR_DEFAULT)
  private File docsDir;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File radlFile = RadlFileUtil.findRadlFile(radlDirName, serviceName);
    DocumentationGenerator documentationGenerator = new DocumentationGenerator();
    List<String> args = genArguments(radlFile);
    documentationGenerator.run(new Arguments(args.toArray(new String[args.size()])));
    getLog().info(String.format(MSG, docsDir));
  }

  private List<String> genArguments(File radlFile) {
    List<String> args = new ArrayList<>();
    args.add(docsDir.getAbsolutePath());
    if (cssURL != null) {
      args.add(cssURL.toString());
    }
    if (hideLocation) {
      args.add("hide-location");
    }
    args.add(radlFile.getAbsolutePath());
    return args;
  }
}
