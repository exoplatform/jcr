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

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Is supposed to be executed on coordinator only.
 *
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: ApplyPersistedChangesTask.java 34360 Aug 22, 2012 tolusha $
 */
public class ApplyPersistedChangesTask implements Runnable
{

   /**
    * Logger.
    */
   protected final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ApplyPersistedChangesTask");

   /**
    * The changes to be applied.
    */
   protected final ChangesItem changesItem;

   /**
    * Workspace name.
    */
   protected final String wsName;

   /**
    * Repository name.
    */
   protected final String rName;

   /**
    * {@link QuotaPersister}
    */
   protected final QuotaPersister quotaPersister;

   /**
    * {@link WorkspaceQuotaContext} instance.
    */
   protected final WorkspaceQuotaContext context;

   /**
    * Helps to calculate node data size.
    */
   protected final CalculateNodeDataSizeTool calculateNodeDataSizeTool;

   /**
    * ApplyPersistedChangesTask constructor.
    */
   ApplyPersistedChangesTask(WorkspaceQuotaContext context, ChangesItem changesItem)
   {
      this.changesItem = changesItem;
      this.context = context;

      this.wsName = context.wsName;
      this.rName = context.rName;
      this.quotaPersister = context.quotaPersister;
      this.calculateNodeDataSizeTool = new CalculateNodeDataSizeTool(context);
   }

   /**
    * {@inheritDoc}
    */
   public void run()
   {
      try
      {
         long delta = changesItem.getWorkspaceChangedSize();

         accumulatePersistedWorkspaceChanges(delta);
         accumulatePersistedRepositoryChanges(delta);
         accumulatePersistedGlobalChanges(delta);

         removeNodeDataSizeFor(changesItem.getAllNodesUnknownChangedSize());

         accumulatePersistedNodesChanges(changesItem.getAllNodesCalculatedChangedSize());
      }
      catch (QuotaManagerException e)
      {
         LOG.error("Can't apply persisted changes", e);
      }
   }

   /**
    * Remove from {@link QuotaPersister} stored node data sized
    * if node path is considered as path where changes were made
    * but changed size is unknown due to different reasons.
    */
   private void removeNodeDataSizeFor(Set<String> paths)
   {
      Set<String> trackedNodes = quotaPersister.getAllTrackedNodes(rName, wsName);

      for (String nodePath : paths)
      {
         for (String trackedPath : trackedNodes)
         {
            if (trackedPath.startsWith(nodePath))
            {
               quotaPersister.removeNodeDataSize(rName, wsName, trackedPath);
            }
         }
      }
   }

   /**
    * Update nodes data size.
    */
   protected void accumulatePersistedNodesChanges(Map<String, Long> calculatedChangedNodesSize)
      throws QuotaManagerException
   {
      for (Entry<String, Long> entry : calculatedChangedNodesSize.entrySet())
      {
         String nodePath = entry.getKey();
         long delta = entry.getValue();

         try
         {
            long dataSize = delta + quotaPersister.getNodeDataSize(rName, wsName, nodePath);
            quotaPersister.setNodeDataSizeIfQuotaExists(rName, wsName, nodePath, dataSize);
         }
         catch (UnknownDataSizeException e)
         {
            calculateNodeDataSizeTool.getAndSetNodeDataSize(nodePath);
         }
      }
   }

   /**
    * Update workspace data size.
    */
   protected void accumulatePersistedWorkspaceChanges(long delta) throws QuotaManagerException
   {
      long dataSize = 0;
      try
      {
         dataSize = quotaPersister.getWorkspaceDataSize(rName, wsName);
      }
      catch (UnknownDataSizeException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }

      long newDataSize = Math.max(dataSize + delta, 0); // avoid possible inconsistency
      quotaPersister.setWorkspaceDataSize(rName, wsName, newDataSize);
   }

   /**
    * Update repository data size.
    */
   protected void accumulatePersistedRepositoryChanges(long delta)
   {
      long dataSize = 0;
      try
      {
         dataSize = quotaPersister.getRepositoryDataSize(rName);
      }
      catch (UnknownDataSizeException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }

      long newDataSize = Math.max(dataSize + delta, 0); // to avoid possible inconsistency
      quotaPersister.setRepositoryDataSize(rName, newDataSize);
   }

   /**
    * Update global data size.
    */
   private void accumulatePersistedGlobalChanges(long delta)
   {
      long dataSize = 0;
      try
      {
         dataSize = quotaPersister.getGlobalDataSize();
      }
      catch (UnknownDataSizeException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace(e.getMessage(), e);
         }
      }

      long newDataSize = Math.max(dataSize + delta, 0); // to avoid possible inconsistency
      quotaPersister.setGlobalDataSize(newDataSize);
   }
}
