/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.eclipse.builder;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.internal.resources.Marker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import radl.common.io.StringStream;
import radl.core.validation.Issue;
import radl.core.validation.Issue.Level;
import radl.core.validation.Validator;
import radl.test.RandomData;


public class RadlValidatingVisitorTest {

  private static final RandomData RANDOM = new RandomData();

  private final Validator validator = mock(Validator.class);
  private final RadlValidatingVisitor visitor = new RadlValidatingVisitor(validator);

  @SuppressWarnings("unchecked")
  @Test
  public void createsMarkersForRadlIssues() throws CoreException {
    StringStream contents = new StringStream(RANDOM.string());
    IResourceDelta delta = someResourceDelta();
    IFile file = (IFile)delta.getResource();
    when(file.getContents()).thenReturn(contents);
    final String error = RANDOM.string();
    final int line1 = RANDOM.integer();
    final int column1 = RANDOM.integer();
    final String warning = RANDOM.string();
    final int line2 = RANDOM.integer();
    final int column2 = RANDOM.integer();
    doAnswer(new Answer<Void>() {
      @Override
      public Void answer(InvocationOnMock invocation) throws Throwable {
        Collection<Issue> issues = (Collection<Issue>)invocation.getArguments()[1];
        issues.add(new Issue(Validator.class, Level.ERROR, line1, column1, error));
        issues.add(new Issue(Validator.class, Level.WARNING, line2, column2, warning));
        return null;
      }
    }).when(validator).validate(eq(contents), any(Collection.class));
    final AtomicInteger numMarkers = new AtomicInteger();
    final Marker errorMarker = mock(Marker.class);
    final Marker warningMarker = mock(Marker.class);
    when(file.createMarker(RadlValidatingVisitor.MARKER_TYPE)).thenAnswer(new Answer<Marker>() {
      @Override
      public Marker answer(InvocationOnMock invocation) throws Throwable {
        return numMarkers.incrementAndGet() == 1 ? errorMarker : warningMarker;
      }
    });

    visitor.visit(delta);

    verify(errorMarker).setAttribute(Marker.SEVERITY, Marker.SEVERITY_ERROR);
    verify(errorMarker).setAttribute(Marker.LINE_NUMBER, line1);
    verify(errorMarker).setAttribute(Marker.MESSAGE, error);
    verify(warningMarker).setAttribute(Marker.SEVERITY, Marker.SEVERITY_WARNING);
    verify(warningMarker).setAttribute(Marker.LINE_NUMBER, line2);
    verify(warningMarker).setAttribute(Marker.MESSAGE, warning);
  }

  private IResourceDelta someResourceDelta() {
    IFile file = mock(IFile.class);
    when(file.getFileExtension()).thenReturn("radl");
    IResourceDelta delta = mock(IResourceDelta.class);
    when(delta.getResource()).thenReturn(file);
    when(delta.getKind()).thenReturn(IResourceDelta.CHANGED);
    return delta;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void validatesNewlyAddedRadlFile() throws CoreException {
    IResourceDelta delta = someResourceDelta();
    when(delta.getKind()).thenReturn(IResourceDelta.ADDED);

    visitor.visit(delta);

    verify(validator).validate(any(InputStream.class), any(Collection.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void doesntValidateDeletedRadlFile() throws CoreException {
    IResourceDelta delta = someResourceDelta();
    when(delta.getKind()).thenReturn(IResourceDelta.REMOVED);

    visitor.visit(delta);

    verify(validator, never()).validate(any(InputStream.class), any(Collection.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void doesntValidateNonRadlFile() throws CoreException {
    IResourceDelta delta = someResourceDelta();
    when(delta.getResource().getFileExtension()).thenReturn(RANDOM.string(3));

    visitor.visit(delta);

    verify(validator, never()).validate(any(InputStream.class), any(Collection.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void doesntValidateNonFile() throws CoreException {
    IResourceDelta delta = someResourceDelta();
    when(delta.getResource()).thenReturn(mock(IResource.class));

    visitor.visit(delta);

    verify(validator, never()).validate(any(InputStream.class), any(Collection.class));
  }

  @SuppressWarnings("unchecked")
  @Test
  public void validatesDuringFullBuild() throws CoreException {
    IResource resource = someResourceDelta().getResource();

    visitor.visit(resource);

    verify(validator).validate(any(InputStream.class), any(Collection.class));
  }

}
