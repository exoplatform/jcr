/**
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.artifact.rest;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.resource.NodeRepresentation;
import org.exoplatform.services.jcr.ext.resource.NodeRepresentationService;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.util.EntityCollection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.IdentityConstants;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * @author Volodymyr Krasnikov volodymyr.krasnikov@exoplatform.com.ua
 * @version $Id: RESTArtifactLoaderService.java 11:37:47 andrew00x $
 */
@Path("/maven2/")
public class RESTArtifactLoaderService implements ResourceContainer
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger(RESTArtifactLoaderService.class);

   /**
    * XHTML namespace.
    */
   private static final String XHTML_NS = "http://www.w3.org/1999/xhtml";

   /**
    * Uses for represent JCR node in comfortable form.
    */
   private NodeRepresentationService nodeRepresentationService;

   /**
    * Root node for maven repository.
    */
   private String mavenRoot;

   /**
    * Repository name.
    */
   private String repository;

   /**
    * Workspace name.
    */
   private String workspace;

   /**
    * Realm name.
    */
   private String realmName;

   /**
    * Keeps SessionProvider for user.
    */
   private SessionProviderService sessionProviderService;

   /**
    * RepositoryService.
    */
   private RepositoryService repositoryService;

   /**
    * @param initParams
    *          the initialized parameters. Set repository name, workspace name, root node for Maven
    *          repository, username(optional) and password (optional).
    * @param sessionProviderService
    *          the SessionProviderService.
    * @param repositoryService
    *          the RepositoryService.
    * @param nodeRepresentationService
    *          the NodeRepresentationService.
    * @throws Exception
    *           if any errors occur or not valid configuration.
    */
   public RESTArtifactLoaderService(InitParams initParams, SessionProviderService sessionProviderService,
      RepositoryService repositoryService, NodeRepresentationService nodeRepresentationService,
      ExoContainerContext context) throws Exception
   {

      PropertiesParam props = initParams.getPropertiesParam("artifact.workspace");

      if (props == null)
         throw new IllegalArgumentException("Properties-param 'artifact.workspace' expected.");

      this.nodeRepresentationService = nodeRepresentationService;
      this.repositoryService = repositoryService;
      this.sessionProviderService = sessionProviderService;

      this.repository = props.getProperty("repository");
      this.workspace = props.getProperty("workspace");

      this.mavenRoot = props.getProperty("mavenRoot");
      if (mavenRoot.startsWith("/"))
         mavenRoot = mavenRoot.substring(1);
      if (!mavenRoot.endsWith("/"))
         mavenRoot += "/";
      this.realmName = context.getRealmName();
   }

   /**
    * Return Response with Maven artifact if it is file or HTML page for browsing if requested URL is
    * folder.
    * 
    * @param mavenPath
    *          the relative part of requested URL.
    * @param uriInfo
    * @param view
    * @param gadget
    * @return {@link Response}.
    */
   @GET
   @Path("/{path:.*}/")
   public Response getResource(@PathParam("path") String mavenPath, final @Context UriInfo uriInfo,
      final @QueryParam("view") String view, final @QueryParam("gadget") String gadget)
   {

      String resourcePath = mavenRoot + mavenPath; // JCR resource
      String shaResourcePath = mavenPath.endsWith(".sha1") ? mavenRoot + mavenPath : mavenRoot + mavenPath + ".sha1";

      mavenPath = uriInfo.getBaseUriBuilder().path(getClass()).path(mavenPath).build().toString();

      Session ses = null;

      try
      {
         // JCR resource
         SessionProvider sp = sessionProviderService.getSessionProvider(null);
         if (sp == null)
            throw new RepositoryException("Access to JCR Repository denied. SessionProvider is null.");

         ses = sp.getSession(workspace, repositoryService.getRepository(repository));

         ExtendedNode node = (ExtendedNode)ses.getRootNode().getNode(resourcePath);

         if (isFile(node))
         {
            if (view != null && view.equalsIgnoreCase("true"))
            {
               ExtendedNode shaNode = null;
               try
               {
                  shaNode = (ExtendedNode)ses.getRootNode().getNode(shaResourcePath);
               }
               catch (RepositoryException e)
               { // no .sh1 file found
                  if (LOG.isTraceEnabled())
                  {
                     LOG.trace("An exception occurred: " + e.getMessage());
                  }
               }
               return getArtifactInfo(node, mavenPath, gadget, shaNode);
            }
            else
            {
               return downloadArtifact(node);
            }
         }
         else
         {
            return browseRepository(node, mavenPath, gadget);
         }

      }
      catch (PathNotFoundException e)
      {
         if (LOG.isDebugEnabled())
            LOG.debug(e.getLocalizedMessage(), e);
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (AccessDeniedException e)
      {
         if (LOG.isDebugEnabled())
            LOG.debug(e.getLocalizedMessage(), e);
         if (ses == null || ses.getUserID().equals(IdentityConstants.ANONIM))
            return Response.status(Response.Status.UNAUTHORIZED).header(ExtHttpHeaders.WWW_AUTHENTICATE,
               "Basic realm=\"" + realmName + "\"").build();
         else
            return Response.status(Response.Status.FORBIDDEN).build();
      }
      catch (Exception e)
      {
         LOG.error("Failed get maven artifact", e);
         throw new WebApplicationException(e);
      }
      finally
      {
         if (ses != null)
            ses.logout();
      }

   }

   /**
    * Browsing of root node of Maven repository.
    * 
    * @param uriInfo
    * @param view
    * @param gadget
    * @return {@link Response}.
    */
   @GET
   public Response getRootNodeList(final @Context UriInfo uriInfo, final @QueryParam("view") String view,
      final @QueryParam("gadget") String gadget)
   {
      return getResource("", uriInfo, view, gadget);
   }

   /**
    * Check is node represents file.
    * 
    * @param node
    *          the node.
    * @return true if node represents file false otherwise.
    * @throws RepositoryException
    *           in JCR errors occur.
    */
   private static boolean isFile(Node node) throws RepositoryException
   {
      if (!node.isNodeType("nt:file"))
      {
         return false;
      }
      if (!node.getNode("jcr:content").isNodeType("nt:resource"))
      {
         return false;
      }
      return true;
   }

   /**
    * Create response for browsing Maven repository.
    * 
    * @param node
    *          the root node for browsing.
    * @param mavenPath
    *          the Maven path, used for creating &lt;a&gt; element.
    * @return @see {@link Response}.
    * @throws IOException
    *           if i/o error occurs.
    */
   private Response browseRepository(final Node node, final String mavenPath, final String gadget) throws IOException
   {

      final PipedOutputStream po = new PipedOutputStream();
      final PipedInputStream pi = new PipedInputStream(po);
      new Thread()
      {
         @Override
         @SuppressWarnings("unchecked")
         public void run()
         {
            try
            {
               XMLOutputFactory factory = XMLOutputFactory.newInstance();
               // name spaces
               factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
               XMLStreamWriter xsw = factory.createXMLStreamWriter(po, Constants.DEFAULT_ENCODING);
               xsw.writeStartDocument(Constants.DEFAULT_ENCODING, "1.0");
               xsw.writeDTD("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
                  + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
               xsw.writeCharacters("\n");
               if (gadget == null || !gadget.equalsIgnoreCase("true"))
               {
                  xsw.writeStartElement("html");
                  xsw.writeDefaultNamespace(XHTML_NS);
                  xsw.writeStartElement("head");
                  xsw.writeStartElement("style");
                  xsw.writeAttribute("type", "text/css");
                  xsw.writeCharacters("a {text-decoration: none; color: #10409C; }"
                     + "a:hover {text-decoration: underline;}" + ".centered { text-align: center; }"
                     + ".underlined { border-bottom : 1px solid #cccccc;  font-weight: bold;  text-align: center; }\n");
                  xsw.writeEndElement(); // style
                  xsw.writeStartElement("title");
                  xsw.writeCharacters("Maven2 Repository Browser");
                  xsw.writeEndElement(); // title
                  xsw.writeEndElement(); // head
               }
               xsw.writeStartElement("body");
               //
               xsw.writeStartElement("h2");
               xsw.writeAttribute("class", "centered");
               xsw.writeCharacters("Maven2 Repository");
               xsw.writeEndElement();
               //
               xsw.writeStartElement("table");
               xsw.writeAttribute("width", "90%");
               xsw.writeAttribute("style", "table-layout:fixed;");
               // table header
               xsw.writeStartElement("tr");
               xsw.writeStartElement("td");
               xsw.writeAttribute("class", "underlined");
               xsw.writeAttribute("width", "7%");
               xsw.writeEndElement(); // th
               xsw.writeStartElement("td");
               xsw.writeAttribute("class", "underlined");
               xsw.writeCharacters("name");
               xsw.writeEndElement(); // th
               xsw.writeStartElement("td");
               xsw.writeAttribute("class", "underlined");
               xsw.writeAttribute("width", "18%");
               xsw.writeCharacters("media-type");
               xsw.writeEndElement(); // th
               xsw.writeStartElement("td");
               xsw.writeAttribute("class", "underlined");
               xsw.writeAttribute("width", "15%");
               xsw.writeCharacters("size");
               xsw.writeEndElement(); // th
               xsw.writeStartElement("td");
               xsw.writeAttribute("class", "underlined");
               xsw.writeAttribute("width", "18%");
               xsw.writeCharacters("last modified");
               xsw.writeEndElement(); // th
               xsw.writeEndElement(); // tr
               // end table header

               // parent href
               String parent = mavenPath.substring(0, mavenPath.lastIndexOf('/'));
               xsw.writeStartElement("td");
               xsw.writeAttribute("class", "parenticon");
               xsw.writeEndElement();// td

               xsw.writeStartElement("td");
               xsw.writeStartElement("a");
               xsw.writeAttribute("href", parent + "?view=true&gadget=" + gadget);
               xsw.writeCharacters("..");
               xsw.writeEndElement(); // a
               xsw.writeEndElement(); // td
               xsw.writeEmptyElement("td");
               xsw.writeEmptyElement("td");
               xsw.writeEmptyElement("td");

               // NodeIterator nodes = node.getNodes();
               EntityCollection nodes = (EntityCollection)node.getNodes();
               Collections.sort(nodes.getList(), NODE_COMPARATOR);
               while (nodes.hasNext())
               {
                  Node node = nodes.nextNode();
                  xsw.writeStartElement("tr");
                  if (RESTArtifactLoaderService.isFile(node))
                  {
                     if (node.getName().endsWith("sha1"))
                        continue;
                     NodeRepresentation nodeRepresentation =
                        nodeRepresentationService.getNodeRepresentation(node, null);
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "fileicon");
                     xsw.writeEndElement();// td

                     xsw.writeStartElement("td");
                     xsw.writeAttribute("style", "font-style: italic;");
                     xsw.writeStartElement("a");
                     xsw.writeAttribute("href", (mavenPath.endsWith("/") ? mavenPath + node.getName() : mavenPath + "/"
                        + node.getName())
                        + "?view=true&gadget=" + gadget);
                     xsw.writeCharacters(node.getName());
                     xsw.writeEndElement(); // a
                     xsw.writeEndElement(); // td
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "centered");
                     xsw.writeCharacters(nodeRepresentation.getMediaType());
                     xsw.writeEndElement(); // td
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "centered");
                     xsw.writeCharacters("" + nodeRepresentation.getContentLenght());
                     xsw.writeEndElement(); // td
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "centered");
                     xsw.writeCharacters(new Date(nodeRepresentation.getLastModified()).toString());
                     xsw.writeEndElement(); // td
                  }
                  else
                  {
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "foldericon");
                     xsw.writeEndElement();// td
                     xsw.writeStartElement("td");
                     xsw.writeStartElement("a");
                     xsw.writeAttribute("href", (mavenPath.endsWith("/") ? mavenPath + node.getName() : mavenPath + "/"
                        + node.getName())
                        + "?view=true&gadget=" + gadget);
                     xsw.writeCharacters(node.getName());
                     xsw.writeEndElement(); // a
                     xsw.writeEndElement(); // td
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "centered");
                     xsw.writeCharacters("-");
                     xsw.writeEndElement(); // td
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "centered");
                     xsw.writeCharacters("-");
                     xsw.writeEndElement(); // td
                     xsw.writeStartElement("td");
                     xsw.writeAttribute("class", "centered");
                     xsw.writeCharacters("-");
                     xsw.writeEndElement(); // td
                  }
                  xsw.writeEndElement(); // tr
               }

               xsw.writeStartElement("tr");
               xsw.writeEndElement();

               xsw.writeEndElement(); // table
               xsw.writeEndElement(); // body
               if (gadget == null || !gadget.equalsIgnoreCase("true"))
               {
                  xsw.writeEndElement(); // html
               }

               xsw.writeEndDocument();
            }
            catch (XMLStreamException xmle)
            {
               LOG.error(xmle.getLocalizedMessage(), xmle);
            }
            catch (RepositoryException re)
            {
               LOG.error(re.getLocalizedMessage(), re);
            }
            finally
            {
               try
               {
                  po.flush();
                  po.close();
               }
               catch (IOException e)
               {
                  LOG.error(e.getLocalizedMessage(), e);
               }
            }
         }
      }.start();

      // application/xhtml+xml content type is recommended for XHTML, but IE6
      // does't support this.
      return Response.ok(pi, "text/html").build();

   }

   /**
    * Get content of JCR node.
    * 
    * @param node
    *          the node.
    * @return @see {@link Response}.
    * @throws Exception
    *           if any errors occurs.
    */
   private Response downloadArtifact(Node node) throws Exception
   {
      NodeRepresentation nodeRepresentation = nodeRepresentationService.getNodeRepresentation(node, null);
      if (node.canAddMixin("exo:mavencounter"))
      {
         node.addMixin("exo:mavencounter");
         node.getSession().save();
      }
      node.setProperty("exo:downloadcounter", node.getProperty("exo:downloadcounter").getLong() + 1l);
      node.getSession().save();
      long lastModified = nodeRepresentation.getLastModified();
      String contentType = nodeRepresentation.getMediaType();
      long contentLength = nodeRepresentation.getContentLenght();
      InputStream entity = nodeRepresentation.getInputStream();
      Response response =
         Response.ok(entity, contentType).header(HttpHeaders.CONTENT_LENGTH, Long.toString(contentLength))
            .lastModified(new Date(lastModified)).build();
      return response;
   }

   /**
    * Get JCR node information.
    * 
    * @param node
    *          the node.
    * @return @see {@link Response}.
    * @throws Exception
    *           if any errors occurs.
    */
   private Response getArtifactInfo(Node node, final String mavenPath, final String gadget, Node shaNode)
      throws Exception
   {
      NodeRepresentation nodeRepresentation = nodeRepresentationService.getNodeRepresentation(node, null);

      NodeRepresentation shNodeRepresentation = null;
      if (shaNode != null)
         shNodeRepresentation = nodeRepresentationService.getNodeRepresentation(shaNode, null);

      final PipedOutputStream po = new PipedOutputStream();
      final PipedInputStream pi = new PipedInputStream(po);

      long lastModified = nodeRepresentation.getLastModified();
      long contentLength = nodeRepresentation.getContentLenght();

      try
      {

         if (node.canAddMixin("exo:mavencounter"))
         {
            node.addMixin("exo:mavencounter");
            node.getSession().save();
         }
         int count = (int)node.getProperty("exo:downloadcounter").getLong();

         XMLOutputFactory factory = XMLOutputFactory.newInstance();
         factory.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
         XMLStreamWriter xsw = factory.createXMLStreamWriter(po, Constants.DEFAULT_ENCODING);
         xsw.writeStartDocument(Constants.DEFAULT_ENCODING, "1.0");
         xsw.writeDTD("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
         xsw.writeCharacters("\n");
         xsw.writeStartElement("html");
         xsw.writeDefaultNamespace(XHTML_NS);
         xsw.writeStartElement("head");
         xsw.writeStartElement("style");
         xsw.writeAttribute("type", "text/css");
         xsw.writeCharacters("a {text-decoration: none; color: #10409C;}" + "a:hover {text-decoration: underline;}"
            + ".centered { text-align: center; }" + ".underlined { border-bottom : 1px solid #cccccc; }\n");
         xsw.writeEndElement(); // style
         xsw.writeStartElement("title");
         xsw.writeCharacters("Maven2 Artifact Information");
         xsw.writeEndElement(); // title
         xsw.writeEndElement(); // head
         xsw.writeStartElement("body");

         xsw.writeStartElement("b");
         xsw.writeCharacters("Artifact Information :");
         xsw.writeEndElement();
         xsw.writeEmptyElement("br");

         xsw.writeCharacters("Name:  " + node.getName());
         xsw.writeEmptyElement("br");

         xsw.writeCharacters("Size:  " + contentLength);
         xsw.writeEmptyElement("br");
         xsw.writeCharacters("Last modified:  " + new Date(lastModified).toString());
         xsw.writeEmptyElement("br");
         xsw.writeCharacters("Download:  ");
         xsw.writeStartElement("a");
         xsw.writeAttribute("href", mavenPath.endsWith("/") ? mavenPath.substring(0, mavenPath.length() - 1)
            : mavenPath);
         xsw.writeCharacters("Link");
         xsw.writeEndElement(); // a

         if (shNodeRepresentation != null)
         {
            xsw.writeEmptyElement("br");
            xsw.writeCharacters("Checksum:  " + getStreamAsString(shNodeRepresentation.getInputStream()));
            xsw.writeEmptyElement("br");
         }

         xsw.writeEmptyElement("br");
         xsw.writeCharacters("Downloads count :  " + count);
         xsw.writeEmptyElement("br");

         xsw.writeEmptyElement("br");
         xsw.writeStartElement("a");
         xsw.writeAttribute("href", (mavenPath.endsWith("/") ? mavenPath.substring(0, mavenPath.length() - 1)
            .substring(0, mavenPath.lastIndexOf("/")) : mavenPath.substring(0, mavenPath.lastIndexOf("/")))
            + "?view=true&gadget=" + gadget);
         xsw.writeCharacters("Back to browsing");
         xsw.writeEndElement(); // a

         xsw.writeEmptyElement("br");

         xsw.writeEndElement(); // body
         xsw.writeEndElement(); // html

      }
      catch (XMLStreamException xmle)
      {
         LOG.error(xmle.getLocalizedMessage(), xmle);
      }
      catch (RepositoryException re)
      {
         LOG.error(re.getLocalizedMessage(), re);
      }
      finally
      {
         try
         {
            po.flush();
            po.close();
         }
         catch (IOException e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }

      }

      return Response.ok(pi, "text/html").build();

   }

   /**
    * @param stream
    *          stream
    * @return string representation of stream
    * @throws IOException
    *           if any i/o errors occurs
    */
   protected String getStreamAsString(InputStream stream) throws IOException
   {
      byte[] buff = new byte[stream.available()];
      stream.read(buff);
      return new String(buff);
   }

   /**
    * Node type comparator.
    * 
    * @see NodeComparator
    */
   private static final Comparator<Node> NODE_COMPARATOR = new NodeComparator();

   /**
    * It is needs to display artifact in alphabetical order and display folders before files.
    */
   private static class NodeComparator implements Comparator<Node>
   {

      /**
       * {@inheritDoc}
       */
      public int compare(Node o1, Node o2)
      {
         try
         {
            if (isFile(o1) && !isFile(o2))
               return 1;
            if (!isFile(o1) && isFile(o2))
               return -1;
            return o1.getName().compareTo(o2.getName());
         }
         catch (RepositoryException e)
         {
            LOG.error("", e);
            return 0;
         }
      }

   }

}
