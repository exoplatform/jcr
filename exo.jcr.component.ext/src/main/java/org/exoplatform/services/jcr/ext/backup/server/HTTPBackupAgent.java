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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.RestoreConfigurationException;
import org.exoplatform.services.jcr.ext.backup.WorkspaceRestoreException;
import org.exoplatform.services.jcr.ext.backup.impl.BackupLogsFilter;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
import org.exoplatform.services.jcr.ext.backup.impl.RepositoryBackupLogsFilter;
import org.exoplatform.services.jcr.ext.backup.server.bean.BackupConfigBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.BackupServiceInfoBean;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.DetailedInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.DetailedInfoEx;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfo;
import org.exoplatform.services.jcr.ext.backup.server.bean.response.ShortInfoList;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.security.RolesAllowed;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by The eXo Platform SAS.
 *
 * HTTPBackupAgent is a RESTfull service on top of the BackupManager
 * allowing to manage the backups and the restores.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: BackupServer.java 111 2008-11-11 11:11:11Z rainf0x $
 */

@Path("/jcr-backup/")
public class HTTPBackupAgent implements ResourceContainer
{

   /**
    * Definition the constants.
    */
   public static final class Constants
   {

      /**
       * The date format RFC_1123.
       */
      public static final String DATE_FORMAT_RFC_1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

      /**
       * The base path to this service.
       */
      public static final String BASE_URL = "/jcr-backup";

      /**
       * Definition the operation types.
       */
      public static final class OperationType
      {
         /**
          * Start backup operation.
          */
         public static final String START_BACKUP = "/start";

         /**
          * Start backup repository operation.
          */
         public static final String START_BACKUP_REPOSITORY = "/start-backup-repository";

         /**
          * Restore operations.
          */
         public static final String RESTORE = "/restore";

         /**
          * Restore operations from backup set.
          */
         public static final String RESTORE_BACKUP_SET = "/restore/backup-set";

         /**
          * Restore repository operations.
          */
         public static final String RESTORE_REPOSITORY = "/restore-repository";

         /**
          * Restore repository operations from backup set.
          */
         public static final String RESTORE_REPOSITORY_BACKUP_SET = "/restore-repository/backup-set";

         /**
          * Stop backup operations.
          */
         public static final String STOP_BACKUP = "/stop";

         /**
          * Stop repository backup operations.
          */
         public static final String STOP_BACKUP_REPOSITORY = "/stop-backup-repository";

         /**
          * The current and completed backups info operation.
          */
         public static final String CURRENT_AND_COMPLETED_BACKUPS_INFO = "/info/backup";

         /**
          * The current and completed repository backups info operation.
          */
         public static final String CURRENT_AND_COMPLETED_BACKUPS_REPOSITORY_INFO = "/info/backup-repository";

         /**
          * The current and completed backups info operation for specific workspace.
          */
         public static final String CURRENT_AND_COMPLETED_BACKUPS_INFO_ON_WS = "/info/backup";

         /**
          * The current backups info operations.
          */
         public static final String CURRENT_BACKUPS_INFO = "/info/backup/current";

         /**
          * The current repository backups info operations.
          */
         public static final String CURRENT_BACKUPS_REPOSITORY_INFO = "/info/backup-repository/current";

         /**
          * The current or completed backup info operations.
          */
         public static final String CURRENT_OR_COMPLETED_BACKUP_INFO = "/info/backup";

         /**
          * The current or completed repository backup info operations.
          */
         public static final String CURRENT_OR_COMPLETED_BACKUP_REPOSITORY_INFO = "/info/backup-repository-id";

         /**
          * The current restore info operations for specific workspace.
          */
         public static final String CURRENT_RESTORE_INFO_ON_WS = "/info/restore";

         /**
         * The current restore info operations for specific repository.
         */
         public static final String CURRENT_RESTORE_INFO_ON_REPOSITORY = "/info/restore-repository";

         /**
          * The current restores info operations.
          */
         public static final String CURRENT_RESTORES = "/info/restores";

         /**
          * The current repository restores info operations.
          */
         public static final String CURRENT_RESTORES_REPOSITORY = "/info/restores-repository";

         /**
          * The completed backups info operations.
          */
         public static final String COMPLETED_BACKUPS_INFO = "/info/backup/completed";

         /**
          * The completed repository backups info operations.
          */
         public static final String COMPLETED_BACKUPS_REPOSITORY_INFO = "/info/backup-repository/completed";

         /**
          * The backup service info operations.
          */
         public static final String BACKUP_SERVICE_INFO = "/info";

         /**
          * The drop workspace operations.
          */
         public static final String DROP_WORKSPACE = "/drop-workspace";

         /**
          * The get default workspace configuration.
          */
         public static final String GET_DEFAULT_WORKSPACE_CONFIG = "/info/default-ws-config";

         /**
          * The get default repository configuration.
          */
         public static final String GET_DEFAULT_REPOSITORY_CONFIG = "/info/default-repository-config";

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
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.HTTPBackupAgent");

   /**
    * To disable cache control.
    */
   private static final CacheControl noCache;

   static
   {
      noCache = new CacheControl();
      noCache.setNoCache(true);
      noCache.setNoStore(true);
   }

   /**
    * The repository service.
    */
   private RepositoryService repositoryService;

   /**
    * The backup manager.
    */
   private ExtendedBackupManager backupManager;

   /**
    * Will be get session over base authenticate.
    */
   private SessionProviderService sessionProviderService;

   /**
    * ReplicationTestService constructor.
    * 
    * @param repoService
    *          the RepositoryService
    * @param backupManager
    *          the BackupManager
    * @param sessionProviderService
    *          the ThreadLocalSessionProviderService
    */
   public HTTPBackupAgent(RepositoryService repoService, BackupManager backupManager,
      SessionProviderService sessionProviderService)
   {
      this.repositoryService = repoService;
      this.backupManager = (ExtendedBackupManager) backupManager;
      this.sessionProviderService = sessionProviderService;

      LOG.info("HTTPBackupAgent inited");
   }

