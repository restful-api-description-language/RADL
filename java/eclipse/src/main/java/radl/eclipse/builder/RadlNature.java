/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.builder;

import java.util.Collection;
import java.util.LinkedHashSet;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;


/**
 * Project nature that adds a {@linkplain RadlBuilder} to the project's builders.
 */
public class RadlNature implements IProjectNature {

  public static final String NATURE_ID = "radl.eclipse.radlNature";

  private IProject project;

  @Override
  public void configure() throws CoreException {
    IProjectDescription desc = getProject().getDescription();
    ICommand[] commands = desc.getBuildSpec();
    boolean found = false;
    for (ICommand command : commands) {
      if (command.getBuilderName().equals(RadlBuilder.BUILDER_ID)) {
        found = true;
        break;
      }
    }
    if (!found) {
      ICommand[] newCommands = new ICommand[commands.length + 1];
      System.arraycopy(commands, 0, newCommands, 0, commands.length);
      ICommand command = desc.newCommand();
      command.setBuilderName(RadlBuilder.BUILDER_ID);
      newCommands[newCommands.length - 1] = command;
      desc.setBuildSpec(newCommands);
    }
    desc.setNatureIds(ensureNature(desc.getNatureIds()));
    getProject().setDescription(desc, null);
  }

  private String[] ensureNature(String[] natureIds) {
    Collection<String> result = new LinkedHashSet<String>();
    for (String natureId : natureIds) {
      result.add(natureId);
    }
    result.add(RadlNature.NATURE_ID);
    return result.toArray(new String[result.size()]);
  }

  @Override
  public void deconfigure() throws CoreException {
    IProjectDescription description = getProject().getDescription();
    ICommand[] commands = description.getBuildSpec();
    for (int i = 0; i < commands.length; ++i) {
      if (commands[i].getBuilderName().equals(RadlBuilder.BUILDER_ID)) {
        ICommand[] newCommands = new ICommand[commands.length - 1];
        System.arraycopy(commands, 0, newCommands, 0, i);
        System.arraycopy(commands, i + 1, newCommands, i, commands.length - i - 1);
        description.setBuildSpec(newCommands);
        getProject().setDescription(description, null);
        return;
      }
    }
  }

  @Override
  public IProject getProject() {
    return project;
  }

  @Override
  public void setProject(IProject project) {
    this.project = project;
  }

}
