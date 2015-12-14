/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.maven;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import radl.core.cli.Arguments;
import radl.core.validation.RadlValidator;
import radl.maven.util.RadlFileUtil;


/**
 * RADL Maven plugin for RADL validation.
 */
@Mojo(name = "validateRadl", defaultPhase = LifecyclePhase.PACKAGE)
public class RadlValidationPlugin extends AbstractMojo implements MavenConfig {

  private static final String SHORT_ERROR_MSG = "RADL_VALIDATION_ERROR";
  private static final String LONG_ERROR_MSG =
      "There are %s issues found in the validation of the RADL file. " +
          "Please check the output issue file at: %s";
  private static final String VALIDATION_SUCCEED = "No issues found in RADL validation";

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
   * The output issue file reported by the validation. Defaults to <pre>target/radl/radl-issues.xml</pre>.
   */
  @Parameter(property = ISSUE_FILE, defaultValue = ISSUE_FILE_DEFAULT)
  private File issueFile;

  /**
   * Specifying whether to fail on receiving validation errors or ignore errors.
   * Defaults to <pre>true</pre>, which means to fail on errors.
   */
  @Parameter(property = FAIL_ON_VALIDATION_ERRORS, defaultValue = FAIL_ON_VALIDATION_ERRORS_DEFAULT)
  private boolean failOnValidationErrors;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File radlFile = RadlFileUtil.findRadlFile(radlDirName, serviceName);
    RadlValidator radlValidator = new RadlValidator();
    int issues = radlValidator.run(
        new Arguments(new String[] {
            issueFile.getAbsolutePath(),
            radlFile.getAbsolutePath() }));
    report(issues);
  }

  private void report(int issues) throws MojoFailureException {
    if (issues > 0) {
      String longMsg = String.format(LONG_ERROR_MSG, issues, issueFile);
      if (failOnValidationErrors) {
        throw new MojoFailureException(issueFile, SHORT_ERROR_MSG, longMsg);
      } else {
        getLog().warn(longMsg);
      }
    } else {
      getLog().info(VALIDATION_SUCCEED);
    }
  }

}
