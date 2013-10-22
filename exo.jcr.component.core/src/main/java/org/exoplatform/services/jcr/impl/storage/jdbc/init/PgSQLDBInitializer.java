/*
 * Copyright (C) 2009 eXo Platform SAS.
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

import java.io.IOException;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by The eXo Platform SAS
 *
 * 26.03.2007
 *
 * PgSQL convert all db object names to lower case, so respect it.
 * Same as Ingres initializer.
 *
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: PgSQLDBInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class PgSQLDBInitializer extends StorageDBInitializer
{

   public PgSQLDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   @Override
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      return super.isTableExists(conn, tableName.toUpperCase().toLowerCase());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isSequenceExists(final Connection conn, final String sequenceName) throws SQLException
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Boolean>()
      {
         public Boolean run()
         {
            return sequenceExists(sequenceName, conn);
         }
      });
   }

   private boolean sequenceExists(String sequenceName, Connection con)
   {
      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String query;
         query = "SELECT count(*) FROM information_schema.sequences where sequence_name='"+sequenceName.toLowerCase()+"'";

         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         return (trs.next() && trs.getInt(1) >= 1);

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while checking the sequence " + sequenceName, e);
         }
         return false;
      }
      finally
      {
         JDBCUtils.freeResources(trs, stmt, null);
      }
   }

}
