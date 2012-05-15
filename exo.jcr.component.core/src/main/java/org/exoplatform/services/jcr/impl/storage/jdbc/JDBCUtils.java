/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * This class provides JDBC tools
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class JDBCUtils
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JDBCUtils");

   private JDBCUtils()
   {
   }

   /**
    * Indicates whether or not a given table exists
    * @param tableName the name of the table to check
    * @param con the connection to use
    * @return <code>true</code> if it exists, <code>false</code> otherwise
    */
   public static boolean tableExists(String tableName, Connection con)
   {
      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String dialect = DialectDetecter.detect(con.getMetaData());
         String query;
         if (DBConstants.DB_DIALECT_MYSQL.equals(dialect))
         {
            query = "SELECT count(*) from (SELECT 1 FROM " + tableName + " LIMIT 1) T";
         }
         else
         {
            query = "SELECT count(*) FROM " + tableName;
         }
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         return trs.next();
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while checking the table " + tableName, e);
         }
         return false;
      }
      finally
      {
         freeResources(trs, stmt, null);
      }
   }

   /**
    * Closes database related resources.
    */
   public static void freeResources(ResultSet resultSet, Statement statement, Connection conn)
   {
      if (resultSet != null)
      {
         try
         {
            resultSet.close();
         }
         catch (SQLException e)
         {
            LOG.error(e.getMessage(), e);
         }
      }

      if (statement != null)
      {
         try
         {
            statement.close();
         }
         catch (SQLException e)
         {
            LOG.error(e.getMessage(), e);
         }
      }

      if (conn != null)
      {
         try
         {
            conn.close();
         }
         catch (SQLException e)
         {
            LOG.error(e.getMessage(), e);
         }
      }
   }
}
