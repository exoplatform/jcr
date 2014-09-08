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
import org.exoplatform.services.database.utils.DialectConstants;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
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
    * Table prefix for all tables used in JCR.
    */
   public static final String JCR_TABLE_PREFIX = "JCR_";

   /**
    * Returns SQL scripts for initialization database for defined {@link JDBCDataContainerConfig}.  
    */
   public static String prepareScripts(JDBCDataContainerConfig containerConfig) throws IOException
   {
      String itemTableSuffix = getItemTableSuffix(containerConfig);
      String valueTableSuffix = getValueTableSuffix(containerConfig);
      String refTableSuffix = getRefTableSuffix(containerConfig);

      boolean isolatedDB = containerConfig.dbStructureType == DatabaseStructureType.ISOLATED;

      return prepareScripts(containerConfig.initScriptPath, itemTableSuffix, valueTableSuffix, refTableSuffix, isolatedDB);
   }

   /**
    * Returns SQL scripts for initialization database for defined {@link WorkspaceEntry}. 
    * 
    * @param wsEntry
    *          workspace configuration 
    * @param dialect
    *          database dialect which is used, since {@link JDBCWorkspaceDataContainer#DB_DIALECT} parameter 
    *          can contain {@link DialectConstants#DB_DIALECT_AUTO} value it is necessary to resolve dialect
    *          before based on database connection.
    */
   public static String prepareScripts(WorkspaceEntry wsEntry, String dialect) throws IOException,
      RepositoryConfigurationException
   {
      String itemTableSuffix = getItemTableSuffix(wsEntry);
      String valueTableSuffix = getValueTableSuffix(wsEntry);
      String refTableSuffix = getRefTableSuffix(wsEntry);

      DatabaseStructureType dbType = DBInitializerHelper.getDatabaseType(wsEntry);

      boolean isolatedDB = dbType == DatabaseStructureType.ISOLATED;
      String initScriptPath = DBInitializerHelper.scriptPath(dialect, dbType.isMultiDatabase());

      return prepareScripts(initScriptPath, itemTableSuffix, valueTableSuffix, refTableSuffix, isolatedDB);
   }

   /**
    * Preparing SQL scripts for database initialization.
    */
   private static String prepareScripts(String initScriptPath, String itemTableSuffix, String valueTableSuffix,
      String refTableSuffix, boolean isolatedDB) throws IOException
   {
      String scripts = IOUtil.getStreamContentAsString(PrivilegedFileHelper.getResourceAsStream(initScriptPath));

      if (isolatedDB)
      {
         scripts =
            scripts.replace("MITEM", itemTableSuffix).replace("MVALUE", valueTableSuffix)
               .replace("MREF", refTableSuffix);
      }

      return scripts;
   }

   /**
    * Returns path where SQL scripts for database initialization is stored.
    */
   public static String scriptPath(String dbDialect, boolean multiDb)
   {
      String suffix = multiDb ? "m" : "s";

      String sqlPath = null;
      if (dbDialect.startsWith(DBConstants.DB_DIALECT_ORACLE))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.ora.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_PGSQL))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.pgsql.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_MYSQL))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mysql.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_MYSQL_NDB))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mysql-ndb.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_MYSQL_NDB_UTF8))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mysql-ndb-utf8.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_MYSQL_MYISAM))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mysql-myisam.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mysql-utf8.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_MYSQL_MYISAM_UTF8))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mysql-myisam-utf8.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_MSSQL))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.mssql.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_DERBY))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.derby.sql";
      }
      else if (dbDialect.equals(DBConstants.DB_DIALECT_DB2V8))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.db2v8.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_DB2))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.db2.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_SYBASE))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.sybase.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_INGRES))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.ingres.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_H2))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.h2.sql";
      }
      else if (dbDialect.startsWith(DBConstants.DB_DIALECT_HSQLDB))
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.sql";
      }
      else
      {
         sqlPath = "/conf/storage/jcr-" + suffix + "jdbc.sql";
      }

      return sqlPath;
   }

   /**
    * Initialization script for root node based on {@link JDBCDataContainerConfig}.
    */
   public static String getRootNodeInitializeScript(JDBCDataContainerConfig containerConfig)
   {
      boolean multiDb = containerConfig.dbStructureType.isMultiDatabase();
      String itemTableName = getItemTableName(containerConfig);

      return getRootNodeInitializeScript(itemTableName, multiDb);
   }

   /**
    * Initialization script for root node.
    */
   public static String getRootNodeInitializeScript(String itemTableName, boolean multiDb)
   {
      String singeDbScript =
         "insert into " + itemTableName + "(ID, PARENT_ID, NAME, CONTAINER_NAME, VERSION, I_CLASS, I_INDEX, "
            + "N_ORDER_NUM) VALUES('" + Constants.ROOT_PARENT_UUID + "', '" + Constants.ROOT_PARENT_UUID + "', '"
            + Constants.ROOT_PARENT_NAME + "', '" + Constants.ROOT_PARENT_CONAINER_NAME + "', 0, 0, 0, 0)";

      String multiDbScript =
         "insert into " + itemTableName + "(ID, PARENT_ID, NAME, VERSION, I_CLASS, I_INDEX, " + "N_ORDER_NUM) VALUES('"
            + Constants.ROOT_PARENT_UUID + "', '" + Constants.ROOT_PARENT_UUID + "', '" + Constants.ROOT_PARENT_NAME
            + "', 0, 0, 0, 0)";

      return multiDb ? multiDbScript : singeDbScript;
   }

   public static String getItemTableSuffix(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      return getTableSuffix(getDatabaseType(wsConfig), getDBTableSuffix(wsConfig), "ITEM");
   }

   public static String getValueTableSuffix(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      return getTableSuffix(getDatabaseType(wsConfig), getDBTableSuffix(wsConfig), "VALUE");
   }

   public static String getRefTableSuffix(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      return getTableSuffix(getDatabaseType(wsConfig), getDBTableSuffix(wsConfig), "REF");
   }

   public static String getItemTableName(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      return JCR_TABLE_PREFIX + getTableSuffix(getDatabaseType(wsConfig), getDBTableSuffix(wsConfig), "ITEM");
   }

   public static String getValueTableName(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      return JCR_TABLE_PREFIX + getTableSuffix(getDatabaseType(wsConfig), getDBTableSuffix(wsConfig), "VALUE");
   }

   public static String getRefTableName(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      return JCR_TABLE_PREFIX + getTableSuffix(getDatabaseType(wsConfig), getDBTableSuffix(wsConfig), "REF");
   }

   public static String getItemTableSuffix(JDBCDataContainerConfig containerConfig)
   {
      return getTableSuffix(containerConfig.dbStructureType, containerConfig.dbTableSuffix, "ITEM");
   }

   public static String getValueTableSuffix(JDBCDataContainerConfig containerConfig)
   {
      return getTableSuffix(containerConfig.dbStructureType, containerConfig.dbTableSuffix, "VALUE");
   }

   public static String getRefTableSuffix(JDBCDataContainerConfig containerConfig)
   {
      return getTableSuffix(containerConfig.dbStructureType, containerConfig.dbTableSuffix, "REF");
   }

   public static String getItemTableName(JDBCDataContainerConfig containerConfig)
   {
      return JCR_TABLE_PREFIX + getTableSuffix(containerConfig.dbStructureType, containerConfig.dbTableSuffix, "ITEM");
   }

   public static String getValueTableName(JDBCDataContainerConfig containerConfig)
   {
      return JCR_TABLE_PREFIX + getTableSuffix(containerConfig.dbStructureType, containerConfig.dbTableSuffix, "VALUE");
   }

   public static String getRefTableName(JDBCDataContainerConfig containerConfig)
   {
      return JCR_TABLE_PREFIX + getTableSuffix(containerConfig.dbStructureType, containerConfig.dbTableSuffix, "REF");
   }

   private static String getTableSuffix(JDBCDataContainerConfig.DatabaseStructureType dbType, String dbTableSuffix,
      String forTable)
   {
      String tableSuffix = "";
      switch (dbType)
      {
         case MULTI :
            tableSuffix = "M" + forTable;
            break;
         case SINGLE :
            tableSuffix = "S" + forTable;
            break;
         case ISOLATED :
            tableSuffix = forTable.substring(0, 1) + dbTableSuffix;
            break;
      }
      return tableSuffix;
   }

   /**
    * Returns SQL script for create objects such as index, primary of foreign key. 
    */
   public static String getObjectScript(String objectName, boolean multiDb, String dialect, WorkspaceEntry wsEntry)
      throws RepositoryConfigurationException, IOException
   {
      String scripts = prepareScripts(wsEntry, dialect);

      String sql = null;
      for (String query : JDBCUtils.splitWithSQLDelimiter(scripts))
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

   /**
    * Returns {@link DatabaseStructureType} based on workspace configuration.
    */
   public static DatabaseStructureType getDatabaseType(WorkspaceEntry wsConfig) throws RepositoryConfigurationException
   {
      try
      {
         if (wsConfig.getContainer().getParameterBoolean("multi-db"))
         {
            return JDBCDataContainerConfig.DatabaseStructureType.MULTI;
         }
         else
         {
            return JDBCDataContainerConfig.DatabaseStructureType.SINGLE;
         }
      }
      catch (Exception e)
      {
         String dbStructureType =
            wsConfig.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE).toUpperCase();
         return JDBCDataContainerConfig.DatabaseStructureType.valueOf(dbStructureType);
      }
   }

   /**
    * Returns value of {@link JDBCWorkspaceDataContainer#DB_TABLENAME_SUFFIX} parameter 
    * from workspace configuration.
    */
   public static String getDBTableSuffix(WorkspaceEntry wsConfig)
   {
      String defaultSuffix = replaceIncorrectChars(wsConfig.getName());

      String suffix =
         wsConfig.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_TABLENAME_SUFFIX, defaultSuffix);
      return suffix;
   }

   /**
    * Tries to fix name of the workspace if it is not corresponding to SQL table name specification.
    */
   private static String replaceIncorrectChars(String workspaceName)
   {
      return workspaceName.replaceAll("[^A-Za-z_0-9]", "").toUpperCase();
   }

   /**
    * Returns database dialects stored in configuration and {@link DialectConstants#DB_DIALECT_AUTO}
    * in other case. Always in upper register.
    */
   public static String getDatabaseDialect(WorkspaceEntry wsConfig)
   {
      String dialect =
         wsConfig.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT, DBConstants.DB_DIALECT_AUTO);

      return dialect.toUpperCase();
   }

   /**
    * Use sequence for order number.
    *
    * @param wsConfig The workspace configuration.
    * @return true if the sequence are enable. False otherwise.
    */
   public static boolean useSequenceForOrderNumber(WorkspaceEntry wsConfig, String dbDialect) throws RepositoryConfigurationException
   {
      try
      {
         if (wsConfig.getContainer().getParameterValue(JDBCWorkspaceDataContainer.USE_SEQUENCE_FOR_ORDER_NUMBER, JDBCWorkspaceDataContainer.USE_SEQUENCE_AUTO).equalsIgnoreCase(JDBCWorkspaceDataContainer.USE_SEQUENCE_AUTO))
         {
            return JDBCWorkspaceDataContainer.useSequenceDefaultValue();
         }
         else
         {
            return wsConfig.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.USE_SEQUENCE_FOR_ORDER_NUMBER);
         }
      }
      catch (RepositoryConfigurationException e)
      {
         return JDBCWorkspaceDataContainer.useSequenceDefaultValue();
      }
   }
}
