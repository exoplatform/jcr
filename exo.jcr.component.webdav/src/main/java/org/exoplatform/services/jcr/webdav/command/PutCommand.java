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
import org.exoplatform.services.jcr.webdav.MimeTypeRecognizer;
import org.exoplatform.services.jcr.webdav.lock.NullResourceLocksHolder;
import org.exoplatform.services.jcr.webdav.util.TextUtil;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by The eXo Platform SAS Author : <a
 * href="gavrikvetal@gmail.com">Vitaly Guly</a>.
 * 
 * @version $Id: $
 */

public class PutCommand
{

   /**
    * resource locks.
    */
   private final NullResourceLocksHolder nullResourceLocks;

   /**
    * Provides URI information needed for 'location' header in 'CREATED'
    * response
    */
   private final UriBuilder uriBuilder;

   /**
    * To access mime-type and encoding
    */
   private final MimeTypeRecognizer mimeTypeRecognizer;

   /**
    * Constructor.
    * 
    * @param nullResourceLocks resource locks.
    * @param uriBuilder - provides data used in 'location' header
    * @param mimeTypeRecognizer - provides mime-type recognizer
    */
   public PutCommand(NullResourceLocksHolder nullResourceLocks, UriBuilder uriBuilder,
      MimeTypeRecognizer mimeTypeRecognizer)
   {
      this.nullResourceLocks = nullResourceLocks;
      this.uriBuilder = uriBuilder;
      this.mimeTypeRecognizer = mimeTypeRecognizer;
   }

   /**
    * Webdav Put method implementation.
    * 
    * @param session current session
    * @param path resource path
    * @param inputStream stream that contains resource content
    * @param fileNodeType the node type of file node
    * @param contentNodeType the node type of content
    * @param mixins the list of mixins
    * @param updatePolicyType update policy
    * @param tokens tokens
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response put(Session session, String path, InputStream inputStream, String fileNodeType,
      String contentNodeType, List<String> mixins, String updatePolicyType, String autoVersion, List<String> tokens)
   {

      try
      {

         Node node = null;
         try
         {
            node = (Node)session.getItem(path);
         }

         catch (PathNotFoundException pexc)
         {
            nullResourceLocks.checkLock(session, path, tokens);
         }

         //if (node == null || "add".equals(updatePolicyType))
         if (node == null)
         {

            node = session.getRootNode().addNode(TextUtil.relativizePath(path), fileNodeType);

            node.addNode("jcr:content", contentNodeType);
            updateContent(node, inputStream, mixins);
         }
         else
         {
            if ("add".equals(updatePolicyType))
            {
               node = session.getRootNode().getNode(TextUtil.relativizePath(path));
               if (!node.isNodeType("mix:versionable"))
               {
                  node = session.getRootNode().addNode(TextUtil.relativizePath(path), fileNodeType);
                  node.addNode("jcr:content", contentNodeType);
                  updateContent(node, inputStream, mixins);
               }
               else
               {
                  updateVersion(node, inputStream, autoVersion, mixins);
               }

            }
            else if ("create-version".equals(updatePolicyType))
            {
               createVersion(node, inputStream, mixins);
            }
            else
            {
               if (!node.isNodeType("mix:versionable"))
               {
                  updateContent(node, inputStream, mixins);
               }
               else
               {
                  updateVersion(node, inputStream, autoVersion, mixins);
               }
            }
         }

         session.save();

      }
      catch (LockException exc)
      {
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (AccessDeniedException exc)
      {
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();
      }
      if (uriBuilder != null)
      {
         return Response.created(uriBuilder.path(session.getWorkspace().getName()).path(path).build()).build();
      }

      // to save compatibility if uriBuilder is not provided
      return Response.status(HTTPStatus.CREATED).build();
   }

   /**
    * Creates the new version of file.
    * 
    * @param fileNode file node
    * @param inputStream input stream that contains the content of file
    * @param mixins list of mixins
    * @throws RepositoryException {@link RepositoryException}
    */
   private void createVersion(Node fileNode, InputStream inputStream, List<String> mixins) throws RepositoryException
   {
      if (!fileNode.isNodeType("mix:versionable"))
      {
         if (fileNode.canAddMixin("mix:versionable"))
         {
            fileNode.addMixin("mix:versionable");
            fileNode.getSession().save();
         }
         fileNode.checkin();
         fileNode.getSession().save();
      }

      if (!fileNode.isCheckedOut())
      {
         fileNode.checkout();
         fileNode.getSession().save();
      }

      updateContent(fileNode, inputStream, mixins);
      fileNode.getSession().save();
      fileNode.checkin();
      fileNode.getSession().save();
   }

   /**
    * Updates jcr:content node.
    * 
    * @param node parent node
    * @param inputStream inputStream input stream that contains the content of 
    *          file
    * @param mixins list of mixins
    * @throws RepositoryException  {@link RepositoryException}
    */
   private void updateContent(Node node, InputStream inputStream, List<String> mixins) throws RepositoryException
   {

      Node content = node.getNode("jcr:content");

      if (mimeTypeRecognizer.isMimeTypeRecognized() || !content.hasProperty("jcr:mimeType"))
      {
         content.setProperty("jcr:mimeType", mimeTypeRecognizer.getMimeType());
      }

      if (mimeTypeRecognizer.isEncodingSet())
      {
         content.setProperty("jcr:encoding", mimeTypeRecognizer.getEncoding());
      }
      content.setProperty("jcr:lastModified", Calendar.getInstance());
      content.setProperty("jcr:data", inputStream);

      for (String mixinName : mixins)
      {
         if (content.canAddMixin(mixinName))
         {
            content.addMixin(mixinName);
         }
      }
      node.getSession().save();

   }

   /**
    * Updates the content of the versionable file according to auto-version value.
    * 
    * @param fileNode Node to update
    * @param inputStream input stream that contains the content of
    *          file
    * @param autoVersion auto-version value
    * @param mixins list of mixins
    * @throws RepositoryException {@link RepositoryException}
    */
   private void updateVersion(Node fileNode, InputStream inputStream, String autoVersion, List<String> mixins)
      throws RepositoryException
   {
      if (!fileNode.isCheckedOut())
      {
         fileNode.checkout();
         fileNode.getSession().save();
      }
      if ("checkout".equals(autoVersion))
      {
         updateContent(fileNode, inputStream, mixins);
      }
      else if ("checkout-checkin".equals(autoVersion))
      {
         updateContent(fileNode, inputStream, mixins);
         fileNode.getSession().save();
         fileNode.checkin();
      }
      fileNode.getSession().save();
   }

}
