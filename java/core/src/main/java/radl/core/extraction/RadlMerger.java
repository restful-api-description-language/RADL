/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.core.extraction;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;

import radl.common.xml.DocumentBuilder;
import radl.common.xml.Xml;
import radl.core.Log;
import radl.core.Radl;


/**
 * Merge a resource model into an existing RADL document.
 */
public class RadlMerger implements ResourceModelMerger {

  static final String HTML_NAMESPACE_URI = "http://www.w3.org/1999/xhtml";

  private String service;
  private final Map<String, String> specificationByMediaType = new HashMap<String, String>();

  public RadlMerger() {
    registerStandardMediaTypes();
    registerImageMediaTypes();
    registerMsOfficeMediaTypes();
    registerOtherMediaTypes();
  }

  private void registerStandardMediaTypes() {
    specificationByMediaType.put("application/atom+xml", "http://tools.ietf.org/html/rfc4287");
    specificationByMediaType.put("application/json", "https://tools.ietf.org/html/rfc4627");
    specificationByMediaType.put("application/pdf", "http://tools.ietf.org/html/rfc3778");
    specificationByMediaType.put("application/rtf",
        "http://msdn.microsoft.com/en-us/library/aa140277%28v=office.10%29.aspx");
    specificationByMediaType.put("application/xml", "http://tools.ietf.org/html/rfc7303");
    specificationByMediaType.put("application/x-www-form-urlencoded",
        "http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.1");
    specificationByMediaType.put("application/zip", "https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT");
    specificationByMediaType.put("text/html", "http://www.w3.org/TR/html401/");
    specificationByMediaType.put("text/plain", "http://tools.ietf.org/html/rfc2046");
    specificationByMediaType.put("text/xml", "http://tools.ietf.org/html/rfc7303");
  }

  private void registerImageMediaTypes() {
    specificationByMediaType.put("image/bpm", "http://www.fileformat.info/format/bmp/egff.htm");
    specificationByMediaType.put("image/gif", "http://www.w3.org/Graphics/GIF/spec-gif89a.txt");
    specificationByMediaType.put("image/jpeg", "http://www.w3.org/Graphics/JPEG/itu-t81.pdf");
    specificationByMediaType.put("image/png", "https://tools.ietf.org/html/rfc2083");
    specificationByMediaType.put("image/svg+xml", "http://www.w3.org/TR/SVG11/");
    specificationByMediaType.put("image/tiff", "http://tools.ietf.org/html/rfc3302");
  }

  private void registerMsOfficeMediaTypes() {
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-word.document.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.wordprocessingml.template",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-word.template.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-excel.sheet.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.spreadsheetml.template",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-excel.template.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-excel.sheet.binary.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-excel.addin.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-powerpoint.presentation.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-powerpoint.slideshow.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.presentationml.template",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-powerpoint.template.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-powerpoint.addin.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.openxmlformats-officedocument.presentationml.slide",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-powerpoint.slide.macroEnabled.12",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/msonenote",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
    specificationByMediaType.put("application/vnd.ms-officetheme",
        "http://technet.microsoft.com/en-us/library/ee309278%28office.12%29.aspx");
  }

  private void registerOtherMediaTypes() {
    specificationByMediaType.put("application/epub+zip", "http://www.idpf.org/epub/30/spec/epub30-ocf.html");
    specificationByMediaType.put("application/json-home", "http://tools.ietf.org/html/draft-nottingham-json-home-03");
    specificationByMediaType.put("application/home+xml", "http://tools.ietf.org/html/draft-wilde-home-xml-03");
    specificationByMediaType.put("application/xacml+xml; version=2.0",
        "http://www.iana.org/assignments/media-types/application/xacml+xml");
    specificationByMediaType.put("application/xacml+xml; version=3.0",
        "http://www.iana.org/assignments/media-types/application/xacml+xml");
    specificationByMediaType.put("application/vnd.xacml+json",
        "https://www.oasis-open.org/apps/org/workgroup/xacml/download.php/52828/xacml-json-http-v1.0-wd18.doc");
  }

