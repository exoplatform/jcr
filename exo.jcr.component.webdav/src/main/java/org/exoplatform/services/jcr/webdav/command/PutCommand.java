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

import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.jcr.ext.utils.VersionHistoryUtils;
import org.exoplatform.services.jcr.webdav.MimeTypeRecognizer;
import org.exoplatform.services.jcr.webdav.lock.NullResourceLocksHolder;
import org.exoplatform.services.jcr.webdav.util.TextUtil;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.InputStream;
import java.util.Calendar;
import java.util.List;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

/**
 * Created by The eXo Platform SAS.
 * @author Vitaly Guly - gavrikvetal@gmail.com
 * 
 * @version $Id: $
 */

public class PutCommand
{

   /**
    * Logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.webdav.command.PutCommand");

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

   private final String CHECKOUT_CHECKIN = "checkout-checkin";

   private final String CHECKIN_CHECKOUT = "checkin-checkout";

   private final String CHECKOUT = "checkout";


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

         if (node == null)
         {
            node = session.getRootNode().addNode(TextUtil.relativizePath(path, false), fileNodeType);
            // We set the new path
            path = node.getPath();
            node.addNode("jcr:content", contentNodeType);
            updateContent(node, inputStream, mixins);
         }
         else
         {
            if ("add".equals(updatePolicyType))
            {
               node = session.getRootNode().getNode(TextUtil.relativizePath(path));
               if (!node.isNodeType(VersionHistoryUtils.MIX_VERSIONABLE))
               {
                  node = session.getRootNode().addNode(TextUtil.relativizePath(path, false), fileNodeType);
                  // We set the new path
                  path = node.getPath();
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
               createVersion(node, inputStream, autoVersion, mixins);
            }
            else
            {
               if (!node.isNodeType(VersionHistoryUtils.MIX_VERSIONABLE))
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
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (AccessDeniedException exc)
      {
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
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
    * Webdav Put method implementation.
    *
    * @param session current session
    * @param path resource path
    * @param inputStream stream that contains resource content
    * @param fileNodeType the node type of file node
    * @param contentNodeType the node type of content
    * @param mixins the list of mixins
    * @param tokens tokens
    * @param allowedAutoVersionPath
    * @return the instance of javax.ws.rs.core.Response
    */
   public Response put(Session session, String path, InputStream inputStream, String fileNodeType,
                       String contentNodeType, List<String> mixins, List<String> tokens, MultivaluedMap<String, String> allowedAutoVersionPath)
   {
      try
      {

         Node node = null;
         boolean isVersioned;
         try
         {
            node = (Node)session.getItem(path);
         }
         catch (PathNotFoundException pexc)
         {
            nullResourceLocks.checkLock(session, path, tokens);
         }

         if (node == null)
         {
            node = session.getRootNode().addNode(TextUtil.relativizePath(path, false), fileNodeType);
            // We set the new path
            path = node.getPath();
            node.addNode("jcr:content", contentNodeType);
            updateContent(node, inputStream, mixins);
            isVersioned = isVersionSupported(node.getPath(), session.getWorkspace().getName(), allowedAutoVersionPath);
            if (isVersioned && node.canAddMixin(VersionHistoryUtils.MIX_VERSIONABLE))
            {
               node.addMixin(VersionHistoryUtils.MIX_VERSIONABLE);
            }
            node.getSession().save();
         }
         else
         {
            isVersioned = isVersionSupported(node.getPath(), session.getWorkspace().getName(), allowedAutoVersionPath);
            if (isVersioned)
            {

               VersionHistoryUtils.createVersion(node);
               updateContent(node, inputStream, mixins);

            }
            else
            {
               updateContent(node, inputStream, mixins);
            }
         }

         session.save();
      }
      catch (LockException exc)
      {
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
         return Response.status(HTTPStatus.LOCKED).entity(exc.getMessage()).build();

      }
      catch (AccessDeniedException exc)
      {
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
         return Response.status(HTTPStatus.FORBIDDEN).entity(exc.getMessage()).build();

      }
      catch (RepositoryException exc)
      {
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
         return Response.status(HTTPStatus.CONFLICT).entity(exc.getMessage()).build();
      }
      catch (Exception exc)
      {
         if(log.isDebugEnabled())
         {
            log.debug(exc.getMessage(), exc);
         }
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
    * @param autoVersion auto-version value
    * @param mixins list of mixins
    * @throws RepositoryException {@link RepositoryException}
    */
   private void createVersion(Node fileNode, InputStream inputStream, String autoVersion, List<String> mixins) throws RepositoryException
   {
      if (!fileNode.isNodeType(VersionHistoryUtils.MIX_VERSIONABLE))
      {
         if (fileNode.canAddMixin(VersionHistoryUtils.MIX_VERSIONABLE))
         {
            fileNode.addMixin(VersionHistoryUtils.MIX_VERSIONABLE);
            fileNode.getSession().save();
         }
         if (!(CHECKIN_CHECKOUT.equals(autoVersion)))
         {
            fileNode.checkin();
            fileNode.getSession().save();
         }
      }
      if (CHECKIN_CHECKOUT.equals(autoVersion))
      {
         fileNode.checkin();
         fileNode.checkout();
         fileNode.getSession().save();
         updateContent(fileNode, inputStream, mixins);
         fileNode.getSession().save();
      }
      else
      {
         createVersion(fileNode, inputStream, mixins);
      }
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
      if (CHECKOUT.equals(autoVersion))
      {
         updateContent(fileNode, inputStream, mixins);
      }
      else if (CHECKOUT_CHECKIN.equals(autoVersion))
      {
         updateContent(fileNode, inputStream, mixins);
         fileNode.getSession().save();
         fileNode.checkin();
      }
      fileNode.getSession().save();
   }

   private boolean isVersionSupported(String nodePath, String workspaceName, MultivaluedMap<String, String> allowedAutoVersionPath)
   {
      if (StringUtils.isEmpty(nodePath) || allowedAutoVersionPath.isEmpty())
      {
         return false;
      }

      List<String> paths = allowedAutoVersionPath.get(workspaceName);

      if(paths == null)
      {
         return false;
      }
      
      for (String p : paths)
      {
         if (!StringUtils.isEmpty(p) && nodePath.startsWith(p))
         {
            return true;
         }
      }
      return false;
   }

}
