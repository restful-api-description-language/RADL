# RESTful API Description Language (RADL) #


## Status of this Document ##

This is a draft.


## Copyright Notice ##

Copyright (c) 2014-2015 EMC Corporation. All rights reserved.


## Note to Readers ##

This draft should be discussed on the RADL mailing list [1].
Online access to all versions and files is available on GitHub [2].

   
## Abstract ##

This document specifies RESTful API Description Language (RADL),
an XML vocabulary for describing hypermedia-driven RESTful APIs. 
   
   
# Introduction #

RESTful API Description Language (RADL) is an XML vocabulary for specifying and documenting RESTful APIs. 


## Motivation ##

The ever increasing number of APIs requires tooling to manage. Such tools need to understand the APIs they're managing,
which leads to a need to describe APIs in a machine-processable way.

There have been several attempts at API description languages. WSDL [3] is the language of choice for SOAP-based APIs.
For HTTP APIs, we've seen WADL [4], RAML [5], Swagger [6].

While these description languages work, and have tooling ecosystems of varying maturity around them, they don't have 
special provisions for the hypermedia aspect of truly RESTful APIs. In terms of the Richardson Maturity Model [8], they 
can only describe APIs up to level 2.

API Blueprint [7] supports link relations, and as such can be used to describe level 3 APIs. It is based on
Markdown syntax, which makes it relatively easy to read and write by a human. However, there is no schema language
that can be used to describe and validate Markdown. The API Blueprint specification is therefore a lot less readable
than it could have been and it places a greater burden on tooling to validate API descriptions than necessary.
An API description language's primary function is to enable tooling. Optimizing for human readers and writers should
come second. XML meets those requirements much better than Markdown.

Many developers struggle with REST concepts. It would therefore be advantageous if the description language could guide 
developers when making their choices. The structure of the description language should take the developer by the hand
and make it easy to do the right thing.
API Blueprint has no such facilities. It merely records the end product and provides no help along the way.

Another problem that API Blueprint shares with RAML and Swagger is that encourages certain anti-patterns. For instance,
each HTTP method must include a response which must include a status code. This encourages client to expect only the
documented status codes. It's much better for clients to have general status code handling capabilities, so that they
don't break whenever the server changes. The same holds true for HTTP headers.

RADL is an XML-based API description language that can describe fully hypermedia-driven APIs (level 3). Its structure
guides the author through the API design process and makes it easy to do the right thing.


## Namespace and Version ##

The XML Namespaces URI [W3C.REC-xml-names-19990114] for the XML data format described in this specification is:

    urn:radl:service

When this specification uses "RADL", it refers to an XML document in the above namespace.


## Notational Conventions ##

This specification uses the namespace prefix `radl:` for the Namespace URI identified above. Note that the choice
of namespace prefix is arbitrary and not semantically significant.

RADL is specified using terms from the XML Infoset [W3C.REC-xml-infoset-20040204].  However, this specification uses a
shorthand for two common terms: the phrase "Information Item" is omitted when naming Element Information Items and 
Attribute Information Items. Therefore, when this specification uses the term "element," it is referring to an Element 
Information Item in Infoset terms.  Likewise, when it uses the term "attribute," it is referring to an Attribute 
Information Item.

Some sections of this specification are illustrated with fragments of a non-normative RELAX NG Compact schema 
[RELAX-NG]. However, the text of this specification provides the definition of conformance. A complete schema appears 
in [RADL Schema].
      
The key words "MUST", "MUST NOT", "REQUIRED", "SHALL", "SHALL NOT", "SHOULD", "SHOULD NOT", "RECOMMENDED", "MAY", 
and "OPTIONAL" in this document are to be interpreted as described in [RFC2119].


# RADL Service Documents #

This specification describes RADL Service Documents. A RADL Service Document is a representation of a RESTful API.
Its root element is the `radl:service` element.

    default namespace radl = "urn:radl:service"
    start = service

RADL Service Documents are specified in terms of the XML Information Set, serialized as XML 1.0 [W3C.REC-xml-20040204].
RADL Service Documents MUST be well-formed XML.  This specification does not define a DTD for RADL Service Documents, 
and hence does not require them to be valid (in the sense used by XML).


