<html>
<head>
  <title>eXo Platform JCR browser sample ERROR</title>
	<link rel="stylesheet" href="../exojcrstyle.css">
</head>
<body>
  <h1>eXo Platform JCR browser sample</h1>
  <h2>Repository accessing error: [<%= (String) request.getSession().getAttribute("msg")%>]</h2>
  <form action="../admin/index.jsp" method="post"><input name="submit" type="submit" value="Administration"></form>
</body>  
</html>
