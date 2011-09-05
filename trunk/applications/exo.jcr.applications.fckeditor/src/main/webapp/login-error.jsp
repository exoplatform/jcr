<%String loginAction = request.getContextPath() + "/j_security_check";%>
<html>
<head>
  <title>eXo Platform JCR FCKeditor sample LOGIN ERROR</title>
	<link rel="stylesheet" href="exojcrstyle.css">
</head>
<body>
  <h1>eXo JCR FCKeditor sample</h1>
  <h2>Login error. Try again:</h2>
  <form name="loginForm" method="post" action="<%=loginAction%>">
    Enter your login and password to view requested content: <input name="j_username" value="admin"/>
    <input type="password" name="j_password" value="admin"/>
    <input name="submit" type="submit" value="Login">
  </form>
</body>  
</html>

