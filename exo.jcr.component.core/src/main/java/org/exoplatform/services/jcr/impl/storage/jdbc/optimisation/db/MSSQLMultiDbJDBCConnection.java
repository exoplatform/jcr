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

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 19 mars 2010  
 */
public class MSSQLMultiDbJDBCConnection extends MultiDbJDBCConnection
{
   /**
    * Template for query. Since there is no way to set parameter for TOP via prepared statement.
    * We need to replace it in the code.
    */
   public String FIND_NODES_AND_PROPERTIES_TEMPLATE;

   /**
    * MSSQL Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public MSSQLMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {
      super.prepareQueries();

      FIND_PROPERTY_BY_ID =
         "select len(DATA), I.P_TYPE, V.STORAGE_DESC from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V where I.ID = ? and V.PROPERTY_ID = I.ID";

      FIND_NODES_AND_PROPERTIES_TEMPLATE =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from " + JCR_VALUE + " V WITH (INDEX (" + JCR_IDX_VALUE_PROPERTY
            + ")), " + JCR_ITEM + " P"
            + " join (select TOP ${TOP} I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from "
            + JCR_ITEM + " I" + " WITH (INDEX (" + JCR_PK_ITEM
            + ")) where I.I_CLASS=1 AND I.ID > ? order by I.ID) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and V.PROPERTY_ID=P.ID order by J.ID";

      FIND_WORKSPACE_DATA_SIZE = "select sum(len(DATA)) from " + JCR_VALUE;

      FIND_NODE_DATA_SIZE =
         "select sum(len(DATA)) from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V  where I.PARENT_ID=? and I.I_CLASS=2 and I.ID=V.PROPERTY_ID";

      FIND_VALUE_STORAGE_DESC_AND_SIZE = "select len(DATA), STORAGE_DESC from " + JCR_VALUE + " where PROPERTY_ID=?";

      if (containerConfig.useSequenceForOrderNumber)
      {
         FIND_LAST_ORDER_NUMBER_BY_PARENTID = "exec " + JCR_ITEM_NEXT_VAL + " 'LAST_N_ORDER_NUM'";
         FIND_NODES_BY_PARENTID_LAZILY_CQ =
            "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from " + JCR_VALUE + " V, " + JCR_ITEM + " P "
               + " join (select TOP ${TOP} J.* from " + JCR_ITEM + " J where J.I_CLASS=1 and J.PARENT_ID=?"
               + " AND J.N_ORDER_NUM  >= ? order by J.N_ORDER_NUM, J.ID ) I on P.PARENT_ID = I.ID"
               + " where P.I_CLASS=2 and P.PARENT_ID=I.ID and"
               + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
               + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
               + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
               + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
               + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      if (findNodesAndProperties != null)
      {
         findNodesAndProperties.close();
      }

      findNodesAndProperties =
         dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES_TEMPLATE.replace("${TOP}",
            new Integer(offset + limit).toString()));

      findNodesAndProperties.setString(1, getInternalId(lastNodeId));

      return findNodesAndProperties.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   protected boolean needToSkipOffsetNodes()
   {
      return true;
   }

   /**
    * Replace underscore in pattern with escaped symbol. Replace jcr-wildcard '*' with sql-wildcard '%'.
    * <p>
    * MSSQL have a range pattern '[..]' so we need to escape it too.
    * 
    * @param pattern
    * @return pattern with escaped underscore and fixed wildcard symbols
    */
   protected String escapeSpecialChars(String pattern)
   {
      char[] chars = pattern.toCharArray();
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < chars.length; i++)
      {
         switch (chars[i])
         {
            case '*' :
               sb.append('%');
               break;
            case '_' :
            case '%' :
            case '[' :
            case ']' :
               sb.append(getWildcardEscapeSymbol());
            default :
               sb.append(chars[i]);
         }
      }
      return sb.toString();
   }

   @Override
   protected ResultSet findLastOrderNumberByParentIdentifier(String parentIdentifier) throws SQLException
   {
      if (!containerConfig.useSequenceForOrderNumber)
      {
         return super.findLastOrderNumberByParentIdentifier(parentIdentifier);
      }
      if (findLastOrderNumberByParentId == null)
      {
         findLastOrderNumberByParentId = dbConnection.prepareCall(FIND_LAST_ORDER_NUMBER_BY_PARENTID);
      }
      findLastOrderNumberByParentId.execute();
      return (findLastOrderNumberByParentId).getResultSet();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifier(String parentCid, int fromOrderNum, int offset, int limit)
      throws SQLException
   {
      if (!containerConfig.useSequenceForOrderNumber)
      {
         return super.findChildNodesByParentIdentifier(parentCid, fromOrderNum, offset, limit);
      }
      if (findNodesByParentIdLazilyCQ == null)
      {
         findNodesByParentIdLazilyCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_LAZILY_CQ.replace("${TOP}",
            new Integer(offset + limit).toString()));
      }
      else
      {
         findNodesByParentIdLazilyCQ.clearParameters();
      }

      findNodesByParentIdLazilyCQ.setString(1, parentCid);
      findNodesByParentIdLazilyCQ.setInt(2, fromOrderNum);

      return findNodesByParentIdLazilyCQ.executeQuery();
   }
}
