<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ page isELIgnored="false"%>

<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
  <title>eXo Platform JCR browser sample SELECT REPOSITORY</title>
  <link rel="stylesheet" href="../exojcrstyle.css">

  <script type="text/javascript">
    <!--
    var delay = 10000;
    var intervalID;
    function initAreplicationValidation() {
      if (intervalID) {
        clearInterval("validateAreplication");
      }     
      intervalID = setInterval("validateAreplication()", delay);
    }
    
    var req;
    function initRequest(url) {
      if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
      } else if (window.ActiveXObject) {
        isIE = true;
        req = new ActiveXObject("Microsoft.XMLHTTP");
      }
      //req.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
    }
    
    function validateAreplication() {
      var url = "arepl.jsp?status";
      initRequest(url);
      req.onreadystatechange = function() {
        var done = 4, ok = 200;
        if (req.readyState == done && req.status == ok) {
          if (req.responseXML) {
            var status = req.responseXML.getElementsByTagName("active")[0].childNodes[0].nodeValue;
            var sform = document.getElementById("synchronizationForm");
            if (status == "true") {
              sform.innerHTML = "<form><input name=\"submit\" type=\"submit\" value=\"synchronizing\" disabled=\"disabled\"></form>";    
            } else {
              sform.innerHTML = "<form><input name=\"submit\" type=\"submit\" value=\"synchronize\" onclick=\"runAreplication()\"></form>";  
            }
          }
        }
      };
      req.open("GET", url, true);
      req.send(null);
    }

    function runAreplication() {
      var url = "index?synchronize=run";
      initRequest(url);
      req.open("POST", url, false);
      req.send(null);

      var sform = document.getElementById("synchronizationForm");
      sform.innerHTML = "<form><input name=\"submit\" type=\"submit\" value=\"synchronizing\" disabled=\"disabled\"></form>";
      initAreplicationValidation();
    }
    // -->
  </script>
</head>

<jsp:useBean id="browser" scope="session" class="org.exoplatform.applications.jcr.browser.JCRBrowser"></jsp:useBean>

<body>

  <h1>eXo Platform JCR browser sample</h1>
  <form method="link" action="../logout.jsp">
    <input type="hidden" name="ap" value="admin"/>
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
	  <h2 class="info">Selected:</h2>
	  <h2 class="info">Repository:&nbsp;
	  	<span class="infoText"><c:out value="${browser.repository.configuration.name}"/>
          <form method="post" name="repository" action="repository.jsp" >
            <input name="submit" type="submit" value="change">
          </form>
          <span id="synchronizationForm">
            <c:choose>
              <c:when test="${browser.asynchronousReplicationPresent}">
                <c:choose>
                  <c:when test="${browser.asynchronousReplicationActive}">
                    <form>
                      <input name="submit" type="submit" value="synchronizing" disabled="disabled">
                    </form>
                  </c:when>
                  <c:otherwise>
                    <form>
                      <input name="submit" type="submit" value="synchronize" onclick="runAreplication()">
                    </form>
                  </c:otherwise>
                </c:choose>
              </c:when>
            </c:choose>
          </span>
        </span>		
	  </h2>
	  <h2 class="info">Workspace:&nbsp;
	  <span class="infoText"><c:out value="${browser.session.workspace.name}"/>
		  <form action="workspace.jsp" method="post" name="workspace">
		    <input name="submit" type="submit" value="change">
		  </form>
	  </span>
	  </h2><h2>&nbsp;
	  <span class="infoText">
		  <form action="../user/index.jsp" method="post" name="browse">
		    <input name="submit" type="submit" value="browse!">
		  </form>
	  </span>
	  </h2>
    </c:otherwise>
  </c:choose>
  
</body>  
</html>

