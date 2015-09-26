/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.integration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.java.code.Java;


public class JavaCompiler implements Application {

  @Override
  public int run(Arguments arguments) {
    return Java.getCompiler().run(System.in, System.out, System.err, getCompileArguments(arguments));
  }

  private String[] getCompileArguments(Arguments arguments) {
    Collection<String> result = new ArrayList<String>();
    Properties properties = arguments.properties();
    File baseDir = new File(properties.getProperty("base.dir"));
    File outputDir = new File(baseDir, "classes");
    outputDir.mkdirs();
    String classpath = properties.getProperty("classpath");
    String javaVersion = properties.getProperty("java.version");
    result.addAll(Arrays.asList(
        "-d", outputDir.getPath(),
        "-cp", classpath,
        "-source", javaVersion
    ));
    addSource(baseDir, result);
    return result.toArray(new String[result.size()]);
  }

  private void addSource(File dir, Collection<String> paths) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        addSource(file, paths);
      } else if (file.isFile() && file.getName().endsWith(".java")) {
        paths.add(file.getPath());
      }
    }
  }

}
