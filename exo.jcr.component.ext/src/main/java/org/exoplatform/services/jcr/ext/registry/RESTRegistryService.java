/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.jcr.ext.registry;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.Registry.RegistryNode;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ext.util.XlinkHref;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.PrivilegedExceptionAction;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
@Path("/registry/")
public class RESTRegistryService implements ResourceContainer
{

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.RESTRegistryService");

   /**
    * 
    */
   private static final String REGISTRY = "registry";

   private static final String EXO_REGISTRY = "exo:registry/";

   /**
    * See {@link RegistryService}.
    */
   private RegistryService regService;

   /**
    * See {@link ThreadLocalSessionProviderService}.
    */
   private ThreadLocalSessionProviderService sessionProviderService;

   public RESTRegistryService(RegistryService regService, ThreadLocalSessionProviderService sessionProviderService)
      throws Exception
   {
      this.regService = regService;
      this.sessionProviderService = sessionProviderService;
   }

   @GET
   @Produces(MediaType.APPLICATION_XML)
   public Response getRegistry(@Context UriInfo uriInfo)
   {
      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      try
      {
         RegistryNode registryEntry = regService.getRegistry(sessionProvider);
         if (registryEntry != null)
         {
            Node registryNode = registryEntry.getNode();
            NodeIterator registryIterator = registryNode.getNodes();
            Document entry = SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Document>()
            {
               public Document run() throws Exception
               {
                  return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
               }
            });

            String fullURI = uriInfo.getRequestUri().toString();
            XlinkHref xlinkHref = new XlinkHref(fullURI);
            Element root = entry.createElement(REGISTRY);
            xlinkHref.putToElement(root);
            while (registryIterator.hasNext())
            {
               NodeIterator entryIterator = registryIterator.nextNode().getNodes();
               while (entryIterator.hasNext())
               {
                  Node node = entryIterator.nextNode();
                  Element xmlNode = entry.createElement(node.getName());
                  xlinkHref.putToElement(xmlNode, node.getPath().substring(EXO_REGISTRY.length()));
                  root.appendChild(xmlNode);
               }
            }
            entry.appendChild(root);
            return Response.ok(new DOMSource(entry), "text/xml").build();
         }
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Get registry failed", e);
         throw new WebApplicationException(e);
      }
   }

   @GET
   @Path("/{entryPath:.+}")
   @Produces(MediaType.APPLICATION_XML)
   public Response getEntry(@PathParam("entryPath") String entryPath)
   {

      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      try
      {
         RegistryEntry entry;
         entry = regService.getEntry(sessionProvider, normalizePath(entryPath));
         return Response.ok(new DOMSource(entry.getDocument())).build();
      }
      catch (PathNotFoundException e)
      {
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (RepositoryException e)
      {
         LOG.error("Get registry entry failed", e);
         throw new WebApplicationException(e);
      }
   }

   @POST
   @Path("/{groupName:.+}")
   @Consumes(MediaType.APPLICATION_XML)
   public Response createEntry(InputStream entryStream, @PathParam("groupName") String groupName, @Context UriInfo uriInfo)
   {

      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      try
      {
         RegistryEntry entry = RegistryEntry.parse(entryStream);
         regService.createEntry(sessionProvider, normalizePath(groupName), entry);
         URI location = uriInfo.getRequestUriBuilder().path(entry.getName()).build();
         return Response.created(location).build();
      }
      catch (IllegalArgumentException e)
      {
         LOG.error("Create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (IOException e)
      {
         LOG.error("Create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (SAXException e)
      {
         LOG.error("Create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (ParserConfigurationException e)
      {
         LOG.error("Create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (RepositoryException e)
      {
         LOG.error("Create registry entry failed", e);
         throw new WebApplicationException(e);
      }
   }

   @PUT
   @Path("/{groupName:.+}")
   @Consumes(MediaType.APPLICATION_XML)
   public Response recreateEntry(InputStream entryStream, @PathParam("groupName") String groupName,
            @Context UriInfo uriInfo, @QueryParam("createIfNotExist") boolean createIfNotExist)
   {

      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      try
      {
         RegistryEntry entry = RegistryEntry.parse(entryStream);
         if (createIfNotExist)
         {
            regService.updateEntry(sessionProvider, normalizePath(groupName), entry);
         }
         else
         {
            regService.recreateEntry(sessionProvider, normalizePath(groupName), entry);
         }
         URI location = uriInfo.getRequestUriBuilder().path(entry.getName()).build();
         return Response.created(location).build();
      }
      catch (IllegalArgumentException e)
      {
         LOG.error("Re-create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (IOException e)
      {
         LOG.error("Re-create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (SAXException e)
      {
         LOG.error("Re-create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (ParserConfigurationException e)
      {
         LOG.error("Re-create registry entry failed", e);
         throw new WebApplicationException(e);
      }
      catch (RepositoryException e)
      {
         LOG.error("Re-create registry entry failed", e);
         throw new WebApplicationException(e);
      }
   }

   @DELETE
   @Path("/{entryPath:.+}")
   public Response removeEntry(@PathParam("entryPath") String entryPath)
   {

      SessionProvider sessionProvider = sessionProviderService.getSessionProvider(null);
      try
      {
         regService.removeEntry(sessionProvider, normalizePath(entryPath));
         return null; // minds status 204 'No content'
      }
      catch (PathNotFoundException e)
      {
         return Response.status(Response.Status.NOT_FOUND).build();
      }
      catch (Exception e)
      {
         LOG.error("Remove registry entry failed", e);
         throw new WebApplicationException(e);
      }
   }

   private static String normalizePath(String path)
   {
      if (path.endsWith("/"))
         return path.substring(0, path.length() - 1);
      return path;
   }

}
