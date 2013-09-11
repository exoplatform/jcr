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
         if (dialect.startsWith(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_PGSQL))
         {
            query = "SELECT count(*) from (SELECT 1 FROM " + tableName + " LIMIT 1) T";
         }
         else if (dialect.startsWith(DBConstants.DB_DIALECT_ORACLE))
         {
            query = "SELECT count(*) from (SELECT 1 FROM " + tableName + " WHERE ROWNUM = 1) T";
         }
         else if (dialect.startsWith(DBConstants.DB_DIALECT_DB2) || dialect.equals(DBConstants.DB_DIALECT_DERBY)
            || dialect.equals(DBConstants.DB_DIALECT_INGRES))
         {
            query = "SELECT count(*) from (SELECT 1 FROM " + tableName + " FETCH FIRST 1 ROWS ONLY) T";
         }
         else if (dialect.equals(DBConstants.DB_DIALECT_MSSQL))
         {
            query = "SELECT count(*) from (SELECT TOP (1) 1 as C FROM " + tableName + ") T";
         }
         else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
         {
            query = "SELECT count(*) from (SELECT TOP 1 1 FROM " + tableName + ") T";
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

   public static boolean sequenceExists(String sequenceName, Connection con)
   {
      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String dialect = DialectDetecter.detect(con.getMetaData());
         String query;
         if (dialect.startsWith(DBConstants.DB_DIALECT_DB2))
         {
            query = "SELECT count(*) FROM SYSCAT.SEQUENCES WHERE SYSCAT.SEQUENCES.SEQNAME = '" + sequenceName + "'";
         }
         else
         {
            return false;
         }
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         if (trs.next() && trs.getInt(1) >= 1)
         {
            return true;
         }
         else
         {
            return false;
         }

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
         freeResources(trs, stmt, null);
      }
   }

   public static String setStartValue(Connection con)
   {

      Statement stmt = null;
      ResultSet trs = null;
      try
      {
         String query;

         if (tableExists("JCR_SITEM", con))
         {
            query = "select max(N_ORDER_NUM) from JCR_SITEM";
         }
         else if (tableExists("JCR_MITEM", con))
         {
            query = "select max(N_ORDER_NUM) from JCR_MITEM";
         }
         else
         {
            return " Start with -1";
         }
         stmt = con.createStatement();
         trs = stmt.executeQuery(query);
         if (trs.next() && trs.getInt(1) > 0)
         {
            return " Start with " + trs.getString(1);
         }
         else
         {
            return " Start with -1";
         }

      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while update the sequence start value", e);
         }
         return " Start with -1";
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
