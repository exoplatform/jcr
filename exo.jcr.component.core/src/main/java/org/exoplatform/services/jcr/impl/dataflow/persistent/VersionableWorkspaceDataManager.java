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
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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

   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.VersionableWorkspaceDataManager");

   private ShareableSupportedWorkspaceDataManager versionDataManager;

   public VersionableWorkspaceDataManager(CacheableWorkspaceDataManager persistentManager)
   {
      super(persistentManager);
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
      if (isSystemDescendant(nodeData.getQPath()) && !this.equals(versionDataManager))
      {
         return versionDataManager.getChildNodesData(nodeData);
      }
      return super.getChildNodesData(nodeData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean getChildNodesDataByPage(NodeData nodeData, int fromOrderNum, int limit, List<NodeData> childs)
      throws RepositoryException
   {
      if (isSystemDescendant(nodeData.getQPath()) && !this.equals(versionDataManager))
      {
         return versionDataManager.getChildNodesDataByPage(nodeData, fromOrderNum, limit, childs);
      }
      return super.getChildNodesDataByPage(nodeData, fromOrderNum, limit, childs);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(final NodeData nodeData, final List<QPathEntryFilter> patternFilters)
      throws RepositoryException
   {
      if (isSystemDescendant(nodeData.getQPath()) && !this.equals(versionDataManager))
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
      if (isSystemDescendant(parent.getQPath()) && !this.equals(versionDataManager))
      {
         return versionDataManager.getChildNodesCount(parent);
      }
      return super.getChildNodesCount(parent);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(final NodeData nodeData) throws RepositoryException
   {
      if (isSystemDescendant(nodeData.getQPath()) && !this.equals(versionDataManager))
      {
         return versionDataManager.getChildPropertiesData(nodeData);
      }
      return super.getChildPropertiesData(nodeData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(final NodeData nodeData, final List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException
   {
      if (isSystemDescendant(nodeData.getQPath()) && !this.equals(versionDataManager))
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
      if (isSystemDescendant(nodeData.getQPath()) && !this.equals(versionDataManager))
      {
         return versionDataManager.listChildPropertiesData(nodeData);
      }
      return super.listChildPropertiesData(nodeData);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public ItemData getItemData(NodeData parentData, QPathEntry name) throws RepositoryException
   {
      return getItemData(parentData, name, ItemType.UNKNOWN);
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
   public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType, boolean createNullItemData)
      throws RepositoryException
   {
      if (parentData != null)
      {
         final QPath ipath = QPath.makeChildPath(parentData.getQPath(), name);
         if (isSystemDescendant(ipath) && !this.equals(versionDataManager))
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

   public void save(final CompositeChangesLog changesLog) throws RepositoryException, InvalidItemStateException
   {

      final ChangesLogIterator logIterator = changesLog.getLogIterator();

      final TransactionChangesLog versionLogs = new TransactionChangesLog();
      final TransactionChangesLog nonVersionLogs = new TransactionChangesLog();

      while (logIterator.hasNextLog())
      {
         List<ItemState> vstates = new ArrayList<ItemState>();
         List<ItemState> nvstates = new ArrayList<ItemState>();

         PlainChangesLog changes = logIterator.nextLog();
         for (ItemState change : changes.getAllStates())
         {
            if (isSystemDescendant(change.getData().getQPath()) && !this.equals(versionDataManager))
            {
               vstates.add(change);
            }
            else
            {
               nvstates.add(change);
            }
         }

         if (vstates.size() > 0)
         {
            if (nvstates.size() > 0)
            {
               // we have pair of logs for system and non-system (this) workspaces
               final String pairId = IdGenerator.generate();

               versionLogs.addLog(new PlainChangesLogImpl(vstates, changes.getSessionId(), changes.getEventType(),
                  pairId));
               nonVersionLogs.addLog(new PlainChangesLogImpl(nvstates, changes.getSessionId(), changes.getEventType(),
                  pairId));
            }
            else
            {
               versionLogs.addLog(new PlainChangesLogImpl(vstates, changes.getSessionId(), changes.getEventType()));
            }
         }
         else if (nvstates.size() > 0)
         {
            nonVersionLogs.addLog(new PlainChangesLogImpl(nvstates, changes.getSessionId(), changes.getEventType()));
         }
      }

      if (versionLogs.getSize() > 0)
      {
         versionDataManager.save(versionLogs);
      }

      if (nonVersionLogs.getSize() > 0)
      {
         super.save(nonVersionLogs);
      }
   }

   private boolean isSystemDescendant(QPath path)
   {
      return path.isDescendantOf(Constants.JCR_SYSTEM_PATH) || path.equals(Constants.JCR_SYSTEM_PATH);
   }

}
