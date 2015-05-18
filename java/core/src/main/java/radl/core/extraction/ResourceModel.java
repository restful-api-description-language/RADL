/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;


/**
 * A set of interlinked HTTP resources.
 */
public interface ResourceModel {

  void configure(Properties configuration);

  void addResource(String resourceName, String documentation);

  void addParentResource(String childResource, String parentResource);

  void addLocations(String resourceName, Collection<String> lcoations);

  void setLocations(String resourceName, Collection<String> locations);

  void addLocationVar(String resourceName, String varName, String documentation);

  Iterable<String> getLocationVars(String resourceName);

  String getLocationVarDocumentation(String resourceName, String varName);

  void addMethod(String resourceName, String methodName, String consumes, String produces, String documentation);

  Iterable<String> mediaTypes();

  Set<String> resourcesWithMethods();

  String getFriendlyName(String resourceName);

  String getUri(String resourceName);

  Collection<Method> methodsOf(String resourceName);

  void build();

  String getDocumentation(String resourceName);

}
