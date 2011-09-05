<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>
<%@ page isELIgnored="false"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ page import='javax.jcr.Repository' %>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>eXo Platform JCR browser sample</title>
	<link rel="stylesheet" href="../exojcrstyle.css">
</head>

<jsp:useBean id="browser" scope="session" class="org.exoplatform.applications.jcr.browser.JCRBrowser"></jsp:useBean>

<body>
  <h1>eXo Platform JCR browser sample</h1>

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
  
		  <form action="." method="post" name="back">
		  Repository selected: <input disabled="disabled" name="rep0" type="text" maxlength="30" value="${browser.repository.configuration.name}">
		    <input type="submit" name="submit" value="back <--">
		  </form>
		  <hr width="25%" align="left">
		  <form action="." method="post" name="workspace">
		    <h2>
		    Select workspace:
		    <select name="workspaceName">
			    <c:forEach var="wsName" items="${browser.repository.workspaceNames}">
			      <%-- TODO RMI etc --%>
			      <c:choose>
				      <c:when test="${wsName == browser.session.workspace.name}">
				        <option selected="selected">${wsName}</option>
				      </c:when>
				      <c:otherwise>
		            <option>${wsName}</option>
		          </c:otherwise>
	          </c:choose>
			    </c:forEach>
		    </select>
		    <input name="submit" type="submit" value="select">
		    </h2>
		  </form>
		  <hr width="25%" align="left">
		  <table cellpadding="0" cellspacing="0">
		    <tr>
		      <td colspan="2" align="left"><h2>Descriptors</h2></td>
		    </tr>
		    <tr>
		      <td class="key">Name</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.REP_NAME_DESC) %></td>
		    </tr>
		    <tr>
		      <td class="key">Vendor</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.REP_VENDOR_DESC) %></td>
		    </tr>
		    <tr>
		      <td class="key">URL</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.REP_VENDOR_URL_DESC) %></td>
		    </tr>
		    <tr>
		      <td class="key">Repository version</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.REP_VERSION_DESC) %></td>
		    </tr>
		    <tr>
		      <td class="key">Spec name</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.SPEC_NAME_DESC) %></td>
		    </tr>
		    <tr>
		      <td class="key">Spec version</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.SPEC_VERSION_DESC) %></td>
		    </tr>
		    <tr>
		      <td class="key">Level 1 supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.LEVEL_1_SUPPORTED) %></td>
		    </tr>
		    <tr>
		      <td class="key">Level 2 supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.LEVEL_2_SUPPORTED) %></td>
		    </tr>
		    <tr>
		      <td class="key">Observation supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED) %></td>
		    </tr>
		    <tr>
		      <td class="key">Versioning supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.OPTION_VERSIONING_SUPPORTED) %></td>
		    </tr>
		    <tr>
		      <td class="key">Locking supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.OPTION_LOCKING_SUPPORTED) %></td>
		    </tr>
		    <tr>
		      <td class="key">Transactions supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.OPTION_TRANSACTIONS_SUPPORTED) %></td>
		    </tr>      
		    <tr>
		      <td class="key">Query SQL supported</td>
		      <td class="value"><%= browser.getRepository().getDescriptor(Repository.OPTION_QUERY_SQL_SUPPORTED) %></td>
		    </tr>      
		  </table>
		  
		  <hr width="25%" align="left">
		  <table cellpadding="0" cellspacing="0">
		    <tr>
		      <td colspan="2" align="center" class="key"><h2>Configuration</h2></td>
		    </tr>
		    
		    <c:forEach var="wsEntry" items="${browser.repository.configuration.workspaceEntries}">
		      <c:set var="wsParamNames">Container,QueryHandler,Cache,Initializer</c:set>
		      <tr>
	          <td class="key"><c:out value="${wsEntry.name}"/></td>
	          <td>
	            <table cellpadding="0" cellspacing="0" class="subTable">
	              <%-- tr --%>
	                <c:forTokens var="wsParamName" items="${wsParamNames}" delims=",">
	                <%-- c:forEach items="${wsParamNames}" --%>
	                  <c:choose>
	                    <c:when test="${wsParamName == 'Container'}">
	                      <c:set var="wsParamEntry" value="${wsEntry.container}"/>    
	                    </c:when>
	                    <c:when test="${wsParamName == 'Query Handler'}">
                        <c:set var="wsParamEntry" value="${wsEntry.queryHandler}"/>    
                      </c:when>
                      <c:when test="${wsParamName == 'Cache'}">
                        <c:set var="wsParamEntry" value="${wsEntry.cache}"/>    
                      </c:when>
                      <c:when test="${wsParamName == 'Initializer'}">
                        <c:set var="wsParamEntry" value="${wsEntry.initializer}"/>    
                      </c:when>
	                  </c:choose>
	                  <tr>
			                <td width="15%" class="key" rowspan="${fn:length(wsParamEntry.parameters) + 1}">${wsParamName} {${wsParamEntry.type}}</td>
                      <c:forEach var="ep" items="${wsParamEntry.parameters}">
                        <tr>
                          <td width="15%" class="key">${ep.name}</td>
                          <td width="15%" class="value">${ep.value}</td>
                        </tr>
                      </c:forEach>
	                  </tr>
	                </c:forTokens>
              </table>
            </td>
          </tr>
        </c:forEach>
		  </table>
	  
	  </c:otherwise>
  </c:choose>
  
</body>  
</html>