  @Override
  public void setService(String serviceName) {
    this.service = serviceName;
  }

  @Override
  public Document toRadl(ResourceModel resourceModel) {
    resourceModel.build();
    DocumentBuilder radl = startService();
    addMediaTypes(radl, resourceModel);
    addResources(radl, resourceModel);
    return radl.build();
  }

  private DocumentBuilder startService() {
    return DocumentBuilder.newDocument()
        .namespace(Radl.NAMESPACE_URI)
        .element("service")
            .attribute("name", service);
  }

  private void addMediaTypes(DocumentBuilder radl, ResourceModel resourceModel) {
    Iterable<String> mediaTypes = resourceModel.mediaTypes();
    if (mediaTypes.iterator().hasNext()) {
      radl.element("media-types");
      for (String mediaType : mediaTypes) {
        radl.element("media-type")
            .attribute("name", mediaType);
        String uri = specificationByMediaType.get(mediaType);
        if (uri != null) {
          radl.element("specification")
              .attribute("href", uri)
          .end();
        }
        radl.end();
      }
      radl.end();
    }
  }

  private void addResources(DocumentBuilder radl, ResourceModel resourceModel) {
    Iterable<String> resourcesWithMethods = resourceModel.resourcesWithMethods();
    if (resourcesWithMethods.iterator().hasNext()) {
      radl.element("resources");
      for (String resource : resourcesWithMethods) {
        addResource(radl, resource, resourceModel);
      }
      radl.end();
    }
  }

  private void addResource(DocumentBuilder radl, String resource, ResourceModel resourceModel) {
    radl.element("resource")
        .attribute("name", resourceModel.getFriendlyName(resource));
    addDocumentation(radl, resourceModel.getDocumentation(resource));
    addLocation(radl, resource, resourceModel);
    addMethods(radl, resource, resourceModel);
    radl.end();
  }

  private void addDocumentation(DocumentBuilder radl, String documentation) {
    if (documentation != null) {
      radl.importXml(String.format("<radl:documentation xmlns:radl=\"%s\" xmlns=\"%s\">%s</radl:documentation>",
          Radl.NAMESPACE_URI, HTML_NAMESPACE_URI, documentation));
      if (Radl.NAMESPACE_URI.equals(radl.getCurrent().getNamespaceURI())) {
        radl.getCurrent().setPrefix("");
      } else {
        Log.error("Documentation in wrong namespace: " + Xml.toString(radl.getCurrent()));
      }
      radl.end();
    }
  }

  private void addLocation(DocumentBuilder radl, String resource, ResourceModel resourceModel) {
    String uri = resourceModel.getUri(resource);
    if (uri != null) {
      String uriAttribute = uri.contains("{") ? "uri-template" : "uri";
      radl.element("location").attribute(uriAttribute, uri);
      for (String var : resourceModel.getLocationVars(resource)) {
        radl.element("var").attribute("name", var);
        addDocumentation(radl, resourceModel.getLocationVarDocumentation(resource, var));
        radl.end();
      }
      radl.end();
    }
  }

  private void addMethods(DocumentBuilder radl, String resource, ResourceModel resourceModel) {
    Iterable<Method> methods = resourceModel.methodsOf(resource);
    if (methods.iterator().hasNext()) {
      radl.element("methods");
      for (Method method : methods) {
        radl.element("method")
            .attribute("name", method.getName());
        addDocumentation(radl, method.getDocumentation());
        if (method.hasConsumes()) {
          addRepresentations(radl, "request", method.getConsumes());
        }
        if (method.hasProduces()) {
          addRepresentations(radl, "response", method.getProduces());
        }
        radl.end();
      }
      radl.end();
    }
  }

  private void addRepresentations(DocumentBuilder radl, String type, Iterable<String> mediaTypes) {
    radl.element(type)
        .element("representations");
    for (String mediaType : mediaTypes) {
      radl.element("representation")
          .attribute("media-type", mediaType)
      .end();
    }
    radl.end().end();
  }

}
