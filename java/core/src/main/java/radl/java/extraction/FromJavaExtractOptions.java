/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import radl.core.extraction.ExtractOptions;


/**
 * Options for extracting RADL from Java code. Supports Spring and JAX-RS by default.
 */
public class FromJavaExtractOptions implements ExtractOptions {

  private final Collection<File> classpath;
  private final String extraProcessors;
  private final String javaVersion;
  private final String annotationProcessorOptions;
  private final Collection<File> extraSource;
  private final boolean serializeModel;

  public FromJavaExtractOptions(Collection<File> extraSource, Collection<File> classpath, String extraProcessors,
      String javaVersion, String annotationProcessorOptions, boolean serializeModel) {
    this.extraSource = extraSource;
    this.classpath = classpath;
    this.extraProcessors = extraProcessors;
    this.javaVersion = javaVersion;
    this.annotationProcessorOptions = annotationProcessorOptions;
    this.serializeModel = serializeModel;
  }

  public Collection<File> getClasspath() {
    return classpath;
  }

  public String getAnnotationProcessors() {
    String result = JaxrsProcessor.class.getName() + ',' + SpringProcessor.class.getName();
    return extraProcessors.isEmpty() ? result : result + ',' + extraProcessors;
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public Map<String, String> getAnnotationProcessorOptions() {
    Map<String, String> result = new HashMap<>();
    if (annotationProcessorOptions == null) {
      return result;
    }
    for (String nameValue : annotationProcessorOptions.split(";")) {
      String[] nameAndValue = nameValue.split(":");
      result.put(nameAndValue[0], nameAndValue[1]);
    }
    return result;
  }

  public Collection<File> getExtraSource() {
    return extraSource;
  }

  public boolean isSerializeModel() {
    return serializeModel;
  }

}
