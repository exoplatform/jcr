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
import java.sql.PreparedStatement;
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
public class DBCleaner
{
   public final String CLEAN_JCR_SITEM_AS_DEFAULT = "/*$CLEAN_JCR_SITEM_DEFAULT*/";

   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleaner");

   protected final int MAX_IDS_RETURNED = 100;

   protected String GET_CHILD_IDS;

   protected String REMOVE_ITEMS;

   protected final String containerName;

   protected final Pattern dbObjectNamePattern;

   protected final Connection connection;

   protected String[] scripts;

   protected final boolean isMultiDB;

   /**
    * Constructor.
    * 
    * @param containerName - container name (a.k.a workspace name)
    * @param connection - connection to database where workspace tables is placed
    * @param inputStream - inputStream from script file
    * @param isMultiDB - isMultiDB
    * @throws IOException - if exception occures on parsing script file input stream
    */
   public DBCleaner(String containerName, Connection connection, InputStream inputStream, boolean isMultiDB)
      throws IOException
   {
      this.dbObjectNamePattern = Pattern.compile(DBInitializer.SQL_OBJECTNAME, Pattern.CASE_INSENSITIVE);
      this.connection = connection;
      // parse script
      this.scripts = readScriptResource(inputStream);
      this.containerName = containerName;
      this.isMultiDB = isMultiDB;
      prepareQueries();
   }

   protected void prepareQueries()
   {
      GET_CHILD_IDS =
         "select ID from JCR_SITEM where CONTAINER_NAME=? and ID not in(select PARENT_ID from JCR_SITEM where CONTAINER_NAME=?)";

      REMOVE_ITEMS = "delete from JCR_SITEM where ID in( ? )";
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
                  // table from query not found , so try drop other
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

   /**
    * The default implementation of remove rows from JCR_SITEM table.
    * Some database do not support cascade delete, or need special sittings, so 
    * query "delete from JCR_SITEM where CONTAINER_NAME=?" may cause constraint violation exception.
    * This method takes a leafs (child nodes/properties) and remove them till, no one record is left.
    * 
    * @throws SQLException - SQL exception. 
    */
   protected void clearItemsByDefault(Connection connection, String containerName) throws SQLException
   {
      // Remove only child nodes in cycle, till all nodes will be removed.
      // Such algorithm used to avoid any constraint violation exception related to foreign key.
      PreparedStatement getChildItems = null;
      final Statement removeItems = connection.createStatement();

      try
      {
         getChildItems = connection.prepareStatement(GET_CHILD_IDS);
         getChildItems.setString(1, containerName);
         getChildItems.setString(2, containerName);

         getChildItems.setMaxRows(MAX_IDS_RETURNED);

         do
         {
            final PreparedStatement getChildIds = getChildItems;
            ResultSet result =
               (ResultSet)SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
               {
                  public Object run() throws Exception
                  {
                     return getChildIds.executeQuery();
                  }
               });

            StringBuilder childListBuilder = new StringBuilder();
            if (result.next())
            {
               childListBuilder.append("'" + result.getString(1) + "'");
            }
            else
            {
               break;
            }
            while (result.next())
            {
               childListBuilder.append(" , '" + result.getString(1) + "'");
            }
            // now remove nodes;
            final String q = REMOVE_ITEMS.replace("?", childListBuilder.toString());
            SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
            {
               public Object run() throws Exception
               {
                  removeItems.executeUpdate(q);
                  return null;
               }
            });
         }
         while (true);
      }
      finally
      {
         if (getChildItems != null)
         {
            getChildItems.close();
            getChildItems = null;
         }
         if (removeItems != null)
         {
            removeItems.close();
         }
      }
   }

   /**
    * Check can we safely execute query.
    * If tables used in query does not exists ,we can not execute query.
    */
   protected boolean canExecuteQuery(Connection conn, String sql) throws SQLException
   {
      if (!isMultiDB && sql.equalsIgnoreCase(CLEAN_JCR_SITEM_AS_DEFAULT))
      {
         // check queries used in clearItemsByDefault
         if (!canExecuteQuery(conn, GET_CHILD_IDS))
         {
            return false;
         }
         if (!canExecuteQuery(conn, REMOVE_ITEMS))
         {
            return false;
         }
         return true;
      }
      else
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
   }

   /**
    * Cleans redundant whitespaces from query.
    */
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

   /**
    * Execute query.
    */
   protected void executeQuery(final Statement statement, String sql) throws SQLException
   {
      if (!isMultiDB && sql.equalsIgnoreCase(CLEAN_JCR_SITEM_AS_DEFAULT))
      {
         clearItemsByDefault(statement.getConnection(), containerName);
      }
      else
      {
         // in case of singleDB query -  check query for "?" mask and replace it with containerName
         final String q = (containerName != null) ? sql.replace("?", "'" + containerName + "'") : sql;
         //super.executeQuery(statement, q);
         SecurityHelper.doPriviledgedSQLExceptionAction(new PrivilegedExceptionAction<Object>()
         {
            public Object run() throws Exception
            {
               statement.executeUpdate(q);
               return null;
            }
         });
      }
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

   /**
    * Extracts SQL queries from script file input stream. 
    */
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
