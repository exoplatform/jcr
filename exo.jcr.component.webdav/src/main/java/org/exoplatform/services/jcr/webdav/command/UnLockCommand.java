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
import org.exoplatform.services.jcr.webdav.lock.NullResourceLocksHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS. Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>
 * 
 * @version $Id: $
 */

public class UnLockCommand
{

   /**
    * logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.UnLockCommand");

   /**
    * resource locks.
    */
   protected final NullResourceLocksHolder nullResourceLocks;

   /**
    * Constructor.
    * 
    * @param nullResourceLocks resource locks.
    */
   public UnLockCommand(final NullResourceLocksHolder nullResourceLocks)
   {
      this.nullResourceLocks = nullResourceLocks;
   }

   /**
    * Webdav Unlock method implementation.
    * 
    * @param session current seesion
    * @param path resource path
    * @param tokens tokens
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response unLock(Session session, String path, List<String> tokens)
   {
      try
      {
         try
         {
            Node node = (Node)session.getItem(path);

            if (node.isLocked())
            {
               node.unlock();
               session.save();
            }

            return Response.status(HTTPStatus.NO_CONTENT).build();
         }
         catch (PathNotFoundException exc)
         {
            if (nullResourceLocks.isLocked(session, path))
            {
               nullResourceLocks.checkLock(session, path, tokens);
               nullResourceLocks.removeLock(session, path);
               return Response.status(HTTPStatus.NO_CONTENT).build();
            }

            return Response.status(HTTPStatus.NOT_FOUND).entity(exc.getMessage()).build();
         }

      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }

   }

}
