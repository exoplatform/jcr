/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.storage.jdbc.init;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;

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
 * For statistic compute on a user schema (PL/SQL):
 *    {@code exec DBMS_STATS.GATHER_SCHEMA_STATS(ownname=>'exoadmin')}
 *
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: OracleDBInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class OracleDBInitializer extends StorageDBInitializer
{

   public OracleDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
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
         {
            return false;
         }
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
}
