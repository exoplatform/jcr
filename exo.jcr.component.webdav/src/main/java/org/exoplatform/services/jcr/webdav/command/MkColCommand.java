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
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
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

public class MkColCommand
{

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger(MkColCommand.class);

   /**
    * resource locks.
    */
   private final NullResourceLocksHolder nullResourceLocks;

   /**
    * Constructor. 
    * 
    * @param nullResourceLocks resource locks. 
    */
   public MkColCommand(final NullResourceLocksHolder nullResourceLocks)
   {
      this.nullResourceLocks = nullResourceLocks;
   }

   /**
    * Webdav Mkcol method implementation.
    * 
    * @param session current session
    * @param path resource path
    * @param nodeType folder node type
    * @param mixinTypes mixin types 
    * @param tokens tokens
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response mkCol(Session session, String path, String nodeType, List<String> mixinTypes, List<String> tokens)
   {
      Node node;
      try
      {
         nullResourceLocks.checkLock(session, path, tokens);
         node = session.getRootNode().addNode(TextUtil.relativizePath(path), nodeType);

         if (mixinTypes != null)
         {
            addMixins(node, mixinTypes);
         }
         session.save();

      }
      catch (ItemExistsException exc)
      {
         return Response.status(HTTPStatus.METHOD_NOT_ALLOWED).entity(exc.getMessage()).build();

      }
      catch (PathNotFoundException exc)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();

      }
      catch (AccessDeniedException exc)
      {
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();

      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         return Response.serverError().entity(exc.getMessage()).build();
      }

      return Response.status(HTTPStatus.CREATED).build();
   }

   /**
    * Adds mixins to node.
    * 
    * @param node node.
    * @param mixinTypes mixin types.
    */
   private void addMixins(Node node, List<String> mixinTypes)
   {
      for (int i = 0; i < mixinTypes.size(); i++)
      {
         String curMixinType = mixinTypes.get(i);
         try
         {
            node.addMixin(curMixinType);
         }
         catch (Exception exc)
         {
            log.error("Can't add mixin [" + curMixinType + "]", exc);
         }
      }
   }
}
