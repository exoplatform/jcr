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
package org.exoplatform.services.jcr.impl.storage.jdbc.indexing;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedNodeData;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.NodeDataIndexing;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.query.NodeDataIndexingIterator;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CleanableFilePersistedValueData;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.ValueStorageNotFoundException;
import org.exoplatform.services.jcr.impl.storage.value.fs.operations.ValueFileIOHelper;
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
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.jcr.InvalidItemStateException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 1 02 2011
 * 
 * Iterator for fetching NodeData from database with all properties and its values.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JdbcIndexingDataIterator.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JdbcNodeDataIndexingIterator extends DBConstants implements NodeDataIndexingIterator
{

   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JdbcIndexingDataIterator");

   /**
    * Connection to the database. Should be released on close.
    */
   private final Connection jdbcConn;

   /**
    * Temporary used prepared statement for query execution. Should be released on close.
    */
   private PreparedStatement st = null;

   /**
    * Temporary used prepared statement. Should be released on close.
    */
   private PreparedStatement findItemQPathByIdentifierCQ;

   /**
    * Temporary used result set during fetching data. Should be released on close.
    */
   private ResultSet resultSet = null;

   /**
    * Connection to the database. Should be released on close.
    */
   private TempNodeData data = null;

   /**
    * Indicates if mulit db is used or not.
    */
   private final boolean multiDb;

   /**
    * Container name.
    */
   private final String containerName;

   /**
    * The File Cleaner.
    */
   private final FileCleaner swapCleaner;

   /**
    * Maximum buffer size.
    */
   private final int maxBufferSize;

   /**
    * Swap directory.
    */
   private final File swapDirectory;

   /**
    * Value storage provider.
    */
   private final ValueStoragePluginProvider valueStorageProvider;

   /**
    * The next node data to return in next() method.
    */
   private NodeDataIndexing nextNode = null;

   /**
    * Search query for single db.
    */
   private static final String FIND_NODES_SINGLE_DB =
      "select I.ID AS N_ID, I.PARENT_ID AS N_PARENT_ID, I.NAME AS N_NAME, I.VERSION AS N_VERSION, I.I_INDEX AS N_I_INDEX, I.N_ORDER_NUM, "
         + "P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED, "
         + "V.DATA, V.ORDER_NUM,  V.STORAGE_DESC"
         + " from JCR_SITEM I, JCR_SITEM P, JCR_SVALUE V where I.I_CLASS=1 and I.CONTAINER_NAME=? and"
         + " P.I_CLASS=2 and P.CONTAINER_NAME=? and P.PARENT_ID=I.ID" + " and V.PROPERTY_ID=P.ID order by N_ID";

   /**
    * Search query for multi db.
    */
   private static final String FIND_NODES_MULTI_DB =
      "select I.ID AS N_ID, I.PARENT_ID AS N_PARENT_ID, I.NAME AS N_NAME, I.VERSION AS N_VERSION, I.I_INDEX AS N_I_INDEX, I.N_ORDER_NUM, "
         + "P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED, "
         + "V.DATA, V.ORDER_NUM, V.STORAGE_DESC"
         + " from JCR_MITEM I, JCR_MITEM P, JCR_MVALUE V where I.I_CLASS=1 and"
         + " P.I_CLASS=2 and V.PROPERTY_ID=P.ID order by N_ID";

   /**
    * Constructor JdbcIndexingDataIterator.
    * 
    */
   public JdbcNodeDataIndexingIterator(Connection jdbcConn, boolean multiDb, String containerName, FileCleaner swapCleaner,
      int maxBufferSize, File swapDirectory, ValueStoragePluginProvider valueStorageProvider) throws SQLException,
      PrimaryTypeNotFoundException, InvalidItemStateException, ValueStorageNotFoundException, IllegalNameException,
      IOException
   {
      this.jdbcConn = jdbcConn;
      this.multiDb = multiDb;
      this.containerName = containerName;
      this.swapCleaner = swapCleaner;
      this.maxBufferSize = maxBufferSize;
      this.swapDirectory = swapDirectory;
      this.valueStorageProvider = valueStorageProvider;

      String sql = multiDb ? FIND_NODES_MULTI_DB : FIND_NODES_SINGLE_DB;
      st = jdbcConn.prepareStatement(sql);

      if (!multiDb)
      {
         st.setString(1, containerName);
         st.setString(2, containerName);
      }
      resultSet = st.executeQuery();

      this.nextNode = readNext();
   }

   /**
    * {@inheritDoc}
    */
   public boolean hasNext()
   {
      return this.nextNode != null;
   }

   /**
    * {@inheritDoc}
    */
   public NodeDataIndexing next() throws IOException
   {
      NodeDataIndexing current = this.nextNode;

      try
      {
         this.nextNode = readNext();
      }
      catch (PrimaryTypeNotFoundException e)
      {
         throw new IOException(e);
      }
      catch (InvalidItemStateException e)
      {
         throw new IOException(e);
      }
      catch (ValueStorageNotFoundException e)
      {
         throw new IOException(e);
      }
      catch (SQLException e)
      {
         throw new IOException(e);
      }
      catch (IllegalNameException e)
      {
         throw new IOException(e);
      }

      return current;
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws IOException
   {
      try
      {
         if (resultSet != null)
         {
            resultSet.close();
         }

         if (st != null)
         {
            st.close();
         }
         
         if (findItemQPathByIdentifierCQ != null)
         {
            findItemQPathByIdentifierCQ.close();
         }

         jdbcConn.close();
      }
      catch (SQLException e)
      {
         throw new IOException(e);
      }
   }

   /**
    * Read next node from database. 
    * 
    * @return NodeDataIndexing 
    */
   private NodeDataIndexing readNext() throws PrimaryTypeNotFoundException, InvalidItemStateException,
      ValueStorageNotFoundException, SQLException, IllegalNameException, IOException
   {
      while (resultSet.next())
      {
         if (data == null)
         {
            data = new TempNodeData(resultSet);
            readTempPropertyData();
         }
         else if (!resultSet.getString("N_ID").equals(data.cid))
         {
            NodeDataIndexing node = createNodeData(data);

            data = new TempNodeData(resultSet);
            readTempPropertyData();

            return node;
         }
         else
         {
            readTempPropertyData();
         }
      }

      if (data != null)
      {
         NodeDataIndexing node = createNodeData(data);
         data = null;

         return node;
      }

      return null;
   }

   /**
    * Read temporary property data.
    * 
    * @throws SQLException 
    */
   private void readTempPropertyData() throws SQLException
   {
      String key = resultSet.getString("P_NAME");

      SortedSet<TempPropertyData> values = data.properties.get(key);
      if (values == null)
      {
         values = new TreeSet<TempPropertyData>();
         data.properties.put(key, values);
      }

      values.add(new TempPropertyData(resultSet));
   }

   /**
    * Build node data and its properties data from temporary stored info.
    * 
    * @return NodeDataIndexing
    */
   private NodeDataIndexing createNodeData(TempNodeData tempNode) throws IllegalNameException,
      InvalidItemStateException, SQLException, PrimaryTypeNotFoundException, IOException, ValueStorageNotFoundException
   {
      QPath parentPath;
      String parentCid;

      if (tempNode.cpid.equals(Constants.ROOT_PARENT_UUID))
      {
         // root node
         parentPath = Constants.ROOT_PATH;
         parentCid = null;
      }
      else
      {
         parentPath =
            QPath.makeChildPath(traverseQPath(tempNode.cpid), InternalQName.parse(tempNode.cname), tempNode.cindex);
         parentCid = tempNode.cpid;
      }

      // primary type
      SortedSet<TempPropertyData> primaryTypeTempProp = tempNode.properties.get(Constants.JCR_PRIMARYTYPE.getAsString());
      if (primaryTypeTempProp == null)
      {
         throw new PrimaryTypeNotFoundException("FATAL ERROR primary type record not found. Node "
            + parentPath.getAsString() + ", id " + tempNode.cid + ", container " + this.containerName, null);
      }

      ByteArrayInputStream ba = ((ByteArrayInputStream)primaryTypeTempProp.first().cdata);
      byte[] data = new byte[ba.available()];
      ba.read(data);

      primaryTypeTempProp.first().cdata = new ByteArrayInputStream(data);

      InternalQName ptName =
         InternalQName.parse(new String((data != null ? data : new byte[]{}), Constants.DEFAULT_ENCODING));

      // mixins
      List<InternalQName> mixins = new ArrayList<InternalQName>();
      Set<TempPropertyData> mixinsTempProps = tempNode.properties.get(Constants.JCR_MIXINTYPES.getAsString());
      if (mixinsTempProps != null)
      {

         for (TempPropertyData mxnb : mixinsTempProps)
         {
            ba = ((ByteArrayInputStream)mxnb.cdata);
            data = new byte[ba.available()];
            ba.read(data);

            mxnb.cdata = new ByteArrayInputStream(data);

            mixins.add(InternalQName.parse(new String(data, Constants.DEFAULT_ENCODING)));
         }
      }

      // build node data
      NodeData nodeData =
         new PersistedNodeData(getIdentifier(tempNode.cid), parentPath, getIdentifier(parentCid), tempNode.cversion,
            tempNode.cnordernumb, ptName, mixins.toArray(new InternalQName[mixins.size()]), null);

      List<PropertyData> childProps = new ArrayList<PropertyData>();

      for (String propName : tempNode.properties.keySet())
      {
         TempPropertyData prop = tempNode.properties.get(propName).first();
         String identifier = getIdentifier(prop.cid);

         // read values
         List<ValueData> valueData = new ArrayList<ValueData>();
         for (TempPropertyData tempProp : tempNode.properties.get(propName))
         {
            ValueData vdata =
               tempProp.cstorage_desc == null ? readValueData(tempProp.cid, tempProp.corderNum, tempProp.cversion,
                  tempProp.cdata) : readValueData(identifier, tempProp.corderNum, tempProp.cstorage_desc);

            valueData.add(vdata);
         }
         Collections.sort(valueData, COMPARATOR_VALUE_DATA);

         QPath qpath = QPath.makeChildPath(parentPath, InternalQName.parse(prop.cname));

         // build property data
         PropertyData pdata =
            new PersistedPropertyData(identifier, qpath, tempNode.cid, prop.cversion, prop.ctype, prop.cmulti,
               valueData);

         childProps.add(pdata);
      }

      return new NodeDataIndexing(nodeData, childProps);
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
   private QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
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
            result = findItemQPathByIdentifier(caid);
            if (!result.next())
               throw new InvalidItemStateException("Parent not found, uuid: " + getIdentifier(caid));

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
    * Invoke item identifier from internalId. In case of single db need to
    * remove prefix from internalId.
    * 
    * @param internalId
    *          the internal identifier
    */
   private String getIdentifier(final String internalId)
   {
      if (internalId == null)
         return null;

      return multiDb ? internalId : internalId.substring(containerName.length());
   }

   /**
    * 
    * @param identifier
    * @return
    * @throws SQLException
    */
   private ResultSet findItemQPathByIdentifier(String identifier) throws SQLException
   {
      String findItemQPathByIdentifier;
      if (multiDb)
      {
         findItemQPathByIdentifier =
            "select I.ID, I.PARENT_ID, I.NAME, I.I_INDEX"
               + " from JCR_MITEM I, (SELECT ID, PARENT_ID from JCR_MITEM where ID=?) J"
               + " where I.ID = J.ID or I.ID = J.PARENT_ID";
      }
      else
      {
         findItemQPathByIdentifier =
            "select I.ID, I.PARENT_ID, I.NAME, I.I_INDEX"
               + " from JCR_SITEM I, (SELECT ID, PARENT_ID from JCR_SITEM where ID=?) J"
               + " where I.ID = J.ID or I.ID = J.PARENT_ID";
      }

      if (findItemQPathByIdentifierCQ == null)
      {
         findItemQPathByIdentifierCQ = jdbcConn.prepareStatement(findItemQPathByIdentifier);
      }
      else
      {
         findItemQPathByIdentifierCQ.clearParameters();
      }

      findItemQPathByIdentifierCQ.setString(1, identifier);
      return findItemQPathByIdentifierCQ.executeQuery();
   }

   /**
    * Read ValueData from database.
    * 
    * @param cid
    *          property id
    * @param orderNumber
    *          value order number
    * @param version
    *          persistent version (used for BLOB swapping)
    * @param content
    *          input stream
    * @return ValueData
    * @throws SQLException
    *           database error
    * @throws IOException
    *           I/O error (swap)
    */
   private ValueData readValueData(String cid, int orderNumber, int version, final InputStream content)
      throws SQLException, IOException
   {

      byte[] buffer = new byte[0];
      byte[] spoolBuffer = new byte[ValueFileIOHelper.IOBUFFER_SIZE];
      int read;
      int len = 0;
      OutputStream out = null;

      SwapFile swapFile = null;
      try
      {
         // stream from database
         if (content != null)
            while ((read = content.read(spoolBuffer)) >= 0)
            {
               if (out != null)
               {
                  // spool to temp file
                  out.write(spoolBuffer, 0, read);
                  len += read;
               }
               else if (len + read > maxBufferSize)
               {
                  // threshold for keeping data in memory exceeded;
                  // create temp file and spool buffer contents
                  swapFile = SwapFile.get(swapDirectory, cid + orderNumber + "." + version);
                  if (swapFile.isSpooled())
                  {
                     // break, value already spooled
                     buffer = null;
                     break;
                  }
                  out = PrivilegedFileHelper.fileOutputStream(swapFile);
                  out.write(buffer, 0, len);
                  out.write(spoolBuffer, 0, read);
                  buffer = null;
                  len += read;
               }
               else
               {
                  // reallocate new buffer and spool old buffer contents
                  byte[] newBuffer = new byte[len + read];
                  System.arraycopy(buffer, 0, newBuffer, 0, len);
                  System.arraycopy(spoolBuffer, 0, newBuffer, len, read);
                  buffer = newBuffer;
                  len += read;
               }
            }
      }
      finally
      {
         if (out != null)
         {
            out.close();
            swapFile.spoolDone();
         }
      }

      if (buffer == null)
      {
         return new CleanableFilePersistedValueData(orderNumber, swapFile, swapCleaner);
      }

      return new ByteArrayPersistedValueData(orderNumber, buffer);
   }

   /**
    * Read ValueData from external storage.
    * 
    * @param identifier
    *          property identifier
    * @param orderNumber
    *          value order number
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
   private ValueData readValueData(String identifier, int orderNumber, String storageId) throws SQLException,
      IOException, ValueStorageNotFoundException
   {
      ValueIOChannel channel = valueStorageProvider.getChannel(storageId);
      try
      {
         return channel.read(identifier, orderNumber, maxBufferSize);
      }
      finally
      {
         channel.close();
      }
   }

   /**
    * The comparator used to sort the value data
    */
   private static Comparator<ValueData> COMPARATOR_VALUE_DATA = new Comparator<ValueData>()
   {
      public int compare(ValueData vd1, ValueData vd2)
      {
         return vd1.getOrderNumber() - vd2.getOrderNumber();
      }
   };

   /**
    * Class needed to store temporary node data. 
    */
   private class TempNodeData
   {
      String cid;

      String cname;

      int cversion;

      String cpid;

      int cindex;

      int cnordernumb;

      Map<String, SortedSet<TempPropertyData>> properties = new HashMap<String, SortedSet<TempPropertyData>>();

      public TempNodeData(ResultSet item) throws SQLException
      {
         cid = item.getString("N_ID");
         cname = item.getString("N_NAME");
         cversion = item.getInt("N_VERSION");
         cpid = item.getString("N_PARENT_ID");
         cindex = item.getInt("N_I_INDEX");
         cnordernumb = item.getInt("N_ORDER_NUM");
      }
   }

   /**
    * Class needs to store temporary property data.
    */
   private class TempPropertyData implements Comparable<TempPropertyData>
   {
      String cid;

      String cname;

      int cversion;

      int ctype;

      boolean cmulti;

      int corderNum;

      InputStream cdata;

      String cstorage_desc;

      public TempPropertyData(ResultSet item) throws SQLException
      {
         cid = item.getString("P_ID");
         cname = item.getString("P_NAME");
         cversion = item.getInt("P_VERSION");
         ctype = item.getInt("P_TYPE");
         cmulti = item.getBoolean("P_MULTIVALUED");
         cdata = item.getBinaryStream(COLUMN_VDATA);
         cstorage_desc = item.getString(COLUMN_VSTORAGE_DESC);
         corderNum = item.getInt(COLUMN_VORDERNUM);
      }

      public int compareTo(TempPropertyData o)
      {
         return corderNum - o.corderNum;
      }
   }
}

