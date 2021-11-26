/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.InvalidItemStateException;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: H2MultiDbJDBCConnection.java 34360 23 Oct 2012 andrew.plotnikov $
 *
 */
public class H2MultiDbJDBCConnection extends MultiDbJDBCConnection
{

   /**
    * H2 Multidatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, should be opened before
    * @param readOnly
    *          boolean if true the dbConnection was marked as READ-ONLY.
    * @param containerConfig
    *          Workspace Storage Container configuration
    */
   public H2MultiDbJDBCConnection(Connection dbConnection, boolean readOnly, JDBCDataContainerConfig containerConfig)
      throws SQLException
   {
      super(dbConnection, readOnly, containerConfig);
   }

   /**
    * Use simple queries since it is much faster
    */
   @Override
   protected QPath traverseQPath(String cpid) throws SQLException, InvalidItemStateException, IllegalNameException
   {
      return traverseQPathSQ(cpid);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void prepareQueries() throws SQLException
   {
      super.prepareQueries();
      if (containerConfig.useSequenceForOrderNumber)
      {
         FIND_LAST_ORDER_NUMBER = "call " + JCR_ITEM_SEQ + ".NEXTVAL";
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
