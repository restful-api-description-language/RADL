/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.documentation;

import java.io.File;

/* TODO: Not supported on Java 6
import com.mxgraph.layout.hierarchical.mxHierarchicalLayout;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxConstants;
import com.mxgraph.view.mxGraph;
*/


/**
 * State diagram built using the JGraph library.
 */
public class JGraphStateDiagram implements StateDiagram {

  /*
  private static final String VERTEX_STYLE
      = String.format("%s=%s", mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
  private static final String START_VERTEX_STYLE = VERTEX_STYLE + ";fillColor=#FFDEAD;strokeColor=maroon";
  private static final String EDGE_STYLE = String.format("%s=%s;%s=black", mxConstants.STYLE_ALIGN,
      mxConstants.ALIGN_LEFT, mxConstants.STYLE_FONTCOLOR);

  private final mxGraph graph;
  private final Map<String, Object> cells = new HashMap<>();
  private final Object parent;

  public JGraphStateDiagram() {
    graph = new mxGraph();
    parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
  }
  */

  @Override
  public void setTitle(String title) {
    // Nothing to do
  }

  @Override
  public void addStartState(String name) {
    //doAddState(name, START_VERTEX_STYLE);
  }

  @Override
  public void addState(String name) {
//    doAddState(name, VERTEX_STYLE);
  }

  /*
  protected Object doAddState(String name, String style) {
    int count = graph.getModel().getChildCount(parent);
    return cells.put(name, graph.insertVertex(parent, null, name, count, count, name.length() * 10 + 10, 30, style));
  }
  */

  @Override
  public void addTransition(String from, String to, String name) {
//    graph.insertEdge(parent, null, name, cells.get(from), cells.get(to), EDGE_STYLE);
  }

  @Override
  public void toImage(File file) {
    /*
    new mxHierarchicalLayout(graph).execute(parent);
    graph.getModel().endUpdate();
    RenderedImage image = mxCellRenderer.createBufferedImage(graph, null, 1, Color.WHITE, false, null);
    try {
      ImageIO.write(image, "png", file);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    */
  }

}
