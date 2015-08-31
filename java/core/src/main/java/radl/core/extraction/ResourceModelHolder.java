/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

/**
 * Singleton pattern for {@linkplain ResourceModel}.
 */
public enum ResourceModelHolder {

  INSTANCE;

  private ResourceModel resourceModel= new ResourceModelImpl();

  public ResourceModel get() {
    return this.resourceModel;
  }

  public void set(ResourceModel given) {
    this.resourceModel = given;
  }
}
