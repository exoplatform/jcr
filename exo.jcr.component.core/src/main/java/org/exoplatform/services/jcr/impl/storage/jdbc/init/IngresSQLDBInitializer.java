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

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by The eXo Platform SAS
 * 
 * 26.03.2007
 * 
 * Ingres convert all db object names to lower case, so respect it.
 * Same as PgSQL initializer.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: IngresSQLDBInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class IngresSQLDBInitializer extends DBInitializer
{

   public IngresSQLDBInitializer(Connection connection, JDBCDataContainerConfig containerConfig) throws IOException
   {
      super(connection, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      return super.isTableExists(conn, tableName.toUpperCase().toLowerCase());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean isSequenceExists(Connection conn, String sequenceName) throws SQLException
   {
      String seqName = sequenceName.toUpperCase().toLowerCase();
      ResultSet srs = null;
      Statement st = null;
      try
      {
         st = conn.createStatement();
         srs = st.executeQuery("SELECT NEXT VALUE FOR " + seqName);
         if (srs.next())
         {
            return true;
         }
         return false;
      }
      catch (final SQLException e)
      {
         // check if sequence does not exist
         if (e.getMessage().indexOf("DEFINE CURSOR") >= 0 && e.getMessage().indexOf("Sequence") >= 0)
         {
            return false;
         }
         throw new SQLException(e.getMessage())
         {
            /**
             * {@inheritDoc}
             */
            @Override
            public Throwable getCause()
            {
               return e;
            }
         };
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
