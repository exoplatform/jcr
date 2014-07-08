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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JCR Storage Sybase initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */
public class SybaseDBInitializer extends StorageDBInitializer
{

   public SybaseDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isProcedureExists(final Connection conn, final String procedureName) throws SQLException
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Boolean>()
      {
         public Boolean run()
         {
            return procedureExists(procedureName, conn);
         }
      });
   }

   private boolean procedureExists(String procedureName, Connection con)
   {
      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String query;
         query = "select count(*) from sysobjects where type='P' and name='" + procedureName + "'";
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         return (trs.next() && trs.getInt(1) >= 1);
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while checking the procedure " + procedureName, e);
         }
         return false;

      }
      finally
      {
         JDBCUtils.freeResources(trs, stmt, null);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void postInit(Connection connection) throws SQLException
   {
      super.postInit(connection);
      Statement stmt = null;
      ResultSet rs = null;
      try
      {
         if (containerConfig.useSequenceForOrderNumber)
         {
            String select =
               "select * from JCR_" + DBInitializerHelper.getItemTableSuffix(containerConfig) + "_SEQ  where name='LAST_N_ORDER_NUM'";
            stmt = connection.createStatement();
            rs = stmt.executeQuery(select);
            if (!rs.next())
            {
               String insert = "INSERT INTO JCR_" + DBInitializerHelper.getItemTableSuffix(containerConfig) + "_SEQ  (name, nextVal) VALUES ('LAST_N_ORDER_NUM'," + getStartValue(connection) + ")";
               stmt.executeUpdate(insert);
            }
         }
      }
      finally
      {
         JDBCUtils.freeResources(rs, stmt, null);
      }
   }

}
