/*
 * Copyright © EMC Corporation. All rights reserved.
 */
package radl.java.generation.spring;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import radl.core.code.Code;
import radl.core.code.common.Constants;
import radl.core.code.radl.RadlCode;


public class FromRadlCodeGenerationInitializer extends FromRadlCodeGenerator {

  @Override
  protected Collection<Code> generateFromRadl(RadlCode radl, Map<String, Object> context) {
    context.put(DEFAULT_MEDIA_TYPE, radl.defaultMediaType());
    context.put(HAS_HYPERMEDIA, radl.hasHyperMediaTypes());
    
    Constants linkRelationConstants = new Constants("LINK_REL", "Link relations");
    addLinkRelationConstants(radl, linkRelationConstants);
    context.put(CONSTANTS_LINK_RELATIONS, linkRelationConstants);
    
    Map<Integer, String> httpStatuses = new HashMap<Integer, String>();
    initHttpStatuses(httpStatuses);
    context.put(SPRING_HTTP_STATUSES, httpStatuses);
    
    return Collections.emptyList();
  }

  private void addLinkRelationConstants(RadlCode radl, Constants linkRelationConstants) {
    for (String value : radl.linkRelationNames()) {
      String[] segments = value.split("/");
      String name = segments[segments.length - 1];
      String documentation = radl.linkRelationDocumentation(value);
      linkRelationConstants.add(name, value, documentation);
    }
  }

  private void initHttpStatuses(Map<Integer, String> httpStatuses) {
    httpStatuses.put(400, "BAD_REQUEST");
    httpStatuses.put(401, "UNAUTHORIZED");
    httpStatuses.put(402, "PAYMENT_REQUIRED");
    httpStatuses.put(403, "FORBIDDEN");
    httpStatuses.put(404, "NOT_FOUND");
    httpStatuses.put(405, "METHOD_NOT_ALLOWED");
    httpStatuses.put(406, "NOT_ACCEPTABLE");
    httpStatuses.put(407, "PROXY_AUTHENTICATION_REQUIRED");
    httpStatuses.put(408, "REQUEST_TIMEOUT");
    httpStatuses.put(409, "CONFLICT");
    httpStatuses.put(410, "GONE");
    httpStatuses.put(411, "LENGTH_REQUIRED");
    httpStatuses.put(412, "PRECONDITION_FAILED");
    httpStatuses.put(413, "PAYLOAD_TOO_LARGE");
    httpStatuses.put(414, "URI_TOO_LONG");
    httpStatuses.put(415, "UNSUPPORTED_MEDIA_TYPE");
    httpStatuses.put(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
    httpStatuses.put(417, "EXPECTATION_FAILED");
    httpStatuses.put(422, "UNPROCESSABLE_ENTITY");
    httpStatuses.put(423, "LOCKED");
    httpStatuses.put(424, "FAILED_DEPENDENCY");
    httpStatuses.put(426, "UPGRADE_REQUIRED");
    httpStatuses.put(428, "PRECONDITION_REQUIRED");
    httpStatuses.put(429, "TOO_MANY_REQUESTS");
    httpStatuses.put(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");
    httpStatuses.put(500, "INTERNAL_SERVER_ERROR");
    httpStatuses.put(501, "NOT_IMPLEMENTED");
    httpStatuses.put(502, "BAD_GATEWAY");
    httpStatuses.put(503, "SERVICE_UNAVAILABLE");
    httpStatuses.put(504, "GATEWAY_TIMEOUT");
    httpStatuses.put(505, "HTTP_VERSION_NOT_SUPPORTED");
    httpStatuses.put(506, "VARIANT_ALSO_NEGOTIATES");
    httpStatuses.put(507, "INSUFFICIENT_STORAGE");
    httpStatuses.put(508, "LOOP_DETECTED");
    httpStatuses.put(509, "BANDWIDTH_LIMIT_EXCEEDED");
    httpStatuses.put(510, "NOT_EXTENDED");
    httpStatuses.put(511, "NETWORK_AUTHENTICATION_REQUIRED");
  }

}
