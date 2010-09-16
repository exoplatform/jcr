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
package org.exoplatform.services.jcr.impl.dataflow.session;

import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.SharedDataManager;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.ItemImpl.ItemType;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LocalWorkspaceDataManagerStub;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.TransactionException;
import org.exoplatform.services.transaction.TransactionResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: TransactionableDataManager.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TransactionableDataManager implements TransactionResource, DataManager
{

   // use LocalWorkspaceDataManagerStub, otherwise JVM will use save(ItemStateChangesLog changes) instead of
   // VersionableWorkspaceDataManager.save(CompositeChangesLog changesLog) 
   private LocalWorkspaceDataManagerStub storageDataManager;

   protected static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.TransactionableDataManager");

   private TransactionChangesLog transactionLog;

   public TransactionableDataManager(LocalWorkspaceDataManagerStub dataManager, SessionImpl session)
      throws RepositoryException
   {
      super();
      this.storageDataManager = dataManager;

      // TODO EXOJCR-272
      //      try
      //      {
      //         this.storageDataManager = new LocalWorkspaceStorageDataManagerProxy(dataManager, session.getValueFactory());
      //      }
      //      catch (Exception e1)
      //      {
      //         String infoString = "[Error of read value factory: " + e1.getMessage() + "]";
      //         throw new RepositoryException(infoString);
      //      }
   }

   // --------------- ItemDataConsumer --------

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException
   {
      List<NodeData> nodes = storageDataManager.getChildNodesData(parent);

      if (txStarted())
      {
         // merge data
         List<ItemState> txChanges = transactionLog.getChildrenChanges(parent.getIdentifier(), true);
         if (txChanges.size() > 0)
         {
            List<NodeData> res = new ArrayList<NodeData>(nodes);

            for (ItemState state : txChanges)
            {
               res.remove(state.getData());
               if (!state.isDeleted())
               {
                  res.add((NodeData)state.getData());
               }
            }

            return Collections.unmodifiableList(res);
         }
      }

      return nodes;
   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(final NodeData parent) throws RepositoryException
   {
      if (txStarted())
      {
         int txChildsCount = 0;
         for (ItemState change : transactionLog.getAllStates())
         {
            if (change.isNode() && change.isPersisted()
               && change.getData().getParentIdentifier().equals(parent.getIdentifier()))
            {
               if (change.isDeleted())
               {
                  txChildsCount--;
               }
               else if (change.isAdded())
               {
                  txChildsCount++;
               }
            }
         }

         final int childsCount = storageDataManager.getChildNodesCount(parent) + txChildsCount;
         if (childsCount < 0)
         {
            throw new InvalidItemStateException("Node's child nodes were changed in another Transaction "
               + parent.getQPath().getAsString());
         }

         return childsCount;
      }
      else
      {
         return storageDataManager.getChildNodesCount(parent);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException
   {
      List<PropertyData> props = storageDataManager.getChildPropertiesData(parent);

      if (txStarted())
      {
         // merge data
         List<ItemState> txChanges = transactionLog.getChildrenChanges(parent.getIdentifier(), false);
         if (txChanges.size() > 0)
         {
            List<PropertyData> res = new ArrayList<PropertyData>(props);
            for (ItemState state : txChanges)
            {
               res.remove(state.getData());
               if (!state.isDeleted())
               {
                  res.add((PropertyData)state.getData());
               }
            }

            return Collections.unmodifiableList(res);
         }
      }

      return props;
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException
   {
      List<PropertyData> props = storageDataManager.listChildPropertiesData(parent);

      if (txStarted())
      {
         // merge data
         List<ItemState> txChanges = transactionLog.getChildrenChanges(parent.getIdentifier(), false);
         if (txChanges.size() > 0)
         {
            List<PropertyData> res = new ArrayList<PropertyData>(props);
            for (ItemState state : txChanges)
            {
               res.remove(state.getData());
               if (!state.isDeleted())
               {
                  res.add((PropertyData)state.getData());
               }
            }

            return Collections.unmodifiableList(res);
         }
      }

      return props;
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException
   {
      ItemData data = null;
      if (txStarted())
      {
         ItemState state = transactionLog.getItemState(parentData, name, itemType);
         if (state != null)
         {
            data = state.getData();
         }
      }
      if (data != null)
      {
         return data;
      }
      else
      {
         return storageDataManager.getItemData(parentData, name, itemType);
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      ItemData data = null;
      if (txStarted())
      {
         ItemState state = transactionLog.getItemState(identifier);
         if (state != null)
         {
            data = state.getData();
         }
      }
      if (data != null)
      {
         return data;
      }
      else
      {
         return storageDataManager.getItemData(identifier);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      return storageDataManager.getReferencesData(identifier, skipVersionStorage);
   }

   // --------------- --------

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("tx start() " + this + " txStarted(): " + txStarted());
      }

      if (!txStarted())
      {
         transactionLog = new TransactionChangesLog();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws TransactionException
   {
      if (txStarted())
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("tx commit() " + this + "\n" + transactionLog.dump());
         }

         try
         {
            TransactionChangesLog tl = transactionLog;
            transactionLog = null;
            storageDataManager.save(tl);
         }
         catch (InvalidItemStateException e)
         {
            throw new TransactionException(e.getMessage(), e);
         }
         catch (RepositoryException e)
         {
            throw new TransactionException(e.getMessage(), e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rollback()
   {
      if (LOG.isDebugEnabled())
      {
         LOG.debug("tx rollback() " + this + (transactionLog != null ? "\n" + transactionLog.dump() : "[NULL]"));
      }

      if (txStarted())
      {
         transactionLog = null;
      }
   }

   /**
    * Updates the manager with new changes. If transaction is started it will fill manager's changes
    * log, else just move changes to workspace storage manager. It saves the changes AS IS - i.e. id
    * DOES NOT care about cloning of this objects etc. Here PlainChangesLog expected.
    * 
    * @param changes
    * @throws RepositoryException
    */
   public void save(ItemStateChangesLog changes) throws RepositoryException
   {

      PlainChangesLog statesLog = (PlainChangesLog)changes;

      if (LOG.isDebugEnabled())
      {
         LOG.debug("save() " + this + " txStarted: " + txStarted() + "\n====== Changes ======\n"
            + (statesLog != null ? "\n" + statesLog.dump() : "[NULL]") + "=====================");
      }

      if (txStarted())
      {
         transactionLog.addLog(statesLog);
      }
      else
      {
         storageDataManager.save(new TransactionChangesLog(statesLog));
      }

   }

   public boolean txStarted()
   {
      return transactionLog != null;
   }

   public boolean txHasPendingChages()
   {
      return txStarted() && transactionLog.getSize() > 0;
   }

   public SharedDataManager getStorageDataManager()
   {
      return storageDataManager;
   }

}
