/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.quota;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.WorkspaceManagingListener;
import org.exoplatform.services.jcr.impl.proccess.WorkerThread;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: RepositoryQuotaManager.java 34360 2009-07-22 23:58:59Z tolusha $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "RepositoryQuotaManager"))
public class RepositoryQuotaManager implements Startable, WorkspaceManagingListener
{

   /**
    * Logger.
    */
   protected final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryQuotaManager");

   /**
    * All {@link WorkspaceQuotaManager} belonging to repository.
    */
   private Map<String, WorkspaceQuotaManager> wsQuotaManagers = new ConcurrentHashMap<String, WorkspaceQuotaManager>();

   /**
    * The repository name.
    */
   protected final String rName;

   /**
    * The quota manager.
    */
   protected final BaseQuotaManager globalQuotaManager;

   /**
    * {@link QuotaPersister}
    */
   protected final QuotaPersister quotaPersister;

   /**
    * {@link ManageableRepository} instance.
    */
   protected final RepositoryImpl repository;

   /**
    * Task is obligated to push all changes to coordinator where they will
    * be persisted.
    */
   protected final PushTask pushTask;

   /**
    * Timeout for task.
    */
   protected final long DEFAULT_TIMEOUT = 5000; // 5 sec

   /**
    * RepositoryQuotaManagerImpl constructor.
    */
   public RepositoryQuotaManager(RepositoryImpl repository, BaseQuotaManager quotaManager, RepositoryEntry rEntry)
   {
      this.rName = rEntry.getName();
      this.globalQuotaManager = quotaManager;
      this.quotaPersister = globalQuotaManager.quotaPersister;
      this.repository = repository;

      this.pushTask = new PushTask("PushQuotaChangesTask-" + rName, DEFAULT_TIMEOUT);
      this.pushTask.start();

      globalQuotaManager.registerRepositoryQuotaManager(rName, this);
      this.repository.addWorkspaceManagingListener(this);
   }

