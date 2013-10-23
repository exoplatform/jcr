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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

import javax.jcr.RepositoryException;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DB2MultiDbJDBCConnection.ajva 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DB2MultiDbJDBCConnection extends MultiDbJDBCConnection
{
   /**
    * DB2 Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, shoudl be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerName
    *          Workspace Storage Container name (see configuration)
    * @param valueStorageProvider
    *          External Value Storages provider
    * @param maxBufferSize
    *          Maximum buffer size (see configuration)
    * @param swapDirectory
    *          Swap directory File (see configuration)
    * @param swapCleaner
    *          Swap cleaner (internal FileCleaner).
    * @throws SQLException
    * 
    * @see org.exoplatform.services.jcr.impl.util.io.FileCleaner
    */
   public DB2MultiDbJDBCConnection(Connection dbConnection, boolean readOnly, String containerName,
      ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory, FileCleaner swapCleaner)
      throws SQLException
   {
      super(dbConnection, readOnly, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {
      super.prepareQueries();
      FIND_NODES_AND_PROPERTIES =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_MVALUE V, JCR_MITEM P"
            + " join (select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from JCR_MITEM I"
            + " where I.I_CLASS=1 AND I.ID > ? order by I.ID FETCH FIRST $rowNb ROWS ONLY) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and V.PROPERTY_ID=P.ID  order by J.ID";
      FIND_NODES_BY_PARENTID_LAZILY_CQ =
         "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from JCR_MVALUE V, JCR_MITEM P "
            + " join (select J.* from JCR_MITEM J where J.I_CLASS=1 and J.PARENT_ID=?"
            + " AND J.N_ORDER_NUM  >= ? order by J.N_ORDER_NUM, J.ID FETCH FIRST $rowNb ROWS ONLY) I on P.PARENT_ID = I.ID"
            + " where P.I_CLASS=2 and P.PARENT_ID=I.ID and"
            + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
            + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
            + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";
      FIND_LAST_ORDER_NUMBER_BY_PARENTID =
         "VALUES NEXT VALUE FOR JCR_MITEM_SEQ";
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      if (findNodesAndProperties == null)
      {
         FIND_NODES_AND_PROPERTIES=FIND_NODES_AND_PROPERTIES.replace("$rowNb",Integer.toString(limit)) ;
         findNodesAndProperties = dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES);
      }
      else
      {
         findNodesAndProperties.clearParameters();
      }

      findNodesAndProperties.setString(1, getInternalId(lastNodeId));

      return findNodesAndProperties.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findLastOrderNumberByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (findLastOrderNumberByParentId == null)
      {
         findLastOrderNumberByParentId = dbConnection.prepareStatement(FIND_LAST_ORDER_NUMBER_BY_PARENTID);
      }

      return findLastOrderNumberByParentId.executeQuery();
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
               LOG.error("Can't close the ResultSet: " + e);
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
   protected ResultSet findChildNodesByParentIdentifier(String parentCid, int fromOrderNum, int toOrderNum)
      throws SQLException
   {
      if (findNodesByParentIdLazilyCQ == null)
      {
         int page= toOrderNum-fromOrderNum+1 ;
         FIND_NODES_BY_PARENTID_LAZILY_CQ=FIND_NODES_BY_PARENTID_LAZILY_CQ.replace("$rowNb",Integer.toString(page));
         findNodesByParentIdLazilyCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_LAZILY_CQ);
      }
      else
      {
         findNodesByParentIdLazilyCQ.clearParameters();
      }

      findNodesByParentIdLazilyCQ.setString(1, parentCid);
      findNodesByParentIdLazilyCQ.setInt(2, fromOrderNum);

      return findNodesByParentIdLazilyCQ.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void deleteLockProperties() throws SQLException
   {
      PreparedStatement removeValuesStatement = null;
      PreparedStatement removeItemsStatement = null;

      try
      {
         removeValuesStatement =
            dbConnection.prepareStatement("DELETE FROM JCR_MVALUE WHERE PROPERTY_ID IN"
               + " (SELECT ID FROM JCR_MITEM WHERE  I_CLASS = 2 AND NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR"
               + " NAME = '[http://www.jcp.org/jcr/1.0]lockOwner')");

         removeItemsStatement =
            dbConnection.prepareStatement("DELETE FROM JCR_MITEM WHERE  I_CLASS = 2 AND "
               + " NAME = '[http://www.jcp.org/jcr/1.0]lockIsDeep' OR"
               + " NAME = '[http://www.jcp.org/jcr/1.0]lockOwner'");

         removeValuesStatement.executeUpdate();
         removeItemsStatement.executeUpdate();
      }
      finally
      {
         if (removeValuesStatement != null)
         {
            try
            {
               removeValuesStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close statement", e);
            }
         }

         if (removeItemsStatement != null)
         {
            try
            {
               removeItemsStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close statement", e);
            }
         }
      }
   }
}
