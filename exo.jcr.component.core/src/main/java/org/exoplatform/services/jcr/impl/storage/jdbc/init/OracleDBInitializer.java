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

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by The eXo Platform SAS
 * 
 * 22.03.2007
 * 
 * For statistic compute on a user schema (PL/SQL): exec
 * DBMS_STATS.GATHER_SCHEMA_STATS(ownname=>'exoadmin')
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: OracleDBInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class OracleDBInitializer extends StorageDBInitializer
{

   public OracleDBInitializer(String containerName, Connection connection, String scriptPath, boolean multiDb)
      throws IOException
   {
      super(containerName, connection, scriptPath, multiDb);
   }

   @Override
   protected boolean isSequenceExists(Connection conn, String sequenceName) throws SQLException
   {
      ResultSet srs = null;
      Statement st = null;
      try
      {
         st = conn.createStatement();
         srs = st.executeQuery("SELECT " + sequenceName + ".nextval FROM DUAL");
         if (srs.next())
         {
            return true;
         }
         return false;
      }
      catch (SQLException e)
      {
         // check: ORA-02289: sequence does not exist
         if (e.getMessage().indexOf("ORA-02289") >= 0)
            return false;
         throw e;
      }
      finally
      {
         if (srs != null)
         {
            try
            {
               srs.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }

         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the Statement: " + e);
            }
         }
      }
   }

   @Override
   protected boolean isTriggerExists(Connection conn, String triggerName) throws SQLException
   {
      String sql = "SELECT COUNT(trigger_name) FROM all_triggers WHERE trigger_name = '" + triggerName + "'";
      Statement st = null;
      ResultSet r = null;
      try
      {
         st = conn.createStatement();
         r = st.executeQuery(sql);

         if (r.next())
            return r.getInt(1) > 0;
         else
            return false;
      }
      finally
      {
         if (r != null)
         {
            try
            {
               r.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }

         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the Statement: " + e);
            }
         }
      }
   }

   @Override
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      Statement st = null;
      try
      {
         st = conn.createStatement();
         st.executeUpdate("SELECT 1 FROM " + tableName);
         return true;
      }
      catch (SQLException e)
      {
         // check: ORA-00942: table or view does not exist
         if (e.getMessage().indexOf("ORA-00942") >= 0)
            return false;
         throw e;
      }
      finally
      {
         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the Statement: " + e);
            }
         }
      }
   }

   @Override
   protected boolean isIndexExists(Connection conn, String tableName, String indexName) throws SQLException
   {
      // use of oracle system view
      String sql = "SELECT COUNT(index_name) FROM all_indexes WHERE index_name='" + indexName + "'";
      Statement st = null;
      ResultSet r = null;
      try
      {
         st = conn.createStatement();
         r = st.executeQuery(sql);

         if (r.next())
            return r.getInt(1) > 0;
         else
            return false;
      }
      finally
      {
         if (r != null)
         {
            try
            {
               r.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the ResultSet: " + e);
            }
         }

         if (st != null)
         {
            try
            {
               st.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can't close the Statement: " + e);
            }
         }

      }
   }
}
