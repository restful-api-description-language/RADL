/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.java.extraction;

import java.io.File;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import radl.core.extraction.ResourceModel;
import radl.core.extraction.ResourceModelHolder;
import radl.core.extraction.ResourceModelSerializer;

/**
 * Base class for processing annotations.
 */
public abstract class AbstractRestProcessor extends AbstractProcessor {

  private static final String CONSTRUCTOR_NAME = "<init>";
  private ResourceModel resourceModel;

  protected ResourceModel getResourceModel() {
    return resourceModel;
  }

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    initResourceModel();
    doProcess(annotations, roundEnv);
    writeResourceModelToFile();
    return false;
  }

  protected abstract void doProcess(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv);

  protected String nameOf(Element element) {
    return element.getSimpleName().toString();
  }

  protected String qualifiedNameOf(Element classElement) {
    return ((TypeElement)classElement).getQualifiedName().toString();
  }

  protected TypeElement getSuperType(TypeElement type) {
    TypeMirror superClass = type.getSuperclass();
    if (!(superClass instanceof DeclaredType)) {
      return null;
    }
    TypeElement superType = (TypeElement)((DeclaredType)superClass).asElement();
    if (isBasicType(superType)) {
      return null;
    }
    return superType;
  }

  private boolean isBasicType(TypeElement type) {
    Name qualifiedName = type.getQualifiedName();
    return isBasicType(qualifiedName, "Object") || isBasicType(qualifiedName, "Enum");
  }

  private boolean isBasicType(Name name, String basicType) {
    return name.contentEquals("java.lang." + basicType);
  }

  protected String packageOf(TypeElement type) {
    Element element = type;
    while (element.getKind() != ElementKind.PACKAGE) {
      element = element.getEnclosingElement();
    }
    return element.toString();
  }

  protected boolean isConcreteClass(TypeElement type) {
    return type.getKind() == ElementKind.CLASS && !type.getModifiers().contains(Modifier.ABSTRACT);
  }

  protected boolean implementsInterface(TypeElement type, String interfaceName) {
    for (TypeMirror implementedInterface : type.getInterfaces()) {
      if (!(implementedInterface instanceof DeclaredType)) {
        continue;
      }
      TypeElement interfaceElement = (TypeElement)((DeclaredType)implementedInterface).asElement();
      if (interfaceElement.getQualifiedName().contentEquals(interfaceName)) {
        return true;
      }
    }
    TypeElement superType = getSuperType(type);
    if (superType == null) {
      return false;
    }
    return implementsInterface(superType, interfaceName);
  }

  protected Element getMember(TypeElement type, String name) {
    if (type == null) {
      return null;
    }
    for (Element member : type.getEnclosedElements()) {
      if (member.getSimpleName().contentEquals(name)) {
        return member;
      }
    }
    return null;
  }

  protected TypeElement getType(Set<? extends Element> allTypes, String name) {
    for (Element element : allTypes) {
      if (element instanceof TypeElement && element.getSimpleName().contentEquals(name)) {
        return (TypeElement)element;
      }
    }
    return null;
  }

  protected boolean isConstructor(Element method) {
    return CONSTRUCTOR_NAME.equals(nameOf(method));
  }

  protected String getDocumentationFor(Element element) {
    String docComment = processingEnv.getElementUtils().getDocComment(element);
    if (docComment == null) {
      return null;
    }
    StringBuilder documentation = new StringBuilder();
    for (String line : docComment.split("\n")) {
      String text = line.trim();
      if (text.startsWith("@")) {
        break;
      }
      documentation.append(text).append(' ');
    }
    String result = documentation.toString().trim();
    return result.isEmpty() ? null : result;
  }

  private void initResourceModel() {
    String resourceModelFile = processingEnv.getOptions().get(ProcessorOptions.RESOURCE_MODEL_FILE);
    resourceModel = resourceModelFile == null ? ResourceModelHolder.INSTANCE.get() :
        ResourceModelSerializer.deserializeModelFromFile(new File(resourceModelFile));
  }


  private void writeResourceModelToFile() {
    resourceModel.markComplete();
    String resourceModelFile = processingEnv.getOptions().get(ProcessorOptions.RESOURCE_MODEL_FILE);
    if (resourceModelFile != null) {
      ResourceModelSerializer.serializeModelToFile(resourceModel, new File(resourceModelFile));
    }
  }
}
