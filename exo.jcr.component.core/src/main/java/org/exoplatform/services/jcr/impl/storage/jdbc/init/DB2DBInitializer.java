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
import org.exoplatform.services.database.utils.ExceptionManagementHelper;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerException;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

/**
 * JCR Storage DB2 initializer.
 *
 * Created by The eXo Platform SAS* 11.09.2013
 *
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 */

public class DB2DBInitializer extends StorageDBInitializer
{
   public DB2DBInitializer(String containerName, Connection connection, String scriptPath, boolean multiDb) throws IOException
   {
      super(containerName, connection, scriptPath, multiDb);
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
            return JDBCUtils.sequenceExists(sequenceName, conn);
         }
      });
   }

   protected String setSequenceStartValue(final Connection conn) throws SQLException
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<String>()
      {
         public String run()
         {
            return JDBCUtils.setStartValue(conn);
         }
      });
   }
   /**
    * {@inheritDoc}
    */
   @Override
   public void init() throws DBInitializerException
   {
      String[] scripts = DBInitializerHelper.scripts(script);
      String sql = null;
      Statement st = null;
      Set<String> existingTables = new HashSet<String>();
      try
      {
         st = connection.createStatement();
         // all DDL queries executed in separated transactions
         // Required for SyBase, when checking table existence
         // and performing DDLs inside single transaction.
         connection.setAutoCommit(true);
         for (String scr : scripts)
         {
            String s = DBInitializerHelper.cleanWhitespaces(scr.trim());
            if (s.length() > 0)
            {
               if (isObjectExists(connection, sql = s, existingTables))
               {
                  continue;
               }

               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Execute script: \n[" + sql + "]");
               }
               if ((creatSequencePattern.matcher(sql)).find())
               {
                  sql = sql.concat(setSequenceStartValue(connection));
               }
               final Statement finalSt = st;
               final String finalSql = sql;
               SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
               {
                  public Object run() throws Exception
                  {
                     finalSt.executeUpdate(finalSql);
                     return null;
                  }
               });
            }
         }

         postInit(connection);
         LOG.info("DB schema of DataSource: '" + containerName + "' initialized succesfully");
      }
      catch (SQLException e)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.error("Problem creating database structure.", e);
         }
         LOG
            .warn("Some tables were created and not rolled back. Please make sure to drop them manually in datasource : '"
               + containerName + "'");

         boolean isAlreadyCreated = false;
         try
         {
            isAlreadyCreated = isObjectExists(connection, sql, existingTables);
         }
         catch (SQLException ce)
         {
            LOG.warn("Can not check does the objects from " + sql + " exists");
         }

         if (isAlreadyCreated)
         {
            LOG.warn("Could not create db schema of DataSource: '" + containerName + "'. Reason: Objects form " + sql
               + " already exists");
         }
         else
         {
            String msg =
               "Could not create db schema of DataSource: '" + containerName + "'. Reason: " + e.getMessage() + "; "
                  + ExceptionManagementHelper.getFullSQLExceptionMessage(e) + ". Last command: " + sql;

            throw new DBInitializerException(msg, e);
         }
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

         try
         {
            connection.close();
         }
         catch (SQLException e)
         {
            LOG.error("Error of a connection closing. " + e, e);
         }
      }
   }
}
