/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.query.lucene;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class DefaultIndexUpdateMonitor implements IndexUpdateMonitor
{
   private final AtomicBoolean updateInProgress;

   /**
    * The list of all the listeners
    */
   private final List<IndexUpdateMonitorListener> listeners;

   /**
    *
    */
   public DefaultIndexUpdateMonitor()
   {
      super();
      this.updateInProgress = new AtomicBoolean(false);
      this.listeners = new CopyOnWriteArrayList<IndexUpdateMonitorListener>();
   }

   /**
    * 
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#getUpdateInProgress()
    */
   public boolean getUpdateInProgress()
   {
      return updateInProgress.get();
   }

   /**
    * 
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#setUpdateInProgress(boolean, boolean)
    */
   public void setUpdateInProgress(boolean updateInProgress, boolean persitentUpdate)
   {
      this.updateInProgress.set(updateInProgress);
      for (IndexUpdateMonitorListener listener : listeners)
      {
         listener.onUpdateInProgressChange(updateInProgress);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addIndexUpdateMonitorListener(IndexUpdateMonitorListener listener)
   {
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void removeIndexUpdateMonitorListener(IndexUpdateMonitorListener listener)
   {
      listeners.remove(listener);
   }

}
