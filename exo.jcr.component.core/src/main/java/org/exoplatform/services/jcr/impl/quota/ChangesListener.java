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

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ExtendedMandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.quota.BaseQuotaManager.ExceededQuotaBehavior;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.jcr.RepositoryException;

/**
 * {@link ExtendedMandatoryItemsPersistenceListener} implementation.
 *
 * Is TX aware listener. Receive changes before data is committed to storage.
 * It allows to validate if some entity can exceeds quota limit if new changes
 * is coming.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ChangesListener.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ChangesListener implements ExtendedMandatoryItemsPersistenceListener
{
   /**
    * Logger.
    */
   protected final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ChangesListener");

   /**
    * Pending changes of current save. If save failed changes will be removed, otherwise
    * is moved into changes log to be pushed to coordinator by timer.
    */
   protected ThreadLocal<ChangesItem> pendingChanges = new ThreadLocal<ChangesItem>();

   /**
    * Accumulates changes of every save.
    */
   protected ChangesLog changesLog = new ChangesLog();

   /**
    * {@link WorkspaceQuotaManager} instance.
    */
   protected final WorkspaceQuotaManager wqm;

   /**
    * {@link RPCService}
    */
   protected final RPCService rpcService;

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
    * {@link QuotaPersister}
    */
   protected final QuotaPersister quotaPersister;

   /**
    * Remote command is obligated to apply changes log at coordinator.
    */
   protected RemoteCommand applyPersistedChangesTask;

   /**
    * @see WorkspaceQuotaContext#executor
    */
   protected final ExecutorService executor;

   /**
    * @see ExceededQuotaLimitException
    */
   protected final ExceededQuotaBehavior exceededQuotaBehavior;

   /**
    * {@link LocationFactory} instance.
    */
   protected final LocationFactory lFactory;

   /**
    * ChangesListener constructor.
    */
   ChangesListener(WorkspaceQuotaManager wqm)
   {
      this.wqm = wqm;
      this.rpcService = wqm.getContext().rpcService;
      this.wsName = wqm.getContext().wsName;
      this.rName = wqm.getContext().rName;
      this.uniqueName = wqm.getContext().uniqueName;
      this.quotaPersister = wqm.getContext().quotaPersister;
      this.executor = wqm.getContext().executor;
      this.exceededQuotaBehavior = wqm.getContext().exceededQuotaBehavior;
      this.lFactory = wqm.getContext().lFactory;

      initApplyPersistedChangesTask();
      rpcService.registerCommand(applyPersistedChangesTask);
   }

   /**
    * {@inheritDoc}
    *
    * Checks if new changes can exceeds some limits. It either can be node, workspace,
    * repository or global JCR instance.
    *
    * @throws IllegalStateException if data size exceeded quota limit
    */
   public void onSaveItems(ItemStateChangesLog itemStates)
   {
      try
      {
         ChangesItem changesItem = new ChangesItem();

         for (ItemState state : itemStates.getAllStates())
         {
            if (!state.getData().isNode())
            {
               String nodePath = getPath(state.getData().getQPath().makeParentPath());

               Set<String> parentsWithQuota = quotaPersister.getAllParentNodesWithQuota(rName, wsName, nodePath);

               for (String parent : parentsWithQuota)
               {
                  changesItem.updateNodeChangedSize(parent, state.getChangedSize());
                  addPathsWithAsyncUpdate(changesItem, parent);
               }

               changesItem.updateWorkspaceChangedSize(state.getChangedSize());
            }
            else
            {
               addPathsWithUnknownChangedSize(changesItem, state);
            }
         }

         validatePendingChanges(changesItem);
         pendingChanges.set(changesItem);
      }
      catch (ExceededQuotaLimitException e)
      {
         throw new IllegalStateException(e.getMessage(), e);
      }
   }

   /**
    * Checks if changes were made but changed size is unknown. If so, determinate
    * for which nodes data size should be recalculated at all and put those paths into
    * respective collection.
    */
   private void addPathsWithUnknownChangedSize(ChangesItem changesItem, ItemState state)
   {
      if (!state.isPersisted() && (state.isDeleted() || state.isRenamed()))
      {
         String itemPath = getPath(state.getData().getQPath());

         for (String trackedPath : quotaPersister.getAllTrackedNodes(rName, wsName))
         {
            if (itemPath.startsWith(trackedPath))
            {
               changesItem.addPathWithUnknownChangedSize(itemPath);
            }
         }
      }
   }

   /**
    * Checks if data size for node is represented by <code>quotableParent</code> path
    * should be updated asynchronously. If so that path is putting into respective collection.
    *
    * @param quotableParent
    *          absolute path to node for which quota is set
    */
   private void addPathsWithAsyncUpdate(ChangesItem changesItem, String quotableParent)
   {
      boolean isAsyncUpdate;
      try
      {
         isAsyncUpdate = quotaPersister.isNodeQuotaOrGroupOfNodesQuotaAsync(rName, wsName, quotableParent);
      }
      catch (UnknownQuotaLimitException e)
      {
         isAsyncUpdate = true;
      }

      if (isAsyncUpdate)
      {
         changesItem.addPathWithAsyncUpdate(quotableParent);
      }
   }

   /**
    * Checks if entities can accept new changes. It is not possible when
    * current behavior is {@link ExceededQuotaBehavior#EXCEPTION} and
    * new data size exceeds quota limit.
    *
    * @throws ExceededQuotaLimitException if new data size exceeds quota limit
    */
   private void validatePendingChanges(ChangesItem changesItem) throws ExceededQuotaLimitException
   {
      long delta = changesItem.getWorkspaceChangedSize() + changesLog.getWorkspaceChangedSize();
      if (delta > 0)
      {
         validatePendingWorkspaceChanges(delta);
         validatePendingRepositoryChanges(delta);
         validatePendingGlobalChanges(delta);
      }

      validatePendingNodesChanges(changesItem.getAllNodesCalculatedChangedSize());
   }

   /**
    * @see #validatePendingChanges(ChangesItem)
    */
   private void validatePendingWorkspaceChanges(long delta) throws ExceededQuotaLimitException
   {
      try
      {
         long quotaLimit = quotaPersister.getWorkspaceQuota(rName, wsName);

         try
         {
            long dataSize = quotaPersister.getWorkspaceDataSize(rName, wsName);

            if (dataSize + delta > quotaLimit)
            {
               behaveWhenQuotaExceeded("In workspace '" + wqm.uniqueName + "' data size exceeded quota limit");
            }
         }
         catch (UnknownDataSizeException e)
         {
            return;
         }
      }
      catch (UnknownQuotaLimitException e)
      {
         return;
      }
   }

   /**
    * @see #validatePendingChanges(ChangesItem)
    */
   private void validatePendingRepositoryChanges(long delta) throws ExceededQuotaLimitException
   {
      try
      {
         long quotaLimit = quotaPersister.getRepositoryQuota(rName);

         try
         {
            long dataSize = quotaPersister.getRepositoryDataSize(rName);

            if (dataSize + delta > quotaLimit)
            {
               behaveWhenQuotaExceeded("In repository '" + rName + "' data size exceeded quota limit");
            }
         }
         catch (UnknownDataSizeException e)
         {
            return;
         }
      }
      catch (UnknownQuotaLimitException e)
      {
         return;
      }
   }

   /**
    * @see #validatePendingChanges(ChangesItem)
    */
   private void validatePendingGlobalChanges(long delta) throws ExceededQuotaLimitException
   {
      try
      {
         long quotaLimit = quotaPersister.getGlobalQuota();

         try
         {
            long dataSize = quotaPersister.getGlobalDataSize();

            if (dataSize + delta > quotaLimit)
            {
               behaveWhenQuotaExceeded("Global data size exceeded quota limit");
            }
         }
         catch (UnknownDataSizeException e)
         {
            return;
         }
      }
      catch (UnknownQuotaLimitException e)
      {
         return;
      }
   }

   /**
    * @see #validatePendingChanges(ChangesItem)
    */
   private void validatePendingNodesChanges(Map<String, Long> calculatedNodesChangedSize)
      throws ExceededQuotaLimitException
   {
      for (Entry<String, Long> entry : calculatedNodesChangedSize.entrySet())
      {
         String nodePath = entry.getKey();
         long delta = entry.getValue() + changesLog.getNodeChangedSize(nodePath);

         if (delta > 0)
         {
            try
            {
               long dataSize = quotaPersister.getNodeDataSize(rName, wsName, nodePath);
               try
               {
                  long quotaLimit = quotaPersister.getNodeQuotaOrGroupOfNodesQuota(rName, wsName, nodePath);
                  if (dataSize + delta > quotaLimit)
                  {
                     behaveWhenQuotaExceeded("Node '" + nodePath + "' data size exceeded quota limit");
                  }
               }
               catch (UnknownQuotaLimitException e)
               {
                  continue;
               }
            }
            catch (UnknownDataSizeException e)
            {
               continue;
            }
         }
      }
   }

   /**
    * What to do if data size exceeded quota limit. Throwing exception or logging only.
    * Depends on preconfigured parameter.
    *
    * @param message
    *          the detail message for exception or log operation
    * @throws ExceededQuotaLimitException
    *          if current behavior is {@link ExceededQuotaBehavior#EXCEPTION}
    */
   private void behaveWhenQuotaExceeded(String message) throws ExceededQuotaLimitException
   {
      switch (exceededQuotaBehavior)
      {
         case EXCEPTION :
            throw new ExceededQuotaLimitException(message);

         case WARNING :
            LOG.warn(message);
            break;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onCommit()
   {
      ChangesItem changesItem = pendingChanges.get();
      try
      {
         pushChangesToCoordinator(changesItem.extractSyncChanges());
         changesLog.add(changesItem);
      }
      catch (SecurityException e)
      {
         throw new IllegalStateException("Can't push changes to coordinator", e.getCause());
      }
      catch (RPCException e)
      {
         throw new IllegalStateException("Can't push changes to coordinator", e.getCause());
      }
      finally
      {
         pendingChanges.remove();
      }
   }

   /**
    * Push all changes to coordinator to apply.
    */
   protected void pushAllChangesToCoordinator() throws SecurityException, RPCException
   {
      ChangesItem changesItem = changesLog.pollAndMergeAll();
      pushChangesToCoordinator(changesItem);
   }

   /**
    * Push changes to coordinator to apply.
    */
   protected void pushChangesToCoordinator(ChangesItem changesItem) throws SecurityException, RPCException
   {
      if (!changesItem.isEmpty())
      {
         rpcService.executeCommandOnCoordinator(applyPersistedChangesTask, true, changesItem);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onRollback()
   {
      pendingChanges.remove();
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return true;
   }

   /**
    * Returns item absolute path.
    *
    * @param path
    *          {@link QPath} representation
    * @throws IllegalStateException if something wrong
    */
   private String getPath(QPath path)
   {
      try
      {
         return lFactory.createJCRPath(path).getAsString(false);
      }
      catch (RepositoryException e)
      {
         throw new IllegalStateException(e.getMessage(), e);
      }
   }

   /**
    * Free all allocated resources.
    */
   public void destroy()
   {
      rpcService.unregisterCommand(applyPersistedChangesTask);
   }

   /**
    * Initialize remote command {@link #applyPersistedChangesTask}
    */
   private void initApplyPersistedChangesTask()
   {
      applyPersistedChangesTask = new RemoteCommand()
      {
         /**
          * {@inheritDoc}
          */
         public String getId()
         {
            return "ChangesListener-" + uniqueName + "-applyPersistedChangesTask";
         }

         /**
          * Accumulates persisted changes.
          */
         public Serializable execute(final Serializable[] args) throws Throwable
         {
            ChangesItem changesItem = (ChangesItem)args[0];

            Runnable task = new ApplyPersistedChangesTask(wqm.getContext(), changesItem);
            executor.execute(task);

            return null;
         }
      };
   }
}
