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

import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.annotations.ManagedName;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.ExtendedMandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.backup.DataRestore;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;
import org.exoplatform.services.jcr.impl.backup.rdbms.DataRestoreContext;
import org.exoplatform.services.jcr.impl.core.JCRPath;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.picocontainer.Startable;

import java.io.File;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;

/**
 * {@link WorkspaceQuotaManager} listens all changes performed in
 * {@link WorkspacePersistentDataManager} and checks if some of them might
 * exceed defined quota limit. If so, exception might be thrown or warning printing.
 * Then all changes are devided into two part. First one should be applied synchronously.
 * Another ones are put into changes log and to be passed to coordinator by timer.
 * It is managed by {@link RepositoryQuotaManager}
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: WorkspaceQuotaManager.java 34360 2009-07-22 23:58:59Z tolusha $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "WorkspaceQuotaManager"))
public class WorkspaceQuotaManager implements Startable, Backupable, Suspendable
{

   /**
    * Workspace name.
    */
   protected final String wsName;

   /**
    * Repository name.
    */
   protected final String rName;

   /**
    * Unique name.
    */
   protected final String uniqueName;

   /**
    * Workspace container.
    */
   protected final WorkspaceContainerFacade wsContainer;

   /**
    * {@link WorkspacePersistentDataManager}.
    */
   protected final WorkspacePersistentDataManager dataManager;

   /**
    * Quota manager of repository level.
    */
   protected final RepositoryQuotaManager repositoryQuotaManager;

   /**
    * Executor service per workspace to be able to suspend all tasks devoted current workspace..
    */
   protected final QuotaExecutorService executor;

   /**
    * {@link QuotaPersister}
    */
   protected final QuotaPersister quotaPersister;

   /**
    * {@link RPCService}
    */
   protected final RPCService rpcService;

   /**
    * {@link ExtendedMandatoryItemsPersistenceListener} implementations.
    */
   protected final ChangesListener changesListener;

   /**
    * Indicates if component suspended or not.
    */
   protected final AtomicBoolean isSuspended = new AtomicBoolean();

   /**
    * Context;
    */
   protected final WorkspaceQuotaContext context;

   /**
    * Helps calculate node data size.
    */
   protected final CalculateNodeDataSizeTool calculateNodeDataSizeTool;

   /**
    * BaseWorkspaceQuotaManager constructor.
    */
   public WorkspaceQuotaManager(RepositoryImpl repository, RepositoryQuotaManager rQuotaManager,
      RepositoryEntry rEntry, WorkspaceEntry wsEntry, WorkspacePersistentDataManager dataManager)
   {
      this.rName = rEntry.getName();
      this.wsName = wsEntry.getName();
      this.uniqueName = "/" + rName + "/" + wsName;
      this.wsContainer = repository.getWorkspaceContainer(wsName);
      this.dataManager = dataManager;
      this.repositoryQuotaManager = rQuotaManager;
      this.quotaPersister = rQuotaManager.globalQuotaManager.quotaPersister;
      this.rpcService = rQuotaManager.globalQuotaManager.rpcService;
      this.executor = new QuotaExecutorService(uniqueName);

      repositoryQuotaManager.registerWorkspaceQuotaManager(wsName, this);

      this.context =
         new WorkspaceQuotaContext(wsName, rName, uniqueName, dataManager, repository.getLocationFactory(), executor,
            quotaPersister, rpcService, rQuotaManager.globalQuotaManager.exceededQuotaBehavior);

      changesListener = new ChangesListener(this);
      calculateNodeDataSizeTool = new CalculateNodeDataSizeTool(context);
      dataManager.addItemPersistenceListener(changesListener);
   }

   /**
    * @see QuotaManager#getNodeDataSize(String, String, String)
    */
   @Managed
   @ManagedDescription("Returns a node data size")
   public long getNodeDataSize(@ManagedName("nodePath") String nodePath) throws QuotaManagerException
   {
      if (nodePath.equals(JCRPath.ROOT_PATH))
      {
         return getWorkspaceDataSize();
      }

      return quotaPersister.getNodeDataSize(rName, wsName, nodePath);
   }

   /**
    * @see QuotaManager#getNodeQuota(String, String, String)
    */
   @Managed
   @ManagedDescription("Returns a node quota limit")
   public long getNodeQuota(@ManagedName("nodePath") String nodePath) throws QuotaManagerException
   {
      if (nodePath.equals(JCRPath.ROOT_PATH))
      {
         return getWorkspaceQuota();
      }

      return quotaPersister.getNodeQuotaOrGroupOfNodesQuota(rName, wsName, nodePath);
   }

   /**
    * @see QuotaManager#setNodeQuota(String, String, String, long, boolean)
    */
   @Managed
   @ManagedDescription("Sets a node quota limit")
   public void setNodeQuota(@ManagedName("nodePath") String nodePath, @ManagedName("quotaLimit") long quotaLimit,
      @ManagedName("asyncUpdate") boolean asyncUpdate) throws QuotaManagerException
   {
      if (nodePath.equals(JCRPath.ROOT_PATH))
      {
         setWorkspaceQuota(quotaLimit);
      }
      else
      {
         quotaPersister.setNodeQuota(rName, wsName, nodePath, quotaLimit, asyncUpdate);
      }
   }

   /**
    * @see QuotaManager#setGroupOfNodesQuota(String, String, String, long, boolean)
    */
   @Managed
   @ManagedDescription("Sets a quota limit for a bunch of nodes")
   public void setGroupOfNodesQuota(@ManagedName("patternPath") String patternPath,
      @ManagedName("quotaLimit") long quotaLimit, @ManagedName("asyncUpdate") boolean asyncUpdate)
      throws QuotaManagerException
   {
      if (patternPath.equals(JCRPath.ROOT_PATH))
      {
         setWorkspaceQuota(quotaLimit);
      }
      else
      {
         quotaPersister.setGroupOfNodesQuota(rName, wsName, patternPath, quotaLimit, asyncUpdate);
      }
   }

   /**
    * @see QuotaManager#removeNodeQuota(String, String, String)
    */
   @Managed
   @ManagedDescription("Removes a quota limit for a node")
   public void removeNodeQuota(@ManagedName("nodePath") String nodePath) throws QuotaManagerException
   {
      if (nodePath.equals(JCRPath.ROOT_PATH))
      {
         removeWorkspaceQuota();
      }
      else
      {
         quotaPersister.removeNodeQuotaAndDataSize(rName, wsName, nodePath);
      }
   }

   /**
    * @see QuotaManager#removeGroupOfNodesQuota(String, String, String)
    */
   @Managed
   @ManagedDescription("Removes a quota limit for a bunch of nodes")
   public void removeGroupOfNodesQuota(@ManagedName("patternPath") String patternPath) throws QuotaManagerException
   {
      if (patternPath.equals(JCRPath.ROOT_PATH))
      {
         removeWorkspaceQuota();
      }
      else
      {
         quotaPersister.removeGroupOfNodesAndDataSize(rName, wsName, patternPath);
      }
   }

   /**
    * @see QuotaManager#setWorkspaceQuota(String, String, long)
    */
   @Managed
   @ManagedDescription("Sets workspace quota limit")
   public void setWorkspaceQuota(long quotaLimit) throws QuotaManagerException
   {
      quotaPersister.setWorkspaceQuota(rName, wsName, quotaLimit);
   }

   /**
    * @see QuotaManager#removeWorkspaceQuota(String, String, long)
    */
   @Managed
   @ManagedDescription("Removes workspace quota limit")
   public void removeWorkspaceQuota() throws QuotaManagerException
   {
      quotaPersister.removeWorkspaceQuota(rName, wsName);
   }

   /**
    * @see QuotaManager#getWorkspaceQuota(String, String)
    */
   @Managed
   @ManagedDescription("Returns workspace quota limit")
   public long getWorkspaceQuota() throws QuotaManagerException
   {
      return quotaPersister.getWorkspaceQuota(rName, wsName);
   }

   /**
    * @see QuotaManager#getWorkspaceDataSize(String, String)
    */
   @Managed
   @ManagedDescription("Returns a size of the Workspace")
   public long getWorkspaceDataSize() throws QuotaManagerException
   {
      return quotaPersister.getWorkspaceDataSize(rName, wsName);
   }

   /**
    * @see QuotaManager#getWorkspaceIndexSize(String, String)
    */
   @Managed
   @ManagedDescription("Returns a size of the index")
   public long getWorkspaceIndexSize() throws QuotaManagerException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Long>()
         {
            public Long run() throws Exception
            {
               Method getIndexDirMethod = SearchManager.class.getDeclaredMethod("getIndexDirectory");
               getIndexDirMethod.setAccessible(true);

               // get all instances, since for system workspace we have 2 SearchManager
               List<SearchManager> searchers = wsContainer.getComponentInstancesOfType(SearchManager.class);

               long size = 0;
               for (SearchManager searchManager : searchers)
               {
                  File indexDir = (File)getIndexDirMethod.invoke(searchManager);
                  size += DirectoryHelper.getSize(indexDir);
               }

               return size;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw new QuotaManagerException(e.getMessage(), e);
      }
   }

   /**
    * Calculates node data size by asking directly respective {@link WorkspacePersistentDataManager}.
    */
   public long getNodeDataSizeDirectly(final String nodePath) throws QuotaManagerException
   {
      try
      {
         return SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Long>()
         {
            public Long run() throws Exception
            {
               if (nodePath.equals(JCRPath.ROOT_PATH))
               {
                  return getWorkspaceDataSizeDirectly();
               }

               return calculateNodeDataSizeTool.getNodeDataSizeDirectly(nodePath);
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         throw new QuotaManagerException(e.getMessage(), e);
      }
   }

   /**
    * Calculate workspace data size by asking directly respective {@link WorkspacePersistentDataManager}.
    */
   public long getWorkspaceDataSizeDirectly() throws QuotaManagerException
   {
      try
      {
         return dataManager.getWorkspaceDataSize();
      }
      catch (RepositoryException e)
      {
         throw new QuotaManagerException(e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      boolean isCoordinator;
      try
      {
         isCoordinator = rpcService.isCoordinator();
      }
      catch (RPCException e)
      {
         throw new IllegalStateException(e.getMessage(), e);
      }

      if (isCoordinator)
      {
         try
         {
            quotaPersister.getWorkspaceDataSize(rName, wsName);
         }
         catch (UnknownDataSizeException e)
         {
            calculateWorkspaceDataSize();
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      executor.shutdownNow();

      repositoryQuotaManager.unregisterWorkspaceQuotaManager(wsName);

      changesListener.destroy();
      dataManager.removeItemPersistenceListener(changesListener);
   }

   /**
    * Getter for {@link #context}.
    */
   protected WorkspaceQuotaContext getContext()
   {
      return context;
   }

   // ======================================> Backup & Suspend

   /**
    * {@inheritDoc}
    */
   public void backup(File storageDir) throws BackupException
   {
      WorkspaceQuotaRestore wqr = new WorkspaceQuotaRestore(this, storageDir);
      wqr.backup();
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      File storageDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

      WorkspaceQuotaRestore wqr = new WorkspaceQuotaRestore(this, storageDir);
      wqr.clean();
   }

   /**
    * {@inheritDoc}
    */
   public DataRestore getDataRestorer(DataRestoreContext context) throws BackupException
   {
      return new WorkspaceQuotaRestore(this, context);
   }

   /**
    * {@inheritDoc}
    */
   public void suspend() throws SuspendException
   {
      executor.suspend();
      isSuspended.set(true);
   }

   /**
    * {@inheritDoc}
    */
   public void resume() throws ResumeException
   {
      executor.resume();
      isSuspended.set(false);
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSuspended()
   {
      return isSuspended.get();
   }

   /**
    * {@inheritDoc}
    */
   public int getPriority()
   {
      return Suspendable.PRIORITY_LOW;
   }

   /**
    *
    * @throws QuotaManagerException
    */
   protected void pushAllChangesToCoordinator() throws QuotaManagerException
   {
      try
      {
         changesListener.pushAllChangesToCoordinator();
      }
      catch (SecurityException e)
      {
         throw new QuotaManagerException("Can't push changes to coordinator", e);
      }
      catch (RPCException e)
      {
         throw new QuotaManagerException("Can't push changes to coordinator", e);
      }
   }

   /**
    * Calculates and accumulates workspace data size.
    */
   private void calculateWorkspaceDataSize()
   {
      long dataSize;
      try
      {
         dataSize = getWorkspaceDataSizeDirectly();
      }
      catch (QuotaManagerException e1)
      {
         throw new IllegalStateException("Can't calculate workspace data size", e1);
      }

      ChangesItem changesItem = new ChangesItem();
      changesItem.updateWorkspaceChangedSize(dataSize);

      Runnable task = new ApplyPersistedChangesTask(context, changesItem);
      task.run();
   }
}
