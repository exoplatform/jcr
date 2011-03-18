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

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by The eXo Platform SARL .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public abstract class AbstractIncrementalBackupJob extends AbstractBackupJob implements ItemsPersistenceListener
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.IncrementalBackupJob");

   /**
    * Should be synchronized collection.
    */
   protected final List<ItemStateChangesLog> suspendBuffer;

   /**
    * The amount of current working threads.
    */
   protected AtomicInteger workingThreads = new AtomicInteger();

   /**
    * Allows to make all threads waiting. 
    */
   protected CountDownLatch latcher = null;

   public AbstractIncrementalBackupJob()
   {
      this.suspendBuffer = Collections.synchronizedList(new ArrayList<ItemStateChangesLog>());
      this.id = 1;

      notifyListeners();
   }

   public final int getType()
   {
      return INCREMENTAL;
   }

   /**
    * {@inheritDoc}
    */
   public void onSaveItems(ItemStateChangesLog chlog)
   {
      if (state == FINISHED)
      {
         return;
      }
      else if (state == WAITING)
      {
         suspendBuffer.add(chlog);
      }
      else if (state == WORKING)
      {
         try
         {
            // wait while suspended buffer is not clear
            if (latcher != null && latcher.getCount() != 0)
            {
               latcher.await();
            }

            workingThreads.incrementAndGet();

            save(chlog);

            workingThreads.decrementAndGet();
         }
         catch (IOException e)
         {
            log.error("Incremental backup: Can't save log ", e);
            notifyError("Incremental backup: Can't save log ", e);
         }
         catch (InterruptedException e)
         {
            log.error("Incremental backup: Can't save log ", e);
            notifyError("Incremental backup: Can't save log ", e);
         }
      }
   }

   /**
    * @see java.lang.Runnable#run()
    */
   public final void run()
   {
      // TODO [PN] listener was added but never will be removed
      repository.addItemPersistenceListener(workspaceName, this);
      state = WORKING;

      notifyListeners();
   }

   public final void suspend()
   {
      state = WAITING;
      id++;

      notifyListeners();
   }

   public final URL resume()
   {
      try
      {
         // waiting until all current working threads finished
         while (workingThreads.get() != 0)
         {
            try
            {
               Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
               throw new SuspendException(e);
            }
         }

         // nobody write into storage (all threads add changes logs to suspendBuffer), so create new one
         url = createStorage();

         //  make all threads wait on save, that allow flush suspendBuffer
         latcher = new CountDownLatch(1);
         state = WORKING;

         for (ItemStateChangesLog log : suspendBuffer)
         {
            save(log);
         }

         if (config.getIncrementalJobNumber() != 0 && id == config.getIncrementalJobNumber() + 1)
         {
            state = FINISHED;
         }
      }
      catch (Throwable e)
      {
         log.error("Incremental backup: resume failed ", e);
         notifyError("Incremental backup: resume failed ", e);
      }
      finally
      {
         // all threads now continue their work
         suspendBuffer.clear();
         latcher.countDown();
      }

      notifyListeners();

      return url;
   }

   /**
    * Implementation specific saving
    * 
    * @param log
    */
   protected abstract void save(ItemStateChangesLog log) throws IOException;
}
