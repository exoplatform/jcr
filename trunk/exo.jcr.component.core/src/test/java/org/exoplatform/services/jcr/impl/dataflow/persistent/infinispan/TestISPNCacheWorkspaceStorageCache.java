/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan;

import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.ISPNCacheWorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.TestJBossCacheWorkspaceStorageCache;

import java.util.ArrayList;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestISPNCacheWorkspaceStorageCache.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestISPNCacheWorkspaceStorageCache extends TestJBossCacheWorkspaceStorageCache
{
   @Override
   public WorkspaceStorageCache getCacheImpl() throws Exception
   {
      ArrayList<SimpleParameterEntry> list = new ArrayList<SimpleParameterEntry>();
      list.add(new SimpleParameterEntry("infinispan-configuration", "jar:/conf/standalone/test-infinispan-config.xml"));

      CacheEntry entry = new CacheEntry(list);
      entry.setEnabled(true);
      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setUniqueName("WS_UUID");
      workspaceEntry.setCache(entry);
      return new ISPNCacheWorkspaceStorageCache(workspaceEntry, new ConfigurationManagerImpl());
   }
}