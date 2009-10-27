<%String loginAction = request.getContextPath() + "/j_security_check";%>
<html>
<head>
  <title>eXo Platform JCR browser sample LOGIN</title>
	<link rel="stylesheet" href="../exojcrstyle.css">
</head>
<body>
  <h1>eXo Platform JCR browser sample</h1>
  <form name="loginForm" method="post" action="<%=loginAction%>">
    Enter your login and password to view requested content: <input name="j_username" value="root"/>
    <input type="password" name="j_password" value="exo"/>
    <input name="submit" type="submit" value="Login">
  </form>
</body>  
</html>

