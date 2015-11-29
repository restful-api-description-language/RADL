/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Arrays;

import org.junit.Test;
import org.w3c.dom.Document;

import radl.java.code.JavaCode;
import radl.test.RadlBuilder;
import radl.test.TestUtil;


public class ErrorDtoGeneratorTest extends AbstractSpringCodeGeneratorTestCase {

  // #39 Add error conditions to generated API
  @Test
  public void generatesErrorDtoWhenErrors() {
    Document radl = RadlBuilder.aRadlDocument()
        .withErrors()
            .error(RANDOM.string(12), RANDOM.string(42))
        .end()
    .build();

    JavaCode errorDto = generateType(radl, TYPE_ERROR_DTO);

    TestUtil.assertCollectionEquals("Fields", Arrays.asList("title", "type"), errorDto.fieldNames());
  }

}
