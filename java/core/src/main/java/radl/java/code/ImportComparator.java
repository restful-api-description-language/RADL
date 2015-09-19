/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.code;

import java.util.Comparator;


public class ImportComparator implements Comparator<String> {

  private static final String[] PRIORITY_PACKAGES = { "java", "javax", "org" };
  
  @Override
  public int compare(String fqn1, String fqn2) {
    String pkg1 = packageOf(fqn1);
    String pkg2 = packageOf(fqn2);
    for (String pkg : PRIORITY_PACKAGES) {
      if (pkg.equals(pkg1)) {
        if (!pkg.equals(pkg2)) {
          return -1;
        }
      } else if (pkg.equals(pkg2)) {
        return 1;
      }
    }
    return fqn1.compareTo(fqn2);
  }

  private String packageOf(String fqn) {
    return fqn.split("\\.")[0];
  }

}
