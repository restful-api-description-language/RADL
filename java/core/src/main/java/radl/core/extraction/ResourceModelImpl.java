/*
 * Copyright (c) EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import radl.core.Log;


/**
 * Default implementation of {@linkplain ResourceModel}.
 */
public class ResourceModelImpl implements ResourceModel, Serializable {

  private static final String NAME_SEPARATOR = ".";
  private static final String PATH_SEPARATOR = "/";
  private static final String[] REMOVABLE_CLASS_SUFFIXES = { "Controller", "RestResource", "Resource" };

  private final Collection<String> resources = new HashSet<>();
  private final Map<String, Collection<Method>> methodsByResource = new HashMap<>();
  private final Map<String, Collection<String>> locationsByResource = new HashMap<>();
  private final Map<String, Collection<UriTemplateVar>> locationVarsByResource = new HashMap<>();
  private final Map<String, Collection<String>> parentResourcesByChild = new LinkedHashMap<>();
  private final Map<String, Collection<String>> childResourcesByParent = new LinkedHashMap<>();
  private final Map<String, String> documentationByResource = new HashMap<>();
  private final Map<String, String> overrideNames = new HashMap<>();
  private final Collection<String> ignorePackageParts = new HashSet<>();
  private final Collection<String> ignoreResourceParts = new HashSet<>();
  private final Collection<String> resourcesToLog = new HashSet<>();
  private final AtomicInteger counter = new AtomicInteger();
  private boolean shouldSimplifyResourceNames;
  private boolean completed;

  @Override public void markComplete() {
    this.completed = true;
  }

  @Override public boolean isCompleted() {
    return this.completed;
  }

  @Override
  public void addResource(String resourceName, String documentation) {
    if (isIgnorable(resourceName)) {
      return;
    }
    if (resources.add(resourceName)) {
      logResource(resourceName, "is added");
    }
    if (documentation != null) {
      documentationByResource.put(resourceName, documentation);
    }
  }

  private boolean isIgnorable(String name) {
    for (String part : name.split("\\" + NAME_SEPARATOR)) {
      if (ignoreResourceParts.contains(part)) {
        return true;
      }
    }
    return false;
  }

  private void logResource(String resourceName, String message) {
    for (String resourceToLog : resourcesToLog) {
      if (resourceName.contains(resourceToLog)) {
        log(resourceName, message);
      }
    }
  }

  private void log(String resourceName, String message) {
    Log.info(resourceName + " - " + message);
  }

  @Override
  public void addParentResource(String childResource, String parentResource) {
    if (isIgnorable(childResource) || isIgnorable(parentResource)) {
      return;
    }
    if (parentResource.equals(childResource)) {
      return;
    }
    if (ancestorOrSelfNames(parentResource).contains(childResource)) {
      Log.error("Parent-child cycle between " + parentResource + " and " + childResource);
    }
    Collection<String> parents = parentResourcesByChild.get(childResource);
    if (parents == null) {
      parents = new HashSet<>();
      parentResourcesByChild.put(childResource, parents);
    }
    if (parents.add(parentResource)) {
      logResource(parentResource, "is parent of " + childResource);
      logResource(childResource, "is child of " + parentResource);
      Collection<String> children = childResourcesByParent.get(parentResource);
      if (children == null) {
        children = new ArrayList<>();
        childResourcesByParent.put(parentResource, children);
      }
      children.add(childResource);
    }
  }

  @Override
  public void configure(Properties configuration) {
    configureOverrideNames(configuration);
    configureIgnorePackageParts(configuration);
    configureIgnoreResourceParts(configuration);
    configureResourcesToLog(configuration);
    configureSimplify(configuration);
  }

  private void configureOverrideNames(Properties configuration) {
    String overrides = configuration.getProperty("resources.override.names");
    if (overrides != null) {
      for (String override : overrides.split(",")) {
        int index = override.indexOf(':');
        String resourceName = override.substring(0, index);
        String overrideName = override.substring(index + 1);
        overrideNames.put(resourceName, overrideName);
      }
    }
  }

