/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.enforce;

import java.util.Collection;


/**
 * The desired situation. A situation is identified by a number of objects of type <code>T</code> that are each
 * identified by an ID of type <code>I</code>.
 */
public interface Desired<I, T> {

  /**
   * @return The IDs of all the desired objects
   */
  Collection<I> getIds();

  /**
   * @param id The ID of a desired object. This must be an ID returned by {@linkplain #getIds()}
   * @return The desired object identified by the given ID
   */
  T get(I id);

}
