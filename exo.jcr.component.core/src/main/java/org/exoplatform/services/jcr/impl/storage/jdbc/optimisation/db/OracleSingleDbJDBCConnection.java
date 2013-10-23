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
public class OracleSingleDbJDBCConnection extends SingleDbJDBCConnection
{

   protected static final String FIND_NODES_BY_PARENTID_CQ_QUERY =
      SingleDbJDBCConnection.FIND_NODES_BY_PARENTID_CQ_QUERY
         .replaceFirst("select",
            "select /*+ INDEX(I JCR_IDX_SITEM_PARENT_ID) INDEX(P JCR_IDX_SITEM_PARENT_ID) INDEX(V JCR_IDX_SVALUE_PROPERTY)*/");

   protected static final String FIND_PROPERTIES_BY_PARENTID_CQ_QUERY =
      SingleDbJDBCConnection.FIND_PROPERTIES_BY_PARENTID_CQ_QUERY.replaceFirst("select",
         "select /*+ INDEX(I JCR_IDX_SITEM_PARENT_ID) INDEX(V JCR_IDX_SVALUE_PROPERTY)*/");

   protected static final String FIND_ITEM_QPATH_BY_ID_CQ_QUERY =
            SingleDbJDBCConnection.FIND_ITEM_QPATH_BY_ID_CQ_QUERY.replaceFirst("SELECT",
                     "SELECT /*+ INDEX(JCR_SITEM JCR_PK_SITEM) */");

   /**
    * Oracle Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public OracleSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
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
      FIND_NODES_BY_PARENTID_CQ = FIND_NODES_BY_PARENTID_CQ_QUERY;
      FIND_PROPERTIES_BY_PARENTID_CQ = FIND_PROPERTIES_BY_PARENTID_CQ_QUERY;
      FIND_ITEM_QPATH_BY_ID_CQ = FIND_ITEM_QPATH_BY_ID_CQ_QUERY;
      FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE =
         "select /*+ INDEX(I JCR_FK_SITEM_PARENT) INDEX(V JCR_IDX_SVALUE_PROPERTY)*/"
            + " I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE, I.P_MULTIVALUED,"
            + " V.ORDER_NUM, V.DATA, V.STORAGE_DESC from JCR_SITEM I LEFT OUTER JOIN JCR_SVALUE V ON (V.PROPERTY_ID=I.ID)";

      FIND_NODES_AND_PROPERTIES =
         "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
            + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V, JCR_SITEM P"
            + " join ( select * from ( select A.*, ROWNUM r__ from ("
            + " select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from JCR_SITEM I "
            + " where I.CONTAINER_NAME=? and I.I_CLASS=1 order by I.ID"
            + " ) A where ROWNUM <= ?) where r__ > ?) J on P.PARENT_ID = J.ID"
            + " where P.I_CLASS=2 and P.CONTAINER_NAME=? and V.PROPERTY_ID=P.ID order by J.ID";

      FIND_NODES_BY_PARENTID_LAZILY_CQ =
         "select /*+ USE_NL(V) INDEX(I JCR_IDX_SITEM_N_ORDER_NUM) INDEX(P JCR_IDX_SITEM_PARENT_FK) INDEX(V JCR_IDX_SVALUE_PROPERTY) */"
            + " I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from JCR_SVALUE V, JCR_SITEM P "
            + " join ( select * from ( select A.*, ROWNUM r__ from ( select J.* from JCR_SITEM J "
            + " where J.CONTAINER_NAME=? and J.I_CLASS=1 and J.PARENT_ID=? order by J.N_ORDER_NUM, J.ID "
            + " ) A where ROWNUM <= ?) where r__ > ?) I on P.PARENT_ID = I.ID"
            + " where P.I_CLASS=2 and P.CONTAINER_NAME=? and P.PARENT_ID=I.ID and"
            + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
            + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')"
            + " and V.PROPERTY_ID=P.ID order by I.N_ORDER_NUM, I.ID";
      
      FIND_ITEM_BY_NAME = FIND_ITEM_BY_NAME.replaceFirst("select", "select /*+ INDEX(I JCR_IDX_SITEM_PARENT) */");

      FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ =
         FIND_NODE_MAIN_PROPERTIES_BY_PARENTID_CQ.replaceFirst("select",
            "select /*+ INDEX(I JCR_IDX_SITEM_PARENT_NAME) */");

      DELETE_ITEM = "delete /*+ INDEX(I JCR_PK_SITEM)*/ from JCR_SITEM I where I.ID=?";

      FIND_LAST_ORDER_NUMBER_BY_PARENTID ="SELECT "+ JCR_ITEM_SEQ +".nextval FROM dual";
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

      findNodesAndProperties.setString(1, this.containerConfig.containerName);
      findNodesAndProperties.setInt(2, offset + limit);
      findNodesAndProperties.setInt(3, offset);
      findNodesAndProperties.setString(4, this.containerConfig.containerName);

      return findNodesAndProperties.executeQuery();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifier(String parentCid, int fromOrderNum, int offset, int limit)
      throws SQLException
   {
      if (findNodesByParentIdLazilyCQ == null)
      {
         findNodesByParentIdLazilyCQ = dbConnection.prepareStatement(FIND_NODES_BY_PARENTID_LAZILY_CQ);
      }
      else
      {
         findNodesByParentIdLazilyCQ.clearParameters();
      }

      findNodesByParentIdLazilyCQ.setString(1, this.containerConfig.containerName);
      findNodesByParentIdLazilyCQ.setString(2, parentCid);
      findNodesByParentIdLazilyCQ.setInt(3, offset + limit);
      findNodesByParentIdLazilyCQ.setInt(4, offset);
      findNodesByParentIdLazilyCQ.setString(5, this.containerConfig.containerName);

      return findNodesByParentIdLazilyCQ.executeQuery();
   }
}
