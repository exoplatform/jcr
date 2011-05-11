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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestLinkedWorkspaceStorageCache.java 1518 2010-01-20 23:33:30Z sergiykarpenko $
 */
public class TestLinkedWorkspaceStorageCache extends WorkspaceStorageCacheBaseCase
{

   @Override
   public WorkspaceStorageCache getCacheImpl() throws Exception
   {
      /*String name, boolean enabled, int maxSize, long liveTimeSec,
      long cleanerPeriodMillis, long statisticPeriodMillis, boolean deepDelete, boolean cleanStatistics,
      int blockingUsers, boolean showStatistic*/
      return new LinkedWorkspaceStorageCacheImpl("test_WorkspaceStorageCacheBaseCase", true, 
         100 * 1024, 120, 5 * 60000, 30000, false, true, 0, false);
      //return new LinkedWorkspaceStorageCacheImpl((WorkspaceEntry)session.getContainer().getComponentInstanceOfType(
      //   WorkspaceEntry.class));
   }   
}
