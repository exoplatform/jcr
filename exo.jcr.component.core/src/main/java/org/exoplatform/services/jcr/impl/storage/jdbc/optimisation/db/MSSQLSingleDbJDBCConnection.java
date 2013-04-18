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
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: MSSQLSingleDbJDBCConnection.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class MSSQLSingleDbJDBCConnection extends SingleDbJDBCConnection
{
   /**
    * Template for query. Since there is no way to set parameter for TOP via prepared statement.
    * We need to replace it in the code.
    */
   private static final String FIND_NODES_AND_PROPERTIES_TEMPLATE =
      "select J.*, P.ID AS P_ID, P.NAME AS P_NAME, P.VERSION AS P_VERSION, P.P_TYPE, P.P_MULTIVALUED,"
         + " V.DATA, V.ORDER_NUM, V.STORAGE_DESC from JCR_SVALUE V WITH (INDEX (JCR_IDX_SVALUE_PROPERTY)), JCR_SITEM P"
         + " join (select TOP ${TOP} I.ID, I.PARENT_ID, I.NAME, I.VERSION, I.I_INDEX, I.N_ORDER_NUM from"
         + " JCR_SITEM I WITH (INDEX (JCR_PK_SITEM))"
         + " where I.CONTAINER_NAME=? AND I.I_CLASS=1 AND I.ID > ? order by I.ID) J on P.PARENT_ID = J.ID"
         + " where P.I_CLASS=2 and P.CONTAINER_NAME=? and V.PROPERTY_ID=P.ID order by J.ID";

   /**
    * MSSQL Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public MSSQLSingleDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
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

      findNodesAndProperties.setString(1, this.containerConfig.containerName);
      findNodesAndProperties.setString(2, getInternalId(lastNodeId));
      findNodesAndProperties.setString(3, this.containerConfig.containerName);

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

}
