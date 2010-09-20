/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.statistics;

import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.statistics.JCRStatisticsManager;
import org.exoplatform.services.jcr.statistics.Statistics;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * This class is used to give statistics about the time spent in the database access layer.  
 * To activate the statistics, set the JVM parameter called
 * "JDBCWorkspaceDataContainer.statistics.enabled" to <code>true</code>.
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 26 f�vr. 2010  
 */
public class StatisticsJDBCStorageConnection implements WorkspaceStorageConnection
{

   /**
    * The description of the statistics corresponding to the method 
    * <code>update(PropertyData data)</code>
    */
   private static final String UPDATE_PROPERTY_DATA_DESCR = "updatePropertyData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>update(NodeData data)</code>
    */
   private static final String UPDATE_NODE_DATA_DESCR = "updateNodeData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>rollback()</code>
    */
   private static final String ROLLBACK_DESCR = "rollback";

   /**
    * The description of the statistics corresponding to the method 
    * <code>rename(NodeData data)</code>
    */
   private static final String RENAME_NODE_DATA_DESCR = "renameNodeData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>listChildPropertiesData(NodeData parent)</code>
    */
   private static final String LIST_CHILD_PROPERTIES_DATA_DESCR = "listChildPropertiesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>isOpened()</code>
    */
   private static final String IS_OPENED_DESCR = "isOpened";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getReferencesData(String nodeIdentifier)</code>
    */
   private static final String GET_REFERENCES_DATA_DESCR = "getReferencesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getItemData(String identifier)</code>
    */
   private static final String GET_ITEM_DATA_BY_ID_DESCR = "getItemDataById";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getItemData(NodeData parentData, QPathEntry name)</code>
    */
   private static final String GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR = "getItemDataByNodeDataNQPathEntry";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getChildPropertiesData(NodeData parent)</code>
    */
   private static final String GET_CHILD_PROPERTIES_DATA_DESCR = "getChildPropertiesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getChildNodesData(NodeData parent)</code>
    */
   private static final String GET_CHILD_NODES_DATA_DESCR = "getChildNodesData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>getChildNodesCount(NodeData parent)</code>
    */
   private static final String GET_CHILD_NODES_COUNT_DESCR = "getChildNodesCount";

   /**
    * The description of the statistics corresponding to the method 
    * <code>delete(PropertyData data)</code>
    */
   private static final String DELETE_PROPERTY_DATA_DESCR = "deletePropertyData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>delete(NodeData data)</code>
    */
   private static final String DELETE_NODE_DATA_DESCR = "deleteNodeData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>commit()</code>
    */
   private static final String COMMIT_DESCR = "commit";

   /**
    * The description of the statistics corresponding to the method 
    * <code>close()</code>
    */
   private static final String CLOSE_DESCR = "close";

   /**
    * The description of the statistics corresponding to the method 
    * <code>add(PropertyData data)</code>
    */
   private static final String ADD_PROPERTY_DATA_DESCR = "addPropertyData";

   /**
    * The description of the statistics corresponding to the method 
    * <code>add(NodeData data)</code>
    */
   private static final String ADD_NODE_DATA_DESCR = "addNodeData";

   /**
    * The global statistics for all the database accesses
    */
   private final static Statistics GLOBAL_STATISTICS = new Statistics(null, "global");

