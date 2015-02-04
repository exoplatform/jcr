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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 19 mars 2010  
 */
public class OracleMultiDbJDBCConnection extends MultiDbJDBCConnection
{

   /**
    * Oracle Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public OracleMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
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

      FIND_NODES_BY_PARENTID_CQ_QUERY =
         super.FIND_NODES_BY_PARENTID_CQ_QUERY.replaceFirst("select", "select /*+ INDEX(I " + JCR_IDX_ITEM_PARENT_ID
            + ") INDEX(P " + JCR_IDX_ITEM_PARENT_ID + ") INDEX(V " + JCR_IDX_VALUE_PROPERTY + ")*/");

      FIND_PROPERTIES_BY_PARENTID_CQ_QUERY =
         super.FIND_PROPERTIES_BY_PARENTID_CQ_QUERY.replaceFirst("select", "select /*+ INDEX(I "
            + JCR_IDX_ITEM_PARENT_ID + ") INDEX(V " + JCR_IDX_VALUE_PROPERTY + ")*/");

      FIND_ITEM_QPATH_BY_ID_CQ_QUERY =
         super.FIND_ITEM_QPATH_BY_ID_CQ_QUERY.replaceFirst("SELECT", "SELECT /*+ INDEX(" + JCR_ITEM + " " + JCR_PK_ITEM
            + ") */");

      FIND_NODES_BY_PARENTID_CQ = FIND_NODES_BY_PARENTID_CQ_QUERY;
      FIND_PROPERTIES_BY_PARENTID_CQ = FIND_PROPERTIES_BY_PARENTID_CQ_QUERY;
      FIND_ITEM_QPATH_BY_ID_CQ = FIND_ITEM_QPATH_BY_ID_CQ_QUERY;
      FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE =
         "select /*+ INDEX(I " + JCR_FK_ITEM_PARENT + ") INDEX(V " + JCR_IDX_VALUE_PROPERTY + ")*/"
            + " I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE, I.P_MULTIVALUED,"
            + " V.ORDER_NUM, V.DATA, V.STORAGE_DESC from " + JCR_ITEM + " I LEFT OUTER JOIN " + JCR_VALUE
            + " V ON (V.PROPERTY_ID=I.ID)";

      FIND_NODES_AND_PROPERTIES =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from " + JCR_VALUE + " V, " + JCR_ITEM + " P"
            + " join ( select * from ( select A.*, ROWNUM r__ from ("
            + " select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from " + JCR_ITEM + " I "
            + " where I.I_CLASS=1 order by I.ID) A where ROWNUM <= ?) where r__ > ?) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and V.PROPERTY_ID=P.ID order by J.ID";
      if (containerConfig.useSequenceForOrderNumber)
      {
         FIND_NODES_BY_PARENTID_LAZILY_CQ =
            "select /*+ USE_NL(V) INDEX(I "
               + JCR_IDX_ITEM_N_ORDER_NUM + ") INDEX(P " + JCR_IDX_ITEM_PARENT_FK + ") INDEX(V " + JCR_IDX_VALUE_PROPERTY + ") */"
               + " I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from " + JCR_VALUE + " V, " + JCR_ITEM + " P "
               + " join ( select * from ( select A.*, ROWNUM r__ from ( select J.* from " + JCR_ITEM + " J "
               + " where J.I_CLASS=1 and J.PARENT_ID=? order by J.N_ORDER_NUM, J.ID "
               + " ) A where ROWNUM <= ?) where r__ > ?)  I on P.PARENT_ID = I.ID"
               + " where P.I_CLASS=2 and P.PARENT_ID=I.ID and"
               + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
               + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
               + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
               + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
               + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";
         FIND_LAST_ORDER_NUMBER = "SELECT " + JCR_ITEM_NEXT_VAL + " ('" + JCR_ITEM_SEQ + "', ?, ?) FROM dual";
      }
      else
      {
         FIND_NODES_BY_PARENTID_LAZILY_CQ =
            FIND_NODES_BY_PARENTID_LAZILY_CQ.replaceFirst("select", "select /*+ USE_NL(V) INDEX(I "
               + JCR_IDX_ITEM_N_ORDER_NUM + ") INDEX(P " + JCR_IDX_ITEM_PARENT_FK + ") INDEX(V " + JCR_IDX_VALUE_PROPERTY
               + ") */");
      }
      FIND_REFERENCES = FIND_REFERENCES.replaceFirst("select", "select /*+ INDEX(R " + JCR_PK_REF + ")*/");

      FIND_PROPERTIES_BY_PARENTID =
         FIND_PROPERTIES_BY_PARENTID.replaceFirst("select", "select /*+ INDEX(" + JCR_ITEM + " "
            + JCR_IDX_ITEM_PARENT_NAME + ")*/");
      
      FIND_ITEM_BY_NAME = FIND_ITEM_BY_NAME.replaceFirst("select", "select /*+ INDEX(I " + JCR_IDX_ITEM_PARENT + ") */"); 

      FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ =
         FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ.replaceFirst("select",
            "select /*+ INDEX(I " + JCR_IDX_ITEM_PARENT_NAME + ") */"); 

      DELETE_ITEM = "delete /*+ INDEX(I " + JCR_PK_ITEM + ")*/ from " + JCR_ITEM + " I where I.ID=?";
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findNodesAndProperties(String lastNodeId, int offset, int limit) throws SQLException
   {
      if (findNodesAndProperties == null)
      {
         findNodesAndProperties = dbConnection.prepareStatement(FIND_NODES_AND_PROPERTIES);
      }
      else
      {
         findNodesAndProperties.clearParameters();
      }

      findNodesAndProperties.setInt(1, offset + limit);
      findNodesAndProperties.setInt(2, offset);

      return findNodesAndProperties.executeQuery();
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
         findNodesByParentIdLazilyCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_LAZILY_CQ);
      }
      else
      {
         findNodesByParentIdLazilyCQ.clearParameters();
      }

      findNodesByParentIdLazilyCQ.setString(1, parentCid);
      findNodesByParentIdLazilyCQ.setInt(2, offset + limit);
      findNodesByParentIdLazilyCQ.setInt(3, offset);

      return findNodesByParentIdLazilyCQ.executeQuery();
   }

}
