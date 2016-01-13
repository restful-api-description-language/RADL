/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import radl.common.io.ByteArrayInputOutputStream;
import radl.common.io.IO;
import radl.core.validation.Issue.Level;


/**
 * Composite pattern for RADL validators.
 */
public class CompositeValidator implements Validator {

  private final Validator[] validators;

  public CompositeValidator(Validator... validators) {
    this.validators = validators;
  }

  @Override
  public void validate(InputStream contents, Collection<Issue> issues) {
   ByteArrayInputOutputStream reusableStream = new ByteArrayInputOutputStream();
    try {
      IO.copy(contents, reusableStream);
    } catch (IOException e) {
      addExceptionIssue(e, issues);
    }
    for (Validator validator : validators) {
      try (InputStream stream = reusableStream.getInputStream()) {
        validator.validate(stream, issues);
      } catch (IOException e) {
        addExceptionIssue(e, issues);
      }
    }
  }

  private boolean addExceptionIssue(IOException exception, Collection<Issue> issues) {
    return issues.add(new Issue(getClass(), Level.ERROR, 0, 0, exception.toString()));
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (Validator validator : validators) {
      result.append(prefix).append(validator.toString());
      prefix = ",";
    }
    return result.toString();
  }

}
