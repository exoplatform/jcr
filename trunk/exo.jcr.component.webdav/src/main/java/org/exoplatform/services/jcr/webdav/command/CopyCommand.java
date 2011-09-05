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

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by The eXo Platform SAS Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class CopyCommand
{

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.CopyCommand");

   /**
    * Provides URI information needed for 'location' header in 'CREATED'
    * response
    */
   private final UriBuilder uriBuilder;

   /**
    * To trace if an item on destination path existed. 
    */
   private final boolean itemExisted;

   /**
    * Empty constructor
    */
   public CopyCommand()
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
    * 
    * @param uriBuilder - provide data used in 'location' header
    * @param itemExisted - indicates if an item existed on copy destination
    */
   public CopyCommand(UriBuilder uriBuilder, boolean itemExisted)
   {
      this.uriBuilder = uriBuilder;
      this.itemExisted = itemExisted;
   }

   /**
    * Webdav COPY method implementation for the same workspace.
    * 
    * @param destSession destination session
    * @param sourcePath source file path
    * @param destPath destination file path
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response copy(Session destSession, String sourcePath, String destPath)
   {
      try
      {
         Workspace workspace = destSession.getWorkspace();
         workspace.copy(sourcePath, destPath);

         // If the source resource was successfully moved
         // to a pre-existing destination resource.
         if (itemExisted)
         {
            return Response.noContent().build();
         }
         // If the source resource was successfully moved,
         // and a new resource was created at the destination.
         else
         {
            if (uriBuilder != null)
            {
               return Response.created(uriBuilder.path(workspace.getName()).path(destPath).build()).build();
            }

            // to save compatibility if uribuilder is not provided
            return Response.status(HTTPStatus.CREATED).build();
         }

      }
      catch (ItemExistsException e)
      {
         return Response.status(HTTPStatus.METHOD_NOT_ALLOWED).entity(e.getMessage()).build();
      }
      catch (PathNotFoundException e)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(e.getMessage()).build();
      }
      catch (AccessDeniedException e)
      {
         return Response.status(HTTPStatus.FORBIDDEN).entity(e.getMessage()).build();
      }
      catch (LockException e)
      {
         return Response.status(HTTPStatus.LOCKED).entity(e.getMessage()).build();
      }
      catch (RepositoryException e)
      {
         log.error(e.getMessage(), e);
         return Response.serverError().entity(e.getMessage()).build();
      }
   }

   /**
    * Webdav COPY method implementation for the different workspaces.
    * 
    * @param destSession destination session
    * @param sourceWorkspace source workspace name
    * @param sourcePath source file path
    * @param destPath destination file path
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response copy(Session destSession, String sourceWorkspace, String sourcePath, String destPath)
   {
      try
      {
         Workspace destWorkspace = destSession.getWorkspace();
         destWorkspace.copy(sourceWorkspace, sourcePath, destPath);

         // If the source resource was successfully moved
         // to a pre-existing destination resource.
         if (itemExisted)
         {
            return Response.noContent().build();
         }
         // If the source resource was successfully moved,
         // and a new resource was created at the destination.
         else
         {
            if (uriBuilder != null)
            {
               return Response.created(uriBuilder.path(destWorkspace.getName()).path(destPath).build()).build();
            }

            // to save compatibility if uriBuilder is not provided
            return Response.status(HTTPStatus.CREATED).build();
         }

      }
      catch (ItemExistsException e)
      {
         return Response.status(HTTPStatus.METHOD_NOT_ALLOWED).entity(e.getMessage()).build();
      }
      catch (PathNotFoundException e)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(e.getMessage()).build();
      }
      catch (AccessDeniedException e)
      {
         return Response.status(HTTPStatus.FORBIDDEN).entity(e.getMessage()).build();
      }
      catch (LockException e)
      {
         return Response.status(HTTPStatus.LOCKED).entity(e.getMessage()).build();
      }
      catch (RepositoryException e)
      {
         return Response.serverError().entity(e.getMessage()).build();
      }
   }
}
