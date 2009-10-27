<html>
<head>
  <title>eXo Platform JCR browser sample REPOSITORY NOT FOUND</title>
	<link rel="stylesheet" href="../exojcrstyle.css">
</head>
<body>
  <h1>eXo Platform JCR browser sample</h1>
  <h2>Requested repository [<%= (String) request.getSession().getAttribute("repe")%>] not found</h2>
  <form action="." method="post"><input name="submit" type="submit" value="OK"></form>
</body>  
</html>

