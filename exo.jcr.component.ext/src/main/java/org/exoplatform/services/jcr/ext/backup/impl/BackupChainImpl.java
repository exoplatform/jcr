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

import org.exoplatform.commons.utils.Tools;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupChain;
import org.exoplatform.services.jcr.ext.backup.BackupChainLog;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupJobListener;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SARL .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public class BackupChainImpl implements BackupChain
{

   private final BackupConfig config;

   private List<BackupJob> jobs;

   private AbstractFullBackupJob fullBackup;

   private AbstractIncrementalBackupJob incrementalBackup;

   private final BackupChainLog chainLog;

   private final String backupId;

   private int state;

   private PeriodConroller periodConroller;

   private Timer timer;

   private final Calendar timeStamp;

   private Set<BackupJobListener> listeners = new LinkedHashSet<BackupJobListener>();

   public BackupChainImpl(BackupConfig config, File logDirectory,
            RepositoryService repositoryService,
            String fullBackupType, String incrementalBackupType, String backupId, File rootDir, Calendar startTime)
            throws BackupOperationException,
      BackupConfigurationException
   {
      this.config = config;
      this.jobs = new ArrayList<BackupJob>();
      this.timeStamp = startTime;

      this.chainLog =
               new BackupChainLog(logDirectory, config, fullBackupType, incrementalBackupType, backupId,
                        repositoryService.getConfig(), rootDir);

      this.backupId = backupId;
      
      ManageableRepository repository = null;
      try
      {
         repository = repositoryService.getRepository(config.getRepository());
      }
      catch (RepositoryConfigurationException e)
      {
         throw new BackupOperationException("Can not get repository \"" + config.getRepository() + "\"", e);
      }
      catch (RepositoryException e)
      {
         throw new BackupOperationException("Can not get repository \"" + config.getRepository() + "\"", e);
      }

      try
      {
         this.fullBackup = (AbstractFullBackupJob)Tools.forName(fullBackupType, this).newInstance();
      }
      catch (Exception e)
      {
         throw new BackupConfigurationException("FullBackupType error, " + e, e);
      }
      fullBackup.init(repository, config.getWorkspace(), config, timeStamp);

      if (config.getBackupType() == BackupManager.FULL_AND_INCREMENTAL)
      {
         try
         {
            this.incrementalBackup = (AbstractIncrementalBackupJob)Tools.forName(incrementalBackupType, this).newInstance();
         }
         catch (Exception e)
         {
            throw new BackupConfigurationException("IncrementalBackupType error, " + e, e);
         }
         incrementalBackup.init(repository, config.getWorkspace(), config, timeStamp);

         periodConroller = new PeriodConroller(config.getIncrementalJobPeriod() * 1000); // sec --> ms
      }
      this.state = INITIALIZED;
      this.timer =
         new Timer("BackupChain_" + getBackupConfig().getRepository() + "@" + getBackupConfig().getWorkspace()
            + "_PeriodTimer_" + new SimpleDateFormat("yyyyMMdd.HHmmss.SSS").format(new Date()), true);
   }

   /**
    * Add all listeners to a given job. Used in startBackup() which itself is synchronized.
    * 
    * @param job
    */
   private void addJobListeners(BackupJob job)
   {
      for (BackupJobListener jl : listeners)
         job.addListener(jl);
   }

   /**
    * Remove all listeners from a given job. Used in stoptBackup() which itself is synchronized.
    * 
    * @param job
    */
   private void removeJobListeners(BackupJob job)
   {
      for (BackupJobListener jl : listeners)
         job.removeListener(jl);
   }

   public void addListener(BackupJobListener listener)
   {
      if (listener != null)
      {
         synchronized (jobs)
         {
            for (BackupJob job : jobs)
               job.addListener(listener);
         }
         synchronized (listeners)
         {
            listeners.add(listener);
         }
      }
   }

   public void removeListener(BackupJobListener listener)
   {
      if (listener != null)
      {
         try
         {
            synchronized (jobs)
            {
               for (BackupJob job : jobs)
                  job.removeListener(listener);
            }
         }
         finally
         {
            // remove anyway
            synchronized (listeners)
            {
               listeners.remove(listener);
            }
         }
      }
   }

   public List<BackupJob> getBackupJobs()
   {
      return jobs;
   }

   public final synchronized void startBackup()
   {

      addJobListeners(fullBackup);

      Thread fexecutor =
         new Thread(fullBackup, config.getRepository() + "@" + config.getWorkspace() + "-" + fullBackup.getId());
      fexecutor.start();
      state |= FULL_WORKING;
      chainLog.addJobEntry(fullBackup);
      jobs.add(fullBackup);

      if (incrementalBackup != null)
      {
         addJobListeners(incrementalBackup);

         Thread iexecutor =
            new Thread(incrementalBackup, config.getRepository() + "@" + config.getWorkspace() + "-"
               + incrementalBackup.getId());
         iexecutor.start();
         state |= INCREMENTAL_WORKING;
         chainLog.addJobEntry(incrementalBackup);
         jobs.add(incrementalBackup);

         if (config.getIncrementalJobPeriod() > 0)
            periodConroller.start();
      }
   }

   public final synchronized void stopBackup()
   {
      if (!chainLog.isFinilized())
      {
         fullBackup.stop();
         chainLog.addJobEntry(fullBackup);
         removeJobListeners(fullBackup);

         if (incrementalBackup != null)
         {
            if (config.getIncrementalJobPeriod() > 0)
               periodConroller.stop();

            incrementalBackup.stop();
            chainLog.addJobEntry(incrementalBackup);
            removeJobListeners(incrementalBackup);
         }

         this.state |= FINISHED;
         chainLog.endLog();
      }
   }

   public void restartIncrementalBackup()
   {
      incrementalBackup.suspend();
      incrementalBackup.resume();
      chainLog.addJobEntry(incrementalBackup);
   }

   public BackupConfig getBackupConfig()
   {
      return config;
   }

   public int getFullBackupState()
   {
      return fullBackup.getState();
   }
   
   public int getIncrementalBackupState()
   {
      if (incrementalBackup == null)
         throw new RuntimeException("The incremental bacup was not configured. Only full backup.");
         
      return incrementalBackup.getState();
   }

   public String getLogFilePath()
   {
      return chainLog.getLogFilePath();
   }

   private class PeriodConroller extends TimerTask
   {
      protected Log log = ExoLogger.getLogger("exo.jcr.component.ext.PeriodConroller");

      protected Long period;

      private boolean isFirst = false;

      public PeriodConroller(long period)
      {
         this.period = period;
      }

      @Override
      public void run()
      {
         if (incrementalBackup.getState() == BackupJob.FINISHED)
         {
            this.stop();
         }
         else if (!isFirst)
         {
            isFirst = true;
         }
         else
         {
            Thread starter = new Thread("IncrementalBackup Starter")
            {
               @Override
               public void run()
               {
                  restartIncrementalBackup();
                  if (log.isDebugEnabled())
                     log.debug("Restart incrementalBackup :" + new Date().toString());
               }
            };
            starter.start();
         }
      }

      public void start()
      {
         timer.schedule(this, new Date(), period);
      }

      public boolean stop()
      {
         log.info("Stop period controller");
         return this.cancel();
      }
   }

   public int getState()
   {
      return state;
   }

   public boolean isFinished()
   {
      return ((state & BackupChain.FINISHED) == BackupChain.FINISHED ? true : false);
   }

   public String getBackupId()
   {
      return backupId;
   }

   public Calendar getStartedTime()
   {
      return timeStamp;
   }
}