   /**
    * Starts a backup on a given workspace.
    * 
    * @param bConfigBean the bean with backup configuration.
    * @param repository the name of the repository
    * @param workspace the name of the workspace
    * @return Response return the response
    * @request
    * {code:json}
    * {
    *   "backupType" : the backup type (full or full+incremental),
    *   "incrementalJobPeriod" : the incremental job period,
    *   "incrementalRepetitionNumber" : the incremental repetition number,
    *   "fullBackupJobConfig" : the BackupJobConfig to full backup,
    *   "incrementalBackupJobConfig" : the BackupJobConfig to incremental backup,
    *   "backupDir" : the folder for backup data
    * }
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/start/{repo}/{ws}")
   public Response start(BackupConfigBean bConfigBean, @PathParam("repo") String repository,
      @PathParam("ws") String workspace)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         File backupDir;

         if (bConfigBean.getBackupDir() == null)
         {
            backupDir = backupManager.getBackupDirectory();
         }
         else
         {
            backupDir = new File(bConfigBean.getBackupDir());
            if (!PrivilegedFileHelper.exists(backupDir))
               throw new BackupDirNotFoundException("The backup folder not exists :  "
                        + PrivilegedFileHelper.getAbsolutePath(backupDir));
         }

         BackupConfig config = new BackupConfig();
         config.setBackupType(bConfigBean.getBackupType());
         config.setRepository(repository);
         config.setWorkspace(workspace);
         config.setBackupDir(backupDir);
         config.setIncrementalJobPeriod(bConfigBean.getIncrementalJobPeriod());
         config.setIncrementalJobNumber(bConfigBean.getIncrementalRepetitionNumber());

         validateRepositoryName(repository);
         validateWorkspaceName(repository, workspace);
         validateOneBackupInstants(repository, workspace);

         BackupChain chain = backupManager.startBackup(config);

         ShortInfo shortInfo = new ShortInfo(ShortInfo.CURRENT, chain);

         return Response.ok(shortInfo).cacheControl(noCache).build();
      }
      catch (NoSuchWorkspaceException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (LoginException e)
      {
         exception = e;
         status = Response.Status.UNAUTHORIZED;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (WorkspaceRestoreExeption e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupOperationException e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      catch (BackupConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start backup", exception);

      return Response.status(status).entity("Can not start backup : " + failMessage).type(MediaType.TEXT_PLAIN)
         .cacheControl(noCache).build();
   }

   /**
    * Starts a backup on a given repository.
    * 
    * @param bConfigBean the bean with backup configuration.
    * @param repository the name of the repository
    * @return Response return the response
    * @LevelAPI Provisional
    * @request
    * {code:json}
    * {
    *   "backupType" : the backup type (full or full+incremental),
    *   "incrementalJobPeriod" : the incremental job period,
    *   "incrementalRepetitionNumber" : the incremental repetition number,
    *   "fullBackupJobConfig" : the BackupJobConfig to full backup,
    *   "incrementalBackupJobConfig" : the BackupJobConfig to incremental backup,
    *   "backupDir" : the folder for backup data
    * }
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    * }
    * {code}
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/start-backup-repository/{repo}")
   public Response startBackupRepository(BackupConfigBean bConfigBean, @PathParam("repo") String repository)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         File backupDir;

         if (bConfigBean.getBackupDir() == null)
         {
            backupDir = backupManager.getBackupDirectory();
         }
         else
         {
            backupDir = new File(bConfigBean.getBackupDir());
            if (!PrivilegedFileHelper.exists(backupDir))
               throw new BackupDirNotFoundException("The backup folder not exists :  "
                        + PrivilegedFileHelper.getAbsolutePath(backupDir));
         }

         RepositoryBackupConfig config = new RepositoryBackupConfig();
         config.setBackupType(bConfigBean.getBackupType());
         config.setRepository(repository);
         config.setBackupDir(backupDir);
         config.setIncrementalJobPeriod(bConfigBean.getIncrementalJobPeriod());
         config.setIncrementalJobNumber(bConfigBean.getIncrementalRepetitionNumber());

         validateRepositoryName(repository);

         RepositoryBackupChain chain = backupManager.startBackup(config);

         ShortInfo shortInfo = new ShortInfo(ShortInfo.CURRENT, chain);

         return Response.ok(shortInfo).cacheControl(noCache).build();
      }
      catch (LoginException e)
      {
         exception = e;
         status = Response.Status.UNAUTHORIZED;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupOperationException e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      catch (BackupConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start backup", exception);

      return Response.status(status).entity("Can not start backup : " + failMessage).type(MediaType.TEXT_PLAIN)
         .cacheControl(noCache).build();
   }

   /**
    * Drops a given workspace.
    * 
    * @param repository the name of the repository
    * @param workspace the name of the workspace
    * @param forceSessionClose the flag indicating whether or not we need to force closing the current sessions
    * @return Response return the response
    * @LevelAPI Provisional
    */
   @GET
   @RolesAllowed("administrators")
   @Path("/drop-workspace/{repo}/{ws}/{force-session-close}")
   public Response dropWorkspace(@PathParam("repo") String repository, @PathParam("ws") String workspace,
      @PathParam("force-session-close") Boolean forceSessionClose)
   {

      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         validateRepositoryName(repository);
         validateWorkspaceName(repository, workspace);

         if (forceSessionClose)
            forceCloseSession(repository, workspace);

         RepositoryImpl repositoryImpl = (RepositoryImpl)repositoryService.getRepository(repository);
         repositoryImpl.removeWorkspace(workspace);
         repositoryService.getConfig().retain(); // save configuration to persistence (file or persister)

         return Response.ok().cacheControl(noCache).build();

      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not drop the workspace '" + "/" + repository + "/" + workspace + "'", exception);

      return Response.status(status).entity(
         "Can not drop the workspace '" + "/" + repository + "/" + workspace + "' : " + failMessage).type(
         MediaType.TEXT_PLAIN).cacheControl(noCache).build();

   }

   /**
    * Restores a workspace.
    * 
    * @param wEntry the configuration of the workspace to restore
    * @param repository the name of the repository
    * @param backupId the identifier of the backup
    * @return Response return the response
    * @request
    * {code:json}
    *  "wEntry" : the configuration of the workspace to restore.
    * {code}
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore) ,
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore/{repo}/{id}")
   public Response restore(WorkspaceEntry wEntry, @PathParam("repo") String repository, @PathParam("id") String backupId)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         validateOneRestoreInstants(repository, wEntry.getName());

         File backupLog = getBackupLogbyId(backupId);

         // validate backup log file
         if (backupLog == null)
            throw new BackupLogNotFoundException("The backup log file with id " + backupId + " not exists.");

         validateRepositoryName(repository);

         if (isWorkspaceExist(repository, wEntry.getName()))
            throw new WorkspaceRestoreException("Workspace " + wEntry.getName() + " already exist!");

         BackupChainLog backupChainLog = new BackupChainLog(backupLog);

         backupManager.restore(backupChainLog, repository, wEntry, true);

         /*
          * Sleeping
          * Restore must be initialized by job thread
          */

         Thread.sleep(100);

         /*
          * search necessary restore
          */

         List<JobWorkspaceRestore> restoreJobs = backupManager.getRestores();
         JobWorkspaceRestore restore = null;
         for (JobWorkspaceRestore curRestore : restoreJobs)
         {
            if (curRestore.getRepositoryName().equals(repository)
               && curRestore.getWorkspaceName().equals(wEntry.getName()))
            {
               restore = curRestore;
               break;
            }
         }

         if (restore != null)
         {
            ShortInfo info =
               new ShortInfo(ShortInfo.RESTORE, restore.getBackupChainLog(), restore.getStartTime(), restore
                  .getEndTime(), restore.getStateRestore(), restore.getRepositoryName(), restore.getWorkspaceName());
            return Response.ok(info).cacheControl(noCache).build();
         }

         return Response.ok().cacheControl(noCache).build();
      }
      catch (WorkspaceRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
         + "' from backup log with id '" + backupId + "'", exception);

      return Response.status(status).entity(
         "Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
            + "' from backup log with id '" + backupId + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(
         noCache).build();
   }

   /**
    * Restores a workspace.
    * 
    * @param wEntry the configuration of the workspace to restore
    * @param repository the name of the repository
    * @param backupId the identifier of the backup
    * @param removeExisting if 'true', it will remove fully (db, value storage, index) the existing workspace.  
    * @return Response return the response
    * @request
    * {code:json}
    *  "wEntry" : "the configuration of the workspace to restore.
    * {code}
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore) ,
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore/{repo}/{id}/{remove-Existing}")
   public Response restore(WorkspaceEntry wEntry, @PathParam("repo") String repository,
            @PathParam("id") String backupId, @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         validateOneRestoreInstants(repository, wEntry.getName());

         File backupLog = getBackupLogbyId(backupId);

         // validate backup log file
         if (backupLog == null)
         {
            throw new BackupLogNotFoundException("The backup log file with id " + backupId + " not exists.");
         }

         validateRepositoryName(repository);

         BackupChainLog backupChainLog = new BackupChainLog(backupLog);

         if (removeExisting)
         {
            if (!isWorkspaceExist(repository, wEntry.getName()))
            {
               throw new WorkspaceRestoreException("Workspace " + wEntry.getName() + " is not exist!");
            }

            backupManager.restoreExistingWorkspace(backupChainLog, repository, wEntry, true);
         }
         else
         {
            if (isWorkspaceExist(repository, wEntry.getName()))
            {
               throw new Exception("Workspace " + wEntry.getName() + " already exist!");
            }

            backupManager.restore(backupChainLog, repository, wEntry, true);
         }

         /*
          * Sleeping
          * Restore must be initialized by job thread
          */

         Thread.sleep(100);

         /*
          * search necessary restore
          */

         List<JobWorkspaceRestore> restoreJobs = backupManager.getRestores();
         JobWorkspaceRestore restore = null;
         for (JobWorkspaceRestore curRestore : restoreJobs)
         {
            if (curRestore.getRepositoryName().equals(repository)
                     && curRestore.getWorkspaceName().equals(wEntry.getName()))
            {
               restore = curRestore;
               break;
            }
         }

         if (restore != null)
         {
            ShortInfo info =
                     new ShortInfo(ShortInfo.RESTORE, restore.getBackupChainLog(), restore.getStartTime(), restore
                              .getEndTime(), restore.getStateRestore(), restore.getRepositoryName(), restore
                              .getWorkspaceName());
            return Response.ok(info).cacheControl(noCache).build();
         }

         return Response.ok().cacheControl(noCache).build();
      }
      catch (WorkspaceRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
               + "' from backup log with id '" + backupId + "'", exception);

