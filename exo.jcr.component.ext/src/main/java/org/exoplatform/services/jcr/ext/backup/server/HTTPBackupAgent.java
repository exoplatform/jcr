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

import java.io.File;
import java.io.FilenameFilter;
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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.app.ThreadLocalSessionProviderService;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.impl.JobRepositoryRestore;
import org.exoplatform.services.jcr.ext.backup.impl.JobWorkspaceRestore;
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

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 24.02.2009
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
          * Restore repository operations.
          */
         public static final String RESTORE_REPOSITORY = "/restore-repository";

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
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.HTTPBackupAgent");

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
   private BackupManager backupManager;

   /**
    * Will be get session over base authenticate.
    */
   private ThreadLocalSessionProviderService sessionProviderService;

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
      ThreadLocalSessionProviderService sessionProviderService)
   {
      this.repositoryService = repoService;
      this.backupManager = backupManager;
      this.sessionProviderService = sessionProviderService;

      log.info("HTTPBackupAgent inited");
   }

   /**
    * The start backup.
    * 
    * @param bConfigBeen
    *          BackupConfigBeen, the been with backup configuration.
    * @param repository
    *          String, the repository name
    * @param workspace
    *          String, the workspace name
    * @return Response return the response
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/start/{repo}/{ws}")
   public Response start(BackupConfigBean bConfigBeen, @PathParam("repo") String repository,
      @PathParam("ws") String workspace)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         File backupDir = new File(bConfigBeen.getBackupDir());
         if (!backupDir.exists())
            throw new BackupDirNotFoundException("The backup folder not exists :  " + backupDir.getAbsolutePath());

         BackupConfig config = new BackupConfig();
         config.setBackupType(bConfigBeen.getBackupType());
         config.setRepository(repository);
         config.setWorkspace(workspace);
         config.setBackupDir(backupDir);
         config.setIncrementalJobPeriod(bConfigBeen.getIncrementalJobPeriod());
         config.setIncrementalJobNumber(bConfigBeen.getIncrementalRepetitionNumber());

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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not start backup", exception);

      return Response.status(status).entity("Can not start backup : " + failMessage).type(MediaType.TEXT_PLAIN)
         .cacheControl(noCache).build();
   }

   /**
    * The start repository backup.
    * 
    * @param bConfigBeen
    *          BackupConfigBeen, the been with backup configuration.
    * @param repository
    *          String, the repository name
    * @return Response return the response
    */
   @POST
   @Consumes(MediaType.APPLICATION_JSON)
   @Produces(MediaType.APPLICATION_JSON)
   @RolesAllowed("administrators")
   @Path("/start-backup-repository/{repo}")
   public Response startBackupRepository(BackupConfigBean bConfigBeen, @PathParam("repo") String repository)
   {
      String failMessage;
      Response.Status status;
      Throwable exception;

      try
      {
         File backupDir = new File(bConfigBeen.getBackupDir());
         if (!backupDir.exists())
         {
            throw new BackupDirNotFoundException("The backup folder not exists :  " + backupDir.getAbsolutePath());
         }

         RepositoryBackupConfig config = new RepositoryBackupConfig();
         config.setBackupType(bConfigBeen.getBackupType());
         config.setRepository(repository);
         config.setBackupDir(backupDir);
         config.setIncrementalJobPeriod(bConfigBeen.getIncrementalJobPeriod());
         config.setIncrementalJobNumber(bConfigBeen.getIncrementalRepetitionNumber());

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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not start backup", exception);

      return Response.status(status).entity("Can not start backup : " + failMessage).type(MediaType.TEXT_PLAIN)
         .cacheControl(noCache).build();
   }

   /**
    * The delete workspace.
    * 
    * @param repository
    *          String, the repository name
    * @param workspace
    *          String, the workspace name
    * @param forceSessionClose
    *          Boolean, flag to force session close
    * @return Response return the response
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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not drop the workspace '" + "/" + repository + "/" + workspace + "'", exception);

      return Response.status(status).entity(
         "Can not drop the workspace '" + "/" + repository + "/" + workspace + "' : " + failMessage).type(
         MediaType.TEXT_PLAIN).cacheControl(noCache).build();

   }

   /**
    * Restore the workspace.
    * 
    * @param wEntry
    *          WorkspaceEntry, the configuration to restored workspace
    * @param repository
    *          String, the repository name
    * @param backupId
    *          String, the identifier of backup
    * @return Response return the response
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
            throw new Exception("Workspace " + wEntry.getName() + " already exist!");

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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
         + "' from backup log with id '" + backupId + "'", exception);

      return Response.status(status).entity(
         "Can not start restore the workspace '" + "/" + repository + "/" + wEntry.getName()
            + "' from backup log with id '" + backupId + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(
         noCache).build();
   }

   /**
    * Restore the repository.
    * 
    * @param rEntry
    *          RepositoryEntry, the configuration to restored repository
    * @param repository
    *          String, the repository name
    * @param backupId
    *          String, the identifier of backup
    * @return Response return the response
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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup log with id '"
         + backupId + "'", exception);

      return Response.status(status).entity(
         "Can not start restore the repository '" + "/" + rEntry.getName() + "' from backup log with id '" + backupId
            + "' : " + failMessage).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
   }

   /**
    * The backup stop by 'id'.
    * 
    * @param backupId
    *          String, the identifier to backup
    * @return Response return the response
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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not stop backup", exception);

      return Response.status(status).entity("Can not stop backup : " + failMessage).type(MediaType.TEXT_PLAIN)
         .cacheControl(noCache).build();
   }

   /**
    * The repository backup stop by 'id'.
    * 
    * @param backupId
    *          String, the identifier to backup
    * @return Response return the response
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
      catch (Throwable e)
      {
         exception = e;
         status = Response.Status.INTERNAL_SERVER_ERROR;
         failMessage = e.getMessage();
      }

      log.error("Can not stop repository backup ", exception);

      return Response.status(status).entity("Can not stop repository backup : " + failMessage).type(
         MediaType.TEXT_PLAIN).cacheControl(noCache).build();
   }

   /**
    * Will be returned the backup service info.
    * 
    * @return Response return the response
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
               backupManager.getBackupDirectory().getAbsolutePath(), backupManager.getDefaultIncrementalJobPeriod());

         return Response.ok(infoBeen).cacheControl(noCache).build();
      }
      catch (Throwable e)
      {
         log.error("Can not get information about backup service", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about backup service : " + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the list short info of current and completed backups .
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current or completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the list short info of current and completed repository backups .
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current or completed reposioty backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed repository backups" + e.getMessage()).type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the detailed info of current or completed backup by 'id'.
    * 
    * @param id
    *          String, the identifier to backup
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current or completed backup with 'id' " + id, e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed backup with 'id' " + id + " : " + e.getMessage()).type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the detailed info of current or completed repository backup by 'id'.
    * 
    * @param id
    *          String, the identifier to repository backup
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current or completed repository backup with 'id' " + id, e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed repository backup with 'id' " + id + " : "
               + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the list short info of current backups .
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current backups" + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(
            noCache).build();
      }
   }

   /**
    * Will be returned the list short info of current backups .
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current repositorty backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current backups" + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(
            noCache).build();
      }
   }

   /**
    * Will be returned the list short info of completed backups .
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the list short info of completed backups .
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the list short info of current and completed backups. Filtered by specific
    * workspace.
    * 
    * @param repository
    *          String, the repository name
    * @param workspace
    *          String, the workspace name
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current or completed backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed backups" + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the list short info of current and completed backups. Filtered by specific
    * repository.
    * 
    * @param repository
    *          String, the repository name
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current or completed repository backups", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current or completed repository backups" + e.getMessage()).type(
            MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the detailed information about last restore for specific workspace.
    * 
    * @param repository
    *          String, the repository name
    * @param workspace
    *          String, the workspace name
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error(
            "Can not get information about current restore for workspace /" + repository + "/" + workspace + "'", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current restore for workspace /" + repository + "/" + workspace + "' : "
               + e.getMessage()).type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }

   }

   /**
    * Will be returned the detailed information about last restore for specific repository.
    * 
    * @param repository
    *          String, the repository name
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current restore for repository /" + repository + "'", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current restore for repository /" + repository + "' : " + e.getMessage())
            .type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }

   }

   /**
    * Will be returned the detailed information about last restores.
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current restores.", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current restores : " + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the detailed information about last restores.
    * 
    * @return Response return the response
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
      catch (Throwable e)
      {
         log.error("Can not get information about current repository restores.", e);

         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get information about current repository restores : " + e.getMessage()).type(MediaType.TEXT_PLAIN)
            .cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the default workspace configuration.
    * 
    * @return Response return the JSON to WorkspaceEntry
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
      catch (Throwable e)
      {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get default workspace configuration.").type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * Will be returned the default repository configuration.
    * 
    * @return Response return the JSON to WorkspaceEntry
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
      catch (Throwable e)
      {
         return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
            "Can not get default workspace configuration.").type(MediaType.TEXT_PLAIN).cacheControl(noCache).build();
      }
   }

   /**
    * validateRepositoryName.
    * 
    * @param repositoryName
    *          the repository name
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
    *          the repository name
    * @param workspaceName
    *          the workspace name
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
    *          the repository name
    * @param workspaceName
    *          the workspace name
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
    *          the repository name
    * @param workspaceName
    *          the workspace name
    * @throws WorkspaceRestoreExeption
    *           will be generated WorkspaceRestoreExeption
    */
   private void validateOneRestoreInstants(String repositoryName, String workspaceName) throws WorkspaceRestoreExeption
   {

      for (JobWorkspaceRestore job : backupManager.getRestores())
         if (repositoryName.equals(job.getRepositoryName())
            && workspaceName.endsWith(job.getWorkspaceName())
            && (job.getStateRestore() == JobWorkspaceRestore.RESTORE_INITIALIZED || job.getStateRestore() == JobWorkspaceRestore.RESTORE_STARTED))
         {
            throw new WorkspaceRestoreExeption("The workspace '" + "/" + repositoryName + "/" + workspaceName
               + "' is already restoring.");
         }
   }

   /**
    * validateOneRepositoryRestoreInstants.
    * 
    * @param repositoryName
    *          the repository name
    * @throws WorkspaceRestoreExeption
    *           will be generated WorkspaceRestoreExeption
    */
   private void validateOneRepositoryRestoreInstants(String repositoryName) throws RepositoryRestoreExeption
   {
      for (JobRepositoryRestore job : backupManager.getRepositoryRestores())
      {
         if (repositoryName.equals(job.getRepositoryName())
            && (job.getStateRestore() == JobWorkspaceRestore.RESTORE_INITIALIZED || job.getStateRestore() == JobWorkspaceRestore.RESTORE_STARTED))
         {
            throw new RepositoryRestoreExeption("The repository '" + "/" + repositoryName + "' is already restoring.");
         }
      }
   }

   /**
    * validateOneRestoreInstants.
    * 
    * @param repositoryName
    *          the repository name
    * @throws WorkspaceRestoreExeption
    *           will be generated WorkspaceRestoreExeption
    */
   private void validateOneRestoreInstants(String repositoryName) throws WorkspaceRestoreExeption
   {

      for (JobWorkspaceRestore job : backupManager.getRestores())
         if (repositoryName.equals(job.getRepositoryName())
            && (job.getStateRestore() == JobWorkspaceRestore.RESTORE_INITIALIZED || job.getStateRestore() == JobWorkspaceRestore.RESTORE_STARTED))
         {
            throw new WorkspaceRestoreExeption("The workspace '" + "/" + repositoryName + "' is already restoring.");
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

      File[] files = backupManager.getBackupDirectory().listFiles(backupLogsFilter);

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

      File[] files = backupManager.getBackupDirectory().listFiles(backupLogsFilter);

      if (files.length != 0)
         for (File f : files)
            if (f.getName().replaceAll(".xml", "").replaceAll(RepositoryBackupChainLog.PREFIX, "").equals(backupId))
               return f;

      return null;
   }
}
