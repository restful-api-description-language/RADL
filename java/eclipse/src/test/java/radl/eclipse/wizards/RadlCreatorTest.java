/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.wizards;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.Radl;
import radl.eclipse.builder.RadlNature;
import radl.test.RandomData;


public class RadlCreatorTest {

  private static final RandomData RANDOM = new RandomData();

  private final RadlCreator creator = new RadlCreator();
  private final IWorkspaceRoot root = mock(IWorkspaceRoot.class);
  private final IProgressMonitor monitor = mock(IProgressMonitor.class);
  private final String folderName = RANDOM.string();
  private final String serviceName = RANDOM.string();
  private final IFolder folder = mock(IFolder.class);
  private final IFile file = mock(IFile.class);
  private final IProject project = mock(IProject.class);
  private final Collection<String> natureIds = new ArrayList<String>();
  private InputStream contents;

  @Before
  public void init() throws CoreException {
    IProjectDescription description = mock(IProjectDescription.class);
    when(description.getBuildSpec()).thenReturn(new ICommand[0]);
    when(description.newCommand()).thenReturn(mock(ICommand.class));
    when(description.getNatureIds()).thenReturn(natureIds.toArray(new String[natureIds.size()]));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        natureIds.clear();
        natureIds.addAll(Arrays.asList((String[])invocation.getArguments()[0]));
        return null;
      }
    }).when(description).setNatureIds(any(String[].class));
    when(project.getDescription()).thenReturn(description);
    when(folder.getProject()).thenReturn(project);
    when(folder.exists()).thenReturn(true);
    when(folder.getFile(anyString())).thenReturn(file);
    when(root.getFolder(new Path(folderName))).thenReturn(folder);
  }

  @Test
  public void createsMissingFolders() throws IOException, CoreException {
    when(folder.exists()).thenReturn(false);
    IFolder parent = mock(IFolder.class);
    IFolder grandParent = mock(IFolder.class);
    when(grandParent.getParent()).thenReturn(grandParent);
    when(grandParent.exists()).thenReturn(true);
    when(parent.getParent()).thenReturn(grandParent);
    when(parent.exists()).thenReturn(false);
    when(folder.getParent()).thenReturn(parent);
    when(folder.getFile(anyString())).thenReturn(mock(IFile.class));

    createRadl();

    verify(parent).create(true, false, monitor);
    verify(folder).create(true, false, monitor);
  }

  private void createRadl() throws CoreException {
    creator.createRadlFile(folderName, serviceName, monitor, root);
  }

  @Test
  public void createsRadlFile() throws Exception {
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        contents = (InputStream)invocation.getArguments()[0];
        return null;
      }
    }).when(file).create(any(InputStream.class), any(Boolean.class), eq(monitor));

    createRadl();

    assertContents();
  }

  private void assertContents() throws IOException {
    assertEquals("Contents", expectedContents(), Xml.toString(Xml.parse(contents)));
  }

  private String expectedContents() {
    return Xml.toString(DocumentBuilder.newDocument()
        .namespace(Radl.NAMESPACE_URI)
        .element("service")
            .attribute("name", serviceName)
            .element("states")
                .element("start-state")
                    .element("transitions")
                    .end()
                .end()
            .end()
            .element("link-relations")
            .end()
            .element("media-types")
            .end()
            .element("resources")
            .end()
    .build());
  }

  @Test
  public void updatesRadlFileWhenItAlreadyExists() throws Exception {
    when(file.exists()).thenReturn(true);
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        contents = (InputStream)invocation.getArguments()[0];
        return null;
      }
    }).when(file).setContents(any(InputStream.class), any(Boolean.class), any(Boolean.class), eq(monitor));

    createRadl();

    assertContents();
  }

  @Test
  public void configuresNatureForProject() throws Exception {
    createRadl();

    verify(project).setDescription(any(IProjectDescription.class), isNull(IProgressMonitor.class));
    assertTrue("Nature not set on project", natureIds.contains(RadlNature.NATURE_ID));
  }

}
