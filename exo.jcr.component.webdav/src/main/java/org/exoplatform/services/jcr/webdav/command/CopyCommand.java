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
import org.exoplatform.services.rest.ExtHttpHeaders;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;

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
     * Provides URI information needed for 'location' header in 'CREATED' response
     */
   private static String destination;

   /**
    * Empty constructor
    */
   public CopyCommand()
   {
      this.destination = null;
   }

   /**
    * Constructor
    * 
    * @param destination - provide data used in 'location' header
    */
   public CopyCommand(String destination)
   {
      this.destination = destination;
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
         destSession.getWorkspace().copy(sourcePath, destPath);
         if (destination != null)
         {
            return Response.status(HTTPStatus.CREATED).header(ExtHttpHeaders.LOCATION, destination).build();
         }

         // to save compatibility for deprecated WebDavServiceImpl.put(..), which does not provide uriInfo
         return Response.status(HTTPStatus.CREATED).build();
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
         destSession.getWorkspace().copy(sourceWorkspace, sourcePath, destPath);
         if (destination != null)
         {
            return Response.status(HTTPStatus.CREATED).header(ExtHttpHeaders.LOCATION, destination).build();
         }

         // to save compatibility for deprecated WebDavServiceImpl.put(..), which does not provide uriInfo
         return Response.status(HTTPStatus.CREATED).build();
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