   /**
    * @see QuotaManager#getNodeDataSize(String, String, String)
    */
   public long getNodeDataSize(String workspaceName, String nodePath) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      return wqm.getNodeDataSize(nodePath);
   }

   /**
    * @see QuotaManager#getNodeQuota(String, String, String)
    */
   public long getNodeQuota(String workspaceName, String nodePath) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      return wqm.getNodeQuota(nodePath);
   }

   /**
    * @see QuotaManager#setNodeQuota(String, String, String, long, boolean)
    */
   public void setNodeQuota(String workspaceName, String nodePath, long quotaLimit, boolean asyncUpdate)
      throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      wqm.setNodeQuota(nodePath, quotaLimit, asyncUpdate);
   }

   /**
    * @see QuotaManager#setGroupOfNodesQuota(String, String, String, long, boolean)
    */
   public void setGroupOfNodesQuota(String workspaceName, String patternPath, long quotaLimit,
      boolean asyncUpdate) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      wqm.setGroupOfNodesQuota(patternPath, quotaLimit, asyncUpdate);
   }

   /**
    * @see QuotaManager#removeNodeQuota(String, String, String)
    */
   public void removeNodeQuota(String workspaceName, String nodePath) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      wqm.removeNodeQuota(nodePath);
   }

   /**
    * @see QuotaManager#removeGroupOfNodesQuota(String, String, String)
    */
   public void removeGroupOfNodesQuota(String workspaceName, String nodePath) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      wqm.removeGroupOfNodesQuota(nodePath);
   }

   /**
    * @see QuotaManager#getWorkspaceQuota(String, String)
    */
   public long getWorkspaceQuota(String workspaceName) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      return wqm.getWorkspaceQuota();
   }

   /**
    * @see QuotaManager#setWorkspaceQuota(String, String, long)
    */
   public void setWorkspaceQuota(String workspaceName, long quotaLimti) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      wqm.setWorkspaceQuota(quotaLimti);
   }

   /**
    * @see QuotaManager#removeWorkspaceQuota(String, String, long)
    */
   public void removeWorkspaceQuota(String workspaceName) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      wqm.removeWorkspaceQuota();
   }

   /**
    * @see QuotaManager#getWorkspaceDataSize(String, String)
    */
   public long getWorkspaceDataSize(String workspaceName) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      return wqm.getWorkspaceDataSize();
   }

   /**
    * @see QuotaManager#getWorkspaceIndexSize(String, String)
    */
   public long getWorkspaceIndexSize(String workspaceName) throws QuotaManagerException
   {
      WorkspaceQuotaManager wqm = getWorkspaceQuotaManager(workspaceName);
      return wqm.getWorkspaceIndexSize();
   }

   /**
    * @see QuotaManager#setRepositoryQuota(String, long)
    */
   @Managed
   @ManagedDescription("Sets repository quta limit")
   public void setRepositoryQuota(@ManagedName("quotaLimit") long quotaLimit) throws QuotaManagerException
   {
      quotaPersister.setRepositoryQuota(rName, quotaLimit);
   }

   /**
    * @see QuotaManager#removeRepositoryQuota(String)
    */
   @Managed
   @ManagedDescription("Removes repository quta limit")
   public void removeRepositoryQuota() throws QuotaManagerException
   {
      quotaPersister.removeRepositoryQuota(rName);
   }

   /**
    * @see QuotaManager#getRepositoryQuota(String)
    */
   @Managed
   @ManagedDescription("Returns repository quta limit")
   public long getRepositoryQuota() throws QuotaManagerException
   {
      return quotaPersister.getRepositoryQuota(rName);
   }

   /**
    * @see QuotaManager#getRepositoryDataSize(String)
    */
   @Managed
   @ManagedDescription("Returns repository data size")
   public long getRepositoryDataSize() throws QuotaManagerException
   {
      return quotaPersister.getRepositoryDataSize(rName);
   }

   /**
    * @see QuotaManager#getRepositoryIndexSize(String)
    */
   @Managed
   @ManagedDescription("Returns repository index size")
   public long getRepositoryIndexSize() throws QuotaManagerException
   {
      long size = 0;
      for (WorkspaceQuotaManager wQuotaManager : wsQuotaManagers.values())
      {
         size += wQuotaManager.getWorkspaceIndexSize();
      }

      return size;
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      wsQuotaManagers.clear();
      globalQuotaManager.unregisterRepositoryQuotaManager(rName);
      repository.removeWorkspaceManagingListener(this);

      pushTask.halt();
   }

   /**
    * Registers {@link WorkspaceQuotaManager} by name. To delegate workspace based operation
    * to appropriate level.
    */
   public void registerWorkspaceQuotaManager(String workspaceName, WorkspaceQuotaManager wQuotaManager)
   {
      wsQuotaManagers.put(workspaceName, wQuotaManager);
   }

   /**
    * Unregisters {@link WorkspaceQuotaManager} by name.
    */
   public void unregisterWorkspaceQuotaManager(String workspaceName)
   {
      wsQuotaManagers.remove(workspaceName);
   }

   /**
    * Returns repository data size by summing size of all workspaces.
    */
   public long getRepositoryDataSizeDirectly() throws QuotaManagerException
   {
      long size = 0;
      for (WorkspaceQuotaManager wQuotaManager : wsQuotaManagers.values())
      {
         size += wQuotaManager.getWorkspaceDataSizeDirectly();
      }

      return size;
   }

   /**
    * {@inheritDoc}
    */
   public void onWorkspaceRemove(String workspaceName)
   {
      long dataSize;

      try
      {
         dataSize = quotaPersister.getWorkspaceDataSize(rName, workspaceName);
      }
      catch (UnknownDataSizeException e)
      {
         return;
      }

      ChangesItem changesItem = new ChangesItem();
      changesItem.updateWorkspaceChangedSize(-dataSize);

      WorkspaceQuotaContext context =
         new WorkspaceQuotaContext(workspaceName, rName, null, null, null, null, quotaPersister, null, null);

      Runnable task = new ApplyPersistedChangesTask(context, changesItem);
      task.run();

      quotaPersister.setWorkspaceDataSize(rName, workspaceName, dataSize); // workaround
   }

   // ============================================> pushing changes to coordinator

   /**
    * Push changes to coordinator.
    */
   private class PushTask extends WorkerThread
   {

      /**
       * PushTask constructor.
       */
      public PushTask(String name, long timeout)
      {
         super(name, timeout);
      }

      /**
       * {@inheritDoc}
       */
      protected void callPeriodically() throws Exception
      {
         for (WorkspaceQuotaManager wqm : wsQuotaManagers.values())
         {
            wqm.pushAllChangesToCoordinator();
         }
      }
   }

   private WorkspaceQuotaManager getWorkspaceQuotaManager(String workspaceName) throws IllegalStateException
   {
      WorkspaceQuotaManager wqm = wsQuotaManagers.get(workspaceName);
      if (wqm == null)
      {
         throw new IllegalStateException("Workspace " + workspaceName + " is not registered in " + rName);
      }

      return wqm;
   }
}
