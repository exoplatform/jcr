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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by The eXo Platform SAS Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */
public class MoveCommand
{

   /**
    * Cache control object.
    */
   private static CacheControl cacheControl = new CacheControl();

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.MoveCommand");

   /**
    * Provides URI information needed for 'location' header in 'CREATED' response
    */
   private final UriBuilder uriBuilder;
   
   /**
    * To trace if an item on destination path existed. 
    */
   private final boolean itemExisted;

   // Fix problem with moving under Windows Explorer.
   static
   {
      cacheControl.setNoCache(true);
   }

   /**
    * Empty constructor
    */
   public MoveCommand()
   {
      this.uriBuilder = null;
      this.itemExisted = false;
   }

   /**
    * Here we pass URI builder and info about pre-existence of item on the move
    * destination path If an item existed, we must respond with NO_CONTENT (204)
    * HTTP status.
    * If an item did not exist, we must respond with CREATED (201) HTTP status
    * More info can be found <a
    * href=http://www.webdav.org/specs/rfc2518.html#METHOD_MOVE>here</a>.
    * @param uriBuilder - provide data used in 'location' header
    * @param itemExisted - indicates if an item existed on copy destination
    */
   public MoveCommand(UriBuilder uriBuilder, boolean itemExisted)
   {
      this.uriBuilder = uriBuilder;
      this.itemExisted = itemExisted;
   }

   /**
    * Webdav Move method implementation.
    * 
    * @param session current session.
    * @param srcPath source resource path
    * @param destPath destination resource path
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response move(Session session, String srcPath, String destPath)
   {
      try
      {
         session.move(srcPath, destPath);
         session.save();

         // If the source resource was successfully moved
         // to a pre-existing destination resource.
         if (itemExisted)
         {
            return Response.status(HTTPStatus.NO_CONTENT).cacheControl(cacheControl).build();
         }
         // If the source resource was successfully moved,
         // and a new resource was created at the destination.
         else
         {
            if (uriBuilder != null)
            {
               return Response.created(uriBuilder.path(session.getWorkspace().getName()).path(destPath).build())
                  .cacheControl(cacheControl).build();
            }

            // to save compatibility if uriBuilder is not provided
            return Response.status(HTTPStatus.CREATED).cacheControl(cacheControl).build();
         }

      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }

   }

   /**
    * Webdav Move method implementation.
    * 
    * @param sourceSession source session
    * @param destSession destination session
    * @param srcPath source resource path
    * @param destPath destination resource path
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response move(Session sourceSession, Session destSession, String srcPath, String destPath)
   {
      try
      {

         destSession.getWorkspace().copy(sourceSession.getWorkspace().getName(), srcPath, destPath);
         sourceSession.getItem(srcPath).remove();
         sourceSession.save();

         // If the source resource was successfully moved
         // to a pre-existing destination resource.
         if (itemExisted)
         {
            return Response.status(HTTPStatus.NO_CONTENT).cacheControl(cacheControl).build();
         }
         // If the source resource was successfully moved,
         // and a new resource was created at the destination.
         else
         {
            if (uriBuilder != null)
            {
               return Response.created(uriBuilder.path(destSession.getWorkspace().getName()).path(destPath).build())
                  .cacheControl(cacheControl).build();
            }

            // to save compatibility if uriBuilder is not provided
            return Response.status(HTTPStatus.CREATED).cacheControl(cacheControl).build();
         }

      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         log.error(exc.getMessage(), exc);
         return Response.serverError().entity(exc.getMessage()).build();
      }

   }

}
