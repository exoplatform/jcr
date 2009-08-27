<?xml version='1.0' encoding='ISO-8859-1'?>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isELIgnored="false"%>

<%@ page language="java" contentType="text/xml; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>

<jsp:useBean id="browser" scope="session" class="org.exoplatform.applications.jcr.browser.JCRBrowser"></jsp:useBean>
<areplication>
  <c:choose>
    <c:when test="${browser.asynchronousReplicationPresent}">
      <active><c:out value="${browser.asynchronousReplicationActive}"/></active>
    </c:when>
    <c:otherwise>
      <active><c:out value="${browser.asynchronousReplicationPresent}"/></active> 
    </c:otherwise>
  </c:choose>    
  <description>eXo Platform JCR Asynchronous replication state</description>
</areplication>
