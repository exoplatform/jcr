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
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.ByteArrayPersistedValueData;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

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
   protected static final Log LOG = ExoLogger.getLogger("jcr.optimisation.CQJDBCStorageConnection");

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
    * FIND_REFERENCE_PROPERTIES_CQ.
    */
   protected String FIND_REFERENCE_PROPERTIES_CQ;

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

      Map<String, List<byte[]>> properties = new HashMap<String, List<byte[]>>();

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
            Map<String, List<byte[]>> properties = data.properties;
            String key = resultSet.getString("PROP_NAME");
            List<byte[]> values = properties.get(key);
            if (values == null)
            {
               values = new ArrayList<byte[]>();
               properties.put(key, values);
            }
            values.add(resultSet.getBytes(COLUMN_VDATA));
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
    * {@inheritDoc}
    */
   public List<PropertyData> getReferencesData(String nodeIdentifier) throws RepositoryException, IllegalStateException
   {
      checkIfOpened();
      ResultSet refProps = null;
      try
      {
         refProps = findReferencePropertiesCQ(getInternalId(nodeIdentifier));
         return loadReferences(refProps);
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
         if (refProps != null)
         {
            try
            {
               refProps.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
            }
         }
      }

   }

   /**
    * Load Property references
    * 
    * @param resultSet
    * @return
    * @throws RepositoryException
    * @throws SQLException
    * @throws IOException
    */
   private List<PropertyData> loadReferences(ResultSet resultSet) throws RepositoryException, SQLException, IOException
   {
      List<PropertyData> resultProps = new ArrayList<PropertyData>();

      // Property Id and amount of copies in result
      Map<String, Integer> dublicatedProps = new HashMap<String, Integer>();
      Map<String, PersistedPropertyData> propertyBuffer = new HashMap<String, PersistedPropertyData>();
      Map<String, List<ValueData>> valuesBuffer = new HashMap<String, List<ValueData>>();

      try
      {
         while (resultSet.next())
         {
            String cid = resultSet.getString(COLUMN_ID);
            String identifier = getIdentifier(cid);

            int cversion = resultSet.getInt(COLUMN_VERSION);

            int valueOrderNum = resultSet.getInt(COLUMN_VORDERNUM);
            PersistedPropertyData prop = propertyBuffer.get(identifier);

            if (prop == null)
            {
               // make temporary PropertyData without values
               String cname = resultSet.getString(COLUMN_NAME);

               String cpid = resultSet.getString(COLUMN_PARENTID);
               int cptype = resultSet.getInt(COLUMN_PTYPE);
               boolean cpmultivalued = resultSet.getBoolean(COLUMN_PMULTIVALUED);
               QPath qpath = QPath.makeChildPath(traverseQPath(cpid), InternalQName.parse(cname));

               prop =
                  new PersistedPropertyData(identifier, qpath, getIdentifier(cpid), cversion, cptype, cpmultivalued,
                     null); // null values!
               propertyBuffer.put(identifier, prop);
               valuesBuffer.put(identifier, new ArrayList<ValueData>());
               dublicatedProps.put(identifier, new Integer(1));
            }

            List<ValueData> values = valuesBuffer.get(identifier);
            if (valueOrderNum == 0 && values.size() > 0)
            {
               // ignore it, this is a new copy
               Integer copies = dublicatedProps.get(identifier);
               copies++;
               dublicatedProps.put(identifier, copies);
            }
            else if (values.size() <= valueOrderNum)
            {
               // read value and put into values buffer
               final String storageId = resultSet.getString(COLUMN_VSTORAGE_DESC);
               ValueData vdata =
                  resultSet.wasNull() ? readValueData(cid, valueOrderNum, cversion, resultSet
                     .getBinaryStream(COLUMN_VDATA)) : readValueData(identifier, valueOrderNum, storageId);

               values.add(vdata);
               valuesBuffer.put(identifier, values);
            }
         }

         for (String id : propertyBuffer.keySet())
         {

            PersistedPropertyData prop = propertyBuffer.get(id);
            List<ValueData> values = valuesBuffer.get(id);
            int count = dublicatedProps.get(id).intValue();

            for (int i = 0; i < count; i++)
            {
               //make a copy
               List<ValueData> newValues = new ArrayList<ValueData>();
               for (ValueData vd : values)
               {
                  newValues.add(new ByteArrayPersistedValueData(vd.getOrderNumber(), vd.getAsByteArray()));
               }

               PersistedPropertyData pdata =
                  new PersistedPropertyData(prop.getIdentifier(), prop.getQPath(), prop.getParentIdentifier(), prop
                     .getPersistedVersion(), prop.getType(), prop.isMultiValued(), newValues);
               resultProps.add(pdata);
            }
            values.clear();
         }
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
      finally
      {
         // clean buffers
         propertyBuffer.clear();
         valuesBuffer.clear();
         dublicatedProps.clear();
      }

      return resultProps;
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
   protected List<AccessControlEntry> readACLPermisions(String cid, Map<String, List<byte[]>> properties)
      throws SQLException, IllegalACLException
   {
      List<AccessControlEntry> naPermissions = new ArrayList<AccessControlEntry>();
      List<byte[]> permValues = properties.get(Constants.EXO_PERMISSIONS.getAsString());

      if (permValues != null)
      {
         for (byte[] value : permValues)
         {
            StringTokenizer parser = new StringTokenizer(new String(value), AccessControlEntry.DELIMITER);
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
    * @param properties - Proeprty name and property values
    * @return ACL owner
    * @throws IllegalACLException
    */
   protected String readACLOwner(String cid, Map<String, List<byte[]>> properties) throws IllegalACLException
   {
      List<byte[]> ownerValues = properties.get(Constants.EXO_OWNER.getAsString());
      if (ownerValues != null)
         return new String(ownerValues.get(0));
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
      Map<String, List<byte[]>> properties = new HashMap<String, List<byte[]>>();
      while (ptProp.next())
      {
         String key = ptProp.getString(COLUMN_NAME);
         List<byte[]> values = properties.get(key);
         if (values == null)
         {
            values = new ArrayList<byte[]>();
            properties.put(key, values);
         }
         values.add(ptProp.getBytes(COLUMN_VDATA));
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
      int cversion, int cnordernumb, Map<String, List<byte[]>> properties, AccessControlList parentACL)
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
         List<byte[]> primaryType = properties.get(Constants.JCR_PRIMARYTYPE.getAsString());
         if (primaryType == null || primaryType.isEmpty())
         {
            throw new PrimaryTypeNotFoundException("FATAL ERROR primary type record not found. Node "
               + qpath.getAsString() + ", id " + cid + ", container " + this.containerName, null);
         }

         byte[] data = primaryType.get(0);
         InternalQName ptName = InternalQName.parse(new String((data != null ? data : new byte[]{})));

         // MIXIN
         InternalQName[] mts;
         boolean owneable = false;
         boolean privilegeable = false;
         List<byte[]> mixTypes = properties.get(Constants.JCR_MIXINTYPES.getAsString());
         if (mixTypes != null)
         {
            List<InternalQName> mNames = new ArrayList<InternalQName>();
            for (byte[] mxnb : mixTypes)
            {
               InternalQName mxn = InternalQName.parse(new String(mxnb));
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

   protected abstract ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findChildPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findNodeMainPropertiesByParentIdentifierCQ(String parentIdentifier) throws SQLException;

   protected abstract ResultSet findReferencePropertiesCQ(String nodeIdentifier) throws SQLException;
}
