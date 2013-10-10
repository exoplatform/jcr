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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedPropertyType;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.IllegalACLException;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ACLHolder;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id$
 */
abstract public class CQJDBCStorageConnection extends JDBCStorageConnection
{

   /**
    * Connection logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.CQJDBCStorageConnection");

   protected static final String PATTERN_ESCAPE_STRING = "\\"; //valid for HSQL, Sybase, DB2, MSSQL, ORACLE
   protected static final String SINGLE_QUOTE_ESCAPE_PATTERN = "'"; //valid for HSQL, Sybase, DB2, MSSQL, ORACLE, PGSQL

   /**
    * FIND_NODES_BY_PARENTID NEW.
    */
   protected String FIND_NODES_BY_PARENTID_CQ;

   /**
    * FIND_PROPERTIES_BY_PARENTID NEW.
    */
   protected String FIND_PROPERTIES_BY_PARENTID_CQ;

   /**
    * FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ.
    */
   protected String FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ;

   /**
    * FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE.
    */
   protected String FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE;

   /**
    * FIND_NODES_BY_PARENTID_LAZILY.
    */
   protected String FIND_NODES_BY_PARENTID_LAZILY_CQ;

   /**
    * FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE.
    */
   protected String FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE;

   /**
    * FIND_ITEM_QPATH_BY_ID_CQ.
    */
   protected String FIND_ITEM_QPATH_BY_ID_CQ;

   /**
    * FIND_PROPERTY_BY_ID.
    */
   protected String FIND_PROPERTY_BY_ID;

   /**
    * DELETE_VALUE_BY_ORDER_NUM.
    */
   protected String DELETE_VALUE_BY_ORDER_NUM;

   /**
    * DELETE_REFERENCE_BY_ORDER_NUM.
    */
   protected String DELETE_REFERENCE_BY_ORDER_NUM;

   /**
    * UPDATE_VALUE.
    */
   protected String UPDATE_VALUE;

   /**
    * UPDATE_REFERENCE.
    */
   protected String UPDATE_REFERENCE;

   /**
    * FIND_ACL_HOLDERS.
    */
   protected String FIND_ACL_HOLDERS;

   protected PreparedStatement findACLHolders;

   protected PreparedStatement findNodesByParentIdCQ;

   protected PreparedStatement findPropertiesByParentIdCQ;

   protected PreparedStatement findNodeMainPropertiesByParentIdentifierCQ;

   protected PreparedStatement findItemQPathByIdentifierCQ;

   protected PreparedStatement findPropertyById;

   protected PreparedStatement findNodesByParentIdLazilyCQ;

   protected PreparedStatement deleteValueDataByOrderNum;
   
   protected PreparedStatement deleteReferenceByOrderNum;

   protected PreparedStatement updateReference;
   
   protected PreparedStatement updateValue;

   protected Statement findPropertiesByParentIdAndComplexPatternCQ;

   protected Statement findNodesByParentIdAndComplexPatternCQ;
   
   private int changeStatus;
   
   private int changeCount;
   
   private static final NodeData FAKE_NODE;
   static
   {
      try
      {
         FAKE_NODE = new TransientNodeData(QPath.parse("[]unknown"), "unknown", -1, null, null, -1, "unknwon", null);
      }
      catch (IllegalPathException e)
      {
         throw new RuntimeException(e);
      }
   }
   
   private static final PropertyData FAKE_PROPERTY;
   static
   {
      try
      {
         FAKE_PROPERTY = new TransientPropertyData(QPath.parse("[]unknown"), "unknown", -1, -1, "unknwon", false);
      }
      catch (IllegalPathException e)
      {
         throw new RuntimeException(e);
      }      
   }
   
   private static final int TYPE_ADD = 1 << 0;
   
   private static final int TYPE_UPDATE = 1 << 1;
   
   private static final int TYPE_DELETE = 1 << 2;
   
   private static final int TYPE_RENAME = 1 << 3;
   
   protected static final int TYPE_DELETE_LOCK = 1 << 4;

   protected static final int TYPE_INSERT_NODE = 1 << 5;

   protected static final int TYPE_INSERT_PROPERTY = 1 << 6;

   protected static final int TYPE_INSERT_REFERENCE = 1 << 7;

   protected static final int TYPE_INSERT_VALUE = 1 << 8;

   protected static final int TYPE_UPDATE_NODE = 1 << 9;

   protected static final int TYPE_UPDATE_PROPERTY = 1 << 10;

   protected static final int TYPE_UPDATE_VALUE = 1 << 11;

   protected static final int TYPE_UPDATE_REFERENCE = 1 << 12;

   protected static final int TYPE_DELETE_ITEM = 1 << 13;

   protected static final int TYPE_DELETE_REFERENCE = 1 << 14;

   protected static final int TYPE_DELETE_VALUE = 1 << 15;

   protected static final int TYPE_DELETE_VALUE_BY_ORDER_NUM = 1 << 16;

   protected static final int TYPE_DELETE_REFERENCE_BY_ORDER_NUM = 1 << 17;

   protected static final int TYPE_RENAME_NODE = 1 << 18;

   private ItemData currentItem;
   
