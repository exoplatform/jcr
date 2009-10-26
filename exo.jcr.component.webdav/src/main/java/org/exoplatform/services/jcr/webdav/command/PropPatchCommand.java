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
import org.exoplatform.services.jcr.webdav.command.proppatch.PropPatchResponseEntity;
import org.exoplatform.services.jcr.webdav.lock.NullResourceLocksHolder;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.jcr.webdav.xml.WebDavNamespaceContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

/**
 * Created by The eXo Platform SAS. Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class PropPatchCommand
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger(PropPatchCommand.class);

   /**
    * resource locks.
    */
   protected final NullResourceLocksHolder lockHolder;

   /**
    * Constructor.
    * 
    * @param lockHolder resource locks
    */
   public PropPatchCommand(NullResourceLocksHolder lockHolder)
   {
      this.lockHolder = lockHolder;
   }

   /**
    * Webdav Proppatch method method implementation.
    * 
    * @param session current session
    * @param path resource path
    * @param body request body
    * @param tokens tokens
    * @param baseURI base uri
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response propPatch(Session session, String path, HierarchicalProperty body, List<String> tokens,
      String baseURI)
   {
      try
      {

         lockHolder.checkLock(session, path, tokens);

         Node node = (Node)session.getItem(path);

         WebDavNamespaceContext nsContext = new WebDavNamespaceContext(session);
         URI uri = new URI(TextUtil.escape(baseURI + node.getPath(), '%', true));

         List<HierarchicalProperty> setList = Collections.emptyList();
         if (body.getChild(new QName("DAV:", "set")) != null)
         {
            setList = setList(body);
         }

         List<HierarchicalProperty> removeList = Collections.emptyList();
         if (body.getChild(new QName("DAV:", "remove")) != null)
         {
            removeList = removeList(body);
         }

         PropPatchResponseEntity entity = new PropPatchResponseEntity(nsContext, node, uri, setList, removeList);

         return Response.status(HTTPStatus.MULTISTATUS).entity(entity).type(MediaType.TEXT_XML).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.NOT_FOUND).build();
      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().build();
      }

   }

   /**
    * List of properties to set.
    * 
    * @param request request body
    * @return list of properties to set.
    */
   public List<HierarchicalProperty> setList(HierarchicalProperty request)
   {
      HierarchicalProperty set = request.getChild(new QName("DAV:", "set"));
      HierarchicalProperty prop = set.getChild(new QName("DAV:", "prop"));
      List<HierarchicalProperty> setList = prop.getChildren();
      return setList;
   }

   /**
    * List of properties to remove.
    * 
    * @param request request body
    * @return list of properties to remove.
    */
   public List<HierarchicalProperty> removeList(HierarchicalProperty request)
   {
      HierarchicalProperty remove = request.getChild(new QName("DAV:", "remove"));
      HierarchicalProperty prop = remove.getChild(new QName("DAV:", "prop"));
      List<HierarchicalProperty> removeList = prop.getChildren();
      return removeList;
   }

}
