/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.enforce;


/**
 * The real situation. A situation is identified by a number of objects of type <code>T</code> that are each
 * identified by an ID of type <code>I</code>.
 */
public interface Reality<I, T> extends Desired<I, T> {

  /**
   * Add a desired object to reality.
   * @param id The ID of the desired object
   * @param object The desired object
   */
  void add(I id, T object);

  /**
   * Remove an unwanted object from reality.
   * @param id The ID of the unwanted object
   */
  void remove(I id);

  /**
   * Replace an object by a desired version of it.
   * @param id The ID of the object to update
   * @param oldObject The object as it currently exists in reality
   * @param newObject The object as it is desired
   */
  void update(I id, T oldObject, T newObject);

}
