/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.java.extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.tools.JavaCompiler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import radl.common.io.IO;
import radl.common.xml.ElementProcessor;
import radl.common.xml.Xml;
import radl.core.Log;
import radl.core.cli.Application;
import radl.core.cli.Arguments;
import radl.core.cli.Cli;
import radl.core.code.SourceFile;
import radl.core.extraction.ExtractOptions;
import radl.core.extraction.RadlExtractor;
import radl.core.extraction.RadlMerger;
import radl.core.extraction.ResourceModel;
import radl.core.extraction.ResourceModelHolder;
import radl.core.extraction.ResourceModelMerger;
import radl.core.extraction.ResourceModelSerializer;
import radl.core.scm.ScmFactory;
import radl.core.scm.SourceCodeManagementSystem;
import radl.core.xml.DocumentProcessor;
import radl.core.xml.XmlMerger;
import radl.java.code.Java;
import radl.java.code.JavaCode;


/**
 * Extract RADL from Java code.
 */
public class FromJavaRadlExtractor implements RadlExtractor, Application {

  private static final String TARGET_FILE_EXTENSION = ".target";
  private static final String CLASSPATH_FILE = ".classpath";
  private static final String ENVIRONMENT_VAR_MARKER = "${env_var:";
  private static final String WORKSPACE_LOCATION_MARKER = "${workspace_loc:";

  private final ResourceModelMerger merger;
  private ResourceModel resourceModel;

  public static void main(String[] args) {
    Cli.run(FromJavaRadlExtractor.class, args);
  }

  public FromJavaRadlExtractor() {
    this(new RadlMerger(), ResourceModelHolder.INSTANCE.get());
  }

  public FromJavaRadlExtractor(ResourceModelMerger merger, ResourceModel resourceModel) {
    this.merger = merger;
    this.resourceModel = resourceModel;
  }

  @Override
  public int run(Arguments arguments) {
    Timer timer = new Timer();
    RunOptions options = getOptions(arguments);
    String configurationFileName = options.getConfigurationFileName();
    String annotationProcessorOptions = null;
    Properties configuration = new Properties();
    if (configurationFileName != null) {
      File configurationFile = new File(configurationFileName);
      if (configurationFile.exists()) {
        configuration = loadConfigurationFrom(configurationFile);
        annotationProcessorOptions = (String)configuration.remove("annotation.processor.options");
      } else {
        Log.error("Configuration file not found: " + configurationFile);
      }
    }
    resourceModel.configure(configuration);
    Document radl = extractFrom(options.getServiceName(), options.getBaseDir(), new FromJavaExtractOptions(
        options.getExtraSource(), options.getClasspath(), options.getExtraProcessors(), options.getJavaVersion(),
        annotationProcessorOptions));
    writeRadl(radl, options.getRadlFile(), options.getScm());
    Log.info("-> RADL extraction took " + timer);
    return 0;
  }

  private RunOptions getOptions(Arguments arguments) {
    String firstArgument = arguments.next();
    if (firstArgument.startsWith("@")) {
      return getOptionsByFile(firstArgument.substring(1));
    } else {
      return getOptionsByCommandLine(arguments, firstArgument);
    }
  }

  private RunOptions getOptionsByCommandLine(Arguments arguments, String serviceName) {
    File baseDir;
    File radlFile;
    String extraProcessors;
    Collection<File> classpath;
    String configurationFileName = null;
    baseDir = arguments.file();
    radlFile = arguments.file();
    if (arguments.hasNext()) {
      configurationFileName = arguments.next();
    }
    extraProcessors = getProcessors(arguments);
    Collection<File> extraSource = getExtraSource(arguments);
    classpath = getClasspath(arguments, baseDir);
    String javaVersion = arguments.next("1.7");
    String scmId = arguments.next("default");
    return new RunOptions(serviceName, baseDir, configurationFileName, extraSource, classpath, extraProcessors,
        radlFile, javaVersion, scmId);
  }

  private Collection<File> getExtraSource(Arguments arguments) {
    Collection<File> result = new TreeSet<File>();
    if (arguments.hasNext()) {
      for (String path : arguments.next().split(File.pathSeparator)) {
        result.add(new File(path));
      }
    }
    return result;
  }

