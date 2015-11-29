/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import javax.xml.datatype.XMLGregorianCalendar;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;


public class DtosGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  // #9 Generate data transfer objects for property groups
  @Test
  public void generatesDtosForPropertySources() {
    String name1 = 'a' + aName();
    String property1 = 'a' + aName();
    String property2 = 'b' + aName();
    String property2Type = "int";
    String property3 = 'c' + aName();
    String name2 = 'b' + aName();
    String uri1 = aUri();
    String property4 = aName();
    String uri2 = aUri();
    String name3 = 'c' + aName();
    String property5 = 'a' + aName();
    String property6 = 'b' + aName();
    String name4 = 'd' + aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(true, JSON_LD)
        .withPropertyGroup()
            .named(name1)
            .withProperty(property1)
            .end()
            .withProperty(property2)
                .as(property2Type)
            .end()
            .withProperty(property3)
                .repeating()
            .end()
        .end()
        .withPropertyGroup()
            .named(name2)
            .meaning(uri1)
            .withProperty(property4)
                .meaning(uri2)
            .end()
        .end()
        .withPropertyGroup()
            .named(name3)
            .withPropertyGroup()
                .named(property5)
                .referencing(name1)
            .endNested()
            .withPropertyGroup()
                .named(property6)
                .withProperty(property1)
                .end()
            .endNested()
        .end()
        .withPropertyGroup()
            .named(name4)
            .meaning(uri1)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode dto1 = getType(sources, dtoName(name1));
    assertEquals("Package", packagePrefix + '.' + name1, dto1.packageName());
    assertTrue("Annotations #1", dto1.typeAnnotations().isEmpty());
    TestUtil.assertCollectionEquals("Fields #1", Arrays.asList(property1, property2, property3 + 's'), dto1.fieldNames());
    assertEquals("Field #1 type", "String", dto1.fieldType(property1));
    assertEquals("Field #2 type", property2Type, dto1.fieldType(property2));
    assertEquals("Field #3 type", "String[]", dto1.fieldType(property3 + 's'));

    JavaCode dto2 = getType(sources, dtoName(name2));
    assertTrue("Missing import on Expose", dto2.imports().contains("de.escalon.hypermedia.hydra.mapping.Expose"));
    TestUtil.assertCollectionEquals("Annotations #2", Collections.<String>singleton("@Expose(\"" + uri1 + "\")"),
        dto2.typeAnnotations());
    TestUtil.assertCollectionEquals("Annotations property #4", Collections.<String>singleton("@Expose(\"" + uri2 + "\")"),
        dto2.fieldAnnotations(property4));

    JavaCode dto3 = getType(sources, dtoName(name3));
    TestUtil.assertCollectionEquals("Fields #3", Arrays.asList(property5, property6), dto3.fieldNames());
    assertTrue("Imports #3 doesn't contain " + dto1.fullyQualifiedName(), dto3.imports().contains(dto1.fullyQualifiedName()));
    assertEquals("Field #5 type", dto1.typeName(), dto3.fieldType(property5));
    String nestedDtoName = dtoName(property6);
    assertEquals("Field #6 type", nestedDtoName, dto3.fieldType(property6));

    JavaCode nestedDto = getType(sources, nestedDtoName);
    assertTrue("Imports #3 doesn't contain " + nestedDto.fullyQualifiedName(),
        dto3.imports().contains(nestedDto.fullyQualifiedName()));

    JavaCode dto4 = getType(sources, dtoName(name4));
    assertTrue("Missing import on Expose #4", dto4.imports().contains("de.escalon.hypermedia.hydra.mapping.Expose"));
  }

  @Test
  public void generatesDtoWithDateTimeField() {
    String data = aName();
    String property = aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withPropertyGroup()
            .named(data)
            .withProperty(property)
                .as("xsd:dateTime")
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode dto = getType(sources, dtoName(data));
    assertEquals("Field type", XMLGregorianCalendar.class.getSimpleName(), dto.fieldType(property));
    assertTrue("Imports", dto.imports().contains(XMLGregorianCalendar.class.getName()));
  }

  @Test
  public void generatesDtoWithNumberField() {
    String data = aName();
    String property = aName();
    Document radl = RadlBuilder.aRadlDocument()
        .withPropertyGroup()
            .named(data)
            .withProperty(property)
                .as("number")
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode dto = getType(sources, dtoName(data));
    assertEquals("Field type", "double", dto.fieldType(property));
  }

}
