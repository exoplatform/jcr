<%
String pathPrefix = request.getParameter("ap");
if (pathPrefix == null)
{
	pathPrefix = "";
}
else
{
	pathPrefix = pathPrefix + "/";
}
session.invalidate();
response.sendRedirect(pathPrefix + "index.jsp");
%>

