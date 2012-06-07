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
package org.exoplatform.services.jcr.ext.backup.impl;

import org.apache.commons.collections.map.HashedMap;
import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.config.WorkspaceInitializerEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupJobListener;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.ExtendedBackupManager;
import org.exoplatform.services.jcr.ext.backup.JobEntryInfo;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.ext.backup.WorkspaceRestoreException;
import org.exoplatform.services.jcr.ext.backup.impl.fs.FullBackupJob;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.registry.RegistryEntry;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.jcr.impl.backup.JCRRestore;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SysViewWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.picocontainer.Startable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by The eXo Platform SAS .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class BackupManagerImpl implements ExtendedBackupManager, Startable
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.BackupManagerImpl");

   /**
    *  Name of default incremental job period parameter in configuration. 
    */
   public final static String DEFAULT_INCREMENTAL_JOB_PERIOD = "default-incremental-job-period";

   /**
    *  Name of backup properties parameter in configuration. 
    */
   public final static String BACKUP_PROPERTIES = "backup-properties";

   /**
    *  Name of full backup type parameter in configuration. 
    */
   public final static String FULL_BACKUP_TYPE = "full-backup-type";

   /**
    *  Name of incremental backup type parameter in configuration. 
    */
   public final static String INCREMENTAL_BACKUP_TYPE = "incremental-backup-type";

   /**
    *  Default value of incremental job period parameter in configuration. 
    */
   public final static String DEFAULT_VALUE_INCREMENTAL_JOB_PERIOD = "3600";

   /**
    *  Default value of incremental backup type parameter in configuration. 
    */
   public final static String DEFAULT_VALUE_INCREMENTAL_BACKUP_TYPE =
      org.exoplatform.services.jcr.ext.backup.impl.fs.IncrementalBackupJob.class.getName();

   /**
    *  Default value of incremental backup type parameter in configuration. 
    */
   public final static String DEFAULT_VALUE_FULL_BACKUP_TYPE =
      org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class.getName();

   /**
    *  Name of backup dir parameter in configuration. 
    */
   public final static String BACKUP_DIR = "backup-dir";

   /**
    *  Backup messages log max. size.
    */
   private static final int MESSAGES_MAXSIZE = 5;

   private static final String SERVICE_NAME = "BackupManager";

   /**
    * The timeout to checking finish of backup.
    */
   private static final long AUTO_STOPPER_TIMEOUT = 5000;

   /**
    * Value of default incremental job period.
    */
   private long defaultIncrementalJobPeriod;

   /**
    *  Value of default incremental period.
    */
   private String defIncrPeriod;

   /**
    *  Path to backup folder.
    */
   private String backupDir;

   /**
    * Value of full backup type.
    */
   private String fullBackupType;

   /**
    * Value of incremental backup type.
    */
   private String incrementalBackupType;

   /**
    * Set of current workspace backups.
    */
   private final Set<BackupChain> currentBackups;

   /**
    * Set of current repository backups.
    */
   private final Set<RepositoryBackupChain> currentRepositoryBackups;

   /**
    * The list of workspace restore job.
    */
   private List<JobWorkspaceRestore> restoreJobs;

   /**
    * The list of repository restore job.
    */
   private List<JobRepositoryRestore> restoreRepositoryJobs;

   /**
    * Initialization parameters of service.
    */
   private InitParams initParams;

   /**
    * Directory to log.
    */
   private File logsDirectory;

   private final RepositoryService repoService;

   private final RegistryService registryService;

   private final BackupMessagesLog messages;

   private final MessagesListener messagesListener;

   private final WorkspaceBackupAutoStopper workspaceBackupStopper;

   private final RepositoryBackupAutoStopper repositoryBackupStopper;

   /**
    * Temporary directory;
    */
   private final File tempDir;

   class MessagesListener implements BackupJobListener
   {

      public void onError(BackupJob job, String message, Throwable error)
      {
         messages.addError(makeJobInfo(job, error) + message, error);
         LOG.error(makeJobInfo(job, error) + message, error);
      }

      public void onStateChanged(BackupJob job)
      {
         messages.addMessage(makeJobInfo(job, null));
         if (LOG.isDebugEnabled())
         {
            LOG.debug(makeJobInfo(job, null));
         }
      }

      private String makeJobInfo(BackupJob job, Throwable error)
      {
         StringBuilder jobInfo = new StringBuilder();

         if (job != null)
         {
            switch (job.getType())
            {
               case BackupJob.FULL : {
                  jobInfo.append("FULL BACKUP");
                  break;
               }
               case BackupJob.INCREMENTAL : {
                  jobInfo.append("INCREMENTAL BACKUP");
                  break;
               }
            }

            jobInfo.append(" [");
            switch (job.getState())
            {
               case BackupJob.FINISHED : {
                  jobInfo.append("FINISHED");
                  break;
               }
               case BackupJob.STARTING : {
                  jobInfo.append("STARTING");
                  break;
               }
               case BackupJob.WAITING : {
                  jobInfo.append("WAITING");
                  break;
               }
               case BackupJob.WORKING : {
                  jobInfo.append("WORKING");
                  break;
               }
            }

            jobInfo.append("]");

            if (error != null)
            {
               jobInfo.append(" Error: ").append(error.getMessage());
            }

            try
            {
               jobInfo.append(" log: ").append(job.getStorageURL().getPath());
            }
            catch (BackupOperationException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
            finally
            {
               jobInfo.append(" ");
            }
         }

         return jobInfo.toString();
      }

      public String printStackTrace(Throwable error)
      {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         PrintWriter writer = new PrintWriter(out);
         error.printStackTrace(writer);

         return new String(out.toByteArray());
      }
   }

   class TaskFilter implements FileFilter
   {

      public boolean accept(File pathname)
      {
         return pathname.getName().endsWith(".task");
      }
   }

   class WorkspaceBackupAutoStopper extends Thread
   {

      private boolean isToBeStopped = false;

      WorkspaceBackupAutoStopper(ExoContainerContext ctx)
      {
         super("WorkspaceBackupAutoStopper" + (ctx == null ? "" : " " + ctx.getName()));
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void run()
      {
         while (!isToBeStopped)
         {
            try
            {
               Thread.sleep(AUTO_STOPPER_TIMEOUT);

               Iterator<BackupChain> it = currentBackups.iterator();
               List<BackupChain> stopedList = new ArrayList<BackupChain>();

               while (it.hasNext())
               {
                  BackupChain chain = it.next();
                  boolean isFinished = (chain.getBackupJobs().get(0).getState() == BackupJob.FINISHED);

                  for (int i = 1; i < chain.getBackupJobs().size(); i++)
                  {
                     isFinished &= (chain.getBackupJobs().get(i).getState() == BackupJob.FINISHED);
                  }

                  if (isFinished)
                  {
                     stopedList.add(chain);
                  }
               }

               // STOP backups
               for (BackupChain chain : stopedList)
               {
                  stopBackup(chain);
               }
            }
            catch (InterruptedException e)
            {
               LOG.error("The interapted this thread.", e);
            }
            catch (Throwable e)
            {
               LOG.error("The unknown error", e);
            }
         }
      }

      public void close()
      {
         isToBeStopped = true;
      }
   }

   class RepositoryBackupAutoStopper extends Thread
   {
      boolean isToBeStopped = false;

      RepositoryBackupAutoStopper(ExoContainerContext ctx)
      {
         super("RepositoryBackupAutoStopper" + (ctx == null ? "" : " " + ctx.getName()));
      }

      /**
       * {@inheritDoc}
       */
      @Override
      public void run()
      {
         while (!isToBeStopped)
         {
            try
            {
               Thread.sleep(AUTO_STOPPER_TIMEOUT);

               Iterator<RepositoryBackupChain> it = currentRepositoryBackups.iterator();
               List<RepositoryBackupChain> stopedList = new ArrayList<RepositoryBackupChain>();

               while (it.hasNext())
               {
                  RepositoryBackupChain chain = it.next();

                  if (chain.isFinished())
                  {
                     stopedList.add(chain);
                  }
               }

               // STOP backups
               for (RepositoryBackupChain chain : stopedList)
               {
                  stopBackup(chain);
               }
            }
            catch (InterruptedException e)
            {
               LOG.error("The interapted this thread.", e);
            }
            catch (Throwable e)
            {
               LOG.error("The unknown error", e);
            }
         }
      }

      public void close()
      {
         isToBeStopped = true;
      }
   }

   /**
    * BackupManagerImpl  constructor.
    *
    * @param initParams
    *          InitParams,  the init parameters
    * @param repoService
    *          RepositoryService, the repository service
    */
   public BackupManagerImpl(InitParams initParams, RepositoryService repoService)
   {
      this(null, initParams, repoService, null);
   }

   /**
    * BackupManagerImpl  constructor.
    *
    * @param initParams
    *          InitParams,  the init parameters
    * @param repoService
    *          RepositoryService, the repository service
    */
   public BackupManagerImpl(ExoContainerContext ctx, InitParams initParams, RepositoryService repoService)
   {
      this(ctx, initParams, repoService, null);
   }

   /**
    * BackupManagerImpl  constructor.
    *
    *          InitParams,  the init parameters
    * @param repoService
    *          RepositoryService, the repository service
    * @param registryService
    *          RegistryService, the registry service
    */
   public BackupManagerImpl(InitParams initParams, RepositoryService repoService, RegistryService registryService)
   {
      this(null, initParams, repoService, registryService);
   }

   /**
    * BackupManagerImpl  constructor.
    *
    *          InitParams,  the init parameters
    * @param repoService
    *          RepositoryService, the repository service
    * @param registryService
    *          RegistryService, the registry service
    */
   public BackupManagerImpl(ExoContainerContext ctx, InitParams initParams, RepositoryService repoService,
      RegistryService registryService)
   {
      this.messagesListener = new MessagesListener();
      this.repoService = repoService;
      this.registryService = registryService;
      this.initParams = initParams;
      this.tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

      currentBackups = Collections.synchronizedSet(new HashSet<BackupChain>());

      currentRepositoryBackups = Collections.synchronizedSet(new HashSet<RepositoryBackupChain>());

      messages = new BackupMessagesLog(MESSAGES_MAXSIZE);

      this.restoreJobs = new ArrayList<JobWorkspaceRestore>();
      this.restoreRepositoryJobs = new ArrayList<JobRepositoryRestore>();

      this.workspaceBackupStopper = new WorkspaceBackupAutoStopper(ctx);
      this.workspaceBackupStopper.start();

      this.repositoryBackupStopper = new RepositoryBackupAutoStopper(ctx);
      this.repositoryBackupStopper.start();
   }

   /**
    * {@inheritDoc}
    */
   public Set<BackupChain> getCurrentBackups()
   {
      return currentBackups;
   }

   /**
    * {@inheritDoc}
    */
   public BackupMessage[] getMessages()
   {
      return messages.getMessages();
   }

   /**
    * {@inheritDoc}
    */
   public BackupChainLog[] getBackupsLogs()
   {
      File[] cfs = PrivilegedFileHelper.listFiles(logsDirectory, new BackupLogsFilter());
      List<BackupChainLog> logs = new ArrayList<BackupChainLog>();
      for (int i = 0; i < cfs.length; i++)
      {
         File cf = cfs[i];

         try
         {
            if (!isCurrentBackup(cf))
            {
               logs.add(new BackupChainLog(cf));
            }
         }
         catch (BackupOperationException e)
         {
            LOG.warn("Log file " + PrivilegedFileHelper.getAbsolutePath(cf) + " is bussy or corrupted. Skipped. " + e,
               e);
         }
      }
      BackupChainLog[] ls = new BackupChainLog[logs.size()];
      logs.toArray(ls);
      return ls;
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryBackupChainLog[] getRepositoryBackupsLogs()
   {
      File[] cfs = PrivilegedFileHelper.listFiles(logsDirectory, new RepositoryBackupLogsFilter());
      List<RepositoryBackupChainLog> logs = new ArrayList<RepositoryBackupChainLog>();
      for (int i = 0; i < cfs.length; i++)
      {
         File cf = cfs[i];

         try
         {
            if (!isCurrentRepositoryBackup(cf))
            {
               logs.add(new RepositoryBackupChainLog(cf));
            }
         }
         catch (BackupOperationException e)
         {
            LOG.warn("Log file " + PrivilegedFileHelper.getAbsolutePath(cf) + " is bussy or corrupted. Skipped. " + e,
               e);
         }
      }
      RepositoryBackupChainLog[] ls = new RepositoryBackupChainLog[logs.size()];
      logs.toArray(ls);
      return ls;
   }

   /**
    * isCurrentBackup.
    * 
    * @param log
    *          File, the log to backup
    * @return boolean return the 'true' if this log is current backup.
    */
   private boolean isCurrentBackup(File log)
   {
      for (BackupChain chain : currentBackups)
      {
         if (log.getName().equals(new File(chain.getLogFilePath()).getName()))
         {
            return true;
         }
      }

      return false;
   }

   /**
    * isCurrentRepositoryBackup.
    * 
    * @param log
    *          File, the log to backup
    * @return boolean return the 'true' if this log is current backup.
    */
   private boolean isCurrentRepositoryBackup(File log)
   {
      for (RepositoryBackupChain chain : currentRepositoryBackups)
      {
         if (log.getName().equals(new File(chain.getLogFilePath()).getName()))
         {
            return true;
         }
      }

      return false;
   }

   protected void restoreOverInitializer(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry)
      throws BackupOperationException, RepositoryException, RepositoryConfigurationException,
      BackupConfigurationException
   {
      List<JobEntryInfo> list = log.getJobEntryInfos();
      BackupConfig config = log.getBackupConfig();

      String reposytoryName = (repositoryName == null ? config.getRepository() : repositoryName);
      String workspaceName = workspaceEntry.getName();

      String fullbackupType = null;

      try
      {
         if ((ClassLoading.forName(log.getFullBackupType(), this).equals(FullBackupJob.class)))
         {
            fullbackupType = log.getFullBackupType();
         }
         else if ((ClassLoading.forName(log.getFullBackupType(), this)
            .equals(org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class)))
         {
            fullbackupType = log.getFullBackupType();
         }
         else
         {
            throw new BackupOperationException("Class  \"" + log.getFullBackupType()
               + "\" is not support as full backup.");
         }
      }
      catch (ClassNotFoundException e)
      {
         throw new BackupOperationException("Class \"" + log.getFullBackupType() + "\" is not found.", e);
      }

      // ws should not exists.
      if (!workspaceAlreadyExist(reposytoryName, workspaceName))
      {
         for (int i = 0; i < list.size(); i++)
         {
            if (i == 0)
            {
               try
               {
                  fullRestoreOverInitializer(list.get(i).getURL().getPath(), reposytoryName, workspaceEntry,
                     fullbackupType);
               }
               catch (FileNotFoundException e)
               {
                  throw new BackupOperationException("Restore of full backup file error " + e, e);
               }
               catch (IOException e)
               {
                  throw new BackupOperationException("Restore of full backup file I/O error " + e, e);
               }
               catch (ClassNotFoundException e)
               {
                  throw new BackupOperationException("Restore of full backup class load error " + e, e);
               }

               repoService.getConfig().retain(); // save configuration to persistence (file or persister)
            }
            else
            {
               try
               {
                  incrementalRestore(list.get(i).getURL().getPath(), reposytoryName, workspaceName);
               }
               catch (FileNotFoundException e)
               {
                  throw new BackupOperationException("Restore of incremental backup file error " + e, e);
               }
               catch (IOException e)
               {
                  throw new BackupOperationException("Restore of incremental backup file I/O error " + e, e);
               }
               catch (ClassNotFoundException e)
               {
                  throw new BackupOperationException("Restore of incremental backup error " + e, e);
               }
            }
         }
      }
      else
      {
         throw new BackupConfigurationException("Workspace \"" + workspaceName + "\" should not exists.");
      }
   }

   private boolean workspaceAlreadyExist(String repository, String workspace) throws RepositoryException,
      RepositoryConfigurationException
   {
      String[] ws = repoService.getRepository(repository).getWorkspaceNames();

      for (int i = 0; i < ws.length; i++)
      {
         if (ws[i].equals(workspace))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public BackupChain startBackup(BackupConfig config) throws BackupOperationException, BackupConfigurationException,
      RepositoryException, RepositoryConfigurationException
   {
      return startBackup(config, null);
   }

   /**
    * Internally used for call with job listener from scheduler.
    * 
    * @param config
    * @param jobListener
    * @return
    * @throws BackupOperationException
    * @throws BackupConfigurationException
    * @throws RepositoryException
    * @throws RepositoryConfigurationException
    */
   BackupChain startBackup(BackupConfig config, BackupJobListener jobListener) throws BackupOperationException,
      BackupConfigurationException, RepositoryException, RepositoryConfigurationException
   {
      validateBackupConfig(config);

      Calendar startTime = Calendar.getInstance();
      File dir =
         FileNameProducer.generateBackupSetDir(config.getRepository(), config.getWorkspace(), config.getBackupDir()
            .getPath(), startTime);
      PrivilegedFileHelper.mkdirs(dir);
      config.setBackupDir(dir);

      BackupChain bchain =
         new BackupChainImpl(config, logsDirectory, repoService, fullBackupType, incrementalBackupType, IdGenerator
            .generate(), logsDirectory, startTime);

      bchain.addListener(messagesListener);
      bchain.addListener(jobListener);

      currentBackups.add(bchain);
      bchain.startBackup();

      return bchain;
   }

   /**
    * Initialize backup chain to workspace backup. 
    * 
    * @param config
    * @return
    * @throws BackupOperationException
    * @throws BackupConfigurationException
    * @throws RepositoryException
    * @throws RepositoryConfigurationException
    */
   private void validateBackupConfig(RepositoryBackupConfig config) throws BackupConfigurationException
   {
      if (config.getIncrementalJobPeriod() < 0)
      {
         throw new BackupConfigurationException("The parameter 'incremental job period' can not be negative.");
      }

      if (config.getIncrementalJobNumber() < 0)
      {
         throw new BackupConfigurationException("The parameter 'incremental job number' can not be negative.");
      }

      if (config.getIncrementalJobPeriod() == 0 && config.getBackupType() == BackupManager.FULL_AND_INCREMENTAL)
      {
         config.setIncrementalJobPeriod(defaultIncrementalJobPeriod);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stopBackup(BackupChain backup)
   {
      backup.stopBackup();
      currentBackups.remove(backup);
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      if (!PrivilegedFileHelper.exists(tempDir))
      {
         throw new IllegalStateException("Directory " + tempDir.getAbsolutePath() + " not found. Please create it.");
      }

      //remove if exists all old jcrrestorewi*.tmp files.
      File[] files = PrivilegedFileHelper.listFiles(tempDir, new JcrRestoreWiFilter());
      for (int i = 0; i < files.length; i++)
      {
         PrivilegedFileHelper.delete(files[i]);
      }

      // start all scheduled before tasks
      if (registryService != null && !registryService.getForceXMLConfigurationValue(initParams))
      {
         SessionProvider sessionProvider = SessionProvider.createSystemProvider();
         try
         {
            readParamsFromRegistryService(sessionProvider);
         }
         catch (Exception e)
         {
            readParamsFromFile();
            try
            {
               writeParamsToRegistryService(sessionProvider);
            }
            catch (Exception exc)
            {
               LOG.error("Cannot write init configuration to RegistryService.", exc);
            }
         }
         finally
         {
            sessionProvider.close();
         }
      }
      else
      {
         readParamsFromFile();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      workspaceBackupStopper.close();
      repositoryBackupStopper.close();

      // 1. stop current backup chains
      // for (Iterator iterator = currentBackups.iterator(); iterator.hasNext();) {
      // BackupChain bc = (BackupChain) iterator.next();
      // stopBackup(bc);
      // }
      //    
      // // 2. stop all scheduled tasks
      // scheduler = null;
   }

   private void fullRestoreOverInitializer(String pathBackupFile, String repositoryName, WorkspaceEntry workspaceEntry,
      String fBackupType) throws FileNotFoundException, IOException, RepositoryException,
      RepositoryConfigurationException, ClassNotFoundException
   {
      WorkspaceInitializerEntry wieOriginal = workspaceEntry.getInitializer();

      RepositoryImpl defRep = (RepositoryImpl)repoService.getRepository(repositoryName);

      WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();

      if ((ClassLoading.forName(fBackupType, this).equals(FullBackupJob.class)))
      {
         // set the initializer SysViewWorkspaceInitializer
         wiEntry.setType(SysViewWorkspaceInitializer.class.getCanonicalName());

         List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
         wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, pathBackupFile));

         wiEntry.setParameters(wieParams);
      }
      else if ((ClassLoading.forName(fBackupType, this)
         .equals(org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class)))
      {
         // set the initializer RdbmsWorkspaceInitializer
         wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

         List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
         wieParams.add(new SimpleParameterEntry(RdbmsWorkspaceInitializer.RESTORE_PATH_PARAMETER, new File(
            pathBackupFile).getParent()));

         wiEntry.setParameters(wieParams);
      }

      workspaceEntry.setInitializer(wiEntry);

      //restore
      defRep.configWorkspace(workspaceEntry);
      defRep.createWorkspace(workspaceEntry.getName());

      //set original workspace initializer
      WorkspaceContainerFacade wcf = defRep.getWorkspaceContainer(workspaceEntry.getName());
      WorkspaceEntry createdWorkspaceEntry = (WorkspaceEntry)wcf.getComponent(WorkspaceEntry.class);
      createdWorkspaceEntry.setInitializer(wieOriginal);
   }

   private void incrementalRestore(String pathBackupFile, String repositoryName, String workspaceName)
      throws RepositoryException, RepositoryConfigurationException, BackupOperationException, FileNotFoundException,
      IOException, ClassNotFoundException
   {
      WorkspaceContainerFacade workspaceContainer =
         repoService.getRepository(repositoryName).getWorkspaceContainer(workspaceName);
      WorkspacePersistentDataManager dataManager =
         (WorkspacePersistentDataManager)workspaceContainer.getComponent(WorkspacePersistentDataManager.class);
      JCRRestore restorer = new JCRRestore(dataManager);
      restorer.incrementalRestore(new File(pathBackupFile));
   }

   /**
    * Write parameters to RegistryService.
    * 
    * @param sessionProvider
    *          The SessionProvider
    * @throws ParserConfigurationException
    * @throws SAXException
    * @throws IOException
    * @throws RepositoryException
    */
   private void writeParamsToRegistryService(SessionProvider sessionProvider) throws IOException, SAXException,
      ParserConfigurationException, RepositoryException
   {
      Document doc = SecurityHelper.doPrivilegedParserConfigurationAction(new PrivilegedExceptionAction<Document>()
      {
         public Document run() throws Exception
         {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
         }
      });

      Element root = doc.createElement(SERVICE_NAME);
      doc.appendChild(root);

      Element element = doc.createElement(BACKUP_PROPERTIES);
      setAttributeSmart(element, BACKUP_DIR, backupDir);
      setAttributeSmart(element, DEFAULT_INCREMENTAL_JOB_PERIOD, defIncrPeriod);
      setAttributeSmart(element, FULL_BACKUP_TYPE, fullBackupType);
      setAttributeSmart(element, INCREMENTAL_BACKUP_TYPE, incrementalBackupType);
      root.appendChild(element);

      RegistryEntry serviceEntry = new RegistryEntry(doc);
      registryService.createEntry(sessionProvider, RegistryService.EXO_SERVICES, serviceEntry);
   }

   /**
    * Read parameters from RegistryService.
    * 
    * @param sessionProvider
    *          The SessionProvider
    * @throws RepositoryException
    * @throws PathNotFoundException
    */
   private void readParamsFromRegistryService(SessionProvider sessionProvider) throws PathNotFoundException,
      RepositoryException
   {
      String entryPath = RegistryService.EXO_SERVICES + "/" + SERVICE_NAME + "/" + BACKUP_PROPERTIES;
      RegistryEntry registryEntry = registryService.getEntry(sessionProvider, entryPath);
      Document doc = registryEntry.getDocument();
      Element element = doc.getDocumentElement();

      backupDir = getAttributeSmart(element, BACKUP_DIR);
      defIncrPeriod = getAttributeSmart(element, DEFAULT_INCREMENTAL_JOB_PERIOD);
      fullBackupType = getAttributeSmart(element, FULL_BACKUP_TYPE);
      incrementalBackupType = getAttributeSmart(element, INCREMENTAL_BACKUP_TYPE);

      LOG.info("Backup dir from RegistryService: " + backupDir);
      LOG.info("Default incremental job period from RegistryService: " + defIncrPeriod);
      LOG.info("Full backup type from RegistryService: " + fullBackupType);
      LOG.info("Incremental backup type from RegistryService: " + incrementalBackupType);

      checkParams();
   }

   /**
    * Get attribute value.
    * 
    * @param element
    *          The element to get attribute value
    * @param attr
    *          The attribute name
    * @return Value of attribute if present and null in other case
    */
   private String getAttributeSmart(Element element, String attr)
   {
      return element.hasAttribute(attr) ? element.getAttribute(attr) : null;
   }

   /**
    * Set attribute value. If value is null the attribute will be removed.
    * 
    * @param element
    *          The element to set attribute value
    * @param attr
    *          The attribute name
    * @param value
    *          The value of attribute
    */
   private void setAttributeSmart(Element element, String attr, String value)
   {
      if (value == null)
      {
         element.removeAttribute(attr);
      }
      else
      {
         element.setAttribute(attr, value);
      }
   }

   /**
    * Get parameters which passed from the file.
    * 
    * @throws RepositoryConfigurationException
    */
   private void readParamsFromFile()
   {
      PropertiesParam pps = initParams.getPropertiesParam(BACKUP_PROPERTIES);

      backupDir = pps.getProperty(BACKUP_DIR);
      // full backup type can be not defined. Using default.
      fullBackupType =
         pps.getProperty(FULL_BACKUP_TYPE) == null ? DEFAULT_VALUE_FULL_BACKUP_TYPE : pps.getProperty(FULL_BACKUP_TYPE);
      // incremental backup can be not configured. Using default values.
      defIncrPeriod =
         pps.getProperty(DEFAULT_INCREMENTAL_JOB_PERIOD) == null ? DEFAULT_VALUE_INCREMENTAL_JOB_PERIOD : pps
            .getProperty(DEFAULT_INCREMENTAL_JOB_PERIOD);
      incrementalBackupType =
         pps.getProperty(INCREMENTAL_BACKUP_TYPE) == null ? DEFAULT_VALUE_INCREMENTAL_BACKUP_TYPE : pps
            .getProperty(INCREMENTAL_BACKUP_TYPE);

      LOG.info("Backup dir from configuration file: " + backupDir);
      LOG.info("Full backup type from configuration file: " + fullBackupType);
      LOG.info("(Experimental) Incremental backup type from configuration file: " + incrementalBackupType);
      LOG.info("(Experimental) Default incremental job period from configuration file: " + defIncrPeriod);

      checkParams();
   }

   /**
    * Check read params and initialize.
    */
   private void checkParams()
   {
      if (backupDir == null)
      {
         throw new IllegalStateException(BACKUP_DIR + " not specified");
      }

      logsDirectory = new File(backupDir);
      if (!PrivilegedFileHelper.exists(logsDirectory))
      {
         if (!PrivilegedFileHelper.mkdirs(logsDirectory))
         {
            throw new IllegalStateException("Could not create the backup directory at "
               + logsDirectory.getAbsolutePath());
         }
      }

      if (defIncrPeriod == null)
      {
         throw new IllegalStateException(DEFAULT_INCREMENTAL_JOB_PERIOD + " not specified");
      }

      defaultIncrementalJobPeriod = Integer.valueOf(defIncrPeriod);

      if (fullBackupType == null)
      {
         throw new IllegalStateException(FULL_BACKUP_TYPE + " not specified");
      }

      if (incrementalBackupType == null)
      {
         throw new IllegalStateException(INCREMENTAL_BACKUP_TYPE + " not specified");
      }
   }

   /**
    * {@inheritDoc}
    */
   public BackupChain findBackup(String repository, String workspace)
   {
      Iterator<BackupChain> it = currentBackups.iterator();
      while (it.hasNext())
      {
         BackupChain chain = it.next();
         if (repository.equals(chain.getBackupConfig().getRepository())
            && workspace.equals(chain.getBackupConfig().getWorkspace()))
         {
            return chain;
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public BackupChain findBackup(String backupId)
   {
      Iterator<BackupChain> it = currentBackups.iterator();
      while (it.hasNext())
      {
         BackupChain chain = it.next();
         if (backupId.equals(chain.getBackupId()))
         {
            return chain;
         }
      }
      return null;
   }

   /**
    * getLogsDirectory.
    *
    * @return File
    *           return the logs directory
    */
   File getLogsDirectory()
   {
      return logsDirectory;
   }

   /**
    * {@inheritDoc}
    */
   public File getBackupDirectory()
   {
      return getLogsDirectory();
   }

   /**
    * {@inheritDoc}
    */
   public String getFullBackupType()
   {
      return fullBackupType;
   }

   /**
    * {@inheritDoc}
    */
   public String getIncrementalBackupType()
   {
      return incrementalBackupType;
   }

   /**
    * {@inheritDoc}
    */
   public long getDefaultIncrementalJobPeriod()
   {
      return defaultIncrementalJobPeriod;
   }

   /**
    * {@inheritDoc}
    */
   public List<JobWorkspaceRestore> getRestores()
   {
      return restoreJobs;
   }

   /**
    * {@inheritDoc}
    */
   public JobWorkspaceRestore getLastRestore(String repositoryName, String workspaceName)
   {
      for (int i = restoreJobs.size() - 1; i >= 0; i--)
      {
         JobWorkspaceRestore job = restoreJobs.get(i);

         if (repositoryName.equals(job.getRepositoryName()) && workspaceName.equals(job.getWorkspaceName()))
         {
            return job;

         }
      }

      return null;
   }

   /**
    * {@inheritDoc}
    */
   public JobRepositoryRestore getLastRepositoryRestore(String repositoryName)
   {

      for (int i = restoreRepositoryJobs.size() - 1; i >= 0; i--)
      {
         JobRepositoryRestore job = restoreRepositoryJobs.get(i);

         if (repositoryName.equals(job.getRepositoryName()))
         {
            return job;

         }
      }

      return null;
   }

   public List<JobRepositoryRestore> getRepositoryRestores()
   {
      return restoreRepositoryJobs;
   }

   /**
    * {@inheritDoc}
    */
   public void restore(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException, RepositoryException,
      RepositoryConfigurationException
   {
      if (workspaceEntry == null)
      {
         if (!log.getBackupConfig().getRepository().equals(repositoryName))
         {
            throw new WorkspaceRestoreException(
               "If workspaceEntry is null, so will be restored with original configuration. "
                  + "The repositoryName (\"" + repositoryName + "\")  should be equals original repository name (\""
                  + log.getBackupConfig().getRepository() + "\"). ");
         }

         if (log.getOriginalWorkspaceEntry() == null)
         {
            throw new RepositoryRestoreExeption("The backup log is not contains original repository log : "
               + log.getLogFilePath());
         }

         this.restore(log, log.getBackupConfig().getRepository(), log.getOriginalWorkspaceEntry(), asynchronous);
         return;
      }

      if (asynchronous)
      {
         JobWorkspaceRestore jobRestore =
            new JobWorkspaceRestore(repoService, this, repositoryName, log, workspaceEntry);
         restoreJobs.add(jobRestore);
         jobRestore.start();
      }
      else
      {
         this.restoreOverInitializer(log, repositoryName, workspaceEntry);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restore(RepositoryBackupChainLog log, RepositoryEntry repositoryEntry, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException, RepositoryException,
      RepositoryConfigurationException
   {
      if (repositoryEntry == null)
      {
         if (log.getOriginalRepositoryEntry() == null)
         {
            throw new RepositoryRestoreExeption("The backup log is not contains original repository log : "
               + log.getLogFilePath());
         }

         this.restore(log, log.getOriginalRepositoryEntry(), asynchronous);
         return;
      }

      this.restore(log, repositoryEntry, null, asynchronous);
   }

   /**
    * {@inheritDoc}
    */
   public void restore(RepositoryBackupChainLog rblog, RepositoryEntry repositoryEntry,
      Map<String, String> workspaceNamesCorrespondMap, boolean asynchronous) throws BackupOperationException,
      BackupConfigurationException, RepositoryException, RepositoryConfigurationException
   {
      // Checking repository exists. 
      try
      {
         repoService.getRepository(repositoryEntry.getName());
         throw new BackupConfigurationException("Repository \"" + repositoryEntry.getName() + "\" is already exists.");
      }
      catch (RepositoryException e)
      {
         //OK. Repository with "repositoryEntry.getName" is not exists.
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }

      Map<String, BackupChainLog> workspacesMapping = new HashedMap();

      Map<String, BackupChainLog> backups = new HashedMap();

      if (workspaceNamesCorrespondMap == null)
      {
         for (String path : rblog.getWorkspaceBackupsInfo())
         {
            BackupChainLog bLog = new BackupChainLog(new File(path));
            backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
         }

         if (!rblog.getSystemWorkspace().equals(repositoryEntry.getSystemWorkspaceName()))
         {
            throw new BackupConfigurationException(
               "The backup to system workspace is not system workspace in repository entry: "
                  + rblog.getSystemWorkspace() + " is not equal " + repositoryEntry.getSystemWorkspaceName());
         }

         if (backups.size() != repositoryEntry.getWorkspaceEntries().size())
         {
            throw new BackupConfigurationException(
               "The repository entry is contains more or less workspace entry than backups of workspace in "
                  + rblog.getLogFilePath());
         }

         for (WorkspaceEntry wsEntry : repositoryEntry.getWorkspaceEntries())
         {
            if (!backups.containsKey(wsEntry.getName()))
            {
               throw new BackupConfigurationException("The workspace '" + wsEntry.getName()
                  + "' is not found in backup " + rblog.getLogFilePath());
            }
            else
            {
               workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
            }
         }
      }
      else
      {
         // Create map [new_ws_name : backupLog to that workspace].
         for (String path : rblog.getWorkspaceBackupsInfo())
         {
            BackupChainLog bLog = new BackupChainLog(new File(path));

            if (!workspaceNamesCorrespondMap.containsKey(bLog.getBackupConfig().getWorkspace()))
            {
               throw new BackupConfigurationException("Can not found coresptonding workspace name to workspace '"
                  + bLog.getBackupConfig().getWorkspace() + "' in  " + workspaceNamesCorrespondMap.keySet());
            }

            backups.put(workspaceNamesCorrespondMap.get(bLog.getBackupConfig().getWorkspace()), bLog);
         }

         // Checking system workspace.
         if (!repositoryEntry.getSystemWorkspaceName().equals(
            workspaceNamesCorrespondMap.get(rblog.getSystemWorkspace())))
         {
            throw new BackupConfigurationException(
               "The backup to system workspace is not system workspace in repository entry: "
                  + repositoryEntry.getSystemWorkspaceName() + " is not equal "
                  + workspaceNamesCorrespondMap.get(rblog.getSystemWorkspace()));
         }

         // Checking count of corresponding workspaces.
         if (workspaceNamesCorrespondMap.size() != repositoryEntry.getWorkspaceEntries().size())
         {
            throw new BackupConfigurationException(
               "The repository entry is contains more or less workspace entry than backups of workspace in "
                  + rblog.getLogFilePath());
         }

         for (WorkspaceEntry wsEntry : repositoryEntry.getWorkspaceEntries())
         {
            if (!workspaceNamesCorrespondMap.containsValue(wsEntry.getName()))
            {
               throw new BackupConfigurationException("The workspace '" + wsEntry.getName()
                  + "' is not found workspaceNamesCorrespondMap  : " + workspaceNamesCorrespondMap.values());
            }
            else if (!backups.containsKey(wsEntry.getName()))
            {
               throw new BackupConfigurationException("The workspace '" + wsEntry.getName()
                  + "' is not found in backup " + rblog.getLogFilePath());
            }
            else
            {
               workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
            }
         }
      }

      JobRepositoryRestore jobRepositoryRestore =
         new JobRepositoryRestore(repoService, this, repositoryEntry, workspacesMapping, rblog);

      restoreRepositoryJobs.add(jobRepositoryRestore);
      if (asynchronous)
      {
         jobRepositoryRestore.start();
      }
      else
      {
         jobRepositoryRestore.restore();
      }
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryBackupChain startBackup(RepositoryBackupConfig config) throws BackupOperationException,
      BackupConfigurationException, RepositoryException, RepositoryConfigurationException
   {
      validateBackupConfig(config);

      File dir =
         new File(config.getBackupDir() + File.separator + "repository_" + config.getRepository() + "_backup_"
            + System.currentTimeMillis());
      PrivilegedFileHelper.mkdirs(dir);
      config.setBackupDir(dir);

      RepositoryBackupChain repositoryBackupChain =
         new RepositoryBackupChainImpl(config, logsDirectory, repoService, fullBackupType, incrementalBackupType,
            IdGenerator.generate());

      repositoryBackupChain.startBackup();

      currentRepositoryBackups.add(repositoryBackupChain);

      return repositoryBackupChain;
   }

   /**
    * {@inheritDoc}
    */
   public void stopBackup(RepositoryBackupChain backup)
   {
      backup.stopBackup();
      currentRepositoryBackups.remove(backup);
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryBackupChain findRepositoryBackup(String repository)
   {
      Iterator<RepositoryBackupChain> it = currentRepositoryBackups.iterator();
      while (it.hasNext())
      {
         RepositoryBackupChain chain = it.next();
         if (repository.equals(chain.getBackupConfig().getRepository()))
         {
            return chain;
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public Set<RepositoryBackupChain> getCurrentRepositoryBackups()
   {
      return currentRepositoryBackups;
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryBackupChain findRepositoryBackupId(String backupId)
   {
      Iterator<RepositoryBackupChain> it = currentRepositoryBackups.iterator();
      while (it.hasNext())
      {
         RepositoryBackupChain chain = it.next();
         if (backupId.equals(chain.getBackupId()))
         {
            return chain;
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingRepository(RepositoryBackupChainLog rblog, RepositoryEntry repositoryEntry,
      boolean asynchronous) throws BackupOperationException, BackupConfigurationException
   {
      try
      {
         // repository should be existed
         repoService.getRepository(repositoryEntry.getName());
      }
      catch (RepositoryException e)
      {
         throw new RepositoryRestoreExeption("Repository \"" + repositoryEntry.getName() + "\" should be existed", e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryRestoreExeption("Repository \"" + repositoryEntry.getName() + "\" should be existed", e);
      }

      Map<String, BackupChainLog> workspacesMapping = new HashedMap();

      Map<String, BackupChainLog> backups = new HashedMap();

      for (String path : rblog.getWorkspaceBackupsInfo())
      {
         BackupChainLog bLog = new BackupChainLog(new File(path));
         backups.put(bLog.getBackupConfig().getWorkspace(), bLog);
      }

      if (!rblog.getSystemWorkspace().equals(repositoryEntry.getSystemWorkspaceName()))
      {
         throw new BackupConfigurationException(
            "The backup to system workspace is not system workspace in repository entry: " + rblog.getSystemWorkspace()
               + " is not equal " + repositoryEntry.getSystemWorkspaceName());
      }

      if (backups.size() != repositoryEntry.getWorkspaceEntries().size())
      {
         throw new BackupConfigurationException(
            "The repository entry is contains more or less workspace entry than backups of workspace in "
               + rblog.getLogFilePath());
      }

      for (WorkspaceEntry wsEntry : repositoryEntry.getWorkspaceEntries())
      {
         if (!backups.containsKey(wsEntry.getName()))
         {
            throw new BackupConfigurationException("The workspace '" + wsEntry.getName() + "' is not found in backup "
               + rblog.getLogFilePath());
         }
         else
         {
            workspacesMapping.put(wsEntry.getName(), backups.get(wsEntry.getName()));
         }
      }

      // check if we have deal with RDBMS backup
      boolean isSameConfigRestore = false;
      try
      {
         if (ClassLoading.forName(
            workspacesMapping.get(repositoryEntry.getWorkspaceEntries().get(0).getName()).getFullBackupType(), this)
            .equals(org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class))
         {
            String newConf = new JsonGeneratorImpl().createJsonObject(repositoryEntry).toString();
            String currnetConf =
               new JsonGeneratorImpl().createJsonObject(
                  repoService.getRepository(repositoryEntry.getName()).getConfiguration()).toString();

            isSameConfigRestore = newConf.equals(currnetConf);
         }
      }
      catch (JsonException e)
      {
         this.LOG.error("Can't get JSON object from wokrspace configuration", e);
      }
      catch (RepositoryException e)
      {
         this.LOG.error(e);
      }
      catch (RepositoryConfigurationException e)
      {
         this.LOG.error(e);
      }
      catch (ClassNotFoundException e)
      {
         this.LOG.error(e);
      }

      JobRepositoryRestore jobExistedRepositoryRestore =
         isSameConfigRestore ? new JobExistingRepositorySameConfigRestore(repoService, this, repositoryEntry,
            workspacesMapping, rblog) : new JobExistingRepositoryRestore(repoService, this, repositoryEntry,
            workspacesMapping, rblog);

      restoreRepositoryJobs.add(jobExistedRepositoryRestore);
      if (asynchronous)
      {
         jobExistedRepositoryRestore.start();
      }
      else
      {
         jobExistedRepositoryRestore.restore();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingRepository(String repositoryBackupIdentifier, RepositoryEntry repositoryEntry,
      boolean asynchronous) throws BackupOperationException, BackupConfigurationException
   {
      RepositoryBackupChainLog backupChainLog = null;

      for (RepositoryBackupChainLog chainLog : getRepositoryBackupsLogs())
      {
         if (chainLog.getBackupId().equals(repositoryBackupIdentifier))
         {
            backupChainLog = chainLog;
            break;
         }
      }

      if (backupChainLog == null)
      {
         throw new BackupConfigurationException("Can not found backup of repository with id \""
            + repositoryBackupIdentifier + "\"");
      }

      this.restoreExistingRepository(backupChainLog, repositoryEntry, asynchronous);
   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingWorkspace(BackupChainLog log, String repositoryName, WorkspaceEntry workspaceEntry,
      boolean asynchronous) throws BackupOperationException, BackupConfigurationException
   {
      try
      {
         // repository should be existed
         repoService.getRepository(repositoryName);

         // workspace should be existed
         if (!workspaceAlreadyExist(repositoryName, workspaceEntry.getName()))
         {
            throw new WorkspaceRestoreException("Workspace \"" + workspaceEntry.getName()
               + "\" should be existed in repository \"" + repositoryName + "\".");
         }
      }
      catch (RepositoryException e)
      {
         throw new WorkspaceRestoreException("Repository \"" + repositoryName + "\" should be existed", e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new WorkspaceRestoreException("Repository \"" + repositoryName + "\" should be existed", e);
      }

      // check if we need to use restore with same configuration as original
      // it allows to use atomic restore in cluster env
      boolean isSameConfigRestore = false;
      try
      {
         if (ClassLoading.forName(log.getFullBackupType(), this).equals(
            org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class))
         {

            WorkspaceEntry currentWsEntry = null;
            for (WorkspaceEntry wsEntry : repoService.getRepository(repositoryName).getConfiguration()
               .getWorkspaceEntries())
            {
               if (wsEntry.getName().equals(workspaceEntry.getName()))
               {
                  currentWsEntry = wsEntry;
                  break;
               }
            }

            String newConf = new JsonGeneratorImpl().createJsonObject(workspaceEntry).toString();
            String currnetConf = new JsonGeneratorImpl().createJsonObject(currentWsEntry).toString();

            isSameConfigRestore = newConf.equals(currnetConf);
         }
      }
      catch (JsonException e)
      {
         this.LOG.error("Can't get JSON object from wokrspace configuration", e);
      }
      catch (RepositoryException e)
      {
         this.LOG.error(e);
      }
      catch (RepositoryConfigurationException e)
      {
         this.LOG.error(e);
      }
      catch (ClassNotFoundException e)
      {
         this.LOG.error(e);
      }

      JobWorkspaceRestore jobRestore =
         isSameConfigRestore ? new JobExistingWorkspaceSameConfigRestore(repoService, this, repositoryName, log,
            workspaceEntry) : new JobExistingWorkspaceRestore(repoService, this, repositoryName, log, workspaceEntry);
      restoreJobs.add(jobRestore);

      if (asynchronous)
      {
         jobRestore.start();
      }
      else
      {
         try
         {
            jobRestore.restore();
         }
         catch (Throwable e)
         {
            throw new BackupOperationException(e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingWorkspace(String workspaceBackupIdentifier, String repositoryName,
      WorkspaceEntry workspaceEntry, boolean asynchronous) throws BackupOperationException,
      BackupConfigurationException
   {
      BackupChainLog backupChainLog = null;

      for (BackupChainLog chainLog : getBackupsLogs())
      {
         if (chainLog.getBackupId().equals(workspaceBackupIdentifier))
         {
            backupChainLog = chainLog;
            break;
         }
      }

      if (backupChainLog == null)
      {
         throw new BackupConfigurationException("Can not found backup of workspace with id \""
            + workspaceBackupIdentifier + "\"");
      }

      this.restoreExistingWorkspace(backupChainLog, repositoryName, workspaceEntry, asynchronous);
   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingRepository(String repositoryBackupIdentifier, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException
   {
      RepositoryBackupChainLog backupChainLog = null;

      for (RepositoryBackupChainLog chainLog : getRepositoryBackupsLogs())
      {
         if (chainLog.getBackupId().equals(repositoryBackupIdentifier))
         {
            backupChainLog = chainLog;
            break;
         }
      }

      if (backupChainLog == null)
      {
         throw new BackupConfigurationException("Can not found backup of repository with id \""
            + repositoryBackupIdentifier + "\"");
      }

      this.restoreExistingRepository(backupChainLog, backupChainLog.getOriginalRepositoryEntry(), asynchronous);

   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingWorkspace(String workspaceBackupIdentifier, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException
   {
      BackupChainLog backupChainLog = null;

      for (BackupChainLog chainLog : getBackupsLogs())
      {
         if (chainLog.getBackupId().equals(workspaceBackupIdentifier))
         {
            backupChainLog = chainLog;
            break;
         }
      }

      if (backupChainLog == null)
      {
         throw new BackupConfigurationException("Can not found backup of workspace with id \""
            + workspaceBackupIdentifier + "\"");
      }

      this.restoreExistingWorkspace(backupChainLog, backupChainLog.getBackupConfig().getRepository(), backupChainLog
         .getOriginalWorkspaceEntry(), asynchronous);
   }

   /**
    * {@inheritDoc}
    */
   public void restoreRepository(String repositoryBackupIdentifier, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException
   {
      RepositoryBackupChainLog backupChainLog = null;

      for (RepositoryBackupChainLog chainLog : getRepositoryBackupsLogs())
      {
         if (chainLog.getBackupId().equals(repositoryBackupIdentifier))
         {
            backupChainLog = chainLog;
            break;
         }
      }

      if (backupChainLog == null)
      {
         throw new BackupConfigurationException("Can not found backup of repository with id \""
            + repositoryBackupIdentifier + "\"");
      }

      try
      {
         this.restore(backupChainLog, backupChainLog.getOriginalRepositoryEntry(), asynchronous);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryRestoreExeption("Repository \"" + backupChainLog.getOriginalRepositoryEntry().getName()
            + "\" was not restored", e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryRestoreExeption("Repository \"" + backupChainLog.getOriginalRepositoryEntry().getName()
            + "\" was not restored", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restoreWorkspace(String workspaceBackupIdentifier, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException
   {
      BackupChainLog backupChainLog = null;

      for (BackupChainLog chainLog : getBackupsLogs())
      {
         if (chainLog.getBackupId().equals(workspaceBackupIdentifier))
         {
            backupChainLog = chainLog;
            break;
         }
      }

      if (backupChainLog == null)
      {
         throw new BackupConfigurationException("Can not found backup of workspace with id \""
            + workspaceBackupIdentifier + "\"");
      }

      try
      {
         this.restore(backupChainLog, backupChainLog.getBackupConfig().getRepository(), backupChainLog
            .getOriginalWorkspaceEntry(), asynchronous);
      }
      catch (RepositoryException e)
      {
         throw new WorkspaceRestoreException("Workapce \"" + backupChainLog.getOriginalWorkspaceEntry().getName()
            + "\" was not restored in repository \"" + backupChainLog.getBackupConfig().getRepository() + "\"", e);
      }
      catch (RepositoryConfigurationException e)
      {

         throw new WorkspaceRestoreException("Workapce \"" + backupChainLog.getOriginalWorkspaceEntry().getName()
            + "\" was not restored in repository \"" + backupChainLog.getBackupConfig().getRepository() + "\"", e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingRepository(File repositoryBackupSetDir, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException
   {
      File[] cfs = PrivilegedFileHelper.listFiles(repositoryBackupSetDir, new RepositoryBackupLogsFilter());

      if (cfs.length == 0)
      {
         throw new BackupConfigurationException("Can not found repository backup log in directory : "
            + repositoryBackupSetDir.getPath());
      }

      if (cfs.length > 1)
      {
         throw new BackupConfigurationException(
            "Backup set directory should contains only one repository backup log : " + repositoryBackupSetDir.getPath());
      }

      RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(cfs[0]);

      this.restoreExistingRepository(backupChainLog, backupChainLog.getOriginalRepositoryEntry(), asynchronous);
   }

   /**
    * {@inheritDoc}
    */
   public void restoreExistingWorkspace(File workspaceBackupSetDir, boolean asynchronous)
      throws BackupOperationException, BackupConfigurationException
   {
      File[] cfs = PrivilegedFileHelper.listFiles(workspaceBackupSetDir, new BackupLogsFilter());

      if (cfs.length == 0)
      {
         throw new BackupConfigurationException("Can not found workspace backup log in directory : "
            + workspaceBackupSetDir.getPath());
      }

      if (cfs.length > 1)
      {
         throw new BackupConfigurationException("Backup set directory should contains only one workspace backup log : "
            + workspaceBackupSetDir.getPath());
      }

      BackupChainLog backupChainLog = new BackupChainLog(cfs[0]);

      this.restoreExistingWorkspace(backupChainLog, backupChainLog.getBackupConfig().getRepository(), backupChainLog
         .getOriginalWorkspaceEntry(), asynchronous);
   }

   /**
    * {@inheritDoc}
    */
   public void restoreRepository(File repositoryBackupSetDir, boolean asynchronous) throws BackupOperationException,
      BackupConfigurationException
   {
      File[] cfs = PrivilegedFileHelper.listFiles(repositoryBackupSetDir, new RepositoryBackupLogsFilter());

      if (cfs.length == 0)
      {
         throw new BackupConfigurationException("Can not found repository backup log in directory : "
            + repositoryBackupSetDir.getPath());
      }

      if (cfs.length > 1)
      {
         throw new BackupConfigurationException(
            "Backup set directory should contains only one repository backup log : " + repositoryBackupSetDir.getPath());
      }

      RepositoryBackupChainLog backupChainLog = new RepositoryBackupChainLog(cfs[0]);

      try
      {
         this.restore(backupChainLog, backupChainLog.getOriginalRepositoryEntry(), asynchronous);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryRestoreExeption("Repository \"" + backupChainLog.getOriginalRepositoryEntry().getName()
            + "\" was not restored", e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryRestoreExeption("Repository \"" + backupChainLog.getOriginalRepositoryEntry().getName()
            + "\" was not restored", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restoreWorkspace(File workspaceBackupSetDir, boolean asynchronous) throws BackupOperationException,
      BackupConfigurationException
   {
      File[] cfs = PrivilegedFileHelper.listFiles(workspaceBackupSetDir, new BackupLogsFilter());

      if (cfs.length == 0)
      {
         throw new BackupConfigurationException("Can not found workspace backup log in directory : "
            + workspaceBackupSetDir.getPath());
      }

      if (cfs.length > 1)
      {
         throw new BackupConfigurationException("Backup set directory should contains only one workspace backup log : "
            + workspaceBackupSetDir.getPath());
      }

      BackupChainLog backupChainLog = new BackupChainLog(cfs[0]);

      try
      {
         this.restore(backupChainLog, backupChainLog.getBackupConfig().getRepository(), backupChainLog
            .getOriginalWorkspaceEntry(), asynchronous);
      }
      catch (RepositoryException e)
      {
         throw new WorkspaceRestoreException("Workapce \"" + backupChainLog.getOriginalWorkspaceEntry().getName()
            + "\" was not restored in repository \"" + backupChainLog.getBackupConfig().getRepository() + "\"", e);
      }
      catch (RepositoryConfigurationException e)
      {

         throw new WorkspaceRestoreException("Workapce \"" + backupChainLog.getOriginalWorkspaceEntry().getName()
            + "\" was not restored in repository \"" + backupChainLog.getBackupConfig().getRepository() + "\"", e);
      }

   }
}