  private RunOptions getOptionsByFile(String optionsFile) {
    Properties properties = new Properties();
    try {
      InputStream optionsStream = new FileInputStream(new File(optionsFile));
      try {
        properties.load(optionsStream);
      } finally {
        optionsStream.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    String serviceName = properties.getProperty("service.name");
    File baseDir = new File(properties.getProperty("base.dir"));
    File radlFile = new File(properties.getProperty("radl.file"));
    String configurationFileName = properties.getProperty("configuration.file");
    Collection<File> extraSource = new HashSet<File>();
    if (properties.containsKey("extra.source")) {
      for (String fileName : properties.getProperty("extra.source", "").split("\\" + File.pathSeparator)) {
        extraSource.add(new File(fileName));
      }
    }
    Collection<File> classpath = new HashSet<File>();
    if (properties.containsKey("classpath")) {
      for (String fileName : properties.getProperty("classpath", "").split("\\" + File.pathSeparator)) {
        classpath.add(new File(fileName));
      }
    } else {
      classpathFromEclipse(baseDir, classpath);
    }
    String extraProcessors = properties.getProperty("extra.processors", "");
    String javaVersion = properties.getProperty("java.version", "1.6");
    String scmId = properties.getProperty("source.code.management.system", "default");
    return new RunOptions(serviceName, baseDir, configurationFileName, extraSource, classpath, extraProcessors,
        radlFile, javaVersion, scmId);
  }

  private String getProcessors(Arguments arguments) {
    if (arguments.hasNext()) {
      String result = arguments.next();
      if (result.contains(File.pathSeparator)) {
        arguments.prev();
      } else {
        return result;
      }
    }
    return "";
  }

  private Collection<File> getClasspath(Arguments arguments, File baseDir) {
    Collection<File> result = new TreeSet<File>();
    if (arguments.hasNext()) {
      classpathFromArgument(arguments.next(), result);
    } else {
      classpathFromEclipse(baseDir, result);
    }
    return result;
  }

  private void classpathFromArgument(String paths, Collection<File> classpath) {
    for (String path : paths.split(File.pathSeparator)) {
      classpath.add(new File(path));
    }
  }

  private void classpathFromEclipse(File file, Collection<File> classpath) {
    if (isCodeDirectory(file)) {
      for (File child : file.listFiles()) {
        classpathFromEclipse(child, classpath);
      }
    } else if (isClassPath(file)) {
      classpath.add(file);
    }
  }

  private boolean isClassPath(File file) {
    return file.isFile() && (CLASSPATH_FILE.equals(file.getName()) || file.getName().endsWith(TARGET_FILE_EXTENSION));
  }

  private Properties loadConfigurationFrom(File configurationFile) {
    Properties result = new Properties();
    try {
      InputStream stream = new FileInputStream(configurationFile);
      try {
        result.load(stream);
      } finally {
        stream.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return result;
  }

  private void writeRadl(Document radl, File radlFile, SourceCodeManagementSystem scm) {
    String xml = merge(radlFile, radl);
    try {
      scm.prepareForUpdate(radlFile);
      PrintWriter writer = new PrintWriter(radlFile, "UTF8");
      try {
        writer.print(xml);
      } finally {
        writer.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String merge(File existingXml, Document mergeInXml) {
    Document result = mergeInXml;
    if (existingXml.exists()) {
      DocumentProcessor processor = new XmlMerger(Xml.parse(existingXml));
      if (mergeInXml != null) {
        processor.process(mergeInXml);
      }
      result = processor.result();
    }
    return Xml.toString(result);
  }

  @Override
  public Document extractFrom(String serviceName, File baseDir, ExtractOptions options) {
    if (!baseDir.exists()) {
      throw new IllegalArgumentException("Missing base directory: " + baseDir.getAbsolutePath());
    }
    FromJavaExtractOptions javaOptions = (FromJavaExtractOptions)options;
    composeRadl(serviceName, baseDir, javaOptions);
    return merger.toRadl(resourceModel);
  }

  private void composeRadl(String serviceName, File baseDir, FromJavaExtractOptions options) {
    merger.setService(serviceName);
    JavaCompiler compiler = Java.getCompiler();
    if (compiler == null) {
      throw new IllegalStateException("Missing Java compiler");
    }
    processAnnotationsOfFilesIn(baseDir, compiler, options);
  }

  private void processAnnotationsOfFilesIn(File baseDir, JavaCompiler compiler, FromJavaExtractOptions extractOptions) {
    Collection<File> javaFiles = new ArrayList<File>();
    javaFiles.addAll(extractOptions.getExtraSource());
    collectJavaFilesIn(baseDir, javaFiles);
    if (javaFiles.isEmpty()) {
      return;
    }
    File compilerOptions = new File("options");
    if (compilerOptions.exists()) {
      compilerOptions.delete();
    }
    File resourceModelFile = new File("resourceModel-" + UUID.randomUUID().toString());
    if (resourceModelFile.exists()) {
      resourceModelFile.delete();
    }
    ResourceModelSerializer.serializeModelToFile(resourceModel, resourceModelFile);

    String resourceModelFileArg = String.format("-A%s=%s",
        ProcessorOptions.RESOURCE_MODEL_FILE, resourceModelFile.getAbsolutePath());
    try {
      try {
        PrintWriter writer = new PrintWriter(compilerOptions, "UTF8");
        try {
          writer.println(resourceModelFileArg);
          writeOptions(extractOptions, writer);
          writeSources(javaFiles, writer);
          writeClasses(javaFiles, writer);
        } finally {
          writer.close();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      System.setProperty("radl.base.dir", baseDir.getAbsolutePath());
      if (compiler.run(null, null, null, String.format("@%s", compilerOptions.getAbsoluteFile())) != 0) {
        throw new IllegalArgumentException("Compilation failed");
      }

      if (!resourceModel.isCompleted()) {
        resourceModel = ResourceModelSerializer.deserializeModelFromFile(resourceModelFile);
      }

    } finally {
      IO.delete(compilerOptions);
      IO.delete(resourceModelFile);
    }
  }

  private void writeClasses(Collection<File> javaFiles, PrintWriter writer) throws IOException {
    for (File javaFile : javaFiles) {
      String fullyQualifiedClassName = toFullyQualifiedClassName(javaFile);
      if (fullyQualifiedClassName != null) {
        writer.println(fullyQualifiedClassName);
      }
    }
  }

  private String toFullyQualifiedClassName(File javaFile) throws IOException {
    try {
      SourceFile source = new SourceFile(javaFile.getAbsolutePath());
      JavaCode javaCode = (JavaCode)source.code();
      return javaCode.fullyQualifiedName();
    } catch (Exception e) {
      return null;
    }
  }

  private void writeOptions(FromJavaExtractOptions options, PrintWriter writer) {
    writer.println("-proc:only");
    String classpath = asPath(options.getClasspath());
    if (!classpath.isEmpty()) {
      writer.print("-cp ");
      writer.println(classpath);
    }
    writer.println("-processor " + options.getAnnotationProcessors());
    writer.println("-source " + options.getJavaVersion());
    for (Entry<String, String> entry : options.getAnnotationProcessorOptions().entrySet()) {
      writer.println("-A" + entry.getKey() + '=' + entry.getValue());
    }
  }

  private String asPath(Collection<File> classpath) {
    Collection<String> paths = new HashSet<String>();
    for (File file : classpath) {
      if (file.getName().endsWith(CLASSPATH_FILE)) {
        addClassPaths(file, paths);
      } else if (file.getName().endsWith(TARGET_FILE_EXTENSION)) {
        addTargetPaths(file, paths);
      } else {
        paths.add(file.getPath());
      }
    }
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String path : paths) {
      result.append(prefix).append(path);
      prefix = File.pathSeparator;
    }
    return result.toString();
  }

  private void addTargetPaths(final File targetFile, final Collection<String> paths) {
    Document document = Xml.parse(targetFile);
    try {
      final Collection<String> locations = new ArrayList<String>();
      Xml.processNestedElements(document.getDocumentElement(), new ElementProcessor() {
        @Override
        public void process(Element element) throws Exception {
          locations.add(resolveVar(targetFile.getParentFile().getAbsolutePath(), element.getAttributeNS(null, "path")));
        }
      }, "locations", "location");
      Xml.processNestedElements(document.getDocumentElement(), new ElementProcessor() {
        @Override
        public void process(Element element) throws Exception {
          String fileName = element.getAttributeNS(null, "id");
          String path = findFile(locations, fileName);
          if (path == null) {
            Log.error("Can't find bundle with Bundle-SymbolicName " + fileName);
          } else {
            paths.add(path);
          }
        }
      }, "includeBundles", "plugin");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String resolveVar(String baseDir, String path) {
    String result = path;
    if (result.startsWith(ENVIRONMENT_VAR_MARKER)) {
      int index = result.indexOf('}', ENVIRONMENT_VAR_MARKER.length());
      String var = result.substring(ENVIRONMENT_VAR_MARKER.length(), index);
      result = valueOf(var) + result.substring(index + 1);
    } else if (result.startsWith(WORKSPACE_LOCATION_MARKER)) {
      int index = result.indexOf('}', WORKSPACE_LOCATION_MARKER.length());
      result = baseDir + result.substring(index + 1);
    }
    return result;
  }

  private String findFile(Collection<String> locations, String fileName) {
    for (String location : locations) {
      String result = findFile(location, fileName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private String findFile(String location, final String fileName) {
    String[] found = new File(location).list(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return isBundle(dir, name, fileName);
      }
    });
    return found.length == 1 ? new File(location, found[0]).getAbsolutePath() : null;
  }

  private boolean isBundle(File dir, String name, String bundleName) {
    if (!name.endsWith(".jar")) {
      return false;
    }
    try {
      ZipFile jar = new ZipFile(new File(dir, name));
      try {
        ZipEntry manifest = jar.getEntry("META-INF/MANIFEST.MF");
        if (manifest == null) {
          return false;
        }
        Properties properties = new Properties();
        InputStream stream = jar.getInputStream(manifest);
        try {
          properties.load(stream);
          if (bundleName.equals(properties.getProperty("Bundle-SymbolicName"))) {
            return true;
          }
        } finally {
          stream.close();
        }
      } finally {
        jar.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return false;
  }

  private void addClassPaths(final File classPathFile, final Collection<String> paths) {
    Document document = Xml.parse(classPathFile);
    try {
      Xml.processChildElements(document.getDocumentElement(), new ElementProcessor() {
        @Override
        public void process(Element element) throws Exception {
          String path = element.getAttributeNS(null, "path");
          String kind = element.getAttributeNS(null, "kind");
          if ("var".equals(kind)) {
            paths.add(resolve(path));
          } else if ("lib".equals(kind) && path.endsWith(".jar")) {
            String jarPath = getJarPath(classPathFile, path);
            if (jarPath != null) {
              paths.add(jarPath);
            }
          }
        }
      }, "classpathentry");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getJarPath(final File classPathFile, String path) {
    File dir = classPathFile.getParentFile();
    File jar = new File(dir, path);
    while (!jar.exists() && dir != null) {
      dir = dir.getParentFile();
      jar = new File(dir, path);
    }
    if (!jar.exists()) {
      return null;
    }
    return jar.getAbsolutePath();
  }

  private String resolve(String variablePath) {
    int index = variablePath.indexOf('/');
    String variable = variablePath.substring(0, index);
    return valueOf(variable) + variablePath.substring(index).replace("/", File.separator);
  }

  private String valueOf(String variable) {
    return System.getenv(variable);
  }

  private void writeSources(Collection<File> javaFiles, PrintWriter writer) throws IOException {
    for (File file : javaFiles) {
      writer.println(file.getCanonicalPath());
    }
  }

  private void collectJavaFilesIn(File file, Collection<File> javaFiles) {
    if (isCodeDirectory(file)) {
      for (File child : file.listFiles()) {
        collectJavaFilesIn(child, javaFiles);
      }
    } else if (isJavaFile(file)) {
      javaFiles.add(file);
    }
  }

  private boolean isCodeDirectory(File file) {
    return file.isDirectory() && !file.getName().toLowerCase(Locale.getDefault()).contains("test");
  }

  private boolean isJavaFile(File file) {
    return file.isFile() && file.getName().endsWith(".java") && !"package-info.java".equals(file.getName());
  }


  private static final class RunOptions {

    private final String serviceName;
    private final File baseDir;
    private final Collection<File> classpath;
    private final String extraProcessors;
    private final File radlFile;
    private final String configurationFileName;
    private final String javaVersion;
    private final Collection<File> extraSource;
    private final String scmId;

    public RunOptions(String serviceName, File baseDir, String configurationFileName, Collection<File> extraSource,
        Collection<File> classpath, String extraProcessors, File radlFile, String javaVersion, String scmId) {
      this.serviceName = serviceName;
      this.baseDir = baseDir;
      this.configurationFileName = configurationFileName;
      this.extraSource = extraSource;
      this.classpath = classpath;
      this.extraProcessors = extraProcessors;
      this.radlFile = radlFile;
      this.javaVersion = javaVersion;
      this.scmId = scmId;
    }

    public SourceCodeManagementSystem getScm() {
      return ScmFactory.newInstance(scmId);
    }

    public Collection<File> getExtraSource() {
      return extraSource;
    }

    public String getJavaVersion() {
      return javaVersion;
    }

    public String getConfigurationFileName() {
      return configurationFileName;
    }

    public String getServiceName() {
      return serviceName;
    }

    public File getBaseDir() {
      return baseDir;
    }

    public Collection<File> getClasspath() {
      return classpath;
    }

    public String getExtraProcessors() {
      return extraProcessors;
    }

    public File getRadlFile() {
      return radlFile;
    }

    @Override
    public String toString() {
      return "serviceName=" + serviceName + "\nbaseDir=" + baseDir + "\nclasspath=" + classpath
          + "\nextraProcessors=" + extraProcessors + "\nradlFile=" + radlFile + "\nconfigurationFileName="
          + configurationFileName + "\njavaVersion=" + javaVersion + "\nextraSource=" + extraSource + "\nscmId="
          + scmId;
    }

  }

}
