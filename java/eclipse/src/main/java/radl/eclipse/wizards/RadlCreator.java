/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;

import radl.core.Radl;
import radl.eclipse.builder.RadlNature;


/**
 * Create a new RADL document.
 */
public class RadlCreator {

  public IFile createRadlFile(String folderName, String serviceName, IProgressMonitor monitor,
      IWorkspaceRoot root) throws CoreException {
    IFolder folder = root.getFolder(new Path(folderName));
    if (!folder.exists()) {
      ensureFolder(monitor, folder);
    }
    final IFile result = folder.getFile(serviceNameToFileName(serviceName));
    try (InputStream stream = getRadlSkeleton(serviceName)) {
      if (result.exists()) {
        result.setContents(stream, true, true, monitor);
      } else {
        result.create(stream, true, monitor);
      }
    } catch (IOException e) { // NOPMD EmptyCatchBlock
    }
    IProjectNature nature = new RadlNature();
    nature.setProject(folder.getProject());
    nature.configure();
    return result;
  }

  private void ensureFolder(IProgressMonitor monitor, IFolder folder) throws CoreException {
    IContainer parent = folder.getParent();
    if (!parent.exists()) {
      ensureFolder(monitor, (IFolder)parent);
    }
    folder.create(true, false, monitor);
  }

  private String serviceNameToFileName(String serviceName) {
    return serviceName.replaceAll("[^a-zA-Z0-9]", "") + ".radl";
  }

  private InputStream getRadlSkeleton(String serviceName) {
    String contents = String.format("<?xml version=\"1.0\" encoding=\"UTF-8\"?>%n"
        + "<service name=\"%s\" xmlns=\"%s\" "
        + "xmlns:html=\"http://www.w3.org/1999/xhtml\">%n"
        + "  <states>%n"
        + "    <start-state>%n"
        + "      <transitions></transitions>%n"
        + "    </start-state>%n"
        + "  </states>%n"
        + "  <link-relations></link-relations>%n"
        + "  <media-types></media-types>%n"
        + "  <resources></resources>%n"
        + "</service>%n", serviceName, Radl.NAMESPACE_URI);
    try {
      return new ByteArrayInputStream(contents.getBytes("UTF8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

}
