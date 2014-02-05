RADL
====

In a REST API, the server provides options to a client in the form of
hypermedia links in documents, and the main thing a client needs to
know is how to locate and use these links in order to use the API. The
main job of a REST API description is to provide this information to
the client in the context of media type descriptions. Unfortunately,
most REST service description languages and design methodologies focus
on other concerns instead.

RESTful API Description Language (RADL) is an XML vocabulary for
describing Hypermedia-driven RESTful APIs. The APIs it describes may
use any media type, in XML, JSON, HTML, or any other format. The
structure of a RADL description is based on media types, including the
documents associated with a media type, links found in these
documents, and the interfaces associated with these links.

RADL can be used as a specification language or as run-time metadata
to describe a service. A JSON representation of RADL is under
development.

See spec/RADL.html for documentation.

Copyright 2014, EMC Corporation
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

