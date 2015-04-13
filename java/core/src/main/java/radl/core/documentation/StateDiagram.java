/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;


/**
 * Diagram with states and transitions between them that can be saved to an image.
 */
public interface StateDiagram {

  void setTitle(String title);

  void addStartState(String name);

  void addState(String name);

  void addTransition(String from, String to, String name);

  void toImage(File image);

}
