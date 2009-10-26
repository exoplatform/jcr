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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.replication.ReplicationService;
import org.exoplatform.services.jcr.ext.replication.test.bandwidth.BandwidthAllocationTestCase;
import org.exoplatform.services.jcr.ext.replication.test.concurrent.ConcurrentModificationTestCase;
import org.exoplatform.services.jcr.ext.replication.test.priority.BasePriorityTestCase;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ReplicationTestService.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

@Path("/replication-test/")
@Produces("text/plain")
public class ReplicationTestService implements ResourceContainer
{

   /**
    * Definition the constants to ReplicationTestService.
    */
   public final class Constants
   {
      /**
       * The base path to this service.
       */
      public static final String BASE_URL = "/rest/replication-test";

      /**
       * Definition the operation types.
       */
      public final class OperationType
      {
         /**
          * Add nt:file operation.
          */
         public static final String ADD_NT_FILE = "addNTFile";

         /**
          * Check nt:file operation.
          */
         public static final String CHECK_NT_FILE = "checkNTFile";

         /**
          * Start backup.
          */
         public static final String START_BACKUP = "startBackup";

         /**
          * Set the lock to node.
          */
         public static final String SET_LOCK = "lock";

         /**
          * Check the lock on node.
          */
         public static final String CECK_LOCK = "checkLock";

         /**
          * Add the versionable node.
          */
         public static final String ADD_VERSIONODE = "addVersionNode";

         /**
          * Check the versionable node.
          */
         public static final String CHECK_VERSION_NODE = "checkVersionNode";

         /**
          * Add new version to versionable node.
          */
         public static final String ADD_NEW_VERSION = "addNewVersion";

         /**
          * Restore the previous version.
          */
         public static final String RESTORE_RPEVIOUS_VERSION = "restorePreviousVersion";

         /**
          * Restore the base version.
          */
         public static final String RESTORE_BASE_VERSION = "restoreBaseVersion";

         /**
          * Delete the node.
          */
         public static final String DELETE = "delete";

         /**
          * Check the deleted node.
          */
         public static final String CHECK_DELETE = "checkDelete";

         /**
          * The copy node by workspace.
          */
         public static final String WORKSPACE_COPY = "workspaceCopy";

         /**
          * The move node by workspace.
          */
         public static final String WORKSPASE_MOVE = "workspaceMove";

         /**
          * The move node by session.
          */
         public static final String SESSION_MOVE = "sessionMove";

         /**
          * Check the copy or move node.
          */
         public static final String CHECK_COPY_MOVE_NODE = "checkCopyMoveNode";

         /**
          * Disconnect the cluster node.
          */
         public static final String DISCONNECT_CLUSTER_NODE = "disconnectClusterNode";

         /**
          * Disconnect by ID the cluster node.
          */
         public static final String DISCONNECT_CLUSTER_NODE_BY_ID = "disconnectClusterNodeById";

         /**
          * Allow the connect the cluster node.
          */
         public static final String ALLOW_CONNECT = "allowConnect";

         /**
          * The forced allow the connect the cluster node.
          */
         public static final String ALLOW_CONNECT_FORCED = "allowConnectForced";

         /**
          * Check 'read-only' the workspace.
          */
         public static final String WORKSPACE_IS_READ_ONLY = "workspaceIsReadOnly";

         /**
          * Create content in workspace.
          */
         public static final String CREATE_CONTENT = "createContent";

         /**
          * Compare data in workspace.
          */
         public static final String COMPARE_DATA = "compareData";

         /**
          * Start the thread updater.
          */
         public static final String START_THREAD_UPDATER = "startThreadUpdater";

         /**
          * Create the base node.
          */
         public static final String CREATE_BASE_NODE = "createBaseNode";

         /**
          * Add empty node.
          */
         public static final String ADD_EMPTY_NODE = "addEmptyNode";

         /**
          * Add only string property to existing node.
          */
         public static final String ADD_STRING_PROPETY_ONLY = "addStringPropertyOnly";