   /**
    * The map containing the list of items currently modified using batch update
    */
   private Map<Integer, List<ItemData>> currentItems;

   
   /**
    * JDBCStorageConnection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   protected CQJDBCStorageConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<ACLHolder> getACLHolders() throws RepositoryException, IllegalStateException,
      UnsupportedOperationException
   {
      checkIfOpened();
      ResultSet resultSet = null;
      try
      {
         // query will return all the ACL holder
         resultSet = findACLHolders();
         Map<String, ACLHolder> mHolders = new HashMap<String, ACLHolder>();

         while (resultSet.next())
         {
            String cpid = resultSet.getString(COLUMN_PARENTID);
            ACLHolder holder = mHolders.get(cpid);
            if (holder == null)
            {
               holder = new ACLHolder(cpid);
               mHolders.put(cpid, holder);
            }
            int cptype = resultSet.getInt(COLUMN_PTYPE);

            if (cptype == ExtendedPropertyType.PERMISSION)
            {
               holder.setPermissions(true);
            }
            else
            {
               holder.setOwner(true);
            }
         }
         return new ArrayList<ACLHolder>(mHolders.values());
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      ResultSet resultSet = null;
      try
      {
         // query will return nodes and properties in same result set
         resultSet = findChildNodesByParentIdentifierCQ(getInternalId(parent.getIdentifier()));
         TempNodeData data = null;
         List<NodeData> childNodes = new ArrayList<NodeData>();
         while (resultSet.next())
         {
            if (data == null)
            {
               data = new TempNodeData(resultSet);
            }
            else if (!resultSet.getString(COLUMN_ID).equals(data.cid))
            {
               NodeData nodeData = loadNodeFromTemporaryNodeData(data, parent.getQPath(), parent.getACL());
               childNodes.add(nodeData);
               data = new TempNodeData(resultSet);
            }
            Map<String, SortedSet<TempPropertyData>> properties = data.properties;
            String key = resultSet.getString("PROP_NAME");
            SortedSet<TempPropertyData> values = properties.get(key);
            if (values == null)
            {
               values = new TreeSet<TempPropertyData>();
               properties.put(key, values);
            }
            values.add(new TempPropertyData(resultSet));
         }
         if (data != null)
         {
            NodeData nodeData = loadNodeFromTemporaryNodeData(data, parent.getQPath(), parent.getACL());
            childNodes.add(nodeData);
         }
         return childNodes;
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> pattern) throws RepositoryException,
      IllegalStateException
   {
      return getChildNodesDataInternal(parent, pattern);
   }

   /**
    * This method is similar to {@link CQJDBCStorageConnection#getChildNodesData(NodeData, List)} except that if the
    * QPathEntryFilter is an exact name the method {@link JDBCStorageConnection#getItemData(NodeData, QPathEntry, ItemType)}
    * will be called instead.
    */
   protected List<NodeData> getDirectChildNodesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      if (itemDataFilters.isEmpty())
      {
         return new ArrayList<NodeData>();
      }

