/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.util.Arrays;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.Radl;
import radl.core.code.radl.RadlCode;
import radl.test.RandomData;
import radl.test.TestUtil;


public class RadlMergerTest {

  private static final RandomData RANDOM = new RandomData();

  private final ResourceModelMerger merger = new RadlMerger();
  private final ResourceModel resourceModel = new ResourceModelImpl();
  private final String serviceName = aName();
  private final String resourceName = aName();
  private final DocumentBuilder radlBuilder = DocumentBuilder.newDocument()
      .namespace(Radl.NAMESPACE_URI)
      .element("service")
          .attribute("name", serviceName);

  private String aName() {
    return 'N' + RANDOM.string(5);
  }

  @Before
  public void init() {
    resourceModel.configure(defaultProperties());
  }

  private Properties defaultProperties() {
    Properties result = new Properties();
    result.put("resources.simplify", Boolean.toString(false));
    return result;
  }

  @Test
  public void buildsRadl() {
    String uri = aUri();
    String methodName = aName();
    String consumes = 'z' + aName();
    String produces = 'a' + aName();
    radlBuilder
        .element("media-types")
            .element("media-type")
                .attribute("name", produces)
            .end()
            .element("media-type")
                .attribute("name", consumes)
            .end()
        .end()
        .element("resources")
            .element("resource")
                .attribute("name", resourceName)
                .element("location")
                    .attribute("uri", uri)
                .end()
                .element("methods")
                    .element("method")
                        .attribute("name", methodName)
                        .element("request")
                            .element("representations")
                                .element("representation")
                                    .attribute("media-type", consumes)
                                .end()
                            .end()
                        .end()
                        .element("response")
                            .element("representations")
                                .element("representation")
                                    .attribute("media-type", produces);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addResource(resourceName, null);
    resourceModel.addMethod(resourceName, methodName, consumes, produces, null);
    resourceModel.addLocations(resourceName, Arrays.asList(uri));

    assertRadl();
  }

  private String aUri() {
    return '/' + aName() + '/' + aName() + '/';
  }

  private void assertRadl() {
    TestUtil.assertXmlEquals("RADL", radlBuilder.build(), merger.toRadl(resourceModel));
  }

  @Test
  public void buildsUriTemplate() throws Exception {
    String varName = aName();
    String varDoc = aName();
    String uri = aUri() + "{" + varName + "}/";
    String method = aMethod();
    radlBuilder.element("resources").element("resource")
        .attribute("name", resourceName)
        .element("location")
            .attribute("uri-template", uri)
            .element("var")
                .attribute("name", varName)
                .element("documentation", varDoc)
            .end()
        .end()
        .element("methods")
            .element("method")
                .attribute("name", method);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addMethod(resourceName, method, null, null, null);
    resourceModel.addLocations(resourceName, Arrays.asList(uri));
    resourceModel.addLocationVar(resourceName, varName, varDoc);

    assertRadl();
  }

  private String aMethod() {
    switch (RANDOM.integer(4)) {
      case 0: return "DELETE";
      case 1: return "POST";
      case 2: return "PUT";
      default: return "GET";
    }
  }

  @Test
  public void buildsAbsoluteLocationForRelativeResources() {
    String parentUri = aUri();
    String childUri = aName();
    String parentResource = 'A' + aName();
    String childResource = 'Z' + aName();
    String method = aMethod();
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", parentResource)
            .element("location")
                .attribute("uri", parentUri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", childResource)
            .element("location")
                .attribute("uri", parentUri + childUri + '/')
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method);

    merger.setService(serviceName);
    resourceModel.addResource(parentResource, null);
    resourceModel.addMethod(parentResource, method, null, null, null);
    resourceModel.addResource(childResource, null);
    resourceModel.addParentResource(childResource, parentResource);
    resourceModel.addMethod(childResource, method, null, null, null);
    resourceModel.addLocations(parentResource, Arrays.asList(parentUri));
    resourceModel.addLocations(childResource, Arrays.asList('/' + childUri));

    assertRadl();
  }

