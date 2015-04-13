/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;


/**
 * Singleton pattern for {@linkplain ResourceModel}.
 */
public final class ResourceModelHolder {

  private static ResourceModel instance;

  private ResourceModelHolder() {
    // Singleton
  }

  public static synchronized ResourceModel getInstance() {
    if (instance == null) {
      instance = new ResourceModelImpl();
    }
    return instance;
  }

  public static void setInstance(ResourceModel instance) {
    ResourceModelHolder.instance = instance;
  }

}