# Common RADL Constructs #

## The `name` Attribute ##

Many elements in RADL support the `name` attribute that contains a human-readable identifier.
A name is a simple string, not an XML ID, so it doesn't have the limitations of XML IDs.

    name = attribute name { text }
    
A named element can be referred to from other elements.


## The `radl:documentation` Element ##    

Many elements in RADL support the `documentation` element that contains information that is intended to be read
by a human. Documentation is mostly meant for client developers that program against the REST API, but it can also
be used to give information to server developers.

    documentation = element documentation { 
      inline?, 
      doc-title?, 
      html
    }
    inline = attribute inline { "true" | "false" }
    doc-title = element title { 
      text
    }
    html = html-content*
    html-content = html-element | text | ref
    html-element = element html:* { 
      html-attribute*, 
      html-content*
    }
    html-attribute = attribute * { text? }

Documentation can be plain text, or it can be marked up in HTML. Here's an example:

    <service name="RESTBucks" xmlns="urn:radl:service"
        xmlns:html="http://www.w3.org/1999/xhtml">
      <documentation>
        This example service follows
        <html:a href="http://www.infoq.com/articles/webber-rest-workflow">RESTBucks</html:a>,
        an online version of coffee shop Starbucks based on Gregor Hohpe's  
        <html:a href="http://www.enterpriseintegrationpatterns.com/ramblings/18_starbucks.html">
        observation</html:a>
        that it is an asynchronous processing pipeline.
      </documentation>


## The `radl:ref` Element ##

From within a `radl:documentation` element you can refer to other RADL elements using the `radl:ref` element.

    ref = element ref {
      ((attribute idref { xsd:string }
        | attribute uri { xsd:anyURI }
        | attribute media-type { xsd:string }
        | attribute header { xsd:string }
        | attribute mechanism { xsd:string }
        | attribute identity-provider { xsd:string }
        | attribute scheme { xsd:string }
        | attribute scheme-parameter { xsd:string }
        | attribute status-code { xsd:string }
        | attribute uri-parameter { xsd:string }
        | attribute resources { xsd:string }
        | attribute resource { xsd:string }
        | attribute var { xsd:string }
        | attribute property { xsd:string }
        | attribute header { xsd:string }
        | attribute method { xsd:string }),
       text?)
      # Uses the name of the referred item if not provided
    }

The value of the referring attribute should equal the value of the `name` attribute of the element you're referring 
to. For instance, to refer to a status code, you could write:

    <status-code name="202"/>
    ...
    <documentation>
      See status <ref status-code="202"/>.
    </documentation>


# RADL Element Definitions #

## The `radl:services` Element ##

In RADL, the `radl:service` element represents the entire REST API. The structure of RADL guides the author through
the API design process. This starts with documenting the requirements and then adds more and more detail until the 
resource model is complete and the service is fully specified.

    service = element service {
      name,
      documentation*,
      states?,    
      link-relations?,
      data-models?,
      media-types?,
      service-conventions?,
      resources?,
      authentication?
    }


## The `radl:states` Element ##

A REST client starts at some well-known URI (the billboard URI). It then follows links that it discovers
through link relations from the responses until its goal is met. In other words, the client starts at some
initial state and then transitions to other states by following links. 
A natural way to capture this information is in the form of a *state diagram*.

In RADL, the state diagram is described as a list of client states and their transitions to other states: 

    states = element states {
      start-state?, 
      state*
    }
    start-state = element start-state {
      documentation*, 
      properties?, 
      state-transitions?
    }
    state-transitions = element transitions { 
      transition*
    }
    transition = element transition {
      name,
      to?,
      documentation*,
      transition-input?
    }
    to = attribute to { xsd:string }  
    transition-input = element input { 
      properties
    }
    state = element state {
      extends?, 
      name, 
      documentation*, 
      properties?, 
      state-transitions?
    }
    extends = attribute extends { xsd:string }
    
