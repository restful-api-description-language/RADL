/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */

package radl.core.extraction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import radl.core.Log;


public final class ResourceModelSerializer {

  private ResourceModelSerializer() {
  }

  public static void serializeModelToFile(ResourceModel resourceModel, File file) {
    Log.info("Saving resource model to file: " + file);
    if (file == null) {
      return;
    }
    try (OutputStream stream = new FileOutputStream(file)) {
      try (ObjectOutputStream output = new ObjectOutputStream(stream)) {
        output.writeObject(resourceModel);
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to write resource model to file.", e);
    }
  }

  public static ResourceModel deserializeModelFromFile(File file) {
    Log.info("Loading resource model from file: " + file);
    try (InputStream stream = new FileInputStream(file)) {
      try (ObjectInputStream input = new ObjectInputStream(stream)) {
        return (ResourceModel)input.readObject();
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to load resource model from file", e);
    }
  }

}
