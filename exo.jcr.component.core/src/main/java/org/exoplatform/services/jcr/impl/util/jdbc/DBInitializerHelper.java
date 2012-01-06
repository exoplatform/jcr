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

import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;

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
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_MYISAM))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql-myisam.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql-utf8.sql";
      }
      else if (dbDialect.equalsIgnoreCase(DBConstants.DB_DIALECT_MYSQL_MYISAM_UTF8))
      {
         sqlPath = "/conf/storage/jcr-" + (multiDb ? "m" : "s") + "jdbc.mysql-myisam-utf8.sql";
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
         script = IOUtil.getStreamContentAsString(PrivilegedFileHelper.getResourceAsStream(scriptsPath));
      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException("Can not read script file " + scriptsPath, e);
      }

      String sql = null;
      for (String query : JDBCUtils.splitWithSQLDelimiter(script))
      {
         String q = JDBCUtils.cleanWhitespaces(query);
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