Here's an example:
    
    <states>
      <start-state>
        <documentation>
          The start state is reached by executing GET on the billboard URI.
        </documentation>
        <transitions>
          <transition name="Entry point" to="Menu"/>
        </transitions>
      </start-state>
      <state name="Menu">
        <documentation>
          The menu lists all the items that can be ordered.
        </documentation>
        <transitions>
          <transition name="Create order" to="Payment expected"/>
        </transitions>
      </state>
      <state name="Payment expected">
        <!-- ... Transitions ... -->
      </state>
      <!-- ... More states ... -->  
    </states>
    

## The `radl:link-relations` Element ##

Link relations identify the semantics of links by describing how the current context is related to
another resource [RFC 5988].
A client transitions from state to state by using link relations to find suitable links.
 
In RADL, link relations are associated with state transitions:

    link-relations = element link-relations { 
      documentation*, 
      link-relation*
    }
    link-relation = element link-relation {
      documentation*, 
      status?, 
      name, 
      href?, 
      linkrel-transitions? 
    }
    status = attribute status { 
      "future" | "assigned" | "poc" | "partial" | "complete" | "passed" 
    }
    href = attribute href { xsd:anyURI }
    linkrel-transitions = element transitions { 
      transition-ref-element*
    }
    transition-ref-element = element transition {
      transition-ref,
      from?,
      documentation*
    }
    transition-ref = attribute ref { xsd:string }
    from = attribute from { xsd:string }   
    
Here's an example:

    <link-relations>
      <link-relation name="http://relations.restbucks.com/order">
        <transitions>
          <transition ref="Create order"/>
        </transitions>
      </link-relation>
     <!-- ... More link relations ... -->
    </link-relations>
   

## The `radl:data-models` Element ##

In a RESTful system, client and server exchange self-describing messages. The content of such messages are
usually structured data that can be described using data models.

