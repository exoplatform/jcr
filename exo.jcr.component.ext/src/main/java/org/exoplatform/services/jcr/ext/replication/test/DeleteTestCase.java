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

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: DeleteTestCase.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class DeleteTestCase extends BaseReplicationTestCase
{

   /**
    * The apache logger.
    */
   private static final Log log = ExoLogger.getLogger(DeleteTestCase.class);

   /**
    * DeleteTestCase constructor.
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
   public DeleteTestCase(RepositoryService repositoryService, String reposytoryName, String workspaceName,
      String userName, String password)
   {
      super(repositoryService, reposytoryName, workspaceName, userName, password);
      log.info("DeleteTestCase inited");
   }

   /**
    * delete.
    * 
    * @param repoPath
    *          repository path
    * @param nodeName
    *          node name
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer delete(String repoPath, String nodeName)
   {
      StringBuffer sb = new StringBuffer();

      try
      {

         String normalizedPath = getNormalizePath(repoPath);

         Node needDeleteNode = (Node)session.getItem(normalizedPath);
         needDeleteNode.getNode(nodeName).remove();

         session.save();

         sb.append("ok");
      }
      catch (Exception e)
      {
         log.error("Can't save nt:file : ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * checkDelete.
    * 
    * @param repoPath
    *          repository path
    * @param nodeName
    *          node name
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer checkDelete(String repoPath, String nodeName)
   {
      StringBuffer sb = new StringBuffer();

      String normalizedPath = null;

      try
      {
         normalizedPath = getNormalizePath(repoPath) + "/" + nodeName;

         Node needDeleteNode = (Node)session.getItem(normalizedPath);

         sb.append("fail");
         log.error("The node has not been deleted : " + normalizedPath);
      }
      catch (PathNotFoundException e)
      {
         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Has not checked : " + normalizedPath, e);
      }

      return sb;
   }
}