         /**
          * Add only binary property to existing node.
          */
         public static final String ADD_BINARY_PROPERTY_ONLY = "addBinaryPropertyOnly";

         /**
          * OperationType constructor.
          */
         private OperationType()
         {

         }
      }

      /**
       * Constants constructor.
       */
      private Constants()
      {
      }
   }

   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("ext.ReplicationTestService");

   /**
    * The repository service.
    */
   private RepositoryService repositoryService;

   /**
    * The backup manager.
    */
   private BackupManager backupManager;

   /**
    * ReplicationTestService constructor.
    * 
    * @param repoService
    *          the RepositoryService
    * @param replicationService
    *          the ReplicationService
    * @param backupManager
    *          the BackupManager
    * @param params
    *          the configuration parameters
    */
   public ReplicationTestService(RepositoryService repoService, ReplicationService replicationService,
      BackupManager backupManager, InitParams params)
   {
      repositoryService = repoService;
      this.backupManager = backupManager;

      log.info("ReplicationTestService inited");
   }

   /**
    * ReplicationTestService constructor.
    * 
    * @param repoService
    *          the RepositoryService
    * @param backupManager
    *          the BackupManager
    * @param params
    *          the configuration parameters
    */
   public ReplicationTestService(RepositoryService repoService, BackupManager backupManager, InitParams params)
   {
      this(repoService, null, backupManager, params);
   }

