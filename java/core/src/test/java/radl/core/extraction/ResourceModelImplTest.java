/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import radl.test.RandomData;
import radl.test.TestUtil;


public class ResourceModelImplTest {

  private static final RandomData RANDOM = new RandomData();

  private final ResourceModel model = new ResourceModelImpl();

  @Before
  public void init() {
    setShouldSimplifyResourceNames(false);
  }

  private void setShouldSimplifyResourceNames(boolean shouldSimplify) {
    Properties configuration = new Properties();
    configuration.put("resources.simplify", Boolean.toString(shouldSimplify));
    model.configure(configuration);
  }

  @Test
  public void prefixesChildLocationWithParentLocation() {
    String parent = 'p' + aName();
    String child = 'c' + aName();
    String grandChild = 'g' + aName();
    String parentUri = 'P' + aUri();
    String childUri = 'C' + aUri();
    String grandChildUri = 'G' + aUri();
    model.addResource(parent, null);
    model.addResource(child, null);
    model.addResource(grandChild, null);
    model.addLocations(parent, Arrays.asList(parentUri));
    model.addLocations(child, Arrays.asList(childUri));
    model.addLocations(grandChild, Arrays.asList(grandChildUri));
    model.addParentResource(grandChild, child);
    model.addParentResource(child, parent);

    model.build();

    assertEquals("Parent location", parentUri, model.getUri(parent));
    assertEquals("Child location", parentUri + childUri, model.getUri(child));
    assertEquals("Grand child location", parentUri + childUri + grandChildUri, model.getUri(grandChild));
  }

  private String aName() {
    return RANDOM.string(8);
  }

  private String aUri() {
    return aName() + '/';
  }

  @Test
  public void favorsUriOverUriTemplateForLocation() throws Exception {
    String resourceName = aName();
    String uri = aUri();
    String uriTemplate = '{' + aName() + '}';
    model.addResource(resourceName, null);
    model.addLocations(resourceName, Arrays.asList(uriTemplate));
    model.addLocations(resourceName, Arrays.asList(uri));

    model.build();

    assertEquals("Location", uri, model.getUri(resourceName));
  }

  @Test
  public void overridesLocation() {
    String resourceName = aName();
    String uri1 = aUri();
    String uri2 = aUri();
    model.addResource(resourceName, null);
    model.addLocations(resourceName, Arrays.asList(uri1));
    model.setLocations(resourceName, Arrays.asList(uri2));

    model.build();

    assertEquals("Location", uri2, model.getUri(resourceName));
  }

  @Test
  public void skipsUriValidationPattern() {
    String resourceName = aName();
    String uriStart = '{' + aName();
    String uriEnd = "}/";
    String uriValidationPattern = ":[0-9]+";
    model.addResource(resourceName, null);
    model.addLocations(resourceName, Arrays.asList(uriStart + uriValidationPattern + uriEnd));

    model.build();

    assertEquals("Location", uriStart + uriEnd, model.getUri(resourceName));
  }

  @Test
  public void skipsIgnorableResources() throws Exception {
    String part = aName();
    String resource = aName() + '.' + part + '.' + aName();
    Properties configuration = new Properties();
    configuration.setProperty("resources.ignore.parts", part);
    model.configure(configuration);

    model.addResource(resource, null);
    model.addMethod(resource, "GET", null, null, null);

    assertTrue("Resource not ignored", model.resourcesWithMethods().isEmpty());
  }

  @Test
  public void storesDocumentation() {
    String resourceName = aName();
    String resourceDoc = aName();
    String methodName = aName();
    String methodDoc = aName();

    model.addResource(resourceName, resourceDoc);
    model.addMethod(resourceName, methodName, null, null, methodDoc);

    assertEquals("Resource documentation", resourceDoc, model.getDocumentation(resourceName));
    assertEquals("Method documentation", methodDoc, model.methodsOf(resourceName).iterator().next().getDocumentation());
  }

  @Test
  public void simplifiesResourceNames() {
    String simpleName1 = "a_simple_" + aName();
    String complexName1 = aName() + '.' + simpleName1;
    String simpleName2 = "z_simple_" + aName();
    String complexName2 = aName() + '.' + simpleName2;
    String methodName = aName();
    model.addResource(complexName1, null);
    model.addMethod(complexName1, methodName, null, null, null);
    model.addResource(complexName2, null);
    model.addMethod(complexName2, methodName, null, null, null);

    setShouldSimplifyResourceNames(true);
    model.build();

    TestUtil.assertCollectionEquals("Resources", Arrays.asList(simpleName1, simpleName2), model.resourcesWithMethods());
  }

  @Test
  public void doesntOverrideSimplifiedName() throws Exception {
    String simpleName = "a_simple";
    String complexName1 = "a_complex." + simpleName;
    String complexName2 = "z_complex." + simpleName;
    String methodName = aName();
    model.addResource(complexName1, null);
    model.addMethod(complexName1, methodName, null, null, null);
    model.addResource(complexName2, null);
    model.addMethod(complexName2, methodName, null, null, null);

    setShouldSimplifyResourceNames(true);
    model.build();

    TestUtil.assertCollectionEquals("Resources", Arrays.asList(simpleName, complexName2), model.resourcesWithMethods());
  }

  // IIGREST-8
  @Test
  public void mergesMethodsWithSameNameButDifferentMediaTypes() {
    String resourceName = aName();
    String methodName = aName();
    String mediaType1 = 'a' + aMediaType();
    String mediaType2 = 'z' + aMediaType();
    model.addResource(resourceName, null);
    model.addMethod(resourceName, methodName, mediaType1, mediaType1, null);
    model.addMethod(resourceName, methodName, mediaType2, mediaType2, null);

    model.build();

    Method method = model.methodsOf(resourceName).iterator().next();
    TestUtil.assertCollectionEquals("Media types", Arrays.asList(mediaType1, mediaType2), method.getConsumes());
  }

  private String aMediaType() {
    return aName() + '/' + aName();
  }

  // IIGREST-10
  @Test
  public void ignoresCatchAllMediaType() {
    String resourceName = aName();
    String methodName = aName();
    String mediaType = "*/*";
    model.addResource(resourceName, null);
    model.addMethod(resourceName, methodName, mediaType, mediaType, null);

    model.build();

    TestUtil.assertCollectionEquals("Media types", Collections.<String>emptyList(), model.mediaTypes());
  }

  // Issue 22
  @Test
  public void maintainsUriTemplateVars() throws Exception {
    String resourceName = aName();
    String paramName = aName();
    String paramDoc = aName();
    String location = String.format("/%s/{%s}/", aName(), paramName);
    model.addResource(resourceName, null);
    model.addLocations(resourceName, Arrays.asList(location));

    model.addLocationVar(resourceName, paramName, paramDoc);

    assertEquals("Names", Arrays.asList(paramName), model.getLocationVars(resourceName));
    assertEquals("Documentation", paramDoc, model.getLocationVarDocumentation(resourceName, paramName));
  }

}
