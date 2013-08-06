<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isELIgnored="false"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<jsp:useBean id="browser" scope="session" class="org.exoplatform.applications.jcr.browser.JCRBrowser"></jsp:useBean>

<html>
<head>
<title>eXo Platform JCR browser sample</title>
<link rel="stylesheet" href="../exojcrstyle.css">
<meta content="text/html; charset=UTF-8" http-equiv="content-type">
</head>

<body>
	<h1>eXo Platform JCR browser sample</h1>
	<form method="link" action="../logout.jsp">
    	<input type="hidden" name="ap" value="user"/>
    	<input type="submit" value="Logout"/>
	</form>
	<c:choose>
		<c:when test="${browser.errorsFound}">
 		  <%-- show errors only --%>
			<div id="browserErrors" class="errors">
			  <p class="errorHeader">Error(s) occurs during the browser work. Check log/console for details.</p>
		    <c:forEach var="err" items="${browser.errorsAndClean}">
		      <c:out value="${err}" escapeXml="true"/><br/>
		    </c:forEach>
		    <span class="errorBack"><a href="${request.contextPath}">Back</a></span>
		  </div>
		</c:when>
		
	  <c:otherwise>
	    <%-- normal work --%>
	    
			<form action="../admin/index.jsp" method="post" name="admin">
			<h2 class="info">Repository:&nbsp;<span class="infoText" />
			  <c:out value="${browser.repository.configuration.name}"/></span></h2>
			<h2 class="info">Workspace:&nbsp;<span class="infoText">
			  <c:out value="${browser.session.workspace.name}" />
			  <input type="submit" name="submit" value="select"></span></h2>
			</form>
		
			<c:choose>
				<c:when test="${browser.node.path != '/'}">
					<form action="." method="post" name="location" name="GoParent">
					<h2 class="info">Path:&nbsp; <span class="infoText">${browser.node.path}&nbsp;<input
						type="submit" name="goParent" value=".."></span></h2>
					</form>
				</c:when>
				<c:otherwise>
					<h2>Path:&nbsp;<span class="infoText">${browser.node.path}</span></h2>
				</c:otherwise>
			</c:choose>
		
			<h2>Childrens:</h2>
			<hr width="25%" align=left>
			<h2 class="child">Properties:</h2>
		
			<table cellpadding="0" cellspacing="0">
				<c:forEach var="prop" items="${browser.node.properties}">
					<tr>
						<td class="key">${prop.name}</td>
						<td class="value">
						
						<c:catch var="perror">
              <span>${prop.string}</span>
            </c:catch>
						
						<c:if test="${perror != null}">
							<%-- seems, multivalued property--%>
							<c:forEach var="pvalue" items="${prop.values}">
								<c:catch var="perror">
                  <span>${pvalue.string},</span>
                </c:catch>
				      </c:forEach>
				      <c:remove var="perror" />
			      </c:if>
			      
						</td>
					</tr>
				</c:forEach>
				<c:if test="${perror != null}">
					<tr>
						<td><c:out value="${perror.message}" /></td>
					</tr>
				</c:if>
			</table>
		
			<hr width="25%" align=left>
			<h2 class="child">Nodes:</h2>
		
			<table cellpadding="0" cellspacing="0">
				<c:set value="" var="nerror" />
				<c:catch var="nerror">
					<c:forEach var="node" items="${browser.node.nodes}">
						<tr>
							<td>
								<form action="." method="post" name="GoNode_${node.name}">
								  <input type="hidden" name="goNodePath" value="${node.path}">
								  <input type="submit" name="submit" value="${node.name}">
								</form>
							</td>
						</tr>
					</c:forEach>
				</c:catch>
			</table>
			
    </c:otherwise>
  </c:choose>

</body>
</html>
