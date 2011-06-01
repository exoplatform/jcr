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
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.IllegalACLException;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;
import org.exoplatform.services.jcr.storage.value.ValueIOChannel;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
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
    * FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE;
    */
   protected String FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE;

   /**
    * FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE;
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
    * UPDATE_VALUE.
    */
   protected String UPDATE_VALUE;

   protected PreparedStatement findNodesByParentIdCQ;

   protected PreparedStatement findPropertiesByParentIdCQ;

   protected PreparedStatement findNodeMainPropertiesByParentIdentifierCQ;

   protected PreparedStatement findItemQPathByIdentifierCQ;

   protected PreparedStatement findPropertyById;

   protected PreparedStatement deleteValueDataByOrderNum;

   protected PreparedStatement updateValue;

   protected Statement findPropertiesByParentIdAndComplexPatternCQ;

   protected Statement findNodesByParentIdAndComplexPatternCQ;

   /**
     * JDBCStorageConnection constructor.
     * 
     * @param dbConnection
     *          JDBC connection
     * @param containerName
     *          Workspace container name
     * @param valueStorageProvider
     *          External Value Storage provider
     * @param maxBufferSize
     *          maximum buffer size (configuration)
     * @param swapDirectory
     *          swap directory (configuration)
     * @param swapCleaner
     *          swap cleaner (FileCleaner)
     * @throws SQLException
     *           database error
     */
   protected CQJDBCStorageConnection(Connection dbConnection, boolean readOnly, String containerName,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      throws SQLException
   {
      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
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
               LOG.error("Can't close the ResultSet: " + e);
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
               LOG.error("Can't close the ResultSet: " + e);
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
      try
      {
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
            if (prevType == PropertyType.REFERENCE)
            {
               deleteReference(cid);
            }

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
         if (rs != null)
         {
            try
            {
               rs.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
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
         ValueIOChannel channel = valueStorageProvider.getApplicableChannel(data, i);
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

               SwapFile swapFile = SwapFile.get(swapDirectory, cid + i + "." + data.getPersistedVersion());
               try
               {
                  writeValueHelper.writeStreamedValue(swapFile, streamData);
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
      throws ValueStorageNotFoundException, IOException, SQLException
   {
      for (String storageId : storageDescs)
      {
         final ValueIOChannel channel = valueStorageProvider.getChannel(storageId);
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
                     QPath.makeChildPath(parentPath == null ? traverseQPath(cpid) : parentPath, InternalQName
                        .parse(cname));
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
                        resultSet.wasNull() ? readValueData(cid, orderNum, cversion, resultSet
                           .getBinaryStream(COLUMN_VDATA)) : readValueData(identifier, orderNum, storageId);
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
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
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
                        resultSet.wasNull() ? readValueData(cid, orderNum, cversion,
                           resultSet.getBinaryStream(COLUMN_VDATA)) : readValueData(identifier, orderNum, storageId);
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
               LOG.error("Can't close the ResultSet: " + e);
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
            StringTokenizer parser = new StringTokenizer(new String(value.data), AccessControlEntry.DELIMITER);
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
         return new String(ownerValues.first().data);
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
            LOG.error("Can't close the ResultSet: " + e);
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
            qpath = QPath.makeChildPath(parentPath, qname, cindex);
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
               qpath = QPath.makeChildPath(traverseQPath(cpid), qname, cindex);
               parentCid = cpid;
            }
         }

         // PRIMARY
         SortedSet<TempPropertyData> primaryType = properties.get(Constants.JCR_PRIMARYTYPE.getAsString());
         if (primaryType == null || primaryType.isEmpty())
         {
            throw new PrimaryTypeNotFoundException("FATAL ERROR primary type record not found. Node "
               + qpath.getAsString() + ", id " + cid + ", container " + this.containerName, null);
         }

         byte[] data = primaryType.first().data;
         InternalQName ptName = InternalQName.parse(new String((data != null ? data : new byte[]{})));

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
               InternalQName mxn = InternalQName.parse(new String(mxnb.data));
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
                     new AccessControlList(readACLOwner(cid, properties), parentACL.hasPermissions() ? parentACL
                        .getPermissionEntries() : null);
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
                     new AccessControlList(parentACL.getOwner(), parentACL.hasPermissions() ? parentACL
                        .getPermissionEntries() : null);
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

            QPathEntry qpe1 =
               new QPathEntry(InternalQName.parse(result.getString(COLUMN_NAME)), result.getInt(COLUMN_INDEX));
            boolean isChild = caid.equals(result.getString(COLUMN_ID));
            caid = result.getString(COLUMN_PARENTID);
            if (result.next())
            {
               QPathEntry qpe2 =
                  new QPathEntry(InternalQName.parse(result.getString(COLUMN_NAME)), result.getInt(COLUMN_INDEX));
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
                  LOG.error("Can't close the ResultSet: " + e);
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

   /**
    * {@inheritDoc}
    */
   @Override
   protected void closeStatements()
   {
      super.closeStatements();

      try
      {
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
      }
      catch (SQLException e)
      {
         LOG.error("Can't close the Statement: " + e);
      }
   }

   protected abstract ResultSet findItemQPathByIdentifierCQ(String identifier) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier,
      List<QPathEntryFilter> patternList) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier,
      List<QPathEntryFilter> patternList) throws SQLException;

   protected abstract ResultSet findNodeMainPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findPropertyById(String id) throws SQLException;

   protected abstract int deleteValueDataByOrderNum(String id, int orderNum) throws SQLException;

   protected abstract int updateValueData(String cid, int i, InputStream stream, int streamLength, String storageId)
      throws SQLException;
}
