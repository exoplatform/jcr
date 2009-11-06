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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a> .
 * 
 * @version $Id: $
 */

public class VersionControlCommand
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger(VersionControlCommand.class);

   /**
    * Webdav Version-Control method implementation.
    * 
    * @param session current session
    * @param path resource path 
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response versionControl(Session session, String path)
   {
      try
      {
         Node node = (Node)session.getItem(path);

         if (!node.isNodeType("mix:versionable"))
         {
            node.addMixin("mix:versionable");
            session.save();
         }
         return Response.ok().build();

      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();

      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().build();
      }
   }

}
