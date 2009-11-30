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

import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.ReadOnlyThroughChanges;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListenerFilter;
import org.exoplatform.services.jcr.dataflow.persistent.MandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientItemData;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br>
 * Workspace-level data manager. Connects persistence layer with <code>WorkspaceDataContainer</code>
 * instance. Provides read and save operations. Handles listeners on save operation.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: WorkspacePersistentDataManager.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public abstract class WorkspacePersistentDataManager implements PersistentDataManager
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("jcr.WorkspacePersistentDataManager");

   /**
    * Workspace data container (persistent storage).
    */
   protected WorkspaceDataContainer dataContainer;

   /**
    * System workspace data container (persistent storage).
    */
   protected WorkspaceDataContainer systemDataContainer;

   /**
    * Persistent level listeners. This listeners can be filtered by filters from
    * <code>liestenerFilters</code> list.
    */
   protected final List<ItemsPersistenceListener> listeners;

   /**
    * Mandatory persistent level listeners.
    */
   protected final List<MandatoryItemsPersistenceListener> mandatoryListeners;

   /**
    * Persistent level liesteners filters.
    */
   protected final List<ItemsPersistenceListenerFilter> liestenerFilters;

   /**
    * Read-only status.
    */
   protected boolean readOnly = false;

   /**
    * WorkspacePersistentDataManager constructor.
    * 
    * @param dataContainer
    *          workspace data container
    * @param systemDataContainerHolder
    *          holder of system workspace data container
    */
   public WorkspacePersistentDataManager(WorkspaceDataContainer dataContainer,
      SystemDataContainerHolder systemDataContainerHolder)
   {
      this.dataContainer = dataContainer;
      this.listeners = new ArrayList<ItemsPersistenceListener>();
      this.mandatoryListeners = new ArrayList<MandatoryItemsPersistenceListener>();
      this.liestenerFilters = new ArrayList<ItemsPersistenceListenerFilter>();
      this.systemDataContainer = systemDataContainerHolder.getContainer();
   }

   /**
    * {@inheritDoc}
    */
   public void save(final ItemStateChangesLog changesLog) throws RepositoryException
   {

      // check if this workspace container is not read-only
      if (readOnly && !(changesLog instanceof ReadOnlyThroughChanges))
         throw new ReadOnlyWorkspaceException("Workspace container '" + dataContainer.getName() + "' is read-only.");

      final Set<QPath> addedNodes = new HashSet<QPath>();

      WorkspaceStorageConnection thisConnection = null;
      WorkspaceStorageConnection systemConnection = null;

      try
      {
         for (Iterator<ItemState> iter = changesLog.getAllStates().iterator(); iter.hasNext();)
         {
            ItemState itemState = iter.next();

            if (!itemState.isPersisted())
               continue;

            long start = System.currentTimeMillis();

            TransientItemData data = (TransientItemData)itemState.getData();

            WorkspaceStorageConnection conn = null;
            if (isSystemDescendant(data.getQPath()))
            {
               conn = systemConnection == null
               // we need system connection but it's not exist
                  ? systemConnection = (systemDataContainer != dataContainer
                  // if it's different container instances
                     ? systemDataContainer.equals(dataContainer) && thisConnection != null
                     // but container confugrations are same and non-system connnection open
                        // reuse this connection as system
                        ? systemDataContainer.reuseConnection(thisConnection)
                        // or open one new system
                        : systemDataContainer.openConnection()
                     // else if it's same container instances (system and this)
                     : thisConnection == null
                     // and non-system connection doens't exist - open it
                        ? thisConnection = dataContainer.openConnection()
                        // if already open - use it
                        : thisConnection)
                  // system connection opened - use it
                  : systemConnection;
            }
            else
            {
               conn = thisConnection == null
               // we need this conatiner conection
                  ? thisConnection = (systemDataContainer != dataContainer
                  // if it's different container instances
                     ? dataContainer.equals(systemDataContainer) && systemConnection != null
                     // but container confugrations are same and system connnection open
                        // reuse system connection as this
                        ? dataContainer.reuseConnection(systemConnection)
                        // or open one new
                        : dataContainer.openConnection()
                     // else if it's same container instances (system and this)
                     : systemConnection == null
                     // and system connection doens't exist - open it
                        ? systemConnection = dataContainer.openConnection()
                        // if already open - use it
                        : systemConnection)
                  // this connection opened - use it
                  : thisConnection;
            }

            data.increasePersistedVersion();

            if (itemState.isAdded())
            {
               doAdd(data, conn, addedNodes);
            }
            else if (itemState.isUpdated())
            {
               doUpdate(data, conn);
            }
            else if (itemState.isDeleted())
            {
               doDelete(data, conn);
            }
            else if (itemState.isRenamed())
            {
               doRename(data, conn, addedNodes);
            }

            if (LOG.isDebugEnabled())
               LOG.debug(ItemState.nameFromValue(itemState.getState()) + " " + (System.currentTimeMillis() - start)
                  + "ms, " + data.getQPath().getAsString());
         }
         if (thisConnection != null)
            thisConnection.commit();
         if (systemConnection != null && !systemConnection.equals(thisConnection))
            systemConnection.commit();
      }
      finally
      {
         if (thisConnection != null && thisConnection.isOpened())
            thisConnection.rollback();
         if (systemConnection != null && !systemConnection.equals(thisConnection) && systemConnection.isOpened())
            systemConnection.rollback();

         // help to GC
         addedNodes.clear();
      }

      notifySaveItems(changesLog);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(final String identifier) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getItemData(identifier);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(final String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {

      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         final List<PropertyData> allRefs = con.getReferencesData(identifier);
         final List<PropertyData> refProps = new ArrayList<PropertyData>();
         for (int i = 0; i < allRefs.size(); i++)
         {
            PropertyData ref = allRefs.get(i);
            if (skipVersionStorage)
            {
               if (!ref.getQPath().isDescendantOf(Constants.JCR_VERSION_STORAGE_PATH))
                  refProps.add(ref);
            }
            else
               refProps.add(ref);
         }
         return refProps;
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(final NodeData nodeData) throws RepositoryException
   {

      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildNodesData(nodeData);
      }
      finally
      {
         con.close();
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(NodeData parent) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildNodesCount(parent);
      }
      finally
      {
         con.close();
      }
   }   

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(final NodeData nodeData) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildPropertiesData(nodeData);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(final NodeData nodeData) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.listChildPropertiesData(nodeData);
      }
      finally
      {
         con.close();
      }
   }

   // ----------------------------------------------
   /**
    * Check if given node path contains index higher 1 and if yes if same-name sibling exists in
    * persistence or in current changes log.
    */
   private void checkSameNameSibling(NodeData node, WorkspaceStorageConnection con, final Set<QPath> addedNodes)
      throws RepositoryException
   {
      if (node.getQPath().getIndex() > 1)
      {
         // check if an older same-name sibling exists
         // the check is actual for all operations including delete

         final QPathEntry[] path = node.getQPath().getEntries();
         final QPathEntry[] siblingPath = new QPathEntry[path.length];
         final int li = path.length - 1;
         System.arraycopy(path, 0, siblingPath, 0, li);

         siblingPath[li] = new QPathEntry(path[li], path[li].getIndex() - 1);

         if (addedNodes.contains(new QPath(siblingPath)))
         {
            // current changes log has the older same-name sibling
            return;
         }
         else
         {
            // check in persistence
            final WorkspaceStorageConnection acon = dataContainer.openConnection();
            try
            {
               NodeData parent = (NodeData)acon.getItemData(node.getParentIdentifier());
               QPathEntry myName = node.getQPath().getEntries()[node.getQPath().getEntries().length - 1];
               ItemData sibling =
                  acon.getItemData(parent, new QPathEntry(myName.getNamespace(), myName.getName(),
                     myName.getIndex() - 1));
               if (sibling == null || !sibling.isNode())
               {
                  throw new InvalidItemStateException("Node can't be saved " + node.getQPath().getAsString()
                     + ". No same-name sibling exists with index " + (myName.getIndex() - 1) + ".");
               }
            }
            finally
            {
               acon.rollback();
            }
         }
      }
   }

   /**
    * Performs actual item data deleting.
    * 
    * @param item
    *          to delete
    * @param con
    * @throws RepositoryException
    * @throws InvalidItemStateException
    *           if the item is already deleted
    */
   protected void doDelete(final TransientItemData item, final WorkspaceStorageConnection con)
      throws RepositoryException, InvalidItemStateException
   {

      if (item.isNode())
         con.delete((NodeData)item);
      else
         con.delete((PropertyData)item);
   }

   /**
    * Performs actual item data updating.
    * 
    * @param item
    *          to update
    * @param con
    *          connection
    * @throws RepositoryException
    * @throws InvalidItemStateException
    *           if the item not found TODO compare persistedVersion number
    */
   protected void doUpdate(final TransientItemData item, final WorkspaceStorageConnection con)
      throws RepositoryException, InvalidItemStateException
   {

      if (item.isNode())
      {
         con.update((NodeData)item);
      }
      else
      {
         con.update((PropertyData)item);
      }
   }

   /**
    * Performs actual item data adding.
    * 
    * @param item
    *          to add
    * @param con
    *          connection
    * @throws RepositoryException
    * @throws InvalidItemStateException
    *           if the item is already added
    */
   protected void doAdd(final TransientItemData item, final WorkspaceStorageConnection con, final Set<QPath> addedNodes)
      throws RepositoryException, InvalidItemStateException
   {

      if (item.isNode())
      {
         final NodeData node = (NodeData)item;

         checkSameNameSibling(node, con, addedNodes);
         addedNodes.add(node.getQPath());

         con.add(node);
      }
      else
      {
         con.add((PropertyData)item);
      }
   }

   /**
    * Perform node rename.
    * 
    * @param item
    * @param con
    * @param addedNodes
    * @throws RepositoryException
    * @throws InvalidItemStateException
    */
   protected void doRename(final TransientItemData item, final WorkspaceStorageConnection con,
      final Set<QPath> addedNodes) throws RepositoryException, InvalidItemStateException
   {
      final NodeData node = (NodeData)item;

      checkSameNameSibling(node, con, addedNodes);
      addedNodes.add(node.getQPath());

      con.rename(node);
   }

   /**
    * 
    * Get current container time.
    * 
    * @return current time
    */
   public Calendar getCurrentTime()
   {
      return dataContainer.getCurrentTime();
   }

   // ---------------------------------------------

   /**
    * Adds listener to the list.
    * 
    * @param listener
    */
   public void addItemPersistenceListener(ItemsPersistenceListener listener)
   {
      if (listener instanceof MandatoryItemsPersistenceListener)
         mandatoryListeners.add((MandatoryItemsPersistenceListener)listener);
      else
         listeners.add(listener);
      if (LOG.isDebugEnabled())
         LOG.debug("Workspace '" + this.dataContainer.getName() + "' listener registered: " + listener);
   }

   public void removeItemPersistenceListener(ItemsPersistenceListener listener)
   {
      if (listener instanceof MandatoryItemsPersistenceListener)
         mandatoryListeners.remove(listener);
      else
         listeners.remove(listener);

      if (LOG.isDebugEnabled())
         LOG.debug("Workspace '" + this.dataContainer.getName() + "' listener unregistered: " + listener);
   }

   /**
    * {@inheritDoc}
    */
   public void addItemPersistenceListenerFilter(ItemsPersistenceListenerFilter filter)
   {
      this.liestenerFilters.add(filter);
   }

   /**
    * {@inheritDoc}
    */
   public void removeItemPersistenceListenerFilter(ItemsPersistenceListenerFilter filter)
   {
      this.liestenerFilters.remove(filter);
   }

   /**
    * Check if the listener can be accepted. If at least one filter doesn't accept the listener it
    * returns false, true otherwise.
    * 
    * @param listener
    *          ItemsPersistenceListener
    * @return boolean, true if accepted, false otherwise.
    */
   protected boolean isListenerAccepted(ItemsPersistenceListener listener)
   {
      for (ItemsPersistenceListenerFilter f : liestenerFilters)
      {
         if (!f.accept(listener))
            return false;
      }

      return true;
   }

   /**
    * Notify all listeners about current changes log persistent state.
    * 
    * @param changesLog
    *          ItemStateChangesLog
    */
   protected void notifySaveItems(ItemStateChangesLog changesLog)
   {
      for (MandatoryItemsPersistenceListener mlistener : mandatoryListeners)
         mlistener.onSaveItems(changesLog);

      for (ItemsPersistenceListener listener : listeners)
      {
         if (isListenerAccepted(listener))
            listener.onSaveItems(changesLog);
      }
   }

   /**
    * Tell if the path is jcr:system descendant.
    * 
    * @param path
    *          path to check
    * @return boolean result, true if yes - it's jcr:system tree path
    */
   private boolean isSystemDescendant(QPath path)
   {
      return path.isDescendantOf(Constants.JCR_SYSTEM_PATH) || path.equals(Constants.JCR_SYSTEM_PATH);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(final NodeData parentData, final QPathEntry name) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getItemData(parentData, name);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}true
    */
   public boolean isReadOnly()
   {
      return readOnly;
   }

   /**
    * {@inheritDoc}
    */
   public void setReadOnly(boolean status)
   {
      this.readOnly = status;
   }

}
