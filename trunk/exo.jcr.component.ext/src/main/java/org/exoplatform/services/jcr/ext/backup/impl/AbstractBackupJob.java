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

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.backup.BackupConfig;
import org.exoplatform.services.jcr.ext.backup.BackupJob;
import org.exoplatform.services.jcr.ext.backup.BackupJobListener;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 05.02.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: AbstrackBackupJob.java 760 2008-02-07 15:08:07Z pnedonosko $
 */
public abstract class AbstractBackupJob implements BackupJob
{

   protected volatile int state;

   protected int id;

   protected BackupConfig config;

   protected ManageableRepository repository;

   protected String workspaceName;

   protected Calendar timeStamp;

   protected URL url;

   private Set<BackupJobListener> listeners = new LinkedHashSet<BackupJobListener>();

   class NotifyThread extends Thread
   {

      protected final BackupJobListener[] listeners;

      protected final BackupJob job;

      NotifyThread(BackupJobListener[] listeners, BackupJob job)
      {
         super("NotifyThread");
         super.setDaemon(true); // The Java Virtual Machine exits when the only threads running are all
         // daemon threads.
         this.listeners = listeners;
         this.job = job;
      }

      @Override
      public void run()
      {
         for (BackupJobListener l : listeners)
         {
            l.onStateChanged(job);
         }
      }
   }

   class ErrorNotifyThread extends NotifyThread
   {

      private final Throwable error;

      private final String message;

      ErrorNotifyThread(BackupJobListener[] listeners, BackupJob job, String message, Throwable error)
      {
         super(listeners, job);
         setName("ErrorNotifyThread");
         this.error = error;
         this.message = message;
      }

      @Override
      public void run()
      {
         for (BackupJobListener l : listeners)
         {
            l.onError(job, message, error);
         }
      }
   }

   public AbstractBackupJob()
   {
      this.state = STARTING;
   }

   /**
    * This method is called by run() and resume() Backup implementation knows how to create new
    * storage
    * 
    * @return URL of new storage
    */
   protected abstract URL createStorage() throws FileNotFoundException, IOException;

   /**
    * @see org.exoplatform.services.jcr.ext.backup.BackupJob#getStorageURL()
    */
   public final URL getStorageURL()
   {
      return url;
   }

   public final int getState()
   {
      return state;
   }

   public final int getId()
   {
      return id;
   }

   public void addListener(BackupJobListener listener)
   {
      if (listener != null)
         synchronized (listeners)
         {
            listeners.add(listener);
         }
   }

   public void removeListener(BackupJobListener listener)
   {
      if (listener != null)
         synchronized (listeners)
         {
            listeners.remove(listener);
         }
   }

   /**
    * Notify all listeners about the job state changed
    */
   protected void notifyListeners()
   {
      synchronized (listeners)
      {
         Thread notifier = new NotifyThread(listeners.toArray(new BackupJobListener[listeners.size()]), this);
         notifier.start();
      }
   }

   /**
    * Notify all listeners about an error
    * 
    * @param error
    *          - Throwable instance
    */
   protected void notifyError(String message, Throwable error)
   {
      synchronized (listeners)
      {
         Thread notifier =
            new ErrorNotifyThread(listeners.toArray(new BackupJobListener[listeners.size()]), this, message, error);
         notifier.start();
      }
   }

}
