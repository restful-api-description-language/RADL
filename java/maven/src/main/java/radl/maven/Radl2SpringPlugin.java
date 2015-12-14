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
import radl.java.generation.spring.RadlToSpringServer;
import radl.maven.util.RadlFileUtil;


/**
 * Maven plugin for generating Spring server code from the RADL file.
 */
@Mojo(name = "radl2spring", defaultPhase = LifecyclePhase.PACKAGE)
public class Radl2SpringPlugin extends AbstractMojo implements MavenConfig {

  private static final String MSG = "Spring source codes are generated at: %s/%s and %s/%s";

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
   * The base project directory. Defaults to project's root directory.
   */
  @Parameter(property = BASE_DIR, defaultValue = BASE_DIR_DEFAULT)
  private File baseDir;

  /**
   * The prefix to use when generating Java packages. Defaults to <pre>radl.sample.rest.server</pre>.
   */
  @Parameter(property = PACKAGE_PREFIX, defaultValue = PACKAGE_PREFIX_DEFAULT)
  private String packagePrefix;

  /**
   * The relative directory path where the generated source codes are put.
   * This set of codes do not need further manual work.
   */
  @Parameter(property = REL_GEN_SRC_DIR, defaultValue = REL_GEN_SRC_DIR_DEFAULT)
  private String relativeGeneratedSourceDir;

  /**
   * The relative directory path where the generated source codes are put.
   * This set of codes need further manual work.
   */
  @Parameter(property = REL_GEN_MAN_SRC_DIR, defaultValue = REL_GEN_MAN_SRC_DIR_DEFAULT)
  private String relativeGeneratedManualSourceDir;

  /**
   * The source code management system to use. Defaults to <pre>default</pre>.
   * The only other valid value is <pre>p4</pre> for Perforce.
   */
  @Parameter(property = SCM, defaultValue = SCM_DEFAULT)
  private String scm;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    File radlFile = RadlFileUtil.findRadlFile(radlDirName, serviceName);
    RadlToSpringServer springCodeGenerator = new RadlToSpringServer();
    springCodeGenerator.run(
        new Arguments(new String[] {
            radlFile.getAbsolutePath(),
            baseDir.getAbsolutePath(),
            packagePrefix,
            relativeGeneratedSourceDir,
            relativeGeneratedManualSourceDir,
            scm }));
    getLog().info(String.format(MSG, baseDir, relativeGeneratedManualSourceDir, baseDir, relativeGeneratedSourceDir));
  }

}
