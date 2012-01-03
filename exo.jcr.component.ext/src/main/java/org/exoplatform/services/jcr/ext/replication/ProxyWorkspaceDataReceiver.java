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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.lock.WorkspaceLockManager;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ProxyWorkspaceDataReceiver.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class ProxyWorkspaceDataReceiver extends AbstractWorkspaceDataReceiver
{

   /**
    * ProxyWorkspaceDataReceiver constructor.
    * 
    * @param dataManager
    *          the CacheableWorkspaceDataManager
    * @param lockManager
    *          the LockManagerImpl
    * @throws RepositoryConfigurationException
    *           will be generated RepositoryConfigurationException
    */
   public ProxyWorkspaceDataReceiver(CacheableWorkspaceDataManager dataManager, WorkspaceLockManager lockManager)
      throws RepositoryConfigurationException
   {
      this(dataManager, null, lockManager);
   }

   /**
    * ProxyWorkspaceDataReceiver constructor.
    * 
    * @param dataManager
    *          the CacheableWorkspaceDataManager
    * @param searchManager
    *          the SearchManager
    * @throws RepositoryConfigurationException
    *           will be generated RepositoryConfigurationException
    */
   public ProxyWorkspaceDataReceiver(CacheableWorkspaceDataManager dataManager, SearchManager searchManager)
      throws RepositoryConfigurationException
   {
      this(dataManager, searchManager, null);
   }

   /**
    * ProxyWorkspaceDataReceiver constructor.
    * 
    * @param dataManager
    *          the CacheableWorkspaceDataManager
    * @throws RepositoryConfigurationException
    *           will be generated RepositoryConfigurationException
    */
   public ProxyWorkspaceDataReceiver(CacheableWorkspaceDataManager dataManager) throws RepositoryConfigurationException
   {
      this(dataManager, null, null);
   }

   /**
    * ProxyWorkspaceDataReceiver constructor.
    * 
    * @param dataManager
    *          the CacheableWorkspaceDataManager
    * @param searchManager
    *          the SearchManager
    * @param lockManager
    *          the LockManagerImpl
    * @throws RepositoryConfigurationException
    *           will be generated the RepositoryConfigurationException
    */
   public ProxyWorkspaceDataReceiver(CacheableWorkspaceDataManager dataManager, SearchManager searchManager,
      WorkspaceLockManager lockManager) throws RepositoryConfigurationException
   {
      dataKeeper = new WorkspaceDataManagerProxy(dataManager, searchManager, lockManager);
   }
}
