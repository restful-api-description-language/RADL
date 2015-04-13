/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.builder;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;

import radl.core.validation.Issue;
import radl.core.validation.Issue.Level;
import radl.core.validation.Validator;


/**
 *  Resource visitor that validates RADL documents.
 */
class RadlValidatingVisitor implements IResourceDeltaVisitor, IResourceVisitor {

  public static final String MARKER_TYPE = "radl.eclipse.radlProblem";

  private final Validator validator;

  public RadlValidatingVisitor(Validator validator) {
    this.validator = validator;
  }

  @Override
  public boolean visit(IResourceDelta delta) throws CoreException {
    IResource resource = delta.getResource();
    switch (delta.getKind()) {
      case IResourceDelta.ADDED:
      case IResourceDelta.CHANGED:
        validateRadl(resource);
        break;
      default:
        // Do nothing
        break;
    }
    // Continue visiting children
    return true;
  }

  private void validateRadl(IResource resource) throws CoreException {
    if ("radl".equals(resource.getFileExtension()) && resource instanceof IFile) {
      IFile file = (IFile)resource;
      Collection<Issue> issues = new ArrayList<Issue>();
      validator.validate(file.getContents(), issues);
      for (Issue issue : issues) {
        IMarker marker = resource.createMarker(MARKER_TYPE);
        marker.setAttribute(IMarker.SEVERITY, levelToSeverity(issue.getLevel()));
        marker.setAttribute(IMarker.LINE_NUMBER, issue.getLine());
        marker.setAttribute(IMarker.MESSAGE, issue.getMessage());
      }
    }
  }

  private int levelToSeverity(Level level) {
    switch (level) {
      case ERROR: return Marker.SEVERITY_ERROR;
      case WARNING: return Marker.SEVERITY_WARNING;
      default: return Marker.SEVERITY_INFO;
    }
  }

  @Override
  public boolean visit(IResource resource) throws CoreException {
    validateRadl(resource);
    return true;
  }

}
