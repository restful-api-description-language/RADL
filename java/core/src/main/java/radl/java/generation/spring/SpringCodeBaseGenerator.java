/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.List;

import radl.core.generation.CodeBaseGeneratorImpl;
import radl.core.generation.CodeGenerator;
import radl.core.generation.Module;


/**
 * Generates a Java code base for the Spring framework from a RADL document.
 */
public class SpringCodeBaseGenerator extends CodeBaseGeneratorImpl {

  private static final String DEFAULT_HEADER = "Generated from RADL.";

  public SpringCodeBaseGenerator(String packagePrefix) {
    this(packagePrefix, null);
  }

  public SpringCodeBaseGenerator(String packagePrefix, String header) {
    super(packagePrefix, header == null || header.trim().isEmpty() ? DEFAULT_HEADER : header,
        new FromRadlCodeGenerationInitializer(),
        new ActionsGenerator(),
        new DtosGenerator(),
        new ErrorDtoGenerator(),
        new ExceptionsGenerator(),
        new IdentifiableGenerator(),
        new ExceptionHandlerGenerator(),
        new RestResponseGenerator(),
        new ControllersGenerator(),
        new ControllerSupportsGenerator(),
        new UrisGenerator(),
        new ApiGenerator());
  }

  @Override
  protected Module moduleFor(List<Module> modules, CodeGenerator generator) {
    if (generator instanceof ControllerSupportsGenerator) {
      return modules.get(1);
    }
    return super.moduleFor(modules, generator);
  }

}