   /**
    * The list of all the statistics, one per method
    */
   private final static Map<String, Statistics> ALL_STATISTICS = new LinkedHashMap<String, Statistics>();
   static
   {
      // Read Methods
      ALL_STATISTICS.put(GET_ITEM_DATA_BY_ID_DESCR, new Statistics(GLOBAL_STATISTICS, GET_ITEM_DATA_BY_ID_DESCR));
      ALL_STATISTICS.put(GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR, new Statistics(GLOBAL_STATISTICS,
         GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR));
      ALL_STATISTICS.put(GET_CHILD_NODES_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, GET_CHILD_NODES_DATA_DESCR));
      ALL_STATISTICS.put(GET_CHILD_NODES_COUNT_DESCR, new Statistics(GLOBAL_STATISTICS, GET_CHILD_NODES_COUNT_DESCR));
      ALL_STATISTICS.put(GET_CHILD_PROPERTIES_DATA_DESCR, new Statistics(GLOBAL_STATISTICS,
         GET_CHILD_PROPERTIES_DATA_DESCR));
      ALL_STATISTICS.put(LIST_CHILD_PROPERTIES_DATA_DESCR, new Statistics(GLOBAL_STATISTICS,
         LIST_CHILD_PROPERTIES_DATA_DESCR));
      ALL_STATISTICS.put(GET_REFERENCES_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, GET_REFERENCES_DATA_DESCR));
      // Write Methods
      // Commit
      ALL_STATISTICS.put(COMMIT_DESCR, new Statistics(GLOBAL_STATISTICS, COMMIT_DESCR));
      // Add methods
      ALL_STATISTICS.put(ADD_NODE_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, ADD_NODE_DATA_DESCR));
      ALL_STATISTICS.put(ADD_PROPERTY_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, ADD_PROPERTY_DATA_DESCR));
      // Update methods
      ALL_STATISTICS.put(UPDATE_NODE_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, UPDATE_NODE_DATA_DESCR));
      ALL_STATISTICS.put(UPDATE_PROPERTY_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, UPDATE_PROPERTY_DATA_DESCR));
      // Delete methods
      ALL_STATISTICS.put(DELETE_NODE_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, DELETE_NODE_DATA_DESCR));
      ALL_STATISTICS.put(DELETE_PROPERTY_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, DELETE_PROPERTY_DATA_DESCR));
      // Rename
      ALL_STATISTICS.put(RENAME_NODE_DATA_DESCR, new Statistics(GLOBAL_STATISTICS, RENAME_NODE_DATA_DESCR));
      // Rollback
      ALL_STATISTICS.put(ROLLBACK_DESCR, new Statistics(GLOBAL_STATISTICS, ROLLBACK_DESCR));
      // Others
      ALL_STATISTICS.put(IS_OPENED_DESCR, new Statistics(null, IS_OPENED_DESCR));
      ALL_STATISTICS.put(CLOSE_DESCR, new Statistics(null, CLOSE_DESCR));
   }

   static
   {
      if (JDBCWorkspaceDataContainer.STATISTICS_ENABLED)
      {
         JCRStatisticsManager.registerStatistics("JDBCStorageConnection", GLOBAL_STATISTICS, ALL_STATISTICS);
      }
   }

   /**
    * The nested {@link WorkspaceStorageConnection}
    */
   private final WorkspaceStorageConnection wcs;

   /**
    * The default constructor
    */
   public StatisticsJDBCStorageConnection(WorkspaceStorageConnection wcs)
   {
      this.wcs = wcs;
   }

   /**
    * @return the nested {@link WorkspaceStorageConnection}
    */
   public WorkspaceStorageConnection getNestedWorkspaceStorageConnection()
   {
      return wcs;
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#add(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void add(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(ADD_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.add(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#add(org.exoplatform.services.jcr.datamodel.PropertyData)
    */
   public void add(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(ADD_PROPERTY_DATA_DESCR);
      try
      {
         s.begin();
         wcs.add(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#close()
    */
   public void close() throws IllegalStateException, RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(CLOSE_DESCR);
      try
      {
         s.begin();
         wcs.close();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#commit()
    */
   public void commit() throws IllegalStateException, RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(COMMIT_DESCR);
      try
      {
         s.begin();
         wcs.commit();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#delete(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(DELETE_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.delete(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#delete(org.exoplatform.services.jcr.datamodel.PropertyData)
    */
   public void delete(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(DELETE_PROPERTY_DATA_DESCR);
      try
      {
         s.begin();
         wcs.delete(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getChildNodesCount(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public int getChildNodesCount(NodeData parent) throws RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(GET_CHILD_NODES_COUNT_DESCR);
      try
      {
         s.begin();
         return wcs.getChildNodesCount(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getChildNodesData(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_CHILD_NODES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.getChildNodesData(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getChildPropertiesData(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_CHILD_PROPERTIES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.getChildPropertiesData(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
      IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_ITEM_DATA_BY_NODE_DATA_NQ_PATH_ENTRY_DESCR);
      try
      {
         s.begin();
         return wcs.getItemData(parentData, name, itemType);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getItemData(java.lang.String)
    */
   public ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(GET_ITEM_DATA_BY_ID_DESCR);
      try
      {
         s.begin();
         return wcs.getItemData(identifier);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#getReferencesData(java.lang.String)
    */
   public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException,
      IllegalStateException, UnsupportedOperationException
   {
      Statistics s = ALL_STATISTICS.get(GET_REFERENCES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.getReferencesData(nodeIdentifier);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#isOpened()
    */
   public boolean isOpened()
   {
      Statistics s = ALL_STATISTICS.get(IS_OPENED_DESCR);
      try
      {
         s.begin();
         return wcs.isOpened();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#listChildPropertiesData(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(LIST_CHILD_PROPERTIES_DATA_DESCR);
      try
      {
         s.begin();
         return wcs.listChildPropertiesData(parent);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#rename(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void rename(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(RENAME_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.rename(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#rollback()
    */
   public void rollback() throws IllegalStateException, RepositoryException
   {
      Statistics s = ALL_STATISTICS.get(ROLLBACK_DESCR);
      try
      {
         s.begin();
         wcs.rollback();
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#update(org.exoplatform.services.jcr.datamodel.NodeData)
    */
   public void update(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(UPDATE_NODE_DATA_DESCR);
      try
      {
         s.begin();
         wcs.update(data);
      }
      finally
      {
         s.end();
      }
   }

   /**
    * @see org.exoplatform.services.jcr.storage.WorkspaceStorageConnection#update(org.exoplatform.services.jcr.datamodel.PropertyData)
    */
   public void update(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      Statistics s = ALL_STATISTICS.get(UPDATE_PROPERTY_DATA_DESCR);
      try
      {
         s.begin();
         wcs.update(data);
      }
      finally
      {
         s.end();
      }
   }
}
