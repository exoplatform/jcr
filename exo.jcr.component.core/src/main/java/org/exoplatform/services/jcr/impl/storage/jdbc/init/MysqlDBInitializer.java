/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc.init;

import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;

/**
 * JCR Storage Mysql initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */
public class MysqlDBInitializer extends StorageDBInitializer
{

   public MysqlDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isFunctionExists(Connection conn, String functionName) throws SQLException
   {
      ResultSet resultSet = null;
      try
      {
         resultSet = conn.getMetaData().getFunctions(
            conn.getCatalog(), null, "%");
         while (resultSet.next())
         {

            if (functionName.equals(resultSet.getString("FUNCTION_NAME")))
            {
               return true;
            }
         }
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while checking the function " + functionName, e);
         }
         return false;
      }
      finally
      {
         JDBCUtils.freeResources(resultSet, null, null);
      }

      return false;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void postInit(Connection connection) throws SQLException
   {
      super.postInit(connection);
      if (containerConfig.use_sequence_for_order_number)
      {
         String select =
            "select * from JCR_" + DBInitializerHelper.getItemTableSuffix(containerConfig) + "_SEQ  where name='LAST_N_ORDER_NUM'";
         if (!connection.createStatement().executeQuery(select).next())
         {
            String insert = "INSERT INTO JCR_" + DBInitializerHelper.getItemTableSuffix(containerConfig) + "_SEQ  (name, nextVal) VALUES ('LAST_N_ORDER_NUM'," + getStartValue(connection) + ")";
            connection.createStatement().executeUpdate(insert);
         }
      }
   }

}