      List<NodeData> children = new ArrayList<NodeData>();
      for (Iterator<QPathEntryFilter> it = itemDataFilters.iterator(); it.hasNext(); )
      {
         QPathEntryFilter filter = it.next();
         if (filter.isExactName())
         {
            NodeData data = (NodeData)getItemData(parent, filter.getQPathEntry(), ItemType.NODE);
            if (data != null)
            {
               children.add(data);
            }
            it.remove();
         }
      }
      if (!itemDataFilters.isEmpty())
      {
         children.addAll(getChildNodesDataInternal(parent, itemDataFilters));
      }
      return children;
   }

   private List<NodeData> getChildNodesDataInternal(NodeData parent, List<QPathEntryFilter> pattern) throws RepositoryException,
      IllegalStateException
   {
      checkIfOpened();

      if (pattern.isEmpty())
      {
         return new ArrayList<NodeData>();
      }

      ResultSet resultSet = null;
      try
      {
         // query will return nodes and properties in same result set
         resultSet = findChildNodesByParentIdentifierCQ(getInternalId(parent.getIdentifier()), pattern);
         TempNodeData data = null;
         List<NodeData> childNodes = new ArrayList<NodeData>();
         while (resultSet.next())
         {
            if (data == null)
            {
               data = new TempNodeData(resultSet);
            }
            else if (!resultSet.getString(COLUMN_ID).equals(data.cid))
            {
               NodeData nodeData = loadNodeFromTemporaryNodeData(data, parent.getQPath(), parent.getACL());
               childNodes.add(nodeData);
               data = new TempNodeData(resultSet);
            }
            Map<String, SortedSet<TempPropertyData>> properties = data.properties;
            String key = resultSet.getString("PROP_NAME");
            SortedSet<TempPropertyData> values = properties.get(key);
            if (values == null)
            {
               values = new TreeSet<TempPropertyData>();
               properties.put(key, values);
            }
            values.add(new TempPropertyData(resultSet));
         }
         if (data != null)
         {
            NodeData nodeData = loadNodeFromTemporaryNodeData(data, parent.getQPath(), parent.getACL());
            childNodes.add(nodeData);
         }
         return childNodes;
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int offset, int pageSize, List<NodeData> childNodes)
      throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      ResultSet resultSet = null;
      try
      {
         resultSet = findChildNodesByParentIdentifier(getInternalId(parent.getIdentifier()),fromOrderNum, offset, pageSize);
         TempNodeData data = null;
         while (resultSet.next())
         {
            if (data == null)
            {
               data = new TempNodeData(resultSet);
            }
            else if (!resultSet.getString(COLUMN_ID).equals(data.cid))
            {
               NodeData nodeData = loadNodeFromTemporaryNodeData(data, parent.getQPath(), parent.getACL());
               childNodes.add(nodeData);
               data = new TempNodeData(resultSet);
            }
            Map<String, SortedSet<TempPropertyData>> properties = data.properties;
            String key = resultSet.getString("PROP_NAME");
            SortedSet<TempPropertyData> values = properties.get(key);
            if (values == null)
            {
               values = new TreeSet<TempPropertyData>();
               properties.put(key, values);
            }
            values.add(new TempPropertyData(resultSet));
         }

         if (data != null)
         {
            NodeData nodeData = loadNodeFromTemporaryNodeData(data, parent.getQPath(), parent.getACL());
            childNodes.add(nodeData);
         }

         return childNodes.size() != 0 ? true : getLastOrderNumber(parent) > (fromOrderNum+pageSize-1);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void update(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      checkIfOpened();
      ResultSet rs = null;
      currentItem = data;
      try
      {
         setOperationType(TYPE_UPDATE);
         String cid = getInternalId(data.getIdentifier());

         // get existing definition first
         rs = findPropertyById(cid);
         Set<String> storageDescs = new HashSet<String>();
         int totalOldValues = 0;
         int prevType = -1;
         while (rs.next())
         {
            if (prevType == -1)
            {
               prevType = rs.getInt(COLUMN_PTYPE);
            }
            totalOldValues++;
            final String storageId = rs.getString(COLUMN_VSTORAGE_DESC);
            if (!rs.wasNull())
            {
               storageDescs.add(storageId);
            }
         }
         
         // then update type
         if (updatePropertyByIdentifier(data.getPersistedVersion(), data.getType(), cid) <= 0)
         {
            throw new JCRInvalidItemStateException("(update) Property not found " + data.getQPath().getAsString() + " "
               + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
               ItemState.UPDATED);
         }

         // update reference
         try
         {
            if (prevType == PropertyType.REFERENCE && data.getType() == PropertyType.REFERENCE)
            {
               // We replace the references
               replaceReference(data, cid, totalOldValues);
            }
            else if (prevType == PropertyType.REFERENCE)
            {
               // We remove the references as the property type has changed and it is no
               // more of type PropertyType.REFERENCE
               deleteReference(cid);
            }
            else if (data.getType() == PropertyType.REFERENCE)
            {
               // We add the references as the property type has changed and it is now
               // of type PropertyType.REFERENCE
               addReference(data);
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("Can't update REFERENCE property (" + data.getQPath() + " "
               + data.getIdentifier() + ") value: " + e.getMessage(), e);
         }

         deleteValues(cid, data, storageDescs, totalOldValues);
         addOrUpdateValues(cid, data, totalOldValues);
      }
      catch (IOException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property update. IO error: " + e, e);
         }
         throw new RepositoryException("Error of Property Value update " + e, e);
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property update. Database error: " + e, e);
         }
         exceptionHandler.handleUpdateException(e, data);
      }
      finally
      {
         currentItem = null;
         if (rs != null)
         {
            try
            {
               rs.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   private void replaceReference(PropertyData data, String cid, int totalOldValues) throws SQLException,
      InvalidItemStateException, RepositoryException, IOException
   {
      List<ValueData> vdata = data.getValues();
      if (vdata.size() < totalOldValues)
      {
         // Remove the extra values
         deleteReferenceByOrderNum(cid, data.getValues().size());
      }
      for (int i = 0; i < vdata.size(); i++)
      {
         ValueData vd = vdata.get(i);
         String refNodeIdentifier;
         try
         {
            refNodeIdentifier = ValueDataUtil.getString(vd);
         }
         catch (RepositoryException e)
         {
            throw new IOException(e.getMessage(), e);
         }
         if (i < totalOldValues)
         {
            updateReference(cid, i, getInternalId(refNodeIdentifier));
         }
         else
         {
            addReference(cid, i, getInternalId(refNodeIdentifier));
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void add(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException
   {
      currentItem = data;
      try
      {
         setOperationType(TYPE_ADD);
         super.add(data);
      }
      finally
      {
         currentItem = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void add(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      currentItem = data;
      try
      {
         setOperationType(TYPE_ADD);
         super.add(data);
      }
      finally
      {
         currentItem = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void rename(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      currentItem = data;
      try
      {
         setOperationType(TYPE_RENAME);
         super.rename(data);
      }
      finally
      {
         currentItem = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      currentItem = data;
      try
      {
         setOperationType(TYPE_DELETE);
         super.delete(data);
      }
      finally
      {
         currentItem = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void delete(PropertyData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      currentItem = data;
      try
      {
         setOperationType(TYPE_DELETE);
         super.delete(data);
      }
      finally
      {
         currentItem = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void update(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      currentItem = data;
      try
      {
         setOperationType(TYPE_UPDATE);
         super.update(data);
      }
      finally
      {
         currentItem = null;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void addValues(String cid, PropertyData data) throws IOException, SQLException, RepositoryException
   {
      addOrUpdateValues(cid, data, 0);
   }

   protected void addOrUpdateValues(String cid, PropertyData data, int totalOldValues) throws IOException,
      RepositoryException, SQLException
   {
      List<ValueData> vdata = data.getValues();

      for (int i = 0; i < vdata.size(); i++)
      {
         ValueData vd = vdata.get(i);
         ValueIOChannel channel = this.containerConfig.valueStorageProvider.getApplicableChannel(data, i);
         InputStream stream;
         int streamLength;
         String storageId;
         if (channel == null)
         {
            // prepare write of Value in database
            if (vd.isByteArray())
            {
               byte[] dataBytes = vd.getAsByteArray();
               stream = new ByteArrayInputStream(dataBytes);
               streamLength = dataBytes.length;
            }
            else
            {
               StreamPersistedValueData streamData = (StreamPersistedValueData)vd;

               SwapFile swapFile =
                  SwapFile.get(this.containerConfig.spoolConfig.tempDirectory,
                     cid + i + "." + data.getPersistedVersion(),this.containerConfig.spoolConfig.fileCleaner);
               try
               {
                  WRITE_VALUE_HELPER.writeStreamedValue(swapFile, streamData);
               }
               finally
               {
                  swapFile.spoolDone();
               }

               long vlen = swapFile.length();
               if (vlen <= Integer.MAX_VALUE)
               {
                  streamLength = (int)vlen;
               }
               else
               {
                  throw new RepositoryException("Value data large of allowed by JDBC (Integer.MAX_VALUE) " + vlen
                     + ". Property " + data.getQPath().getAsString());
               }

               stream = streamData.getAsStream();
            }
            storageId = null;
         }
         else
         {
            // write Value in external VS
            channel.write(data.getIdentifier(), vd);
            valueChanges.add(channel);
            storageId = channel.getStorageId();
            stream = null;
            streamLength = 0;
         }
         if (i < totalOldValues)
         {
            updateValueData(cid, i, stream, streamLength, storageId);
         }
         else
         {
            addValueData(cid, i, stream, streamLength, storageId);
         }
      }
   }

   private void deleteValues(String cid, PropertyData pdata, Set<String> storageDescs, int totalOldValues)
      throws IOException, SQLException, InvalidItemStateException, RepositoryException
   {
      for (String storageId : storageDescs)
      {
         final ValueIOChannel channel = this.containerConfig.valueStorageProvider.getChannel(storageId);
         try
         {
            channel.delete(pdata.getIdentifier());
            valueChanges.add(channel);
         }
         finally
         {
            channel.close();
         }
      }
      if (pdata.getValues().size() < totalOldValues)
      {
         // Remove the extra values
         deleteValueDataByOrderNum(cid, pdata.getValues().size());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      ResultSet resultSet = null;
      try
      {
         resultSet = findChildPropertiesByParentIdentifierCQ(getInternalId(parent.getIdentifier()));
         List<PropertyData> children = new ArrayList<PropertyData>();

         QPath parentPath = parent.getQPath();

         if (resultSet.next())
         {
            boolean isNotLast = true;

            do
            {
               // read property data
               String cid = resultSet.getString(COLUMN_ID);
               String identifier = getIdentifier(cid);

               String cname = resultSet.getString(COLUMN_NAME);
               int cversion = resultSet.getInt(COLUMN_VERSION);

               String cpid = resultSet.getString(COLUMN_PARENTID);
               // if parent ID is empty string - it's a root node

               int cptype = resultSet.getInt(COLUMN_PTYPE);
               boolean cpmultivalued = resultSet.getBoolean(COLUMN_PMULTIVALUED);
               QPath qpath;
               try
               {
                  qpath =
                     QPath.makeChildPath(parentPath == null ? traverseQPath(cpid) : parentPath,
                        InternalQName.parse(cname));
               }
               catch (IllegalNameException e)
               {
                  throw new RepositoryException(e.getMessage(), e);
               }

               // read values
               List<ValueData> data = new ArrayList<ValueData>();
               do
               {
                  int orderNum = resultSet.getInt(COLUMN_VORDERNUM);
                  // check is there value columns
                  if (!resultSet.wasNull())
                  {
                     final String storageId = resultSet.getString(COLUMN_VSTORAGE_DESC);
                     ValueData vdata =
                        resultSet.wasNull() ? ValueDataUtil.readValueData(cid, cptype, orderNum, cversion,
                           resultSet.getBinaryStream(COLUMN_VDATA), containerConfig.spoolConfig) : readValueData(
                           identifier, orderNum, cptype, storageId);
                     data.add(vdata);
                  }

                  isNotLast = resultSet.next();
               }
               while (isNotLast && resultSet.getString(COLUMN_ID).equals(cid));

               // To avoid using a temporary table, we sort the values manually
               Collections.sort(data, COMPARATOR_VALUE_DATA);
               //create property
               PersistedPropertyData pdata =
                  new PersistedPropertyData(identifier, qpath, getIdentifier(cpid), cversion, cptype, cpmultivalued,
                     data);

               children.add(pdata);
            }
            while (isNotLast);
         }
         return children;
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   /**
    * This method is similar to {@link CQJDBCStorageConnection#getChildPropertiesData(NodeData, List)} except that if the
    * QPathEntryFilter is an exact name the method {@link JDBCStorageConnection#getItemData(NodeData, QPathEntry, ItemType)}
    * will be called instead.
    */
   protected List<PropertyData> getDirectChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      List<PropertyData> children = new ArrayList<PropertyData>();
      for (Iterator<QPathEntryFilter> it = itemDataFilters.iterator(); it.hasNext(); )
      {
         QPathEntryFilter filter = it.next();
         if (filter.isExactName())
         {
            PropertyData data = (PropertyData)getItemData(parent, filter.getQPathEntry(), ItemType.PROPERTY);
            if (data != null)
            {
               children.add(data);
            }
            it.remove();
         }
      }
      if (!itemDataFilters.isEmpty())
      {
         children.addAll(getChildPropertiesDataInternal(parent, itemDataFilters));
      }
      return children;
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException, IllegalStateException
   {
      return getChildPropertiesDataInternal(parent, itemDataFilters);
   }

   private List<PropertyData> getChildPropertiesDataInternal(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      ResultSet resultSet = null;
      try
      {
         resultSet = findChildPropertiesByParentIdentifierCQ(getInternalId(parent.getIdentifier()), itemDataFilters);
         List<PropertyData> children = new ArrayList<PropertyData>();

         QPath parentPath = parent.getQPath();

         if (resultSet.next())
         {
            boolean isNotLast = true;

            do
            {
               // read property data
               String cid = resultSet.getString(COLUMN_ID);
               String identifier = getIdentifier(cid);

               String cname = resultSet.getString(COLUMN_NAME);
               int cversion = resultSet.getInt(COLUMN_VERSION);

               String cpid = resultSet.getString(COLUMN_PARENTID);
               // if parent ID is empty string - it's a root node

               int cptype = resultSet.getInt(COLUMN_PTYPE);
               boolean cpmultivalued = resultSet.getBoolean(COLUMN_PMULTIVALUED);
               QPath qpath;
               try
               {
                  qpath =
                     QPath.makeChildPath(parentPath == null ? traverseQPath(cpid) : parentPath,
                        InternalQName.parse(cname));
               }
               catch (IllegalNameException e)
               {
                  throw new RepositoryException(e.getMessage(), e);
               }

               // read values
               List<ValueData> data = new ArrayList<ValueData>();
               do
               {
                  int orderNum = resultSet.getInt(COLUMN_VORDERNUM);
                  // check is there value columns
                  if (!resultSet.wasNull())
                  {
                     final String storageId = resultSet.getString(COLUMN_VSTORAGE_DESC);
                     ValueData vdata =
                        resultSet.wasNull() ? ValueDataUtil.readValueData(cid, cptype, orderNum, cversion,
                           resultSet.getBinaryStream(COLUMN_VDATA), containerConfig.spoolConfig) : readValueData(
                           identifier, orderNum, cptype, storageId);
                     data.add(vdata);
                  }

                  isNotLast = resultSet.next();
               }
               while (isNotLast && resultSet.getString(COLUMN_ID).equals(cid));

               // To avoid using a temporary table, we sort the values manually
               Collections.sort(data, COMPARATOR_VALUE_DATA);
               //create property
               PersistedPropertyData pdata =
                  new PersistedPropertyData(identifier, qpath, getIdentifier(cpid), cversion, cptype, cpmultivalued,
                     data);

               children.add(pdata);
            }
            while (isNotLast);
         }
         return children;
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
   }

   /**
    * Read ACL Permissions from properties set.
    * 
    * @param cid node id (used only for error messages)
    * @param properties - Property name and property values
    * @return list ACL
    * @throws SQLException
    * @throws IllegalACLException
    * @throws IOException 
    */
   protected List<AccessControlEntry> readACLPermisions(String cid, Map<String, SortedSet<TempPropertyData>> properties)
      throws SQLException, IllegalACLException, IOException
   {
      List<AccessControlEntry> naPermissions = new ArrayList<AccessControlEntry>();
      Set<TempPropertyData> permValues = properties.get(Constants.EXO_PERMISSIONS.getAsString());

      if (permValues != null)
      {
         for (TempPropertyData value : permValues)
         {
            StringTokenizer parser;
            try
            {
               parser =
                  new StringTokenizer(ValueDataUtil.getString(value.getValueData()), AccessControlEntry.DELIMITER);
            }
            catch (RepositoryException e)
            {
               throw new IOException(e.getMessage(), e);
            }
            naPermissions.add(new AccessControlEntry(parser.nextToken(), parser.nextToken()));
         }

         return naPermissions;
      }
      else
      {
         throw new IllegalACLException("Property exo:permissions is not found for node with id: " + getIdentifier(cid));
      }
   }

   /**
    * Read ACL owner.
    * 
    * @param cid - node id (used only in exception message)
    * @param properties - Property name and property values
    * @return ACL owner
    * @throws IllegalACLException
    * @throws IOException 
    */
   protected String readACLOwner(String cid, Map<String, SortedSet<TempPropertyData>> properties)
      throws IllegalACLException, IOException
   {
      SortedSet<TempPropertyData> ownerValues = properties.get(Constants.EXO_OWNER.getAsString());
      if (ownerValues != null)
      {
         try
         {
            return ValueDataUtil.getString(ownerValues.first().getValueData());
         }
         catch (RepositoryException e)
         {
            throw new IOException(e.getMessage(), e);
         }
      }
      else
      {
         throw new IllegalACLException("Property exo:owner is not found for node with id: " + getIdentifier(cid));
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected PersistedNodeData loadNodeRecord(QPath parentPath, String cname, String cid, String cpid, int cindex,
      int cversion, int cnordernumb, AccessControlList parentACL) throws RepositoryException, SQLException
   {
      ResultSet ptProp = findNodeMainPropertiesByParentIdentifierCQ(cid);
      try
      {
         Map<String, SortedSet<TempPropertyData>> properties = new HashMap<String, SortedSet<TempPropertyData>>();
         while (ptProp.next())
         {
            String key = ptProp.getString(COLUMN_NAME);
            SortedSet<TempPropertyData> values = properties.get(key);
            if (values == null)
            {
               values = new TreeSet<TempPropertyData>();
               properties.put(key, values);
            }
            values.add(new TempPropertyData(ptProp));
         }

         return loadNodeRecord(parentPath, cname, cid, cpid, cindex, cversion, cnordernumb, properties, parentACL);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         try
         {
            ptProp.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the ResultSet: " + e.getMessage());
         }
      }
   }

   /**
    * Create NodeData from TempNodeData content.
    * 
    * @param tempData
    * @param parentPath
    * @param parentACL
    * @return
    * @throws RepositoryException
    * @throws SQLException
    * @throws IOException
    */
   protected PersistedNodeData loadNodeFromTemporaryNodeData(TempNodeData tempData, QPath parentPath,
      AccessControlList parentACL) throws RepositoryException, SQLException, IOException
   {
      return loadNodeRecord(parentPath, tempData.cname, tempData.cid, tempData.cpid, tempData.cindex,
         tempData.cversion, tempData.cnordernumb, tempData.properties, parentACL);
   }

   /**
    * Create a new node from the given parameter.
    * @throws IOException 
    */
   private PersistedNodeData loadNodeRecord(QPath parentPath, String cname, String cid, String cpid, int cindex,
      int cversion, int cnordernumb, Map<String, SortedSet<TempPropertyData>> properties, AccessControlList parentACL)
      throws RepositoryException, SQLException, IOException
   {
      try
      {
         InternalQName qname = InternalQName.parse(cname);

         QPath qpath;
         String parentCid;
         if (parentPath != null)
         {
            // get by parent and name
            qpath = QPath.makeChildPath(parentPath, qname, cindex, cid);
            parentCid = cpid;
         }
         else
         {
            // get by id
            if (cpid.equals(Constants.ROOT_PARENT_UUID))
            {
               // root node
               qpath = Constants.ROOT_PATH;
               parentCid = null;
            }
            else
            {
               qpath = QPath.makeChildPath(traverseQPath(cpid), qname, cindex, cid);
               parentCid = cpid;
            }
         }

         // PRIMARY
         SortedSet<TempPropertyData> primaryType = properties.get(Constants.JCR_PRIMARYTYPE.getAsString());
         if (primaryType == null || primaryType.isEmpty())
         {
            throw new PrimaryTypeNotFoundException("FATAL ERROR primary type record not found. Node "
               + qpath.getAsString() + ", id " + cid + ", container " + this.containerConfig.containerName, null);
         }

         InternalQName ptName = InternalQName.parse(ValueDataUtil.getString(primaryType.first().getValueData()));

         // MIXIN
         InternalQName[] mts;
         boolean owneable = false;
         boolean privilegeable = false;
         Set<TempPropertyData> mixTypes = properties.get(Constants.JCR_MIXINTYPES.getAsString());
         if (mixTypes != null)
         {
            List<InternalQName> mNames = new ArrayList<InternalQName>();
            for (TempPropertyData mxnb : mixTypes)
            {
               InternalQName mxn = InternalQName.parse(ValueDataUtil.getString(mxnb.getValueData()));
               mNames.add(mxn);

               if (!privilegeable && Constants.EXO_PRIVILEGEABLE.equals(mxn))
               {
                  privilegeable = true;
               }
               else if (!owneable && Constants.EXO_OWNEABLE.equals(mxn))
               {
                  owneable = true;
               }
            }
            mts = new InternalQName[mNames.size()];
            mNames.toArray(mts);
         }
         else
         {
            mts = new InternalQName[0];
         }

         try
         {
            // ACL
            AccessControlList acl; // NO DEFAULT values!

            if (owneable)
            {
               // has own owner
               if (privilegeable)
               {
                  // and permissions
                  acl = new AccessControlList(readACLOwner(cid, properties), readACLPermisions(cid, properties));
               }
               else if (parentACL != null)
               {
                  // use permissions from existed parent
                  acl =
                     new AccessControlList(readACLOwner(cid, properties), parentACL.hasPermissions()
                        ? parentACL.getPermissionEntries() : null);
               }
               else
               {
                  // have to search nearest ancestor permissions in ACL manager
                  acl = new AccessControlList(readACLOwner(cid, properties), null);
               }
            }
            else if (privilegeable)
            {
               // has own permissions
               if (owneable)
               {
                  // and owner
                  acl = new AccessControlList(readACLOwner(cid, properties), readACLPermisions(cid, properties));
               }
               else if (parentACL != null)
               {
                  // use owner from existed parent
                  acl = new AccessControlList(parentACL.getOwner(), readACLPermisions(cid, properties));
               }
               else
               {
                  // have to search nearest ancestor owner in ACL manager
                  // acl = new AccessControlList(traverseACLOwner(cpid), readACLPermisions(cid));
                  acl = new AccessControlList(null, readACLPermisions(cid, properties));
               }
            }
            else
            {
               if (parentACL != null)
               {
                  // construct ACL from existed parent ACL
                  acl =
                     new AccessControlList(parentACL.getOwner(), parentACL.hasPermissions()
                        ? parentACL.getPermissionEntries() : null);
               }
               else
               {
                  // have to search nearest ancestor owner and permissions in ACL manager
                  // acl = traverseACL(cpid);
                  acl = null;
               }
            }

            return new PersistedNodeData(getIdentifier(cid), qpath, getIdentifier(parentCid), cversion, cnordernumb,
               ptName, mts, acl);

         }
         catch (IllegalACLException e)
         {
            throw new RepositoryException("FATAL ERROR Node " + getIdentifier(cid) + " " + qpath.getAsString()
               + " has wrong formed ACL. ", e);
         }
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public int getLastOrderNumber(NodeData parent) throws RepositoryException
   {
      checkIfOpened();
      try
      {
         ResultSet count = findLastOrderNumberByParentIdentifier(getInternalId(parent.getIdentifier()));
         try
         {
            if (count.next())
            {
               return count.getInt(1) - 1;
            }
            else
            {
               return -1;
            }
         }
         finally
         {
            try
            {
               count.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }


   /**
    * {@inheritDoc} 
    */
   @Override
   protected QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      String id = getIdentifier(cpid);
      if (id.equals(Constants.ROOT_UUID))
      {
         return Constants.ROOT_PATH;
      }
      // get item by Identifier usecase
      List<QPathEntry> qrpath = new ArrayList<QPathEntry>(); // reverted path
      String caid = cpid; // container ancestor id
      boolean isRoot = false;
      do
      {
         ResultSet result = null;
         try
         {
            result = findItemQPathByIdentifierCQ(caid);
            if (!result.next())
            {
               throw new InvalidItemStateException("Parent not found, uuid: " + getIdentifier(caid));
            }

            String cid = result.getString(COLUMN_ID);

            QPathEntry qpe1 =
               new QPathEntry(InternalQName.parse(result.getString(COLUMN_NAME)), result.getInt(COLUMN_INDEX), cid);
            boolean isChild = caid.equals(cid);
            caid = result.getString(COLUMN_PARENTID);

            if (cid.equals(caid))
            {
               throw new InvalidItemStateException("An item with id='" + getIdentifier(caid) + "' is its own parent");
            }

            if (result.next())
            {
               QPathEntry qpe2 =
                  new QPathEntry(InternalQName.parse(result.getString(COLUMN_NAME)), result.getInt(COLUMN_INDEX),
                     result.getString(COLUMN_ID));
               if (isChild)
               {
                  // The child is the first result then we have the parent
                  qrpath.add(qpe1);
                  qrpath.add(qpe2);
                  // We need to take the value of the parent node
                  caid = result.getString(COLUMN_PARENTID);
               }
               else
               {
                  // The parent is the first result then we have the child
                  qrpath.add(qpe2);
                  qrpath.add(qpe1);
               }
            }
            else
            {
               qrpath.add(qpe1);
            }
         }
         finally
         {
            if (result != null)
            {
               try
               {
                  result.close();
               }
               catch (SQLException e)
               {
                  LOG.error("Can't close the ResultSet: " + e.getMessage());
               }
            }
         }

         if (caid.equals(Constants.ROOT_PARENT_UUID) || (id = getIdentifier(caid)).equals(Constants.ROOT_UUID))
         {
            if (id.equals(Constants.ROOT_UUID))
            {
               qrpath.add(Constants.ROOT_PATH.getEntries()[0]);
            }
            isRoot = true;
         }
      }
      while (!isRoot);

      QPathEntry[] qentries = new QPathEntry[qrpath.size()];
      int qi = 0;
      for (int i = qrpath.size() - 1; i >= 0; i--)
      {
         qentries[qi++] = qrpath.get(i);
      }
      return new QPath(qentries);
   }
   
   private void endChanges() throws InvalidItemStateException, RepositoryException
   {
      addChange(-1, -1);
   }
   
   protected void addChange(int changeType) throws InvalidItemStateException, RepositoryException
   {
      addChange(changeStatus & 31, changeType);
   }
   
   private void setOperationType(int operationType) throws InvalidItemStateException, RepositoryException
   {
      addChange(operationType, -1);
   }
   
   private boolean updateBatchingEnabled()
   {
      return containerConfig.batchSize > 1;
   }
   
   private void addChange(int operationType, int changeType) throws InvalidItemStateException, RepositoryException
   {
      if (!updateBatchingEnabled())
      {
         return;
      }
      boolean executeBatch = false;
      int currentChangeStatus = changeStatus;
      List<ItemData> pendingChanges = null;
      if (operationType != -1)
      {
         // We add a new change
         if (currentChangeStatus == 0)
         {
            // Initialization of the change status
            changeStatus = operationType;
            return;
         }
         else if (operationType == (currentChangeStatus & 31))
         {
            // We have no current change or the changes are of the same type
            // so we have no risk to get a collision between changes
            if (changeType == -1)
            {
               // no change to be processed
               return;
            }
            executeBatch = ++changeCount >= containerConfig.batchSize;
            if (executeBatch)
            {
               changeStatus = currentChangeStatus & 31;
               changeCount = 0;
            }
            else
            {
               changeStatus |= changeType;
            }
         }
         else
         {
            // We change of operation type so we need to
            // execute the pending changes to prevent collisions
            executeBatch = true;
            changeStatus = operationType;
            changeCount = 0;
            pendingChanges = currentItems.get(changeType);
         }
      }
      else
      {
         // we are about to close the statements so we need to check if there are pending changes
         executeBatch = changeCount > 0;
         if (executeBatch)
         {
            changeStatus = 0;
            changeCount = 0;
         }
      }
      if (executeBatch)
      {
         int currentChange = 0;
         try
         {
            // Delete commands
            if ((currentChangeStatus & TYPE_DELETE_VALUE) > 0)
            {
               currentChange = TYPE_DELETE_VALUE;
               deleteValue.executeBatch();
            }
            if ((currentChangeStatus & TYPE_DELETE_VALUE_BY_ORDER_NUM) > 0)
            {
               currentChange = TYPE_DELETE_VALUE_BY_ORDER_NUM;
               deleteValueDataByOrderNum.executeBatch();
            }
            if ((currentChangeStatus & TYPE_DELETE_REFERENCE) > 0)
            {
               currentChange = TYPE_DELETE_REFERENCE;
               deleteReference.executeBatch();
            }
            if ((currentChangeStatus & TYPE_DELETE_REFERENCE_BY_ORDER_NUM) > 0)
            {
               currentChange = TYPE_DELETE_REFERENCE_BY_ORDER_NUM;
               deleteReferenceByOrderNum.executeBatch();
            }
            if ((currentChangeStatus & TYPE_DELETE_ITEM) > 0)
            {
               currentChange = TYPE_DELETE_ITEM;
               int[] results = deleteItem.executeBatch();
               for (int i = 0; i < results.length; i++)
               {
                  if (results[i] == 0)
                  {
                     ItemData data = getCurrentItem(currentChange, i, FAKE_NODE);
                     if (data == FAKE_NODE)
                     {
                        throw new RepositoryException("Current item cannot be found");                           
                     }
                     throw new JCRInvalidItemStateException("(delete) " + (data.isNode() ? "Node" : "Property")+ " not found "
                        + data.getQPath().getAsString() + " " + data.getIdentifier()
                        + ". Probably was deleted by another session ", data.getIdentifier(), ItemState.DELETED);
                  }
               }
            }
            // Update commands
            if ((currentChangeStatus & TYPE_UPDATE_REFERENCE) > 0)
            {
               currentChange = TYPE_UPDATE_REFERENCE;
               updateReference.executeBatch();
            }
            if ((currentChangeStatus & TYPE_UPDATE_VALUE) > 0)
            {
               currentChange = TYPE_UPDATE_VALUE;
               updateValue.executeBatch();
            }
            if ((currentChangeStatus & TYPE_UPDATE_PROPERTY) > 0)
            {
               currentChange = TYPE_UPDATE_PROPERTY;
               int[] results = updateProperty.executeBatch();
               for (int i = 0; i < results.length; i++)
               {
                  if (results[i] == 0)
                  {
                     ItemData data = getCurrentItem(currentChange, i, FAKE_PROPERTY);
                     if (data == FAKE_PROPERTY)
                     {
                        throw new RepositoryException("Current item cannot be found");                           
                     }
                     throw new JCRInvalidItemStateException("(update) Property not found " + data.getQPath().getAsString() + " "
                              + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
                              ItemState.UPDATED);
                  }
               }
            }
            if ((currentChangeStatus & TYPE_UPDATE_NODE) > 0)
            {
               currentChange = TYPE_UPDATE_NODE;
               int[] results = updateNode.executeBatch();
               for (int i = 0; i < results.length; i++)
               {
                  if (results[i] == 0)
                  {
                     ItemData data = getCurrentItem(currentChange, i, FAKE_NODE);
                     if (data == FAKE_NODE)
                     {
                        throw new RepositoryException("Current item cannot be found");                           
                     }
                     throw new JCRInvalidItemStateException("(update) Node not found " + data.getQPath().getAsString() + " "
                              + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
                              ItemState.UPDATED);
                  }
               }
            } 
            // Rename commands
            if ((currentChangeStatus & TYPE_RENAME_NODE) > 0)
            {
               currentChange = TYPE_RENAME_NODE;
               int[] results = renameNode.executeBatch();
               for (int i = 0; i < results.length; i++)
               {
                  if (results[i] == 0)
                  {
                     ItemData data = getCurrentItem(currentChange, i, FAKE_NODE);
                     if (data == FAKE_NODE)
                     {
                        throw new RepositoryException("Current item cannot be found");                           
                     }
                     throw new JCRInvalidItemStateException("(rename) Node not found " + data.getQPath().getAsString() + " "
                              + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
                              ItemState.RENAMED);
                  }
               }
            }
            // Add commands
            if ((currentChangeStatus & TYPE_INSERT_NODE) > 0)
            {
               currentChange = TYPE_INSERT_NODE;
               insertNode.executeBatch();
            }
            if ((currentChangeStatus & TYPE_INSERT_PROPERTY) > 0)
            {
               currentChange = TYPE_INSERT_PROPERTY;
               insertProperty.executeBatch();
            }
            if ((currentChangeStatus & TYPE_INSERT_REFERENCE) > 0)
            {
               currentChange = TYPE_INSERT_REFERENCE;
               insertReference.executeBatch();
            }
            if ((currentChangeStatus & TYPE_INSERT_VALUE) > 0)
            {
               currentChange = TYPE_INSERT_VALUE;
               insertValue.executeBatch();
            }
         }
         catch (SQLException e)
         {            
            int index = -1;
            if (e instanceof BatchUpdateException)
            {
               // try to found amount of successfully executed updates
               int[] results = ((BatchUpdateException)e).getUpdateCounts();
               for (int i = 0, length = results.length; i < length; i++)
               {
                  int res = results[i];
                  if (res == Statement.EXECUTE_FAILED)
                  {
                     index = i;
                     break;
                  }
               }
               if (index == -1)
               {
                  index = results.length - 1;
               }
            }
            switch (currentChange)
            {
               case TYPE_INSERT_NODE :
               {
                  exceptionHandler.handleAddException(e, getCurrentItem(currentChange, index, FAKE_NODE));
                  break;
               }
               case TYPE_INSERT_PROPERTY :
               {
                  exceptionHandler.handleAddException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;                  
               }
               case TYPE_INSERT_REFERENCE :
               {
                  ItemData data = getCurrentItem(currentChange, index, FAKE_PROPERTY);
                  throw new RepositoryException("Can't read REFERENCE property (" + data.getQPath() + " "
                     + data.getIdentifier() + ") value: " + e.getMessage(), e);
               }
               case TYPE_INSERT_VALUE :
               {
                  exceptionHandler.handleAddException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_DELETE_VALUE :
               {
                  exceptionHandler.handleDeleteException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_DELETE_VALUE_BY_ORDER_NUM :
               {
                  exceptionHandler.handleUpdateException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_DELETE_REFERENCE :
               {
                  exceptionHandler.handleDeleteException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_DELETE_REFERENCE_BY_ORDER_NUM :
               {
                  exceptionHandler.handleDeleteException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_DELETE_ITEM :
               {
                  exceptionHandler.handleDeleteException(e, getCurrentItem(currentChange, index, FAKE_NODE));
                  break;
               }
               case TYPE_UPDATE_VALUE :
               {
                  exceptionHandler.handleUpdateException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_UPDATE_PROPERTY :
               {
                  exceptionHandler.handleUpdateException(e, getCurrentItem(currentChange, index, FAKE_PROPERTY));
                  break;
               }
               case TYPE_UPDATE_NODE :
               {
                  exceptionHandler.handleUpdateException(e, getCurrentItem(currentChange, index, FAKE_NODE));
                  break;
               }
               case TYPE_RENAME_NODE :
               {
                  exceptionHandler.handleAddException(e, getCurrentItem(currentChange, index, FAKE_NODE));
                  break;
               }
            }
            throw new RepositoryException(e);
         }
         finally
         {
            currentItems.clear();
            if (pendingChanges != null)
            {
               // re-add the pending changes
               currentItems.put(changeType, pendingChanges);
            }
         }
      }
   }
   
   protected void addCurrentItem(int changeType)
   {
      if (updateBatchingEnabled())
      {
         if (currentItems == null)
         {
            currentItems = new HashMap<Integer, List<ItemData>>();
         }
         List<ItemData> items = currentItems.get(changeType);
         if (items == null)
         {
            items = new ArrayList<ItemData>();
            currentItems.put(changeType, items);
         }
         items.add(currentItem);
      }
   }

   protected ItemData getCurrentItem(int operationType, int index, ItemData defaultValue)
   {
      if (currentItems != null && !currentItems.isEmpty() && index >= 0)
      {
         List<ItemData> items = currentItems.get(operationType);
         if (items != null && !items.isEmpty())
         {
            return items.get(index);
         }
      }
      return defaultValue;
   }
   
   protected int executeUpdate(PreparedStatement ps, int changeType) throws SQLException, InvalidItemStateException,
      RepositoryException
   {
      if (updateBatchingEnabled())
      {
         addCurrentItem(changeType);
         ps.addBatch();
         addChange(changeType);
         return 1;
      }
      else
      {
         return ps.executeUpdate();
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void prepare() throws IllegalStateException, RepositoryException
   {   
      endChanges();
      super.prepare();
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected void closeStatements()
   {
      super.closeStatements();

      try
      {
         if (findACLHolders != null)
         {
            findACLHolders.close();
         }

         if (findNodesByParentIdCQ != null)
         {
            findNodesByParentIdCQ.close();
         }

         if (findPropertiesByParentIdCQ != null)
         {
            findPropertiesByParentIdCQ.close();
         }

         if (findNodeMainPropertiesByParentIdentifierCQ != null)
         {
            findNodeMainPropertiesByParentIdentifierCQ.close();
         }

         if (findItemQPathByIdentifierCQ != null)
         {
            findItemQPathByIdentifierCQ.close();
         }

         if (findPropertyById != null)
         {
            findPropertyById.close();
         }

         if (deleteValueDataByOrderNum != null)
         {
            deleteValueDataByOrderNum.close();
         }

         if (deleteReferenceByOrderNum != null)
         {
            deleteReferenceByOrderNum.close();
         }
         
         if (updateReference != null)
         {
            updateReference.close();
         }
         
         if (updateValue != null)
         {
            updateValue.close();
         }

         if (findPropertiesByParentIdAndComplexPatternCQ != null)
         {
            findPropertiesByParentIdAndComplexPatternCQ.close();
         }

         if (findNodesByParentIdAndComplexPatternCQ != null)
         {
            findNodesByParentIdAndComplexPatternCQ.close();
         }

         if (findNodesByParentIdLazilyCQ != null)
         {
            findNodesByParentIdLazilyCQ.close();
         }
      }
      catch (SQLException e)
      {
         LOG.error("Can't close the Statement: " + e.getMessage());
      }
   }

   /**
    * Replace underscore and single quote in pattern with escaped symbol. Replace jcr-wildcard '*' with sql-wildcard '%'.
    * 
    * @param pattern
    * @return pattern with escaped underscore, single quote and fixed wildcard symbols
    */
   protected String escapeSpecialChars(String pattern)
   {
      char[] chars = pattern.toCharArray();
      StringBuilder sb = new StringBuilder(chars.length + 1);
      for (int i = 0; i < chars.length; i++)
      {
         switch (chars[i])
         {
            case '*' :
               sb.append('%');
               break;
            case '\'' :
               sb.append(getSingleQuoteEscapeSymbol());
               sb.append(chars[i]);
               break;
            case '_' :
            case '%' :
               sb.append(getWildcardEscapeSymbol());
            default :
               sb.append(chars[i]);
         }
      }
      return sb.toString();
   }

   /**
    * Escape all the single quote found
    */
   protected String escape(String pattern)
   {
      char[] chars = pattern.toCharArray();
      StringBuilder sb = new StringBuilder(chars.length + 1);
      for (int i = 0; i < chars.length; i++)
      {
         switch (chars[i])
         {
            case '\'' :
               sb.append(getSingleQuoteEscapeSymbol());
            default :
               sb.append(chars[i]);
         }
      }
      return sb.toString();
   }
   
   /**
    * Append pattern expression.
    * Appends String "I.NAME LIKE 'escaped pattern' ESCAPE 'escapeString'" or "I.NAME='pattern'"
    * to String builder sb.
    * 
    * @param sb StringBuilder
    * @param indexConstraint 
    * @param pattern
    */
   protected void appendPattern(StringBuilder sb, QPathEntry entry, boolean indexConstraint)
   {
      String pattern = entry.getAsString(false);
      sb.append("(I.NAME");
      if (pattern.contains("*"))
      {
         sb.append(" LIKE '");
         sb.append(escapeSpecialChars(pattern));
         sb.append("' ESCAPE '");
         sb.append(getLikeExpressionEscape());
         sb.append("'");
      }
      else
      {
         sb.append("='");
         sb.append(escape(pattern));
         sb.append("'");
      }

      if (indexConstraint && entry.getIndex() != -1)
      {
         sb.append(" and I.I_INDEX=");
         sb.append(entry.getIndex());
      }
      sb.append(")");
   }

   protected String getSingleQuoteEscapeSymbol()
   {
      return SINGLE_QUOTE_ESCAPE_PATTERN;
   }

   protected String getWildcardEscapeSymbol()
   {
      return PATTERN_ESCAPE_STRING;
   }

   protected String getLikeExpressionEscape()
   {
      return PATTERN_ESCAPE_STRING;
   }
   
   protected abstract ResultSet findACLHolders() throws SQLException;

   protected abstract ResultSet findItemQPathByIdentifierCQ(String identifier) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier,
      List<QPathEntryFilter> patternList) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier,
      List<QPathEntryFilter> patternList) throws SQLException;

   protected abstract ResultSet findNodeMainPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findPropertyById(String id) throws SQLException;

   protected abstract int deleteValueDataByOrderNum(String id, int orderNum) throws SQLException,
      InvalidItemStateException, RepositoryException;

   protected abstract int deleteReferenceByOrderNum(String id, int orderNum) throws SQLException,
      InvalidItemStateException, RepositoryException;
   
   protected abstract int updateValueData(String cid, int i, InputStream stream, int streamLength, String storageId)
      throws SQLException, InvalidItemStateException, RepositoryException;
   
   protected abstract int addReference(String cid, int i, String refNodeIdentifier) throws SQLException,
      InvalidItemStateException, RepositoryException;

   protected abstract int updateReference(String cid, int i, String refNodeIdentifier) throws SQLException,
      InvalidItemStateException, RepositoryException;
}
