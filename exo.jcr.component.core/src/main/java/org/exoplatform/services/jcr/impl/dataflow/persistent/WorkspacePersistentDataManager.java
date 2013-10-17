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
import org.exoplatform.services.jcr.dataflow.FastAddPlainChangesLog;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PersistentDataManager;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ExtendedMandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.MandatoryItemsPersistenceListener;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedItemData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.AbstractValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManagerListener;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionManager;

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
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspacePersistentDataManager");

   /**
    * Workspace data container (persistent storage).
    */
   protected final WorkspaceDataContainer dataContainer;

   /**
    * System workspace data container (persistent storage).
    */
   protected final WorkspaceDataContainer systemDataContainer;

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
    * Mandatory persistent level listeners.
    */
   protected final List<ExtendedMandatoryItemsPersistenceListener> extendedMandatoryListeners;

   /**
    * The resource manager
    */
   private final TransactionableResourceManager txResourceManager;

   /**
    * The transaction manager
    */
   protected final TransactionManager transactionManager;

   /**
    * Changes log wrapper adds possibility to replace changes log.
    * Changes log contains transient data on save but listeners should be notified
    * with persisted data only.
    */
   protected class ChangesLogWrapper implements ItemStateChangesLog
   {
      private ItemStateChangesLog log;

      ChangesLogWrapper(ItemStateChangesLog log)
      {
         this.log = log;
      }

      /**
       * Replace log with persisted data only.
       */
      protected void setLog(ItemStateChangesLog log)
      {
         this.log = log;
      }

      protected ItemStateChangesLog getChangesLog()
      {
         return log;
      }

      protected ItemStateChangesLog optimizeAndGetChangesLog()
      {
         // get all persisted property states, to avoid iterating by other states
         Map<String, LinkedList<RemovableItemState>> persistedCheckedStates =
            new HashMap<String, LinkedList<RemovableItemState>>();

         TransactionChangesLog orig = (TransactionChangesLog)log;
         ChangesLogIterator changesLogIterator = orig.getLogIterator();

         boolean skipOptimizeChangesLog = true;
         while (changesLogIterator.hasNextLog())
         {
            PlainChangesLog changesLog = changesLogIterator.nextLog();

            List<ItemState> states = changesLog.getAllStates();
            for (int i = 0, length = states.size(); i < length; i++)
            {
               ItemState itemState = states.get(i);
               if (itemState.isPersisted() && !itemState.isNode())
               {
                  String pId = itemState.getData().getIdentifier();
                  LinkedList<RemovableItemState> removeItemStates = persistedCheckedStates.get(pId);
                  if (removeItemStates == null)
                  {
                     removeItemStates = new LinkedList<RemovableItemState>();
                     persistedCheckedStates.put(pId, removeItemStates);
                  }
                  else
                  {
                     skipOptimizeChangesLog = false;
                  }
                  removeItemStates.add(new RemovableItemState(itemState, changesLog));
               }
            }
         }

         if (!skipOptimizeChangesLog)
         {
            Map<PlainChangesLog, Set<ItemState>> itemsToBeRemoved = new HashMap<PlainChangesLog, Set<ItemState>>();
            for (LinkedList<RemovableItemState> removableStates : persistedCheckedStates.values())
            {
               // Perform optimizations while list of property states contains more than one state.
               while (removableStates.size() > 1)
               {
                  // Get last state and remove it, since there is no need to review this items once more
                  RemovableItemState lastRemovableState = removableStates.removeLast();
                  ItemState lastState = lastRemovableState.getState();

                  if (lastState.isUpdated() || lastState.isDeleted())
                  {
                     // iterate from the head to the end
                     Iterator<RemovableItemState> iterator = removableStates.iterator();
                     while (iterator.hasNext())
                     {
                        RemovableItemState removableCheckedState = iterator.next();
                        ItemState checkedState = removableCheckedState.getState();
                        // remove updated state, because delete, add or update state 
                        if ((checkedState.isAdded() || checkedState.isUpdated()) && lastState.isDeleted())
                        {
                           removableCheckedState.markAsToBeRemoved();
                           // Usecase when property was added and removed within the transaction or save. 
                           // So make all related changes logical 
                           if (checkedState.isAdded())
                           {
                              lastRemovableState.markAsToBeRemoved();
                           }
                        }
                        else if (checkedState.isAdded() && lastState.isUpdated())
                        {
                           // Usecase when property was added many times within the transaction or save. 
                           // So make last update to add and make all related changes logical
                           removableCheckedState.markAsToBeRemoved();
                           lastState.makeStateAdded();
                        }
                        else if (checkedState.isUpdated())
                        {
                           removableCheckedState.markAsToBeRemoved();
                        }

                        if (removableCheckedState.toBeRemoved() || lastRemovableState.toBeRemoved())
                        {
                           List<RemovableItemState> rItemStates = new ArrayList<RemovableItemState>(2);
                           if (removableCheckedState.toBeRemoved())
                           {
                              rItemStates.add(removableCheckedState);
                           }
                           if (lastRemovableState.toBeRemoved())
                           {
                              rItemStates.add(lastRemovableState);
                           }
                           for (int i = 0, length = rItemStates.size(); i < length; i++)
                           {
                              RemovableItemState ris = rItemStates.get(i);
                              Set<ItemState> items = itemsToBeRemoved.get(ris.getPlainChangesLog());
                              if (items == null)
                              {
                                 items = new HashSet<ItemState>();
                                 itemsToBeRemoved.put(ris.getPlainChangesLog(), items);
                              }
                              items.add(ris.getState());
                           }
                        }
                     }
                     removableStates.clear();
                  }
               }
            }

            TransactionChangesLog compressed = new TransactionChangesLog();
            compressed.setSystemId(orig.getSystemId());
            for (ChangesLogIterator iter = orig.getLogIterator(); iter.hasNextLog();)
            {
               PlainChangesLog changesLog = iter.nextLog();
               Set<ItemState> statesToRemove = itemsToBeRemoved.get(changesLog);
               if (statesToRemove != null)
               {
                  PlainChangesLog newLog = FastAddPlainChangesLog.getInstance(changesLog);
                  List<ItemState> states = changesLog.getAllStates();
                  for (int i = 0, length = states.size(); i < length; i++)
                  {
                     ItemState is = states.get(i);
                     if (!statesToRemove.isEmpty() && statesToRemove.contains(is))
                     {
                        statesToRemove.remove(is);
                     }
                     else
                     {
                        newLog.add(is);
                     }
                  }
                  changesLog = newLog;
               }
               compressed.addLog(changesLog);
            }
            return log = compressed;
         }
         return log;
      }

      /**
       * {@inheritDoc}
       */
      public List<ItemState> getAllStates()
      {
         return log.getAllStates();
      }

      /**
       * {@inheritDoc}
       */
      public int getSize()
      {
         return log.getSize();
      }

      /**
       * {@inheritDoc}
       */
      public String dump()
      {
         return log.dump();
      }
   }

   protected class RemovableItemState
   {
      private boolean toBeRemoved;

      private final ItemState state;

      private final PlainChangesLogImpl changesLog;

      public RemovableItemState(ItemState state, PlainChangesLog changesLog)
      {
         this.state = state;
         this.changesLog = (PlainChangesLogImpl)changesLog;
      }

      public ItemState getState()
      {
         return state;
      }

      public PlainChangesLog getPlainChangesLog()
      {
         return changesLog;
      }

      public void markAsToBeRemoved()
      {
         toBeRemoved = true;
      }

      public boolean toBeRemoved()
      {
         return toBeRemoved;
      }
   }

   /**
    * WorkspacePersistentDataManager constructor.
    * 
    * @param dataContainer
    *          workspace data container
    * @param systemDataContainerHolder
    *          holder of system workspace data container
    */
   protected WorkspacePersistentDataManager(WorkspaceDataContainer dataContainer,
      SystemDataContainerHolder systemDataContainerHolder)
   {
      this(dataContainer, systemDataContainerHolder, null, null);
   }

   /**
    * WorkspacePersistentDataManager constructor.
    * 
    * @param dataContainer
    *          workspace data container
    * @param systemDataContainerHolder
    *          holder of system workspace data container
    * @param txResourceManager
    *          the resource manager used to manage the whole tx
    */
   public WorkspacePersistentDataManager(WorkspaceDataContainer dataContainer,
      SystemDataContainerHolder systemDataContainerHolder, TransactionableResourceManager txResourceManager,
      TransactionManager transactionManager)
   {
      this.dataContainer = dataContainer;
      this.systemDataContainer = systemDataContainerHolder.getContainer();

      this.listeners = new ArrayList<ItemsPersistenceListener>();
      this.mandatoryListeners = new ArrayList<MandatoryItemsPersistenceListener>();
      this.extendedMandatoryListeners = new ArrayList<ExtendedMandatoryItemsPersistenceListener>();

      this.txResourceManager = txResourceManager;
      this.transactionManager = transactionManager;
   }

   /**
    * {@inheritDoc}
    */
   public void save(ChangesLogWrapper logWrapper) throws RepositoryException
   {
      save(logWrapper, txResourceManager);
   }

   /**
    * {@inheritDoc}
    */
   void save(final ChangesLogWrapper logWrapper, TransactionableResourceManager txResourceManager)
      throws RepositoryException
   {
      final ItemStateChangesLog changesLog = logWrapper.optimizeAndGetChangesLog();
      final ChangesLogPersister persister = new ChangesLogPersister();

      // whole log will be reconstructed with persisted data 
      ItemStateChangesLog persistedLog;
      boolean failed = true;
      ConnectionMode mode = getMode(txResourceManager);
      try
      {
         persister.init(mode, txResourceManager);
         if (changesLog instanceof TransactionChangesLog)
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
         // replace log with persisted data only
         logWrapper.setLog(persistedLog);
         persister.prepare();
         notifySaveItems(persistedLog, true);
         onCommit(persister, mode, txResourceManager);
         failed = false;
      }
      catch (IOException e)
      {
         throw new RepositoryException("Save error", e);
      }
      finally
      {
         persister.clear();
         if (failed)
         {
            persister.rollback();
         }
      }
   }

   /**
    * @return the current tx mode
    */
   private ConnectionMode getMode(TransactionableResourceManager txResourceManager)
   {
      if (txResourceManager != null && txResourceManager.isGlobalTxActive())
      {
         return ConnectionMode.GLOBAL_TX;
      }
      else if (transactionManager != null)
      {
         return ConnectionMode.WITH_TRANSACTION_MANAGER;
      }
      return ConnectionMode.NORMAL;
   }

   /**
    * @param persister
    * @throws RepositoryException
    */
   private void onCommit(final ChangesLogPersister persister, ConnectionMode mode,
      TransactionableResourceManager txResourceManager) throws RepositoryException
   {
      if (mode == ConnectionMode.NORMAL)
      {
         // The commit is done normally
         persister.commit();
      }
      else if (mode == ConnectionMode.WITH_TRANSACTION_MANAGER)
      {
         try
         {
            transactionManager.getTransaction().registerSynchronization(new Synchronization()
            {

               public void beforeCompletion()
               {
               }

               public void afterCompletion(int status)
               {
                  switch (status)
                  {
                     case Status.STATUS_COMMITTED :
                        try
                        {
                           persister.commit();
                        }
                        catch (Exception e)
                        {
                           throw new RuntimeException("Could not commit the transaction", e);
                        }
                        break;
                     case Status.STATUS_UNKNOWN :
                        LOG.warn("Status UNKNOWN received in afterCompletion method, some data could have been corrupted !!");
                     case Status.STATUS_MARKED_ROLLBACK :
                     case Status.STATUS_ROLLEDBACK :
                        try
                        {
                           persister.rollback();
                        }
                        catch (Exception e)
                        {
                           LOG.error("Could not roll back the transaction", e);
                        }
                        break;

                     default :
                        throw new IllegalStateException("illegal status: " + status);
                  }
               }
            });
         }
         catch (Exception e)
         {
            throw new RepositoryException("Cannot register the synchronization for a late commit", e);
         }
      }
      else if (mode == ConnectionMode.GLOBAL_TX)
      {
         // The commit or rollback will be done by callback once the tx will be completed since it could
         // fail later in the tx
         txResourceManager.addListener(new TransactionableResourceManagerListener()
         {

            public void onCommit(boolean onePhase) throws Exception
            {
               persister.commit();
            }

            public void onAfterCompletion(int status) throws Exception
            {
            }

            public void onAbort() throws Exception
            {
               persister.rollback();
            }
         });
         // We share the system connection to prevent deadlocks
         if (persister.systemConnectionShared == null && persister.systemConnection != null)
         {
            txResourceManager.putSharedObject(ChangesLogPersister.class.getName(), persister.systemConnection);
         }
      }
   }

   private enum ConnectionMode {
      NORMAL, WITH_TRANSACTION_MANAGER, GLOBAL_TX
   }

   class ChangesLogPersister
   {

      private final Set<QPath> addedNodes = new HashSet<QPath>();

      private WorkspaceStorageConnection thisConnection;

      private WorkspaceStorageConnection systemConnectionShared;

      private WorkspaceStorageConnection systemConnection;

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

         notifyCommit();
      }

      public void init(ConnectionMode mode, TransactionableResourceManager txResourceManager) throws RepositoryException
      {
         if (mode == ConnectionMode.GLOBAL_TX)
         {
            // We are in a global tx so we should check if there is a shared system connection
            this.systemConnectionShared =
               txResourceManager.<WorkspaceStorageConnection> getSharedObject(ChangesLogPersister.class.getName());
         }
      }

      protected void prepare() throws IllegalStateException, RepositoryException
      {
         if (thisConnection != null && thisConnection.isOpened())
         {
            thisConnection.prepare();
         }
         if (systemConnection != null && !systemConnection.equals(thisConnection) && systemConnection.isOpened())
         {
            systemConnection.prepare();
         }
      }

      protected void clear()
      {
         // help to GC
         addedNodes.clear();
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

         notifyRollback();
      }

      protected WorkspaceStorageConnection getSystemConnection() throws RepositoryException
      {
         if (systemConnection != null)
         {
            // system connection opened - use it
            return systemConnection;
         }
         if (systemConnectionShared != null)
         {
            // A shared system connection exists so we use it
            return systemConnection = systemConnectionShared;
         }
         // we need a system connection but it does not exist
         return systemConnection = (systemDataContainer != dataContainer // NOSONAR
               // if it's different container instances
               ? systemDataContainer.equals(dataContainer) && thisConnection != null
               // but container configurations are same and non-system connection open
               // reuse this connection as system
                  ? systemDataContainer.reuseConnection(thisConnection)
                  // or open one new system
                  : systemDataContainer.openConnection(false)
               // else if it's same container instances (system and this)
               : thisConnection == null
               // and non-system connection doens't exist - open it
                  ? thisConnection = dataContainer.openConnection(false)
                  // if already open - use it
                  : thisConnection);
      }

      protected WorkspaceStorageConnection getThisConnection() throws RepositoryException
      {
         if (thisConnection != null)
         {
         // this connection opened - use it
            return thisConnection;
         }
         if (systemConnectionShared != null && systemDataContainer.isSame(dataContainer))
         {
            // The system data container and the data container are the same
            // and a shared system connection exists so we use it
            return thisConnection = dataContainer.reuseConnection(systemConnectionShared);
         }
         // we need this container connection
         return thisConnection = (systemDataContainer != dataContainer // NOSONAR
               // if it's different container instances
               ? dataContainer.equals(systemDataContainer) && systemConnection != null
               // but container configurations are same and system connection open
               // reuse system connection as this
                  ? dataContainer.reuseConnection(systemConnection)
                  // or open one new
                  : dataContainer.openConnection(false)
               // else if it's same container instances (system and this)
               : systemConnection == null
               // and system connection doens't exist - open it
                  ? systemConnection = dataContainer.openConnection(false)
                  // if already open - use it
                  : systemConnection);
      }

      protected PlainChangesLog save(PlainChangesLog changesLog) throws InvalidItemStateException, RepositoryException,
         IOException
      {
         // copy state
         PlainChangesLog newLog = FastAddPlainChangesLog.getInstance(changesLog);
         List<ItemState> states = changesLog.getAllStates();
         for (int j = 0, length = states.size(); j < length; j++)
         {
            ItemState prevState = states.get(j);
            ItemData newData;

            ChangedSizeHandler sizeHandler = initChangedSizeHandler(newLog, prevState);

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
                     new PersistedNodeData(prevData.getIdentifier(), prevData.getQPath(),
                        prevData.getParentIdentifier(), prevData.getPersistedVersion() + 1, prevData.getOrderNumber(),
                        prevData.getPrimaryTypeName(), prevData.getMixinTypeNames(), prevData.getACL());
               }
               else
               {
                  PropertyData prevData = (PropertyData)prevState.getData();

                  if (!prevState.isDeleted())
                  {
                     List<ValueData> values = new ArrayList<ValueData>();

                     for (int i = 0; i < prevData.getValues().size(); i++)
                     {
                        ValueData vd = prevData.getValues().get(i);

                        if (vd instanceof TransientValueData)
                        {
                           TransientValueData tvd = (TransientValueData)vd;

                           PersistedValueData pvd = tvd.createPersistedCopy(i);
                           tvd.delegate((AbstractValueData)pvd);

                           values.add(pvd);
                        }
                        else
                        {
                           values.add(vd);
                        }
                     }

                     newData =
                        new PersistedPropertyData(prevData.getIdentifier(), prevData.getQPath(),
                           prevData.getParentIdentifier(), prevData.getPersistedVersion() + 1, prevData.getType(),
                           prevData.isMultiValued(), values, new DelegatedPersistedSize(sizeHandler));
                  }
                  else
                  {
                     newData =
                        new PersistedPropertyData(prevData.getIdentifier(), prevData.getQPath(),
                           prevData.getParentIdentifier(), prevData.getPersistedVersion() + 1, prevData.getType(),
                           prevData.isMultiValued(), null, new DelegatedPersistedSize(sizeHandler));
                  }
               }
            }

            ItemState itemState =
               new ItemState(newData, prevState.getState(), prevState.isEventFire(), prevState.getAncestorToSave(),
                  prevState.isInternallyCreated(), prevState.isPersisted(), prevState.getOldPath(), sizeHandler);

            newLog.add(itemState);

            // save state
            if (itemState.isPersisted())
            {
               long start = 0;
               if (LOG.isDebugEnabled())
               {
                  start = System.currentTimeMillis();
               }
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
                  doAdd(data, conn, addedNodes, sizeHandler);
               }
               else if (itemState.isUpdated())
               {
                  doUpdate(data, conn, sizeHandler);
               }
               else if (itemState.isDeleted())
               {
                  doDelete(data, conn, sizeHandler);
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

      /**
       * Initialize appropriate {@link ChangedSizeHandler} instance. Should not exists for Node. If
       * current state already contains {@link ChangedSizeHandler}, it'll be returned as is. 
       * Special logic is implemented for none persisted states by trying to find in new changes log 
       * the last related state and refer on it. Otherwise {@link SimpleChangedSizeHandler} will be returned.
       */
      private ChangedSizeHandler initChangedSizeHandler(PlainChangesLog newLog, ItemState state)
      {
         if (state.getData().isNode())
         {
            return null;
         }
         else if (state.getChangedSizeHandler() != null)
         {
            return state.getChangedSizeHandler();
         }
         else if (!state.isPersisted() && (state.isDeleted() || state.isRenamed()))
         {
            List<ItemState> states = newLog.getAllStates();

            for (int i = states.size() - 1; i >= 0; i--)
            {
               ItemState newState = states.get(i);
               if (newState.getData().getIdentifier().equals(state.getData().getIdentifier()))
               {
                  return new OppositeChangedSizeHandler(newState.getChangedSizeHandler());
               }
            }

            return new SimpleChangedSizeHandler();
         }
         else
         {
            return new SimpleChangedSizeHandler();
         }
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
               {
                  refProps.add(ref);
               }
            }
            else
            {
               refProps.add(ref);
            }
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
   public boolean getChildNodesDataByPage(final NodeData nodeData, int fromOrderNum, int toOrderNum,
      List<NodeData> childNodes) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildNodesDataByPage(nodeData, fromOrderNum, toOrderNum, childNodes);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(final NodeData nodeData, List<QPathEntryFilter> patternFilters)
      throws RepositoryException
   {

      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildNodesData(nodeData, patternFilters);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public int getLastOrderNumber(final NodeData nodeData) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getLastOrderNumber(nodeData);
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
   public List<PropertyData> getChildPropertiesData(final NodeData nodeData,
      final List<QPathEntryFilter> itemDataFilters) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getChildPropertiesData(nodeData, itemDataFilters);
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
            if (dataContainer.isCheckSNSNewConnection())
            {
               final WorkspaceStorageConnection acon = dataContainer.openConnection();
               try
               {
                  checkPersistedSNS(node, acon);
               }
               finally
               {
                  acon.close();
               }
            }
            else
            {
               checkPersistedSNS(node, con);
            }
         }
      }
   }

   /**
    * Check if same-name sibling exists in persistence.
    */
   private void checkPersistedSNS(NodeData node, WorkspaceStorageConnection acon) throws RepositoryException
   {
      NodeData parent = (NodeData)acon.getItemData(node.getParentIdentifier());
      QPathEntry myName = node.getQPath().getEntries()[node.getQPath().getEntries().length - 1];
      ItemData sibling =
         acon.getItemData(parent, new QPathEntry(myName.getNamespace(), myName.getName(), myName.getIndex() - 1),
            ItemType.NODE);

      if (sibling == null || !sibling.isNode())
      {
         throw new InvalidItemStateException("Node can't be saved " + node.getQPath().getAsString()
            + ". No same-name sibling exists with index " + (myName.getIndex() - 1) + ".");
      }
   }

   /**
    * Performs actual item data deleting.
    * 
    * @param item
    *          to delete
    * @param con
    * @param sizeHandler
    * @throws RepositoryException
    * @throws InvalidItemStateException
    *           if the item is already deleted
    */
   protected void doDelete(final ItemData item, final WorkspaceStorageConnection con, ChangedSizeHandler sizeHandler)
      throws RepositoryException, InvalidItemStateException
   {

      if (item.isNode())
      {
         con.delete((NodeData)item);
      }
      else
      {
         con.delete((PropertyData)item, sizeHandler);
      }
   }

   /**
    * Performs actual item data updating.
    * 
    * @param item
    *          to update
    * @param con
    *          connection
    * @param sizeHandler
    * @throws RepositoryException
    * @throws InvalidItemStateException
    *           if the item not found
    */
   protected void doUpdate(final ItemData item, final WorkspaceStorageConnection con, ChangedSizeHandler sizeHandler)
      throws RepositoryException, InvalidItemStateException
   {

      if (item.isNode())
      {
         con.update((NodeData)item);
      }
      else
      {
         con.update((PropertyData)item, sizeHandler);
      }
   }

   /**
    * Performs actual item data adding.
    * 
    * @param item
    *          to add
    * @param con
    *          connection
    * @param sizeHandler
    *          accumulates size changing
    * @throws RepositoryException
    * @throws InvalidItemStateException
    *           if the item is already added
    */
   protected void doAdd(final ItemData item, final WorkspaceStorageConnection con, final Set<QPath> addedNodes,
      ChangedSizeHandler sizeHandler) throws RepositoryException, InvalidItemStateException
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
         con.add((PropertyData)item, sizeHandler);
      }
   }

   /**
    * Perform node rename.
    * 
    * @param item
    * @param con
    * @param addedNodes
    * @param delegated
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
    * {@inheritDoc}
    */
   public void addItemPersistenceListener(ItemsPersistenceListener listener)
   {
      if (listener instanceof ExtendedMandatoryItemsPersistenceListener)
      {
         extendedMandatoryListeners.add((ExtendedMandatoryItemsPersistenceListener)listener);
      }
      else if (listener instanceof MandatoryItemsPersistenceListener)
      {
         mandatoryListeners.add((MandatoryItemsPersistenceListener)listener);
      }
      else
      {
         listeners.add(listener);
      }
      if (LOG.isDebugEnabled())
      {
         LOG.debug("Workspace '" + this.dataContainer.getName() + "' listener registered: " + listener);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeItemPersistenceListener(ItemsPersistenceListener listener)
   {
      if (listener instanceof ExtendedMandatoryItemsPersistenceListener)
      {
         extendedMandatoryListeners.remove(listener);
      }
      else if (listener instanceof MandatoryItemsPersistenceListener)
      {
         mandatoryListeners.remove(listener);
      }
      else
      {
         listeners.remove(listener);
      }

      if (LOG.isDebugEnabled())
      {
         LOG.debug("Workspace '" + this.dataContainer.getName() + "' listener unregistered: " + listener);
      }
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

      for (ExtendedMandatoryItemsPersistenceListener mlistener : extendedMandatoryListeners)
      {
         if (mlistener.isTXAware() == isListenerTXAware)
         {
            mlistener.onSaveItems(changesLog);
         }
      }

      for (ItemsPersistenceListener listener : listeners)
      {
         if (listener.isTXAware() == isListenerTXAware)
         {
            listener.onSaveItems(changesLog);
         }
      }

   }

   /**
    * Notify listeners when changes is committed.
    */
   protected void notifyCommit()
   {
      for (ExtendedMandatoryItemsPersistenceListener listener : extendedMandatoryListeners)
      {
         listener.onCommit();
      }
   }

   /**
    * Notify listeners when changes is rollbacked.
    */
   protected void notifyRollback()
   {
      for (ExtendedMandatoryItemsPersistenceListener listener : extendedMandatoryListeners)
      {
         listener.onRollback();
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
   public ItemData getItemData(final NodeData parentData, final QPathEntry name, ItemType itemType)
      throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getItemData(parentData, name, itemType);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasItemData(final NodeData parentData, final QPathEntry name, ItemType itemType)
      throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.hasItemData(parentData, name, itemType);
      }
      finally
      {
         con.close();
      }
   }

   /**
    * Returns workspace data size. See for details
    * {@link WorkspaceQuotaManagerImpl#getWorkspaceDataSize()}.
    *
    * @throws RepositoryException
    *          if any exception is occurred
    */
   public long getWorkspaceDataSize() throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getWorkspaceDataSize();
      }
      finally
      {
         con.close();
      }
   }

   /**
    * Returns node data size. See for details
    * {@link WorkspaceQuotaManagerImpl#getNodeDataSize(String)}.
    *
    * @param nodeIdentifier
    *          node identifier which size need to calculate
    * @throws RepositoryException
    *          if any exception is occurred
    */
   public long getNodeDataSize(String nodeIdentifier) throws RepositoryException
   {
      final WorkspaceStorageConnection con = dataContainer.openConnection();
      try
      {
         return con.getNodeDataSize(nodeIdentifier);
      }
      finally
      {
         con.close();
      }
   }
}