  private void configureIgnorePackageParts(Properties configuration) {
    String ignores = configuration.getProperty("resources.ignore.package.parts");
    if (ignores != null) {
      for (String ignore : ignores.split(",")) {
        ignorePackageParts.add(ignore);
      }
    }
  }

  private void configureIgnoreResourceParts(Properties configuration) {
    String ignores = configuration.getProperty("resources.ignore.parts");
    if (ignores != null) {
      for (String ignore : ignores.split(",")) {
        ignoreResourceParts.add(ignore);
      }
    }
  }

  private void configureResourcesToLog(Properties configuration) {
    String toLog = configuration.getProperty("resources.log");
    if (toLog != null) {
      for (String resource : toLog.split(",")) {
        resourcesToLog.add(resource);
      }
    }
  }

  private void configureSimplify(Properties configuration) {
    shouldSimplifyResourceNames = Boolean.parseBoolean(configuration.getProperty("resources.simplify",
        Boolean.toString(true)));
  }

  @Override
  public void addMethod(String resourceName, String methodName, String consumes, String produces,
      String documentation) {
    if (isIgnorable(resourceName)) {
      return;
    }
    Collection<Method> resourceMethods = methodsByResource.get(resourceName);
    if (resourceMethods == null) {
      resourceMethods = new TreeSet<>();
      methodsByResource.put(resourceName, resourceMethods);
    }
    Method method = new Method(methodName, documentation, consumes, produces);
    Method existing = null;
    for (Method m : resourceMethods) {
      if (m.equals(method)) {
        existing = m;
        break;
      }
    }
    if (existing != null) {
      resourceMethods.remove(existing);
      method = existing.combineWith(method);
    }
    if (resourceMethods.add(method)) {
      logResource(resourceName, "has method " + methodName);
    }
  }

  @Override
  public void addLocations(String resourceName, Collection<String> uris) {
    if (isIgnorable(resourceName)) {
      return;
    }
    Collection<String> locations = locationsByResource.get(resourceName);
    if (locations == null) {
      locations = new LinkedHashSet<>();
    }
    if (locations.addAll(removeValidation(uris))) {
      logResource(resourceName, "is located at " + uris);
    }
    locationsByResource.put(resourceName, locations);
  }

  private Collection<String> removeValidation(Collection<String> uris) {
    Collection<String> result = new HashSet<>();
    for (String uri : uris) {
      result.add(removeValidation(uri));
    }
    return result;
  }

