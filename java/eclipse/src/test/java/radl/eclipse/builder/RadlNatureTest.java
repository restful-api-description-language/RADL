/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.builder;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;


public class RadlNatureTest {

  private final IProject project = mock(IProject.class);
  private final IProjectNature nature = new RadlNature();
  private final Collection<ICommand> commands = new ArrayList<ICommand>();

  @Test
  public void configureAddsBuilderWhenNotPresent() throws CoreException {
    init();

    nature.configure();

    assertBuilder(true);
  }

  private void init() throws CoreException {
    nature.setProject(project);
    IProjectDescription projectDescription = mock(IProjectDescription.class);
    when(project.getDescription()).thenReturn(projectDescription);
    when(projectDescription.getNatureIds()).thenReturn(new String[0]);
    when(projectDescription.newCommand()).thenReturn(new Command());
    when(projectDescription.getBuildSpec()).thenReturn(commands.toArray(new ICommand[commands.size()]));
    final Collection<ICommand> cache = new ArrayList<ICommand>();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        for (ICommand command : (ICommand[])invocation.getArguments()[0]) {
          cache.add(command);
        }
        return null;
      }

    }).when(projectDescription).setBuildSpec(any(ICommand[].class));
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        commands.clear();
        commands.addAll(cache);
        return null;
      }
    }).when(project).setDescription(eq(projectDescription), any(IProgressMonitor.class));
  }

  private void assertBuilder(boolean expected) throws CoreException {
    boolean found = false;
    for (ICommand command : commands) {
      if (RadlBuilder.BUILDER_ID.equals(command.getBuilderName())) {
        found = true;
        break;
      }
    }
    assertEquals("Has RADL builder", expected, found);
  }

  @Test
  public void deconfigureRemovesBuilderWhenPresent() throws CoreException {
    ICommand command = new Command();
    command.setBuilderName(RadlBuilder.BUILDER_ID);
    commands.add(command);
    init();

    nature.deconfigure();

    assertBuilder(false);
  }


  private static final class Command implements ICommand {

    private String builder;

    @Override
    public String getBuilderName() {
      return builder;
    }

    @Override
    public void setBuilderName(String arg0) {
      builder = arg0;
    }

    @Override
    public Map<String, String> getArguments() {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isBuilding(int arg0) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConfigurable() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setArguments(@SuppressWarnings("rawtypes") Map arg0) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setBuilding(int arg0, boolean arg1) {
      throw new UnsupportedOperationException();
    }

  }

}
