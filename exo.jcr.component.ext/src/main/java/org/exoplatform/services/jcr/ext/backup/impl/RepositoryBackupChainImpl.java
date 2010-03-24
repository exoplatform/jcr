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

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChain;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupConfig;
import org.exoplatform.services.jcr.util.IdGenerator;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class RepositoryBackupChainImpl
   implements RepositoryBackupChain
{
   
   private final RepositoryBackupConfig config;

   private final Set<BackupChain> workspaceBackups;
   
   private final RepositoryBackupChainLog repositoryChainLog;

   private final String repositoryBackupId;
   
   private final Calendar startTime;

   private int state;

   public RepositoryBackupChainImpl(RepositoryBackupConfig config,  File logDirectory, ManageableRepository repository,
            String fullBackupType, String incrementalBackupType, String repositoryBackupId) throws BackupOperationException,
            BackupConfigurationException
   {
      this.config = config;
      this.workspaceBackups = Collections.synchronizedSet(new HashSet<BackupChain>());
      this.startTime = Calendar.getInstance();
      this.repositoryBackupId = repositoryBackupId;

      List<String> wsLogFilePathList = new ArrayList<String>();
      
      for (WorkspaceEntry workspaceEntry : repository.getConfiguration().getWorkspaceEntries()) 
      {
         BackupConfig wsBackupConfig = new BackupConfig();
         wsBackupConfig.setRepository(config.getRepository());
         wsBackupConfig.setWorkspace(workspaceEntry.getName());
         wsBackupConfig.setBackupDir(config.getBackupDir());
         wsBackupConfig.setBackupType(config.getBackupType());
         wsBackupConfig.setIncrementalJobNumber(config.getIncrementalJobNumber());
         wsBackupConfig.setIncrementalJobPeriod(config.getIncrementalJobPeriod());

         BackupChain bchain =
            new BackupChainImpl(wsBackupConfig, config.getBackupDir(), repository,
                     fullBackupType, incrementalBackupType, IdGenerator.generate());
         
         wsLogFilePathList.add(bchain.getLogFilePath());
         workspaceBackups.add(bchain);
      }
      
      this.repositoryChainLog = new RepositoryBackupChainLog(logDirectory, this.config, repository.getConfiguration().getSystemWorkspaceName(),  wsLogFilePathList, this.repositoryBackupId, startTime);
      
      state = INITIALIZED;
   }

   /**
    * {@inheritDoc}
    */
   public String getLogFilePath()
   {
      return repositoryChainLog.getLogFilePath();
   }

   /**
    * {@inheritDoc}
    */
   public int getState()
   {
      if (state != FINISHED)
      {
         int fullBackupsState =-1;
         int incrementalBackupsState = -1;

         for (BackupChain bc : workspaceBackups)
         {
            fullBackupsState = bc.getFullBackupState();

            if (bc.getBackupConfig().getBackupType() == BackupManager.FULL_AND_INCREMENTAL)
            {
               incrementalBackupsState = bc.getIncrementalBackupState();
            }
         }

         if (config.getBackupType() == BackupManager.FULL_BACKUP_ONLY)
         {
            if (fullBackupsState == BackupJob.FINISHED)
            {
               state = FINISHED;
            }
            else
            {
               state = WORKING;
            }
         }
         else
         {
            if (fullBackupsState == BackupJob.FINISHED && incrementalBackupsState == BackupJob.FINISHED)
            {
               state = FINISHED;
            }
            else if (fullBackupsState == BackupJob.FINISHED && incrementalBackupsState != BackupJob.WORKING)
            {
               state = FULL_BACKUP_FINISHED_INCREMENTAL_BACKUP_WORKING;
            }
            
            else
            {
               state = WORKING;
            }
         }
      }
      
      return state;
   }

   /**
    * {@inheritDoc}
    */
   public boolean isFinished()
   {
      return this.getState() == FINISHED;
   }

   /**
    * {@inheritDoc}
    */
   public void startBackup()
   {
      for (BackupChain wsChain : workspaceBackups) 
      {
         wsChain.startBackup();
      }
      
      state = WORKING;
   }

   /**
    * {@inheritDoc}
    */
   public void stopBackup()
   {
      if (state != INITIALIZED || this.getState() != FINISHED)
      {
         for (BackupChain wsChain : workspaceBackups) 
         {
            wsChain.stopBackup();
         }
         
         repositoryChainLog.endLog();
      }

      state = this.getState();
   }

   /**
    * {@inheritDoc}
    */
   public String getBackupId()
   {
      return repositoryBackupId;
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getStartedTime()
   {
      return startTime;
   }

   /**
    * {@inheritDoc}
    */
   public RepositoryBackupConfig getBackupConfig()
   {
      return config;
   }

}
