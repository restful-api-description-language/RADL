/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec


/**
 *  Gradle plugin for working with RADL.
 */
class RadlPlugin implements Plugin<Project> {

  void apply(Project project) {
    project.extensions.create('radl', RadlExtension)

    project.configurations {
      radl
    }

    project.sourceSets {
      main {
        java {
          srcDir "$project.buildDir/src/java"
        }
      }
    }

    project.afterEvaluate {
      project.dependencies {
        radl ("radl:radl-core:$project.radl.coreVersion") {
          transitive = true
        }
      }

      def serviceName = getServiceName(project)
      def radlFile = getRadlFile(project, serviceName)
      def extractionPropertiesFile = new File(radlFile.parentFile,
          "${radlFile.name.substring(0, radlFile.name.lastIndexOf('.'))}.properties")

      addValidateRadlTask        project, radlFile
      addRadlToDocumentationTask project, radlFile
      addRadlToSpringTask        project, radlFile
      addJavaToRadlTask          project, radlFile, serviceName, extractionPropertiesFile
    }
  }

  def addValidateRadlTask(project, radlFile) {
    project.task('validateRadl', type: JavaExec) {
      main = 'radl.core.validation.RadlValidator'
      args = [radlFile.path]
      classpath project.configurations.radl
    }
    project.check.dependsOn 'validateRadl'
  }

  def addRadlToDocumentationTask(project, radlFile) {
    project.task('generateDocumentationFromRadl', type: JavaExec, dependsOn: 'validateRadl') {
      mustRunAfter 'extractRadlFromCode'
      main = 'radl.core.documentation.DocumentationGenerator'
      args new File(project.rootProject.buildDir, project.radl.docsDir).path
      args radlFile.path
      classpath project.configurations.radl
      doFirst {
        errorOutput = new FileOutputStream(new File(project.buildDir, 'radl2doc.out'))
      }
    }
    project.assemble.dependsOn 'generateDocumentationFromRadl'
  }

  def addRadlToSpringTask(project, radlFile) {
    if (!project.radl.generateSpring || radlFile == null) {
      return
    }
    project.configurations {
      spring
      compile { extendsFrom spring }
    }
    project.dependencies {
      spring "org.springframework:spring-webmvc:$project.radl.springVersion"
    }

    project.task('radl2spring', type: JavaExec) {
      // TODO: inputs & outputs
      def name = radlFile.name.substring(0, radlFile.name.lastIndexOf('.'))
      def prefix = project.radl.packagePrefix ? "${project.radl.packagePrefix}.$name" : name
      main = 'radl.java.generation.spring.RadlToSpringServer'
      args = [radlFile.path, project.projectDir.path, "${prefix}.rest.server",
          relative(project.projectDir, project.sourceSets.main.java.srcDirs[1]),
          relative(project.projectDir, project.sourceSets.main.java.srcDirs[0]), project.radl.scm,
          project.radl.header]
      classpath project.configurations.radl
    }
    project.compileJava.dependsOn 'radl2spring'
  }

  def addJavaToRadlTask(project, radlFile, serviceName, extractionPropertiesFile) {
    project.task('extractRadlFromCode', type: JavaExec) {
      project.rootProject.allprojects.each { proj ->
        proj.sourceSets.find { !it.name.toLowerCase().contains('test') }.each { sourceSet ->
          sourceSet.allJava.srcDirs.each { dir ->
            inputs.dir dir
          }
        }
      }
      def argumentsFile = project.file("${project.name.toLowerCase()}.arguments")
      main = 'radl.java.extraction.FromJavaRadlExtractor'
      args = ["@$argumentsFile.absolutePath"]
      classpath project.configurations.radl
      classpath project.configurations.runtime
      doFirst {
        project.radl.preExtracts.each { it() }
        argumentsFile.withWriter { writer ->
          writer.println "service.name = $serviceName"
          writer.println "base.dir = ${escape(project.rootProject.projectDir.path)}"
          writer.println "radl.file = ${escape(radlFile.path)}"
          writer.println "configuration.file = ${escape(extractionPropertiesFile.path)}"
          if (project.radl.extraProcessors != null) {
           writer.println "extra.processors = $project.radl.extraProcessors"
          }
          if (project.radl.extraSourceDir != null) {
            writer.println "extra.source = ${escape(project.fileTree(project.radl.extraSourceDir).asPath)}"
          }
          def classpath = getClasspath(project)
          if (classpath != null) {
            writer.println "classpath = ${escape(classpath)}"
          }
          writer.println "java.version = $project.sourceCompatibility"
          writer.println "source.code.management.system = $project.radl.scm"
        }
      }
      doLast {
        if (!project.radl.keepArgumentsFile) {
          argumentsFile.delete()
        }
      }
    }
  }

  def getServiceName(project) {
    if (project.radl.serviceName != null) {
      return project.radl.serviceName
    }
    project.rootProject.name
  }

  def getRadlFile(project, serviceName) {
    new File(project.file(project.radl.dirName), "${serviceName.toLowerCase()}.radl")
  }

  def relative(base, instance) {
    def result
    if (instance.path.startsWith(base.path)) {
      result = instance.path.substring(base.path.length())
    } else {
      result = instance.path
    }
    result.startsWith(File.separator) ? result.substring(File.separator.length()) : result
  }

  def getClasspath(project) {
    if (project.radl.skipClasspath) {
      return null
    }
    def result = project.files()
    if (project.radl.extraClasspath != null) {
      result += project.radl.extraClasspath
    }
    def projectJars = project.rootProject.subprojects*.jar.archiveName
    project.rootProject.allprojects.each {
      result += configurationFiles(it, 'compile', projectJars)
      result += configurationFiles(it, 'provided', projectJars)
    }
    result.asPath
  }

  def configurationFiles(project, name, projectJars) {
    def result = project.files()
    def configuration = project.configurations.findByName(name)
    if (configuration == null) {
      return result
    }
    configuration.resolvedConfiguration.resolvedArtifacts.each {
      if (!projectJars.contains(it.name)) {
        result += project.files(it.file)
      }
    }
    result
  }

  def escape(value) {
    value.replace('\\', '/')
  }

}
