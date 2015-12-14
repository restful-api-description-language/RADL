/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.core.extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import radl.core.Log;


public final class ResourceModelSerializer {

  private ResourceModelSerializer() {
  }

  public static void serializeModelToFile(ResourceModel resourceModel, File file) {
    Log.info("Saving resource model to file: " + file);
    if (file == null) {
      return;
    }
    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
      oos.writeObject(resourceModel);
    } catch (Exception e) {
      throw new RuntimeException("Failed to write resource model to file.", e);
    }
  }

  public static ResourceModel deserializeModelFromFile(File file) {
    Log.info("Loading resource model from file: " + file);
    try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
      return (ResourceModel)ois.readObject();
    } catch (Exception e) {
      throw new RuntimeException("Failed to load resource model from file", e);
    }
  }

}
