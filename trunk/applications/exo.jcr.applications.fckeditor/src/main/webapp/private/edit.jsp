<%@ page import='java.util.*'%>
<%@ page import='java.io.*'%>
<%@ page import='javax.jcr.*'%>
<%@ page import='org.exoplatform.frameworks.jcr.*'%>
<%@ page import='org.exoplatform.frameworks.jcr.command.*'%>
<%@ page import='org.exoplatform.frameworks.jcr.command.web.*'%>
<%@ page import='org.exoplatform.frameworks.jcr.web.*'%>
<%@ page import='org.apache.commons.chain.*'%>
<%@ page import='org.exoplatform.container.ExoContainer'%>
<%@ page import='org.exoplatform.services.command.impl.*'%>
<%@ page import='org.exoplatform.services.jcr.ext.common.*'%>
<%@ page import='org.exoplatform.services.jcr.ext.app.*'%>
<%@ page import='org.exoplatform.services.jcr.*'%>
<%@ page import='org.exoplatform.services.jcr.config.*'%>
<%@ page import='org.exoplatform.frameworks.jcr.web.fckeditor.*'%>

<%
            String editorName = request.getParameter("editor");
            if (editorName == null) {
                editorName = "FCKeditor1";
            }
            String editor = request.getParameter(editorName);
            String path = request.getParameter("path");
            if (path == null) {
                path = "/content/index";
                
                ExoContainer container = (ExoContainer) application.getAttribute(WebConstants.EXO_CONTAINER); 
                SessionProviderService sessionProviderService =
                        (SessionProviderService) container.getComponentInstanceOfType(SessionProviderService.class);

                RepositoryService repositoryService = (RepositoryService) container.getComponentInstanceOfType(RepositoryService.class);
                GenericWebAppContext ctx;
                try {
                    ctx = new GenericWebAppContext(getServletContext(),
                            request,
                            response,
                            sessionProviderService.getSessionProvider(null), // null for ThreadLocalSessionProvider
                            repositoryService.getDefaultRepository());
                } catch (final RepositoryException e) {
                    throw new IOException() {

                        @Override
                        public Throwable getCause() {
                            return e;
                        }
                    };
                } catch (final RepositoryConfigurationException e) {
                    throw new IOException() {

                        @Override
                        public Throwable getCause() {
                            return e;
                        }
                    };
                }

                CommandService catalog = (CommandService) container.getComponentInstanceOfType(CommandService.class);

                ctx.put(DefaultKeys.WORKSPACE, "production");

                try {
                    Command getNode = catalog.getCatalog().getCommand("getNode");
                    ctx.put("currentNode", "/");
                    ctx.put("path", "content/index");
                    getNode.execute(ctx);
                } catch (PathNotFoundException e) {
                    System.err.println("Test node not found, create one new");
                    // Node not found - create it
                    try {
                        Command addNode = catalog.getCatalog().getCommand("addNode");
                        ctx.put("nodeType", "nt:folder");
                        ctx.put("path", "content");
                        addNode.execute(ctx);

                        Command addResourceFile = catalog.getCatalog().getCommand("addResourceFile");
                        ctx.put("currentNode", "/content");
                        ctx.put("path", "index");
                        ctx.put("data", "NEW Node data");
                        ctx.put("mimeType", "text/html");
                        addResourceFile.execute(ctx);

                        ctx.put("path", "/");
                        Command save = catalog.getCatalog().getCommand("save");
                        save.execute(ctx);
                    } catch (Throwable th) {
                        System.err.println("Error of add node: " + th);
                        th.printStackTrace();
                    }

                } catch (Throwable th) {
                    System.err.println("Error of test node init: " + th);
                    th.printStackTrace();
                }
            }

            String ws = request.getParameter("workspace");

            JCRContentFCKeditor oFCKeditor;
            if (session.getAttribute("ed") == null) {
                oFCKeditor = new JCRContentFCKeditor(request, editorName, ws, path, "nt:file");
                FCKeditorConfigurations conf = new FCKeditorConfigurations();
                conf.put("ImageBrowserURL", "/fckeditor/FCKeditor/editor/filemanager/browser/default/browser.html?Connector=/fckeditor/connector");
                oFCKeditor.setConfig(conf);
                session.setAttribute("ed", oFCKeditor);
            } else {
                oFCKeditor = (JCRContentFCKeditor) session.getAttribute("ed");
                if (editor != null) {
                    oFCKeditor.saveValue(editor);
                }
            }

%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
    <head>
        <title>eXo Platform JCR FCKeditor sample LOGIN ERROR</title>
        <link rel="stylesheet" href="../exojcrstyle.css">
        <script type="text/javascript" src="../FCKeditor/fckeditor.js"></script>
    </head>
    <body>

        <form
            action="/fckeditor/private/edit.jsp?editor=<%=editorName%>&path=<%=path%>"
            method="post"><%=oFCKeditor.create()%> <br>
        <input type="submit" value="Submit" /></form>
        <br>

        <%=oFCKeditor.getValue()%>

    </body>
</html>