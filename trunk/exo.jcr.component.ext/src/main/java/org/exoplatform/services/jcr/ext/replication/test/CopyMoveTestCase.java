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
package org.exoplatform.services.jcr.ext.replication.test;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: CopyMoveTestCase.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class CopyMoveTestCase extends BaseReplicationTestCase
{

   /**
    * The apache logger.
    */
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.CopyMoveTestCase");

   /**
    * CopyMoveTestCase constructor.
    * 
    * @param repositoryService
    *          the RepositoryService.
    * @param reposytoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    */
   public CopyMoveTestCase(RepositoryService repositoryService, String reposytoryName, String workspaceName,
      String userName, String password)
   {
      super(repositoryService, reposytoryName, workspaceName, userName, password);
      log.info("CopyMoveTestCase inited");
   }

   /**
    * workspaceCopy.
    * 
    * @param srcRepoPath
    *          source repository path
    * @param nodeName
    *          source node name
    * @param destNodeName
    *          destination node name
    * @param contentSize
    *          content size
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer workspaceCopy(String srcRepoPath, String nodeName, String destNodeName, long contentSize)
   {
      StringBuffer sb = new StringBuffer();

      // add source node
      byte[] buf = new byte[BUFFER_SIZE];

      File tempFile = null;
      try
      {
         tempFile = PrivilegedFileHelper.createTempFile("tempF", "_");
         FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);

         for (int i = 0; i < buf.length; i++)
            buf[i] = (byte)(i % Byte.MAX_VALUE);

         for (long i = 0; i < contentSize / BUFFER_SIZE; i++)
            fos.write(buf);
         fos.write(buf, 0, (int)(contentSize % BUFFER_SIZE));
         fos.close();

         Node srcNode = addNodePath(srcRepoPath).addNode(nodeName, "nt:file");
         Node contentNode = srcNode.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:encoding", "UTF-8");
         contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile));
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
         contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

         session.save();

         String normalizedSrcPath = getNormalizePath(srcRepoPath) + "/" + nodeName;
         String normalizedDestPath = getNormalizePath(srcRepoPath) + "/" + destNodeName;

         session.getWorkspace().copy(normalizedSrcPath, normalizedDestPath);
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't copy: ", e);
         sb.append("fail");
      }
      catch (IOException e)
      {
         log.error("Can't copy: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * workspaceMove.
    * 
    * @param srcRepoPath
    *          source repository path
    * @param nodeName
    *          source node name
    * @param destNodeName
    *          destination node name
    * @param contentSize
    *          content size
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer workspaceMove(String srcRepoPath, String nodeName, String destNodeName, long contentSize)
   {
      StringBuffer sb = new StringBuffer();

      // add source node
      byte[] buf = new byte[BUFFER_SIZE];

      File tempFile = null;
      try
      {
         tempFile = PrivilegedFileHelper.createTempFile("tempF", "_");
         FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);

         for (int i = 0; i < buf.length; i++)
            buf[i] = (byte)(i % Byte.MAX_VALUE);

         for (long i = 0; i < contentSize / BUFFER_SIZE; i++)
            fos.write(buf);
         fos.write(buf, 0, (int)(contentSize % BUFFER_SIZE));
         fos.close();

         Node srcNode = addNodePath(srcRepoPath).addNode(nodeName, "nt:file");
         Node contentNode = srcNode.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:encoding", "UTF-8");
         contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile));
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
         contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

         session.save();

         String normalizedSrcPath = getNormalizePath(srcRepoPath) + "/" + nodeName;
         String normalizedDestPath = getNormalizePath(srcRepoPath) + "/" + destNodeName;

         session.getWorkspace().move(normalizedSrcPath, normalizedDestPath);
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't copy: ", e);
         sb.append("fail");
      }
      catch (IOException e)
      {
         log.error("Can't copy: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * sessionMove.
    * 
    * @param srcRepoPath
    *          source repository path
    * @param nodeName
    *          source node name
    * @param destNodeName
    *          destination node name
    * @param contentSize
    *          content size
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer sessionMove(String srcRepoPath, String nodeName, String destNodeName, long contentSize)
   {
      StringBuffer sb = new StringBuffer();

      // add source node
      byte[] buf = new byte[BUFFER_SIZE];

      File tempFile = null;
      try
      {
         tempFile = PrivilegedFileHelper.createTempFile("tempF", "_");
         FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);

         for (int i = 0; i < buf.length; i++)
            buf[i] = (byte)(i % Byte.MAX_VALUE);

         for (long i = 0; i < contentSize / BUFFER_SIZE; i++)
            fos.write(buf);
         fos.write(buf, 0, (int)(contentSize % BUFFER_SIZE));
         fos.close();

         Node srcNode = addNodePath(srcRepoPath).addNode(nodeName, "nt:file");
         Node contentNode = srcNode.addNode("jcr:content", "nt:resource");
         contentNode.setProperty("jcr:encoding", "UTF-8");
         contentNode.setProperty("jcr:data", PrivilegedFileHelper.fileInputStream(tempFile));
         contentNode.setProperty("jcr:mimeType", "application/octet-stream");
         contentNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(Calendar.getInstance()));

         session.save();

         String normalizedSrcPath = getNormalizePath(srcRepoPath) + "/" + nodeName;
         String normalizedDestPath = getNormalizePath(srcRepoPath) + "/" + destNodeName;

         session.move(normalizedSrcPath, normalizedDestPath);
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't copy: ", e);
         sb.append("fail");
      }
      catch (IOException e)
      {
         log.error("Can't copy: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * checkCopyMoveNode.
    * 
    * @param srcRepoPath
    *          source repository path
    * @param nodeName
    *          source node name
    * @param destNodeName
    *          destination node name
    * @param contentSize
    *          content size
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer checkCopyMoveNode(String srcRepoPath, String nodeName, String destNodeName, long contentSize)
   {
      StringBuffer sb = new StringBuffer();

      String normalizePath = getNormalizePath(srcRepoPath) + "/" + destNodeName;
      try
      {
         Node ntFile = (Node)session.getItem(normalizePath);

         InputStream stream = ntFile.getNode("jcr:content").getProperty("jcr:data").getStream();

         byte buf[] = new byte[BUFFER_SIZE];
         long length = 0;
         int lenReads = 0;
         while ((lenReads = stream.read(buf)) > 0)
            length += lenReads;

         if (length == contentSize)
            sb.append("ok");
         else
            sb.append("fail");
      }
      catch (PathNotFoundException e)
      {
         log.error("Can't get node : " + normalizePath, e);
         sb.append("fail");
      }
      catch (RepositoryException e)
      {
         log.error("CheckNtFile fail", e);
         sb.append("fail");
      }
      catch (Exception e)
      {
         log.error("CheckNtFile fail", e);
         sb.append("fail");
      }

      return sb;
   }

}
