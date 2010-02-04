/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: OracleLockDBInitializer.java 111 2008-11-11 11:11:11Z serg $
 */
public class OracleLockDBInitializer extends LockDBInitializer
{

   public OracleLockDBInitializer(String containerName, Connection connection, String script, String tableName)
      throws IOException
   {
      super(containerName, connection, script, tableName);
   }

   @Override
   protected boolean isSequenceExists(Connection conn, String sequenceName) throws SQLException
   {
      try
      {
         ResultSet srs = conn.createStatement().executeQuery("SELECT " + sequenceName + ".nextval FROM DUAL");
         if (srs.next())
         {
            return true;
         }
         srs.close();
         return false;
      }
      catch (SQLException e)
      {
         // check: ORA-02289: sequence does not exist
         if (e.getMessage().indexOf("ORA-02289") >= 0)
            return false;
         throw e;
      }
   }

   @Override
   protected boolean isTriggerExists(Connection conn, String triggerName) throws SQLException
   {
      String sql = "SELECT COUNT(trigger_name) FROM all_triggers WHERE trigger_name = '" + triggerName + "'";
      ResultSet r = conn.createStatement().executeQuery(sql);
      if (r.next())
         return r.getInt(1) > 0;
      else
         return false;
   }

   @Override
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      try
      {
         conn.createStatement().executeUpdate("SELECT 1 FROM " + tableName);
         return true;
      }
      catch (SQLException e)
      {
         // check: ORA-00942: table or view does not exist
         if (e.getMessage().indexOf("ORA-00942") >= 0)
            return false;
         throw e;
      }
   }

   @Override
   protected boolean isIndexExists(Connection conn, String tableName, String indexName) throws SQLException
   {
      // use of oracle system view
      String sql = "SELECT COUNT(index_name) FROM all_indexes WHERE index_name='" + indexName + "'";
      ResultSet r = conn.createStatement().executeQuery(sql);
      if (r.next())
         return r.getInt(1) > 0;
      else
         return false;
   }
}