   /**
    * addNTFile.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param fileName
    *          the file name
    * @param fileSize
    *          the file size
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{fileName}/{fileSize}/addNTFile")
   public Response addNTFile(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("fileName") String fileName, @PathParam("fileSize") Long fileSize)
   {
      NtFileTestCase ntFileTestCase =
         new NtFileTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = ntFileTestCase.addNtFile(repoPath, fileName, fileSize);

      return Response.ok(sb.toString()).build();
   }

   /**
    * checkNTFile.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param fileName
    *          the file name
    * @param fileSize
    *          the file size
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{fileName}/{fileSize}/checkNTFile")
   public Response checkNTFile(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("fileName") String fileName, @PathParam("fileSize") Long fileSize)
   {
      NtFileTestCase ntFileTestCase =
         new NtFileTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = ntFileTestCase.checkNtFile(repoPath, fileName, fileSize);

      return Response.ok(sb.toString()).build();
   }

   /**
    * startBackup.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param incementalPeriod
    *          the period for incremental backup (seconds)
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{incementalPeriod}/startBackup")
   public Response startBackup(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("incementalPeriod") Long incementalPeriod)
   {
      BackupConfig config = new BackupConfig();
      config.setBackupType(BackupManager.FULL_AND_INCREMENTAL);
      config.setRepository(repositoryName);
      config.setWorkspace(workspaceName);
      config.setBackupDir(backupManager.getBackupDirectory());
      config.setIncrementalJobPeriod(incementalPeriod);

      String result = "ok";

      try
      {
         backupManager.startBackup(config);
      }
      catch (Exception e)
      {
         result = "fail";
         log.error("Can't start backup", e);
      }

      return Response.ok(result).build();
   }

   /**
    * lock.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/lock")
   public Response lock(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath)
   {
      LockTestCase lockTestCase =
         new LockTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = lockTestCase.lock(repoPath);

      return Response.ok(sb.toString()).build();
   }

   /**
    * checkLock.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/checkLock")
   public Response checkLock(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath)
   {
      LockTestCase lockTestCase =
         new LockTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = lockTestCase.isLocked(repoPath);

      return Response.ok(sb.toString()).build();
   }

   /**
    * addVersionNode.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param value
    *          value to versionable node
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{value}/addVersionNode")
   public Response addVersionNode(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath, @PathParam("value") String value)
   {
      VersionTestCase versionTestCase =
         new VersionTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = versionTestCase.addVersionNode(repoPath, value);

      return Response.ok(sb.toString()).build();
   }

   /**
    * checkVersionNode.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param checkedValue
    *          checking value to versionable node
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{checkedValue}/checkVersionNode")
   public Response checkVersionNode(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("checkedValue") String checkedValue)
   {
      VersionTestCase versionTestCase =
         new VersionTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = versionTestCase.checkVersionNode(repoPath, checkedValue);

      return Response.ok(sb.toString()).build();
   }

   /**
    * addNewVersion.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param newValue
    *          new value to versionable node
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{newValue}/addNewVersion")
   public Response addNewVersion(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("newValue") String newValue)
   {
      VersionTestCase versionTestCase =
         new VersionTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = versionTestCase.addNewVersion(repoPath, newValue);

      return Response.ok(sb.toString()).build();
   }

   /**
    * restorePreviousVersion.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/restorePreviousVersion")
   public Response restorePreviousVersion(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath)
   {
      VersionTestCase versionTestCase =
         new VersionTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = versionTestCase.restorePreviousVersion(repoPath);

      return Response.ok(sb.toString()).build();
   }

   /**
    * restoreBaseVersion.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/restoreBaseVersion")
   public Response restoreBaseVersion(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath)
   {
      VersionTestCase versionTestCase =
         new VersionTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = versionTestCase.restoreBaseVersion(repoPath);

      return Response.ok(sb.toString()).build();
   }

   /**
    * delete.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the name of deleting node
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{nodeName}/delete")
   public Response delete(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("nodeName") String nodeName)
   {
      DeleteTestCase deleteTestCase =
         new DeleteTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = deleteTestCase.delete(repoPath, nodeName);

      return Response.ok(sb.toString()).build();
   }

   /**
    * checkDelete.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the name of deleted node
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{nodeName}/checkDelete")
   public Response checkDelete(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("nodeName") String nodeName)
   {
      DeleteTestCase deleteTestCase =
         new DeleteTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = deleteTestCase.checkDelete(repoPath, nodeName);

      return Response.ok(sb.toString()).build();
   }

   /**
    * workspaceCopy.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param srcRepoPath
    *          the source repository path
    * @param nodeName
    *          the source node name
    * @param destNodeName
    *          the destination node name
    * @param contentSize
    *          the content size
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{srcRepoPath}/{nodeName}/{destNodeName}/{contentSize}/workspaceCopy")
   public Response workspaceCopy(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("srcRepoPath") String srcRepoPath,
      @PathParam("nodeName") String nodeName, @PathParam("destNodeName") String destNodeName,
      @PathParam("contentSize") Long contentSize)
   {
      CopyMoveTestCase copyMoveTestCase =
         new CopyMoveTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = copyMoveTestCase.workspaceCopy(srcRepoPath, nodeName, destNodeName, contentSize);

      return Response.ok(sb.toString()).build();
   }

   /**
    * workspaceMove.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param srcRepoPath
    *          the source repository path
    * @param nodeName
    *          the source node name
    * @param destNodeName
    *          the destination node name
    * @param contentSize
    *          the content size
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{srcRepoPath}/{nodeName}/{destNodeName}/{contentSize}/workspaceMove")
   public Response workspaceMove(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("srcRepoPath") String srcRepoPath,
      @PathParam("nodeName") String nodeName, @PathParam("destNodeName") String destNodeName,
      @PathParam("contentSize") Long contentSize)
   {
      CopyMoveTestCase copyMoveTestCase =
         new CopyMoveTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = copyMoveTestCase.workspaceMove(srcRepoPath, nodeName, destNodeName, contentSize);

      return Response.ok(sb.toString()).build();
   }

   /**
    * sessionMove.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param srcRepoPath
    *          the source repository path
    * @param nodeName
    *          the source node name
    * @param destNodeName
    *          the destination node name
    * @param contentSize
    *          the content size
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{srcRepoPath:.*}/{nodeName}/{destNodeName}/{contentSize}/sessionMove")
   public Response sessionMove(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("srcRepoPath") String srcRepoPath,
      @PathParam("nodeName") String nodeName, @PathParam("destNodeName") String destNodeName,
      @PathParam("contentSize") Long contentSize)
   {
      CopyMoveTestCase copyMoveTestCase =
         new CopyMoveTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = copyMoveTestCase.sessionMove(srcRepoPath, nodeName, destNodeName, contentSize);

      return Response.ok(sb.toString()).build();
   }

   /**
    * checkCopyMoveNode.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param srcRepoPath
    *          the source repository path
    * @param nodeName
    *          the source node name
    * @param destNodeName
    *          the destination node name
    * @param contentSize
    *          the content size
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{srcRepoPath:.*}/{nodeName}/{destNodeName}/{contentSize}/checkCopyMoveNode")
   public Response checkCopyMoveNode(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("srcRepoPath") String srcRepoPath,
      @PathParam("nodeName") String nodeName, @PathParam("destNodeName") String destNodeName,
      @PathParam("contentSize") Long contentSize)
   {
      CopyMoveTestCase copyMoveTestCase =
         new CopyMoveTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = copyMoveTestCase.checkCopyMoveNode(srcRepoPath, nodeName, destNodeName, contentSize);

      return Response.ok(sb.toString()).build();
   }

   /**
    * disconnectClusterNode.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/disconnectClusterNode")
   public Response disconnectClusterNode(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password)
   {
      BasePriorityTestCase priorityTestCase =
         new BasePriorityTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = priorityTestCase.disconnectClusterNode();

      return Response.ok(sb.toString()).build();
   }

   /**
    * disconnectClusterNodeById.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param id
    *          the id
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{id}/disconnectClusterNodeById")
   public Response disconnectClusterNodeById(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("id") Integer id)
   {
      BasePriorityTestCase priorityTestCase =
         new BasePriorityTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = priorityTestCase.disconnectClusterNode(id);

      return Response.ok(sb.toString()).build();
   }

   /**
    * allowConnect.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/allowConnect")
   public Response allowConnect(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password)
   {
      BasePriorityTestCase priorityTestCase =
         new BasePriorityTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = priorityTestCase.allowConnect();

      return Response.ok(sb.toString()).build();
   }

   /**
    * allowConnectForced.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/allowConnectForced")
   public Response allowConnectForced(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password)
   {
      BasePriorityTestCase priorityTestCase =
         new BasePriorityTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = priorityTestCase.allowConnectForced();

      return Response.ok(sb.toString()).build();
   }

   /**
    * workspaceIsReadOnly.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/workspaceIsReadOnly")
   public Response workspaceIsReadOnly(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password)
   {
      BasePriorityTestCase priorityTestCase =
         new BasePriorityTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = priorityTestCase.isReadOnly(workspaceName);

      return Response.ok(sb.toString()).build();
   }

   /**
    * createContent.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param fileName
    *          the file name
    * @param iterations
    *          how many iterations for simple content
    * @param simpleContent
    *          the simple content
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{fileName}/{iterations}/{simpleContent}/createContent")
   public Response createContent(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("fileName") String fileName, @PathParam("iterations") Long iterations,
      @PathParam("simpleContent") String simpleContent)
   {
      ConcurrentModificationTestCase concurrentModificationTestCase =
         new ConcurrentModificationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = concurrentModificationTestCase.createContent(repoPath, fileName, iterations, simpleContent);

      return Response.ok(sb.toString()).build();
   }

   /**
    * compareData.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param srcRepoPath
    *          the source repository path
    * @param srcFileName
    *          the source file name
    * @param destRepoPath
    *          the destination repository path
    * @param destFileName
    *          the destination file name
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{srcRepoPath:.*}/{srcFileName}/{destRepoPath:.*}/{destFileName}/compareData")
   public Response compareData(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("srcRepoPath") String srcRepoPath,
      @PathParam("srcFileName") String srcFileName, @PathParam("destRepoPath") String destRepoPath,
      @PathParam("destFileName") String destFileName)
   {
      ConcurrentModificationTestCase concurrentModificationTestCase =
         new ConcurrentModificationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb =
         concurrentModificationTestCase.compareData(srcRepoPath, srcFileName, destRepoPath, destFileName);

      return Response.ok(sb.toString()).build();
   }

   /**
    * startThreadUpdater.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param srcRepoPath
    *          the source repository path
    * @param srcFileName
    *          the source file name
    * @param destRepoPath
    *          the destination repository path
    * @param destFileName
    *          the destination file name
    * @param iterations
    *          how many iterations the thread
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{srcRepoPath:.*}/{srcFileName}/{destRepoPath:.*}/{destFileName}/{iterations}/startThreadUpdater")
   public Response startThreadUpdater(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("srcRepoPath") String srcRepoPath,
      @PathParam("srcFileName") String srcFileName, @PathParam("destRepoPath") String destRepoPath,
      @PathParam("destFileName") String destFileName, @PathParam("iterations") Long iterations)
   {
      ConcurrentModificationTestCase concurrentModificationTestCase =
         new ConcurrentModificationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb =
         concurrentModificationTestCase.startThreadUpdater(srcRepoPath, srcFileName, destRepoPath, destFileName,
            iterations);

      return Response.ok(sb.toString()).build();
   }

   /**
    * createBaseNode.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath}/{nodeName}/createBaseNode")
   public Response createBaseNode(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("nodeName") String nodeName)
   {
      BandwidthAllocationTestCase bandwidthAllocationTestCase =
         new BandwidthAllocationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = bandwidthAllocationTestCase.createBaseNode(repoPath, nodeName);

      return Response.ok(sb.toString()).build();
   }

   /**
    * addEmptyNode.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @param iterations
    *          how many adding the empty node
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{nodeName}/{iterations}/addEmptyNode")
   public Response addEmptyNode(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("nodeName") String nodeName, @PathParam("iterations") Long iterations)
   {
      BandwidthAllocationTestCase bandwidthAllocationTestCase =
         new BandwidthAllocationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = bandwidthAllocationTestCase.addEmptyNode(repoPath, nodeName, iterations);

      return Response.ok(sb.toString()).build();
   }

   /**
    * addStringPropertyOnly.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @param size
    *          the size of string property
    * @param iterations
    *          how many adding the string property
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{nodeName}/{size}/{iterations}/addEmptyNode")
   public Response addStringPropertyOnly(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("nodeName") String nodeName, @PathParam("size") Long size, @PathParam("iterations") Long iterations)
   {
      BandwidthAllocationTestCase bandwidthAllocationTestCase =
         new BandwidthAllocationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = bandwidthAllocationTestCase.addStringPropertyOnly(repoPath, nodeName, size, iterations);

      return Response.ok(sb.toString()).build();
   }

   /**
    * addBinaryPropertyOnly.
    * 
    * @param repositoryName
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @param userName
    *          the user name
    * @param password
    *          the password
    * @param repoPath
    *          the repository path
    * @param nodeName
    *          the node name
    * @param size
    *          the size of binary property
    * @param iterations
    *          how many adding the binary property
    * @return Response return the response
    */
   @GET
   @Path("/{repositoryName}/{workspaceName}/{userName}/{password}/{repoPath:.*}/{nodeName}/{size}/{iterations}/addBinaryPropertyOnly")
   public Response addBinaryPropertyOnly(@PathParam("repositoryName") String repositoryName,
      @PathParam("workspaceName") String workspaceName, @PathParam("userName") String userName,
      @PathParam("password") String password, @PathParam("repoPath") String repoPath,
      @PathParam("nodeName") String nodeName, @PathParam("size") Long size, @PathParam("iterations") Long iterations)
   {
      BandwidthAllocationTestCase bandwidthAllocationTestCase =
         new BandwidthAllocationTestCase(repositoryService, repositoryName, workspaceName, userName, password);
      StringBuffer sb = bandwidthAllocationTestCase.addBinaryPropertyOnly(repoPath, nodeName, size, iterations);

      return Response.ok(sb.toString()).build();
   }
}
