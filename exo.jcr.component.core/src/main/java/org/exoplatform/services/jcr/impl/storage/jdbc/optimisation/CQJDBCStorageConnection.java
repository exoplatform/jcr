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
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.PrimaryTypeNotFoundException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
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
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.jcr.InvalidItemStateException;
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
    * FIND_ITEM_QPATH_BY_ID_CQ.
    */
   protected String FIND_ITEM_QPATH_BY_ID_CQ;

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
    * Class needed to store node details (property also) since result set is not sorted in valid way. 
    */
   private static class TempNodeData
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
         cid = item.getString(COLUMN_ID);
         cname = item.getString(COLUMN_NAME);
         cversion = item.getInt(COLUMN_VERSION);
         cpid = item.getString(COLUMN_PARENTID);
         cindex = item.getInt(COLUMN_INDEX);
         cnordernumb = item.getInt(COLUMN_NORDERNUM);
      }
   }

   /**
    * store temporary property data to allow to sort it manually
    */
   private static class TempPropertyData implements Comparable<TempPropertyData>
   {
      int orderNum;

      byte[] data;

      public TempPropertyData(ResultSet item) throws SQLException
      {
         orderNum = item.getInt(COLUMN_VORDERNUM);
         data = item.getBytes(COLUMN_VDATA);
      }

      public int compareTo(TempPropertyData o)
      {
         return orderNum - o.orderNum;
      }
   }

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
               LOG.error(e.getMessage(), e);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
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
               LOG.error(e.getMessage(), e);
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
    */
   protected List<AccessControlEntry> readACLPermisions(String cid, Map<String, SortedSet<TempPropertyData>> properties)
      throws SQLException, IllegalACLException
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
         throw new IllegalACLException("Property exo:permissions is not found for node with id: " + getIdentifier(cid));
   }

   /**
    * Read ACL owner.
    * 
    * @param cid - node id (used only in exception message)
    * @param properties - Property name and property values
    * @return ACL owner
    * @throws IllegalACLException
    */
   protected String readACLOwner(String cid, Map<String, SortedSet<TempPropertyData>> properties)
      throws IllegalACLException
   {
      SortedSet<TempPropertyData> ownerValues = properties.get(Constants.EXO_OWNER.getAsString());
      if (ownerValues != null)
         return new String(ownerValues.first().data);
      else
         throw new IllegalACLException("Property exo:owner is not found for node with id: " + getIdentifier(cid));
   }

   /**
    * {@inheritDoc}
    */
   protected PersistedNodeData loadNodeRecord(QPath parentPath, String cname, String cid, String cpid, int cindex,
      int cversion, int cnordernumb, AccessControlList parentACL) throws RepositoryException, SQLException
   {
      ResultSet ptProp = findNodeMainPropertiesByParentIdentifierCQ(cid);
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
    */
   private PersistedNodeData loadNodeRecord(QPath parentPath, String cname, String cid, String cpid, int cindex,
      int cversion, int cnordernumb, Map<String, SortedSet<TempPropertyData>> properties, AccessControlList parentACL)
      throws RepositoryException, SQLException
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
                  privilegeable = true;
               else if (!owneable && Constants.EXO_OWNEABLE.equals(mxn))
                  owneable = true;
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
                  // construct ACL from existed parent ACL
                  acl =
                     new AccessControlList(parentACL.getOwner(), parentACL.hasPermissions() ? parentACL
                        .getPermissionEntries() : null);
               else
                  // have to search nearest ancestor owner and permissions in ACL manager
                  // acl = traverseACL(cpid);
                  acl = null;
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
            result.close();
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

   protected abstract ResultSet findItemQPathByIdentifierCQ(String identifier) throws SQLException;

   protected abstract ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findNodeMainPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;
}
