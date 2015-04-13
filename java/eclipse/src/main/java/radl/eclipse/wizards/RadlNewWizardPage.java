/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.wizards;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;


/**
 * The single page in the {@linkplain RadlNewWizard}.
 */
public class RadlNewWizardPage extends WizardPage {

  private Text folderText;
  private Text serviceText;
  private final ISelection selection;
  private final IWorkspaceRoot root;

  public RadlNewWizardPage(ISelection selection) {
    super("wizardPage");
    setTitle("New RESTful API");
    setDescription("Create a new RESTful API Description Language (RADL) document");
    this.selection = selection;
    root = ResourcesPlugin.getWorkspace().getRoot();
  }

  @Override
  public void createControl(Composite parent) {
    addControls(parent);
    initialize();
    validateInput();
    focus(serviceText);
  }

  private void focus(Text control) {
    setControl(control);
    control.selectAll();
  }

  private void addControls(Composite parent) {
    Composite surface = newSurface(parent);
    addServiceControl(surface);
    addFolderControl(surface);
  }

  private Composite newSurface(Composite parent) {
    Composite result = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    result.setLayout(layout);
    layout.numColumns = 3;
    layout.verticalSpacing = 9;
    return result;
  }

  private void addServiceControl(Composite parent) {
    Label label = new Label(parent, SWT.NULL);
    label.setText("REST &API:");
    serviceText = new Text(parent, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    serviceText.setLayoutData(gd);
    serviceText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validateInput();
      }
    });
    new Label(parent, SWT.NULL);
  }

  private void addFolderControl(Composite parent) {
    Label label = new Label(parent, SWT.NULL);
    label.setText("&Location:");
    folderText = new Text(parent, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    folderText.setLayoutData(gd);
    folderText.addModifyListener(new ModifyListener() {
      @Override
      public void modifyText(ModifyEvent e) {
        validateInput();
      }
    });
    Button button = new Button(parent, SWT.PUSH);
    button.setText("&Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        selectFolder();
      }
    });
  }

  private void initialize() {
    serviceText.setText("New Service");
    Object selectedObject = getSelectedObject();
    if (selectedObject instanceof IJavaElement) {
      selectedObject = ((IJavaElement)selectedObject).getResource();
    }
    if (selectedObject instanceof IResource) {
      setFolder(((IResource)selectedObject).getProject());
      return;
    }
    IProject project = selectProject(root.getProjects());
    if (project != null) {
      setFolder(project);
    }
  }

  private Object getSelectedObject() {
    Object result = null;
    if (selection instanceof IStructuredSelection && !selection.isEmpty()) {
      IStructuredSelection ssel = (IStructuredSelection)selection;
      if (ssel.size() >= 1) {
        result = ssel.getFirstElement();
      }
    }
    return result;
  }

  private IProject selectProject(IProject[] projects) {
    for (IProject project : projects) {
      if (isJavaProject(project)) {
        return project;
      }
    }
    return null;
  }

  private boolean isJavaProject(IProject project) {
    try {
      return !project.isHidden() && project.isOpen() && project.getNature("org.eclipse.jdt.core.javanature") != null;
    } catch (CoreException e) {
      return false;
    }
  }

  private void setFolder(IProject project) {
    IFolder folder = project.getFolder("src/main/radl");
    folderText.setText(folder.getFullPath().toString());
    serviceText.setText(project.getName());
  }

  private void selectFolder() {
    ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), root, false, "Select Location");
    if (dialog.open() == ContainerSelectionDialog.OK) {
      Object[] result = dialog.getResult();
      if (result.length == 1) {
        folderText.setText(((Path)result[0]).toString());
      }
    }
  }

  private void validateInput() {
    if (getFolderName().length() == 0) {
      updateStatus("Folder must be specified");
      return;
    }
    IResource resource = root.findMember(new Path(getFolderName()));
    if (resource != null && resource.exists() && !(resource instanceof IFolder)) {
      updateStatus("Invalid folder");
      return;
    }
    String serviceName = getServiceName();
    if (serviceName.isEmpty()) {
      updateStatus("Service name must be specified");
      return;
    }
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getFolderName() {
    return folderText.getText().trim();
  }

  public String getServiceName() {
    return serviceText.getText().trim();
  }

}
