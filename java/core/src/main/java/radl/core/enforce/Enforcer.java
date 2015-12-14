/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.enforce;

import java.util.ArrayList;
import java.util.Collection;


/**
 * Turn a desired situation into reality.  A situation is identified by a number of objects of type <code>T</code>
 * that are each * identified by an ID of type <code>I</code>.
 */
public class Enforcer<I, T> {

  /**
   * Make sure that reality is exactly as desired.
   * @param desired The desired situation
   * @param reality The actual situation
   */
  public void enforce(Desired<I, T> desired, Reality<I, T> reality) {
    enforce(desired, reality, true);
  }

  /**
   * Make sure that reality matches expectations.
   * @param desired The desired situation
   * @param reality The actual situation
   * @param removeUnlisted Whether to remove objects from reality that are not desired
   */
  public void enforce(Desired<I, T> desired, Reality<I, T> reality, boolean removeUnlisted) {
    Collection<I> toRemove = new ArrayList<>();
    Collection<I> toUpdate = new ArrayList<>();
    Collection<I> toAdd = new ArrayList<>();
    collectUpdates(desired, reality, toRemove, toUpdate, toAdd);
    if (removeUnlisted) {
      remove(toRemove, reality);
    }
    update(toUpdate, desired, reality);
    add(toAdd, desired, reality);
  }

  private void collectUpdates(Desired<I, T> desired, Reality<I, T> reality,
      Collection<I> toRemove, Collection<I> toUpdate, Collection<I> toAdd) {
    Collection<I> wantedIds = new ArrayList<>(desired.getIds());
    for (I id : reality.getIds()) {
      if (wantedIds.contains(id)) {
        wantedIds.remove(id);
        if (!isSame(id, desired, reality)) {
          toUpdate.add(id);
        }
      } else {
        toRemove.add(id);
      }
    }
    toAdd.addAll(wantedIds);
  }

  private boolean isSame(I id, Desired<I, T> desired, Reality<I, T> reality) {
    T expected = desired.get(id);
    T actual = reality.get(id);
    return expected.equals(actual);
  }

  private void remove(Collection<I> toRemove, Reality<I, T> reality) {
    for (I id : toRemove) {
      reality.remove(id);
    }
  }

  private void update(Collection<I> toUpdate, Desired<I, T> desired, Reality<I, T> reality) {
    for (I id : toUpdate) {
      T object = desired.get(id);
      reality.update(id, reality.get(id), object);
    }
  }

  private void add(Collection<I> toAdd, Desired<I, T> desired, Reality<I, T> reality) {
    for (I id : toAdd) {
      T object = desired.get(id);
      reality.add(id, object);
    }
  }

}
