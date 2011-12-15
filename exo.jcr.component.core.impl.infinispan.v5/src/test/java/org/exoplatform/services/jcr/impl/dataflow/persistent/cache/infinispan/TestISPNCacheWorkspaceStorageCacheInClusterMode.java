/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.dataflow.persistent.cache.infinispan;

import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.dataflow.persistent.TestWorkspaceStorageCacheInClusterMode;
import org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.TesterISPNCacheWorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.ISPNCacheWorkspaceStorageCache;
import org.exoplatform.services.jcr.infinispan.ISPNCacheFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestISPNCacheWorkspaceStorageCacheInClusterMode extends TestWorkspaceStorageCacheInClusterMode<ISPNCacheWorkspaceStorageCache>
{

   @SuppressWarnings({"rawtypes", "unchecked"})
   public ISPNCacheWorkspaceStorageCache getCacheImpl() throws Exception
   {
      // Clear the Cache Factory to avoid getting several times the same cache
      Field singletonField = ISPNCacheFactory.class.getDeclaredField("CACHE_MANAGERS");
      singletonField.setAccessible(true);
      Map map = (Map)singletonField.get(null);
      Map backupMap = new HashMap(map);
      map.clear();
      ArrayList<SimpleParameterEntry> list = new ArrayList<SimpleParameterEntry>();
      list.add(new SimpleParameterEntry(ISPNCacheFactory.INFINISPAN_CONFIG,
         "jar:/conf/standalone/cluster/test-infinispan-config.xml"));
      list.add(new SimpleParameterEntry("infinispan-cluster-name", "TestISPNCacheWorkspaceStorageCacheInClusterMode"));
      list.add(new SimpleParameterEntry("jgroups-configuration", "classpath:/flush-udp.xml"));
      
      CacheEntry entry = new CacheEntry(list);
      entry.setEnabled(true);
      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setCache(entry);
      workspaceEntry.setUniqueName("MyWorkspace");
      try
      {
         return new ISPNCacheWorkspaceStorageCache(workspaceEntry, new ConfigurationManagerImpl());
      }
      finally
      {
         map.clear();
         map.putAll(backupMap);
      }
   }
   
   protected void finalize(ISPNCacheWorkspaceStorageCache cache)
   {
      TesterISPNCacheWorkspaceStorageCache.stop(cache);
   }   
}