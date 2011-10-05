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
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 24 01 2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: DBCleanService.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class DBCleanService
{
   /**
    * Logger.
    */
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleanService");

   /**
    * Old object suffix. Will using in rename.
    */
   public static final String OLD_OBJECT_SUFFIX = "_OLD";

   /**
    * Cleans workspace data from database.
    * 
    * @param wsEntry
    *          workspace configuration
    * @throws RepositoryConfigurationException
    * @throws NamingException
    * @throws SQLException
    */
   public static void cleanWorkspaceData(WorkspaceEntry wsEntry) throws RepositoryConfigurationException,
      NamingException, SQLException
   {
      String dsName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      final DataSource ds = (DataSource)new InitialContext().lookup(dsName);
      if (ds == null)
      {
         throw new NameNotFoundException("Data source " + dsName + " not found");
      }

      Connection jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
      {
         public Connection run() throws Exception
         {
            return ds.getConnection();

         }
      });
      jdbcConn.setAutoCommit(false);

      DBCleaner dbCleaner = getWorkspaceDBCleaner(jdbcConn, wsEntry);
      try
      {
         dbCleaner.executeCleanScripts();
         try
         {
            dbCleaner.executeCommitScripts();
         }
         catch (SQLException e)
         {
            LOG.error("Can't remove temporary objects", e);
         }
         jdbcConn.commit();
      }
      catch (SQLException e)
      {
         jdbcConn.rollback();

         dbCleaner.executeRollbackScripts();
         jdbcConn.commit();
      }
      finally
      {
         jdbcConn.close();
      }
   }

   /**
    * Cleans repository data from database.
    * 
    * @param rEntry
    *          the repository configuration
    * @throws RepositoryConfigurationException
    * @throws NamingException
    * @throws SQLException
    */
   public static void cleanRepositoryData(RepositoryEntry rEntry) throws RepositoryConfigurationException,
      NamingException, SQLException
   {
      for (WorkspaceEntry wsEntry : rEntry.getWorkspaceEntries())
      {
         cleanWorkspaceData(wsEntry);
      }
   }

   /**
    * Returns database cleaner for manual cleaning for repository. 
    * 
    * @param jdbcConn
    *          database connection which need to use
    * @param wsEntry
    *          workspace configuration
    * @return database cleaner or null in case of multi-db configuration
    * @throws SQLException
    * @throws RepositoryConfigurationException
    */
   public static DBCleaner getRepositoryDBCleaner(Connection jdbcConn, RepositoryEntry repoEntry) throws SQLException,
      RepositoryConfigurationException
   {
      final boolean isMultiDB =
         Boolean.parseBoolean(repoEntry.getWorkspaceEntries().get(0).getContainer().getParameterValue(
            JDBCWorkspaceDataContainer.MULTIDB));

      if (isMultiDB)
      {
         return null;
      }

      String dialect = DialectDetecter.detect(jdbcConn.getMetaData());

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         ArrayList<String> dbCleanerScripts = new ArrayList<String>();
         dbCleanerScripts.addAll(getRenameScripts(isMultiDB, dialect));
         dbCleanerScripts.addAll(getInitializationDBScript(isMultiDB, dialect));

         return new DBCleaner(jdbcConn, dbCleanerScripts, getRollbackRenamedScript(isMultiDB, dialect),
            getAfterRestoreScript(isMultiDB, dialect));
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
      {
         ArrayList<String> dbCleanerScripts = new ArrayList<String>();
         dbCleanerScripts.add("delete from JCR_" + (isMultiDB ? "M" : "S") + "VALUE");
         dbCleanerScripts.add("delete from JCR_" + (isMultiDB ? "M" : "S") + "ITEM");
         dbCleanerScripts.add("delete from JCR_" + (isMultiDB ? "M" : "S") + "REF");
         dbCleanerScripts.add("delete from JCR_" + (isMultiDB ? "M" : "S") + "CONTAINER");
         dbCleanerScripts.add(DBInitializerHelper.getRootNodeInitializeScript(isMultiDB));

         return new DBCleaner(jdbcConn, dbCleanerScripts);
      }

      ArrayList<String> dbCleanerScripts = new ArrayList<String>();
      dbCleanerScripts.addAll(getDropTableScripts(isMultiDB, dialect));
      dbCleanerScripts.addAll(getInitializationDBScript(isMultiDB, dialect));

      return new DBCleaner(jdbcConn, dbCleanerScripts);

   }

   /**
    * Create list with queries to drop tables, etc...
    * 
    * @param multiDb
    * @return List 
    *           return list with query
    */
   protected static List<String> getDropTableScripts(boolean multiDb, String dialect)
   {
      final String isMultiDB = (multiDb ? "M" : "S");

      List<String> cleanScripts = new ArrayList<String>();

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         cleanScripts.add("drop trigger BI_JCR_" + isMultiDB + "VALUE");
         cleanScripts.add("drop sequence JCR_" + isMultiDB + "VALUE_SEQ");
      }

      cleanScripts.add("drop table JCR_" + isMultiDB + "VALUE");
      cleanScripts.add("drop table JCR_" + isMultiDB + "ITEM");
      cleanScripts.add("drop table JCR_" + isMultiDB + "REF");
      cleanScripts.add("drop table JCR_" + isMultiDB + "CONTAINER");

      return cleanScripts;
   }

   /**
    * Create script to rename tables, indexes, etc...   
    * 
    * @param multiDb
    *          boolean
    * @param dialect
    *          string
    * @return List 
    *           return list with query
    */
   protected static List<String> getRenameScripts(boolean multiDb, String dialect)
   {
      final String isMultiDB = (multiDb ? "M" : "S");

      List<String> renameScripts = new ArrayList<String>();

      // JCR_[S,M]VALUE
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE RENAME TO JCR_" + isMultiDB + "VALUE"
         + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_PK_"
         + isMultiDB + "VALUE TO JCR_PK_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_FK_"
         + isMultiDB + "VALUE_PROPERTY TO JCR_FK_" + isMultiDB + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "VALUE RENAME TO JCR_PK_" + isMultiDB + "VALUE"
         + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "VALUE_PROPERTY RENAME TO JCR_IDX_" + isMultiDB
         + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX);

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         renameScripts.add("RENAME JCR_" + isMultiDB + "VALUE_SEQ TO JCR_" + isMultiDB + "VALUE_SEQ"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER TRIGGER BI_JCR_" + isMultiDB + "VALUE RENAME TO BI_JCR_" + isMultiDB + "VALUE"
            + OLD_OBJECT_SUFFIX);
      }

      // JCR_[S,M]ITEM
      renameScripts
         .add("ALTER TABLE JCR_" + isMultiDB + "ITEM RENAME TO JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_PK_"
         + isMultiDB + "ITEM TO JCR_PK_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_FK_"
         + isMultiDB + "ITEM_PARENT TO JCR_FK_" + isMultiDB + "ITEM_PARENT" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "ITEM RENAME TO JCR_PK_" + isMultiDB + "ITEM"
         + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT_FK RENAME TO JCR_IDX_" + isMultiDB
         + "ITEM_PARENT_FK" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT RENAME TO JCR_IDX_" + isMultiDB
         + "ITEM_PARENT" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT_NAME RENAME TO JCR_IDX_" + isMultiDB
         + "ITEM_PARENT_NAME" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT_ID RENAME TO JCR_IDX_" + isMultiDB
         + "ITEM_PARENT_ID" + OLD_OBJECT_SUFFIX);

      // JCR_[S,M]CONTAINER
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER RENAME TO JCR_" + isMultiDB + "CONTAINER"
         + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_PK_"
         + isMultiDB + "CONTAINER TO JCR_PK_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "CONTAINER RENAME TO JCR_PK_" + isMultiDB + "CONTAINER"
         + OLD_OBJECT_SUFFIX);

      // JCR_[S,M]REF
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF RENAME TO JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_PK_"
         + isMultiDB + "REF TO JCR_PK_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "REF RENAME TO JCR_PK_" + isMultiDB + "REF"
         + OLD_OBJECT_SUFFIX);
      renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "REF_PROPERTY RENAME TO JCR_IDX_" + isMultiDB
         + "REF_PROPERTY" + OLD_OBJECT_SUFFIX);

      return renameScripts;
   }

   /**
    * Create script to rollback changes after rename.
    * 
    * @param multiDb
    *          boolean
    * @param dialect
    *          string
    * @return List 
    *           return list with query
    */
   protected static List<String> getRollbackRenamedScript(boolean multiDb, String dialect)
   {
      final String isMultiDB = (multiDb ? "M" : "S");

      List<String> rollbackScripts = new ArrayList<String>();

      rollbackScripts.addAll(getDropTableScripts(multiDb, dialect));

      // JCR_[S,M]VALUE
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_" + isMultiDB
         + "VALUE");
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE RENAME CONSTRAINT JCR_PK_" + isMultiDB
         + "VALUE" + OLD_OBJECT_SUFFIX + " TO JCR_PK_" + isMultiDB + "VALUE");
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE RENAME CONSTRAINT JCR_FK_" + isMultiDB
         + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX + " TO JCR_FK_" + isMultiDB + "VALUE_PROPERTY");
      rollbackScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_PK_"
         + isMultiDB + "VALUE");
      rollbackScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX
         + " RENAME TO JCR_IDX_" + isMultiDB + "VALUE_PROPERTY");

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         rollbackScripts.add("RENAME JCR_" + isMultiDB + "VALUE_SEQ" + OLD_OBJECT_SUFFIX + " TO JCR_" + isMultiDB
            + "VALUE_SEQ");
         rollbackScripts.add("ALTER TRIGGER BI_JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME TO BI_JCR_"
            + isMultiDB + "VALUE");
      }

      // JCR_[S,M]ITEM
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_" + isMultiDB
         + "ITEM");
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM RENAME CONSTRAINT JCR_PK_" + isMultiDB
         + "ITEM" + OLD_OBJECT_SUFFIX + " TO JCR_PK_" + isMultiDB + "ITEM");
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM RENAME CONSTRAINT JCR_FK_" + isMultiDB
         + "ITEM_PARENT" + OLD_OBJECT_SUFFIX + " TO JCR_FK_" + isMultiDB + "ITEM_PARENT");
      rollbackScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_PK_"
         + isMultiDB + "ITEM");
      rollbackScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT_FK" + OLD_OBJECT_SUFFIX
         + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT_FK");
      rollbackScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT" + OLD_OBJECT_SUFFIX
         + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT");
      rollbackScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT_NAME" + OLD_OBJECT_SUFFIX
         + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT_NAME");
      rollbackScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "ITEM_PARENT_ID" + OLD_OBJECT_SUFFIX
         + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT_ID");

      // JCR_[S,M]CONTAINER
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
         + isMultiDB + "CONTAINER");
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER RENAME CONSTRAINT JCR_PK_" + isMultiDB
         + "CONTAINER" + OLD_OBJECT_SUFFIX + " TO JCR_PK_" + isMultiDB + "CONTAINER");
      rollbackScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_PK_"
         + isMultiDB + "CONTAINER");

      // JCR_[S,M]REF
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_" + isMultiDB
         + "REF");
      rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF RENAME CONSTRAINT JCR_PK_" + isMultiDB
         + "REF" + OLD_OBJECT_SUFFIX + " TO JCR_PK_" + isMultiDB + "REF");
      rollbackScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_PK_"
         + isMultiDB + "REF");
      rollbackScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "REF_PROPERTY" + OLD_OBJECT_SUFFIX
         + " RENAME TO JCR_IDX_" + isMultiDB + "REF_PROPERTY");

      return rollbackScripts;
   }

   /**
    * Create script to drop old tables, indexes, etc...after successful restore DB. 
    * 
    * @param multiDb
    *          boolean
    * @param dialect
    *         string  
    * @return List 
    *           return list with query
    */
   protected static List<String> getAfterRestoreScript(boolean multiDb, String dialect)
   {
      List<String> afterRetoreScripts = new ArrayList<String>();

      for (String query : getDropTableScripts(multiDb, dialect))
      {
         afterRetoreScripts.add(query + OLD_OBJECT_SUFFIX);
      }

      return afterRetoreScripts;
   }

   /**
    * Create list with queries to initialization database.
    * 
    * @param multiDb
    * @param dialect
    * @return
    * @throws RepositoryConfigurationException
    */
   protected static List<String> getInitializationDBScript(boolean multiDb, String dialect)
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

      List<String> scripts = new ArrayList<String>();

      for (String query : DBInitializerHelper.scripts(script))
      {
         scripts.add(DBInitializerHelper.cleanWhitespaces(query));
      }

      String rootParent_container = DBInitializerHelper.getRootNodeInitializeScript(multiDb);

      scripts.add(rootParent_container);

      return scripts;
   }

   /**
    * Returns database cleaner for manual cleaning for workspace.  
    * 
    * @param jdbcConn
    *          database connection which need to use
    * @param wsEntry
    *          workspace configuration
    * @return
    * @throws SQLException
    * @throws RepositoryConfigurationException
    */
   public static DBCleaner getWorkspaceDBCleaner(Connection jdbcConn, WorkspaceEntry wsEntry) throws SQLException,
      RepositoryConfigurationException
   {
      boolean multiDb =
         Boolean.parseBoolean(wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB));

      String containerName = wsEntry.getName();
      String dialect = DialectDetecter.detect(jdbcConn.getMetaData());

      boolean cleanWithHelper = false;
      if (dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
      {
         cleanWithHelper = true;
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         cleanWithHelper = true;

         Statement st = jdbcConn.createStatement();
         st.execute("SELECT ENGINE FROM information_schema.TABLES where TABLE_SCHEMA='" + jdbcConn.getCatalog()
            + "' and (TABLE_NAME='JCR_SITEM' or TABLE_NAME='JCR_MITEM')");
         ResultSet result = st.getResultSet();
         if (result.next())
         {
            String engine = result.getString("ENGINE");
            if (engine.equalsIgnoreCase("MyISAM"))
            {
               cleanWithHelper = false;
            }
         }
      }

      List<String> cleanScripts = new ArrayList<String>();

      if (multiDb)
      {
         if (dialect.equals(DBConstants.DB_DIALECT_SYBASE) || dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
         {
            cleanScripts.add("delete from JCR_MVALUE");
            cleanScripts.add("delete from JCR_MREF");

            if (cleanWithHelper)
            {
               cleanScripts.add("delete from JCR_MITEM where I_CLASS=2");

               String selectItems = "select ID from JCR_MITEM where I_CLASS=1 and PARENT_ID=?";
               String deleteItems = "delete from JCR_MITEM where I_CLASS=1 and PARENT_ID=?";

               return new DBCleaner(jdbcConn, cleanScripts, new RecursiveDBCleanHelper(jdbcConn, selectItems,
                  deleteItems));
            }

            cleanScripts.add("delete from JCR_MITEM where JCR_MITEM.name <> '" + Constants.ROOT_PARENT_NAME + "'");
         }
         else
         {
            cleanScripts.addAll(getDropTableScripts(multiDb, dialect));
            cleanScripts.addAll(getInitializationDBScript(multiDb, dialect));
         }
      }
      else
      {
         cleanScripts
            .add("delete from JCR_SVALUE where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SVALUE.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME='"
               + containerName + "')");
         cleanScripts
            .add("delete from JCR_SREF where exists(select * from JCR_SITEM where JCR_SITEM.ID=JCR_SREF.PROPERTY_ID and JCR_SITEM.CONTAINER_NAME='"
               + containerName + "')");

         if (cleanWithHelper)
         {
            cleanScripts.add("delete from JCR_SITEM where I_CLASS=2 and CONTAINER_NAME='" + containerName + "'");

            String selectItems =
               "select ID from JCR_SITEM where I_CLASS=1 and CONTAINER_NAME='" + containerName + "' and PARENT_ID=?";
            String deleteItems =
               "delete from JCR_SITEM where I_CLASS=1 and CONTAINER_NAME='" + containerName + "' and PARENT_ID=?";

            return new DBCleaner(jdbcConn, cleanScripts, new RecursiveDBCleanHelper(jdbcConn, selectItems, deleteItems));
         }

         cleanScripts.add("delete from JCR_SITEM where CONTAINER_NAME='" + containerName + "'");
      }

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         ArrayList<String> dbCleanerScripts = new ArrayList<String>();
         dbCleanerScripts.addAll(getRenameScripts(multiDb, dialect));
         dbCleanerScripts.addAll(getInitializationDBScript(multiDb, dialect));

         return new DBCleaner(jdbcConn, dbCleanerScripts, getRollbackRenamedScript(multiDb, dialect),
            getAfterRestoreScript(multiDb, dialect));
      }
      else
      {
         return new DBCleaner(jdbcConn, cleanScripts);
      }
   }
}
