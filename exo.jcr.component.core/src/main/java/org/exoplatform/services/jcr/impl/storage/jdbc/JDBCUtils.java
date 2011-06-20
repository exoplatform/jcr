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
import java.sql.Savepoint;
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
      Savepoint savePoint = null;
      Boolean autoCommit = null;
      try
      {
         // safe get autoCommit value
         autoCommit = con.getAutoCommit();
         // set autoCommit to true
         con.setAutoCommit(false);
         // make a savepoint (snapshot)
         savePoint = con.setSavepoint();
         stmt = con.createStatement();
         trs = stmt.executeQuery("SELECT count(*) FROM " + tableName);
         return trs.next();
      }
      catch (SQLException e)
      {
         if (savePoint != null)
         {
            try
            {
               // revert state to savePoint after failed query in transaction. This will allow following queries to 
               // be executed in an ordinary way, like no failed query existed.
               // Obligatory operation for PostgreSQL.
               con.rollback(savePoint);
            }
            catch (SQLException e1)
            {
               LOG.error("Can't rollback to savePoint", e1);
            }
         }
         if (LOG.isDebugEnabled())
         {
            LOG.debug("SQLException occurs while checking the table " + tableName, e);
         }
         return false;
      }
      finally
      {
         if (autoCommit != null)
         {
            try
            {
               con.setAutoCommit(autoCommit);
            }
            catch (SQLException e)
            {
               LOG.error("Can't set autoCommit value back", e);
            }
         }
         if (trs != null)
         {
            try
            {
               trs.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }
         if (stmt != null)
         {
            try
            {
               stmt.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the Statement: " + e);
            }
         }
      }
   }
}
