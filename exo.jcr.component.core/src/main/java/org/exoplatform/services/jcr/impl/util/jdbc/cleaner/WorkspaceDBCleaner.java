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
package org.exoplatform.services.jcr.impl.util.jdbc.cleaner;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.core.lock.cacheable.AbstractCacheableLockManager;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The goal of this class is removing workspace data from database.
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id$
 */
public abstract class WorkspaceDBCleaner implements DBCleaner
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceDBCleaner");

   /**
    * Container name.
    */
   protected final String containerName;

   /**
    * Connection to database.
    */
   protected final Connection connection;

   /**
    * Pattern for JCR tables.
    */
   protected final Pattern dbObjectNamePattern;

   /**
    * Common clean scripts for database.
    */
   protected final List<String> commonDBCleanScripts = new ArrayList<String>();

   /**
    * WorkspaceDBCleaner constructor.
    * 
    * @param containerName 
    *          container name (workspace name)
    * @param connection 
    *          connection to database where workspace tables is placed
    */
   public WorkspaceDBCleaner(WorkspaceEntry wsEntry, Connection connection)
   {
      this.dbObjectNamePattern = Pattern.compile(DBInitializer.SQL_OBJECTNAME, Pattern.CASE_INSENSITIVE);
      this.connection = connection;
      this.containerName = wsEntry.getName();

      LockManagerEntry lockEntry = wsEntry.getLockManager();
      if (lockEntry != null && lockEntry.getParameters() != null)
      {
         for (String tableName : AbstractCacheableLockManager.getLockTableNames(lockEntry))
         {
            commonDBCleanScripts.add("drop table " + tableName);
         }
      }
   }

   /**
    * Remove workspace data from database used specified queries.
    * <p>Connection used by this method will be closed at final.
    * 
    * @throws DBCleanerException
    *          if exception during data cleanup occurred
    */
   public void clean() throws DBCleanerException
   {
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      String sql = null;
      Statement st = null;
      try
      {
         connection.setAutoCommit(false);
         st = connection.createStatement();
         for (String scr : getDBCleanScripts())
         {
            String s = cleanWhitespaces(scr.trim());
            if (s.length() > 0)
            {
               if (!canExecuteQuery(sql = s))
               {
                  // table from query not found, so try drop other
                  continue;
               }

               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Execute script: \n[" + sql + "]");
               }
               executeQuery(st, sql);
            }
         }

         connection.commit();
      }
      catch (SQLException e)
      {
         try
         {
            connection.rollback();
         }
         catch (SQLException rollbackException)
         {
            LOG.error("Can not rollback changes after exception " + e.getMessage(), rollbackException);
         }
         throw new DBCleanerException(e.getMessage(), e);
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

         closeConnection();
      }
   }

   /**
    * Close connection.
    * 
    * @throws SQLException
    */
   protected void closeConnection()
   {
      try
      {
         connection.close();
      }
      catch (SQLException e)
      {
         LOG.error("Error of a connection closing. " + e, e);
      }
   }

   /**
    * Check if we can execute query.
    * If tables used in query does not exists, we can not execute query.
    */
   protected boolean canExecuteQuery(String sql) throws SQLException
   {
      Matcher tMatcher = dbObjectNamePattern.matcher(sql);
      while (tMatcher.find())
      {
         // get table name
         String tableName = sql.substring(tMatcher.start(), tMatcher.end());
         if (!isTableExists(connection, tableName))
         {
            LOG.warn("Table [" + tableName + "] from query [" + sql + "] was not found. So query will not be executed.");
            return false;
         }
      }
      return true;
   }

   /**
    * Execute query.
    */
   protected void executeQuery(final Statement statement, final String sql) throws SQLException
   {
      SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            statement.executeUpdate(sql);
            return null;
         }
      });
   }

   /**
    * Indicates if table exists or not.
    */
   protected boolean isTableExists(Connection conn, String tableName) throws SQLException
   {
      ResultSet trs = conn.getMetaData().getTables(null, null, tableName, null);
      try
      {
         boolean res = false;
         while (trs.next())
         {
            res = true; // check for columns/table type matching etc.
         }
         return res;
      }
      finally
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
   }

   /**
    * Cleans redundant whitespaces from query.
    */
   private String cleanWhitespaces(String string)
   {
      if (string != null)
      {
         char[] cc = string.toCharArray();
         for (int ci = cc.length - 1; ci > 0; ci--)
         {
            if (Character.isWhitespace(cc[ci]))
            {
               cc[ci] = ' ';
            }
         }
         return new String(cc);
      }
      return string;
   }

   /**
    * Get SQL scripts for data cleaning.
    * 
    * @return
    *          List of sql scripts
    */
   abstract List<String> getDBCleanScripts();

}