  private String removeValidation(String uri) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String part : uri.split("/")) {
      result.append(prefix);
      if (isTemplateVariable(part)) {
        int index = part.indexOf(':');
        if (index < 0) {
          result.append(part);
        } else {
          result.append(part.substring(0, index)).append('}');
        }
      } else {
        result.append(part);
      }
      prefix = "/";
    }
    if (uri.endsWith("/")) {
      result.append('/');
    }
    return result.toString();
  }

  @Override
  public void setLocations(String resourceName, Collection<String> uris) {
    if (isIgnorable(resourceName)) {
      return;
    }
    logResource(resourceName, "locations overridden");
    locationsByResource.remove(resourceName);
    addLocations(resourceName, uris);
  }

  @Override
  public void addLocationVar(String resourceName, String varName, String documentation) {
    logResource(resourceName, "location var: " + varName);
    Collection<UriTemplateVar> vars = locationVarsByResource.get(resourceName);
    if (vars == null) {
      vars = new ArrayList<>();
      locationVarsByResource.put(resourceName, vars);
    }
    UriTemplateVar found = null;
    for (UriTemplateVar var : vars) {
      if (var.getName().equals(varName)) {
        if (documentation == null || documentation.equals(var.getDocumentation())) {
          return;
        }
        found = var;
      }
    }
    vars.add(new UriTemplateVar(varName, documentation));
    vars.remove(found);
  }

  @Override
  public Iterable<String> getLocationVars(String resourceName) {
    Collection<String> result = new TreeSet<>();
    Collection<UriTemplateVar> vars = locationVarsByResource.get(resourceName);
    if (vars != null) {
      for (UriTemplateVar var : vars) {
        result.add(var.getName());
      }
    }
    return result;
  }

  @Override
  public String getLocationVarDocumentation(String resourceName, String varName) {
    Collection<UriTemplateVar> vars = locationVarsByResource.get(resourceName);
    if (vars == null) {
      return null;
    }
    for (UriTemplateVar var : vars) {
      if (var.getName().equals(varName)) {
        return var.getDocumentation();
      }
    }
    return null;
  }

  @Override
  public Iterable<String> mediaTypes() {
    Collection<String> result = new TreeSet<>();
    for (Collection<Method> methods : methodsByResource.values()) {
      for (Method method : methods) {
        result.addAll(method.getConsumes());
        result.addAll(method.getProduces());
      }
    }
    return result;
  }

  @Override
  public Set<String> resourcesWithMethods() {
    Set<String> result = new TreeSet<String>(new Comparator<String>() {
      @Override
      public int compare(String r1, String r2) {
        String uri1 = getUri(r1);
        String uri2 = getUri(r2);
        if (uri1 == null) {
          return uri2 == null ? r1.compareTo(r2) : -1;
        }
        if (uri2 == null) {
          return 1;
        }
        return uri1.compareTo(uri2);
      }
    });
    result.addAll(methodsByResource.keySet());
    return result;
  }

  @Override
  public void build() {
    resourcesDagToTree();
    resolveLocations();
    duplicateResourcesWithMultipleLocations();
    mergeChildResourcesAtTheSameLocation();
    mergeResourcesWithSameLocationAndDifferentMethods();
    simplifyResourceNames();
  }

  private void resourcesDagToTree() {
    Collection<String> childResourcesWithMultipleParents = getChildResourcesWithMultipleParents();
    while (!childResourcesWithMultipleParents.isEmpty()) {
      for (String child : childResourcesWithMultipleParents) {
        dagToTree(child);
      }
      childResourcesWithMultipleParents = getChildResourcesWithMultipleParents();
    }
  }

  private Collection<String> getChildResourcesWithMultipleParents() {
    Collection<String> result = new ArrayList<>();
    for (Entry<String, Collection<String>> entry : parentResourcesByChild.entrySet()) {
      if (entry.getValue().size() > 1) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  private void dagToTree(String resource) {
    Collection<String> parents = parentResourcesByChild.remove(resource);
    for (String parent : parents) {
      Collection<String> children = childResourcesByParent.get(parent);
      if (children != null) {
        children.remove(resource);
      }
      if (!ancestorsOf(parent).contains(resource)) {
        copyResource(parent, resource, getChildResourceName(resource, parent));
      }
    }
    removeResource(resource);
  }

  private Collection<String> ancestorsOf(String resource) {
    Collection<String> result = new HashSet<>();
    addAncestorsOf(resource, result);
    return result;
  }

  private void addAncestorsOf(String resource, Collection<String> ancestors) {
    if (parentResourcesByChild.containsKey(resource)) {
      for (String parent : parentResourcesByChild.get(resource)) {
        if (ancestors.add(parent)) {
          addAncestorsOf(parent, ancestors);
        }
      }
    }
  }

  private String getChildResourceName(String original, String parent) {
    if (original.startsWith(parent)) {
      return original + NAME_SEPARATOR + counter.incrementAndGet();
    }
    String prefix = getCommonPrefix(original, parent);
    String parentSuffix = parent.substring(prefix.length());
    String originalSuffix = original.substring(prefix.length());
    StringBuilder result = new StringBuilder().append(prefix).append(parentSuffix);
    if (!parentSuffix.endsWith(NAME_SEPARATOR) && !originalSuffix.isEmpty()
        && !originalSuffix.startsWith(NAME_SEPARATOR)) {
      result.append(NAME_SEPARATOR);
    }
    return result.append(originalSuffix).toString();
  }

  private String getCommonPrefix(String first, String second) {
    String[] parts1 = first.split("\\" + NAME_SEPARATOR);
    String[] parts2 = second.split("\\" + NAME_SEPARATOR);
    for (int i = 0; i < parts1.length && i < parts2.length; i++) {
      if (!parts1[i].equals(parts2[i])) {
        return join(parts1, i);
      }
    }
    return parts1.length < parts2.length ? join(parts1, parts1.length) : join(parts2, parts2.length);
  }

  private String join(String[] parts, int len) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < len; i++) {
      result.append(parts[i]).append(NAME_SEPARATOR);
    }
    return result.toString();
  }

  private void copyResource(String parent, String oldChild, String newChild) {
    if (parent.equals(newChild) || oldChild.equals(newChild)) {
      throw new IllegalStateException("Not very new child " + newChild + " compared to " + parent + " & " + oldChild);
    }
    addResource(newChild, null);
    addParentResource(newChild, parent);
    for (String grandChild : getChildren(oldChild)) {
      addParentResource(grandChild, newChild);
    }
    if (methodsByResource.containsKey(oldChild)) {
      methodsByResource.put(newChild, new TreeSet<>(methodsByResource.get(oldChild)));
    }
    if (locationsByResource.containsKey(oldChild)) {
      addLocations(newChild, locationsByResource.get(oldChild));
    }
    if (locationVarsByResource.containsKey(oldChild)) {
      for (UriTemplateVar var : locationVarsByResource.get(oldChild)) {
        addLocationVar(newChild, var.getName(), var.getDocumentation());
      }
    }
  }

  private Iterable<String> getChildren(String parent) {
    Collection<String> result = childResourcesByParent.get(parent);
    return result == null ? Collections.<String>emptyList() : result;
  }

  private void removeResource(String name) {
    resources.remove(name);
    removeParentChild(name);
    methodsByResource.remove(name);
    locationsByResource.remove(name);
    locationVarsByResource.remove(name);
    logResource(name, "is removed");
  }

  private void removeParentChild(String resource) {
    removeParentChild(resource, parentResourcesByChild, childResourcesByParent);
    removeParentChild(resource, childResourcesByParent, parentResourcesByChild);
  }

  private void removeParentChild(String resource, Map<String, Collection<String>> forward,
      Map<String, Collection<String>> backward) {
    Collection<String> items = forward.remove(resource);
    if (items == null || items.isEmpty()) {
      return;
    }
    for (String item : items) {
      Collection<String> backItems = backward.get(item);
      if (backItems != null) {
        backItems.remove(resource);
      }
    }
  }

  private void duplicateResourcesWithMultipleLocations() {
    for (String resource : getResourcesWithMultipleLocations()) {
      for (String location : locationsByResource.get(resource)) {
        copyResource(resource, location);
      }
      removeResource(resource);
    }
  }

  private Collection<String> getResourcesWithMultipleLocations() {
    Collection<String> result = new ArrayList<>();
    for (Entry<String, Collection<String>> entry : locationsByResource.entrySet()) {
      Collection<String> locations = entry.getValue();
      if (locations != null && locations.size() > 1) {
        result.add(entry.getKey());
      }
    }
    return result;
  }

  private void copyResource(String baseName, String location) {
    String name = baseName + NAME_SEPARATOR + lastSegmentOf(location);
    addResource(name, null);
    if (methodsByResource.containsKey(baseName)) {
      methodsByResource.put(name, new TreeSet<>(methodsByResource.get(baseName)));
    }
    addLocations(name, Arrays.asList(location));
  }

  private String lastSegmentOf(String location) {
    return location.substring(location.lastIndexOf(PATH_SEPARATOR) + 1);
  }

  private void resolveLocations() {
    Map<String, Collection<String>> resolvedLocations = new HashMap<>();
    resolveParameterizedLocations(resolvedLocations);
    resolveChildLocations(resolvedLocations);
    locationsByResource.putAll(resolvedLocations);
  }

  private void resolveChildLocations(Map<String, Collection<String>> resolvedLocations) {
    for (String childResource : parentResourcesByChild.keySet()) {
      resolveChildLocation(childResource, resolvedLocations);
    }
  }

  private void resolveChildLocation(String childResource, Map<String, Collection<String>> resolvedLocations) {
    String location = resolveLocation(childResource);
    if (location.isEmpty()) {
      locationsByResource.remove(childResource);
    } else {
      resolvedLocations.put(childResource, Arrays.asList(location));
    }
    logResource(childResource, "has new location " + location);
  }

  private String resolveLocation(String resource) {
    StringBuilder result = new StringBuilder();
    for (String name : ancestorOrSelfNames(resource)) {
      String uri = firstItem(locationsByResource.get(name));
      if (uri == null) {
        continue;
      }
      if (result.length() > 0 && uri.startsWith(PATH_SEPARATOR)) {
        uri = uri.substring(1);
      }
      result.append(uri);
      if (!uri.endsWith(PATH_SEPARATOR)) {
        result.append(PATH_SEPARATOR);
      }
    }
    return result.toString();
  }

  private Collection<String> ancestorOrSelfNames(String resource) {
    List<String> result = new ArrayList<>();
    String name = resource;
    while (name != null) {
      int index = result.indexOf(name);
      if (index >= 0) {
        List<String> children = new ArrayList<>(parentResourcesByChild.keySet());
        if (children.indexOf(name) < children.indexOf(getParent(name))) {
          while (result.size() > index + 1) {
            result.remove(index + 1);
          }
        } else {
          for (int i = 0; i < index; i++) {
            result.remove(0);
          }
        }
        break;
      }
      result.add(0, name);
      name = getParent(name);
    }
    return result;
  }

  private String getParent(String name) {
    if (parentResourcesByChild.containsKey(name)) {
      return firstItem(parentResourcesByChild.get(name));
    }
    return null;
  }

  private void resolveParameterizedLocations(Map<String, Collection<String>> resolvedLocations) {
    for (Entry<String, Collection<String>> entry : locationsByResource.entrySet()) {
      if (entry.getValue().size() > 1) {
        String resourceName = entry.getKey();
        Collection<String> newLocations = resolveParameterizedLocations(entry.getValue().iterator());
        resolvedLocations.put(resourceName, newLocations);
        logResource(resourceName, "has new locations " + newLocations);
      }
    }
  }

  private Collection<String> resolveParameterizedLocations(Iterator<String> locations) {
    Collection<String> result = new LinkedHashSet<>();
    result.add(locations.next());
    while (locations.hasNext()) {
      String current = locations.next();
      UriTemplateAction action = new AddAction(current);
      for (String existing : result) {
        switch (compareUriTemplates(current, existing)) {
          case DIFFERENT:
            break;
          case SPECIALIZATION:
            action = new ReplaceAction(existing, current);
            break;
          default:
            action = new IgnoreAction();
            break;
        }
        if (action.isFinal()) {
          break;
        }
      }
      action.execute(result);
    }
    return result;
  }

  private UriTemplateComparison compareUriTemplates(String uriTemplate1, String uriTemplate2) {
    String[] parts1 = uriTemplate1.split("/");
    String[] parts2 = uriTemplate2.split("/");
    if (parts1.length != parts2.length) {
      return UriTemplateComparison.DIFFERENT;
    }
    UriTemplateComparison result = UriTemplateComparison.EQUAL;
    for (int i = 0; i < parts1.length; i++) {
      boolean isTemplateVar1 = isTemplateVariable(parts1[i]);
      boolean isTemplateVar2 = isTemplateVariable(parts2[i]);
      if (isTemplateVar1) {
        if (!isTemplateVar2) {
          result = UriTemplateComparison.GENERALIZATION;
        }
      } else if (isTemplateVar2) {
        result = UriTemplateComparison.SPECIALIZATION;
      } else if (!parts1[i].equals(parts2[i])) {
        result = UriTemplateComparison.DIFFERENT;
        break;
      }
    }
    return result;
  }

  private boolean isTemplateVariable(String uriPart) {
    return uriPart.startsWith("{") && uriPart.endsWith("}");
  }

  private void mergeChildResourcesAtTheSameLocation() {
    for (Entry<String, Collection<String>> entry : getDuplicateChildResourcesByLocation().entrySet()) {
      Collection<String> duplicateChildResources = entry.getValue();
      String childResource = locationToChildResource(entry.getKey(), duplicateChildResources);
      mergeResources(childResource, duplicateChildResources);
    }
  }

  private void mergeResources(String newName, Collection<String> oldNames) {
    replace(resources, newName, oldNames);
    replaceKey(parentResourcesByChild, newName, oldNames);
    replaceKey(childResourcesByParent, newName, oldNames);
    replaceKey(locationsByResource, newName, oldNames);
    replaceKey(locationVarsByResource, newName, oldNames);
    replaceKey(methodsByResource, newName, oldNames);
  }

  private Map<String, Collection<String>> getDuplicateChildResourcesByLocation() {
    Map<String, Collection<String>> result = new HashMap<>();
    Map<String, String> resourcesByLocation = new HashMap<>();
    for (Entry<String, Collection<String>> entry : locationsByResource.entrySet()) {
      String location = firstItem(entry.getValue());
      String resource = entry.getKey();
      if (resourcesByLocation.containsKey(location) && parentResourcesByChild.containsKey(resource)) {
        Collection<String> duplicateChildResources = result.get(location);
        if (duplicateChildResources == null) {
          duplicateChildResources = new HashSet<>();
          result.put(location, duplicateChildResources);
        }
        duplicateChildResources.add(resource);
        duplicateChildResources.add(resourcesByLocation.get(location));
      } else {
        resourcesByLocation.put(location, resource);
      }
    }
    return result;
  }

  private void replace(Collection<String> items, String newItem, Collection<String> oldItems) {
    for (String oldItem : oldItems) {
      items.remove(oldItem);
    }
    items.add(newItem);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private <T> void replaceKey(Map<String, Collection<T>> map, String newKey, Collection<String> oldKeys) {
    Map<String, Collection<T>> values = new HashMap<>();
    for (String oldKey : oldKeys) {
      Collection<T> value = map.remove(oldKey);
      if (value == null) {
        continue;
      }
      Collection<T> previous = values.put(newKey, value);
      if (previous != null && !value.equals(previous)) {
        if (previous instanceof Collection && value instanceof Collection) {
          ((Collection)value).addAll(previous);
        } else {
          // This is not supposed to happen, but it does on one project... TBC
          map.put(oldKey, value);
        }
      }
    }
    map.putAll(values);
  }

  private String locationToChildResource(String location, Collection<String> duplicateNames) {
    String result = location;
    if (location.endsWith(PATH_SEPARATOR)) {
      result = result.substring(0, result.length() - 1);
    }
    int index = result.lastIndexOf(PATH_SEPARATOR);
    if (index >= 0) {
      result = result.substring(index + 1);
    }
    if (result.startsWith("{") && result.endsWith("}")) {
      result = result.substring(1, result.length() - 1);
    }
    String prefix = null;
    for (String duplicateName : duplicateNames) {
      String newPrefix = getPrefix(duplicateName);
      if (prefix == null) {
        prefix = newPrefix;
      } else if (!prefix.equals(newPrefix)) {
        prefix = "";
      }
    }
    return prefix + result;
  }

  private String getPrefix(String compoundName) {
    int index = compoundName.lastIndexOf(NAME_SEPARATOR);
    return index < 0 ? "" : compoundName.substring(0, index + 1);
  }

  private void mergeResourcesWithSameLocationAndDifferentMethods() {
    for (Entry<String, MergedMethods> entry : getResourcesWithSameLocationAndDifferentMethods().entrySet()) {
      Collection<Method> methods = methodsByResource.get(entry.getKey());
      if (methods == null) {
        methods = new ArrayList<>();
      }
      MergedMethods mergedMethods = entry.getValue();
      methods.addAll(mergedMethods.getMethods());
      for (String other : mergedMethods.getResources()) {
        removeResource(other);
      }
    }
  }

  private Map<String, MergedMethods> getResourcesWithSameLocationAndDifferentMethods() {
    Map<String, MergedMethods> result = new HashMap<>();
    Collection<String> resourcesToProcess = new ArrayList<>(resources);
    for (String resource : resources) {
      addResourcesWithSameLocationAndDifferentMethods(resource, resourcesToProcess, result);
    }
    return result;
  }

  private void addResourcesWithSameLocationAndDifferentMethods(String resource, Collection<String> resourcesToProcess,
      Map<String, MergedMethods> result) {
    if (isProcessed(resource, resourcesToProcess)) {
      return;
    }
    String uri = getUri(resource);
    if (uri == null) {
      return;
    }
    Collection<Method> methods = getMethods(resource);
    Collection<String> resourcesWithSameLocation = getResourcesWithSameLocationAndDifferentMethods(resourcesToProcess,
        resource, uri, methods);
    if (resourcesWithSameLocation.size() > 1) {
      resourcesToProcess.removeAll(resourcesWithSameLocation);
      String survivor = selectSurvivingResource(resourcesWithSameLocation);
      resourcesWithSameLocation.remove(survivor);
      result.put(survivor, new MergedMethods(resourcesWithSameLocation, methods));
    }
  }

  private Collection<String> getResourcesWithSameLocationAndDifferentMethods(Collection<String> resourcesToProcess,
      String resource, String uri, Collection<Method> methods) {
    Collection<String> result = new TreeSet<>();
    result.add(resource);
    for (String otherResource : resourcesToProcess) {
      if (isResourceWithSameLocationAndDifferentMethods(resource, uri, methods, otherResource)) {
        result.add(otherResource);
      }
    }
    return result;
  }

  private boolean isResourceWithSameLocationAndDifferentMethods(String resource, String uri,
      Collection<Method> methods, String otherResource) {
    if (resource.equals(otherResource)) {
      return false;
    }
    String otherUri = getUri(otherResource);
    if (otherUri == null || !uri.equals(otherUri)) {
      return false;
    }
    if (sharesMethods(methods, otherResource)) {
      return false;
    }
    return true;
  }

  private Collection<Method> getMethods(String resource) {
    Collection<Method> methods = new ArrayList<>();
    Collection<Method> originalMethods = methodsByResource.get(resource);
    if (originalMethods != null) {
      methods.addAll(originalMethods);
    }
    return methods;
  }

  private boolean isProcessed(String resource, Collection<String> resourcesToProcess) {
    return !resourcesToProcess.remove(resource);
  }

  @Override
  public String getUri(String resourceName) {
    return firstItem(locationsByResource.get(resourceName));
  }

  private String firstItem(Collection<String> items) {
    if (items == null) {
      return null;
    }
    Iterator<String> iterator = items.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }

  private String selectSurvivingResource(Collection<String> resourceNames) {
    for (String resource : resourceNames) {
      if (existSingularOrPlural(resource)) {
        return resource;
      }
    }
    return firstItem(resourceNames);
  }

  private boolean existSingularOrPlural(String resource) {
    String name = getFriendlyName(resource);
    if (name.endsWith("s")) {
      if (existsResourceName(name.substring(0, name.length() - 1))) {
        return true;
      }
    } else if (existsResourceName(name + 's')) {
      return true;
    }
    return false;
  }

  private boolean existsResourceName(String name) {
    for (String resource : resources) {
      if (name.equals(getFriendlyName(resource))) {
        return true;
      }
    }
    return false;
  }

  private boolean sharesMethods(Collection<Method> methods, String resource) {
    Collection<Method> otherMethods = methodsByResource.get(resource);
    if (otherMethods == null) {
      return false;
    }
    for (Method method : otherMethods) {
      if (methods.contains(method)) {
        return true;
      }
    }
    methods.addAll(otherMethods);
    return false;
  }

  @Override
  public String getFriendlyName(String resourceName) {
    return initCap(override(joinQualifiedName(removeDuplicates(splitQualifiedName(resourceName)))));
  }

  private List<String> splitQualifiedName(String className) {
    List<String> result = new ArrayList<>();
    for (String rawPart : className.split("\\" + NAME_SEPARATOR)) {
      if (!ignorePackageParts.contains(rawPart)) {
        result.add(removeSuffixes(rawPart));
      }
    }
    return result;
  }

  private String removeSuffixes(String className) {
    String result = className;
    for (String suffix : REMOVABLE_CLASS_SUFFIXES) {
      if (result.endsWith(suffix)) {
        result = result.substring(0, result.length() - suffix.length());
      }
    }
    return result;
  }

  private Iterable<String> removeDuplicates(List<String> parts) {
    int i = 0;
    while (i < parts.size() - 1) {
      if (parts.get(i).equalsIgnoreCase(parts.get(i + 1))) {
        parts.remove(i);
      } else {
        i++;
      }
    }
    return parts;
  }

  private String joinQualifiedName(Iterable<String> items) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String item : items) {
      result.append(prefix).append(item);
      prefix = NAME_SEPARATOR;
    }
    return result.toString();
  }

  private String override(String name) {
    return overrideNames.containsKey(name) ? overrideNames.get(name) : name;
  }

  private String initCap(String value) {
    StringBuilder result = new StringBuilder(value);
    result.setCharAt(0, Character.toUpperCase(result.charAt(0)));
    return result.toString();
  }

  @Override
  public Collection<Method> methodsOf(String resource) {
    return new TreeSet<>(methodsByResource.get(resource));
  }

  @Override
  public String getDocumentation(String resourceName) {
    return documentationByResource.get(resourceName);
  }

  private void simplifyResourceNames() {
    if (!shouldSimplifyResourceNames) {
      return;
    }
    Collection<String> complexNames = new TreeSet<>();
    Map<String, String> friendlyNames = new HashMap<>();
    for (String resource : resources) {
      if (resource.contains(NAME_SEPARATOR)) {
        complexNames.add(resource);
      }
      friendlyNames.put(resource, getFriendlyName(resource).toLowerCase(Locale.getDefault()));
    }
    for (String complexName : complexNames) {
      int index = complexName.lastIndexOf(NAME_SEPARATOR);
      while (index > 0) {
        String simpleName = complexName.substring(index + 1);
        String friendlyName = getFriendlyName(simpleName).toLowerCase(Locale.getDefault());
        if (!friendlyNames.values().contains(friendlyName)) {
          renameResource(complexName, simpleName);
          friendlyNames.remove(complexName);
          friendlyNames.put(simpleName, friendlyName);
          break;
        }
        index = complexName.lastIndexOf(NAME_SEPARATOR, index - 1);
      }
    }
  }

  private void renameResource(String oldName, String newName) {
    resources.add(newName);
    renameKey(oldName, newName, methodsByResource);
    renameKey(oldName, newName, locationsByResource);
    renameKey(oldName, newName, locationVarsByResource);
    resources.remove(oldName);
  }

  private <T> void renameKey(String oldKey, String newKey, Map<String, Collection<T>> valuesByKey) {
    Collection<T> values = valuesByKey.remove(oldKey);
    if (values != null) {
      valuesByKey.put(newKey, values);
    }
  }

}
