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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * JCR Storage Mysql initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */
public class SybaseDBInitializer extends DBInitializer
{

   public SybaseDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void postInit(Connection connection) throws SQLException
   {
      super.postInit(connection);
      String select =
         "select * from JCR_SEQ  where name='JCR_N_ORDER_NUM'";
      if (!connection.createStatement().executeQuery(select).next())
      {
         String insert = "INSERT INTO JCR_SEQ (name, value) VALUES ('JCR_N_ORDER_NUM',"+getStartValue(connection)+")";
         connection.createStatement().executeUpdate(insert);
      }
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

}
