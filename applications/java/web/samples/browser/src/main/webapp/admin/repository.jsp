<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isELIgnored="false"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>eXo Platform JCR browser sample SELECT REPOSITORY</title>
<link rel="stylesheet" href="../exojcrstyle.css">
</head>

<jsp:useBean id="browser" scope="session" class="org.exoplatform.applications.jcr.browser.JCRBrowser"></jsp:useBean>

<body>

<h1>eXo Platform JCR browser sample.</h1>

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
      
	    Change repository name.<br/> 
      It's may be repository name given in configuration or registered in JNDI<br/> <br/> 
		  
		  <form name="repository" action="." method="post">  
			  Predefined set  
		    <select name="reposList" onchange="document.repository.repositoryName.value=this.value">
		       <optgroup label="Current">
		         <option selected="selected" label="${browser.repository.configuration.name}" value="${browser.repository.configuration.name}">${browser.repository.configuration.name}</option>
		       </optgroup>
					 <optgroup label="Local Repositories (configuration)">
			       <c:forEach var="rconfig" items="${browser.repositoryService.config.repositoryConfigurations}">
			         <option label="${rconfig.name}" value='${rconfig.name}'>${rconfig.name}</option>
			       </c:forEach>
			     </optgroup>
			     <optgroup label="Remote Repositories (JNDI lookup)">
			       <option label="rmirepository" value='rmirepository'>rmirepository</option>
			     </optgroup>
		    </select>
	      name  
		    <input name="repositoryName" type="text" size="30" maxlength="30" value="repository"/>
		    <input name="submit" type="submit" value="ok"/>
		  </form>
		    
    </c:otherwise>
  </c:choose>

</body>
</html>