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
package org.exoplatform.services.jcr.ext.backup.server;

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.AbstractBackupTestCase;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.ext.backup.server.bean.BackupConfigBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.BackupServiceInfoBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.DetailedInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfoList;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 21.04.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: HTTPBackupAgentTest.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class HTTPBackupAgentTest extends AbstractBackupTestCase
{

   public static String HTTP_BACKUP_AGENT_PATH = HTTPBackupAgent.Constants.BASE_URL;

   public void testInfo() throws Exception
   {
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.BACKUP_SERVICE_INFO));

      assertEquals(200, cres.getStatus());

      BackupServiceInfoBean info =
         (BackupServiceInfoBean)getObject(BackupServiceInfoBean.class, cres.responseWriter.getBody());

      BackupManager backupManager = (BackupManager)container.getComponentInstanceOfType(BackupManager.class);

      assertNotNull(info);
      assertEquals(backupManager.getBackupDirectory().getAbsolutePath(), info.getBackupLogDir());
      assertEquals(backupManager.getFullBackupType(), info.getFullBackupType());
      assertEquals(backupManager.getIncrementalBackupType(), info.getIncrementalBackupType());
      assertEquals(backupManager.getDefaultIncrementalJobPeriod(), info.getDefaultIncrementalJobPeriod().longValue());
   }

   public void testDropWorkspace() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      rInfo.session.logout();

      int intialCount = repositoryService.getRepository(rInfo.rName).getConfiguration().getWorkspaceEntries().size();

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.DROP_WORKSPACE + "/"
            + rInfo.rName + "/" + rInfo.wsName + "/true"));

      assertEquals(200, cres.getStatus());
      assertEquals(intialCount - 1, repositoryService.getRepository(rInfo.rName).getConfiguration()
         .getWorkspaceEntries().size());
   }

   public void testStart() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();

      BackupConfigBean configBean =
         new BackupConfigBean(BackupManager.FULL_AND_INCREMENTAL, backupDir.getPath(), 10000l);
      
      TesterContainerResponce cres = makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH
         + HTTPBackupAgent.Constants.OperationType.START_BACKUP + "/" + rInfo.rName + "/" + rInfo.wsName), configBean);

      assertEquals(200, cres.getStatus());
   }

   public void testStartBackupRepository() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();

      BackupConfigBean configBean =
         new BackupConfigBean(BackupManager.FULL_AND_INCREMENTAL, backupDir.getPath(), 10000l);

      TesterContainerResponce cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.START_BACKUP_REPOSITORY + "/" + rInfo.rName), configBean);

      assertEquals(200, cres.getStatus());
   }

   public void testInfoBackup() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupWorkspace(rInfo);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertTrue((ShortInfo.CURRENT == info.getType().intValue() || ShortInfo.COMPLETED == info.getType().intValue()));
      assertEquals(BackupChain.FINISHED, info.getState().intValue());
      assertEquals(rInfo.wsName, info.getWorkspaceName());
   }

   public void testInfoBackupRepository() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupRepository(rInfo);
      
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_REPOSITORY_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(RepositoryBackupChain.FINISHED, info.getState().intValue());
   }

   public void testInfoBackupOnWorkspace() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupWorkspace(rInfo);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_INFO_ON_WS + "/" + rInfo.rName
            + "/" + rInfo.wsName));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);
      assertNotNull(info);

      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(BackupChain.FINISHED, info.getState().intValue());
      assertEquals(rInfo.wsName, info.getWorkspaceName());
   }

   public void testInfoBackupOnRepository() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupRepository(rInfo);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_AND_COMPLETED_BACKUPS_REPOSITORY_INFO + "/" + rInfo.rName));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(RepositoryBackupChain.FINISHED, info.getState().intValue());
   }

   public void testInfoBackupCurrent() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupWorkspace(rInfo);

      // get current backup
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(BackupChain.FINISHED, info.getState().intValue());
      assertEquals(rInfo.wsName, info.getWorkspaceName());

      // get current backup by id
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_INFO + "/" + info.getBackupId()));

      assertEquals(200, cres.getStatus());

      DetailedInfo dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, dInfo.getType().intValue());
      assertEquals(RepositoryBackupChain.FINISHED, dInfo.getState().intValue());
      assertEquals(rInfo.wsName, dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());
   }

   public void testInfoBackupRepositoryCurrent() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupRepository(rInfo);

      // get current backup
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_REPOSITORY_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);
      assertNotNull(info);

      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, info.getType().intValue());
      assertEquals(RepositoryBackupChain.FINISHED, info.getState().intValue());

      // get current backup by id
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_REPOSITORY_INFO + "/"
            + info.getBackupId()));

      assertEquals(200, cres.getStatus());

      DetailedInfo dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.CURRENT, dInfo.getType().intValue());
      assertEquals(RepositoryBackupChain.FINISHED, dInfo.getState().intValue());
      assertNotNull(dInfo.getBackupConfig());
   }

   public void testStop() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupWorkspace(rInfo);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);
      assertNotNull(info);
      assertEquals(rInfo.wsName, info.getWorkspaceName());

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.STOP_BACKUP + "/"
            + info.getBackupId()));

      assertEquals(200, cres.getStatus());
   }

   public void testStopBackupRepository() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      backupRepository(rInfo);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_BACKUPS_REPOSITORY_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);
      assertNotNull(info);

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.STOP_BACKUP_REPOSITORY
            + "/" + info.getBackupId()));
      assertEquals(200, cres.getStatus());
   }

   public void testInfoBackupCompleted() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      BackupChain bch = backupWorkspace(rInfo);
      backup.stopBackup(bch);

      // get completed backup
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.COMPLETED, info.getType().intValue());
      assertEquals(0, info.getState().intValue());
      assertEquals(rInfo.wsName, info.getWorkspaceName());
      
      // get completed backup by id
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_OR_COMPLETED_BACKUP_INFO + "/" + info.getBackupId()));

      assertEquals(200, cres.getStatus());

      DetailedInfo dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.COMPLETED, dInfo.getType().intValue());
      assertEquals(0, dInfo.getState().intValue());
      assertEquals(rInfo.wsName, dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());
   }

   public void testInfoBackupRepositoryCompleted() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      RepositoryBackupChain bch = backupRepository(rInfo);
      backup.stopBackup(bch);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.COMPLETED, info.getType().intValue());
      assertEquals(0, info.getState().intValue());
   }

   public void testGetDefaultWorkspaceConfig() throws Exception
   {
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_WORKSPACE_CONFIG));

      assertEquals(200, cres.getStatus());

      WorkspaceEntry defEntry = (WorkspaceEntry)getObject(WorkspaceEntry.class, cres.responseWriter.getBody());

      assertEquals(repository.getConfiguration().getDefaultWorkspaceName(), defEntry.getName());
   }

   public void testGetDefaultRepositoryConfig() throws Exception
   {
      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.GET_DEFAULT_REPOSITORY_CONFIG));

      assertEquals(200, cres.getStatus());

      RepositoryEntry defEntry = (RepositoryEntry)getObject(RepositoryEntry.class, cres.responseWriter.getBody());

      assertEquals(repository.getConfiguration().getName(), defEntry.getName());
   }

   public void testRestore() throws Exception
   {
      RepoInfo rInfo = createRepositoryAndGetSession();
      BackupChain bch = backupWorkspace(rInfo);
      backup.stopBackup(bch);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);

      assertNotNull(info);

      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);

      // restore in new workspace 
      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE + "/"
            + rInfo.rName + "/" + info.getBackupId()), wsEntry);

      waitWorkspaceRestore(rInfo.rName, wsEntry.getName());

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + wsEntry.getName()));

      assertEquals(200, cres.getStatus());

      DetailedInfo dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(wsEntry.getName(), dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());

      Session session = repositoryService.getRepository(rInfo.rName).login(credentials, wsEntry.getName());
      assertNotNull(session);
      assertNotNull(session.getRootNode());

      cres = makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORES));

      assertEquals(200, cres.getStatus());

      infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      assertNotNull(infoList);

      info = new ArrayList<ShortInfo>(infoList.getBackups()).get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, info.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, info.getState().intValue());
      assertEquals(wsEntry.getName(), info.getWorkspaceName());
      assertNotNull(info.getBackupId());
      
      // restore in existed workspace
      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE + "/"
            + rInfo.rName + "/" + info.getBackupId() + "/true"), wsEntry);

      assertEquals(200, cres.getStatus());
      waitWorkspaceRestore(rInfo.rName, wsEntry.getName());
      
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + wsEntry.getName()));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(wsEntry.getName(), dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());

      // restore in new workspace, "remove-existing" is false
      wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);

      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE + "/"
            + rInfo.rName + "/" + info.getBackupId() + "/false"), wsEntry);

      assertEquals(200, cres.getStatus());
      waitWorkspaceRestore(rInfo.rName, wsEntry.getName());

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + wsEntry.getName()));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(wsEntry.getName(), dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());

      info = getBackupInfo(list, rInfo.rName);

      // restore in existed workspace, "remove-existing" is true
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE + "/"
            + info.getBackupId() + "/true"));

      assertEquals(200, cres.getStatus());

      waitWorkspaceRestore(rInfo.rName, rInfo.wsName);

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + rInfo.wsName));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.wsName, dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());

      // restore in existed workspace by @backup-set-path", remove existing is true
      String backupSetPath = null;
      for (BackupChainLog bcl : backup.getBackupsLogs())
      {
         if (bcl.getBackupId().equals(info.getBackupId()))
         {
            backupSetPath = URLEncoder.encode(bcl.getBackupConfig().getBackupDir().getCanonicalPath(), "UTF-8");
            break;
         }
      }

      assertNotNull(backupSetPath);

      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET
            + "/" + rInfo.rName + "/" + "true" + "?backup-set-path=" + backupSetPath), wsEntry);

      assertEquals(200, cres.getStatus());

      waitWorkspaceRestore(rInfo.rName, wsEntry.getName());

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + wsEntry.getName()));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(wsEntry.getName(), dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());

      // restore in existed workspace by @backup-set-path", remove existing is true
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET
            + "/" + "true" + "?backup-set-path=" + backupSetPath));

      assertEquals(200, cres.getStatus());

      waitWorkspaceRestore(rInfo.rName, rInfo.wsName);

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + rInfo.wsName));

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.wsName, dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());

      // restore in existed workspace by @backup-set-path", remove existing is true
      removeWorkspaceFully(rInfo.rName, rInfo.wsName);

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET
            + "/" + "false" + "?backup-set-path=" + backupSetPath));

      assertEquals(200, cres.getStatus());

      waitWorkspaceRestore(rInfo.rName, rInfo.wsName);

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + rInfo.wsName));

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.wsName, dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());
      
      // restore in existed workspace by @backup-set-path", remove existing is true
      removeWorkspaceFully(rInfo.rName, wsEntry.getName());
      
      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET
            + "/" + rInfo.rName + "/" + "false" + "?backup-set-path=" + backupSetPath), wsEntry);

      waitWorkspaceRestore(rInfo.rName, rInfo.wsName);

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_WS + "/" + rInfo.rName + "/"
            + rInfo.wsName));

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobWorkspaceRestore.RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.wsName, dInfo.getWorkspaceName());
      assertNotNull(dInfo.getBackupConfig());
   }

   public void testRestoreRepository() throws Exception
   {
      // make backup, get backupId and backupSetPath
      RepoInfo rInfo = createRepositoryAndGetSession();
      RepositoryBackupChain bch = backupRepository(rInfo);
      backup.stopBackup(bch);

      TesterContainerResponce cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.COMPLETED_BACKUPS_REPOSITORY_INFO));

      assertEquals(200, cres.getStatus());

      ShortInfoList infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      List<ShortInfo> list = new ArrayList<ShortInfo>(infoList.getBackups());

      ShortInfo info = getBackupInfo(list, rInfo.rName);
      assertNotNull(info);
      
      String backupId = info.getBackupId();
      String backupSetPath = null;
      for (RepositoryBackupChainLog bcl : backup.getRepositoryBackupsLogs())
      {
         if (bcl.getBackupId().equals(backupId))
         {
            backupSetPath = URLEncoder.encode(bcl.getBackupConfig().getBackupDir().getCanonicalPath(), "UTF-8");
            break;
         }
      }
      assertNotNull(backupSetPath);

      // restore in new repository
      RepositoryEntry rEntry = helper.createRepositoryEntry(DatabaseStructureType.MULTI, rInfo.sysWsName, null);
      WorkspaceEntry wsEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
      wsEntry.setName(rInfo.wsName);
      rEntry.getWorkspaceEntries().add(wsEntry);

      assertFalse(isRepositoryExists(rEntry.getName()));

      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY
            + "/" + info.getBackupId()), rEntry);

      waitRepositoryRestore(rEntry.getName());
      assertTrue(isRepositoryExists(rEntry.getName()));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rEntry.getName()));

      assertEquals(200, cres.getStatus());

      DetailedInfo dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rEntry.getName(), dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORES_REPOSITORY));

      assertEquals(200, cres.getStatus());

      infoList = (ShortInfoList)getObject(ShortInfoList.class, cres.responseWriter.getBody());
      assertNotNull(infoList);

      info = new ArrayList<ShortInfo>(infoList.getBackups()).get(0);

      assertNotNull(info);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(info.getStartedTime());
      assertNotNull(info.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, info.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, info.getState().intValue());
      assertEquals(rEntry.getName(), info.getRepositoryName());
      assertNotNull(info.getBackupId());

      // restore by id, "remove-existing" is true
      assertTrue(isRepositoryExists(rEntry.getName()));
      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY
            + "/" + backupId + "/true"), rEntry);

      assertEquals(200, cres.getStatus());

      waitRepositoryRestore(rEntry.getName());
      assertTrue(isRepositoryExists(rEntry.getName()));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rEntry.getName()));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, info.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rEntry.getName(), info.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by id, "remove-existing" is false
      removeRepositoryFully(rEntry.getName());
      assertFalse(isRepositoryExists(rEntry.getName()));

      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY
            + "/" + backupId + "/false"), rEntry);
         assertEquals(200, cres.getStatus());

      waitRepositoryRestore(rEntry.getName());
      assertTrue(isRepositoryExists(rEntry.getName()));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rEntry.getName()));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rEntry.getName(), dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by id, "remove-exising" is true
      assertTrue(isRepositoryExists(rInfo.rName));
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY
            + "/" + backupId + "/" + "true"));

      assertEquals(200, cres.getStatus());

      waitRepositoryRestore(rInfo.rName);
      assertTrue(isRepositoryExists(rInfo.rName));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rInfo.rName));
      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.rName, dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by id, "remove-existing" if false
      removeRepositoryFully(rInfo.rName);
      assertFalse(isRepositoryExists(rInfo.rName));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY
            + "/" + backupId + "/false"));

      waitRepositoryRestore(rInfo.rName);
      assertTrue(isRepositoryExists(rInfo.rName));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rInfo.rName));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, info.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.rName, dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by backup-set path, "remove-existing" is true
      assertTrue(isRepositoryExists(rEntry.getName()));
      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY_BACKUP_SET + "/" + "true"
            + "?backup-set-path=" + backupSetPath), rEntry);

      assertEquals(200, cres.getStatus());

      waitRepositoryRestore(rEntry.getName());
      assertTrue(isRepositoryExists(rEntry.getName()));
      
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rEntry.getName()));

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rEntry.getName(), dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by backup set path, "remove existing" is false
      removeRepositoryFully(rEntry.getName());
      assertFalse(isRepositoryExists(rEntry.getName()));

      cres =
         makePostRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.RESTORE_REPOSITORY_BACKUP_SET + "/false" + "?backup-set-path="
            + backupSetPath), rEntry);

      assertEquals(200, cres.getStatus());

      waitRepositoryRestore(rEntry.getName());
      assertTrue(isRepositoryExists(rEntry.getName()));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rEntry.getName()));
      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rEntry.getName(), dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by backup-set-path, "remove-existing" is true
      assertTrue(isRepositoryExists(rInfo.rName));
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET
            + "/true" + "?backup-set-path=" + backupSetPath));

      waitRepositoryRestore(rInfo.rName);
      assertTrue(isRepositoryExists(rInfo.rName));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rInfo.rName));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.rName, dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());

      // restore by backup-set-path, "remove-existing" is false
      removeRepositoryFully(rInfo.rName);
      assertFalse(isRepositoryExists(rInfo.rName));
      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH + HTTPBackupAgent.Constants.OperationType.RESTORE_BACKUP_SET
            + "/false" + "?backup-set-path=" + backupSetPath));

      waitRepositoryRestore(rInfo.rName);
      assertTrue(isRepositoryExists(rInfo.rName));

      cres =
         makeGetRequest(new URI(HTTP_BACKUP_AGENT_PATH
            + HTTPBackupAgent.Constants.OperationType.CURRENT_RESTORE_INFO_ON_REPOSITORY + "/" + rInfo.rName));

      assertEquals(200, cres.getStatus());

      dInfo = (DetailedInfo)getObject(DetailedInfo.class, cres.responseWriter.getBody());

      assertNotNull(dInfo);
      assertEquals(BackupManager.FULL_BACKUP_ONLY, dInfo.getBackupType().intValue());
      assertNotNull(dInfo.getStartedTime());
      assertNotNull(dInfo.getFinishedTime());
      assertEquals(ShortInfo.RESTORE, dInfo.getType().intValue());
      assertEquals(JobRepositoryRestore.REPOSITORY_RESTORE_SUCCESSFUL, dInfo.getState().intValue());
      assertEquals(rInfo.rName, dInfo.getRepositoryName());
      assertNotNull(dInfo.getBackupConfig());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ExtendedBackupManager getBackupManager()
   {
      return (ExtendedBackupManager)container.getComponentInstanceOfType(BackupManager.class);
   }
}
