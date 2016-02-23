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
package org.exoplatform.services.jcr.webdav.command.deltav;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.Depth;
import org.exoplatform.services.jcr.webdav.command.deltav.report.VersionTreeResponseEntity;
import org.exoplatform.services.jcr.webdav.resource.ResourceUtil;
import org.exoplatform.services.jcr.webdav.resource.VersionedCollectionResource;
import org.exoplatform.services.jcr.webdav.resource.VersionedFileResource;
import org.exoplatform.services.jcr.webdav.resource.VersionedResource;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public class ReportCommand
{

   /**
    * logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.ReportCommand");

   /**
    * Webdav Report method implementation.
    * 
    * @param session current session
    * @param path resource path
    * @param body request bidy
    * @param depth report depth
    * @param baseURI base Uri
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response report(Session session, String path, HierarchicalProperty body, Depth depth, String baseURI)
   {
      try
      {
         Node node = (Node)session.getItem(path);
         WebDavNamespaceContext nsContext = new WebDavNamespaceContext(session);
         String strUri = baseURI + node.getPath();
         URI uri = new URI(TextUtil.escape(strUri, '%', true));

         if (!ResourceUtil.isVersioned(node))
         {
            return Response.status(HTTPStatus.PRECON_FAILED).build();
         }

         VersionedResource resource;
         if (ResourceUtil.isFile(node))
         {
            resource = new VersionedFileResource(uri, node, nsContext);
         }
         else
         {
            resource = new VersionedCollectionResource(uri, node, nsContext);
         }

         Set<QName> properties = getProperties(body);

         VersionTreeResponseEntity response = new VersionTreeResponseEntity(nsContext, resource, properties);

         return Response.status(HTTPStatus.MULTISTATUS).entity(response).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
      }
      catch (RepositoryException exc)
      {
         LOG.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         LOG.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }
   }

   /**
    * Returns the list of properties.
    * 
    * @param body request body
    * @return the list of properties
    */
   protected Set<QName> getProperties(HierarchicalProperty body)
   {
      HashSet<QName> properties = new HashSet<QName>();

      HierarchicalProperty prop = body.getChild(new QName("DAV:", "prop"));
      if (prop == null)
      {
         return properties;
      }

      for (int i = 0; i < prop.getChildren().size(); i++)
      {
         HierarchicalProperty property = prop.getChild(i);
         properties.add(property.getName());
      }

      return properties;
   }

}
