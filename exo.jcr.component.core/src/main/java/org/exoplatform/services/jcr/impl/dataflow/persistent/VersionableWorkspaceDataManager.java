/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.CompositeChangesLog;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NullItemData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;
import org.exoplatform.services.jcr.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. Responsible for: *redirecting repository operations if item is
 * descendant of /jcr:system/jcr:versionStorage *adding version history for newly added/assigned
 * mix:versionable
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id$ 
 */

public class VersionableWorkspaceDataManager extends ShareableSupportedWorkspaceDataManager
{
   private ShareableSupportedWorkspaceDataManager versionDataManager;
   
   /**
    * The resource manager
    */
   private final TransactionableResourceManager txResourceManager;

   public VersionableWorkspaceDataManager(CacheableWorkspaceDataManager persistentManager,
      TransactionableResourceManager txResourceManager)
   {
      super(persistentManager);
      this.txResourceManager = txResourceManager;
   }

   /**
    * Called by WorkspaceContainer after repository initialization.
    */
   public void setSystemDataManager(DataManager systemDataManager)
   {
      this.versionDataManager = (ShareableSupportedWorkspaceDataManager)systemDataManager;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(final NodeData nodeData) throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(nodeData.getQPath()))
      {
         return versionDataManager.getChildNodesData(nodeData);
      }
      return super.getChildNodesData(nodeData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean getChildNodesDataByPage(NodeData nodeData, int fromOrderNum, int offset,int pageSize, List<NodeData> childs)
      throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(nodeData.getQPath()))
      {
         return versionDataManager.getChildNodesDataByPage(nodeData, fromOrderNum, offset, pageSize, childs);
      }
      return super.getChildNodesDataByPage(nodeData, fromOrderNum, offset, pageSize, childs);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(final NodeData nodeData, final List<QPathEntryFilter> patternFilters)
      throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(nodeData.getQPath()))
      {
         return versionDataManager.getChildNodesData(nodeData, patternFilters);
      }
      return super.getChildNodesData(nodeData, patternFilters);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getChildNodesCount(final NodeData parent) throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(parent.getQPath()))
      {
         return versionDataManager.getChildNodesCount(parent);
      }
      return super.getChildNodesCount(parent);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getLastOrderNumber(final NodeData parent) throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(parent.getQPath()))
      {
         return versionDataManager.getLastOrderNumber(parent);
      }
      return super.getLastOrderNumber(parent);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(final NodeData nodeData) throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(nodeData.getQPath()))
      {
         return versionDataManager.getChildPropertiesData(nodeData);
      }
      return super.getChildPropertiesData(nodeData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(final NodeData nodeData,
      final List<QPathEntryFilter> itemDataFilters) throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(nodeData.getQPath()))
      {
         return versionDataManager.getChildPropertiesData(nodeData, itemDataFilters);
      }
      return super.getChildPropertiesData(nodeData, itemDataFilters);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> listChildPropertiesData(final NodeData nodeData) throws RepositoryException
   {
      if (!this.equals(versionDataManager) && isSystemDescendant(nodeData.getQPath()))
      {
         return versionDataManager.listChildPropertiesData(nodeData);
      }
      return super.listChildPropertiesData(nodeData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException
   {
      return getItemData(parentData, name, itemType, true);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasItemData(NodeData parentData, QPathEntry name, ItemType itemType)
      throws RepositoryException
   {
      if (parentData != null)
      {
         final QPath ipath = QPath.makeChildPath(parentData.getQPath(), name);
         if (!this.equals(versionDataManager) && isSystemDescendant(ipath))
         {
            return versionDataManager.hasItemData(parentData, name, itemType);
         }
      }
      return super.hasItemData(parentData, name, itemType);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType, boolean createNullItemData)
      throws RepositoryException
   {
      if (parentData != null)
      {
         final QPath ipath = QPath.makeChildPath(parentData.getQPath(), name);
         if (!this.equals(versionDataManager) && isSystemDescendant(ipath))
         {
            return versionDataManager.getItemData(parentData, name, itemType, createNullItemData);
         }
      }
      return super.getItemData(parentData, name, itemType, createNullItemData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      // from cache at first
      ItemData cdata = persistentManager.getCachedItemData(identifier);
      if (cdata != null && !(cdata instanceof NullItemData))
      {
         return super.getItemData(identifier);
      }

      if (!this.equals(versionDataManager) && !identifier.equals(Constants.ROOT_UUID))
      {
         // search in System cache for /jcr:system nodes only
         cdata = versionDataManager.persistentManager.getCachedItemData(identifier);
         if (cdata != null && !(cdata instanceof NullItemData))
         {
            if (isSystemDescendant(cdata.getQPath()))
            {
               return versionDataManager.getItemData(identifier);
            }
            else
            {
               return null;
            }
         }
      }

      // then from persistence
      ItemData data = super.getItemData(identifier);
      if (data != null)
      {
         return data;
      }

      else if (!this.equals(versionDataManager))
      {
         // try from version storage if not the same
         data = versionDataManager.getItemData(identifier);
         if (data != null && isSystemDescendant(data.getQPath()))
         {
            return data;
         }
      }
      return null;
   }

   public void save(CompositeChangesLog changesLog) throws RepositoryException, InvalidItemStateException
   {
      save(changesLog, false);
   }

   public void save(CompositeChangesLog changesLog, boolean versionLogsFirst) throws RepositoryException, InvalidItemStateException
   {

      final ChangesLogIterator logIterator = changesLog.getLogIterator();

      final TransactionChangesLog versionLogs = new TransactionChangesLog();
      final TransactionChangesLog nonVersionLogs = new TransactionChangesLog();

      while (logIterator.hasNextLog())
      {
         List<ItemState> vstates = new ArrayList<ItemState>();
         List<ItemState> nvstates = new ArrayList<ItemState>();

         PlainChangesLog changes = logIterator.nextLog();
         if (this.equals(versionDataManager))
         {
            nvstates.addAll(changes.getAllStates());
         }
         else
         {
            for (ItemState change : changes.getAllStates())
            {
               if (isSystemDescendant(change.getData().getQPath()))
               {
                  vstates.add(change);
               }
               else
               {
                  nvstates.add(change);
               }
            }
         }

         if (!vstates.isEmpty())
         {
            if (!nvstates.isEmpty())
            {
               // we have pair of logs for system and non-system (this) workspaces
               final String pairId = IdGenerator.generate();

               versionLogs.addLog(PlainChangesLogImpl.createCopy(vstates, pairId, changes));
               nonVersionLogs.addLog(PlainChangesLogImpl.createCopy(nvstates, pairId, changes));
            }
            else
            {
               versionLogs.addLog(PlainChangesLogImpl.createCopy(vstates, changes));
               nonVersionLogs.addLog(PlainChangesLogImpl.createCopy(nvstates, changes));
            }
         }
         else if (!nvstates.isEmpty())
         {
            nonVersionLogs.addLog(PlainChangesLogImpl.createCopy(nvstates, changes));
         }
      }

      if (versionLogsFirst && versionLogs.getSize() > 0)
      {
         versionDataManager.save(versionLogs, txResourceManager);
      }

      if (nonVersionLogs.getSize() > 0)
      {
         super.save(nonVersionLogs);
      }

      if (!versionLogsFirst && versionLogs.getSize() > 0)
      {
         versionDataManager.save(versionLogs, txResourceManager);
      }
   }

   private boolean isSystemDescendant(QPath path)
   {
      return path.isDescendantOf(Constants.JCR_SYSTEM_PATH) || path.equals(Constants.JCR_SYSTEM_PATH);
   }

}
