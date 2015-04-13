/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.enforce;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import radl.test.RandomData;


@SuppressWarnings("unchecked")
public class EnforcerTest {

  private static final RandomData RANDOM = new RandomData();

  private final Enforcer<String, String> enforcer = new Enforcer<String, String>();

  @Test
  public void addsMissingDesiredObjects() {
    String id1 = anId();
    String id2 = anId();
    String object1 = anObject();
    String object2 = anObject();

    Desired<String, String> desired = mock(Desired.class);
    when(desired.getIds()).thenReturn(Arrays.asList(id1, id2));
    when(desired.get(id1)).thenReturn(object1);
    when(desired.get(id2)).thenReturn(object2);
    Reality<String, String> reality = mock(Reality.class);
    when(reality.getIds()).thenReturn(Collections.<String>emptyList());

    enforcer.enforce(desired, reality);

    verify(reality, never()).remove(anyString());
    verify(reality).add(id1, object1);
    verify(reality).add(id2, object2);
  }

  private String anId() {
    return RANDOM.string();
  }

  private String anObject() {
    return RANDOM.string();
  }

  @Test
  public void skipsExistingDesiredObjects() {
    String id = anId();
    String object = anObject();

    Desired<String, String> desired = mock(Desired.class);
    when(desired.getIds()).thenReturn(Arrays.asList(id));
    when(desired.get(id)).thenReturn(object);
    Reality<String, String> reality = mock(Reality.class);
    when(reality.getIds()).thenReturn(Arrays.asList(id));
    when(reality.get(id)).thenReturn(object);

    enforcer.enforce(desired, reality);

    verify(reality, never()).remove(anyString());
    verify(reality, never()).add(anyString(), anyString());
  }

  @Test
  public void removesUndesiredObjects() {
    String id = anId();

    Desired<String, String> desired = mock(Desired.class);
    when(desired.getIds()).thenReturn(Collections.<String>emptyList());
    Reality<String, String> reality = mock(Reality.class);
    when(reality.getIds()).thenReturn(Arrays.asList(id));

    enforcer.enforce(desired, reality);

    verify(reality).remove(id);
    verify(reality, never()).add(anyString(), anyString());
  }

  @Test
  public void replacesChangedObjects() {
    String id = anId();
    String oldObject = anObject();
    String newObject = anObject();

    Desired<String, String> desired = mock(Desired.class);
    when(desired.getIds()).thenReturn(Arrays.asList(id));
    when(desired.get(id)).thenReturn(newObject);
    Reality<String, String> reality = mock(Reality.class);
    when(reality.getIds()).thenReturn(Arrays.asList(id));
    when(reality.get(id)).thenReturn(oldObject);

    enforcer.enforce(desired, reality);

    verify(reality).update(id, oldObject, newObject);
  }

  @Test
  public void leavesUnlistedObjectsAlone() {
    String id = anId();

    Desired<String, String> desired = mock(Desired.class);
    when(desired.getIds()).thenReturn(Collections.<String>emptyList());
    Reality<String, String> reality = mock(Reality.class);
    when(reality.getIds()).thenReturn(Arrays.asList(id));

    enforcer.enforce(desired, reality, false);

    verify(reality, never()).remove(id);
    verify(reality, never()).add(anyString(), anyString());
  }

}
