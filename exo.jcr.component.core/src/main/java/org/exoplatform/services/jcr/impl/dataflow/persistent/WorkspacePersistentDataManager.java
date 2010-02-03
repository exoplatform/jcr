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

import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.ReadOnlyThroughChanges;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListenerFilter;
import org.exoplatform.services.jcr.dataflow.persistent.MandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedItemData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileIOChannel;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br>
 * Workspace-level data manager. Connects persistence layer with <code>WorkspaceDataContainer</code>
 * instance. Provides read and save operations. Handles listeners on save operation.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id$
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
   protected final WorkspaceDataContainer dataContainer;

   /**
    * System workspace data container (persistent storage).
    */
   protected final WorkspaceDataContainer systemDataContainer;

   /**
    * Value sorages provider (for dest file suggestion on save).
    */
   // TODO protected final ValueStoragePluginProvider valueStorageProvider;

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
   //ValueStoragePluginProvider valueStorageProvider, 
      SystemDataContainerHolder systemDataContainerHolder)
   {
      this.dataContainer = dataContainer;
      this.systemDataContainer = systemDataContainerHolder.getContainer();
      // this.valueStorageProvider = valueStorageProvider;

      this.listeners = new ArrayList<ItemsPersistenceListener>();
      this.mandatoryListeners = new ArrayList<MandatoryItemsPersistenceListener>();
      this.liestenerFilters = new ArrayList<ItemsPersistenceListenerFilter>();
   }

   /**
    * {@inheritDoc}
    */
   public void save(final ItemStateChangesLog changesLog) throws RepositoryException
   {
      // check if this workspace container is not read-only
      if (readOnly && !(changesLog instanceof ReadOnlyThroughChanges))
      {
         throw new ReadOnlyWorkspaceException("Workspace container '" + dataContainer.getName() + "' is read-only.");
      }

      final ChangesLogPersister persister = new ChangesLogPersister();

      // whole log will be reconstructed with persisted data 
      ItemStateChangesLog persistedLog;

      try
      {
         if (changesLog instanceof PlainChangesLogImpl)
         {
            persistedLog = persister.save((PlainChangesLogImpl)changesLog);
         }
         else if (changesLog instanceof TransactionChangesLog)
         {
            TransactionChangesLog orig = (TransactionChangesLog)changesLog;

            TransactionChangesLog persisted = new TransactionChangesLog();
            persisted.setSystemId(orig.getSystemId());

            for (ChangesLogIterator iter = orig.getLogIterator(); iter.hasNextLog();)
            {
               persisted.addLog(persister.save(iter.nextLog()));
            }

            persistedLog = persisted;
         }
         else
         {
            // we don't support other types now... i.e. add else-if for that type here
            throw new RepositoryException("Unsupported changes log class " + changesLog.getClass());
         }
         persister.commit();
      }
      catch (IOException e)
      {
         throw new RepositoryException("Save error", e);
      }
      finally
      {
         persister.rollback();
      }

      notifySaveItems(persistedLog, true);
   }

   class ChangesLogPersister
   {

      private final Set<QPath> addedNodes = new HashSet<QPath>();

      private WorkspaceStorageConnection thisConnection = null;

      private WorkspaceStorageConnection systemConnection = null;

      protected void commit() throws IllegalStateException, RepositoryException
      {
         if (thisConnection != null)
         {
            thisConnection.commit();
         }
         if (systemConnection != null && !systemConnection.equals(thisConnection))
         {
            systemConnection.commit();
         }
      }

      protected void rollback() throws IllegalStateException, RepositoryException
      {
         if (thisConnection != null && thisConnection.isOpened())
         {
            thisConnection.rollback();
         }
         if (systemConnection != null && !systemConnection.equals(thisConnection) && systemConnection.isOpened())
         {
            systemConnection.rollback();
         }

         // help to GC
         addedNodes.clear();
      }

      protected WorkspaceStorageConnection getSystemConnection() throws RepositoryException
      {
         return systemConnection == null
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

      protected WorkspaceStorageConnection getThisConnection() throws RepositoryException
      {
         return thisConnection == null
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

      protected PlainChangesLogImpl save(PlainChangesLog changesLog) throws InvalidItemStateException,
         RepositoryException, IOException
      { //LOG.info(changesLog.dump())
        // copy state
         PlainChangesLogImpl newLog =
            new PlainChangesLogImpl(new ArrayList<ItemState>(), changesLog.getSessionId(), changesLog.getEventType(),
               changesLog.getPairId());

         for (Iterator<ItemState> iter = changesLog.getAllStates().iterator(); iter.hasNext();)
         {
            ItemState prevState = iter.next();
            ItemData newData;

            if (prevState.getData() instanceof PersistedItemData)
            {
               // use existing if persisted 
               newData = prevState.getData();
            }
            else
            {
               // copy transient as persisted 
               if (prevState.isNode())
               {
                  NodeData prevData = (NodeData)prevState.getData();
                  newData =
                     new PersistedNodeData(prevData.getIdentifier(), prevData.getQPath(), prevData
                        .getParentIdentifier(), prevData.getPersistedVersion() + 1, prevData.getOrderNumber(), prevData
                        .getPrimaryTypeName(), prevData.getMixinTypeNames(), prevData.getACL());
               }
               else
               {
                  PropertyData prevData = (PropertyData)prevState.getData();

                  if (prevData.getValues() != null) // null if it's DELETE state
                  {
                     List<ValueData> values = new ArrayList<ValueData>();
                     for (int i = 0; i < prevData.getValues().size(); i++)
                     {
                        ValueData vd = prevData.getValues().get(i);

                        if (vd instanceof TransientValueData)
                        {
                           TransientValueData tvd = (TransientValueData)vd;
                           ValueData pvd;

                           if (vd.isByteArray())
                           {
                              pvd = new ByteArrayPersistedValueData(i, vd.getAsByteArray());
                              values.add(pvd);
                           }
                           else
                           {
                              // TODO ask dest file from VS provider, can be null after
                              // TODO for JBC case, the storage connection will evict the replicated Value to read it from the DB
                              File destFile = null;

                              if (tvd.getSpoolFile() != null)
                              {
                                 // spooled to temp file
                                 pvd = new StreamPersistedValueData(i, tvd.getSpoolFile(), destFile);
                              }
                              else
                              {
                                 // with original stream
                                 pvd = new StreamPersistedValueData(i, tvd.getOriginalStream(), destFile);
                              }

                              values.add(pvd);
                           }

                           tvd.delegate(pvd);
                        }
                        else
                        {
                           values.add(vd);
                        }
                     }

                     newData =
                        new PersistedPropertyData(prevData.getIdentifier(), prevData.getQPath(), prevData
                           .getParentIdentifier(), prevData.getPersistedVersion() + 1, prevData.getType(), prevData
                           .isMultiValued(), values);
                  }
                  else
                  {
                     newData =
                        new PersistedPropertyData(prevData.getIdentifier(), prevData.getQPath(), prevData
                           .getParentIdentifier(), prevData.getPersistedVersion() + 1, prevData.getType(), prevData
                           .isMultiValued(), null);
                  }
               }
            }

            ItemState itemState =
               new ItemState(newData, prevState.getState(), prevState.isEventFire(), prevState.getAncestorToSave(),
                  prevState.isInternallyCreated(), prevState.isPersisted());

            newLog.add(itemState);

            // save state
            if (itemState.isPersisted())
            {
               long start = System.currentTimeMillis();

               ItemData data = itemState.getData();

               WorkspaceStorageConnection conn;
               if (isSystemDescendant(data.getQPath()))
               {
                  conn = getSystemConnection();
               }
               else
               {
                  conn = getThisConnection();
               }

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
               {
                  LOG.debug(ItemState.nameFromValue(itemState.getState()) + " " + (System.currentTimeMillis() - start)
                     + "ms, " + data.getQPath().getAsString());
               }
            }
         }
         return newLog;
      }
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
               acon.close();
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
   protected void doDelete(final ItemData item, final WorkspaceStorageConnection con) throws RepositoryException,
      InvalidItemStateException
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
   protected void doUpdate(final ItemData item, final WorkspaceStorageConnection con) throws RepositoryException,
      InvalidItemStateException
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
   protected void doAdd(final ItemData item, final WorkspaceStorageConnection con, final Set<QPath> addedNodes)
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
   protected void doRename(final ItemData item, final WorkspaceStorageConnection con, final Set<QPath> addedNodes)
      throws RepositoryException, InvalidItemStateException
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
    * Notify listeners about current changes log persistent state.
    * Listeners notified according to is listener transaction aware.
    * 
    * @param changesLog
    *          ItemStateChangesLog
    * @param isListenerTXAware - is listeners notified in transaction, or after transaction
    */
   protected void notifySaveItems(final ItemStateChangesLog changesLog, boolean isListenerTXAware)
   {
      for (MandatoryItemsPersistenceListener mlistener : mandatoryListeners)
      {
         if (mlistener.isTXAware() == isListenerTXAware)
         {
            mlistener.onSaveItems(changesLog);
         }
      }

      for (ItemsPersistenceListener listener : listeners)
      {
         if (listener.isTXAware() == isListenerTXAware && isListenerAccepted(listener))
         {
            listener.onSaveItems(changesLog);
         }
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
