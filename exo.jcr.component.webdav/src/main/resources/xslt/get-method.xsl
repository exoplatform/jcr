<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:sv="http://www.jcp.org/jcr/sv/1.0" xmlns:xlink="http://www.w3.org/1999/xlink"
  exclude-result-prefixes="xlink">
  <xsl:output method="html" encoding="UTF-8" />
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
                <xsl:value-of select="substring(./@xlink:href, 1, string-length(./@xlink:href) - string-length(./@sv:name))" />
              </xsl:attribute>
              <img src="/ecm/skin/icons/16x16/NodeTypes/DefaultSkin/nt-folder.gif" alt="" />
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
      <img src="/ecm/skin/icons/16x16/NodeTypes/DefaultSkin/nt-folder.gif" alt="" />
      <xsl:text> </xsl:text>
      <xsl:value-of select="./@sv:name" />
    </a>
    <br />
  </xsl:template>
  <!--
    <xsl:template match="sv:property"> <img src="/ecm/skin/icons/16x16/NodeTypes/DefaultSkin/nt-file.gif" alt=""/> <xsl:text> </xsl:text> <xsl:value-of select="./@sv:name"/><br/> </xsl:template>
  -->
</xsl:stylesheet>
