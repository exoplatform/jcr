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
import javax.jcr.RepositoryException;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: VersionTestCase.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class VersionTestCase extends BaseReplicationTestCase
{

   /**
    * The Exo logger.
    */
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.VersionTestCase");

   /**
    * VersionTestCase constructor.
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
   public VersionTestCase(RepositoryService repositoryService, String reposytoryName, String workspaceName,
      String userName, String password)
   {
      super(repositoryService, reposytoryName, workspaceName, userName, password);
      log.info("NtFileTestCase inited");
   }

   /**
    * addVersionNode.
    * 
    * @param repoPath
    *          repository path
    * @param value
    *          the String value
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer addVersionNode(String repoPath, String value)
   {
      StringBuffer sb = new StringBuffer();

      try
      {
         Node srcVersionNode = addNodePath(repoPath);
         srcVersionNode.setProperty("jcr:data", value);
         srcVersionNode.addMixin("mix:versionable");
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't create versioning node: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * checkVersionNode.
    * 
    * @param repoPath
    *          repository path
    * @param checkedValue
    *          the checked String value
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer checkVersionNode(String repoPath, String checkedValue)
   {
      StringBuffer sb = new StringBuffer();

      String normalizePath = getNormalizePath(repoPath);
      try
      {
         Node destVersionNode = (Node)session.getItem(normalizePath);
         if (checkedValue.equals(destVersionNode.getProperty("jcr:data").getString()))
            sb.append("ok");
         else
            sb.append("fail");

      }
      catch (RepositoryException e)
      {
         log.error("Can't create versioning node: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * addNewVersion.
    * 
    * @param repoPath
    *          repository path
    * @param newValue
    *          the new String value
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer addNewVersion(String repoPath, String newValue)
   {
      StringBuffer sb = new StringBuffer();

      String normalizePath = getNormalizePath(repoPath);
      try
      {
         Node srcVersionNode = (Node)session.getItem(normalizePath);

         srcVersionNode.checkin();
         session.save();

         srcVersionNode.checkout();
         srcVersionNode.setProperty("jcr:data", newValue);
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't add versioning node value: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * restorePreviousVersion.
    * 
    * @param repoPath
    *          repository path
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer restorePreviousVersion(String repoPath)
   {
      StringBuffer sb = new StringBuffer();

      String normalizePath = getNormalizePath(repoPath);
      try
      {
         Node srcVersionNode = (Node)session.getItem(normalizePath);

         Version baseVersion = srcVersionNode.getBaseVersion();
         srcVersionNode.restore(baseVersion, true);
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't restore previous version: ", e);
         sb.append("fail");
      }

      return sb;
   }

   /**
    * restoreBaseVersion.
    * 
    * @param repoPath
    *          repository path
    * @return StringBuffer return the responds {'ok', 'fail'}
    */
   public StringBuffer restoreBaseVersion(String repoPath)
   {
      StringBuffer sb = new StringBuffer();

      String normalizePath = getNormalizePath(repoPath);
      try
      {
         Node srcVersionNode = (Node)session.getItem(normalizePath);

         Version baseVersion1 = srcVersionNode.getBaseVersion();
         Version[] predesessors = baseVersion1.getPredecessors();
         Version restoreToBaseVersion = predesessors[0];

         srcVersionNode.restore(restoreToBaseVersion, true);
         session.save();

         sb.append("ok");
      }
      catch (RepositoryException e)
      {
         log.error("Can't restore previous version: ", e);
         sb.append("fail");
      }

      return sb;
   }
}
