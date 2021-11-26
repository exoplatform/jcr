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

package org.exoplatform.services.jcr.impl.dataflow.persistent.cache;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspaceStorageCacheBaseCase;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
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
