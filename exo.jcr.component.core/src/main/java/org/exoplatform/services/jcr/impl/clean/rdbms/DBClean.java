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
package org.exoplatform.services.jcr.impl.clean.rdbms;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
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
 * @version $Id: DBClean.java 3769 2011-01-04 15:36:06Z areshetnyak $
 */
public class DBClean
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBClean");

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
   protected final List<String> cleanScripts = new ArrayList<String>();

   /**
    * DB clean helper.
    */
   protected final DBCleanHelper dbCleanHelper;

   /**
    * WorkspaceDBCleaner constructor.
    * 
    * @param containerName 
    *          container name (workspace name)
    * @param connection 
    *          connection to database where workspace tables is placed
    */
   public DBClean(Connection connection, List<String> cleanScripts)
   {
      this(connection, cleanScripts, null);
   }

   /**
    * WorkspaceDBCleaner constructor.
    * 
    * @param containerName 
    *          container name (workspace name)
    * @param connection 
    *          connection to database where workspace tables is placed
    * @param dbCleanHelper
    */
   public DBClean(Connection connection, List<String> cleanScripts, DBCleanHelper dbCleanHelper)
   {
      this.dbObjectNamePattern = Pattern.compile(DBInitializer.SQL_OBJECTNAME, Pattern.CASE_INSENSITIVE);
      this.connection = connection;
      this.cleanScripts.addAll(cleanScripts);
      this.dbCleanHelper = dbCleanHelper;
   }

   /**
    * Clean data from database. The method doesn't close connection or perform commit.
    * 
    * @throws SQLException
    *          if any errors occurred 
    */
   public void clean() throws SQLException
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
         st = connection.createStatement();
         for (String scr : cleanScripts)
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

         if (dbCleanHelper != null)
         {
            dbCleanHelper.clean();
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
               LOG.error("Can't close the Statement." + e);
            }
         }
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
      return JDBCUtils.tableExists(tableName, conn);
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
}
