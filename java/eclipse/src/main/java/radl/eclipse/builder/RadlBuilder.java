/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.builder;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import radl.core.validation.CompositeValidator;
import radl.core.validation.LintValidator;
import radl.core.validation.RelaxNgValidator;
import radl.core.validation.Validator;


/**
 * Project builder that validates RADL documents.
 */
public class RadlBuilder extends IncrementalProjectBuilder {

  public static final String BUILDER_ID = "radl.eclipse.radlBuilder";

  private final RadlValidatingVisitor radlValidatingVisitor;

  public RadlBuilder() {
    this(new CompositeValidator(new RelaxNgValidator(), new LintValidator()));
  }

  RadlBuilder(Validator validator) {
    radlValidatingVisitor = new RadlValidatingVisitor(validator);
  }

  @Override
  protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map args, IProgressMonitor monitor)
      throws CoreException {
    if (kind == FULL_BUILD) {
      fullBuild();
    } else {
      IResourceDelta delta = getDelta(getProject());
      if (delta == null) {
        fullBuild();
      } else {
        incrementalBuild(delta);
      }
    }
    return new IProject[0];
  }

  @Override
  protected void clean(IProgressMonitor monitor) throws CoreException {
    getProject().deleteMarkers(RadlValidatingVisitor.MARKER_TYPE, true, IResource.DEPTH_INFINITE);
  }

  private void fullBuild() throws CoreException {
    getProject().accept(radlValidatingVisitor);
  }

  private void incrementalBuild(IResourceDelta delta) throws CoreException {
    delta.accept(radlValidatingVisitor);
  }

}
