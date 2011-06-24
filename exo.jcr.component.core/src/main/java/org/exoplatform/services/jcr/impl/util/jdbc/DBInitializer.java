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
package org.exoplatform.services.jcr.impl.util.jdbc;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generic DB initializer.
 * Created by The eXo Platform SAS 12.03.2007.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: StorageDBInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class DBInitializer
{

   static public String SQL_DELIMITER = ";";

   static public String SQL_DELIMITER_COMMENT_PREFIX = "/*$DELIMITER:";

   static public String SQL_CREATETABLE = "^(CREATE(\\s)+TABLE(\\s)+(IF(\\s)+NOT(\\s)+EXISTS(\\s)+)*){1}";

   static public String SQL_CREATEVIEW = "^(CREATE(\\s)+VIEW(\\s)+(IF(\\s)+NOT(\\s)+EXISTS(\\s)+)*){1}";

   static public String SQL_OBJECTNAME = "((JCR_[A-Z_]+){1}(\\s*?|(\\(\\))*?)+)+?";

   static public String SQL_CREATEINDEX = "^(CREATE(\\s)+(UNIQUE(\\s)+)*INDEX(\\s)+){1}";

   static public String SQL_ONTABLENAME = "(ON(\\s)+(JCR_[A-Z_]+){1}(\\s*?|(\\(\\))*?)+){1}";

   static public String SQL_CREATESEQUENCE = "^(CREATE(\\s)+SEQUENCE(\\s)+){1}";

   static public String SQL_CREATETRIGGER = "^(CREATE(\\s)+(OR(\\s){1}REPLACE(\\s)+)*TRIGGER(\\s)+){1}";

   static public String SQL_TRIGGERNAME = "(([A-Z_]+JCR_[A-Z_]+){1}(\\s*?|(\\(\\))*?)+)+?";

   protected final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBInitializer");

   protected final Connection connection;

   protected final String containerName;

   protected final String script;

   protected final Pattern creatTablePattern;

   protected final Pattern creatViewPattern;

   protected final Pattern dbObjectNamePattern;

   protected final Pattern creatIndexPattern;

   protected final Pattern onTableNamePattern;

   protected final Pattern creatSequencePattern;

   protected final Pattern creatTriggerPattern;

   protected final Pattern dbTriggerNamePattern;

   public DBInitializer(String containerName, Connection connection, String scriptPath) throws IOException
   {
      this.connection = connection;
      this.containerName = containerName;
      this.script = script(scriptPath);

      this.creatTablePattern = Pattern.compile(SQL_CREATETABLE, Pattern.CASE_INSENSITIVE);
      this.creatViewPattern = Pattern.compile(SQL_CREATEVIEW, Pattern.CASE_INSENSITIVE);
      this.dbObjectNamePattern = Pattern.compile(SQL_OBJECTNAME, Pattern.CASE_INSENSITIVE);
      this.creatIndexPattern = Pattern.compile(SQL_CREATEINDEX, Pattern.CASE_INSENSITIVE);
      this.onTableNamePattern = Pattern.compile(SQL_ONTABLENAME, Pattern.CASE_INSENSITIVE);
      this.creatSequencePattern = Pattern.compile(SQL_CREATESEQUENCE, Pattern.CASE_INSENSITIVE);
      this.creatTriggerPattern = Pattern.compile(SQL_CREATETRIGGER, Pattern.CASE_INSENSITIVE);
      this.dbTriggerNamePattern = Pattern.compile(SQL_TRIGGERNAME, Pattern.CASE_INSENSITIVE);
   }

   protected String script(String scriptPath) throws IOException
   {
      return readScriptResource(scriptPath);
   }

   protected String readScriptResource(final String path) throws IOException
   {
      PrivilegedAction<InputStream> action = new PrivilegedAction<InputStream>()
      {
         public InputStream run()
         {
            return this.getClass().getResourceAsStream(path);
         }
      };
      final InputStream is = SecurityHelper.doPrivilegedAction(action);

      PrivilegedAction<InputStreamReader> actionGetReader = new PrivilegedAction<InputStreamReader>()
      {
         public InputStreamReader run()
         {
            return new InputStreamReader(is);
         }
      };
      InputStreamReader isr = SecurityHelper.doPrivilegedAction(actionGetReader);

      try
      {
         StringBuilder sbuff = new StringBuilder();
         char[] buff = new char[is.available()];
         int r = 0;
         while ((r = isr.read(buff)) > 0)
         {
            sbuff.append(buff, 0, r);
         }

         return sbuff.toString();
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
   }

   public String cleanWhitespaces(String string)
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

   protected boolean isTableExists(final Connection conn, final String tableName) throws SQLException
   {
      return SecurityHelper.doPrivilegedAction(new PrivilegedAction<Boolean>()
      {
         public Boolean run()
         {
            return JDBCUtils.tableExists(tableName, conn);
         }
      });
   }

   protected boolean isSequenceExists(Connection conn, String sequenceName) throws SQLException
   {
      return false;
   }

   private boolean isObjectExists(Connection conn, String sql, Set<String> existingTables) throws SQLException
   {
      Matcher tMatcher = creatTablePattern.matcher(sql);
      if (tMatcher.find())
      {
         // CREATE TABLE
         tMatcher = dbObjectNamePattern.matcher(sql);
         if (tMatcher.find())
         {
            // got table name
            String tableName = sql.substring(tMatcher.start(), tMatcher.end());
            if (isTableExists(conn, tableName))
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Table is already exists " + tableName);
               }
               existingTables.add(tableName);
               return true;
            }
         }
      }
      else if ((tMatcher = creatViewPattern.matcher(sql)).find())
      {
         // CREATE VIEW
         tMatcher = dbObjectNamePattern.matcher(sql);
         if (tMatcher.find())
         {
            // got view name
            String tableName = sql.substring(tMatcher.start(), tMatcher.end());
            if (isTableExists(conn, tableName))
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("View is already exists " + tableName);
               }
               existingTables.add(tableName);
               return true;
            }
         }
      }
      else if ((tMatcher = creatIndexPattern.matcher(sql)).find())
      {
         // CREATE INDEX
         tMatcher = dbObjectNamePattern.matcher(sql);
         if (tMatcher.find())
         {
            // got index name
            String indexName = sql.substring(tMatcher.start(), tMatcher.end());
            if ((tMatcher = onTableNamePattern.matcher(sql)).find())
            {
               String onTableName = sql.substring(tMatcher.start(), tMatcher.end());
               if ((tMatcher = dbObjectNamePattern.matcher(onTableName)).find())
               {
                  String tableName = onTableName.substring(tMatcher.start(), tMatcher.end());
                  if (existingTables.contains(tableName))
                  {
                     if (LOG.isDebugEnabled())
                     {
                        LOG.debug("The table " + tableName + " already exists so we assume that the index " + indexName
                           + " exists also.");
                     }
                     return true;
                  }
               }
               else
               {
                  LOG.warn("Index found but $TABLE_NAME is not detected '" + sql + "'");
               }
            }
            else
            {
               LOG.warn("Index found but ON $TABLE_NAME clause is not detected '" + sql + "'");
            }
         }
         else
         {
            LOG.warn("Create index clause found but $INDEX_NAME is not detected '" + sql + "'");
         }
      }
      else if ((tMatcher = creatSequencePattern.matcher(sql)).find())
      {
         tMatcher = dbObjectNamePattern.matcher(sql);
         if (tMatcher.find())
         {
            // got sequence name
            String sequenceName = sql.substring(tMatcher.start(), tMatcher.end());
            if (isSequenceExists(conn, sequenceName))
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("Sequence is already exists " + sequenceName);
               }
               return true;
            }
         }
      }
      else if ((tMatcher = creatTriggerPattern.matcher(sql)).find())
      {
         tMatcher = dbTriggerNamePattern.matcher(sql);
         if (tMatcher.find())
         {
            // got trigger name
            String triggerName = sql.substring(tMatcher.start(), tMatcher.end());
            if (!existingTables.isEmpty())
            {
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("At least one table has been created so we assume that the trigger " + triggerName
                     + " exists also");
               }
               return true;
            }
         }
      }
      else
      {
         if (LOG.isDebugEnabled())
         {
            LOG.debug("Command is not detected for check '" + sql + "'");
         }
      }

      return false;
   }

   public void init() throws DBInitializerException
   {
      String[] scripts = null;
      if (script.startsWith(SQL_DELIMITER_COMMENT_PREFIX))
      {
         // read custom prefix
         try
         {
            String s = script.substring(SQL_DELIMITER_COMMENT_PREFIX.length());
            int endOfDelimIndex = s.indexOf("*/");
            String delim = s.substring(0, endOfDelimIndex).trim();
            s = s.substring(endOfDelimIndex + 2).trim();
            scripts = s.split(delim);
         }
         catch (IndexOutOfBoundsException e)
         {
            LOG.warn("Error of parse SQL-script file. Invalid DELIMITER configuration. Valid format is '"
               + SQL_DELIMITER_COMMENT_PREFIX + "XXX*/' at begin of the SQL-script file, where XXX - DELIMITER string."
               + " Spaces will be trimed. ", e);
            LOG.info("Using DELIMITER:[" + SQL_DELIMITER + "]");
            scripts = script.split(SQL_DELIMITER);
         }
      }
      else
      {
         scripts = script.split(SQL_DELIMITER);
      }

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
            String s = cleanWhitespaces(scr.trim());
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
         connection.commit();
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
            SQLException next = e.getNextException();
            String errorTrace = "";
            while (next != null)
            {
               errorTrace += next.getMessage() + "; ";
               next = next.getNextException();
            }
            Throwable cause = e.getCause();
            String msg =
               "Could not create db schema of DataSource: '" + containerName + "'. Reason: " + e.getMessage() + "; "
                  + errorTrace + (cause != null ? " (Cause: " + cause.getMessage() + ")" : "") + ". Last command: "
                  + sql;

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

   /**
    * Place to perform additional operations in overriden classes.
    */
   protected void postInit(Connection connection) throws SQLException
   {

   }
}
