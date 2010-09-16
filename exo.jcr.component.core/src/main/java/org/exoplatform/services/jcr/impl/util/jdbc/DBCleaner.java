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
package org.exoplatform.services.jcr.impl.util.jdbc;

import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.util.SecurityHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The goal of this class is remove workspace data from database.
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: DBCleaner.java 111 2008-11-11 11:11:11Z serg $
 */
public abstract class DBCleaner
{

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleaner");

   protected final Pattern dbObjectNamePattern;

   protected final Connection connection;

   protected String[] scripts;

   /**
    * Constructor.
    * 
    * @param containerName - workspace name
    * @param connection - SQL conneciton
    */
   public DBCleaner(Connection connection, InputStream inputStream) throws IOException
   {
      this.dbObjectNamePattern = Pattern.compile(DBInitializer.SQL_OBJECTNAME, Pattern.CASE_INSENSITIVE);
      this.connection = connection;
      // parse script
      this.scripts = readScriptResource(inputStream);
   }

   /**
    * Remove workspace data from database.
    * <ul>
    * <li>If workspace uses multiDB data source - tables associated with this workspace
    * will be dropped.
    * <li>If workspace uses singleDB data source - all records of this workspace will
    * be removed.
    * </ul> 
    *  
    * <p>Connection used by this method will be closed at final.
    * 
    * @throws DBCleanerException - if exception during data cleanup occures.
    */
   public void cleanWorkspace() throws DBCleanerException
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
         for (String scr : scripts)
         {
            String s = cleanWhitespaces(scr.trim());
            if (s.length() > 0)
            {
               if (!canExecuteQuery(connection, sql = s))
               {
                  // table not found , so try drop other
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

   protected boolean canExecuteQuery(Connection conn, String sql) throws SQLException
   {
      return isTablesFromQueryExists(conn, sql);
   }

   protected String cleanWhitespaces(String string)
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

   protected void executeQuery(final Statement statement, final String sql) throws SQLException
   {
      SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
      {
         public Object run() throws Exception
         {
            statement.executeUpdate(sql);
            return null;
         }
      });
   }

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

   protected boolean isTablesFromQueryExists(Connection conn, String sql) throws SQLException
   {
      Matcher tMatcher = dbObjectNamePattern.matcher(sql);
      while (tMatcher.find())
      {
         // got table name
         String tableName = sql.substring(tMatcher.start(), tMatcher.end());
         if (!isTableExists(conn, tableName))
         {
            LOG.error("Table [" + tableName + "] from query [" + sql
               + "] was not found. So query will not be executed , but will try execute next one.");
            return false;
         }
      }
      return true;
   }

   protected String[] readScriptResource(final InputStream is) throws IOException
   {
      //extract string
      PrivilegedAction<InputStreamReader> actionGetReader = new PrivilegedAction<InputStreamReader>()
      {
         public InputStreamReader run()
         {
            return new InputStreamReader(is);
         }
      };
      InputStreamReader isr = AccessController.doPrivileged(actionGetReader);

      String script = null;
      try
      {
         StringBuilder sbuff = new StringBuilder();
         char[] buff = new char[is.available()];
         int r = 0;
         while ((r = isr.read(buff)) > 0)
         {
            sbuff.append(buff, 0, r);
         }

         script = sbuff.toString();
      }
      finally
      {
         try
         {
            is.close();
         }
         catch (IOException e)
         {
         }
      }

      // parse scripts
      String[] scripts = null;
      if (script.startsWith(DBInitializer.SQL_DELIMITER_COMMENT_PREFIX))
      {
         // read custom prefix
         try
         {
            String s = script.substring(DBInitializer.SQL_DELIMITER_COMMENT_PREFIX.length());
            int endOfDelimIndex = s.indexOf("*/");
            String delim = s.substring(0, endOfDelimIndex).trim();
            s = s.substring(endOfDelimIndex + 2).trim();
            scripts = s.split(delim);
         }
         catch (IndexOutOfBoundsException e)
         {
            LOG.warn("Error of parse SQL-script file. Invalid DELIMITER configuration. Valid format is '"
               + DBInitializer.SQL_DELIMITER_COMMENT_PREFIX
               + "XXX*/' at begin of the SQL-script file, where XXX - DELIMITER string." + " Spaces will be trimed. ",
               e);
            LOG.info("Using DELIMITER:[" + DBInitializer.SQL_DELIMITER + "]");
            scripts = script.split(DBInitializer.SQL_DELIMITER);
         }
      }
      else
      {
         scripts = script.split(DBInitializer.SQL_DELIMITER);
      }
      return scripts;
   }
}
