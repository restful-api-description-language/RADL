<?xml version="1.0" encoding="utf-8"?>
<!-- 
##
## Converts a RADL description to HTML.
##
## Copyright © 2013-2015 EMC Corporation. All rights reserved.
##
-->

<!--
   ##########################################################################
   ###  Do not change XSLT version to "3.0" - this must run with Saxon HE ###
   ##########################################################################
-->
<xsl:stylesheet version="2.0" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:html="http://www.w3.org/1999/xhtml"
  xmlns:radl="urn:radl:service" exclude-result-prefixes="#all">

  <xsl:output method="html" encoding="utf-8" indent="yes" cdata-section-elements="radl:example"/>

  <xsl:key name="status" match="//radl:status-codes/radl:status" use="@name"/>

  <xsl:variable name="start-state-name">Start</xsl:variable>
  <xsl:variable name="general-media-types"
    select="('application/ld+json', 'application/vnd.mason+json', 'application/vnd.siren+json', 'application/hal+json', '*/*')"/>

  <xsl:template match="/radl:service">
    <html>
      <head>
        <title>
          <xsl:call-template name="title"/>
        </title>
        <xsl:call-template name="style"/>
      </head>
      <body>
        <div class="outline index">
          <xsl:call-template name="index"/>
        </div>
        <div class="outline reference">
          <xsl:call-template name="reference"/>
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template name="title">
    <xsl:value-of select="@name"/> REST Service </xsl:template>

  <xsl:template name="style">
    <style type="text/css">
      body{
          margin:0;
          padding:0 0 0 16em;
      }
      
      h1,
      h2{
          color:Navy;
      }
      h3{
          color:Blue;
      }
      
      table{
          border-collapse:collapse;
          margin-bottom:1em;
          width:90%
      }
      th,
      td{
          border:1px solid;
          padding:0.35em;
          vertical-align:top;
      }
      th{
          color:White;
          background-color:CornflowerBlue;
          text-align:left;
          border-color:Black;
      }
      td{
          border-color:LightGrey;
          vertical-align:middle;
      }
      tr:nth-child(odd){
          background-color:eeeeef;
          padding:16px;
      }
      
      dl{
          margin:16px;
      }
      dl dl{
          background-color:eeeeef;
          padding:12px;
      }
      
      dt{
          font-weight:bold;
          padding-top:9px;
      }
      dt::after{
          content:":";
      }
      dd{
          margin:0;
          padding-left:16px;
      }
      div.nested{
          margin:0;
          padding-left:16px;
      }
      
      li.transition{
          list-style-type:none;
      }
      
      .outline{
          vertical-align:top;
          padding:1em;
      }
      .index{
          position:fixed;
          top:0;
          left:0;
          width:16em;
          height:100%;
          font-size:smaller;
      }
      .reference{
          height:100%;
      }
      .buggy{
          background-color:yellow;
      }
      .http{
          border-radius:2px;
          color:white;
          display:inline-block;
          font-size:0.7em;
          padding:7px 0 4px;
          text-align:center;
          width:50px;
      }
      .http a{
          color:white;
      }
      .DELETE{
          background-color:#a41e22;
      }
      .GET{
          background-color:#0f6ab4;
      }
      .PATCH{
          background-color:#d38042;
      }
      .POST{
          background-color:#10a54a;
      }
      .PUT{
          background-color:#c5862b;
      }
      code{
          background:none repeat scroll 0 0 #f5f5f5;
          border:1px solid #ccc;
          border-radius:2px;
          padding:1px 3px;
      }</style>
  </xsl:template>

  <xsl:template name="index">
    <h2>
      <xsl:value-of select="/radl:service/@name"/>
    </h2>
    <ul>
      <li>
        <h3>
          <a href="#states">States</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#link-relations">Link Relations</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#property-groups">Property Groups</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#uri-parameters">URI Parameters</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#media-types">Media Types</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#headers">Custom Headers</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#status-codes">Status Codes</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#resources">Resources</a>
        </h3>
      </li>
      <li>
        <h3>
          <a href="#authentication">Authentication</a>
        </h3>
      </li>
    </ul>
  </xsl:template>

  <xsl:template name="reference">
    <h1>
      <xsl:value-of select="/radl:service/@name"/>
    </h1>
    <xsl:apply-templates select="radl:documentation"/>

    <xsl:call-template name="states"/>
    <xsl:call-template name="link-relations"/>
    <xsl:call-template name="property-groups"/>
    <xsl:call-template name="uri-parameters"/>
    <xsl:call-template name="custom-headers"/>
    <xsl:call-template name="status-codes"/>
    <xsl:call-template name="media-types"/>
    <xsl:call-template name="resources"/>
    <xsl:call-template name="authentication"/>

  </xsl:template>

  <xsl:template name="states">
    <hr/>
    <h1 id="states">States</h1>
    <xsl:call-template name="states-table"/>
    <xsl:call-template name="states-detail"/>
  </xsl:template>

  <xsl:template name="states-table">
    <xsl:choose>
      <xsl:when test="//radl:start-state | //radl:state">
        <table sortable="true">
          <thead>
            <tr>
              <th>State</th>
              <th>Transition</th>
              <th>Result State</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates mode="table-row" select="//radl:start-state"/>
            <xsl:apply-templates mode="table-row" select="//radl:state"/>
          </tbody>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <h3>*** TODO ***</h3>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="radl:start-state|radl:state" mode="table-row">

    <xsl:variable name="state" select="."/>
    <xsl:variable name="state-name" select="($state/@name|$start-state-name)[1]"/>

    <xsl:for-each select="radl:transitions/radl:transition">
      <tr>
        <td>
          <xsl:call-template name="a-href">
            <xsl:with-param name="prefix">state</xsl:with-param>
            <xsl:with-param name="name" select="$state-name"/>
          </xsl:call-template>
        </td>
        <td>
          <xsl:call-template name="a-href">
            <xsl:with-param name="prefix">transition</xsl:with-param>
            <xsl:with-param name="scope" select="$state-name"/>
            <xsl:with-param name="name" select="./@name"/>
          </xsl:call-template>
        </td>
        <td>
          <xsl:call-template name="a-href">
            <xsl:with-param name="prefix">state</xsl:with-param>
            <xsl:with-param name="name" select="@to"/>
          </xsl:call-template>
        </td>
      </tr>
    </xsl:for-each>
  </xsl:template>

  <!-- State and transitions -->
  <xsl:template name="states-detail">
    <div class="state-detail">
      <xsl:apply-templates select="//radl:start-state"/>
      <xsl:apply-templates select="//radl:state"/>
    </div>
  </xsl:template>

  <xsl:template match="radl:start-state|radl:state">
    <hr/>
    <h2>
      <xsl:variable name="state" select="(@name|$start-state-name)[1]"/>
      <xsl:call-template name="id">
        <xsl:with-param name="prefix">state</xsl:with-param>
        <xsl:with-param name="name" select="$state"/>
      </xsl:call-template>
      <xsl:text>State: </xsl:text>
      <xsl:value-of select="$state"/>
    </h2>
    <xsl:if test="@property-group">
      <h3>Properties</h3>
      <p>See <xsl:call-template name="a-href">
          <xsl:with-param name="prefix">propertygroup</xsl:with-param>
          <xsl:with-param name="name" select="@property-group"/>
        </xsl:call-template>
      </p>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:template>

  <xsl:template match="radl:transitions">
    <h3>Transitions</h3>
    <xsl:choose>
      <xsl:when test=".//radl:transition">
        <table sortable="true">
          <thead>
            <tr>
              <th width="20%">Transition</th>
              <th width="50%">Link Relation</th>
              <th width="10%">Method</th>
              <th width="20%">Result State</th>
            </tr>
          </thead>
          <tbody>
            <xsl:apply-templates mode="table-row" select="radl:transition"/>
          </tbody>
        </table>

        <xsl:apply-templates select=".//radl:transition"/>
      </xsl:when>
      <xsl:otherwise>
        <p>No transitions.</p>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template match="radl:transition" mode="table-row">
    <xsl:variable name="from-state" select="(ancestor::radl:state/@name|$start-state-name)[1]"/>
    <xsl:variable name="transition-name" select="@name"/>
    <xsl:variable name="result-state" select="@to"/>
    <xsl:variable name="link-relation"
      select="//radl:link-relation[radl:transitions/radl:transition/@ref=$transition-name]/@name"/>
    <xsl:variable name="interfaces"
      select="//radl:method[radl:transitions/radl:transition[@ref=$transition-name and (if (@from) then @from=$from-state else true())]]"/>

    <tr>
      <td>
        <xsl:call-template name="a-href">
          <xsl:with-param name="prefix">transition</xsl:with-param>
          <xsl:with-param name="scope" select="$from-state"/>
          <xsl:with-param name="name" select="$transition-name"/>
        </xsl:call-template>
      </td>
      <td>
        <xsl:choose>
          <xsl:when test="$link-relation">
            <code>
              <xsl:call-template name="a-href">
                <xsl:with-param name="prefix">linkrel</xsl:with-param>
                <xsl:with-param name="name" select="$link-relation"/>
              </xsl:call-template>
            </code>
          </xsl:when>
          <xsl:otherwise>[None]</xsl:otherwise>
        </xsl:choose>
      </td>
      <td>
        <xsl:for-each select="$interfaces">
          <xsl:variable name="interface" select="."/>
          <xsl:variable name="resource" select="ancestor::radl:resource/@name"/>
          <xsl:variable name="http-method" select="./@name"/>
          <span>
            <xsl:attribute name="class">
              <xsl:text>http </xsl:text>
              <xsl:value-of select="$http-method"/>
            </xsl:attribute>
            <a>
              <xsl:call-template name="href">
                <xsl:with-param name="prefix">method</xsl:with-param>
                <xsl:with-param name="scope" select="$resource"/>
                <xsl:with-param name="name" select="$http-method"/>
              </xsl:call-template>
              <xsl:value-of select="$http-method"/>
            </a>
          </span>
        </xsl:for-each>
      </td>
      <td>
        <xsl:call-template name="a-href">
          <xsl:with-param name="prefix">state</xsl:with-param>
          <xsl:with-param name="name" select="$result-state"/>
        </xsl:call-template>
      </td>
    </tr>

  </xsl:template>

  <xsl:template match="radl:transition">
    <xsl:variable name="transition" select="."/>
    <xsl:variable name="transition-name" select="@name"/>
    <xsl:variable name="from-state" select="(ancestor::radl:state/@name|$start-state-name)[1]"/>
    <xsl:variable name="link-relation"
      select="//radl:link-relation[radl:transitions/radl:transition/@ref=$transition-name]/@name"/>

    <xsl:variable name="interface"
      select="//radl:method[radl:transitions/radl:transition[@ref=$transition-name and (if (@from) then @from=$from-state else true())]]"/>

    <!-- merge transitions with the same name within a given state -->
    <xsl:if test="not(preceding-sibling::radl:transition[@name=$transition-name])">
      <xsl:variable name="transitions-with-same-name"
        select="following-sibling::radl:transition[@name=$transition-name]"/>

      <li class="transition">
        <h3>
          <xsl:call-template name="id">
            <xsl:with-param name="prefix">transition</xsl:with-param>
            <xsl:with-param name="scope" select="$from-state"/>
            <xsl:with-param name="name" select="$transition-name"/>
          </xsl:call-template>
          <xsl:text>Transition: </xsl:text>
          <xsl:value-of select="$transition-name"/>
        </h3>

        <xsl:apply-templates select="radl:documentation"/>

        <xsl:if test="empty($interface)">
          <h4>This transition is not yet specified in an HTTP interface.</h4>
        </xsl:if>

        <xsl:for-each select="$interface">
          <xsl:variable name="method" select="."/>
          <dl>

            <dt>HTTP Method</dt>
            <dd>

              <xsl:call-template name="id">
                <xsl:with-param name="prefix">transition</xsl:with-param>
                <xsl:with-param name="scope" select="$from-state"/>
                <xsl:with-param name="name" select="concat($transition-name, '-', $method/@name)"/>
              </xsl:call-template>

              <span>
                <xsl:attribute name="class">
                  <xsl:text>http </xsl:text>
                  <xsl:value-of select="$method/@name"/>
                </xsl:attribute>
                <xsl:value-of select="$method/@name"/>
              </span>
              <xsl:apply-templates select="radl:documentation"/>
            </dd>
            <dt>Link Relation</dt>
            <dd>
              <xsl:choose>
                <xsl:when test="$link-relation">
                  <code>
                    <xsl:call-template name="a-href">
                      <xsl:with-param name="prefix">linkrel</xsl:with-param>
                      <xsl:with-param name="name" select="$link-relation"/>
                    </xsl:call-template>
                  </code>
                </xsl:when>
                <xsl:otherwise>[None]</xsl:otherwise>
              </xsl:choose>
            </dd>

            <xsl:apply-templates select="$method/radl:request">
              <xsl:with-param name="property-group" select="$transition/radl:input/@property-group"/>
              <xsl:with-param name="transitions" select="$transition"/>
            </xsl:apply-templates>
            <xsl:apply-templates select="$method/radl:response">
              <xsl:with-param name="transitions" select="$transition"/>
            </xsl:apply-templates>
          </dl>
        </xsl:for-each>
      </li>
    </xsl:if>
  </xsl:template>

  <xsl:template match="radl:request">
    <xsl:param name="property-group"/>
    <xsl:param name="transitions"/>
    <dt>Request</dt>
    <dd>
      <dl>
        <xsl:apply-templates select="radl:headers"/>
        <xsl:apply-templates select="radl:authentication"/>
        <xsl:apply-templates select="radl:uri-parameters"/>
        <xsl:if test="$property-group">
          <dt>Input</dt>
          <dd>
            <xsl:call-template name="a-href">
              <xsl:with-param name="prefix">propertygroup</xsl:with-param>
              <xsl:with-param name="name" select="$property-group"/>
            </xsl:call-template>
          </dd>
        </xsl:if>
        <xsl:apply-templates select="radl:representations">
          <xsl:with-param name="representation-names"
            select="distinct-values($transitions/radl:input/radl:properties/@name)"/>
        </xsl:apply-templates>
      </dl>
    </dd>
  </xsl:template>

  <xsl:template match="radl:response">
    <xsl:param name="transitions"/>
    <xsl:variable name="result-states" as="xs:string*">
      <xsl:call-template name="end-states-from-transitions">
        <xsl:with-param name="transitions"
          select="preceding-sibling::radl:transitions/radl:transition"/>
      </xsl:call-template>
    </xsl:variable>

    <dt>Response</dt>
    <dd>
      <dl>
        <xsl:if test="count($result-states) > 0">
          <dt>Result States</dt>
          <dd>
            <ul>
              <xsl:for-each select="$result-states">
                <xsl:sort select="."/>
                <li>
                  <xsl:call-template name="a-href">
                    <xsl:with-param name="prefix">state</xsl:with-param>
                    <xsl:with-param name="name" select="."/>
                  </xsl:call-template>
                </li>
              </xsl:for-each>
            </ul>
          </dd>
        </xsl:if>
        <!-- ### ### -->
        <xsl:apply-templates select="radl:headers"/>
        <xsl:apply-templates select="radl:authentication"/>
        <xsl:apply-templates select="radl:representations">
          <xsl:with-param name="representation-names" select="$result-states"/>
        </xsl:apply-templates>
        <xsl:apply-templates select="radl:status-codes"/>
      </dl>
    </dd>
  </xsl:template>

  <xsl:template match="radl:headers">
    <dt>Headers</dt>
    <dd>
      <ul>
        <xsl:for-each select="radl:header">
          <li>
            <xsl:call-template name="a-href">
              <xsl:with-param name="prefix">header</xsl:with-param>
              <xsl:with-param name="name" select="@ref"/>
            </xsl:call-template>
            <xsl:choose>
              <xsl:when test="@required='true'">(required)</xsl:when>
              <xsl:otherwise> (optional)</xsl:otherwise>
            </xsl:choose>
          </li>
        </xsl:for-each>
      </ul>
    </dd>
  </xsl:template>


  <xsl:template match="radl:status-codes">
    <dt>Status Codes</dt>
    <dd>
      <ul>
        <xsl:for-each select="radl:status-code">
          <li>
            <xsl:call-template name="a-href">
              <xsl:with-param name="prefix">statuscode</xsl:with-param>
              <xsl:with-param name="name" select="@ref"/>
            </xsl:call-template>
          </li>
        </xsl:for-each>
      </ul>
    </dd>
  </xsl:template>

  <xsl:template match="radl:request/radl:uri-parameters">
    <dt>URI Parameters</dt>
    <dd>
      <ul>
        <xsl:for-each select="radl:uri-parameter">
          <li>
            <code>
              <xsl:call-template name="a-href">
                <xsl:with-param name="prefix">uriparameter</xsl:with-param>
                <xsl:with-param name="name" select="@ref"/>
              </xsl:call-template>
            </code>
          </li>
        </xsl:for-each>
      </ul>
    </dd>
  </xsl:template>

  <xsl:template match="radl:representations">
    <xsl:param name="representation-names"/>
    <dt>Media Types</dt>
    <dd>
      <ul>
        <xsl:for-each select="radl:representation">
          <xsl:variable name="mediatype-name" select="@media-type"/>
          <li>
            <code>
              <xsl:call-template name="a-href">
                <xsl:with-param name="prefix">mediatype</xsl:with-param>
                <xsl:with-param name="name" select="$mediatype-name"/>
              </xsl:call-template>
            </code>
            <xsl:if
              test="count($representation-names) > 0 and //radl:media-type[@name=$mediatype-name]/radl:representation[@name=$representation-names]">
              <xsl:text> (</xsl:text>
              <xsl:for-each
                select="//radl:media-type[@name=$mediatype-name]/radl:representation[@name=$representation-names]/@name">
                <code>
                  <xsl:call-template name="a-href">
                    <xsl:with-param name="prefix">representation</xsl:with-param>
                    <xsl:with-param name="scope" select="$mediatype-name"/>
                    <xsl:with-param name="name" select="."/>
                  </xsl:call-template>
                </code>
              </xsl:for-each>
              <xsl:text> )</xsl:text>
            </xsl:if>
          </li>
        </xsl:for-each>
      </ul>
    </dd>
  </xsl:template>

  <xsl:template name="resources">
    <hr/>
    <h1 id="resources">Resources</h1>
    <xsl:choose>
      <xsl:when test="//radl:resource">
        <table>
          <thead>
            <tr>
              <th>Resource</th>
              <th>Location</th>
              <th>Methods</th>
            </tr>
          </thead>
          <tbody>
            <xsl:for-each select="//radl:resource">
              <tr>
                <td>
                  <xsl:call-template name="a-href">
                    <xsl:with-param name="prefix">resource</xsl:with-param>
                    <xsl:with-param name="name" select="@name"/>
                  </xsl:call-template>
                </td>
                <td>
                  <code>
                    <xsl:value-of select="radl:location/@uri | radl:location/@uri-template"/>
                  </code>
                </td>
                <td>
                  <xsl:variable name="resource-name" select="@name"/>
                  <xsl:for-each select=".//radl:methods/radl:method">
                    <span>
                      <xsl:attribute name="class">
                        <xsl:text>http </xsl:text>
                        <xsl:value-of select="@name"/>
                      </xsl:attribute>
                      <xsl:call-template name="a-href">
                        <xsl:with-param name="prefix">method</xsl:with-param>
                        <xsl:with-param name="scope" select="$resource-name"/>
                        <xsl:with-param name="name" select="@name"/>
                      </xsl:call-template>
                    </span>
                    <xsl:text> </xsl:text>
                  </xsl:for-each>
                </td>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>
        <xsl:apply-templates select="radl:resources/radl:resource"/>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="radl:resource">
    <h2>
      <xsl:call-template name="id">
        <xsl:with-param name="prefix">resource</xsl:with-param>
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
      <xsl:text>Resource: </xsl:text>
      <xsl:value-of select="@name"/>
    </h2>

    <dl>
      <dt>Location</dt>
      <dd>
        <code>
          <xsl:value-of select="radl:location/@uri | radl:location/@uri-template"/>
        </code>
      </dd>
    </dl>

    <xsl:apply-templates select="radl:documentation"/>

    <xsl:apply-templates select="radl:methods"/>

  </xsl:template>

  <xsl:template match="radl:methods">
    <h3>Methods:</h3>
    <xsl:apply-templates select="radl:method"/>
  </xsl:template>

  <xsl:template match="radl:method">
    <dl>
      <xsl:call-template name="id">
        <xsl:with-param name="prefix">method</xsl:with-param>
        <xsl:with-param name="scope" select="./ancestor::radl:resource/@name"/>
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>

      <dt>HTTP Method</dt>
      <dd>
        <span>
          <xsl:attribute name="class">
            <xsl:text>http </xsl:text>
            <xsl:value-of select="@name"/>
          </xsl:attribute>
          <xsl:value-of select="@name"/>
        </span>
        <xsl:apply-templates select="radl:documentation"/>
      </dd>

      <xsl:variable name="transitions" select="radl:transitions/radl:transition"/>

      <xsl:if test="$transitions">
        <dt>Transitions</dt>
        <dd>
          <ul>
            <xsl:for-each select="$transitions">
              <xsl:sort select="@ref"/>

              <xsl:variable name="transition-name" select="@ref"/>
              <xsl:variable name="from" select="@from"/>
              <xsl:variable name="states"
                select="if ($from) then
                                                    if ($from = $start-state-name)
                                                    then //radl:start-state
                                                    else //radl:state[@name=$from]
                                                  else //radl:start-state|//radl:state"/>

              <xsl:for-each
                select="$states[radl:transitions/radl:transition[@name=$transition-name]]">
                <xsl:sort select="@name"/>
                <xsl:variable name="state-name" select="(@name|$start-state-name)[1]"/>
                <li>
                  <xsl:call-template name="a-href">
                    <xsl:with-param name="prefix">transition</xsl:with-param>
                    <xsl:with-param name="scope" select="$state-name"/>
                    <xsl:with-param name="name" select="$transition-name"/>
                  </xsl:call-template>
                  <xsl:text> : </xsl:text>
                  <xsl:call-template name="a-href">
                    <xsl:with-param name="prefix">state</xsl:with-param>
                    <xsl:with-param name="name" select="@name"/>
                  </xsl:call-template>
                  <xsl:variable name="previous" select="@name"/>
                  <xsl:for-each
                    select="radl:transitions/radl:transition[@name=$transition-name]/@to">
                    <xsl:choose>
                      <xsl:when test="position() = 1">
                        <xsl:text> &#8594; </xsl:text>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:text>, </xsl:text>
                      </xsl:otherwise>
                    </xsl:choose>
                    <xsl:call-template name="a-href">
                      <xsl:with-param name="prefix">state</xsl:with-param>
                      <xsl:with-param name="name" select="."/>
                    </xsl:call-template>
                  </xsl:for-each>
                </li>
              </xsl:for-each>
            </xsl:for-each>
          </ul>
        </dd>
      </xsl:if>

      <xsl:apply-templates select="radl:request">
        <xsl:with-param name="transitions" select="$transitions"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="radl:response">
        <xsl:with-param name="transitions" select="$transitions"/>
      </xsl:apply-templates>
    </dl>
  </xsl:template>

  <xsl:template name="authentication">
    <hr/>
    <h1 id="authentication">Authentication</h1>
    <xsl:choose>
      <xsl:when test="/radl:service/radl:authentication">
        <xsl:apply-templates select="/radl:service/radl:authentication"/>
      </xsl:when>
      <xsl:otherwise>
        <p>No authentication.</p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="radl:service/radl:authentication">
    <xsl:if test="radl:conventions">
      <h2 id="authentication-conventions">Conventions</h2>
      <dl>
        <xsl:apply-templates select="radl:conventions/radl:headers"/>
        <xsl:apply-templates select="radl:conventions/radl:status-codes"/>
      </dl>
    </xsl:if>
    <xsl:if test="radl:identity-provider">
      <h2 id="authentication-identity-providers">Identity Providers</h2>
      <table sortable="true">
        <thead>
          <tr>
            <th>Mechanism</th>
            <th>Description</th>
          </tr>
        </thead>
        <tbody>
          <xsl:for-each select="radl:identity-provider">
            <tr>
              <td>
                <xsl:call-template name="a-href">
                  <xsl:with-param name="prefix">mechanism</xsl:with-param>
                  <xsl:with-param name="name" select="@mechanism"/>
                </xsl:call-template>
              </td>
              <td>
                <xsl:apply-templates select="radl:documentation"/>
              </td>
            </tr>
          </xsl:for-each>
        </tbody>
      </table>
    </xsl:if>
    <xsl:if test="radl:mechanism">
      <h2 id="authentication-mechanisms">Mechanisms</h2>
      <dl>
        <xsl:apply-templates select="radl:mechanism"/>
      </dl>
    </xsl:if>
  </xsl:template>

  <xsl:template match="radl:mechanism">
    <dt>
      <xsl:call-template name="id">
        <xsl:with-param name="prefix">mechanism</xsl:with-param>
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
      <xsl:value-of select="@name"/>
      <xsl:if test="@authentication-type != @name">
        <xsl:text> (</xsl:text>
        <xsl:value-of select="@authentication-type"/>
        <xsl:text>) </xsl:text>
      </xsl:if>
    </dt>
    <dd>
      <table sortable="true">
        <thead>
          <tr>
            <th>Scheme</th>
            <th>Description</th>
            <th>Parameters</th>
          </tr>
        </thead>
        <tbody>
          <xsl:for-each select="radl:scheme">
            <tr>
              <td>
                <xsl:call-template name="id">
                  <xsl:with-param name="prefix">
                    <xsl:value-of
                      select="concat('mechanism-',ancestor::radl:scheme/@name,'-scheme-', @name)"/>
                  </xsl:with-param>
                  <xsl:with-param name="name" select="@name"/>
                </xsl:call-template>
                <xsl:value-of select="@name"/>
              </td>
              <td>
                <xsl:apply-templates select="radl:documentation"/>
              </td>
              <td>
                <xsl:if test="radl:parameter">
                  <h3>Parameters:</h3>
                  <dl>
                    <xsl:for-each select="radl:parameter">
                      <dt>
                        <xsl:value-of select="@name"/>
                      </dt>
                      <dd>
                        <xsl:apply-templates select="radl:documentation"/>
                      </dd>
                    </xsl:for-each>
                  </dl>
                </xsl:if>
              </td>
            </tr>
          </xsl:for-each>
        </tbody>
      </table>
    </dd>
  </xsl:template>

  <xsl:template match="radl:identity-provider"> </xsl:template>

  <xsl:template name="no-authentication">
    <xsl:if test="//radl:authentication/radl:mechanism">
      <h4>Authentication</h4>
      <p>This resource requires no authentication.</p>
    </xsl:if>
  </xsl:template>

  <xsl:template name="identity-provider">
    <xsl:param name="id"/>
    <xsl:variable name="idp" select="//radl:authentication/radl:identity-provider[@id = $id]"/>
    <xsl:variable name="mechanismId" select="$idp/@mechanism-ref"/>
    <xsl:variable name="mechanism" select="//radl:authentication/radl:mechanism[@id = $mechanismId]"/>
    <h4>Authentication</h4>
    <table>
      <tr>
        <th>Mechanism</th>
        <th>Identity Provider</th>
      </tr>
      <tr>
        <td>
          <a>
            <xsl:attribute name="href">#<xsl:value-of select="$mechanismId"/></xsl:attribute>
            <xsl:apply-templates select="$mechanism/@name"/>
          </a>
        </td>
        <td>
          <xsl:apply-templates select="$idp/radl:documentation"/>
        </td>
      </tr>
    </table>
  </xsl:template>

  <xsl:template name="implemented">
    <xsl:choose>
      <xsl:when test="@status = 'full'">
        <span id="full">&#x2714;</span>
        <span id="hint">Fully implemented</span>
      </xsl:when>
      <xsl:when test="@status = 'partial'">
        <span id="partial">?</span>
        <span id="hint">Partially implemented</span>
      </xsl:when>
      <xsl:otherwise>
        <span id="no">&#x2718;</span>
        <span id="hint">Not implemented</span>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="radl:documentation">
    <!-- If text occurs at the root level, wrap it in a <p/>; otherwise assume HTML elements -->
    <xsl:choose>
      <xsl:when test="text()[normalize-space()]">
        <p>
          <xsl:apply-templates/>
        </p>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="html:*">
    <xsl:element name="{local-name()}">
      <xsl:for-each select="@*">
        <xsl:attribute name="{local-name()}">
          <xsl:value-of select="."/>
        </xsl:attribute>
      </xsl:for-each>
      <xsl:apply-templates select="text() | node()"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="radl:authentication">
    <xsl:variable name="mechanism" select="@mechanism-ref"/>
    <h4>Authentication</h4>
    <p>
      <a>
        <xsl:attribute name="href">#<xsl:value-of select="$mechanism"/></xsl:attribute>
        <xsl:value-of select="//radl:authentication/radl:mechanism[@id = $mechanism]/@name"/>
      </a>.&#160; <xsl:apply-templates select="*"/>
    </p>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="."/>
  </xsl:template>

  <xsl:template match="radl:ref">
    <xsl:choose>
      <xsl:when test="@resource">
        <xsl:variable name="id" select="@resource"/>
        <xsl:call-template name="ref-by-id">
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="name" select="//radl:resources/radl:resource[@id = $id]/@name"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="@status-code">
        <xsl:variable name="id" select="@status-code"/>
        <xsl:variable name="code" select="//radl:status-codes/radl:status-code[@id = $id]/@name"/>
        <xsl:call-template name="ref-by-id">
          <xsl:with-param name="id" select="concat('statuscode-', $code)"/>
          <xsl:with-param name="name" select="$code"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="@uri-parameter">
        <xsl:variable name="id" select="@uri-parameter"/>
        <xsl:call-template name="ref-by-id">
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="name"
            select="//radl:uri-parameters/radl:uri-parameter[@id = $id]/@name"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="@header">
        <xsl:variable name="id" select="@header"/>
        <xsl:call-template name="ref-by-id">
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="name" select="//radl:headers/radl:header[@id = $id]/@name"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="@mechanism">
        <xsl:variable name="id" select="@mechanism"/>
        <xsl:call-template name="ref-by-id">
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="name" select="//radl:authentication/radl:mechanism[@id = $id]/@name"
          />
        </xsl:call-template>
      </xsl:when>
      <xsl:when test="@media-type">
        <xsl:variable name="id" select="@media-type"/>
        <xsl:call-template name="ref-by-id">
          <xsl:with-param name="id" select="$id"/>
          <xsl:with-param name="name" select="//radl:media-types/radl:media-type[@id = $id]/@name"/>
        </xsl:call-template>
      </xsl:when>
      <xsl:otherwise>
        <a>
          <xsl:attribute name="href">
            <xsl:value-of select="@uri"/>
          </xsl:attribute>
          <xsl:apply-templates select="*|text()"/>
        </a>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="ref-by-id">
    <xsl:param name="id"/>
    <xsl:param name="name"/>
    <a>
      <xsl:attribute name="href">#<xsl:value-of select="$id"/></xsl:attribute>
      <xsl:choose>
        <xsl:when test="text()">
          <xsl:apply-templates select="*|text()"/>
        </xsl:when>
        <xsl:otherwise>
          <code>
            <xsl:value-of select="$name"/>
          </code>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template match="radl:location">
    <xsl:choose>
      <xsl:when test="../@id = /radl:service/@home-resource">
        <h4>Location</h4>
        <p> Reach this resource at <code><xsl:value-of select="@href"/></code>. </p>
      </xsl:when>
      <xsl:when test="@template and radl:var[@uri-parameter-ref]">
        <h4>URI Parameters</h4>
        <table>
          <tr>
            <th>Name</th>
            <th>Description</th>
          </tr>
          <xsl:for-each select="radl:var">
            <xsl:sort select="@name"/>
            <xsl:variable name="id" select="@uri-parameter-ref"/>
            <tr>
              <td>
                <code>
                  <xsl:value-of select="@name"/>
                </code>
              </td>
              <td>
                <xsl:apply-templates
                  select="//radl:uri-parameters/radl:uri-parameter[@id = $id]/radl:documentation"/>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:when>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="status-codes">
    <hr/>
    <h1 id="status-codes">Status Codes</h1>

    <xsl:choose>
      <xsl:when test="//radl:conventions/radl:status-codes/radl:status-code">

        <xsl:for-each select="//radl:conventions/radl:status-codes/radl:status-code">
          <xsl:sort select="@name"/>
          <h3>
            <xsl:call-template name="id">
              <xsl:with-param name="prefix">statuscode</xsl:with-param>
              <xsl:with-param name="name" select="@name"/>
            </xsl:call-template>
            <code>
              <xsl:value-of select="@name"/>
            </code>
          </h3>
          <xsl:apply-templates/>
        </xsl:for-each>
      </xsl:when>

      <xsl:otherwise>
        <p>No special use of status codes.</p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="custom-headers">
    <hr/>
    <h1 id="headers">Headers</h1>

    <xsl:choose>
      <xsl:when test="/radl:service/radl:conventions/radl:headers/radl:header">

        <xsl:for-each select="/radl:service/radl:conventions/radl:headers/radl:header">
          <xsl:sort select="@name"/>
          <h2>
            <xsl:call-template name="id">
              <xsl:with-param name="prefix">header</xsl:with-param>
              <xsl:with-param name="name" select="@name"/>
            </xsl:call-template>
            <code><xsl:value-of select="@name"/></code>&#160;&#160;<span class="header-suffix"
                >(<xsl:value-of select="@type"/>)</span>
          </h2>
          <xsl:apply-templates/>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <p>No custom headers.</p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template name="media-types">
    <hr/>
    <h1 id="media-types">Media Types</h1>
    <xsl:choose>
      <xsl:when test="//radl:media-types/radl:media-type">
        <table sortable="true">
          <thead>
            <tr>
              <th>Media Type</th>
              <th>Documentation</th>
            </tr>
          </thead>
          <tbody>
            <xsl:for-each select="//radl:media-types/radl:media-type">
              <xsl:sort select="@name"/>
              <tr>
                <td>
                  <code>
                    <xsl:choose>
                      <xsl:when test="radl:representation">
                        <xsl:call-template name="a-href">
                          <xsl:with-param name="prefix">mediatype</xsl:with-param>
                          <xsl:with-param name="name" select="@name"/>
                        </xsl:call-template>
                      </xsl:when>
                      <xsl:otherwise>
                        <xsl:value-of select="@name"/>
                      </xsl:otherwise>
                    </xsl:choose>
                  </code>
                </td>
                <td>
                  <xsl:apply-templates select="radl:documentation"/>
                  <xsl:apply-templates select="radl:specification"/>
                </td>
              </tr>
            </xsl:for-each>
          </tbody>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <h3>*** TODO ***</h3>
      </xsl:otherwise>
    </xsl:choose>

    <!--
        Provide detail on a media type only if it contains representations.
    -->
    <xsl:for-each select="//radl:media-types/radl:media-type[radl:representation]">
      <xsl:sort select="@name"/>
      <xsl:variable name="mediatype-name" select="@name"/>

      <h2>
        <xsl:call-template name="id">
          <xsl:with-param name="prefix">mediatype</xsl:with-param>
          <xsl:with-param name="name" select="$mediatype-name"/>
        </xsl:call-template>

        <xsl:text>Media Type: </xsl:text>
        <xsl:value-of select="$mediatype-name"/>
      </h2>

      <h3>Representations</h3>
      <table sortable="true">
        <thead>
          <tr>
            <th>Representation</th>
          </tr>
        </thead>
        <tbody>
          <xsl:for-each select="radl:representation">
            <tr>
              <td>
                <xsl:call-template name="a-href">
                  <xsl:with-param name="prefix">representation</xsl:with-param>
                  <xsl:with-param name="scope" select="$mediatype-name"/>
                  <xsl:with-param name="name" select="@name"/>
                </xsl:call-template>
              </td>
            </tr>
          </xsl:for-each>
        </tbody>
      </table>

      <xsl:for-each select="radl:representation">
        <h3>
          <xsl:call-template name="id">
            <xsl:with-param name="prefix">representation</xsl:with-param>
            <xsl:with-param name="scope" select="$mediatype-name"/>
            <xsl:with-param name="name" select="@name"/>
          </xsl:call-template>

          <xsl:text>Representation: </xsl:text>
          <xsl:value-of select="@name"/>
        </h3>

        <xsl:apply-templates select="radl:documentation"/>

        <xsl:for-each select="radl:examples/radl:example">
          <h4>Example: </h4>
          <xsl:apply-templates select="radl:documentation"/>
          <pre><xsl:value-of select="text()"/></pre>
        </xsl:for-each>

      </xsl:for-each>

    </xsl:for-each>

  </xsl:template>

  <xsl:template match="radl:description">
    <a>
      <xsl:attribute name="href">
        <xsl:value-of select="@href"/>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="@type = 'xsd'">
          <xsl:text>XML Schema</xsl:text>
        </xsl:when>
        <xsl:when test="@type = 'rnc'">
          <xsl:text>RELAX-NG Schema (Compact)</xsl:text>
        </xsl:when>
        <xsl:when test="@type = 'rng'">
          <xsl:text>RELAX-NG Schema</xsl:text>
        </xsl:when>
        <xsl:when test="@type = 'dtd'">
          <xsl:text>Document Type Definition</xsl:text>
        </xsl:when>
        <xsl:when test="@type = 'JSONSchema'">
          <xsl:text>JSON Schema</xsl:text>
        </xsl:when>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template match="radl:specification">
    <a>
      <xsl:attribute name="href">
        <xsl:value-of select="@href"/>
      </xsl:attribute>
      <xsl:choose>
        <xsl:when test="text()">
          <xsl:value-of select="text()"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:text>Specification</xsl:text>
        </xsl:otherwise>
      </xsl:choose>
    </a>
  </xsl:template>

  <xsl:template name="link-relations">
    <hr/>
    <h1 id="link-relations">Link Relations</h1>
    <xsl:choose>
      <xsl:when test="//radl:link-relations/radl:link-relation">
        <table>
          <tr>
            <th>Link relation</th>
            <th>Transitions</th>
            <th>Description</th>
          </tr>

          <xsl:for-each select="//radl:link-relations/radl:link-relation">
            <xsl:sort select="@name"/>
            <tr>
              <xsl:call-template name="id">
                <xsl:with-param name="prefix">linkrel</xsl:with-param>
                <xsl:with-param name="name" select="@name"/>
              </xsl:call-template>
              <td>
                <code>
                  <xsl:value-of select="@name"/>
                </code>
              </td>
              <td>
                <xsl:if test="radl:transitions">
                  <xsl:choose>
                    <xsl:when test="@name='self'">
                      <p>(Transitions are not listed for the <code>self</code> link relation.)</p>
                    </xsl:when>
                    <xsl:otherwise>
                      <ul>
                        <xsl:for-each select="radl:transitions/radl:transition">
                          <xsl:sort select="@name"/>
                          <xsl:variable name="transition-name" select="@ref"/>
                          <xsl:for-each
                            select="//radl:states/radl:state[radl:transitions/radl:transition[@name=$transition-name]]">
                            <xsl:sort select="@name"/>
                            <li>
                              <xsl:call-template name="a-href">
                                <xsl:with-param name="prefix">transition</xsl:with-param>
                                <xsl:with-param name="scope" select="@name"/>
                                <xsl:with-param name="name" select="$transition-name"/>
                              </xsl:call-template>
                              <xsl:text> : </xsl:text>
                              <xsl:call-template name="a-href">
                                <xsl:with-param name="prefix">state</xsl:with-param>
                                <xsl:with-param name="name" select="@name"/>
                              </xsl:call-template>
                              <xsl:variable name="previous" select="@name"/>
                              <xsl:for-each
                                select="radl:transitions/radl:transition[@name=$transition-name]/@to">
                                <xsl:choose>
                                  <xsl:when test="position() = 1">
                                    <xsl:text> &#8594; </xsl:text>
                                  </xsl:when>
                                  <xsl:otherwise>
                                    <xsl:text>, </xsl:text>
                                  </xsl:otherwise>
                                </xsl:choose>
                                <xsl:call-template name="a-href">
                                  <xsl:with-param name="prefix">state</xsl:with-param>
                                  <xsl:with-param name="name" select="."/>
                                </xsl:call-template>
                              </xsl:for-each>
                            </li>
                          </xsl:for-each>
                        </xsl:for-each>
                      </ul>
                    </xsl:otherwise>
                  </xsl:choose>
                </xsl:if>
              </td>
              <td>
                <xsl:if test="radl:specification">
                  Specification: <a>
                    <xsl:copy-of select="radl:specification/@href"/>
                    <xsl:value-of select="radl:specification/@href"/>
                  </a>.
                </xsl:if>
                <xsl:apply-templates select="radl:documentation"/>
              </td>
            </tr>
          </xsl:for-each>
        </table>
      </xsl:when>
      <xsl:otherwise>
        <h3>*** TODO ***</h3>
      </xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template match="radl:property-group[exists(@ref)]">
    <p>
      <xsl:text>Property Group: </xsl:text>
      <xsl:value-of select="@name"/> ( see <xsl:call-template name="a-href">
        <xsl:with-param name="prefix">propertygroup</xsl:with-param>
        <xsl:with-param name="name" select="@ref"/>
      </xsl:call-template> ) </p>
  </xsl:template>

  <xsl:template match="radl:property-group[not(@ref)]">
    <h2>
      <xsl:call-template name="id">
        <xsl:with-param name="prefix">propertygroup</xsl:with-param>
        <xsl:with-param name="name" select="@name"/>
      </xsl:call-template>
      <xsl:text>Property Group: </xsl:text>
      <xsl:value-of select="@name"/>

      <xsl:if test="@uri"> (<a>
          <xsl:attribute name="href">
            <xsl:value-of select="@uri"/>
          </xsl:attribute>
          <code>
            <xsl:value-of select="@uri"/>
          </code></a> ) 
      </xsl:if>
    </h2>

      <xsl:if test="radl:documentation">
        <dd>
          <xsl:apply-templates select="radl:documentation"/>
        </dd>
      </xsl:if>
      <xsl:if test="radl:property | radl:property-group">
        <dl>
          <xsl:apply-templates select="radl:property | radl:property-group"/>
        </dl>
      </xsl:if>
    
  </xsl:template>

  <xsl:template match="radl:property">
    <p>
      <xsl:value-of select="@name"/>
      <xsl:if test="@uri"> (<a>
          <xsl:attribute name="href">
            <xsl:value-of select="@uri"/>
          </xsl:attribute>
          <code>
            <xsl:value-of select="@uri"/>
          </code>
        </a> ) </xsl:if>
      <xsl:if test="@type">
        <xsl:text> - </xsl:text>
        <xsl:value-of select="@type"/>
      </xsl:if>
    </p>
    
    <xsl:if test="radl:documentation">
      <dd>
        <xsl:apply-templates select="radl:documentation"/>
      </dd>
    </xsl:if>

  </xsl:template>

  <xsl:template name="property-groups">
    <hr/>
    <h1 id="property-groups">Property Groups</h1>
    <xsl:choose>
      <xsl:when test="//radl:property-groups">

        <xsl:apply-templates select="//radl:property-groups/radl:property-group">
          <xsl:sort select="@name"/>
        </xsl:apply-templates>
      </xsl:when>
      <xsl:otherwise>No property groups</xsl:otherwise>
    </xsl:choose>

  </xsl:template>

  <xsl:template name="uri-parameters">
    <hr/>
    <h1 id="uri-parameters">URI Parameters</h1>

    <xsl:choose>
      <xsl:when test="/radl:service/radl:conventions//radl:uri-parameters/radl:uri-parameter">

        <xsl:for-each
          select="/radl:service/radl:conventions//radl:uri-parameters/radl:uri-parameter[not(@ref)]">
          <xsl:sort select="@name"/>
          <h2>
            <xsl:call-template name="id">
              <xsl:with-param name="prefix">uriparameter</xsl:with-param>
              <xsl:with-param name="name" select="@name"/>
            </xsl:call-template>

            <code>
              <xsl:value-of select="@name"/>
            </code>
          </h2>
          <xsl:choose>
            <xsl:when test="@datatype | radl:value-range | radl:default | radl:documentation">
              <dl>
                <xsl:if test="@datatype">
                  <dt>Datatype</dt>
                  <dd>
                    <code>
                      <xsl:value-of select="@datatype"/>
                    </code>
                  </dd>
                </xsl:if>
                <xsl:if test="radl:value-range">
                  <dt>Values</dt>
                  <dd>
                    <code>
                      <xsl:value-of select="radl:value-range"/>
                    </code>
                  </dd>
                </xsl:if>
                <xsl:if test="radl:default">
                  <dt>Default Value</dt>
                  <dd>
                    <code>
                      <xsl:value-of select="radl:default"/>
                    </code>
                  </dd>
                </xsl:if>
                <xsl:if test="radl:documentation">
                  <dt>Documentation</dt>
                  <dd>
                    <xsl:apply-templates select="radl:documentation"/>
                  </dd>
                </xsl:if>
              </dl>
            </xsl:when>
          </xsl:choose>
        </xsl:for-each>
      </xsl:when>
      <xsl:otherwise>
        <p>No URI parameters.</p>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="radl:properties">
    <xsl:if test="radl:property">
      <h3>Properties</h3>
      <table>
        <tr>
          <th>Property</th>
          <th>Value</th>
          <th>Description</th>
        </tr>
        <xsl:for-each select="radl:property">
          <xsl:sort select="@name"/>
          <tr>
            <td>
              <code>
                <xsl:value-of select="@name"/>
              </code>
            </td>
            <td>
              <code>
                <xsl:value-of select="text()"/>
              </code>
            </td>
            <td>
              <xsl:apply-templates select="radl:documentation"/>
            </td>
          </tr>
        </xsl:for-each>
      </table>
    </xsl:if>
  </xsl:template>

  <xsl:template name="a-href">
    <xsl:param name="prefix"/>
    <xsl:param name="scope" select="()"/>
    <xsl:param name="name"/>
    <a>
      <xsl:call-template name="href">
        <xsl:with-param name="prefix" select="$prefix"/>
        <xsl:with-param name="scope" select="$scope"/>
        <xsl:with-param name="name" select="$name"/>
      </xsl:call-template>
      <xsl:value-of select="$name"/>
    </a>
  </xsl:template>

  <xsl:template name="href">
    <xsl:param name="prefix"/>
    <xsl:param name="scope" select="()"/>
    <xsl:param name="name"/>

    <xsl:attribute name="href">
      <xsl:choose>
        <xsl:when test="$scope">
          <!-- Replace spaces, :, +, or / (the latter two occur in media type names) -->
          <xsl:value-of
            select="concat('#', $prefix, '-', replace($scope,' |/|\+|:','-'), '-', replace($name, ' ', '-'))"
          />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('#', $prefix, '-', replace($name, ' |/|\+|:', '-'))"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="id">
    <xsl:param name="prefix"/>
    <xsl:param name="scope" select="()"/>
    <xsl:param name="name"/>
    <xsl:attribute name="id">
      <xsl:choose>
        <xsl:when test="$scope">
          <!-- Replace spaces, :, +, or / (the latter two occur in media type names) -->
          <xsl:value-of
            select="concat($prefix, '-', replace($scope, ' |/|\+|:', '-'), '-', replace($name, ' ', '-'))"
          />
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat($prefix, '-', replace($name, ' |/|\+|:', '-'))"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>

  <xsl:template name="end-states-from-transitions" as="xs:string*">
    <xsl:param name="transitions" as="element()*"/>
    <xsl:variable name="end-states" as="xs:string*"
      select="
    for $transition in $transitions,
        $candidate-states in
           if ($transition/@from) then
              if ($transition/@from = $start-state-name)
              then //radl:start-state
              else //radl:state[@name=$transition/@from]
           else    //radl:start-state|//radl:state
    return $candidate-states/radl:transitions/radl:transition[@name=$transition/@ref]/string(@to)
    "/>
    <xsl:sequence select="distinct-values($end-states)"/>
  </xsl:template>

  <xsl:template match="*">
    <div class="buggy">
      <p>#### Not processed: element <xsl:value-of select="local-name(.)"/> in namespace
          <xsl:value-of select="namespace-uri()"/></p>
      <xsl:copy>
        <xsl:apply-templates/>
      </xsl:copy>
    </div>
  </xsl:template>

</xsl:stylesheet>
