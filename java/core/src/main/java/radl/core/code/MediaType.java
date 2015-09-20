/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.code;

import java.util.Arrays;
import java.util.Collection;


public class MediaType {

  private static final Collection<String> SEMANTIC_MEDIA_TYPES = Arrays.asList(
      "application/ld+json");
  private static final Collection<String> HYPER_MEDIA_TYPES = Arrays.asList(
      "application/hal+json",
      "application/vnd.collection+json",
      "application/vnd.mason+json",
      "application/vnd.siren+json",
      "application/vnd.uber+json",
      "application/vnd.uber+xml",
      "application/atom+xml");
  
  private final String name;
  
  public MediaType(String name) {
    this.name = name;
  }
  
  public String name() {
    return name;
  }
  
  public boolean isHyperMediaType() {
    return SEMANTIC_MEDIA_TYPES.contains(name) || HYPER_MEDIA_TYPES.contains(name);
  }

  public boolean isSemanticMediaType() {
    return SEMANTIC_MEDIA_TYPES.contains(name);
  }

}
