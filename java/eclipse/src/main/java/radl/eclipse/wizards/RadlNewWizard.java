/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.wizards;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;


/**
 * Wizard for creating a new RADL document.
 */
public class RadlNewWizard extends Wizard implements INewWizard {

  private RadlNewWizardPage page;
  private ISelection selection;
  private final RadlCreator creator = new RadlCreator();

  public RadlNewWizard() {
    super();
    setNeedsProgressMonitor(true);
  }

  @Override
  public void addPages() {
    page = new RadlNewWizardPage(selection);
    addPage(page);
  }

  @Override
  public boolean performFinish() {
    final String folderName = page.getFolderName();
    final String serviceName = page.getServiceName();
    IRunnableWithProgress op = new IRunnableWithProgress() {
      @Override
      public void run(IProgressMonitor monitor) throws InvocationTargetException {
        try {
          doFinish(folderName, serviceName, monitor);
        } catch (CoreException e) {
          throw new InvocationTargetException(e);
        } finally {
          monitor.done();
        }
      }
    };
    try {
      getContainer().run(true, false, op);
    } catch (InterruptedException e) {
      return false;
    } catch (InvocationTargetException e) {
      Throwable realException = e.getTargetException();
      MessageDialog.openError(getShell(), "Error", realException.getMessage());
      return false;
    }
    return true;
  }

  private void doFinish(String folderName, String serviceName, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Creating RADL for API: " + serviceName, 2);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    final IFile file = creator.createRadlFile(folderName, serviceName, monitor, root);
    monitor.worked(1);
    monitor.setTaskName("Opening file for editing...");
    getShell().getDisplay().asyncExec(new OpenEditor(file));
    monitor.worked(1);
  }

  @Override
  public void init(IWorkbench workbench, IStructuredSelection structuredSelection) {
    this.selection = structuredSelection;
  }


  private static final class OpenEditor implements Runnable {

    private final IFile file;

    private OpenEditor(IFile file) {
      this.file = file;
    }

    @Override
    public void run() {
      try {
        IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), file, true);
      } catch (PartInitException e) { // NOPMD EmptyCatchBlock
      }
    }
  }

}
