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

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class DeleteCommand
{

   /**
    * Webdav Delete method implementation.
    * 
    * @param session current session
    * @param path file path
    * @param lockTokenHeader lock tokens
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response delete(Session session, String path, String lockTokenHeader)
   {
      try
      {
         if (lockTokenHeader == null)
         {
            lockTokenHeader = "";
         }

         Item item = session.getItem(path);
         if (item.isNode())
         {
            Node node = (Node)item;
            if (node.isLocked())
            {

               String nodeLockToken = node.getLock().getLockToken();

               if ((nodeLockToken == null) || (!nodeLockToken.equals(lockTokenHeader)))
               {
                  return Response.status(HTTPStatus.LOCKED).build();
               }
            }
         }
         item.remove();
         session.save();
         return Response.status(HTTPStatus.NO_CONTENT).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();
      }
   }

}
