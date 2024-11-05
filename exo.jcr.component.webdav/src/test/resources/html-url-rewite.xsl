<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:html="http://www.w3.org/1999/xhtml" exclude-result-prefixes="html">
	<xsl:output method="xml" omit-xml-declaration="no" encoding="UTF-8"/>

	<!-- URI of current PORTAL page -->
	<xsl:param name="portalURI"/>
	<!-- URI of current PORTLET page -->
	<xsl:param name="portletURI"/>
	<!-- name of GET param-->
	<xsl:param name="param-name">
		<xsl:text>url</xsl:text>
	</xsl:param>
	<!-- ContextPath of portal-->
	<xsl:param name="portalContextPath"/>
	<!-- QueryString of portal-->
	<xsl:param name="portalQueryString"/>
	<!-- prefix of param names-->
	<xsl:param name="paramNamespace"/>

	<!-- drop html, head, body tags -->
	<xsl:template match="html:html | html:body">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="html:head">
		<xsl:comment>
			<xsl:text>Parameter list: </xsl:text>
			<xsl:text>portalURI=[</xsl:text>
			<xsl:value-of select="$portalURI"/>
			<xsl:text>] portletURI=[</xsl:text>
			<xsl:value-of select="$portletURI"/>
			<xsl:text>] param-name=[</xsl:text>
			<xsl:value-of select="$param-name"/>
			<xsl:text>] portalContextPath =[</xsl:text>
			<xsl:value-of select="$portalContextPath"/>
			<xsl:text>] portalQueryString =[</xsl:text>
			<xsl:value-of select="$portalQueryString "/>
			<xsl:text>] paramNamespace =[</xsl:text>
			<xsl:value-of select="$paramNamespace"/>
			<xsl:text>]</xsl:text>
		</xsl:comment>
	</xsl:template>
	<xsl:template match="@*|node()">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()"/>
		</xsl:copy>
	</xsl:template>
	<xsl:template match="html:a[@href] | a[@href]">
		<xsl:element name="a" namespace="http://www.w3.org/1999/xhtml">
			<xsl:attribute name="href"><xsl:call-template name="rewrite-url"><xsl:with-param name="url" select="@href"/></xsl:call-template></xsl:attribute>
			<xsl:copy-of select="*"/>
			<xsl:value-of select="text()"/>
		</xsl:element>
	</xsl:template>
	
	<xsl:template match="html:form | form">
		<xsl:element name="form" namespace="http://www.w3.org/1999/xhtml">			
			<xsl:copy-of select="@*"/>
	
			<xsl:variable name="new-url">				
				<xsl:call-template name="rewrite-url">
					<xsl:with-param name="url" select="@action"/>
				</xsl:call-template>				
			</xsl:variable>
			
			<xsl:attribute name="action">
				<xsl:value-of select="substring-before($new-url,'?')"/>
			</xsl:attribute>
			<xsl:comment>*begin**********Auto added fields*********[new url= <xsl:value-of select="$new-url"/></xsl:comment>
 				<xsl:call-template name="query-string-as-hidden-fields">
 					<xsl:with-param name="params" select="concat(substring-after($new-url,'?'),'&amp;')"/>
 				</xsl:call-template>
			<xsl:comment>*end**********Auto added fields*********</xsl:comment>

			
			<xsl:apply-templates/>
			<xsl:value-of select="text()"/>
		</xsl:element>
	</xsl:template>
	
	
	<!-- when form we must use <input> to pass params</input>-->
	<xsl:template name="query-string-as-hidden-fields">
		<xsl:param name="params"/>
		<xsl:variable name="curr-param" select="substring-before($params,'&amp;')"/>
 		<xsl:if test="$curr-param">
 			<xsl:element name="input" namespace="http://www.w3.org/1999/xhtml">
 				<xsl:attribute name="type"><xsl:text>hidden</xsl:text></xsl:attribute>
 				<xsl:attribute name="name">
 					<xsl:value-of select="substring-before($curr-param,'=')"/> 					
 				</xsl:attribute> 				
 				<xsl:attribute name="value">
 					<xsl:value-of select="substring-after($curr-param,'=')"/> 					
 				</xsl:attribute>
 				<xsl:call-template name="query-string-as-hidden-fields">
 					<xsl:with-param name="params" select="substring-after($params,'&amp;')"/>
 				</xsl:call-template>
 			</xsl:element>
 		</xsl:if>
	</xsl:template>

	
	<xsl:template name="rewrite-url">
		<xsl:param name="url"/>
		<xsl:choose>
			<!-- ignore absolute, mailto, news, javascript URLs -->
			<xsl:when test="contains($url, '://') or starts-with($url, 'mailto:') or starts-with($url, 'news:') or starts-with($url, 'javascript:')
			or starts-with($url, '#')">
				<xsl:value-of select="$url"/>
			</xsl:when>
			<!-- replace url -->
			<xsl:otherwise>
				<xsl:variable name="new-url-0" select="translate($url, '?','&amp;')"/>
				<!-- remove portalContextPath from beginning of url -->
				<xsl:variable name="new-url-1">
					<xsl:choose>
						<xsl:when test="starts-with($new-url-0,$portalContextPath)">
							<xsl:value-of select="substring-after($new-url-0,$portalContextPath)"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$new-url-0"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<!-- transform relative portlet path to absolute -->
				<xsl:variable name="new-url-2">
					<xsl:if test="not(starts-with($new-url-1,'/'))">
						<xsl:value-of select="$portletURI"/>
						<xsl:text>/</xsl:text>
					</xsl:if>
					<xsl:value-of select="$new-url-1"/>
				</xsl:variable>
				<xsl:value-of select="$portalURI"/>
				<xsl:text>?</xsl:text>
				<xsl:value-of select="$portalQueryString"/>
				<xsl:if test="$portalQueryString">
					<xsl:text>&amp;</xsl:text>
				</xsl:if>
				<!--						<xsl:choose>				
							<xsl:when test="contains($portalURI,'?')">	
								<xsl:text>&amp;</xsl:text>
							</xsl:when>
							<xsl:otherwise>
								<xsl:text>?</xsl:text>
							</xsl:otherwise>
						</xsl:choose>
						-->
				<xsl:value-of select="$paramNamespace"/>
				<xsl:value-of select="$param-name"/>
				<xsl:text>=</xsl:text>
				<!--						<xsl:value-of select="$link-prefix"/>-->
				<xsl:value-of select="$new-url-2"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
