/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.generation;

import java.util.ArrayList;

import radl.core.code.Code;


public class Module extends ArrayList<Code> {

  public Module(Code... codes) {
    for (Code code : codes) {
      add(code);
    }
  }

}
