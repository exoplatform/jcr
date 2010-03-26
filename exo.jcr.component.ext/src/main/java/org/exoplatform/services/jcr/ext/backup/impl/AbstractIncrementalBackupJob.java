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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SARL .<br/>
 * 
 * @author Gennady Azarenkov
 * @version $Id: $
 */

public abstract class AbstractIncrementalBackupJob extends AbstractBackupJob implements ItemsPersistenceListener
{

   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.IncrementalBackupJob");

   protected final List<ItemStateChangesLog> suspendBuffer;

   public AbstractIncrementalBackupJob()
   {
      this.suspendBuffer = new ArrayList<ItemStateChangesLog>();
      this.id = 1;

      notifyListeners();
   }

   public final int getType()
   {
      return INCREMENTAL;
   }

   /**
    * @see org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener#onSaveItems(org.exoplatform.services.jcr.dataflow.ItemStateChangesLog)
    */
   public void onSaveItems(ItemStateChangesLog chlog)
   {
      if (state == FINISHED)
         return;
      else if (state == WAITING)
         suspendBuffer.add(chlog);
      else if (state == WORKING)
         try
         {
            save(chlog);
         }
         catch (IOException e)
         {
            log.error("Incremental backup: Can't save log ", e);
            notifyError("Incremental backup: Can't save log ", e);
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
         url = createStorage();
         for (ItemStateChangesLog log : suspendBuffer)
         {
            save(log);
         }
         suspendBuffer.clear();
         state = WORKING;

         if (config.getIncrementalJobNumber() != 0 && id == config.getIncrementalJobNumber() + 1)
            state = FINISHED;

         notifyListeners();
      }
      catch (FileNotFoundException e)
      {
         log.error("Incremental backup: resume failed ", e);
         notifyError("Incremental backup: resume failed ", e);
      }
      catch (IOException e)
      {
         log.error("Incremental backup: resume failed +", e);
         notifyError("Incremental backup: resume failed ", e);
      }

      return url;
   }

   /**
    * Implementation specific saving
    * 
    * @param log
    */
   protected abstract void save(ItemStateChangesLog log) throws IOException;
}
