/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;


public class ApiGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  @Test
  public void generatesApiWithMediaTypes() {
    String httpMethod = aMethod();
    String mediaType1 = 'a' + aName();
    String mediaType2 = 'z' + aMediaType();
    String ignorablePrefix = "application/";
    String fullMediaType1 = ignorablePrefix + mediaType1 + "+xml";
    String fullMediaType2 = mediaType2 + "; version=3.0";
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(fullMediaType1, fullMediaType2)
        .withResource()
            .withMethod(httpMethod)
                .consuming(fullMediaType1)
                .producing(fullMediaType2)
            .end()
        .end()
    .build();

    JavaCode api = generateType(radl, TYPE_API);

    String field1 = mediaTypeToConstant(mediaType1, true) + "_XML";
    String field2 = mediaTypeToConstant(mediaType2, true) + "_VERSION_3_0";
    assertEquals("Fields", Arrays.asList(field1, field2).toString(), api.fieldNames().toString());
    assertEquals("Field value #1", quote(fullMediaType1), api.fieldValue(field1));
    assertEquals("Field value #2", quote(fullMediaType2), api.fieldValue(field2));
  }

  @Test
  public void generatesApiWithLinkRelations() {
    String linkRelationName = "foo-bar";
    String linkRelation = aUri() + linkRelationName;
    Document radl = RadlBuilder.aRadlDocument()
        .withLinkRelations(linkRelation)
        .build();

    JavaCode api = generateType(radl, TYPE_API);
    assertFileComments(api);

    String field = "LINK_REL_FOO_BAR";
    TestUtil.assertCollectionEquals("Fields", Arrays.asList(field), api.fieldNames());
    assertEquals("Field value #1", quote(linkRelation), api.fieldValue(field));
  }

  // #40 Generate JavaDoc for <specification> in link relations
  @Test
  public void generateJavaDocForLinkRelationSpecification() {
    String linkRelationName = "foo-bar";
    String linkRelation = aUri() + linkRelationName;
    String linkRelationSpecificationUri = aUri();
    Document radl = RadlBuilder.aRadlDocument()
        .withLinkRelations()
            .withLinkRelation(linkRelation, linkRelationSpecificationUri)
        .end()
    .build();

    JavaCode api = generateType(radl, TYPE_API);

    String field = "LINK_REL_FOO_BAR";
    TestUtil.assertCollectionEquals("Fields", Arrays.asList(field), api.fieldNames());
    TestUtil.assertCollectionEquals("Field comment", Arrays.asList("See " + linkRelationSpecificationUri + "."),
        api.fieldComments(field));
  }

  // #39 - Add error conditions to generated API
  @Test
  public void addsErrorConditionsToApi() {
    String name1 = 'a' + RANDOM.string(7);
    String name2 = 'm' + RANDOM.string(3) + ':' + RANDOM.string(7);
    String name3 = 'z' + RANDOM.string(7);
    String docPart1 = RANDOM.string(12);
    String docPart2 = RANDOM.string(12);
    String uri = aUri() + name3;
    Document radl = RadlBuilder.aRadlDocument()
        .withErrors()
            .error(name1, docPart1 + "\n\t " + docPart2)
            .error(name2)
            .error(uri)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode api = getType(sources, TYPE_API);
    assertNotNull("Missing API", api);

    String error1 = "ERROR_" + name1.toUpperCase(Locale.getDefault());
    assertTrue("Missing field " + error1, api.fieldNames().contains(error1));
    assertEquals("JavaDoc for error #1", Arrays.asList(docPart1 + ' ' + docPart2 + '.'), api.fieldComments(error1));

    String error3 = "ERROR_" + name3.toUpperCase(Locale.getDefault());
    assertTrue("Missing field " + error3, api.fieldNames().contains(error3));
    assertEquals("Error #3", '"' + uri + '"', api.fieldValue(error3));
  }

  @Test
  public void generatesConstantForDefaultMediaType() {
    String mediaType = aName();
    String fullMediaType = "application/" + mediaType;
    String resource = aName();
    String method = aMethod();
    Document radl = RadlBuilder.aRadlDocument()
        .withMediaTypes(true, fullMediaType)
        .withResource()
            .named(resource)
            .withMethod(method)
                .consuming(fullMediaType)
                .producing(fullMediaType)
            .end()
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    JavaCode api = getType(sources, TYPE_API);
    String mediaTypeToConstant = mediaTypeToConstant(mediaType, true);
    assertEquals("Default media type", mediaTypeToConstant, api.fieldValue(DEFAULT_MEDIA_TYPE_CONSTANT));
  }

}
