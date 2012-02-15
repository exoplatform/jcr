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
package org.exoplatform.services.jcr.webdav.command;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.common.util.HierarchicalProperty;
import org.exoplatform.services.jcr.webdav.command.propfind.PropFindRequestEntity;
import org.exoplatform.services.jcr.webdav.command.propfind.PropFindResponseEntity;
import org.exoplatform.services.jcr.webdav.resource.CollectionResource;
import org.exoplatform.services.jcr.webdav.resource.FileResource;
import org.exoplatform.services.jcr.webdav.resource.IllegalResourceTypeException;
import org.exoplatform.services.jcr.webdav.resource.Resource;
import org.exoplatform.services.jcr.webdav.resource.ResourceUtil;
import org.exoplatform.services.jcr.webdav.resource.VersionedCollectionResource;
import org.exoplatform.services.jcr.webdav.resource.VersionedFileResource;
import org.exoplatform.services.jcr.webdav.util.PropertyConstants;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.ExtHttpHeaders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS <br/>
 * Author : <a href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class PropFindCommand
{

   /**
    * logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.webdav.PropFindCommand");

   /**
    * Webdav Propfind method implementation.
    * 
    * @param session current session
    * @param path resource path
    * @param body request body
    * @param depth request depth
    * @param baseURI base Uri
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response propfind(Session session, String path, HierarchicalProperty body, int depth, String baseURI)
   {
      Node node;
      try
      {
         node = (Node)session.getItem(path);
      }
      catch (PathNotFoundException e)
      {
         return Response.status(HTTPStatus.NOT_FOUND).entity(e.getMessage()).build();
      }
      catch (RepositoryException exc)
      {
         LOG.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }

      WebDavNamespaceContext nsContext;
      Resource resource;
      try
      {
         nsContext = new WebDavNamespaceContext(session);

         resource = null;
         URI uri;
         if ("/".equals(node.getPath()))
         {
            uri = new URI(TextUtil.escape(baseURI, '%', true));
         }
         else
         {
            uri = new URI(TextUtil.escape(baseURI + node.getPath(), '%', true));
         }

         if (ResourceUtil.isVersioned(node))
         {
            if (ResourceUtil.isFile(node))
            {
               resource = new VersionedFileResource(uri, node, nsContext);
            }
            else
            {
               resource = new VersionedCollectionResource(uri, node, nsContext);
            }
         }
         else
         {
            if (ResourceUtil.isFile(node))
            {
               resource = new FileResource(uri, node, nsContext);
            }
            else
            {
               resource = new CollectionResource(uri, node, nsContext);
            }
         }

      }
      catch (RepositoryException e1)
      {
         LOG.error(e1.getMessage(), e1);
         return Response.serverError().build();
      }
      catch (URISyntaxException e1)
      {
         LOG.error(e1.getMessage(), e1);
         return Response.serverError().build();
      }
      catch (IllegalResourceTypeException e1)
      {
         LOG.error(e1.getMessage(), e1);
         return Response.serverError().build();
      }

      PropFindRequestEntity request = new PropFindRequestEntity(body);
      PropFindResponseEntity response;

      if (request.getType().equalsIgnoreCase("allprop"))
      {
         response = new PropFindResponseEntity(depth, resource, null, false);
      }
      else if (request.getType().equalsIgnoreCase("include"))
      {
         response = new PropFindResponseEntity(depth, resource, propertyNames(body), false);
      }
      else if (request.getType().equalsIgnoreCase("propname"))
      {
         response = new PropFindResponseEntity(depth, resource, null, true);
      }
      else if (request.getType().equalsIgnoreCase("prop"))
      {
         response = new PropFindResponseEntity(depth, resource, propertyNames(body), false, session);
      }
      else
      {
         return Response.status(HTTPStatus.BAD_REQUEST).entity("Bad Request").build();
      }

      return Response.status(HTTPStatus.MULTISTATUS).entity(response).header(ExtHttpHeaders.CONTENT_TYPE,
         MediaType.TEXT_XML).build();
   }

   /**
    * Returns the set of properties names.
    * 
    * @param body request body.
    * @return the set of properties names.
    */
   private Set<QName> propertyNames(HierarchicalProperty body)
   {
      HashSet<QName> names = new HashSet<QName>();

      HierarchicalProperty propBody = body.getChild(PropertyConstants.DAV_ALLPROP_INCLUDE);

      if (propBody != null)
      {
         names.add(PropertyConstants.DAV_ALLPROP_INCLUDE);
      }
      else
      {
         propBody = body.getChild(0);
      }

      List<HierarchicalProperty> properties = propBody.getChildren();
      Iterator<HierarchicalProperty> propIter = properties.iterator();
      while (propIter.hasNext())
      {
         HierarchicalProperty property = propIter.next();
         names.add(property.getName());
      }

      return names;
   }

}
