/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.PrivilegedAction;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: DBInitializerHelper.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class DBInitializerHelper
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBInitializerHelper");
   
   /**
    * Default SQL delimiter.
    */
   static public String SQL_DELIMITER = ";";

   /**
    * SQL delimiter comment prefix.
    */
   static public String SQL_DELIMITER_COMMENT_PREFIX = "/*$DELIMITER:";

   /**
    * Getting path to initialization by specific dialect and multidb.
    * 
    * @param dbDialect
    *          String
    * @param multiDb
    *          Boolean
    * @return String
    *           Path to DB initialization script.
    */
   public static String scriptPath(String dbDialect, boolean multiDb)
   {
      String sqlPath = null;
      if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ora.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_ORACLE))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ora.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_PGSQL))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.pgsql.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql-utf8.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MSSQL))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mssql.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_DERBY))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.derby.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_DB2))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.db2.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_DB2V8))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.db2v8.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_SYBASE))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.sybase.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_INGRES))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.ingres.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_HSQLDB))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.sql";
      }
      else
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.sql";
      }

      return sqlPath;
   }

   /**
    * Validate dialect.
    * 
    * @param confParam
    *          String, dialect from configuration.
    * @return String
    *           return dialect. By default return DB_DIALECT_GENERIC. 
    * 
    */
   public static String validateDialect(String confParam)
   {
      for (String dbType : DBConstants.DB_DIALECTS)
      {
         if (dbType.equalsIgnoreCase(confParam))
         {
            return dbType;
         }
      }

      return DBConstants.DB_DIALECT_GENERIC; // by default
   }

   /**
    * Read DB initialization script as string.
    * 
    * @param path
    *          String, path to DB initialization script.
    * @return String,
    *           DB initialization script as string.
    * @throws IOException
    *           Will throw IOException if file with DB initialization script is not exists.
    */
   public static String readScriptResource(final String path) throws IOException
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
   
   /**
    * Determinate DB initialization script by separate query.
    * 
    * @param script
    *         String, DB initialization script as String. 
    * @return String[]
    *           Queries to DB initialization.
    */
   public static String[] scripts(String script)
   {
      if (script.startsWith(SQL_DELIMITER_COMMENT_PREFIX))
      {
         // read custom prefix
         try
         {
            String s = script.substring(SQL_DELIMITER_COMMENT_PREFIX.length());
            int endOfDelimIndex = s.indexOf("*/");
            String delim = s.substring(0, endOfDelimIndex).trim();
            s = s.substring(endOfDelimIndex + 2).trim();
            return s.split(delim);
         }
         catch (IndexOutOfBoundsException e)
         {
            LOG.warn("Error of parse SQL-script file. Invalid DELIMITER configuration. Valid format is '"
                     + SQL_DELIMITER_COMMENT_PREFIX
                     + "XXX*/' at begin of the SQL-script file, where XXX - DELIMITER string."
                     + " Spaces will be trimed. ", e);
            LOG.info("Using DELIMITER:[" + SQL_DELIMITER + "]");
            return script.split(SQL_DELIMITER);
         }
      }
      else
      {
         return script.split(SQL_DELIMITER);
      }
   }

   /**
    * Initialization script for root node.
    * 
    * @param multiDb
    *          indicates if we have multi-db configuration or not
    * @return SQL script
    */
   public static String getRootNodeInitializeScript(boolean multiDb)
   {
      return "insert into JCR_" + (multiDb ? "M" : "S") + "ITEM(ID, PARENT_ID, NAME, " + (multiDb ? "" : "CONTAINER_NAME, ") 
         + "VERSION, I_CLASS, I_INDEX, N_ORDER_NUM)" + " VALUES('"
         + Constants.ROOT_PARENT_UUID + "', '" + Constants.ROOT_PARENT_UUID + "', '" + Constants.ROOT_PARENT_NAME
         + "', " + (multiDb ? "" : "'" + Constants.ROOT_PARENT_CONAINER_NAME + "', ") + "0, 0, 0, 0)";
   }

   /**
    * Cleans redundant whitespaces from query.
    */
   public static String cleanWhitespaces(String string)
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
    * Get script for creating object (index, etc...)
    * @throws RepositoryConfigurationException 
    */
   public static String getObjectScript(String objectName, boolean multiDb, String dialect)
      throws RepositoryConfigurationException
   {
      String scriptsPath = DBInitializerHelper.scriptPath(dialect, multiDb);
      String script;
      try
      {
         script = DBInitializerHelper.readScriptResource(scriptsPath);
      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException("Can not read script file " + scriptsPath, e);
      }

      String sql = null;
      for (String query : DBInitializerHelper.scripts(script))
      {
         String q = DBInitializerHelper.cleanWhitespaces(query);
         if (q.contains(objectName))
         {
            if (sql != null)
            {
               throw new RepositoryConfigurationException("Can't find unique script for object creation. Object name: "
                  + objectName);
            }

            sql = q;
         }
      }

      if (sql != null)
      {
         return sql;
      }

      throw new RepositoryConfigurationException("Script for object creation is not found. Object name: " + objectName);
   }
}
