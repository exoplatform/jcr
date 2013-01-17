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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.IllegalACLException;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ItemType;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NodeDataIndexing;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil;
import org.exoplatform.services.jcr.impl.dataflow.ValueDataUtil.ValueDataWrapper;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ACLHolder;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ChangedSizeHandler;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimplePersistedSize;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.ValueFileIOHelper;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.jcr.InvalidItemStateException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JDBCStorageConnection.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public abstract class JDBCStorageConnection extends DBConstants implements WorkspaceStorageConnection
{

   /**
    * Helper.
    */
   protected static class WriteValueHelper extends ValueFileIOHelper
   {
      /**
       * {@inheritDoc}
       */
      @Override
      public long writeStreamedValue(File file, ValueData value) throws IOException
      {
         return super.writeStreamedValue(file, value);
      }
   }

   /**
    * Connection logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCStorageConnection");

   /**
    * NODE type.
    */
   public static final int I_CLASS_NODE = 1;

   /**
    * PROPERTY type.
    */
   public static final int I_CLASS_PROPERTY = 2;

   protected final JDBCDataContainerConfig containerConfig;

   protected final Connection dbConnection;

   protected final SQLExceptionHandler exceptionHandler;

   protected final List<ValueIOChannel> valueChanges;

   protected static final WriteValueHelper WRITE_VALUE_HELPER = new WriteValueHelper();

   // All statements should be closed in closeStatements() method.

   protected PreparedStatement findItemById;

   protected PreparedStatement findItemByPath;

   protected PreparedStatement findItemByName;

   protected PreparedStatement findChildPropertyByPath;

   protected PreparedStatement findPropertyByName;

   protected PreparedStatement findDescendantNodes;

   protected PreparedStatement findDescendantProperties;

   protected PreparedStatement findReferences;

   protected PreparedStatement findValuesByPropertyId;

   protected PreparedStatement findValuesDataByPropertyId;

   protected PreparedStatement findNodesByParentId;

   protected PreparedStatement findLastOrderNumberByParentId;

   protected PreparedStatement findNodesCountByParentId;

   protected PreparedStatement findPropertiesByParentId;

   protected PreparedStatement findMaxPropertyVersions;

   protected PreparedStatement insertNode;

   protected PreparedStatement insertProperty;

   protected PreparedStatement insertReference;

   protected PreparedStatement insertValue;

   protected PreparedStatement updateNode;

   protected PreparedStatement updateProperty;

   protected PreparedStatement deleteItem;

   protected PreparedStatement deleteReference;

   protected PreparedStatement deleteValue;

   protected PreparedStatement renameNode;

   protected PreparedStatement findNodesAndProperties;

   protected PreparedStatement findNodesCount;

   protected PreparedStatement findWorkspaceDataSize;

   protected PreparedStatement findNodeDataSize;

   protected PreparedStatement findNodePropertiesOnValueStorage;

   protected PreparedStatement findWorkspacePropertiesOnValueStorage;

   protected PreparedStatement findValueStorageDescAndSize;

   /**
    * Exception instance for logging of call stack which called a closing of connection.
    */
   private Exception closedByCallStack;

   /**
    * Read-only flag, if true the connection is marked as READ-ONLY.
    */
   protected final boolean readOnly;

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
   protected JDBCStorageConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {

      this.dbConnection = dbConnection;
      this.readOnly = readOnly;
      this.containerConfig = containerConfig;

      if (!readOnly && dbConnection.getAutoCommit())
      {
         dbConnection.setAutoCommit(false);
      }

      prepareEntityNames();
      prepareQueries();
      this.exceptionHandler = new SQLExceptionHandler(this.containerConfig.containerName, this);

      this.valueChanges = new ArrayList<ValueIOChannel>();
   }

   protected void prepareEntityNames()
   {
      switch (containerConfig.dbStructureType)
      {
         case MULTI :
            JCR_ITEM = "JCR_MITEM";
            JCR_VALUE = "JCR_MVALUE";
            JCR_REF = "JCR_MREF";
            JCR_PK_ITEM = "JCR_PK_MITEM";
            JCR_FK_ITEM_PARENT = "JCR_FK_MITEM_PARENT";
            JCR_IDX_ITEM_PARENT = "JCR_IDX_MITEM_PARENT";
            JCR_IDX_ITEM_PARENT_NAME = "JCR_IDX_MITEM_PARENT_NAME";
            JCR_IDX_ITEM_PARENT_ID = "JCR_IDX_MITEM_PARENT_ID";
            JCR_PK_VALUE = "JCR_PK_MVALUE";
            JCR_FK_VALUE_PROPERTY = "JCR_FK_MVALUE_PROPERTY";
            JCR_IDX_VALUE_PROPERTY = "JCR_IDX_MVALUE_PROPERTY";
            JCR_PK_REF = "JCR_PK_MREF";
            JCR_IDX_REF_PROPERTY = "JCR_IDX_MREF_PROPERTY";
            JCR_IDX_ITEM_N_ORDER_NUM = "JCR_IDX_MITEM_N_ORDER_NUM";
            JCR_IDX_ITEM_PARENT_FK = "JCR_IDX_MITEM_PARENT_FK";
            break;

         case SINGLE :
            JCR_ITEM = "JCR_SITEM";
            JCR_VALUE = "JCR_SVALUE";
            JCR_REF = "JCR_SREF";
            JCR_PK_ITEM = "JCR_PK_SITEM";
            JCR_FK_ITEM_PARENT = "JCR_FK_SITEM_PARENT";
            JCR_IDX_ITEM_PARENT = "JCR_IDX_SITEM_PARENT";
            JCR_IDX_ITEM_PARENT_NAME = "JCR_IDX_SITEM_PARENT_NAME";
            JCR_IDX_ITEM_PARENT_ID = "JCR_IDX_SITEM_PARENT_ID";
            JCR_PK_VALUE = "JCR_PK_SVALUE";
            JCR_FK_VALUE_PROPERTY = "JCR_FK_SVALUE_PROPERTY";
            JCR_IDX_VALUE_PROPERTY = "JCR_IDX_SVALUE_PROPERTY";
            JCR_PK_REF = "JCR_PK_SREF";
            JCR_IDX_REF_PROPERTY = "JCR_IDX_SREF_PROPERTY";
            JCR_IDX_ITEM_N_ORDER_NUM = "JCR_IDX_SITEM_N_ORDER_NUM";
            JCR_IDX_ITEM_PARENT_FK = "JCR_IDX_SITEM_PARENT_FK";
            break;

         case ISOLATED :
            JCR_ITEM = "JCR_I" + containerConfig.dbTableSuffix;
            JCR_VALUE = "JCR_V" + containerConfig.dbTableSuffix;
            JCR_REF = "JCR_R" + containerConfig.dbTableSuffix;
            JCR_PK_ITEM = "JCR_PK_I" + containerConfig.dbTableSuffix;
            JCR_FK_ITEM_PARENT = "JCR_FK_I" + containerConfig.dbTableSuffix + "_PARENT";
            JCR_IDX_ITEM_PARENT = "JCR_IDX_I" + containerConfig.dbTableSuffix + "_PARENT";
            JCR_IDX_ITEM_PARENT_NAME = "JCR_IDX_I" + containerConfig.dbTableSuffix + "_PARENT_NAME";
            JCR_IDX_ITEM_PARENT_ID = "JCR_IDX_I" + containerConfig.dbTableSuffix + "_PARENT_ID";
            JCR_PK_VALUE = "JCR_PK_V" + containerConfig.dbTableSuffix;
            JCR_FK_VALUE_PROPERTY = "JCR_FK_V" + containerConfig.dbTableSuffix + "_PROPERTY";
            JCR_IDX_VALUE_PROPERTY = "JCR_IDX_V" + containerConfig.dbTableSuffix + "_PROPERTY";
            JCR_PK_REF = "JCR_PK_R" + containerConfig.dbTableSuffix;
            JCR_IDX_REF_PROPERTY = "JCR_IDX_R" + containerConfig.dbTableSuffix + "_PROPERTY";
            JCR_IDX_ITEM_N_ORDER_NUM = "JCR_IDX_I" + containerConfig.dbTableSuffix + "_N_ORDER_NUM";
            JCR_IDX_ITEM_PARENT_FK = "JCR_IDX_I" + containerConfig.dbTableSuffix + "_PARENT_FK";
            break;

         default :
            break;
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean equals(Object obj)
   {
      if (obj == this)
      {
         return true;
      }

      if (obj instanceof JDBCStorageConnection)
      {
         JDBCStorageConnection another = (JDBCStorageConnection)obj;
         return getJdbcConnection() == another.getJdbcConnection();
      }

      return false;
   }

   /**
    * Return JDBC connection obtained from initialized data source. NOTE: Helper can obtain one new
    * connection per each call of the method or return one obtained once.
    */
   public Connection getJdbcConnection()
   {
      return dbConnection;
   }

   /**
    * Prepared queries at start time.
    * 
    * @throws SQLException
    *           database error
    */
   abstract protected void prepareQueries() throws SQLException;

   /**
    * Used in Single Db Connection classes for Identifier related queries.
    * 
    * @param identifier
    *          Item id
    * @return String with container internal id
    */
   protected abstract String getInternalId(String identifier);

   /**
    * Used in loadXYZRecord methods for extract real Identifier from container value.
    * 
    * @param internalId
    * @return
    */
   protected abstract String getIdentifier(String internalId);

   // ---------------- WorkspaceStorageConnection -------------

   /**
    * @throws IllegalStateException
    *           if connection is closed.
    */
   protected void checkIfOpened() throws IllegalStateException
   {
      if (!isOpened())
      {
         throw new IllegalStateException("Connection is already closed", this.closedByCallStack);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean isOpened()
   {
      try
      {
         return !dbConnection.isClosed();
      }
      catch (SQLException e)
      {
         LOG.error("An exception occured: " + e.getMessage());
         return false;
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void rollback() throws IllegalStateException, RepositoryException
   {
      checkIfOpened();
      try
      {
         closeStatements();

         if (!readOnly)
         {
            try
            {
               dbConnection.rollback();
            }
            finally
            {
               // rollback from the end
               IOException e = null;
               for (int p = valueChanges.size() - 1; p >= 0; p--)
               {
                  try
                  {
                     valueChanges.get(p).rollback();
                  }
                  catch (IOException e1)
                  {
                     if (e == null)
                     {
                        e = e1;
                     }
                     else
                     {
                        LOG.error("Could not rollback value change", e1);
                     }
                  }
               }
               if (e != null)
               {
                  throw e;
               }
            }
         }
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
         valueChanges.clear();
         try
         {
            dbConnection.close();
         }
         catch (SQLException e)
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn("Could not close the connection", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IllegalStateException, RepositoryException
   {
      checkIfOpened();
      this.closedByCallStack = new Exception("The connection has been closed by the following call stack");

      try
      {
         closeStatements();

         if (!readOnly && dbConnection.getTransactionIsolation() > Connection.TRANSACTION_READ_COMMITTED)
         {
            dbConnection.rollback();
         }

         dbConnection.close();
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Close all statements.
    */
   protected void closeStatements()
   {
      try
      {
         if (findItemById != null)
         {
            findItemById.close();
         }

         if (findItemByPath != null)
         {
            findItemByPath.close();
         }

         if (findItemByName != null)
         {
            findItemByName.close();
         }

         if (findChildPropertyByPath != null)
         {
            findChildPropertyByPath.close();
         }

         if (findPropertyByName != null)
         {
            findPropertyByName.close();
         }

         if (findDescendantNodes != null)
         {
            findDescendantNodes.close();
         }

         if (findDescendantProperties != null)
         {
            findDescendantProperties.close();
         }

         if (findReferences != null)
         {
            findReferences.close();
         }

         if (findValuesByPropertyId != null)
         {
            findValuesByPropertyId.close();
         }

         if (findValuesDataByPropertyId != null)
         {
            findValuesDataByPropertyId.close();
         }

         if (findNodesByParentId != null)
         {
            findNodesByParentId.close();
         }

         if (findLastOrderNumberByParentId != null)
         {
            findLastOrderNumberByParentId.close();
         }

         if (findNodesCountByParentId != null)
         {
            findNodesCountByParentId.close();
         }

         if (findPropertiesByParentId != null)
         {
            findPropertiesByParentId.close();
         }

         if (findMaxPropertyVersions != null)
         {
            findMaxPropertyVersions.close();
         }
         
         if (insertNode != null)
         {
            insertNode.close();
         }

         if (insertProperty != null)
         {
            insertProperty.close();
         }

         if (insertReference != null)
         {
            insertReference.close();
         }

         if (insertValue != null)
         {
            insertValue.close();
         }

         if (updateNode != null)
         {
            updateNode.close();
         }

         if (updateProperty != null)
         {
            updateProperty.close();
         }

         if (deleteItem != null)
         {
            deleteItem.close();
         }

         if (deleteReference != null)
         {
            deleteReference.close();
         }

         if (deleteValue != null)
         {
            deleteValue.close();
         }

         if (renameNode != null)
         {
            renameNode.close();
         }

         if (findNodesAndProperties != null)
         {
            findNodesAndProperties.close();
         }

         if (findNodesCount != null)
         {
            findNodesCount.close();
         }

         if (findWorkspaceDataSize != null)
         {
            findWorkspaceDataSize.close();
         }

         if (findNodeDataSize != null)
         {
            findNodeDataSize.close();
         }

         if (findNodePropertiesOnValueStorage != null)
         {
            findNodePropertiesOnValueStorage.close();
         }

         if (findWorkspacePropertiesOnValueStorage != null)
         {
            findWorkspacePropertiesOnValueStorage.close();
         }

         if (findValueStorageDescAndSize != null)
         {
            findValueStorageDescAndSize.close();
         }
      }
      catch (SQLException e)
      {
         LOG.error("Can't close the statement: " + e.getMessage());
      }
   }

   /**
    * {@inheritDoc}
    */
   public void prepare() throws IllegalStateException, RepositoryException
   {
      try
      {
         for (ValueIOChannel vo : valueChanges)
         {
            vo.prepare();
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public final void commit() throws IllegalStateException, RepositoryException
   {
      checkIfOpened();
      try
      {
         closeStatements();

         if (!readOnly)
         {
            try
            {
               for (ValueIOChannel vo : valueChanges)
               {
                  vo.twoPhaseCommit();
               }
            }
            catch (IOException e)
            {
               throw new RepositoryException(e);
            }
            finally
            {
               valueChanges.clear();
            }
            dbConnection.commit();
         }
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         try
         {
            dbConnection.close();
         }
         catch (SQLException e)
         {
            if (LOG.isWarnEnabled())
            {
               LOG.warn("Could not close the connection", e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void add(NodeData data) throws RepositoryException, UnsupportedOperationException, InvalidItemStateException,
      IllegalStateException
   {
      checkIfOpened();
      try
      {
         addNodeRecord(data);
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Node added " + data.getQPath().getAsString() + ", " + data.getIdentifier() + ", "
               + data.getPrimaryTypeName().getAsString());
         }
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Node add. Database error: " + e);
         }

         exceptionHandler.handleAddException(e, data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void add(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
      UnsupportedOperationException, InvalidItemStateException, IllegalStateException
   {
      checkIfOpened();

      try
      {
         addPropertyRecord(data);

         if (data.getType() == PropertyType.REFERENCE)
         {
            try
            {
               addReference(data);
            }
            catch (IOException e)
            {
               throw new RepositoryException("Can't read REFERENCE property (" + data.getQPath() + " "
                  + data.getIdentifier() + ") value: " + e.getMessage(), e);
            }
         }

         addValues(getInternalId(data.getIdentifier()), data, sizeHandler);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Property added " + data.getQPath().getAsString() + ", " + data.getIdentifier()
               + (data.getValues() != null ? ", values count: " + data.getValues().size() : ", NULL data"));
         }

      }
      catch (IOException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property add. IO error: " + e, e);
         }
         throw new RepositoryException("Error of Property Value add " + e, e);
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property add. Database error: " + e, e);
         }
         exceptionHandler.handleAddException(e, data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void rename(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {

      checkIfOpened();
      try
      {
         if (renameNode(data) <= 0)
         {
            throw new JCRInvalidItemStateException("(rename) Node not found " + data.getQPath().getAsString() + " "
               + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
               ItemState.RENAMED);
         }
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property add. Database error: " + e, e);
         }
         exceptionHandler.handleAddException(e, data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void delete(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      checkIfOpened();

      final String cid = getInternalId(data.getIdentifier());

      try
      {
         int nc = deleteItemByIdentifier(cid);
         if (nc <= 0)
         {
            throw new JCRInvalidItemStateException("(delete) Node not found " + data.getQPath().getAsString() + " "
               + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
               ItemState.DELETED);
         }

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Node deleted " + data.getQPath().getAsString() + ", " + data.getIdentifier() + ", "
               + (data).getPrimaryTypeName().getAsString());
         }

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Node remove. Database error: " + e, e);
         }
         exceptionHandler.handleDeleteException(e, data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void delete(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
      UnsupportedOperationException, InvalidItemStateException, IllegalStateException
   {
      checkIfOpened();

      final String cid = getInternalId(data.getIdentifier());

      try
      {
         deleteValues(cid, data, false, sizeHandler);

         // delete references
         deleteReference(cid);

         // delete item
         int nc = deleteItemByIdentifier(cid);
         if (nc <= 0)
         {
            throw new JCRInvalidItemStateException("(delete) Property not found " + data.getQPath().getAsString() + " "
               + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
               ItemState.DELETED);
         }

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Property deleted " + data.getQPath().getAsString() + ", " + data.getIdentifier()
               + ((data).getValues() != null ? ", values count: " + (data).getValues().size() : ", NULL data"));
         }

      }
      catch (IOException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property remove. IO error: " + e, e);
         }
         throw new RepositoryException("Error of Property Value delete " + e, e);
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Property remove. Database error: " + e, e);
         }
         exceptionHandler.handleDeleteException(e, data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void update(NodeData data) throws RepositoryException, UnsupportedOperationException,
      InvalidItemStateException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         String cid = getInternalId(data.getIdentifier());
         // order numb update
         if (updateNodeByIdentifier(data.getPersistedVersion(), data.getQPath().getIndex(), data.getOrderNumber(), cid) <= 0)
         {
            throw new JCRInvalidItemStateException("(update) Node not found " + data.getQPath().getAsString() + " "
               + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
               ItemState.UPDATED);
         }

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Node updated " + data.getQPath().getAsString() + ", " + data.getIdentifier() + ", "
               + data.getPrimaryTypeName().getAsString());
         }

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Node update. Database error: " + e, e);
         }
         exceptionHandler.handleUpdateException(e, data);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void update(PropertyData data, ChangedSizeHandler sizeHandler) throws RepositoryException,
      UnsupportedOperationException, InvalidItemStateException, IllegalStateException
   {
      checkIfOpened();

      try
      {
         String cid = getInternalId(data.getIdentifier());

         // update type
         if (updatePropertyByIdentifier(data.getPersistedVersion(), data.getType(), cid) <= 0)
         {
            throw new JCRInvalidItemStateException("(update) Property not found " + data.getQPath().getAsString() + " "
               + data.getIdentifier() + ". Probably was deleted by another session ", data.getIdentifier(),
               ItemState.UPDATED);
         }

         // update reference
         try
         {
            deleteReference(cid);

            if (data.getType() == PropertyType.REFERENCE)
            {
               addReference(data);
            }
         }
         catch (IOException e)
         {
            throw new RepositoryException("Can't update REFERENCE property (" + data.getQPath() + " "
               + data.getIdentifier() + ") value: " + e.getMessage(), e);
         }

         // do Values update: delete all and add all
         deleteValues(cid, data, true, sizeHandler);
         addValues(cid, data, sizeHandler);

         if (LOG.isDebugEnabled())
         {
            LOG.debug("Property updated " + data.getQPath().getAsString() + ", " + data.getIdentifier()
               + (data.getValues() != null ? ", values count: " + data.getValues().size() : ", NULL data"));
         }

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
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet node = findChildNodesByParentIdentifier(getInternalId(parent.getIdentifier()));
         try
         {
            List<NodeData> childrens = new ArrayList<NodeData>();
            while (node.next())
            {
               childrens.add((NodeData)itemData(parent.getQPath(), node, I_CLASS_NODE, parent.getACL()));
            }

            return childrens;
         }
         finally
         {
            try
            {
               node.close();
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
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> pattern) throws RepositoryException,
      IllegalStateException
   {
      //return all child nodes by default
      return getChildNodesData(parent);
   }

   /**
    * {@inheritDoc}
    */
   public int getLastOrderNumber(NodeData parent) throws RepositoryException
   {
      checkIfOpened();
      try
      {
         ResultSet count = findLastOrderNumberByParentIdentifier(getInternalId(parent.getIdentifier()));
         try
         {
            if (count.next() && count.getInt(1) > 0)
            {
               return count.getInt(2);
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
   public int getMaxPropertyVersion(PropertyData data) throws RepositoryException
   {
      checkIfOpened();
      try
      {
         ResultSet count =
            findMaxPropertyVersion(data.getParentIdentifier(), data.getQPath().getName().getAsString(), data.getQPath()
               .getIndex());
         try
         {
            if (count.next())
            {
               return count.getInt(1);
            }
            else
            {
               return 0;
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
               LOG.error("Can't close the ResultSet: " + e, e);
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
   public int getChildNodesCount(NodeData parent) throws RepositoryException
   {
      checkIfOpened();
      try
      {
         ResultSet count = findChildNodesCountByParentIdentifier(getInternalId(parent.getIdentifier()));
         try
         {
            if (count.next())
            {
               return count.getInt(1);
            }
            else
            {
               throw new RepositoryException("FATAL No resulton childNodes count for "
                  + parent.getQPath().getAsString());
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
   public List<PropertyData> getChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet prop = findChildPropertiesByParentIdentifier(getInternalId(parent.getIdentifier()));
         try
         {
            List<PropertyData> children = new ArrayList<PropertyData>();
            while (prop.next())
            {
               children.add((PropertyData)itemData(parent.getQPath(), prop, I_CLASS_PROPERTY, null));
            }

            return children;
         }
         finally
         {
            try
            {
               prop.close();
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
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilter)
      throws RepositoryException, IllegalStateException
   {
      //return all child properties by default
      return getChildPropertiesData(parent);
   }

   /**
    * Returns from storage the next page of nodes and its properties.
    */
   public List<NodeDataIndexing> getNodesAndProperties(String lastNodeId, int offset, int limit)
      throws RepositoryException, IllegalStateException
   {
      List<NodeDataIndexing> result = new ArrayList<NodeDataIndexing>();

      checkIfOpened();
      try
      {
         ResultSet resultSet = findNodesAndProperties(lastNodeId, offset, limit);
         int processed = 0;

         try
         {
            TempNodeData tempNodeData = null;

            while (resultSet.next())
            {
               if (tempNodeData == null)
               {
                  tempNodeData = new TempNodeData(resultSet);
                  processed++;
               }
               else if (!resultSet.getString(COLUMN_ID).equals(tempNodeData.cid))
               {
                  if (!needToSkipOffsetNodes() || processed > offset)
                  {
                     result.add(createNodeDataIndexing(tempNodeData));
                  }

                  tempNodeData = new TempNodeData(resultSet);
                  processed++;
               }

               if (!needToSkipOffsetNodes() || processed > offset)
               {
                  String key = resultSet.getString("P_NAME");

                  SortedSet<TempPropertyData> values = tempNodeData.properties.get(key);
                  if (values == null)
                  {
                     values = new TreeSet<TempPropertyData>();
                     tempNodeData.properties.put(key, values);
                  }

                  values.add(new ExtendedTempPropertyData(resultSet));
               }
            }

            if (tempNodeData != null && (!needToSkipOffsetNodes() || processed > offset))
            {
               result.add(createNodeDataIndexing(tempNodeData));
            }
         }
         finally
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
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }

      return result;
   }

   /**
    * Some implementations could require to skip first 'offset' nodes from
    * result set. 
    */
   protected boolean needToSkipOffsetNodes()
   {
      return false;
   }

   /**
    * 
    * @param parent
    * @param lastOrderNum
    * @param limit
    * @return
    * @throws RepositoryException
    * @throws IllegalStateException
    */
   public boolean getChildNodesDataByPage(NodeData parent, int fromOrderNum, int toOrderNum, List<NodeData> childNodes)
      throws RepositoryException, IllegalStateException
   {
      // not supported by non-CQ deprecated JDBC container
      throw new UnsupportedRepositoryOperationException();
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> listChildPropertiesData(NodeData parent) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet prop = findChildPropertiesByParentIdentifier(getInternalId(parent.getIdentifier()));
         try
         {
            List<PropertyData> children = new ArrayList<PropertyData>();
            while (prop.next())
            {
               children.add(propertyData(parent.getQPath(), prop));
            }

            return children;
         }
         finally
         {
            try
            {
               prop.close();
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
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<ACLHolder> getACLHolders() throws RepositoryException, IllegalStateException,
      UnsupportedOperationException
   {
      throw new UnsupportedOperationException(
         "This method is not supported by the old JDBCWorkspaceDataContainer, use CQJDBCWorkspaceDataContainer instead.");
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(String identifier) throws RepositoryException, IllegalStateException
   {
      return getItemByIdentifier(getInternalId(identifier));
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
      IllegalStateException
   {
      return hasItemByName(parentData, getInternalId(parentData.getIdentifier()), name, itemType);
   }

   /**
    * {@inheritDoc}
    */
   public ItemData getItemData(NodeData parentData, QPathEntry name, ItemType itemType) throws RepositoryException,
      IllegalStateException
   {

      if (parentData != null)
      {
         return getItemByName(parentData, getInternalId(parentData.getIdentifier()), name, itemType);
      }

      // it's a root node
      return getItemByName(null, null, name, itemType);
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet refProps = findReferences(getInternalId(nodeIdentifier));
         try
         {
            List<PropertyData> references = new ArrayList<PropertyData>();
            while (refProps.next())
            {
               references.add((PropertyData)itemData(null, refProps, I_CLASS_PROPERTY, null));
            }
            return references;
         }
         finally
         {
            try
            {
               refProps.close();
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
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Reads count of nodes in workspace.
    * 
    * @return
    *          nodes count 
    * @throws RepositoryException
    *           if a database access error occurs
    */
   public long getNodesCount() throws RepositoryException
   {
      try
      {
         ResultSet countNodes = findNodesCount();
         try
         {
            if (countNodes.next())
            {
               return countNodes.getLong(1);
            }
            else
            {
               throw new SQLException("ResultSet has't records.");
            }
         }
         finally
         {
            JDBCUtils.freeResources(countNodes, null, null);
         }
      }
      catch (SQLException e)
      {
         throw new RepositoryException("Can not calculate nodes count", e);
      }
   }

   /**
    * Calculates workspace data size.
    *
    * @throws RepositoryException
    *           if a database access error occurs
    */
   public long getWorkspaceDataSize() throws RepositoryException
   {
      long dataSize = 0;

      ResultSet result = null;
      try
      {
         result = findWorkspaceDataSize();
         try
         {
            if (result.next())
            {
               dataSize += result.getLong(1);
            }
         }
         finally
         {
            JDBCUtils.freeResources(result, null, null);
         }

         result = findWorkspacePropertiesOnValueStorage();
         try
         {
            while (result.next())
            {
               String storageDesc = result.getString(DBConstants.COLUMN_VSTORAGE_DESC);
               String propertyId = result.getString(DBConstants.COLUMN_VPROPERTY_ID);
               int orderNum = result.getInt(DBConstants.COLUMN_VORDERNUM);

               ValueIOChannel channel = containerConfig.valueStorageProvider.getChannel(storageDesc);

               dataSize += channel.getValueSize(getIdentifier(propertyId), orderNum);
            }
         }
         finally
         {
            JDBCUtils.freeResources(result, null, null);
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }

      return dataSize;
   }

   /**
    * Calculates node data size.
    *
    * @throws RepositoryException
    *           if a database access error occurs
    */
   public long getNodeDataSize(String nodeIdentifier) throws RepositoryException
   {
      long dataSize = 0;

      ResultSet result = null;
      try
      {
         result = findNodeDataSize(getInternalId(nodeIdentifier));
         try
         {
            if (result.next())
            {
               dataSize += result.getLong(1);
            }
         }
         finally
         {
            JDBCUtils.freeResources(result, null, null);
         }

         result = findNodePropertiesOnValueStorage(getInternalId(nodeIdentifier));
         try
         {
            while (result.next())
            {
               String storageDesc = result.getString(DBConstants.COLUMN_VSTORAGE_DESC);
               String propertyId = result.getString(DBConstants.COLUMN_VPROPERTY_ID);
               int orderNum = result.getInt(DBConstants.COLUMN_VORDERNUM);

               ValueIOChannel channel = containerConfig.valueStorageProvider.getChannel(storageDesc);

               dataSize += channel.getValueSize(getIdentifier(propertyId), orderNum);
            }
         }
         finally
         {
            JDBCUtils.freeResources(result, null, null);
         }
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }

      return dataSize;
   }

   // ------------------ Private methods ---------------

   /**
    * Get Item By Identifier.
    * 
    * @param cid
    *          Item id (container internal)
    * @return ItemData
    * @throws RepositoryException
    *           Repository error
    * @throws IllegalStateException
    *           if connection is closed
    */
   protected ItemData getItemByIdentifier(String cid) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet item = findItemByIdentifier(cid);
         try
         {
            if (item.next())
            {
               return itemData(null, item, item.getInt(COLUMN_CLASS), null);
            }
            return null;
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
      catch (SQLException e)
      {
         throw new RepositoryException("getItemData() error", e);
      }
      catch (IOException e)
      {
         throw new RepositoryException("getItemData() error", e);
      }
   }

   protected boolean hasItemByName(NodeData parent, String parentId, QPathEntry name, ItemType itemType)
      throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet item = null;
         try
         {
            item = findItemByName(parentId, name.getAsString(), name.getIndex());
            while (item.next())
            {
               int columnClass = item.getInt(COLUMN_CLASS);
               if (itemType == ItemType.UNKNOWN || columnClass == itemType.ordinal())
               {
                  return true;
               }
            }

            return false;
         }
         finally
         {
            try
            {
               if (item != null)
               {
                  item.close();
               }
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
    * Gets an item data from database.
    * 
    * @param parentPath
    *          - parent QPath
    * @param parentId
    *          - parent container internal id (depends on Multi/Single DB)
    * @param name
    *          - item name
    * @param itemType
    *          - item type         
    * @return - ItemData instance
    * @throws RepositoryException
    *           Repository error
    * @throws IllegalStateException
    *           if connection is closed
    */
   protected ItemData getItemByName(NodeData parent, String parentId, QPathEntry name, ItemType itemType)
      throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      try
      {
         ResultSet item = null;
         try
         {
            item = findItemByName(parentId, name.getAsString(), name.getIndex());
            while (item.next())
            {
               int columnClass = item.getInt(COLUMN_CLASS);
               if (itemType == ItemType.UNKNOWN || columnClass == itemType.ordinal())
               {
                  return itemData(parent.getQPath(), item, columnClass, parent.getACL());
               }
            }

            return null;
         }
         finally
         {
            try
            {
               if (item != null)
               {
                  item.close();
               }
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
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Build Item path by id.
    * 
    * @param cpid
    *          - Item id (container id)
    * @return Item QPath
    * @throws SQLException
    *           - if database error occurs
    * @throws InvalidItemStateException
    *           - if parent not found
    * @throws IllegalNameException
    *           - if name on the path is wrong
    */
   protected QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      return traverseQPathSQ(cpid);
   }

   /**
    * The method <code>traverseQPath</code> implemented thanks to simple queries. It allows
    * to use Simple Queries instead of Complex Queries when complex queries are much slower such
    * as with HSQLDB for example.
    */
   protected QPath traverseQPathSQ(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      // get item by Identifier usecase 
      List<QPathEntry> qrpath = new ArrayList<QPathEntry>(); // reverted path
      String caid = cpid; // container ancestor id
      do
      {
         ResultSet parent = findItemByIdentifier(caid);
         try
         {
            if (!parent.next())
            {
               throw new InvalidItemStateException("Parent not found, uuid: " + getIdentifier(caid));
            }

            QPathEntry qpe =
               new QPathEntry(InternalQName.parse(parent.getString(COLUMN_NAME)), parent.getInt(COLUMN_INDEX), caid);
            qrpath.add(qpe);
            caid = parent.getString(COLUMN_PARENTID);

            if (caid.equals(parent.getString(COLUMN_ID)))
            {
               throw new InvalidItemStateException("An item with id='" + getIdentifier(caid) + "' is its own parent");
            }
         }
         finally
         {
            try
            {
               parent.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e.getMessage());
            }
         }
      }
      while (!caid.equals(Constants.ROOT_PARENT_UUID));

      QPathEntry[] qentries = new QPathEntry[qrpath.size()];
      int qi = 0;
      for (int i = qrpath.size() - 1; i >= 0; i--)
      {
         qentries[qi++] = qrpath.get(i);
      }
      return new QPath(qentries);
   }

   /**
    * ItemLocationInfo.
    * 
    */
   class ItemLocationInfo
   {
      /**
       * Item qpath
       */
      final QPath qpath;

      /**
       * All ancestors of the item with qpath
       */
      final List<String> ancestors;

      /**
       * Item id.
       */
      final String itemId;

      /**
       * ItemLocationInfo constructor.
       * 
       * @param qpath
       *          Item path
       * @param ancestors
       *          ancesstors id list
       * @param itemId
       *          Item id
       */
      ItemLocationInfo(QPath qpath, List<String> ancestors, String itemId)
      {
         this.qpath = qpath;
         this.ancestors = ancestors;
         this.itemId = itemId;
      }
   }

   protected String findParentId(String cid) throws SQLException, RepositoryException
   {
      ResultSet pidrs = findItemByIdentifier(cid);
      try
      {
         if (pidrs.next())
         {
            return pidrs.getString(COLUMN_PARENTID);
         }
         else
         {
            throw new RepositoryException("Item not found id: " + getIdentifier(cid));
         }
      }
      finally
      {
         try
         {
            pidrs.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the ResultSet: " + e.getMessage());
         }
      }
   }

   /**
    * Build ItemData.
    * 
    * @param parentPath
    *          - parent path
    * @param item
    *          database - ResultSet with Item record(s)
    * @param itemClass
    *          - Item type (Node or Property)
    * @param parentACL
    *          - parent ACL
    * @return ItemData instance
    * @throws RepositoryException
    *           Repository error
    * @throws SQLException
    *           database error
    * @throws IOException
    *           I/O error
    */
   private ItemData itemData(QPath parentPath, ResultSet item, int itemClass, AccessControlList parentACL)
      throws RepositoryException, SQLException, IOException
   {
      String cid = item.getString(COLUMN_ID);
      String cname = item.getString(COLUMN_NAME);
      int cversion = item.getInt(COLUMN_VERSION);

      String cpid = item.getString(COLUMN_PARENTID);
      // if parent ID is empty string - it's a root node
      // cpid = cpid.equals(Constants.ROOT_PARENT_UUID) ? null : cpid;

      try
      {
         if (itemClass == I_CLASS_NODE)
         {
            int cindex = item.getInt(COLUMN_INDEX);
            int cnordernumb = item.getInt(COLUMN_NORDERNUM);
            return loadNodeRecord(parentPath, cname, cid, cpid, cindex, cversion, cnordernumb, parentACL);
         }

         int cptype = item.getInt(COLUMN_PTYPE);
         boolean cpmultivalued = item.getBoolean(COLUMN_PMULTIVALUED);
         return loadPropertyRecord(parentPath, cname, cid, cpid, cversion, cptype, cpmultivalued);
      }
      catch (InvalidItemStateException e)
      {
         throw new InvalidItemStateException("FATAL: Can't build item path for name " + cname + " id: "
            + getIdentifier(cid) + ". " + e);
      }
   }

   /**
    * Read property data without value data. For listChildPropertiesData(NodeData).
    * 
    * @param parentPath
    *          - parent path
    * @param item
    *          database - ResultSet with Item record(s)
    * @return PropertyData instance
    * @throws RepositoryException
    *           Repository error
    * @throws SQLException
    *           database error
    * @throws IOException
    *           I/O error
    */
   private PropertyData propertyData(QPath parentPath, ResultSet item) throws RepositoryException, SQLException,
      IOException
   {
      String cid = item.getString(COLUMN_ID);
      String cname = item.getString(COLUMN_NAME);
      int cversion = item.getInt(COLUMN_VERSION);
      String cpid = item.getString(COLUMN_PARENTID);
      int cptype = item.getInt(COLUMN_PTYPE);
      boolean cpmultivalued = item.getBoolean(COLUMN_PMULTIVALUED);

      try
      {
         InternalQName qname = InternalQName.parse(cname);

         QPath qpath = QPath.makeChildPath(parentPath == null ? traverseQPath(cpid) : parentPath, qname);

         PersistedPropertyData pdata =
            new PersistedPropertyData(getIdentifier(cid), qpath, getIdentifier(cpid), cversion, cptype, cpmultivalued,
               new ArrayList<ValueData>(), new SimplePersistedSize(0));

         return pdata;
      }
      catch (InvalidItemStateException e)
      {
         throw new InvalidItemStateException("FATAL: Can't build property path for name " + cname + " id: "
            + getIdentifier(cid) + ". " + e);
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Mixin types description (internal use).
    * 
    */
   public class MixinInfo
   {

      /**
       * OWNEABLE constant.
       */
      static final int OWNEABLE = 0x0001; // bits 0001

      /**
       * PRIVILEGEABLE constant.
       */
      static final int PRIVILEGEABLE = 0x0002; // bits 0010

      /**
       * OWNEABLE_PRIVILEGEABLE constant.
       */
      static final int OWNEABLE_PRIVILEGEABLE = OWNEABLE | PRIVILEGEABLE; // bits 0011

      /**
       * Mixin types.
       */
      final List<InternalQName> mixinTypes;

      /**
       * oexo:owneable flag.
       */
      final boolean owneable;

      /**
       * exo:privilegeable flag.
       */
      final boolean privilegeable;

      /**
       * Parent Id.
       */
      final String parentId = null;

      /**
       * MixinInfo constructor.
       * 
       * @param mixinTypes
       *          mixin types
       * @param owneable
       *          exo:owneable flag
       * @param privilegeable
       *          exo:privilegeable flag
       */
      public MixinInfo(List<InternalQName> mixinTypes, boolean owneable, boolean privilegeable)
      {
         this.mixinTypes = mixinTypes;
         this.owneable = owneable;
         this.privilegeable = privilegeable;
      }

      /**
       * Return Mixin names array.
       * 
       * @return InternalQName[] Mixin names array
       */
      public InternalQName[] mixinNames()
      {
         if (mixinTypes != null)
         {
            InternalQName[] mns = new InternalQName[mixinTypes.size()];
            mixinTypes.toArray(mns);
            return mns;
         }
         else
         {
            return new InternalQName[0];
         }
      }

      /**
       * Tell is exo:privilegeable.
       * 
       * @return boolean
       */
      public boolean hasPrivilegeable()
      {
         return privilegeable;
      }

      /**
       * Tell is exo:owneable.
       * 
       * @return boolean
       */
      public boolean hasOwneable()
      {
         return owneable;
      }

      public String getParentId()
      {
         return parentId;
      }
   }

   /**
    * Read mixins from database.
    * 
    * @param cid
    *          - Item id (internal)
    * @return MixinInfo
    * @throws SQLException
    *           database error
    * @throws IllegalNameException
    *           if nodetype name in mixin record is wrong
    */
   protected MixinInfo readMixins(String cid) throws SQLException, IllegalNameException
   {
      ResultSet mtrs = findPropertyByName(cid, Constants.JCR_MIXINTYPES.getAsString());

      try
      {
         List<InternalQName> mts = null;
         boolean owneable = false;
         boolean privilegeable = false;
         if (mtrs.next())
         {
            mts = new ArrayList<InternalQName>();
            do
            {
               byte[] mxnb = mtrs.getBytes(COLUMN_VDATA);
               if (mxnb != null)
               {
                  InternalQName mxn = InternalQName.parse(new String(mxnb));
                  mts.add(mxn);

                  if (!privilegeable && Constants.EXO_PRIVILEGEABLE.equals(mxn))
                  {
                     privilegeable = true;
                  }
                  else if (!owneable && Constants.EXO_OWNEABLE.equals(mxn))
                  {
                     owneable = true;
                  }
               } // else, if SQL NULL - skip it
            }
            while (mtrs.next());
         }

         return new MixinInfo(mts, owneable, privilegeable);
      }
      finally
      {
         try
         {
            mtrs.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the ResultSet: " + e.getMessage());
         }
      }
   }

   /**
    * Return permission values or throw an exception. We assume the node is mix:privilegeable.
    * 
    * @param cid
    *          Node id
    * @return list of ACL entries
    * @throws SQLException
    *           database error
    * @throws IllegalACLException
    *           if property exo:permissions is not found for node
    */
   protected List<AccessControlEntry> readACLPermisions(String cid) throws SQLException, IllegalACLException
   {
      List<AccessControlEntry> naPermissions = new ArrayList<AccessControlEntry>();
      ResultSet exoPerm = findPropertyByName(cid, Constants.EXO_PERMISSIONS.getAsString());
      try
      {
         if (exoPerm.next())
         {
            do
            {
               StringTokenizer parser =
                  new StringTokenizer(new String(exoPerm.getBytes(COLUMN_VDATA)), AccessControlEntry.DELIMITER);
               naPermissions.add(new AccessControlEntry(parser.nextToken(), parser.nextToken()));
            }
            while (exoPerm.next());

            return naPermissions;
         }
         else
         {
            throw new IllegalACLException("Property exo:permissions is not found for node with id: "
               + getIdentifier(cid));
         }
      }
      finally
      {
         try
         {
            exoPerm.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the ResultSet: " + e.getMessage());
         }
      }
   }

   /**
    * Return owner value or throw an exception. We assume the node is mix:owneable.
    * 
    * @param cid
    *          Node id
    * @return ACL owner
    * @throws SQLException
    *           database error
    * @throws IllegalACLException
    *           Property exo:owner is not found for node
    */
   protected String readACLOwner(String cid) throws SQLException, IllegalACLException
   {
      ResultSet exoOwner = findPropertyByName(cid, Constants.EXO_OWNER.getAsString());
      try
      {
         if (exoOwner.next())
         {
            return new String(exoOwner.getBytes(COLUMN_VDATA));
         }
         else
         {
            throw new IllegalACLException("Property exo:owner is not found for node with id: " + getIdentifier(cid));
         }
      }
      finally
      {
         try
         {
            exoOwner.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the ResultSet: " + e.getMessage());
         }
      }
   }

   /**
    * Load NodeData record.
    * 
    * @param parentPath
    *          parent path
    * @param cname
    *          Node name
    * @param cid
    *          Node id
    * @param cpid
    *          Node parent id
    * @param cindex
    *          Node index
    * @param cversion
    *          Node persistent version
    * @param cnordernumb
    *          Node order number
    * @param parentACL
    *          Node parent ACL
    * @return PersistedNodeData
    * @throws RepositoryException
    *           Repository error
    * @throws SQLException
    *           database error
    */
   protected PersistedNodeData loadNodeRecord(QPath parentPath, String cname, String cid, String cpid, int cindex,
      int cversion, int cnordernumb, AccessControlList parentACL) throws RepositoryException, SQLException
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
         ResultSet ptProp = findPropertyByName(cid, Constants.JCR_PRIMARYTYPE.getAsString());
         try
         {

            if (!ptProp.next())
            {
               throw new PrimaryTypeNotFoundException("FATAL ERROR primary type record not found. Node "
                  + qpath.getAsString() + ", id " + cid + ", container " + this.containerConfig.containerName, null);
            }

            byte[] data = ptProp.getBytes(COLUMN_VDATA);
            InternalQName ptName = InternalQName.parse(new String((data != null ? data : new byte[]{})));

            // MIXIN
            MixinInfo mixins = readMixins(cid);

            // ACL
            AccessControlList acl; // NO DEFAULT values!

            if (mixins.hasOwneable())
            {
               // has own owner
               if (mixins.hasPrivilegeable())
               {
                  // and permissions
                  acl = new AccessControlList(readACLOwner(cid), readACLPermisions(cid));
               }
               else if (parentACL != null)
               {
                  // use permissions from existed parent
                  acl =
                     new AccessControlList(readACLOwner(cid), parentACL.hasPermissions()
                        ? parentACL.getPermissionEntries() : null);
               }
               else
               {
                  // have to search nearest ancestor permissions in ACL manager
                  // acl = new AccessControlList(readACLOwner(cid), traverseACLPermissions(cpid));
                  acl = new AccessControlList(readACLOwner(cid), null);
               }
            }
            else if (mixins.hasPrivilegeable())
            {
               // has own permissions
               if (mixins.hasOwneable())
               {
                  // and owner
                  acl = new AccessControlList(readACLOwner(cid), readACLPermisions(cid));
               }
               else if (parentACL != null)
               {
                  // use owner from existed parent
                  acl = new AccessControlList(parentACL.getOwner(), readACLPermisions(cid));
               }
               else
               {
                  // have to search nearest ancestor owner in ACL manager
                  // acl = new AccessControlList(traverseACLOwner(cpid), readACLPermisions(cid));
                  acl = new AccessControlList(null, readACLPermisions(cid));
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
               ptName, mixins.mixinNames(), acl);
         }
         catch (IllegalACLException e)
         {
            throw new RepositoryException("FATAL ERROR Node " + getIdentifier(cid) + " " + qpath.getAsString()
               + " has wrong formed ACL. ", e);
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
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Load PropertyData record.
    * 
    * @param parentPath
    *          parent path
    * @param cname
    *          Property name
    * @param cid
    *          Property id
    * @param cpid
    *          Property parent id
    * @param cversion
    *          Property persistent verison
    * @param cptype
    *          Property type
    * @param cpmultivalued
    *          Property multivalued status
    * @return PersistedPropertyData
    * @throws RepositoryException
    *           Repository error
    * @throws SQLException
    *           database error
    * @throws IOException
    *           I/O error
    */
   protected PersistedPropertyData loadPropertyRecord(QPath parentPath, String cname, String cid, String cpid,
      int cversion, int cptype, boolean cpmultivalued) throws RepositoryException, SQLException, IOException
   {

      // NOTE: cpid never should be null or root parent (' ')

      try
      {
         QPath qpath =
            QPath.makeChildPath(parentPath == null ? traverseQPath(cpid) : parentPath, InternalQName.parse(cname));

         String identifier = getIdentifier(cid);

         List<ValueDataWrapper> data = readValues(cid, cptype, identifier, cversion);

         long size = 0;
         List<ValueData> values = new ArrayList<ValueData>();
         for (ValueDataWrapper vdDataWrapper : data)
         {
            values.add(vdDataWrapper.value);
            size += vdDataWrapper.size;
         }

         PersistedPropertyData pdata =
            new PersistedPropertyData(identifier, qpath, getIdentifier(cpid), cversion, cptype, cpmultivalued, values,
               new SimplePersistedSize(size));

         return pdata;
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Delete Property Values.
    * 
    * @param cid
    *          Property id
    * @param pdata
    *          PropertyData
    * @param update
    *          boolean true if it's delete-add sequence (update operation)
    * @param sizeHandler
    *          accumulates changed size
    * @throws IOException
    *           i/O error
    * @throws SQLException
    *           if database error occurs
    * @throws RepositoryException 
    * @throws InvalidItemStateException 
    */
   private void deleteValues(String cid, PropertyData pdata, boolean update, ChangedSizeHandler sizeHandler)
      throws IOException, SQLException, RepositoryException, InvalidItemStateException
   {
      Set<String> storages = new HashSet<String>();

      final ResultSet valueRecords = findValueStorageDescAndSize(cid);
      try
      {
         if (valueRecords.next())
         {
            do
            {
               final String storageId = valueRecords.getString(COLUMN_VSTORAGE_DESC);
               if (!valueRecords.wasNull())
               {
                  storages.add(storageId);
               }
               else
               {
                  sizeHandler.accumulatePrevSize(valueRecords.getLong(1));
               }
            }
            while (valueRecords.next());
         }

         // delete all values in value storage
         for (String storageId : storages)
         {
            final ValueIOChannel channel = this.containerConfig.valueStorageProvider.getChannel(storageId);
            try
            {
               sizeHandler.accumulatePrevSize(channel.getValueSize(pdata.getIdentifier()));

               channel.delete(pdata.getIdentifier());
               valueChanges.add(channel);
            }
            finally
            {
               channel.close();
            }
         }

         // delete all Values in database
         deleteValueData(cid);
      }
      finally
      {
         JDBCUtils.freeResources(valueRecords, null, null);
      }
   }

   /**
    * Read Property Values.
    * 
    * @param identifier
    *          property identifier
    * @param cid
    *          Property id
    * @param pdata
    *          PropertyData
    * @return list of ValueData
    * @throws IOException
    *           i/O error
    * @throws SQLException
    *           if database errro occurs
    * @throws ValueStorageNotFoundException
    *           if no such storage found with Value storageId
    */
   private List<ValueDataWrapper> readValues(String cid, int cptype, String identifier, int cversion)
      throws IOException, SQLException, ValueStorageNotFoundException
   {
      List<ValueDataWrapper> data = new ArrayList<ValueDataWrapper>();

      final ResultSet valueRecords = findValuesByPropertyId(cid);
      try
      {
         while (valueRecords.next())
         {
            final int orderNum = valueRecords.getInt(COLUMN_VORDERNUM);
            final String storageId = valueRecords.getString(COLUMN_VSTORAGE_DESC);

            ValueDataWrapper vdWrapper =
               valueRecords.wasNull() ? ValueDataUtil.readValueData(cid, cptype, orderNum, cversion,
                  valueRecords.getBinaryStream(COLUMN_VDATA), containerConfig.spoolConfig) : readValueData(identifier,
                  orderNum, cptype, storageId);

            data.add(vdWrapper);
         }
      }
      finally
      {
         try
         {
            valueRecords.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can't close the ResultSet: " + e.getMessage());
         }
      }

      return data;
   }

   /**
    * Read ValueData from External Storage.
    * 
    * @param pdata
    *          PropertyData
    * @param orderNumber
    *          Value order number
    * @param type
    *          property type         
    * @param storageId
    *          external Value storage id
    * @return ValueData
    * @throws SQLException
    *           database error
    * @throws IOException
    *           I/O error
    * @throws ValueStorageNotFoundException
    *           if no such storage found with Value storageId
    */
   protected ValueDataWrapper readValueData(String identifier, int orderNumber, int type, String storageId)
      throws SQLException, IOException, ValueStorageNotFoundException
   {
      ValueIOChannel channel = this.containerConfig.valueStorageProvider.getChannel(storageId);
      try
      {
         return channel.read(identifier, orderNumber, type, containerConfig.spoolConfig);
      }
      finally
      {
         channel.close();
      }
   }

   /**
    * Add Values to Property record.
    * 
    * @param data
    *          PropertyData
    * @param sizeHandler
    *          accumulates size changing
    * @throws SQLException
    *           database error
    * @throws IOException
    *           I/O error
    * @throws RepositoryException if Value data large of JDBC accepted (Integer.MAX_VALUE)
    */
   protected void addValues(String cid, PropertyData data, ChangedSizeHandler sizeHandler) throws IOException,
      SQLException, RepositoryException
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
                     cid + i + "." + data.getPersistedVersion());
               try
               {
                  long vlen = WRITE_VALUE_HELPER.writeStreamedValue(swapFile, streamData);
                  if (vlen <= Integer.MAX_VALUE)
                  {
                     streamLength = (int)vlen;
                  }
                  else
                  {
                     throw new RepositoryException("Value data large of allowed by JDBC (Integer.MAX_VALUE) " + vlen
                        + ". Property " + data.getQPath().getAsString());
                  }
               }
               finally
               {
                  swapFile.spoolDone();
               }

               stream = streamData.getAsStream();
            }
            storageId = null;
            sizeHandler.accumulateNewSize(streamLength);
         }
         else
         {
            // write Value in external VS
            valueChanges.add(channel);
            channel.write(data.getIdentifier(), vd, sizeHandler);
            storageId = channel.getStorageId();
            stream = null;
            streamLength = 0;
         }
         addValueData(cid, i, stream, streamLength, storageId);
      }
   }

   /**
    * Build node data and its properties data from temporary stored info.
    */
   protected NodeDataIndexing createNodeDataIndexing(TempNodeData tempNode) throws RepositoryException, SQLException,
      IOException, IllegalNameException
   {
      String parentCid;
      QPath parentPath;

      if (tempNode.cpid.equals(Constants.ROOT_PARENT_UUID))
      {
         // root node
         parentCid = null;
         parentPath = Constants.ROOT_PATH;
      }
      else
      {
         parentCid = tempNode.cpid;
         parentPath =
            QPath.makeChildPath(traverseQPath(tempNode.cpid), InternalQName.parse(tempNode.cname), tempNode.cindex);
      }

      // primary type if exists in the list of properties
      SortedSet<TempPropertyData> ptTempProp = tempNode.properties.get(Constants.JCR_PRIMARYTYPE.getAsString());
      ValueData ptValueData = ptTempProp.first().getValueData();
      long ptSize = ptTempProp.first().getSize();
      InternalQName ptName = InternalQName.parse(ValueDataUtil.getString(ptValueData));

      // mixins if exist in the list of properties
      List<ValueData> mixinsData = new ArrayList<ValueData>();
      List<InternalQName> mixins = new ArrayList<InternalQName>();
      long mixinsSize = 0;

      Set<TempPropertyData> mixinsTempProps = tempNode.properties.get(Constants.JCR_MIXINTYPES.getAsString());
      if (mixinsTempProps != null)
      {
         for (TempPropertyData mxnb : mixinsTempProps)
         {
            ValueData vdata = mxnb.getValueData();

            mixinsData.add(vdata);
            mixins.add(InternalQName.parse(ValueDataUtil.getString(vdata)));

            mixinsSize += mxnb.getSize();
         }
      }

      // build node data. No need to load ACL. The node will be pushed directly for reindexing. 
      NodeData nodeData =
         new PersistedNodeData(getIdentifier(tempNode.cid), parentPath, getIdentifier(parentCid), tempNode.cversion,
            tempNode.cnordernumb, ptName, mixins.toArray(new InternalQName[mixins.size()]), null);

      Map<String, PropertyData> childProps = new HashMap<String, PropertyData>();
      for (String propName : tempNode.properties.keySet())
      {
         ExtendedTempPropertyData prop = (ExtendedTempPropertyData)tempNode.properties.get(propName).first();

         String identifier = getIdentifier(prop.id);
         QPath qpath = QPath.makeChildPath(parentPath, InternalQName.parse(prop.name));

         List<ValueData> values = new ArrayList<ValueData>();
         long size = 0;

         if (propName.equals(Constants.JCR_PRIMARYTYPE.getAsString()))
         {
            values.add(ptValueData);
            size = ptSize;
         }
         else if (propName.equals(Constants.JCR_MIXINTYPES.getAsString()))
         {
            values = mixinsData;
            size = mixinsSize;
         }
         else
         {
            for (TempPropertyData tempProp : tempNode.properties.get(propName))
            {
               ExtendedTempPropertyData exTempProp = (ExtendedTempPropertyData)tempProp;

               values.add(exTempProp.getValueData());
               size += exTempProp.getSize();
            }
         }

         // build property data
         PropertyData pdata =
            new PersistedPropertyData(identifier, qpath, tempNode.cid, prop.version, prop.type, prop.multi, values,
               new SimplePersistedSize(size));

         childProps.put(propName, pdata);
      }

      return new NodeDataIndexing(nodeData, childProps);
   }

   /**
    * Class needed temporary to store node data info. 
    */
   protected class TempNodeData
   {
      public String cid;

      public String cname;

      public int cversion;

      public String cpid;

      public int cindex;

      public int cnordernumb;

      public Map<String, SortedSet<TempPropertyData>> properties = new HashMap<String, SortedSet<TempPropertyData>>();

      public TempNodeData(ResultSet item) throws SQLException
      {
         cid = item.getString(COLUMN_ID);
         cname = item.getString(COLUMN_NAME);
         cversion = item.getInt(COLUMN_VERSION);
         cpid = item.getString(COLUMN_PARENTID);
         cindex = item.getInt(COLUMN_INDEX);
         cnordernumb = item.getInt(COLUMN_NORDERNUM);
      }
   }

   /**
    * Class needs temporary to store value info.
    */
   protected class TempPropertyData implements Comparable<TempPropertyData>
   {
      protected final int orderNum;

      protected ValueData data;

      protected long size;

      /**
       * Constructor TempPropertyData.
       */
      public TempPropertyData(ResultSet item) throws SQLException, IOException, ValueStorageNotFoundException
      {
         orderNum = item.getInt(COLUMN_VORDERNUM);
         readData(item);
      }

      protected void readData(ResultSet item) throws SQLException, ValueStorageNotFoundException, IOException
      {
         byte[] internalData = item.getBytes(COLUMN_VDATA);

         data = new ByteArrayPersistedValueData(orderNum, internalData);
         size = internalData.length;
      }

      public int compareTo(TempPropertyData o)
      {
         return orderNum - o.orderNum;
      }

      public ValueData getValueData()
      {
         return data;
      }

      public long getSize()
      {
         return size;
      }
   }

   /**
    * Class needs temporary to store whole property data info.
    */
   protected class ExtendedTempPropertyData extends TempPropertyData
   {
      protected final String id;

      protected final String name;

      protected final int version;

      protected final boolean multi;

      protected final int type;

      protected final String storage_desc;

      public ExtendedTempPropertyData(ResultSet item) throws SQLException, ValueStorageNotFoundException, IOException
      {
         super(item);

         id = item.getString("P_ID");
         name = item.getString("P_NAME");
         version = item.getInt("P_VERSION");
         type = item.getInt(COLUMN_PTYPE);
         multi = item.getBoolean("P_MULTIVALUED");
         storage_desc = item.getString(COLUMN_VSTORAGE_DESC);
      }

      protected void readData(ResultSet item) throws SQLException, ValueStorageNotFoundException, IOException
      {
         InputStream is = item.getBinaryStream(COLUMN_VDATA);

         ValueDataWrapper vdWrapper =
            storage_desc == null ? ValueDataUtil.readValueData(id, type, orderNum, version, is,
               containerConfig.spoolConfig) : readValueData(getIdentifier(id), orderNum, type, storage_desc);

         data = vdWrapper.value;
         size = vdWrapper.size;
      }
   }

   /**
    * The comparator used to sort the value data
    */
   protected static Comparator<ValueData> COMPARATOR_VALUE_DATA = new Comparator<ValueData>()
   {

      public int compare(ValueData vd1, ValueData vd2)
      {
         return vd1.getOrderNumber() - vd2.getOrderNumber();
      }
   };

   protected abstract int addNodeRecord(NodeData data) throws SQLException, InvalidItemStateException,
      RepositoryException;

   protected abstract int addPropertyRecord(PropertyData prop) throws SQLException, InvalidItemStateException,
      RepositoryException;

   protected abstract ResultSet findItemByIdentifier(String identifier) throws SQLException;

   protected abstract ResultSet findPropertyByName(String parentId, String name) throws SQLException;

   protected abstract ResultSet findItemByName(String parentId, String name, int index) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifier(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findLastOrderNumberByParentIdentifier(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildNodesCountByParentIdentifier(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifier(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifier(String parentCid, int fromOrderNum, int toOrderNum)
      throws SQLException;

   protected abstract int addReference(PropertyData data) throws SQLException, IOException, InvalidItemStateException,
      RepositoryException;

   protected abstract int renameNode(NodeData data) throws SQLException, InvalidItemStateException, RepositoryException;

   protected abstract int deleteReference(String propertyIdentifier) throws SQLException, InvalidItemStateException,
      RepositoryException;

   /**
    * Deletes [http://www.jcp.org/jcr/1.0]lockOwner and [http://www.jcp.org/jcr/1.0]lockIsDeep
    * properties directly from DB.
    * @throws RepositoryException 
    * @throws InvalidItemStateException 
    */
   protected abstract void deleteLockProperties() throws SQLException, InvalidItemStateException, RepositoryException;

   protected abstract ResultSet findReferences(String nodeIdentifier) throws SQLException;

   protected abstract int deleteItemByIdentifier(String identifier) throws SQLException, InvalidItemStateException,
      RepositoryException;

   protected abstract int updateNodeByIdentifier(int version, int index, int orderNumb, String identifier)
      throws SQLException, InvalidItemStateException, RepositoryException;

   protected abstract int updatePropertyByIdentifier(int version, int type, String identifier) throws SQLException,
      InvalidItemStateException, RepositoryException;

   protected abstract ResultSet findNodesCount() throws SQLException;

   // -------- values processing ------------
   protected abstract int addValueData(String cid, int orderNumber, InputStream stream, int streamLength,
      String storageId) throws SQLException, InvalidItemStateException, RepositoryException;

   protected abstract int deleteValueData(String cid) throws SQLException, InvalidItemStateException,
      RepositoryException;

   protected abstract ResultSet findValuesByPropertyId(String cid) throws SQLException;

   protected abstract ResultSet findMaxPropertyVersion(String parentId, String name, int index) throws SQLException;

   protected abstract ResultSet findWorkspaceDataSize() throws SQLException;

   protected abstract ResultSet findWorkspacePropertiesOnValueStorage() throws SQLException;

   protected abstract ResultSet findNodeDataSize(String nodeIdentifier) throws SQLException;

   protected abstract ResultSet findNodePropertiesOnValueStorage(String parentId) throws SQLException;

   protected abstract ResultSet findValueStorageDescAndSize(String cid) throws SQLException;
}
