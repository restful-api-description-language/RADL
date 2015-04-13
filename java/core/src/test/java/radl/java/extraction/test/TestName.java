/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.extraction.test;

import javax.lang.model.element.Name;


public class TestName implements Name {

  private final String value;

  public TestName(String value) {
    this.value = value;
  }

  @Override
  public int length() {
    return value.length();
  }

  @Override
  public char charAt(int index) {
    return value.charAt(index);
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return value.subSequence(start, end);
  }

  @Override
  public boolean contentEquals(CharSequence cs) {
    return value.contentEquals(cs.toString());
  }

  @Override
  public String toString() {
    return value;
  }

}
