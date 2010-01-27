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

import org.exoplatform.container.configuration.ConfigurationManagerImpl;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.dataflow.persistent.jbosscache.JBossCacheWorkspaceStorageCache;
import org.exoplatform.services.transaction.TransactionService;

import java.util.ArrayList;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id$
 */
public class TestJBossCacheWorkspaceStorageCache extends WorkspaceStorageCacheBaseCase
{

   @Override
   public WorkspaceStorageCache getCacheImpl() throws Exception
   {
      TransactionService transactionService =
         (TransactionService)container.getComponentInstanceOfType(TransactionService.class);

      ArrayList<SimpleParameterEntry> list = new ArrayList<SimpleParameterEntry>();
      list.add(new SimpleParameterEntry(JBossCacheWorkspaceStorageCache.JBOSSCACHE_CONFIG,
         "jar:/conf/standalone/test-jbosscache-config.xml"));

      CacheEntry entry = new CacheEntry(list);
      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setCache(entry);
      return new JBossCacheWorkspaceStorageCache(workspaceEntry,
         transactionService == null ? null : transactionService, new ConfigurationManagerImpl());
   }
}
