/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.config.WorkspaceInitializerEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryRestoreExeption;
import org.exoplatform.services.jcr.ext.backup.impl.fs.FullBackupJob;
import org.exoplatform.services.jcr.impl.core.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.core.SysViewWorkspaceInitializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class JobRepositoryRestore extends Thread
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.JobRepositoryRestore");

   /**
    * REPOSITORY_RESTORE_STARTED. The state of start restore.
    */
   public static final int REPOSITORY_RESTORE_STARTED = 1;

   /**
    * REPOSITORY_RESTORE_SUCCESSFUL. The state of restore successful.
    */
   public static final int REPOSITORY_RESTORE_SUCCESSFUL = 2;

   /**
    * REPOSITORY_RESTORE_FAIL. The state of restore fail.
    */
   public static final int REPOSITORY_RESTORE_FAIL = 3;

   /**
    * REPOSITORY_RESTORE_STARTED. The state of initialized restore.
    */
   public static final int REPOSITORY_RESTORE_INITIALIZED = 4;

   /**
    * The state of restore.
    */
   private int stateRestore;

   /**
    * The start time of restore.
    */
   private Calendar startTime;

   /**
    * The end time of restore.
    */
   private Calendar endTime;

   /**
    * The exception on restore.
    */
   private Throwable restoreException = null;

   protected RepositoryService repositoryService;

   private BackupManagerImpl backupManager;

   protected RepositoryEntry repositoryEntry;

   private Map<String, BackupChainLog> workspacesMapping;

   private RepositoryBackupChainLog repositoryBackupChainLog;

   public JobRepositoryRestore(RepositoryService repoService, BackupManagerImpl backupManagerImpl,
      RepositoryEntry repositoryEntry, Map<String, BackupChainLog> workspacesMapping,
      RepositoryBackupChainLog backupChainLog)
   {
      this.repositoryService = repoService;
      this.backupManager = backupManagerImpl;
      this.repositoryEntry = repositoryEntry;
      this.workspacesMapping = workspacesMapping;
      this.repositoryBackupChainLog = backupChainLog;
   }

   /**
    * Restore repository. Provide information about start and finish process.
    * 
    * @throws RepositoryRestoreExeption
    *       if exception occurred during restore 
    */
   protected void restore() throws RepositoryRestoreExeption
   {
      try
      {
         stateRestore = REPOSITORY_RESTORE_STARTED;
         startTime = Calendar.getInstance();

         restoreRepository();

         stateRestore = REPOSITORY_RESTORE_SUCCESSFUL;
         endTime = Calendar.getInstance();
      }
      catch (Throwable t)
      {
         stateRestore = REPOSITORY_RESTORE_FAIL;
         restoreException = t;

         throw new RepositoryRestoreExeption(t.getMessage(), t);
      }
   }

   /**
    * Will be restored the workspace.
    * @throws RepositoryRestoreExeption 
    * @throws ClassNotFoundException 
    * @throws BackupOperationException 
    * 
    * @throws Throwable
    *           will be generated the Throwable
    */
   protected void restoreRepository() throws RepositoryRestoreExeption, BackupOperationException,
            ClassNotFoundException
   {
      List<WorkspaceEntry> originalWorkspaceEntrys = repositoryEntry.getWorkspaceEntries();

      //Getting system workspace entry
      WorkspaceEntry systemWorkspaceEntry = null;

      for (WorkspaceEntry wsEntry : originalWorkspaceEntrys)
      {
         if (wsEntry.getName().equals(repositoryEntry.getSystemWorkspaceName()))
         {
            systemWorkspaceEntry = wsEntry;
            break;
         }
      }

      WorkspaceInitializerEntry wieOriginal = systemWorkspaceEntry.getInitializer();

      //getting backup chail log to system workspace.
      BackupChainLog systemBackupChainLog = workspacesMapping.get(systemWorkspaceEntry.getName());
      
      WorkspaceInitializerEntry wiEntry = getWorkspaceInitializerEntry(systemBackupChainLog);

      // set initializer
      systemWorkspaceEntry.setInitializer(wiEntry);

      ArrayList<WorkspaceEntry> newEntries = new ArrayList<WorkspaceEntry>();
      newEntries.add(systemWorkspaceEntry);

      repositoryEntry.setWorkspaceEntries(newEntries);

      String currennWorkspaceName = repositoryEntry.getSystemWorkspaceName();

      boolean restored = true;
      try
      {
         repositoryService.createRepository(repositoryEntry);

         //set original initializer to created workspace.
         RepositoryImpl defRep = (RepositoryImpl)repositoryService.getRepository(repositoryEntry.getName());
         WorkspaceContainerFacade wcf = defRep.getWorkspaceContainer(systemWorkspaceEntry.getName());
         WorkspaceEntry createdWorkspaceEntry = (WorkspaceEntry)wcf.getComponent(WorkspaceEntry.class);
         createdWorkspaceEntry.setInitializer(wieOriginal);

         // save configuration to persistence (file or persister)
         repositoryService.getConfig().retain();

         for (WorkspaceEntry wsEntry : originalWorkspaceEntrys)
         {
            if (!(wsEntry.getName().equals(repositoryEntry.getSystemWorkspaceName())))
            {
               currennWorkspaceName = wsEntry.getName();
               backupManager.restore(workspacesMapping.get(wsEntry.getName()), repositoryEntry.getName(), wsEntry,
                  false);
            }
         }
      }
      catch (InvalidItemStateException e)
      {
         restored = false;

         log.error("Can not restore workspace \"" + currennWorkspaceName + " in repository \""
            + repositoryEntry.getName() + "\".", e);

         throw new RepositoryRestoreExeption("Can not restore workspace \"" + currennWorkspaceName
            + " in repository \"" + repositoryEntry.getName() + "\"." + " There was database error.", e);

      }
      catch (Throwable t)
      {
         restored = false;

         log.error("Can not restore workspace \"" + currennWorkspaceName + " in repository \""
            + repositoryEntry.getName() + "\".", t);

         throw new RepositoryRestoreExeption("Can not restore workspace \"" + currennWorkspaceName
            + " in repository \"" + repositoryEntry.getName() + "\".", t);

      }
      finally
      {
         if (!restored)
         {
            try
            {
               ManageableRepository mr = null;

               try
               {
                  mr = repositoryService.getRepository(repositoryEntry.getName());
               }
               catch (RepositoryException e)
               {
                  // The repository not exist.
               }

               if (mr != null)
               {
                  closeAllSession(mr);
                  repositoryService.removeRepository(repositoryEntry.getName());
                  repositoryService.getConfig().retain(); // save configuration to persistence (file or persister)
               }
            }
            catch (Throwable thr)
            {
               log.error("The partly restored repository \"" + repositoryEntry.getName() + "\" can not be removed.",
                  thr);
            }
         }
      }
   }

   private WorkspaceInitializerEntry getWorkspaceInitializerEntry(BackupChainLog systemBackupChainLog)
            throws BackupOperationException, ClassNotFoundException
   {
      String fullBackupPath = systemBackupChainLog.getJobEntryInfos().get(0).getURL().getPath();

      String fullbackupType = null;
      try
      {
         if ((Class.forName(systemBackupChainLog.getFullBackupType()).equals(FullBackupJob.class)))
         {
            fullbackupType = systemBackupChainLog.getFullBackupType();
         }
         else if ((Class.forName(systemBackupChainLog.getFullBackupType())
                  .equals(org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class)))
         {
            fullbackupType = systemBackupChainLog.getFullBackupType();
         }
         else
         {
            throw new BackupOperationException("Class  \"" + systemBackupChainLog.getFullBackupType()
                     + "\" is not support as full backup.");
         }
      }
      catch (ClassNotFoundException e)
      {
         throw new BackupOperationException("Class \"" + systemBackupChainLog.getFullBackupType() + "\" is not found.",
                  e);
      }

      WorkspaceInitializerEntry wiEntry = new WorkspaceInitializerEntry();
      if ((Class.forName(fullbackupType).equals(FullBackupJob.class)))
      {
         // set the initializer SysViewWorkspaceInitializer
         wiEntry.setType(SysViewWorkspaceInitializer.class.getCanonicalName());

         List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
         wieParams.add(new SimpleParameterEntry(SysViewWorkspaceInitializer.RESTORE_PATH_PARAMETER, fullBackupPath));

         wiEntry.setParameters(wieParams);
      }
      else if ((Class.forName(fullbackupType)
               .equals(org.exoplatform.services.jcr.ext.backup.impl.rdbms.FullBackupJob.class)))
      {
         // set the initializer RdbmsWorkspaceInitializer
         wiEntry.setType(RdbmsWorkspaceInitializer.class.getCanonicalName());

         List<SimpleParameterEntry> wieParams = new ArrayList<SimpleParameterEntry>();
         wieParams.add(new SimpleParameterEntry(RdbmsWorkspaceInitializer.RESTORE_PATH_PARAMETER, (new File(
                  fullBackupPath).getParent())));

         wiEntry.setParameters(wieParams);
      }

      return wiEntry;
   }

   /**
    * Close all open session in repository
    * 
    * @param mr
    *  
    * @throws NoSuchWorkspaceException
    */
   private void closeAllSession(ManageableRepository mr) throws NoSuchWorkspaceException
   {
      for (String wsName : mr.getWorkspaceNames())
      {
         if (!mr.canRemoveWorkspace(wsName))
         {
            WorkspaceContainerFacade wc = mr.getWorkspaceContainer(wsName);
            SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);
            sessionRegistry.closeSessions(wsName);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void run()
   {
      try
      {
         stateRestore = REPOSITORY_RESTORE_STARTED;
         startTime = Calendar.getInstance();

         restoreRepository();

         stateRestore = REPOSITORY_RESTORE_SUCCESSFUL;
         endTime = Calendar.getInstance();
      }
      catch (Throwable t)
      {
         stateRestore = REPOSITORY_RESTORE_FAIL;
         restoreException = t;

         log.error("The restore was fail", t);
      }
   }

   /**
    * getRestoreException.
    * 
    * @return Throwable return the exception of repository restore.
    */
   public Throwable getRestoreException()
   {
      return restoreException;
   }

   /**
    * getStateRestore.
    * 
    * @return int return state of restore.
    */
   public int getStateRestore()
   {
      return stateRestore;
   }

   /**
    * getBeginTime.
    * 
    * @return Calendar return the start time of restore
    */
   public Calendar getStartTime()
   {
      return startTime;
   }

   /**
    * getEndTime.
    * 
    * @return Calendar return the end time of restore
    */
   public Calendar getEndTime()
   {
      return endTime;
   }

   /**
    * getRepositoryName.
    *
    * @return String
    *           the name of destination repository
    */
   public String getRepositoryName()
   {
      return repositoryEntry.getName();
   }

   /**
    * GetRepositoryBackupChainLog.
    * 
    * @return repositoryBackupChainLog
    */
   public RepositoryBackupChainLog getRepositoryBackupChainLog()
   {
      return repositoryBackupChainLog;
   }

   /**
    * getRepositoryEntry.
    * 
    * @return repositoryBackupChainLog
    */
   public RepositoryEntry getRepositoryEntry()
   {
      return repositoryEntry;
   }

}
