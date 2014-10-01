/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.itemfilters.QPathEntryFilter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * 26.08.2009
 * 
 * @author <a href="mailto:dezder@bk.ru">Denis Grebenyuk</a>
 * @version $Id$
 */
public class HSQLDBMultiDbJDBCConnection extends MultiDbJDBCConnection
{
   /**
    * HSQLDB Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public HSQLDBMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
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
         "select bit_length(DATA)/8, I.P_TYPE, V.STORAGE_DESC from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V where I.ID = ? and V.PROPERTY_ID = I.ID";

      FIND_PROPERTY_BY_NAME =
         "select V.DATA" + " from " + JCR_ITEM + " I, " + JCR_VALUE + " V"
            + " where I.PARENT_ID=? and I.I_CLASS=2 and I.NAME=? and I.ID=V.PROPERTY_ID order by V.ORDER_NUM";
      FIND_NODES_BY_PARENTID =
         "select * from " + JCR_ITEM + " where PARENT_ID=? and I_CLASS=1" + " order by N_ORDER_NUM";
      if (containerConfig.useSequenceForOrderNumber)
      {
         FIND_LAST_ORDER_NUMBER = "call next value for " + JCR_ITEM_SEQ;
      }

      FIND_NODES_COUNT_BY_PARENTID = "select count(ID) from " + JCR_ITEM + " where PARENT_ID=? and I_CLASS=1";
      FIND_PROPERTIES_BY_PARENTID = "select * from " + JCR_ITEM + " where PARENT_ID=? and I_CLASS=2" + " order by ID";
      FIND_NODES_BY_PARENTID_CQ =
         "select I.*, P.NAME AS PROP_NAME, V.ORDER_NUM, V.DATA from " + JCR_ITEM + " I, " + JCR_ITEM + " P, "
            + JCR_VALUE + " V" + " where I.PARENT_ID=? and I.I_CLASS=1 and (P.PARENT_ID=I.ID and P.I_CLASS=2 and"
            + " (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType' or"
            + " P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner' or"
            + " P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions') and V.PROPERTY_ID=P.ID)"
            + " order by I.N_ORDER_NUM, I.ID";
      FIND_PROPERTIES_BY_PARENTID_CQ =
         "select I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_CLASS, I.I_INDEX, I.N_ORDER_NUM, I.P_TYPE, I.P_MULTIVALUED,"
            + " V.ORDER_NUM, V.DATA, V.STORAGE_DESC from " + JCR_ITEM + " I LEFT OUTER JOIN " + JCR_VALUE
            + " V ON (V.PROPERTY_ID=I.ID)" + " where I.PARENT_ID=? and I.I_CLASS=2 order by I.NAME";

      FIND_WORKSPACE_DATA_SIZE = "select sum(bit_length(DATA)/8) from " + JCR_VALUE;

      FIND_NODE_DATA_SIZE =
         "select sum(bit_length(DATA)/8) from " + JCR_ITEM + " I, " + JCR_VALUE
            + " V  where I.PARENT_ID=? and I.I_CLASS=2 and I.ID=V.PROPERTY_ID";

      FIND_VALUE_STORAGE_DESC_AND_SIZE = "select bit_length(DATA)/8, STORAGE_DESC from " + JCR_VALUE + " where PROPERTY_ID=?";
   }

   /**
    * Use simple queries since it is much faster
    */
   @Override
   protected QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      return traverseQPathSQ(cpid);
   }

   public List<NodeData> getChildNodesData(NodeData parent, List<QPathEntryFilter> itemDataFilters) throws RepositoryException,
      IllegalStateException
   {
      return getDirectChildNodesData(parent, itemDataFilters);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildNodesByParentIdentifierCQ(String parentIdentifier, List<QPathEntryFilter> pattern)
      throws SQLException
   {
      if (pattern.isEmpty())
      {
         throw new SQLException("Pattern list is empty.");
      }
      else
      {
         if (findNodesByParentIdAndComplexPatternCQ == null)
         {
            findNodesByParentIdAndComplexPatternCQ = dbConnection.createStatement();
         }

         //create query from list
         StringBuilder query = new StringBuilder(FIND_NODES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE);
         query.append(" where I.PARENT_ID='");
         query.append(parentIdentifier);
         query.append("' and I.I_CLASS=1 and ( ");
         appendPattern(query, pattern.get(0).getQPathEntry(), true);
         for (int i = 1; i < pattern.size(); i++)
         {
            query.append(" or ");
            appendPattern(query, pattern.get(i).getQPathEntry(), true);
         }
         query.append(" ) and (P.PARENT_ID=I.ID and P.I_CLASS=2 and (P.NAME='[http://www.jcp.org/jcr/1.0]primaryType'");
         query.append(" or P.NAME='[http://www.jcp.org/jcr/1.0]mixinTypes'");
         query.append(" or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]owner'");
         query.append(" or P.NAME='[http://www.exoplatform.com/jcr/exo/1.0]permissions')");
         query.append(" and V.PROPERTY_ID=P.ID) order by I.N_ORDER_NUM, I.ID");

         return findNodesByParentIdAndComplexPatternCQ.executeQuery(query.toString());
      }

   }

   public List<PropertyData> getChildPropertiesData(NodeData parent, List<QPathEntryFilter> itemDataFilters)
      throws RepositoryException, IllegalStateException
   {
      return getDirectChildPropertiesData(parent, itemDataFilters);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findChildPropertiesByParentIdentifierCQ(String parentCid, List<QPathEntryFilter> pattern)
      throws SQLException
   {
      if (pattern.isEmpty())
      {
         throw new SQLException("Pattern list is empty.");
      }
      else
      {
         if (findPropertiesByParentIdAndComplexPatternCQ == null)
         {
            findPropertiesByParentIdAndComplexPatternCQ = dbConnection.createStatement();
         }

         //create query from list
         StringBuilder query = new StringBuilder(FIND_PROPERTIES_BY_PARENTID_AND_PATTERN_CQ_TEMPLATE);
         query.append(" where I.PARENT_ID='");
         query.append(parentCid);
         query.append("' and I.I_CLASS=2 and ( ");
         appendPattern(query, pattern.get(0).getQPathEntry(), false);
         for (int i = 1; i < pattern.size(); i++)
         {
            query.append(" or ");
            appendPattern(query, pattern.get(i).getQPathEntry(), false);
         }
         query.append(" ) order by I.NAME");

         return findPropertiesByParentIdAndComplexPatternCQ.executeQuery(query.toString());
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected ResultSet findLastOrderNumber(int localMaxOrderNumber, boolean increment) throws SQLException
   {
      if (findLastOrderNumber == null)
      {
         findLastOrderNumber = dbConnection.prepareStatement(FIND_LAST_ORDER_NUMBER);
      }
      if (!increment)
      {
         ResultSet count;
         int result = -1;
         while (result < localMaxOrderNumber - 1)
         {
            count = findLastOrderNumber.executeQuery();
            if (count.next())
            {
               result = count.getInt(1);
            }
         }
      }
      return findLastOrderNumber.executeQuery();
   }
}