      return Response.status(status).entity(
               "Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
                        + "' from backup log with id '" + backupId + "' : " + failMessage).type(MediaType.TEXT_PLAIN)
               .cacheControl(noCache).build();
   }

   /**
    * Restores the workspace from backup set with changing configuration.
    * 
    * @param wEntry the configuration of the workspace to restore
    * @param repository the name of the repository
    * @param backupSetPathEncoded the path to backup set
    * @param removeExisting if 'true', it will remove fully (db, value storage, index) the existing workspace.  
    * @return Response return the response
    * @request
    * {code:json}
    *  "wEntry" : the configuration of the workspace to restore.
    * {code}
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore/backup-set/{repo}/{remove-Existing}")
   public Response restoreBackupSet(WorkspaceEntry wEntry, @PathParam("repo") String repository,
            @QueryParam("backup-set-path") String backupSetPathEncoded, @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      String backupSetPath = null;

      try
      {
         backupSetPath = URLDecoder.decode(backupSetPathEncoded, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         LOG.error("Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
                  + "' from backup set '" + backupSetPath + "'", e);

         return Response.status(Response.Status.BAD_REQUEST).entity(
                  "Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
                           + "' from backup set '" + backupSetPath + "' : " + e.getMessage())
                  .type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }

      try
      {
         validateOneRestoreInstants(repository, wEntry.getName());

         File backupSetDir = (new File(backupSetPath));

         if (!backupSetDir.exists())
         {
            throw new RestoreConfigurationException("Backup set directory is not exists :" + backupSetPath);
         }

         if (!backupSetDir.isDirectory())
         {
            throw new RestoreConfigurationException("Backup set directory is not directory :" + backupSetPath);
         }

         File[] cfs = PrivilegedFileHelper.listFiles(backupSetDir, new BackupLogsFilter());

         if (cfs.length == 0)
         {
            throw new RestoreConfigurationException("Can not found workspace backup log in directory : "
                     + backupSetPath);
         }

         if (cfs.length > 1)
         {
            throw new RestoreConfigurationException(
                     "Backup set directory should contains only one workspace backup log : " + backupSetPath);
         }

         validateRepositoryName(repository);

         BackupChainLog backupChainLog = new BackupChainLog(cfs[0]);

         if (removeExisting)
         {
            if (!isWorkspaceExist(repository, wEntry.getName()))
            {
               throw new WorkspaceRestoreException("Workspace " + wEntry.getName() + " is not exist!");
            }

            backupManager.restoreExistingWorkspace(backupChainLog, repository, wEntry, true);
         }
         else
         {
            if (isWorkspaceExist(repository, wEntry.getName()))
            {
               throw new Exception("Workspace " + wEntry.getName() + " already exist!");
            }

            backupManager.restore(backupChainLog, repository, wEntry, true);
         }

         /*
          * Sleeping
          * Restore must be initialized by job thread
          */

         Thread.sleep(100);

         /*
          * search necessary restore
          */

         List<JobWorkspaceRestore> restoreJobs = backupManager.getRestores();
         JobWorkspaceRestore restore = null;
         for (JobWorkspaceRestore curRestore : restoreJobs)
         {
            if (curRestore.getRepositoryName().equals(repository)
                     && curRestore.getWorkspaceName().equals(wEntry.getName()))
            {
               restore = curRestore;
               break;
            }
         }

         if (restore != null)
         {
            ShortInfo info =
                     new ShortInfo(ShortInfo.RESTORE, restore.getBackupChainLog(), restore.getStartTime(), restore
                              .getEndTime(), restore.getStateRestore(), restore.getRepositoryName(), restore
                              .getWorkspaceName());
            return Response.ok(info).cacheControl(noCache).build();
         }

         return Response.ok().cacheControl(noCache).build();
      }
      catch (WorkspaceRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
               + "' from backup set '" + backupSetPath + "'", exception);

      return Response.status(status).entity(
               "Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
                        + "' from backup set '" + backupSetPath + "' : " + failMessage).type(MediaType.TEXT_PLAIN)
               .cacheControl(noCache).build();
   }

   /**
    * Restores the workspace with original configuration (this configuration was stored in backup chain log).
    *
    * @param backupId the identifier of the backup
    * @param removeExisting if 'true', it will be remove fully (db, value storage, index) the existing workspace.  
    * @return Response return the response
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore/{id}/{remove-Existing}")
   public Response restore(@PathParam("id") String backupId, @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      String repository = null;
      String workspace = null;

      try
      {
         File backupLog = getBackupLogbyId(backupId);

         // validate backup log file
         if (backupLog == null)
         {
            throw new BackupLogNotFoundException("The backup log file with id " + backupId + " not exists.");
         }

         BackupChainLog backupChainLog = new BackupChainLog(backupLog);

         repository = backupChainLog.getBackupConfig().getRepository();
         workspace = backupChainLog.getBackupConfig().getWorkspace();

         validateOneRestoreInstants(repository, workspace);

         validateRepositoryName(repository);

         //workspace name and repository name should equals original names from backup set.
         if (!repository.equals(backupChainLog.getBackupConfig().getRepository()))
         {
            throw new WorkspaceRestoreException("Repository name\"" + repository
                     + "\" should equals original repository name from backup set : \""
                     + backupChainLog.getBackupConfig().getRepository() + "\".");
         }

         if (!workspace.equals(backupChainLog.getBackupConfig().getWorkspace()))
         {
            throw new WorkspaceRestoreException("Workspace name\"" + workspace
                     + "\" should equals original workspace name from backup set : \""
                     + backupChainLog.getBackupConfig().getWorkspace() + "\".");
         }

         if (removeExisting)
         {
            if (!isWorkspaceExist(repository, workspace))
            {
               throw new WorkspaceRestoreException("Workspace " + workspace + " is not exists!");
            }

            backupManager.restoreExistingWorkspace(backupId, true);
         }
         else
         {
            if (isWorkspaceExist(repository, workspace))
            {
               throw new Exception("Workspace " + workspace + " already exists!");
            }

            backupManager.restoreWorkspace(backupId, true);
         }

         /*
          * Sleeping
          * Restore must be initialized by job thread
          */

         Thread.sleep(100);

         /*
          * search necessary restore
          */

         List<JobWorkspaceRestore> restoreJobs = backupManager.getRestores();
         JobWorkspaceRestore restore = null;
         for (JobWorkspaceRestore curRestore : restoreJobs)
         {
            if (curRestore.getRepositoryName().equals(repository)
                     && curRestore.getWorkspaceName().equals(workspace))
            {
               restore = curRestore;
               break;
            }
         }

         if (restore != null)
         {
            ShortInfo info =
                     new ShortInfo(ShortInfo.RESTORE, restore.getBackupChainLog(), restore.getStartTime(), restore
                              .getEndTime(), restore.getStateRestore(), restore.getRepositoryName(), restore
                              .getWorkspaceName());
            return Response.ok(info).cacheControl(noCache).build();
         }

         return Response.ok().cacheControl(noCache).build();
      }
      catch (WorkspaceRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the workspace '" + "/" + repository + "/" + workspace
               + "' from backup log with id '" + backupId + "'", exception);

      return Response.status(status).entity(
               "Can not start restore the workspace '" + "/" + repository + "/" + workspace
                        + "' from backup log with id '" + backupId + "' : " + failMessage).type(MediaType.TEXT_PLAIN)
               .cacheControl(noCache).build();
   }

   /**
    * Restores the workspace or repository with original configuration (this configuration was stored in backup log).
    * 
    * @param backupSetPathEncoded the path to backup set
    * @param removeExisting if 'true', it will remove fully (db, value storage, index) the existing workspace.  
    * @return Response return the response
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore/backup-set/{remove-Existing}")
   public Response restoreFromBackupSet(@QueryParam("backup-set-path") String backupSetPathEncoded,
            @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage = null;
      Response.Status status = null;
      Throwable exception = null;
      
      String backupSetPath = null;

      try
      {
         backupSetPath = URLDecoder.decode(backupSetPathEncoded, "UTF-8");
      }
      catch (UnsupportedEncodingException e) 
      {
         LOG.error("Can not start restore from backup set '" + backupSetPathEncoded + "'", e);

         return Response.status(Response.Status.BAD_REQUEST).entity(
                  "Can not start restore from backup set  '" + backupSetPathEncoded + "' : " + e.getMessage()).type(
                  MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }

      File backupSetDir = (new File(backupSetPath));
      File backuplog = null;

      boolean restoreWorkspace = true;

      try
      {
         if (!backupSetDir.exists())
         {
            throw new RestoreConfigurationException("Backup set directory is not exists :" + backupSetPath);
         }

         if (!backupSetDir.isDirectory())
         {
            throw new RestoreConfigurationException("Backup set directory is not directory :" + backupSetPath);
         }

         File[] cfsw = PrivilegedFileHelper.listFiles(backupSetDir, new BackupLogsFilter());
         File[] cfsr = PrivilegedFileHelper.listFiles(backupSetDir, new RepositoryBackupLogsFilter());

         if (cfsw.length == 0 && cfsr.length == 0)
         {
            throw new RestoreConfigurationException("Can not found backup log in directory : " + backupSetPath);
         }
         else if ((cfsw.length == 1 && cfsr.length == 1) || (cfsw.length > 1) || (cfsr.length > 1))
         {
            throw new RestoreConfigurationException("Backup set directory should contains only one backup log : "
                     + backupSetPath);
         }
         else if (cfsw.length != 0 && cfsr.length == 0)
         {
            restoreWorkspace = true;
            backuplog = cfsw[0];
         }
         else if (cfsw.length == 0 && cfsr.length != 0)
         {
            restoreWorkspace = false;
            backuplog = cfsr[0];
         }

      }
      catch (RestoreConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }
      finally
      {
         if (exception != null)
         {
            LOG.error("Can not start restore from backup set '" + backupSetPath + "'", exception);

            return Response.status(status).entity(
                     "Can not start restore from backup set  '" + backupSetPath + "' : " + failMessage).type(
                     MediaType.TEXT_PLAIN).cacheControl(noCache).build();
         }
      }

      if (restoreWorkspace)
      {
         
         String repository = null;
         String workspace = null;

         try
         {

            BackupChainLog backupChainLog = new BackupChainLog(backuplog);

            repository = backupChainLog.getBackupConfig().getRepository();
            workspace = backupChainLog.getBackupConfig().getWorkspace();

            validateOneRestoreInstants(repository, workspace);

            validateRepositoryName(repository);

            if (removeExisting)
            {
               if (!isWorkspaceExist(repository, workspace))
               {
                  throw new WorkspaceRestoreException("Workspace " + workspace + " is not exists!");
               }

               backupManager.restoreExistingWorkspace(backupSetDir, true);
            }
            else
            {
               if (isWorkspaceExist(repository, workspace))
               {
                  throw new Exception("Workspace " + workspace + " already exists!");
               }

               backupManager.restoreWorkspace(backupSetDir, true);
            }

            /*
             * Sleeping
             * Restore must be initialized by job thread
             */

            Thread.sleep(100);

            /*
             * search necessary restore
             */

            List<JobWorkspaceRestore> restoreJobs = backupManager.getRestores();
            JobWorkspaceRestore restore = null;
            for (JobWorkspaceRestore curRestore : restoreJobs)
            {
               if (curRestore.getRepositoryName().equals(repository) && curRestore.getWorkspaceName().equals(workspace))
               {
                  restore = curRestore;
                  break;
               }
            }

            if (restore != null)
            {
               ShortInfo info =
                        new ShortInfo(ShortInfo.RESTORE, restore.getBackupChainLog(), restore.getStartTime(), restore
                                 .getEndTime(), restore.getStateRestore(), restore.getRepositoryName(), restore
                                 .getWorkspaceName());
               return Response.ok(info).cacheControl(noCache).build();
            }

            return Response.ok().cacheControl(noCache).build();
         }
         catch (WorkspaceRestoreExeption e)
         {
            exception = e;
            status = Response.Status.FORBIDDEN;
            failMessage = e.getMessage();
         }
         catch (RepositoryException e)
         {
            exception = e;
            status = Response.Status.NOT_FOUND;
            failMessage = e.getMessage();
         }
         catch (RepositoryConfigurationException e)
         {
            exception = e;
            status = Response.Status.NOT_FOUND;
            failMessage = e.getMessage();
         }
         catch (BackupLogNotFoundException e)
         {
            exception = e;
            status = Response.Status.NOT_FOUND;
            failMessage = e.getMessage();
         }
         catch (Throwable e) //NOSONAR
         {
            exception = e;
            status = Response.Status.INTERNAL_SERVER_ERROR;
            failMessage = e.getMessage();
         }

         LOG.error("Can not start restore the workspace '" + "/" + repository + "/" + workspace
                  + "' from backup set \"" + backupSetPath + "\"", exception);

         return Response.status(status).entity(
                  "Can not start restore the workspace '" + "/" + repository + "/" + workspace + "' from backup set \""
                           + backupSetPath + "\" : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache)
                  .build();
      }
      else
      {
         String repository = null;

         try
         {

            RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(backuplog);
            repository = backupChainLog.getBackupConfig().getRepository();

            validateOneRepositoryRestoreInstants(repository);

            if (removeExisting)
            {
               if (!isRepositoryExist(repository))
               {
                  throw new RepositoryRestoreExeption("Repository " + repository + " is not exists!");
               }

               backupManager.restoreExistingRepository(backupSetDir, true);
            }
            else
            {
               if (isRepositoryExist(repository))
               {
                  throw new RepositoryRestoreExeption("Repository " + repository + " already exists!");
               }

               backupManager.restoreRepository(backupSetDir, true);
            }

            // Sleeping. Restore should be initialized by job thread
            Thread.sleep(100);

            // search necessary restore
            JobRepositoryRestore restore = backupManager.getLastRepositoryRestore(repository);
            ShortInfo info =
                     new ShortInfo(ShortInfo.RESTORE, restore.getRepositoryBackupChainLog(), restore.getStartTime(),
                              restore.getEndTime(), restore.getStateRestore(), restore.getRepositoryName());

            return Response.ok(info).cacheControl(noCache).build();
         }
         catch (RepositoryRestoreExeption e)
         {
            exception = e;
            status = Response.Status.FORBIDDEN;
            failMessage = e.getMessage();
         }
         catch (RepositoryException e)
         {
            exception = e;
            status = Response.Status.NOT_FOUND;
            failMessage = e.getMessage();
         }
         catch (RepositoryConfigurationException e)
         {
            exception = e;
            status = Response.Status.NOT_FOUND;
            failMessage = e.getMessage();
         }
         catch (Throwable e) //NOSONAR
         {
            exception = e;
            status = Response.Status.INTERNAL_SERVER_ERROR;
            failMessage = e.getMessage();
         }

         LOG.error("Can not start restore the repository '" + "/" + repository + "' from backup set '" + backupSetPath
                  + "'", exception);

         return Response.status(status).entity(
                  "Can not start restore the repository '" + "/" + repository + "' from backup set  '" + backupSetPath
                           + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Restores a repository.
    * 
    * @param rEntry the configuration of the repository to restore
    * @param backupId the identifier of the backup
    * @return Response return the response
    * @request
    * {code:json}
    * {
    *   "rEntry" :  the configuration of the repository to restore
    * }
    * {code}

    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore-repository/{id}")
   public Response restoreRepository(RepositoryEntry rEntry, @PathParam("id") String backupId)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         validateOneRepositoryRestoreInstants(rEntry.getName());

         File backupLog = getRepositoryBackupLogbyId(backupId);

         // validate backup log file
         if (backupLog == null)
         {
            throw new BackupLogNotFoundException("The repository backup log file with id " + backupId + " not exists.");
         }

         if (isRepositoryExist(rEntry.getName()))
         {
            throw new RepositoryRestoreExeption("Repository " + rEntry.getName() + " already exist!");
         }

         RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(backupLog);

         backupManager.restore(backupChainLog, rEntry, true);

         // Sleeping. Restore should be initialized by job thread
         Thread.sleep(100);

         // search necessary restore
         JobRepositoryRestore restore = backupManager.getLastRepositoryRestore(rEntry.getName());
         ShortInfo info =
            new ShortInfo(ShortInfo.RESTORE, restore.getRepositoryBackupChainLog(), restore.getStartTime(), restore
               .getEndTime(), restore.getStateRestore(), restore.getRepositoryName());

         return Response.ok(info).cacheControl(noCache).build();
      }
      catch (RepositoryRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup log with id '"
         + backupId + "'", exception);

      return Response.status(status).entity(
         "Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup log with id '" + backupId
            + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
   }

   /**
    * Restores a repository.
    * 
    * @param rEntry the configuration of the repository to restore
    * @param backupId the identifier of the backup
    * @param removeExisting if 'true', it will remove fully (db, value storage, index) the existing repository.
    * @return Response return the response
    * @request
    * {code:json}
    * {
    *   "rEntry" :  the configuration of the repository to restore
    * }
    * {code}
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore-repository/{id}/{remove-Existing}")
   public Response restoreRepository(RepositoryEntry rEntry, @PathParam("id") String backupId,
            @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         validateOneRepositoryRestoreInstants(rEntry.getName());

         File backupLog = getRepositoryBackupLogbyId(backupId);

         // validate backup log file
         if (backupLog == null)
         {
            throw new BackupLogNotFoundException("The repository backup log file with id " + backupId + " not exists.");
         }

         RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(backupLog);

         if (removeExisting)
         {
            if (!isRepositoryExist(rEntry.getName()))
            {
               throw new RepositoryRestoreExeption("Repository " + rEntry.getName() + " is not exists!");
            }

            backupManager.restoreExistingRepository(backupChainLog, rEntry, true);
         }
         else
         {
            if (isRepositoryExist(rEntry.getName()))
            {
               throw new RepositoryRestoreExeption("Repository " + rEntry.getName() + " already exists!");
            }

            backupManager.restore(backupChainLog, rEntry, true);
         }

         // Sleeping. Restore should be initialized by job thread
         Thread.sleep(100);

         // search necessary restore
         JobRepositoryRestore restore = backupManager.getLastRepositoryRestore(rEntry.getName());
         ShortInfo info =
                  new ShortInfo(ShortInfo.RESTORE, restore.getRepositoryBackupChainLog(), restore.getStartTime(),
                           restore.getEndTime(), restore.getStateRestore(), restore.getRepositoryName());

         return Response.ok(info).cacheControl(noCache).build();
      }
      catch (RepositoryRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup log with id '"
               + backupId + "'", exception);

      return Response.status(status).entity(
               "Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup log with id '"
                        + backupId + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
   }

   /**
    * Restores a repository from backup set with changing configuration.
    * 
    * @param rEntry the configuration of the repository to restore
    * @param backupSetPathEncoded the path to backup set
    * @param removeExisting if 'true', it will remove fully (db, value storage, index) the existing repository.
    * @return Response return the response
    * @request
    * {code:json}
    * {
    *   "rEntry" :  the configuration of the repository to restore
    * }
    * {code}
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName":the name of repository
    * }
    * {code}

    * @LevelAPI Provisional
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore-repository/backup-set/{remove-Existing}")
   public Response restoreRepositoryBackupSet(RepositoryEntry rEntry,
            @QueryParam("backup-set-path") String backupSetPathEncoded,
            @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      String backupSetPath = null;

      try
      {
         backupSetPath = URLDecoder.decode(backupSetPathEncoded, "UTF-8");
      }
      catch (UnsupportedEncodingException e)
      {
         LOG.error("Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup set '"
                  + backupSetPath + "'", e);

         return Response.status(Response.Status.BAD_REQUEST).entity(
                  "Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup set '"
                           + backupSetPath + "' : " + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(noCache)
                  .build();
      }

      try
      {
         validateOneRepositoryRestoreInstants(rEntry.getName());

         File backupSetDir = (new File(backupSetPath));

         if (!backupSetDir.exists())
         {
            throw new RestoreConfigurationException("Backup set directory is not exists :" + backupSetPath);
         }

         if (!backupSetDir.isDirectory())
         {
            throw new RestoreConfigurationException("Backup set directory is not directory :" + backupSetPath);
         }

         File[] cfs = PrivilegedFileHelper.listFiles(backupSetDir, new RepositoryBackupLogsFilter());

         if (cfs.length == 0)
         {
            throw new RestoreConfigurationException("Can not found repository backup log in directory : "
                     + backupSetPath);
         }

         if (cfs.length > 1)
         {
            throw new RestoreConfigurationException(
                     "Backup set directory should contains only one repository backup log : " + backupSetPath);
         }

         RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(cfs[0]);

         if (removeExisting)
         {
            if (!isRepositoryExist(rEntry.getName()))
            {
               throw new RepositoryRestoreExeption("Repository " + rEntry.getName() + " is not exists!");
            }

            backupManager.restoreExistingRepository(backupChainLog, rEntry, true);
         }
         else
         {
            if (isRepositoryExist(rEntry.getName()))
            {
               throw new RepositoryRestoreExeption("Repository " + rEntry.getName() + " already exists!");
            }

            backupManager.restore(backupChainLog, rEntry, true);
         }

         // Sleeping. Restore should be initialized by job thread
         Thread.sleep(100);

         // search necessary restore
         JobRepositoryRestore restore = backupManager.getLastRepositoryRestore(rEntry.getName());
         ShortInfo info =
                  new ShortInfo(ShortInfo.RESTORE, restore.getRepositoryBackupChainLog(), restore.getStartTime(),
                           restore.getEndTime(), restore.getStateRestore(), restore.getRepositoryName());

         return Response.ok(info).cacheControl(noCache).build();
      }
      catch (RepositoryRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup set '"
               + backupSetPath + "'", exception);

      return Response.status(status).entity(
               "Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup set '"
                        + backupSetPath + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache)
               .build();
   }

   /**
    * Restores a repository.
    * 
    * @param backupId the identifier of the backup
    * @param removeExisting if 'true', it  will remove fully (db, value storage, index) the existing repository.
    * @return Response return the response
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/restore-repository/{id}/{remove-Existing}")
   public Response restoreRepository(@PathParam("id") String backupId,
            @PathParam("remove-Existing") Boolean removeExisting)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      String repository = null;
      
      try
      {
         File backupLog = getRepositoryBackupLogbyId(backupId);

         // validate backup log file
         if (backupLog == null)
         {
            throw new BackupLogNotFoundException("The repository backup log file with id " + backupId + " not exists.");
         }

         RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(backupLog);
         repository = backupChainLog.getBackupConfig().getRepository();

         validateOneRepositoryRestoreInstants(repository);

         if (removeExisting)
         {
            if (!isRepositoryExist(repository))
            {
               throw new RepositoryRestoreExeption("Repository " + repository + " is not exists!");
            }

            backupManager.restoreExistingRepository(backupId, true);
         }
         else
         {
            if (isRepositoryExist(repository))
            {
               throw new RepositoryRestoreExeption("Repository " + repository + " already exists!");
            }

            backupManager.restoreRepository(backupId, true);
         }

         // Sleeping. Restore should be initialized by job thread
         Thread.sleep(100);

         // search necessary restore
         JobRepositoryRestore restore =
                  backupManager.getLastRepositoryRestore(repository);
         ShortInfo info =
                  new ShortInfo(ShortInfo.RESTORE, restore.getRepositoryBackupChainLog(), restore.getStartTime(),
                           restore.getEndTime(), restore.getStateRestore(), restore.getRepositoryName());

         return Response.ok(info).cacheControl(noCache).build();
      }
      catch (RepositoryRestoreExeption e)
      {
         exception = e;
         status = Response.Status.FORBIDDEN;
         failMessage = e.getMessage();
      }
      catch (RepositoryException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (RepositoryConfigurationException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (BackupLogNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not start restore the repository '" + "/" + repository + "' from backup log with id '"
               + backupId + "'", exception);

      return Response.status(status).entity(
               "Can not start restore the repository '" + "/" + repository + "' from backup log with id '"
                        + backupId + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
   }

   /**
    * Stops the backup corresponding to the given id.
    * 
    * @param backupId the identifier of the backup
    * @return Response return the response
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/stop/{id}")
   public Response stop(@PathParam("id") String backupId)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         BackupChain bch = backupManager.findBackup(backupId);

         if (bch != null)
            backupManager.stopBackup(bch);
         else
            throw new BackupNotFoundException("No active backup with id '" + backupId + "'");

         ShortInfo shortInfo = null;
         for (BackupChainLog chainLog : backupManager.getBackupsLogs())
            if (backupId.equals(chainLog.getBackupId()))
            {
               shortInfo = new ShortInfo(ShortInfo.COMPLETED, chainLog);
               break;
            }

         if (shortInfo == null)
            throw new BackupNotFoundException("No completed backup with id '" + backupId + "'");

         return Response.ok(shortInfo).cacheControl(noCache).build();
      }
      catch (BackupNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not stop backup", exception);

      return Response.status(status).entity("Can not stop backup : " + failMessage).type(MediaType.TEXT_PLAIN)
         .cacheControl(noCache).build();
   }

   /**
    * Stops the repository backup corresponding to the given id.
    * 
    * @param backupId the identifier of the backup
    * @return Response return the response
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    * }
    * {code
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/stop-backup-repository/{id}")
   public Response stopBackupRepository(@PathParam("id") String backupId)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         RepositoryBackupChain bch = backupManager.findRepositoryBackupId(backupId);

         if (bch != null)
            backupManager.stopBackup(bch);
         else
            throw new BackupNotFoundException("No active repository backup with id '" + backupId + "'");

         ShortInfo shortInfo = null;
         for (RepositoryBackupChainLog chainLog : backupManager.getRepositoryBackupsLogs())
            if (backupId.equals(chainLog.getBackupId()))
            {
               shortInfo = new ShortInfo(ShortInfo.COMPLETED, chainLog);
               break;
            }

         if (shortInfo == null)
            throw new BackupNotFoundException("No completed backup with id '" + backupId + "'");

         return Response.ok(shortInfo).cacheControl(noCache).build();
      }
      catch (BackupNotFoundException e)
      {
         exception = e;
         status = Response.Status.NOT_FOUND;
         failMessage = e.getMessage();
      }
      catch (Throwable e) //NOSONAR
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      LOG.error("Can not stop repository backup ", exception);

      return Response.status(status).entity("Can not stop repository backup : " + failMessage).type(
         MediaType.TEXT_PLAIN).cacheControl(noCache).build();
   }

   /**
    * Gives info about the backup service.
    * 
    * @return Response return the response
    * @response
    * {code:json}
    * {
    *   "backupLogDir" : the path to backup log folder,
    *   "defaultIncrementalJobPeriod" : the default incremental job period,
    *   "fullBackupType" : the type of full backup,
    *   "incrementalBackupType" : the type of incremental backup
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info")
   public Response info()
   {
      try
      {
         BackupServiceInfoBean infoBeen =
            new BackupServiceInfoBean(backupManager.getFullBackupType(), backupManager.getIncrementalBackupType(),
               PrivilegedFileHelper.getAbsolutePath(backupManager.getBackupDirectory()),
               backupManager.getDefaultIncrementalJobPeriod());

         return Response.ok(infoBeen).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about backup service", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about backup service : " + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives info about all the current and completed backups.
    * 
    * @return Response return the response
    * @response
    * {code:json}
    *  {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    *  ]
    *  {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup")
   public Response infoBackup()
   {
      try
      {
         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (BackupChain chain : backupManager.getCurrentBackups())
            list.add(new ShortInfo(ShortInfo.CURRENT, chain));

         for (BackupChainLog chainLog : backupManager.getBackupsLogs())
            if (backupManager.findBackup(chainLog.getBackupId()) == null)
               list.add(new ShortInfo(ShortInfo.COMPLETED, chainLog));

         ShortInfoList shortInfoList = new ShortInfoList(list);

         return Response.ok(shortInfoList).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current or completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives info about all the current and completed repository backups .
    *
    * @response
    * {code:json}
    * [
    *   {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    *  ]
    *  {code}
    * @LevelAPI Provisional
    * @return Response return the response
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup-repository")
   public Response infoBackupRepository()
   {
      try
      {
         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (RepositoryBackupChain chain : backupManager.getCurrentRepositoryBackups())
            list.add(new ShortInfo(ShortInfo.CURRENT, chain));

         for (RepositoryBackupChainLog chainLog : backupManager.getRepositoryBackupsLogs())
            if (backupManager.findRepositoryBackupId(chainLog.getBackupId()) == null)
               list.add(new ShortInfo(ShortInfo.COMPLETED, chainLog));

         ShortInfoList shortInfoList = new ShortInfoList(list);

         return Response.ok(shortInfoList).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current or completed reposioty backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed repository backups" + e.getMessage()).type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Gives full details about the current or completed backup corresponding to the given id.
    * 
    * @param id the identifier of the backup
    *
    * @response
    * {code:json}
    *  {
    *   "backupType" : the backup type (full or full+incremental),
    *   "incrementalJobPeriod" : the incremental job period,
    *   "incrementalRepetitionNumber" : the incremental repetition number,
    *   "fullBackupJobConfig" : the BackupJobConfig to full backup,
    *   "incrementalBackupJobConfig" : the BackupJobConfig to incremental backup,
    *   "backupDir" : the folder for backup data},
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    *  {code}
    * @return Response return the response
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup/{id}")
   public Response infoBackupId(@PathParam("id") String id)
   {
      try
      {
         BackupChain current = backupManager.findBackup(id);

         if (current != null)
         {
            DetailedInfo info = new DetailedInfo(DetailedInfo.CURRENT, current);
            return Response.ok(info).cacheControl(noCache).build();
         }

         BackupChainLog completed = null;

         for (BackupChainLog chainLog : backupManager.getBackupsLogs())
            if (id.equals(chainLog.getBackupId()))
               completed = chainLog;

         if (completed != null)
         {
            DetailedInfo info = new DetailedInfo(DetailedInfo.COMPLETED, completed);
            return Response.ok(info).cacheControl(noCache).build();
         }

         return Response.status(Response.Status.NOT_FOUND).entity("No current or completed backup with 'id' " + id)
            .type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current or completed backup with 'id' " + id, e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed backup with 'id' " + id + " : " + e.getMessage()).type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Gives full details about the current or completed repository backup corresponding to the given id.
    * 
    * @param id the identifier of the repository backup
    *
    * @response
    * {code:json}
    *  {
    *   "backupType" : the backup type (full or full+incremental),
    *   "incrementalJobPeriod" : the incremental job period,
    *   "incrementalRepetitionNumber" : the incremental repetition number,
    *   "fullBackupJobConfig" : the BackupJobConfig to full backup,
    *   "incrementalBackupJobConfig" : the BackupJobConfig to incremental backup,
    *   "backupDir" : the folder for backup data},
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    *  {code}
    * @return Response return the response
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup-repository-id/{id}")
   public Response infoBackupRepositoryId(@PathParam("id") String id)
   {
      try
      {
         RepositoryBackupChain current = backupManager.findRepositoryBackupId(id);

         if (current != null)
         {
            DetailedInfo info = new DetailedInfo(DetailedInfo.CURRENT, current);
            return Response.ok(info).cacheControl(noCache).build();
         }

         RepositoryBackupChainLog completed = null;

         for (RepositoryBackupChainLog chainLog : backupManager.getRepositoryBackupsLogs())
            if (id.equals(chainLog.getBackupId()))
               completed = chainLog;

         if (completed != null)
         {
            DetailedInfo info = new DetailedInfo(DetailedInfo.COMPLETED, completed);
            return Response.ok(info).cacheControl(noCache).build();
         }

         return Response.status(Response.Status.NOT_FOUND).entity(
            "No current or completed repository backup with 'id' " + id).type(MediaType.TEXT_PLAIN).cacheControl(
            noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current or completed repository backup with 'id' " + id, e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed repository backup with 'id' " + id + " : "
               + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Gives info about all the current backups.
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *   {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup/current")
   public Response infoBackupCurrent()
   {
      try
      {
         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (BackupChain chain : backupManager.getCurrentBackups())
            list.add(new ShortInfo(ShortInfo.CURRENT, chain));

         ShortInfoList shortInfoList = new ShortInfoList(list);

         return Response.ok(shortInfoList).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current backups" + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(
            noCache).build();
      }
   }

   /**
    * Gives info about all the current repository backups.
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *   {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup-repository/current")
   public Response infoBackupRepositoryCurrent()
   {
      try
      {
         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (RepositoryBackupChain chain : backupManager.getCurrentRepositoryBackups())
            list.add(new ShortInfo(ShortInfo.CURRENT, chain));

         ShortInfoList shortInfoList = new ShortInfoList(list);

         return Response.ok(shortInfoList).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current repositorty backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current backups" + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(
            noCache).build();
      }
   }

   /**
    * Gives info about all the completed backups.
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *   {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup/completed")
   public Response infoBackupCompleted()
   {
      try
      {
         List<ShortInfo> completedList = new ArrayList<ShortInfo>();

         for (BackupChainLog chainLog : backupManager.getBackupsLogs())
            if (backupManager.findBackup(chainLog.getBackupId()) == null)
               completedList.add(new ShortInfo(ShortInfo.COMPLETED, chainLog));

         ShortInfoList list = new ShortInfoList(completedList);

         return Response.ok(list).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives info about all the completed repository backups.
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *  {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup-repository/completed")
   public Response infoBackupRepositoryCompleted()
   {
      try
      {
         List<ShortInfo> completedList = new ArrayList<ShortInfo>();

         for (RepositoryBackupChainLog chainLog : backupManager.getRepositoryBackupsLogs())
            if (backupManager.findRepositoryBackupId(chainLog.getBackupId()) == null)
               completedList.add(new ShortInfo(ShortInfo.COMPLETED, chainLog));

         ShortInfoList list = new ShortInfoList(completedList);

         return Response.ok(list).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives info about all the current and completed backups of a specific
    * workspace.
    * 
    * @param repository the name of the repository
    * @param workspace the name of the workspace
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *  {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup/{repo}/{ws}")
   public Response infoBackupByWorkspace(@PathParam("repo") String repository, @PathParam("ws") String workspace)
   {
      try
      {
         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (BackupChain chain : backupManager.getCurrentBackups())
         {
            if (repository.equals(chain.getBackupConfig().getRepository())
               && workspace.equals(chain.getBackupConfig().getWorkspace()))
            {
               list.add(new ShortInfo(ShortInfo.CURRENT, chain));
            }
         }

         for (BackupChainLog chainLog : backupManager.getBackupsLogs())
         {
            if (backupManager.findBackup(chainLog.getBackupId()) == null
               && repository.equals(chainLog.getBackupConfig().getRepository())
               && workspace.equals(chainLog.getBackupConfig().getWorkspace()))
            {
               list.add(new ShortInfo(ShortInfo.COMPLETED, chainLog));
            }
         }

         ShortInfoList shortInfoList = new ShortInfoList(list);

         return Response.ok(shortInfoList).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current or completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives info about all the current and completed backups of a specific
    * repository.
    * 
    * @param repository the name of the repository
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *   {
    *   "startedTime": the start time of backup,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of backup,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of backup,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/backup-repository/{repo}")
   public Response infoBackupByRepository(@PathParam("repo") String repository)
   {
      try
      {
         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (RepositoryBackupChain chain : backupManager.getCurrentRepositoryBackups())
         {
            if (repository.equals(chain.getBackupConfig().getRepository()))
            {
               list.add(new ShortInfo(ShortInfo.CURRENT, chain));
            }
         }

         for (RepositoryBackupChainLog chainLog : backupManager.getRepositoryBackupsLogs())
         {
            if (backupManager.findRepositoryBackupId(chainLog.getBackupId()) == null
               && repository.equals(chainLog.getBackupConfig().getRepository()))
            {
               list.add(new ShortInfo(ShortInfo.COMPLETED, chainLog));
            }
         }

         ShortInfoList shortInfoList = new ShortInfoList(list);

         return Response.ok(shortInfoList).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current or completed repository backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed repository backups" + e.getMessage()).type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Gives all details about the last restore of a specific workspace.
    * 
    * @param repository the name of the repository
    * @param workspace the name of the workspace
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * {
    *  {
    *   "backupType" : the backup type (full or full+incremental),
    *   "incrementalJobPeriod" : the incremental job period,
    *   "incrementalRepetitionNumber" : the incremental repetition number,
    *   "fullBackupJobConfig" : the BackupJobConfig to full backup,
    *   "incrementalBackupJobConfig" : the BackupJobConfig to incremental backup,
    *   "backupDir" : the folder for backup data},
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    *  }
    *  {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/restore/{repo}/{ws}")
   public Response infoRestore(@PathParam("repo") String repository, @PathParam("ws") String workspace)
   {
      try
      {
         JobWorkspaceRestore restoreJob = backupManager.getLastRestore(repository, workspace);

         if (restoreJob != null)
         {
            DetailedInfoEx info =
               new DetailedInfoEx(DetailedInfo.RESTORE, restoreJob.getBackupChainLog(), restoreJob.getStartTime(),
                  restoreJob.getEndTime(), restoreJob.getStateRestore(), restoreJob.getRepositoryName(), restoreJob
                     .getWorkspaceName(),

                  restoreJob.getWorkspaceEntry(), restoreJob.getRestoreException() == null ? "" : restoreJob
                     .getRestoreException().getMessage());

            return Response.ok(info).cacheControl(noCache).build();
         }
         else
         {
            return Response.status(Response.Status.NOT_FOUND).entity(
               "No resrore for workspace /" + repository + "/" + workspace + "'").type(MediaType.TEXT_PLAIN)
               .cacheControl(noCache).build();
         }

      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error(
            "Can not get information about current restore for workspace /" + repository + "/" + workspace + "'", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current restore for workspace /" + repository + "/" + workspace + "' : "
               + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }

   }

   /**
    * Gives all details about the last restore of a specific repository.
    * 
    * @param repository the name of the repository
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * {
    *  {
    *   "backupType" : the backup type (full or full+incremental),
    *   "incrementalJobPeriod" : the incremental job period,
    *   "incrementalRepetitionNumber" : the incremental repetition number,
    *   "fullBackupJobConfig" : the BackupJobConfig to full backup,
    *   "incrementalBackupJobConfig" : the BackupJobConfig to incremental backup,
    *   "backupDir" : the folder for backup data},
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    *  }
    *  {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/restore-repository/{repo}")
   public Response infoRestoreRepository(@PathParam("repo") String repository)
   {
      try
      {
         JobRepositoryRestore restoreJob = backupManager.getLastRepositoryRestore(repository);

         if (restoreJob != null)
         {
            DetailedInfoEx info =
               new DetailedInfoEx(DetailedInfo.RESTORE, restoreJob.getRepositoryBackupChainLog(), restoreJob
                  .getStartTime(), restoreJob.getEndTime(), restoreJob.getStateRestore(), restoreJob
                  .getRepositoryName(),

               restoreJob.getRepositoryEntry(), restoreJob.getRestoreException() == null ? "" : restoreJob
                  .getRestoreException().getMessage());

            return Response.ok(info).cacheControl(noCache).build();
         }
         else
         {
            return Response.status(Response.Status.NOT_FOUND).entity("No restore for repository /" + repository + "'")
               .type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
         }

      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current restore for repository /" + repository + "'", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current restore for repository /" + repository + "' : " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }

   }

   /**
    * Gives all details about the last restores.
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    *  }
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/restores")
   public Response infoRestores()
   {
      try
      {
         List<JobWorkspaceRestore> restoreJobs = backupManager.getRestores();

         List<JobWorkspaceRestore> jobs = new ArrayList<JobWorkspaceRestore>();

         for (int i = restoreJobs.size() - 1; i >= 0; i--)
         {
            JobWorkspaceRestore job = restoreJobs.get(i);
            boolean isUnique = true;
            for (JobWorkspaceRestore unJob : jobs)
            {
               if (unJob.getRepositoryName().equals(job.getRepositoryName())
                  && unJob.getWorkspaceName().equals(job.getWorkspaceName()))
                  isUnique = false;
            }

            if (isUnique)
               jobs.add(job);
         }

         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (JobWorkspaceRestore job : jobs)
         {
            ShortInfo info =
               new ShortInfo(ShortInfo.RESTORE, job.getBackupChainLog(), job.getStartTime(), job.getEndTime(), job
                  .getStateRestore(), job.getRepositoryName(), job.getWorkspaceName());
            list.add(info);
         }

         return Response.ok(new ShortInfoList(list)).cacheControl(noCache).build();

      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current restores.", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current restores : " + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives all details about the last repository restores.
    * 
    * @return Response return the response
    *
    * @response
    * {code:json}
    * [
    *    {
    *   "startedTime": the start time of restore,
    *   "backupId": the backup identifier,
    *   "type": the type of ShortInfo (current, completed, restore),
    *   "state": the state of restore,
    *   "backupType": the backup type (full or full+incremental),
    *   "workspaceName": the name of workspace,
    *   "finishedTime": the finish time of restore,
    *   "repositoryName": the name of repository
    *  }
    * ]
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/restores-repository")
   public Response infoRestoresRepository()
   {
      try
      {
         List<JobRepositoryRestore> restoreJobs = backupManager.getRepositoryRestores();

         List<JobRepositoryRestore> jobs = new ArrayList<JobRepositoryRestore>();

         for (int i = restoreJobs.size() - 1; i >= 0; i--)
         {
            JobRepositoryRestore job = restoreJobs.get(i);
            boolean isUnique = true;
            for (JobRepositoryRestore unJob : jobs)
            {
               if (unJob.getRepositoryName().equals(job.getRepositoryName()))
                  isUnique = false;
            }

            if (isUnique)
               jobs.add(job);
         }

         List<ShortInfo> list = new ArrayList<ShortInfo>();

         for (JobRepositoryRestore job : jobs)
         {
            ShortInfo info =
               new ShortInfo(ShortInfo.RESTORE, job.getRepositoryBackupChainLog(), job.getStartTime(),
                  job.getEndTime(), job.getStateRestore(), job.getRepositoryName());
            list.add(info);
         }

         return Response.ok(new ShortInfoList(list)).cacheControl(noCache).build();

      }
      catch (Throwable e) //NOSONAR
      {
         LOG.error("Can not get information about current repository restores.", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current repository restores : " + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Gives the default workspace configuration.
    * 
    * @return Response return the JSON to WorkspaceEntry
    *
    * @response
    * {code:json}
    * {
    *   "wEntry" : the default workspace configuration.
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/default-ws-config")
   public Response getDefaultWorkspaceConfig()
   {
      try
      {
         String defaultWorkspaceName =
            repositoryService.getDefaultRepository().getConfiguration().getDefaultWorkspaceName();

         for (WorkspaceEntry wEntry : repositoryService.getDefaultRepository().getConfiguration().getWorkspaceEntries())
            if (defaultWorkspaceName.equals(wEntry.getName()))
               return Response.ok(wEntry).cacheControl(noCache).build();

         return Response.status(Response.Status.NOT_FOUND).entity("Can not get default workspace configuration.").type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get default workspace configuration.").type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Gives the default repository configuration.
    * 
    * @return Response return the JSON to WorkspaceEntry
    *
    * @response
    * {code:json}
    * {
    *   "rEntry" : the default repository configuration.
    * }
    * {code}
    * @LevelAPI Provisional
    */
   @GET
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/info/default-repository-config")
   public Response getDefaultRepositoryConfig()
   {
      try
      {
         return Response.ok(repositoryService.getDefaultRepository().getConfiguration()).cacheControl(noCache).build();
      }
      catch (Throwable e) //NOSONAR
      {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get default workspace configuration.").type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * validateRepositoryName.
    * 
    * @param repositoryName
    *          the name of the repository
    * @throws RepositoryConfigurationException
    *           will be generated the exception RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generated the exception RepositoryException
    */
   private void validateRepositoryName(String repositoryName) throws RepositoryException,
      RepositoryConfigurationException
   {
      repositoryService.getRepository(repositoryName);
   }

   /**
    * validateWorkspaceName.
    * 
    * @param repositoryName
    *          the name of the repository
    * @param workspaceName
    *          the name of the workspace
    * @throws RepositoryConfigurationException
    *           will be generated the exception RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generated the exception RepositoryException
    * @throws NoSuchWorkspaceException
    *           will be generated the exception NoSuchWorkspaceException
    * @throws LoginException
    *           will be generated the exception LoginException
    */
   private void validateWorkspaceName(String repositoryName, String workspaceName) throws LoginException,
      NoSuchWorkspaceException, RepositoryException, RepositoryConfigurationException
   {
      Session ses =
         sessionProviderService.getSessionProvider(null).getSession(workspaceName,
            repositoryService.getRepository(repositoryName));
      ses.logout();
   }

   private boolean isWorkspaceExist(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      for (String workspace : repositoryService.getRepository(repositoryName).getWorkspaceNames())
      {
         if (workspaceName.equals(workspace))
         {
            return true;
         }
      }

      return false;
   }

   private boolean isRepositoryExist(String repositoryName) throws RepositoryException,
      RepositoryConfigurationException
   {
      try
      {
         return repositoryService.getRepository(repositoryName) != null;
      }
      catch (RepositoryException e)
      {
         return false;
      }

   }

   /**
    * validateOneBackupInstants.
    * 
    * @param repositoryName
    *          the name of the repository
    * @param workspaceName
    *          the name of the workspace
    * @throws WorkspaceRestoreExeption
    *           will be generated WorkspaceRestoreExeption
    */
   private void validateOneBackupInstants(String repositoryName, String workspaceName) throws WorkspaceRestoreExeption
   {

      BackupChain bch = backupManager.findBackup(repositoryName, workspaceName);

      if (bch != null)
         throw new WorkspaceRestoreExeption("The backup is already working on workspace '" + "/" + repositoryName + "/"
            + workspaceName + "'");
   }

   /**
    * validateOneRestoreInstants.
    * 
    * @param repositoryName
    *          the name of the repository
    * @param workspaceName
    *          the name of the workspace
    * @throws WorkspaceRestoreExeption
    *           will be generated WorkspaceRestoreExeption
    */
   private void validateOneRestoreInstants(String repositoryName, String workspaceName) throws WorkspaceRestoreExeption
   {

      for (JobWorkspaceRestore job : backupManager.getRestores())
         if (repositoryName.equals(job.getRepositoryName())
            && workspaceName.endsWith(job.getWorkspaceName())
            && (job.getStateRestore() == JobWorkspaceRestore.RESTORE_INITIALIZED 
                     || job.getStateRestore() == JobWorkspaceRestore.RESTORE_STARTED))
         {
            throw new WorkspaceRestoreExeption("The workspace '" + "/" + repositoryName + "/" + workspaceName
               + "' is already restoring.");
         }
   }

   /**
    * validateOneRepositoryRestoreInstants.
    * 
    * @param repositoryName
    *          the name of the repository
    * @throws WorkspaceRestoreExeption
    *           will be generated WorkspaceRestoreExeption
    */
   private void validateOneRepositoryRestoreInstants(String repositoryName) throws RepositoryRestoreExeption
   {
      for (JobRepositoryRestore job : backupManager.getRepositoryRestores())
      {
         if (repositoryName.equals(job.getRepositoryName())
            && (job.getStateRestore() == JobWorkspaceRestore.RESTORE_INITIALIZED 
                     || job.getStateRestore() == JobWorkspaceRestore.RESTORE_STARTED))
         {
            throw new RepositoryRestoreExeption("The repository '" + "/" + repositoryName + "' is already restoring.");
         }
      }
   }

   /**
    * forceCloseSession. Close sessions on specific workspace.
    * 
    * @param repositoryName
    *          repository name
    * @param workspaceName
    *          workspace name
    * @return int return the how many sessions was closed
    * @throws RepositoryConfigurationException
    *           will be generate RepositoryConfigurationException
    * @throws RepositoryException
    *           will be generate RepositoryException
    */
   private int forceCloseSession(String repositoryName, String workspaceName) throws RepositoryException,
      RepositoryConfigurationException
   {
      ManageableRepository mr = repositoryService.getRepository(repositoryName);
      WorkspaceContainerFacade wc = mr.getWorkspaceContainer(workspaceName);

      SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

      return sessionRegistry.closeSessions(workspaceName);
   }

   /**
    * getBackupLogbyId.
    * 
    * @param backupId
    *          String, the backup identifier
    * @return File return backup log file
    */
   private File getBackupLogbyId(String backupId)
   {
      FilenameFilter backupLogsFilter = new FilenameFilter()
      {

         public boolean accept(File dir, String name)
         {
            return (name.endsWith(".xml") && name.startsWith("backup-"));
         }
      };

      File[] files = PrivilegedFileHelper.listFiles(backupManager.getBackupDirectory(), backupLogsFilter);

      if (files.length != 0)
         for (File f : files)
            if (f.getName().replaceAll(".xml", "").replaceAll("backup-", "").equals(backupId))
               return f;

      return null;
   }

   /**
    * getRepositoryBackupLogbyId.
    * 
    * @param backupId
    *          String, the backup identifier
    * @return File return backup log file
    */
   private File getRepositoryBackupLogbyId(String backupId)
   {
      FilenameFilter backupLogsFilter = new FilenameFilter()
      {

         public boolean accept(File dir, String name)
         {
            return (name.endsWith(".xml") && name.startsWith(RepositoryBackupChainLog.PREFIX));
         }
      };

      File[] files = PrivilegedFileHelper.listFiles(backupManager.getBackupDirectory(), backupLogsFilter);

      if (files.length != 0)
         for (File f : files)
            if (f.getName().replaceAll(".xml", "").replaceAll(RepositoryBackupChainLog.PREFIX, "").equals(backupId))
               return f;

      return null;
   }
}