  @Test
  public void recognizesAbsoluteFromRelativeLocations() {
    String uri = aUri();
    String method = "GET";
    String childResource = "ZZZ" + aName();
    String childUri = uri + aName() + '/';
    String childMethod = "POST";
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("location")
                .attribute("uri", uri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", childResource)
            .element("location")
                .attribute("uri", childUri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", childMethod);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addLocations(resourceName, Arrays.asList(uri));
    resourceModel.addMethod(resourceName, method, null, null, null);
    resourceModel.addResource(childResource, null);
    resourceModel.addLocations(childResource, Arrays.asList(childUri));
    resourceModel.addMethod(childResource, childMethod, null, null, null);

    assertRadl();
  }

  @Test
  public void overridesGeneratedResourceName() {
    String parentResource = 'A' + aName();
    String childResource = 'Z' + aName();
    String overrideName = 'Y' + aName();
    String method = aMethod();
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", parentResource)
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", overrideName)
            .element("methods")
                .element("method")
                    .attribute("name", method);

    Properties properties = new Properties();
    properties.put("resources.override.names", childResource + ':' + overrideName);
    merger.setService(serviceName);
    resourceModel.configure(properties);
    resourceModel.addResource(parentResource, null);
    resourceModel.addMethod(parentResource, method, null, null, null);
    resourceModel.addResource(childResource, null);
    resourceModel.addMethod(childResource, method, null, null, null);
    resourceModel.addParentResource(childResource, parentResource);

    assertRadl();
  }

  @Test
  public void skipsResourcesWithoutMethods() {
    merger.setService(serviceName);
    resourceModel.addResource(aName(), null);

    assertRadl();
  }

  @Test
  public void mapsMultipleMethodsAnnotatedWithTheSamePathToOneResource() {
    String methodName1 = "m1" + aName();
    String methodName2 = "m2" + aName();
    String httpMethod1 = "GET";
    String httpMethod2 = "POST";
    String baseUri = aUri();
    String childUri = aName();
    String uri = '{' + childUri + "}/";
    String childResource1 = resourceName + '.' + methodName1;
    String childResource2 = resourceName + '.' + methodName2;
    String expectedChildResource = resourceName + '.' + childUri;
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", expectedChildResource)
            .element("location")
                .attribute("uri-template", baseUri + uri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", httpMethod1)
                .end()
                .element("method")
                    .attribute("name", httpMethod2);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addLocations(resourceName, Arrays.asList(baseUri));
    resourceModel.addResource(childResource1, null);
    resourceModel.addParentResource(childResource1, resourceName);
    resourceModel.addMethod(childResource1, httpMethod1, null, null, null);
    resourceModel.addLocations(childResource1, Arrays.asList(uri));
    resourceModel.addResource(childResource2, null);
    resourceModel.addParentResource(childResource2, resourceName);
    resourceModel.addMethod(childResource2, httpMethod2, null, null, null);
    resourceModel.addLocations(childResource2, Arrays.asList(uri));

    assertRadl();
  }

  @Test
  public void skipsResourcesInPackagesConfiguredToIgnore() {
    String part1 = aName();
    String part2 = aName();
    String part3 = aName();
    String name = aName();
    String resource = part1 + '.' + part2 + '.' + part3 + '.' + name;
    String method = "DELETE";
    Properties configuration = defaultProperties();
    configuration.setProperty("resources.ignore.package.parts", part1 + ',' + part3);
    radlBuilder.element("resources")
    .element("resource")
        .attribute("name", part2 + '.' + name)
        .element("methods")
            .element("method")
                .attribute("name", method);

    merger.setService(serviceName);
    resourceModel.configure(configuration);
    resourceModel.addResource(resource, null);
    resourceModel.addMethod(resource, method, null, null, null);

    assertRadl();
  }

  @Test
  public void breaksInfiniteRecursionBetweenResources() {
    String childResource = "ZZZ" + aName();
    String uri = aUri();
    String childUri = aName() + '/';
    String method = "GET";
    String childMethod = "POST";
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("location")
                .attribute("uri", uri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", childResource)
            .element("location")
                .attribute("uri", uri + childUri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", childMethod);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addLocations(resourceName, Arrays.asList(uri));
    resourceModel.addMethod(resourceName, method, null, null, null);
    resourceModel.addResource(childResource, null);
    resourceModel.addLocations(childResource, Arrays.asList(childUri));
    resourceModel.addMethod(childResource, childMethod, null, null, null);
    resourceModel.addParentResource(childResource, resourceName);
    resourceModel.addParentResource(resourceName, childResource);

    assertRadl();
  }

  @Test
  public void stripsPackagePartThatDuplicatesClassName() {
    String packageName = aName();
    String name = TestUtil.initCap(packageName);
    String resource = packageName + '.' + name;
    String method = "PUT";
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", name)
            .element("methods")
                .element("method")
                    .attribute("name", method);

    merger.setService(serviceName);
    resourceModel.addResource(resource, null);
    resourceModel.addMethod(resource, method, null, null, null);

    assertRadl();
  }

  @Test
  public void stripsCommonControllerSuffixes() {
    String method = "GET";
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("methods")
                .element("method")
                    .attribute("name", method);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName + "Controller", null);
    resourceModel.addMethod(resourceName + "Controller", method, null, null, null);

    assertRadl();
  }

  @Test
  public void buildsImplicitResourcesWhenMultipleParents() {
    String prefix = "Com.Company.Application";
    String grandParent1 = "A";
    String grandParent2 = "B";
    String parent = "C";
    String child1 = "D";
    String child2 = "E";
    String grandParent1q = qname(prefix, grandParent1);
    String grandParent2q = qname(prefix, grandParent2);
    String parentq = qname(prefix, parent);
    String child1q = qname(prefix, child1);
    String child2q = qname(prefix, child2);
    merger.setService(serviceName);
    addResourceWithMethod(grandParent1q);
    addResourceWithMethod(grandParent2q);
    addResourceWithMethod(parentq);
    addResourceWithMethod(child1q);
    addResourceWithMethod(child2q);
    resourceModel.addParentResource(parentq, grandParent1q);
    resourceModel.addParentResource(parentq, grandParent2q);
    resourceModel.addParentResource(child1q, parentq);
    resourceModel.addParentResource(child2q, parentq);
    resourceModel.addParentResource(parentq, child2q);

    RadlCode radl = new RadlCode();
    radl.add(Xml.toString(merger.toRadl(resourceModel)));
    TestUtil.assertCollectionEquals("resources", Arrays.asList(grandParent1q, qname(grandParent1q, parent),
        qname(grandParent1q, parent, child1), qname(grandParent1q, parent, child2), grandParent2q,
        qname(grandParent2q, parent), qname(grandParent2q, parent, child1), qname(grandParent2q, parent, child2)),
        radl.resourceNames());
  }

  private String qname(String... parts) {
    StringBuilder result = new StringBuilder();
    String prefix = "";
    for (String part : parts) {
      result.append(prefix).append(part);
      prefix = ".";
    }
    return result.toString();
  }

  private void addResourceWithMethod(String resource) {
    resourceModel.addResource(resource, null);
    resourceModel.addMethod(resource, "GET", null, null, null);
  }

  @Test
  public void splitsResourcesWithMultipleLocations() {
    String uri1 = 'z' + aName();
    String uri2 = 'a' + aName();
    String method = aMethod();
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName + '.' + uri2)
            .element("location")
                .attribute("uri", '/' + uri2)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", resourceName + '.' + uri1)
            .element("location")
                .attribute("uri", '/' + uri1)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addLocations(resourceName, Arrays.asList('/' + uri1, '/' + uri2));
    resourceModel.addMethod(resourceName, method, null, null, null);

    assertRadl();
  }

  @Test
  public void mergesResourcesWithSameLocationButDifferentMethods() {
    String resourceName2 = "zz" + aName();
    String uri = aUri();
    String method1 = "GET";
    String method2 = "PUT";
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("location")
                .attribute("uri", uri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method1)
                .end()
                .element("method")
                    .attribute("name", method2);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addLocations(resourceName, Arrays.asList(uri));
    resourceModel.addMethod(resourceName, method1, null, null, null);
    resourceModel.addResource(resourceName2, null);
    resourceModel.addLocations(resourceName2, Arrays.asList(uri));
    resourceModel.addMethod(resourceName2, method2, null, null, null);

    assertRadl();
  }

  @Test
  public void prefersCollectionNameWhenMergingResourcesWithSameLocation() {
    String resourceName1 = "aa" + aName();
    String resourceName2 = resourceName + 's';
    String uri = aUri();
    String method = "POST";
    String method1 = "GET";
    String method2 = "PUT";
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("methods")
                .element("method")
                    .attribute("name", method)
                .end()
            .end()
        .end()
        .element("resource")
            .attribute("name", resourceName2)
            .element("location")
                .attribute("uri", uri)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", method1)
                .end()
                .element("method")
                    .attribute("name", method2);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addMethod(resourceName, method, null, null, null);
    resourceModel.addResource(resourceName1, null);
    resourceModel.addLocations(resourceName1, Arrays.asList(uri));
    resourceModel.addMethod(resourceName1, method1, null, null, null);
    resourceModel.addResource(resourceName2, null);
    resourceModel.addLocations(resourceName2, Arrays.asList(uri));
    resourceModel.addMethod(resourceName2, method2, null, null, null);

    assertRadl();
  }

  @Test
  public void addsDesciptionForWellKnownMediaTypes() {
    String uri = aUri();
    String methodName = aName();
    String mediaType = "application/atom+xml";
    radlBuilder
        .element("media-types")
            .element("media-type")
                .attribute("name", mediaType)
                .element("specification")
                    .attribute("href", "http://tools.ietf.org/html/rfc4287")
                .end()
            .end()
        .end()
        .element("resources")
            .element("resource")
                .attribute("name", resourceName)
                .element("location")
                    .attribute("uri", uri)
                .end()
                .element("methods")
                    .element("method")
                        .attribute("name", methodName)
                        .element("response")
                            .element("representations")
                                .element("representation")
                                    .attribute("media-type", mediaType);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, null);
    resourceModel.addMethod(resourceName, methodName, null, mediaType, null);
    resourceModel.addLocations(resourceName, Arrays.asList(uri));

    assertRadl();
  }

  @Test
  public void buildsDocumentation() {
    String resourceDoc = aName();
    String methodName = aName();
    String methodDoc = aName();
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("documentation", resourceDoc)
            .element("methods")
                .element("method")
                    .attribute("name", methodName)
                    .element("documentation", methodDoc);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, resourceDoc);
    resourceModel.addMethod(resourceName, methodName, null, null, methodDoc);

    assertRadl();
  }

  @Test
  public void buildsHtmlDocumentation() {
    String doc1 = aName();
    String doc2 = aName();
    String resourceDoc = doc1 + "<br/>" + doc2;
    String methodName = aName();
    String methodDoc = aName();
    radlBuilder.element("resources")
        .element("resource")
            .attribute("name", resourceName)
            .element("documentation")
                .text(doc1)
                .namespace(RadlMerger.HTML_NAMESPACE_URI)
                .element("br")
                .end()
                .namespace(Radl.NAMESPACE_URI)
                .text(doc2)
            .end()
            .element("methods")
                .element("method")
                    .attribute("name", methodName)
                    .element("documentation", methodDoc);

    merger.setService(serviceName);
    resourceModel.addResource(resourceName, resourceDoc);
    resourceModel.addMethod(resourceName, methodName, null, null, methodDoc);

    assertRadl();
  }

}