One way of capturing such data models in RADL is through the `data-models` element:

    data-models = element data-models { 
      data-model*
    }
    data-model = element data-model {
        name?,
        documentation*,
        schema?,
        examples?
    schema = element schema { 
      href?, 
      relax-ng?
    }
    relax-ng = element rng:* { 
      any-attribute*, 
      (relax-ng* | text)*
    }
    example = element example { 
      documentation*, 
      text
    }

An schema can be specified either externally or inside the RADL file. The latter must be a Relax-NG schema.

There are other ways of capturing the information in a `data-model`; see the section *Properties* below.


## The `radl:media-types` Element ##

Media types specify the concrete representation of a state as a series of bytes, typically in a 
JSON or XML format. Standard media types are formally defined in a specification that you can point to using the 
`radl:specification` element. A JSON Schema, RELAX-NG Schema, or XML Schema can be provided through the
`radl:schema` element to precisely specify the structure of the messages:

    media-types = element media-types {
      media-type*
    }
    media-type = element media-type {
      name,   
      documentation*,
      specification*,
      media-type-schema*,
      representation* 
    }
    specification = element specification { 
      href, 
      documentation*
    }
    media-type-schema = element schema {
      href, 
      type, 
      documentation*
    }
    type = attribute type { "rnc" | "rng" | "xsd" | "dtd" | "JSONSchema" }

Here's an example:

    <media-types> 
      <media-type name="application/vnd.restbucks+json">
        <documentation>
          An application-specific media type for the RESTBucks API.
        </documentation>
      </media-type>
      <media-type name="application/ld+json">
        <specification href="http://www.w3.org/TR/json-ld/"/>
      </media-type>
    </media-types>


## The `radl:service-conventions` Element ##

RADL discourages the anti-pattern of specifying the allowable headers, status codes, and URI query parameters 
for each response. REST clients should instead always be prepared to handle all the standard ones.

However, sometimes a service uses a header or status code in a very specific way. In that case, RADL lets
you describe such a service-specific convention in a single location that can be referred to later using the
`radl:service-conventions` element.

    service-conventions = element conventions { 
      documentation*, 
      headers?, 
      uri-parameters?, 
      status-codes?
    }
    uri-parameters = element uri-parameters { 
      documentation*, 
      uri-parameter*
    }
    uri-parameter = element uri-parameter {
      name, 
      documentation, 
      datatype, 
      value-range?, 
      default-value?
    }
    headers = element headers { 
      header*
    }
    header = element header { 
      name, 
      header-type, 
      documentation*
    }
    header-type = attribute type { "request" | "response" | "general" | "entity" }

Here's an example:

    <conventions>
      <uri-parameters>
        <uri-parameter name="startIndex">
          <documentation>
            The index of the first search result desired by the client. See the 
            <html:a href="http://www.opensearch.org/Specifications/OpenSearch/1.1/Draft_5">
              OpenSearch
            </html:a> specification.
          </documentation>
        </uri-parameter>
      </uri-parameters>
    </conventions>


## The `radl:resources` Element ##

Resources implement the service. They support methods, which implement state transitions.

    resources = element resources { 
      documentation*, 
      resource*
    }
    resource = element resource {
      documentation*,
      name,
      identity-provider-ref?,
      public?,
      status?,
      location?,
      methods?
    }
    identity-provider-ref = attribute identity-provider { xsd:string }
    public = attribute public { "true" | "false" }
    location = element location { 
      documentation*, 
      (uri | uri-template), 
      location-var*
    }
    location-var = element var { 
      name, 
      documentation*
    }
    uri-template = attribute uri-template { text }
    methods = element methods { 
      method*
    }
    method = element method { 
      method-name, 
      status?, 
      documentation*, 
      method-transitions?, 
      request?, 
      response?
    }
    method-name = attribute name { http-method }
    http-method = "GET" | "PUT" | "HEAD" | "POST" | "DELETE" | "TRACE" | "OPTIONS" | "CONNECT" | "PATCH"
    method-transitions = element transitions { 
      transition-ref-element*
    }
    request = element request {
      documentation*, 
      request-uri-parameters?, 
      header-refs?, 
      representation-refs?
    }
    request-uri-parameters = element uri-parameters { 
      request-uri-parameter-ref*
    }
    request-uri-parameter-ref = element uri-parameter {
      attribute ref { xsd:string }
    }
    header-refs = element headers { 
      documentation*, 
      header-ref*
    }
    header-ref = element header { 
      attribute ref { xsd:string }
    }
    representation-refs = element representations { 
      representation-ref*
    }
    representation-ref = element representation { 
      media-type-name, 
      documentation*
    }
    media-type-name = attribute media-type { xsd:string }
    response = element response {
      documentation*, 
      status-code-refs?, 
      header-refs?, 
      representation-refs?
    }
    status-code-refs =  element status-codes { 
      status-code-ref*
    }
    status-code-ref = element status-code {
      code-ref
    }
    code-ref = attribute ref { HTTP-status-enum }
    HTTP-status-enum = "100" | "101" | "102" | 200 | 201 | 202 | ...

Here's an example:

    <resources>
      <resource name="Menu">
        <location uri="/menu/"/>
        <methods>
          <method name="GET">
            <transitions>
              <transition name="Entry point"/>
            </transitions>
            <response>
              <representations>
                <representation media-type="application/vnd.restbucks+json"/>
              </representations>
            </response>
          </method>
        </methods>
      </resource>
      <!-- ... More resources ... -->
    </resource>
    
    
## The `radl:authentication` Element ##

An important aspect of any API is its security. Security starts with *authentication*, the act of establishing the 
identity of the caller.
RADL provides the `radl:authentication` element for this purpose:

    authentication = element authentication { 
      authentication-conventions?, 
      mechanism*, 
      identity-provider*
    }
    authentication-conventions = element conventions { 
      documentation*, 
      header-refs?, 
      status-code-refs?
    }
    mechanism = element mechanism {
      name, 
      authentication-type, 
      documentation*, 
      scheme*
    }
    mechanism-ref = attribute mechanism { xsd:string }
    identity-provider = element identity-provider { 
      mechanism-ref, 
      documentation*
    }
    authentication-type = attribute authentication-type { text }
    scheme = element scheme { 
      name, 
      documentation*, 
      scheme-parameter*
    }
    scheme-parameter = element parameter { 
      name, 
      documentation*
    }

RADL supports various authentication mechanisms, like OAuth [9], HTTP Signatures [10], or Basic Authentication [11]. 
An authentication mechanism captures the protocol used for authentication, i.e. the type and method of information 
exchange required to establish the identity.

An *identity provider* is the entity that manages someone's identity, like an LDAP [12] server. For each authentication 
mechanism, there can be one or more identity providers. For instance, in a multi-tenant cloud API, each tenant could
have its own identity provider and more than one identity provider could speak the same protocol.


# Properties #

We want RADL to support these kinds of workflows:

* **Code-driven**
  Use RADL Tooling to extract the RADL file from the source code, then add the missing pieces, like the state diagram. 
  Those missing pieces are reported as issues during validation. Whenever the code changes, you can re-run the 
  extraction, and it will keep the information you added
* **Design-driven**
  Let the user create a RADL file first, then generate code from it, or code by hand (perhaps based on documentation 
  generated from the RADL file).
  
There are a couple of variations of the design-driven approach:

* **Requirements-driven** 
  Start with the requirements in the form of a state diagram.
* **Schema-driven**
  Start with an (industry standard) schema. These schemas are foreordained and usually orthogonal to REST semantics.
* **Examples-driven** 
  Only provide examples.
 
To accommodate all of these approaches, RADL provides multiple facilities for specifying the structure of messages:

* The `radl:data-models` element contains `radl:data-model`s, each with a `radl:schema` specifying the 
  structure and `radl:examples` specifying examples in a structured way. A `radl:state` can refer to a 
  `radl:data-model` using the `radl:properties` element, and so can a `radl:transition` (for input that 
  is required to initiate the transition), and a `radl:representation`.
* The `radl:description` element allows linking to an external schema. This element is contained either in a 
  `radl:media-type`, or in a `radl:representation` contained within a `radl:media-type`.
* The `radl:representation` element can contain an `radl:examples` element as well. This would contain 
  unstructured examples fully serialized in the media type.
* A `radl:state` can have a `radl:properties` element even if there is no corresponding `radl:data-model`. 
  It will then simply list the conceptual name of the properties. A `radl:representation` can contain a 
  `radl:properties` element as well, which implicitly links it to the `radl:state`.


# Extending RADL #

TODO


# References #

## Normative References ##

[RADL Schema] Robie, J., Sinnema, R., and Zhou, W., "RESTful API Description Language Schema", April 2015,
<https://github.com/restful-api-description-language/RADL/blob/master/specification/schema/radl.rnc>

[RELAX-NG] Clark, J., "RELAX NG Compact Syntax", December 2001, 
<http://www.oasis-open.org/committees/relax-ng/compact-20021121.html>.

[RFC2119] Bradner, S., "Key words for use in RFCs to Indicate Requirement Levels", BCP 14, RFC 2119, March 1997, <http://tools.ietf.org/html/rfc2119>.

[RFC5988] Nottingham, M., "Web Linking", RFC 5988, October 2010, <http://tools.ietf.org/html/rfc5988>.

[W3C.REC-xml-names-19990114] Hollander, D., Bray, T., and A. Layman, "Namespaces in XML", W3C REC 
REC-xml-names-19990114, January 1999, <http://www.w3.org/TR/1999/REC-xml-names-19990114>.


## Informal References ##

[1] <https://groups.google.com/forum/#!forum/restful-api-description-language>

[2] <https://github.com/restful-api-description-language/RADL>

[3] <http://www.w3.org/TR/wsdl>

[4] <http://www.w3.org/Submission/wadl/>

[5] <http://raml.org/spec.html>

[6] https://github.com/swagger-api/swagger-spec>

[7] <https://github.com/apiaryio/api-blueprint/blob/master/API%20Blueprint%20Specification.md>

[8] <http://www.crummy.com/writing/speaking/2008-QCon/act3.html>

[9] <http://tools.ietf.org/html/rfc6749>

[10] <http://tools.ietf.org/html/draft-cavage-http-signatures-03>

[11] <http://tools.ietf.org/html/rfc7235>

[12] <https://tools.ietf.org/html/rfc4510>


# Contributors #

The following people contributed to preliminary versions of this document: Jonathan Robie, Remon Sinnema, and 
William Zhou. The content and concepts within are a product of the RADL community.

The RADL specification has has benefited from contributors who proposed ideas and wording for this document, 
including: Erik Wilde and Derek Zasiewski.
