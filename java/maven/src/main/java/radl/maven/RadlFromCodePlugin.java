/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.maven;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.InstantiationStrategy;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import radl.core.cli.Arguments;
import radl.java.extraction.FromJavaRadlExtractor;

/**
 * Maven plugin for extracting RADL file from the Java server code.
 */
@Mojo(name = "radlFromCode", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE, instantiationStrategy = InstantiationStrategy.KEEP_ALIVE)
public class RadlFromCodePlugin extends AbstractMojo implements MavenConfig {

  private static final String MSG = "RADL is extracted from code and saved at: %s";
  private static final String RADL_CORE_ARTIFACT_ID = "radl-core";

  /**
   * The enclosing project.
   */
  @Parameter(defaultValue = "${project}", readonly = true)
  protected MavenProject project;

  /**
   * The enclosing plugin dependencies.
   */
  @Parameter(readonly = true, defaultValue = "${plugin.artifacts}")
  private List<Artifact> pluginDependencies;

  /**
   * The classpath scope which is used for the compilation of the RADL extraction from code.
   * Available values are: <pre>runtime</pre>, <pre>compile</pre>, <pre>test</pre> and <pre>system</pre>.
   * Defaults to <pre>runtime</pre>.
   */
  @Parameter(property = CLASSPATH_SCOPE, defaultValue = CLASSPATH_SCOPE_DEFAULT)
  protected String classpathScope;

  /**
   * The sub-directory of the project build directory into which RADL is generated.
   * Defaults to <pre>target/radl</pre>.
   */
  @Parameter(property = DOCS_DIR, defaultValue = DOCS_DIR_DEFAULT)
  private File docsDir;

  /**
   * The name of the service that the RADL files describe. Defaults to the project's name.
   */
  @Parameter(property = SERVICE_NAME, defaultValue = SERVICE_NAME_DEFAULT)
  private String serviceName;

  @Parameter(property = SRC_SET_DIR, defaultValue = SRC_SET_DIR_DEFAULT)
  private File srcDir;

  @Parameter(property = "configurationFile")
  private File configurationFile;

  @Parameter(property = "argumentFile")
  private File argumentFile;

  public void execute() throws MojoExecutionException, MojoFailureException {
    FromJavaRadlExtractor radlFromJavaExtractor = new FromJavaRadlExtractor();
    try {
      generateProjectArgumentsFile();
      radlFromJavaExtractor.run(
          new Arguments(new String[] { "@" + argumentFile.getAbsolutePath() }));
      getLog().info(String.format(MSG, docsDir));
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private void generateProjectArgumentsFile() throws IOException {

    if (!docsDir.exists()) {
      docsDir.mkdir();
    }
    // validate argumentFile name
    if (argumentFile == null || !argumentFile.exists()) {
      argumentFile = new File(docsDir, "extract-radl.properties");
      if (argumentFile.exists()) {
        argumentFile.delete();
      }
      argumentFile.createNewFile();
    }

    // set radl file
    File radlFile = new File(docsDir, serviceName + ".radl");
    if (radlFile.exists()) {
      radlFile.delete();
    }
    String radlFilePath = radlFile.getAbsolutePath();

    // set properties
    Properties properties = new Properties();
    getLog().info("[RADL Extraction - Service Name] " + serviceName);
    properties.setProperty("service.name", serviceName);
    getLog().info("[RADL Extraction - SRC Dir] " + srcDir);
    properties.setProperty("base.dir", srcDir.getAbsolutePath());
    getLog().info("[RADL Extraction - RADL File] " + radlFilePath);
    properties.setProperty("radl.file", radlFilePath);
    String classpath = buildClasspath();
    getLog().info("[RADL Extraction - Classpath] " + classpath);
    properties.setProperty("classpath", classpath);

    if (configurationFile != null && configurationFile.exists()) {
      getLog().info("[RADL Extraction - configuration] " + configurationFile);
      properties.setProperty("configuration.file", configurationFile.getAbsolutePath());
    }

    // write configuration file
    PrintWriter writer = new PrintWriter(argumentFile, "UTF8");
    try {
      properties.store(writer, "");
    } finally {
      writer.close();
    }
    getLog().info("RADL generation argument file: " + argumentFile);
  }

  @SuppressWarnings("unchecked")
  protected void collectProjectArtifactsAndClasspath(List<Artifact> artifacts, List<File> theClasspathFiles) {
    if ("compile".equals(classpathScope)) {
      artifacts.addAll(project.getCompileArtifacts());
      theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
    } else if ("test".equals(classpathScope)) {
      artifacts.addAll(project.getTestArtifacts());
      theClasspathFiles.add(new File(project.getBuild().getTestOutputDirectory()));
      theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
    } else if ("runtime".equals(classpathScope)) {
      artifacts.addAll(project.getRuntimeArtifacts());
      theClasspathFiles.add(new File(project.getBuild().getOutputDirectory()));
    } else if ("system".equals(classpathScope)) {
      artifacts.addAll(project.getSystemArtifacts());
    } else {
      throw new IllegalStateException("Invalid classpath scope: " + classpathScope);
    }
  }

  protected String buildClasspath() {
    List<Artifact> artifacts = new ArrayList<Artifact>();
    List<File> theClasspathFiles = new ArrayList<File>();
    collectProjectArtifactsAndClasspath(artifacts, theClasspathFiles);

    StringBuilder paths = new StringBuilder();
    for (File classpathFile : theClasspathFiles) {
      getLog().info("Adding project build directory to classpath: " + classpathFile.getName());
      paths.append(classpathFile.getAbsolutePath()).append(File.pathSeparator);
    }

    for (Artifact artifact : artifacts) {
      getLog().info("Adding project dependency artifact to classpath: " + artifact.getArtifactId());
      paths.append(artifact.getFile().getAbsolutePath()).append(File.pathSeparator);
    }

    if (pluginDependencies != null) {
      for (Artifact artifact : pluginDependencies) {
        if (RADL_CORE_ARTIFACT_ID.equals(artifact.getArtifactId())) {
          getLog().info("Adding project plugin dependency artifact to classpath: " + artifact.getArtifactId());
          paths.append(artifact.getFile().getAbsolutePath()).append(File.pathSeparator);
        }
      }
    }

    return paths.toString();
  }
}