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

import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.SharedDataManager;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Calendar;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS. Data Manager supported ACL Inheritance
 * 
 * @author Gennady Azarenkov
 * @version $Id: ACLInheritanceSupportedWorkspaceDataManager.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class ACLInheritanceSupportedWorkspaceDataManager implements SharedDataManager
{

   private static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ACLInheritanceSupportedWorkspaceDataManager");

   protected final CacheableWorkspaceDataManager persistentManager;

   public ACLInheritanceSupportedWorkspaceDataManager(CacheableWorkspaceDataManager persistentManager)
   {
      this.persistentManager = persistentManager;
   }

   /**
    * {@inheritDoc}
    */
   // ------------ ItemDataConsumer impl ------------
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException
   {
      return persistentManager.getChildNodesData(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> patternFilters) throws RepositoryException
   {
      return persistentManager.getChildNodesData(parent, patternFilters);
   }

   /**
    * {@inheritDoc}
    */
   public boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int limit, List<NodeData> childs)
      throws RepositoryException
   {
      return persistentManager.getChildNodesDataByPage(parent, fromOrderNum, limit, childs);
   }

   /**
    * {@inheritDoc}
    */
   public int getLastOrderNumber(final NodeData parent) throws RepositoryException
   {
      return persistentManager.getLastOrderNumber(parent);
   }

   /**
    * {@inheritDoc}
    */
   public int getChildNodesCount(final NodeData parent) throws RepositoryException
   {
      return persistentManager.getChildNodesCount(parent);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parent, QPathEntry name) throws RepositoryException
   {
      return getItemData(parent, name, ItemType.UNKNOWN);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parent, QPathEntry name, ItemType itemType) throws RepositoryException
   {
      return persistentManager.getItemData(parent, name, itemType);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(String identifier) throws RepositoryException
   {
      return persistentManager.getItemData(identifier);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException
   {
      return persistentManager.getChildPropertiesData(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException
   {
      return persistentManager.getChildPropertiesData(parent, itemDataFilters);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException
   {
      return persistentManager.listChildPropertiesData(parent);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(String identifier, boolean skipVersionStorage)
      throws RepositoryException
   {
      return persistentManager.getReferencesData(identifier, skipVersionStorage);
   }

   // ------------ SharedDataManager ----------------------

   /**
    * {@inheritDoc}
    */
   public void save(ItemStateChangesLog changes) throws InvalidItemStateException, UnsupportedOperationException,
      RepositoryException
   {
      persistentManager.save(changes);
   }

   /**
    * {@inheritDoc}
    */
   public Calendar getCurrentTime()
   {
      return persistentManager.getCurrentTime();
   }
}
