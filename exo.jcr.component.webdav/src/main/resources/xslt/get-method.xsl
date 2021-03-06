<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sv="http://www.jcp.org/jcr/sv/1.0" xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink">
  <xsl:output method="html" encoding="UTF-8" />
  <xsl:param name="folder-icon-path"></xsl:param>
  <xsl:param name="file-icon-path"></xsl:param>
  <xsl:template match="/sv:node">
    <html>
      <head>
        <title>WEBDAV Browser</title>
        <style type="text/css">
          <xsl:comment>
            a{text-decoration: none}
            img{border: none}
            #main{
            font-family: TimesNewRoman, Arial, Helvetica, serif;
            font-style: normal;
            font-size: 10pt;
            padding-left: 20px;
            }
          </xsl:comment>
        </style>
      </head>
      <body>
        <div id="main">
          <xsl:if test="./@sv:name!=''">
            <!-- Parent node link -->
            <a>
              <xsl:attribute name="href">
                <xsl:value-of select="./@xlink:parent-href" />
              </xsl:attribute>
              <xsl:if test="$folder-icon-path!=''">
                <img src="{$folder-icon-path}" alt="" />
              </xsl:if>
              <xsl:text> ..</xsl:text>
            </a>
            <br />
          </xsl:if>
          <!-- nodes -->
          <xsl:apply-templates select="sv:node">
            <xsl:sort order="ascending" select="./@sv:name" />
          </xsl:apply-templates>
          <!-- properties -->
          <!--
          <xsl:apply-templates select="sv:property">
            <xsl:sort order="ascending" select="./@sv:name"/>
          </xsl:apply-templates>
          -->
        </div>
      </body>
    </html>
  </xsl:template>

  <xsl:template match="sv:node">
    <a>
      <xsl:attribute name="href">
        <xsl:value-of select="./@xlink:href" />
      </xsl:attribute>
      <xsl:choose>
      <xsl:when test="./@sv:isFile='true'">
        <xsl:if test="$file-icon-path!=''">
          <img src="{$file-icon-path}" alt="" />
        </xsl:if>
      </xsl:when>
      <xsl:otherwise>
        <xsl:if test="$folder-icon-path!=''">
          <img src="{$folder-icon-path}" alt="" />
        </xsl:if>
      </xsl:otherwise>
      </xsl:choose>
      <xsl:text> </xsl:text>
      <xsl:value-of select="./@sv:name" />
    </a>
    <br />
  </xsl:template>
  <!--
    <xsl:template match="sv:property"> <img src="/ecm/skin/icons/16x16/NodeTypes/DefaultSkin/nt-file.gif" alt=""/> <xsl:text> </xsl:text> <xsl:value-of select="./@sv:name"/><br/> </xsl:template>
  -->
</xsl:stylesheet>
