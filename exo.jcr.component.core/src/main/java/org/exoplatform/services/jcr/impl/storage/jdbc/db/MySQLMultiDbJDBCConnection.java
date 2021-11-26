/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS
 * 
 * 20.03.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: MySQLMultiDbJDBCConnection.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class MySQLMultiDbJDBCConnection extends MultiDbJDBCConnection
{

   /**
    * MySQL Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public MySQLMultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {

      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException
   {
      // check if parent exists
      if (data.getParentIdentifier() != null)
      {
         ResultSet item = findItemByIdentifier(data.getParentIdentifier());
         try
         {
            if (!item.next())
            {
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
            }
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }
      return super.addNodeRecord(data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(PropertyData data) throws SQLException
   {
      // check if parent exists
      if (data.getParentIdentifier() != null)
      {
         ResultSet item = findItemByIdentifier(data.getParentIdentifier());
         try
         {
            if (!item.next())
            {
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
            }
         }
         finally
         {
            try
            {
               item.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
      }
      return super.addPropertyRecord(data);
   }

}
