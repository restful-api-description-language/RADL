# RESTful API Description Language (RADL) #


## Abstract ##

This specification describes RESTful API Description Language (RADL),
an XML vocabulary for describing hypermedia-driven RESTful APIs. 

   
## Note to Readers ##

This draft should be discussed on the RADL mailing list [1].
Online access to all versions and files is available on GitHub [1].
   
   
## Status of this Memo ##

This is a draft.


## Copyright Notice ##

Copyright (c) 2014-2015 EMC Corporation. All rights reserved.


# RESTful API Description Language #

RESTful API Description Language (RADL) is an XML vocabulary for specifying and documenting RESTful APIs. 
It is specifically designed to support the design phase of the software development lifecycle.

We decided upon an XML-based format so that we can use our trusted XML toolkit to develop RADL tooling.
You can use RADL to describe REST APIs that use any media type, not just XML-based ones.


## States

A REST client starts at some well-known URI (the billboard URI). It then follows links that it discovers
through link relations from the responses until its goal is met. In other words, the client starts at some
initial state and then transitions to other states by following links. 
A natural way to capture this information is in the form of a *state diagram*.

In RADL, the state diagram is described as a list of client states and their transitions to other states: 

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
    

## Link Relations ##

Link relations identify the semantics of links by describing how the current context is related to
another resource [RFC 5988](http://tools.ietf.org/html/rfc5988).
A client transitions from state to state by using link relations to find suitable links.
 
In RADL, link relations are associated with state transitions:

    <link-relations>
      <link-relation name="http://relations.restbucks.com/order">
        <transitions>
          <transition name="Create order"/>
        </transitions>
      </link-relation>
     <!-- ... More link relations ... -->
    </link-relations>
   

## Media Types ##

Media types specify the concrete representation of a state as a series of bytes, typically in a 
JSON or XML format. A JSON Schema, RELAX-NG Schema, or XML Schema can be provided to precisely specify 
the structure of the messages in such a format.

    <media-types> 
      <media-type name="application/vnd.restbucks+json">
        <documentation>
          An application-specific media type for the RESTBucks API.
        </documentation>
      </media-type>
      <!-- ... More media types ... -->
    </media-types>


## Service Conventions ##

We discourage the anti-pattern of specifying the allowable headers, status codes, and URI query parameters 
for each response. REST clients should instead always be prepared to handle all the standard ones.

However, sometimes a service uses a header or status code in a very specific way. In that case, RADL lets
you describe such a service-specific convention in a single location that can be referred to later.


## Resources ##

Resources implement the service. They support methods, which implement state transitions.

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
    

# References

[1] https://groups.google.com/forum/#!forum/restful-api-description-language

[2] https://github.com/restful-api-description-language/RADL
