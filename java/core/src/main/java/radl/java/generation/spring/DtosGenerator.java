/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.atteo.evo.inflector.English;

import radl.core.code.Code;
import radl.core.code.radl.MediaType;
import radl.core.code.radl.Property;
import radl.core.code.radl.PropertyGroup;
import radl.core.code.radl.PropertyGroups;
import radl.core.code.radl.RadlCode;
import radl.core.generation.CodeGenerator;
import radl.java.code.Java;
import radl.java.code.JavaBeanProperty;
import radl.java.code.JavaCode;


public class DtosGenerator extends FromRadlCodeGenerator {

  private static final String SEMANTIC_ANNOTATION_PACKAGE = "de.escalon.hypermedia.hydra.mapping";
  private static final String SEMANTIC_ANNOTATION = "Expose";

  private MediaType defaultMediaType;
  private String packagePrefix;

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    Collection<Code> result = new ArrayList<Code>();
    defaultMediaType = (MediaType)context.get(FromRadlCodeGenerator.DEFAULT_MEDIA_TYPE);
    packagePrefix = (String)context.get(CodeGenerator.PACKAGE_PREFIX);
    Boolean hasHyperMediaTypes = (Boolean)context.get(FromRadlCodeGenerator.HAS_HYPERMEDIA);
    generateSourcesForPropertyGroups(radl.propertyGroups(), hasHyperMediaTypes, result);
    return result;
  }

  private void generateSourcesForPropertyGroups(PropertyGroups propertyGroups, boolean hasHyperMediaTypes,
      Collection<Code> sources) {
    if (propertyGroups == null) {
      return;
    }
    for (String propertyGroup : propertyGroups.names()) {
      addDtosFor(propertyGroups.item(propertyGroup), hasHyperMediaTypes, sources);
    }
  }

  protected String addDtosFor(PropertyGroup propertyGroup, boolean hasHyperMediaTypes, Collection<Code> sources) {
    final JavaCode code = new JavaCode();
    addPackage(propertyGroup.name(), code);
    code.add("");
    String superType;
    if (hasHyperMediaTypes) {
      code.add("import org.springframework.hateoas.ResourceSupport;");
      code.add("");
      superType = "extends ResourceSupport ";
    } else {
      superType = "";
    }
    addSemanticAnnotationImport(propertyGroup, code);
    addDtoImports(propertyGroup, code);
    code.add("");
    String annotation = getSemanticAnnotation(propertyGroup, "");
    if (annotation != null) {
      code.add(annotation);
    }
    String result = getDtoClass(propertyGroup.name());
    code.add("public class %s %s{", result, superType);
    code.add("");
    addDtoFields(hasHyperMediaTypes, propertyGroup, code, sources);
    code.add("}");
    sources.add(code);
    return result;
  }

  private void addSemanticAnnotationImport(PropertyGroup propertyGroup, Code code) {
    if (defaultMediaType != null && defaultMediaType.isSemanticMediaType() && propertyGroup.hasSemantics()) {
      code.add("import %s.%s;", SEMANTIC_ANNOTATION_PACKAGE, SEMANTIC_ANNOTATION);
      code.add("");
    }
  }

  private void addDtoImports(PropertyGroups propertyGroup, final Code code) {
    boolean added = false;
    for (String name : propertyGroup.names()) {
      String ref = propertyGroup.item(name).reference();
      if (ref.isEmpty()) {
        ref = name;
      }
      code.add("import %s.%s.%s;", packagePrefix, toPackage(ref), getDtoClass(ref));
      added = true;
    }
    if (added) {
      code.add("");
    }
  }

  private String getSemanticAnnotation(Property property, String indent) {
    String result = null;
    if (defaultMediaType != null && defaultMediaType.isSemanticMediaType()) {
      String uri = property.uri();
      if (!uri.isEmpty()) {
        result = String.format("%s@%s(\"%s\")", indent, SEMANTIC_ANNOTATION, Java.toString(uri));
      }
    }
    return result;
  }

  private void addDtoFields(boolean hasHyperMediaTypes, PropertyGroup propertyGroup, JavaCode dto,
      Collection<Code> sources) {
    Collection<JavaBeanProperty> properties = new ArrayList<JavaBeanProperty>();
    for (String propertyName : propertyGroup.propertyNames()) {
      Property property = propertyGroup.property(propertyName);
      String annotation = getSemanticAnnotation(property, "  ");
      String type = getType(property, hasHyperMediaTypes, sources);
      int index = type.lastIndexOf('.');
      if (index > 0) {
        String simpleType = type.substring(index + 1);
        dto.ensureImport(type.substring(0, index), simpleType);
        type = simpleType;
      }
      String fieldName = property.repeats() ? English.plural(propertyName) : propertyName;
      properties.add(new JavaBeanProperty(fieldName, type, annotation));
    }
    addProperties(dto, properties);
  }

  protected String getType(Property property, boolean hasHyperMediaTypes, Collection<Code> sources) {
    String result = null;
    if (property instanceof PropertyGroup) {
      PropertyGroup propertyGroup = (PropertyGroup)property;
      String ref = propertyGroup.reference();
      if (ref.isEmpty()) {
        result = addDtosFor(propertyGroup, hasHyperMediaTypes, sources);
      } else {
        result = getDtoClass(ref);
      }
    }
    if (result == null) {
      String type = property.type();
      result = type.isEmpty() ? "String" : radlTypeToJavaType(type);
    }
    if (property.repeats()) {
      return result + "[]";
    }
    return result;
  }

  private String radlTypeToJavaType(String type) {
    if ("xsd:dateTime".equals(type)) {
      return XMLGregorianCalendar.class.getName();
    }
    if ("number".equals(type)) {
      return "double";
    }
    return type;
  }

}
