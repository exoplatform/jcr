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
    * @param semaphore
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
    * @see org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitor#addIndexUpdateMonitorListener(org.exoplatform.services.jcr.impl.core.query.lucene.IndexUpdateMonitorListener)
    */
   public void addIndexUpdateMonitorListener(IndexUpdateMonitorListener listener)
   {
      listeners.add(listener);
   }

}
