/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.core.code.Code;
import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;

public class UrisGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  @Test
  public void generatesConstantsForUris() {
    String name = aName();
    String billboardUri = aLocalUri();
    String otherUri = aLocalUri();
    Document radl = RadlBuilder.aRadlDocument()
        .startingAt(name)
        .withResource()
            .named(name)
            .locatedAt(billboardUri)
            .withMethod("GET")
                .transitioningTo("Start")
            .end()
        .end()
        .withResource()
            .locatedAt(otherUri)
        .end()
    .build();

    Iterable<Code> sources = radlToCode(radl);

    String field = "URL_BILLBOARD";
    JavaCode api = getType(sources, TYPE_API);
    assertFileComments(api);
    assertEquals("Fields", Arrays.asList(field).toString(), api.fieldNames().toString());
    assertEquals("Field value #1", quote(billboardUri), api.fieldValue(field));

    JavaCode uris = getType(sources, TYPE_URIS);
    assertFileComments(uris);
    assertEquals("# Implementation URIs", 1, uris.fieldNames().size());
    String constant = uris.fieldNames().iterator().next();
    assertTrue("URI constant doesn't fit naming pattern: " + constant, constant.startsWith("URL_"));
    assertEquals("URI value", quote(otherUri), uris.fieldValue(constant));

    JavaCode controller = getType(sources, TestUtil.initCap(name) + "Controller");
    assertFileComments(controller);
    assertNotNull("Missing controller", controller);
    for (String type : controller.imports()) {
      assertFalse("Should not import " + type, type.endsWith("." + TYPE_URIS));
    }
  }

}
