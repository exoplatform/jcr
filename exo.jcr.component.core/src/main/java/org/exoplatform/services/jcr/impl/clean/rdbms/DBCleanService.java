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
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.jdbc.DBInitializerHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
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
    * The constraint name is limited by 18 symbols.  
    */
   private static final int DB2_CONSTRAINT_NAME_LENGTH_LIMIT = 18;

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

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI)
         || dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8)
         || dialect.equals(DBConstants.DB_DIALECT_SYBASE) || dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
      {
         // Sybase doesn't allow DDL scripts inside transaction
         if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
         {
            if (!jdbcConn.getAutoCommit())
            {
               jdbcConn.setAutoCommit(true);
            }
         }
         else
         {
            jdbcConn.setAutoCommit(false);
         }

         ArrayList<String> dbCleanerScripts = new ArrayList<String>();
         dbCleanerScripts.addAll(getRenameScripts(isMultiDB, dialect));
         dbCleanerScripts.addAll(prepareInirializationScript(getInitializationDBScript(isMultiDB, dialect), isMultiDB,
            dialect));
         dbCleanerScripts.addAll(getPreTablesRestoreScript(isMultiDB, dialect));

         ArrayList<String> afterRestoreScript = new ArrayList<String>();
         afterRestoreScript.addAll(getAfterRestoreScript(isMultiDB, dialect));
         afterRestoreScript.addAll(getPostTablesRestoreScript(isMultiDB, dialect));

         return new DBCleaner(jdbcConn, dbCleanerScripts, getRollbackRenamedScript(isMultiDB, dialect),
            afterRestoreScript);
      }

      ArrayList<String> dbCleanerScripts = new ArrayList<String>();
      dbCleanerScripts.addAll(getDropTableScripts(isMultiDB, dialect));
      dbCleanerScripts.addAll(getInitializationDBScript(isMultiDB, dialect));
      dbCleanerScripts.addAll(getPreTablesRestoreScript(isMultiDB, dialect));

      return new DBCleaner(jdbcConn, dbCleanerScripts, new ArrayList<String>(), getPostTablesRestoreScript(isMultiDB,
         dialect));

   }

   /**
    * Prepare database initialization script.
    * 
    * @param initializationDBScript
    *          list with scripts 
    * @param isMultiDB
    *          boolean, is multi-db
    * @param dialect
    *          string, dialect of DB
    * @return List with database initialization scripts
    */
   private static Collection<? extends String> prepareInirializationScript(List<String> initializationDBScript,
      boolean isMultiDB, String dialect)
   {
      if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         String multiDb = isMultiDB ? "M" : "S";

         for (int i = 0; i < initializationDBScript.size(); i++)
         {
            String query = initializationDBScript.get(i);
            if (query.contains("JCR_FK_" + multiDb + "ITEM_PARENT")
               || query.contains("JCR_FK_" + multiDb + "VALUE_PROPERTY"))
            {
               initializationDBScript.remove(i);
               i--;
            }
         }
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
      {
         String multiDb = isMultiDB ? "M" : "S";

         for (int i = 0; i < initializationDBScript.size(); i++)
         {
            String query = initializationDBScript.get(i);
            if (query.contains("JCR_IDX_" + multiDb + "ITEM_PARENT")
               || query.contains("JCR_IDX_" + multiDb + "ITEM_PARENT_NAME")
               || query.contains("JCR_IDX_" + multiDb + "ITEM_PARENT_ID")
               || query.contains("JCR_IDX_" + multiDb + "VALUE_PROPERTY")
               || query.contains("JCR_IDX_" + multiDb + "REF_PROPERTY"))
            {
               initializationDBScript.remove(i);
               i--;
            }
         }
      }

      return initializationDBScript;
   }

   /**
    * Prepare of restore tables. (Drop constraint, etc...)
    * 
    * @param isMultiDb
    *          boolean
    * @param dialect
    *          String, dialect of DB
    */
   private static List<String> getPreTablesRestoreScript(boolean isMultiDB, String dialect)
   {
      ArrayList<String> dropScript = new ArrayList<String>();

      String multiDb = isMultiDB ? "M" : "S";
      String constraintName;
      String constraint;

      if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL))
      {
         return dropScript;
      }

      constraintName = validateConstraintName("JCR_FK_" + multiDb + "ITEM_PARENT", dialect);
      constraint = "CONSTRAINT " + constraintName + " FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
      dropScript.add("ALTER TABLE JCR_" + multiDb + "ITEM " + dropCommand(false, constraintName, dialect));

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         constraintName = validateConstraintName("JCR_PK_" + multiDb + "VALUE", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(ID)";
         dropScript.add("ALTER TABLE JCR_" + multiDb + "VALUE " + dropCommand(true, constraintName, dialect));

         constraintName = validateConstraintName("JCR_FK_" + multiDb + "VALUE_PROPERTY", dialect);
         constraint =
            "CONSTRAINT " + constraintName + " FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
         dropScript.add("ALTER TABLE JCR_" + multiDb + "VALUE " + dropCommand(false, constraintName, dialect));

         constraintName = validateConstraintName("JCR_PK_" + multiDb + "ITEM", dialect);
         dropScript.add("ALTER TABLE JCR_" + multiDb + "ITEM " + dropCommand(true, constraintName, dialect));

         constraintName = validateConstraintName("JCR_PK_" + multiDb + "REF", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)";
         dropScript.add("ALTER TABLE JCR_" + multiDb + "REF " + dropCommand(true, constraintName, dialect));

         constraintName = validateConstraintName("JCR_PK_" + multiDb + "CONTAINER", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(VERSION)";
         dropScript.add("ALTER TABLE JCR_" + multiDb + "CONTAINER " + dropCommand(true, constraintName, dialect));

         dropScript.add("DROP INDEX JCR_IDX_" + multiDb + "ITEM_PARENT_FK");
         dropScript.add("DROP INDEX JCR_IDX_" + multiDb + "ITEM_PARENT");
         dropScript.add("DROP INDEX JCR_IDX_" + multiDb + "ITEM_PARENT_NAME");
         dropScript.add("DROP INDEX JCR_IDX_" + multiDb + "ITEM_PARENT_ID");
         dropScript.add("DROP INDEX JCR_IDX_" + multiDb + "VALUE_PROPERTY");
         dropScript.add("DROP INDEX JCR_IDX_" + multiDb + "REF_PROPERTY");
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
      {
         dropScript.add("ALTER TABLE  JCR_" + multiDb + "VALUE DROP CONSTRAINT JCR_FK_" + multiDb + "VALUE_PROPERTY");
         dropScript.add("ALTER TABLE  JCR_" + multiDb + "ITEM DROP CONSTRAINT JCR_PK_" + multiDb + "ITEM");
         dropScript.add("ALTER TABLE  JCR_" + multiDb + "VALUE DROP CONSTRAINT JCR_PK_" + multiDb + "VALUE");
      }

      return dropScript;
   }

   /**
    * After of restore tables. (Add constraint, etc...)
    * 
    * @param isMultiDb
    *          boolean
    * @param dialect
    *          String, dialect of DB
    */
   private static List<String> getPostTablesRestoreScript(boolean isMultiDB, String dialect)
      throws RepositoryConfigurationException
   {
      ArrayList<String> addScript = new ArrayList<String>();

      String multiDb = isMultiDB ? "M" : "S";

      String constraintName;
      String constraint;

      if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
      {
         addScript.add("ALTER TABLE JCR_" + multiDb + "ITEM ADD CONSTRAINT JCR_PK_" + multiDb
            + "ITEM PRIMARY KEY(ID)");
         addScript.add("ALTER TABLE JCR_" + multiDb + "VALUE ADD CONSTRAINT JCR_PK_" + multiDb
            + "VALUE PRIMARY KEY(ID)");

         constraintName = validateConstraintName("JCR_FK_" + multiDb + "ITEM_PARENT", dialect);
         constraint = "CONSTRAINT " + constraintName + " FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "ITEM ADD " + constraint);

         addScript.add("ALTER TABLE JCR_" + multiDb + "VALUE ADD CONSTRAINT JCR_FK_" + multiDb
            + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)");

         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT", isMultiDB, dialect));
         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT_NAME", isMultiDB,
            dialect));
         addScript
            .add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT_ID", isMultiDB, dialect));
         addScript
            .add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "VALUE_PROPERTY", isMultiDB, dialect));
         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "REF_PROPERTY", isMultiDB, dialect));

         return addScript;
      }

      constraintName = validateConstraintName("JCR_FK_" + multiDb + "ITEM_PARENT", dialect);
      constraint = "CONSTRAINT " + constraintName + " FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
      addScript.add("ALTER TABLE JCR_" + multiDb + "ITEM ADD " + constraint);

      if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL))
      {
         constraintName = validateConstraintName("JCR_FK_" + multiDb + "VALUE_PROPERTY", dialect);
         constraint =
            "CONSTRAINT " + constraintName + " FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "VALUE ADD " + constraint);
      }

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         constraintName = validateConstraintName("JCR_PK_" + multiDb + "VALUE", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(ID)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "VALUE ADD " + constraint);

         constraintName = validateConstraintName("JCR_PK_" + multiDb + "ITEM", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(ID)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "ITEM ADD " + constraint);

         constraintName = validateConstraintName("JCR_FK_" + multiDb + "VALUE_PROPERTY", dialect);
         constraint =
            "CONSTRAINT " + constraintName + " FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + multiDb + "ITEM(ID)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "VALUE ADD " + constraint);


         constraintName = validateConstraintName("JCR_PK_" + multiDb + "REF", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "REF ADD " + constraint);

         constraintName = validateConstraintName("JCR_PK_" + multiDb + "CONTAINER", dialect);
         constraint = "CONSTRAINT " + constraintName + " PRIMARY KEY(VERSION)";
         addScript.add("ALTER TABLE JCR_" + multiDb + "CONTAINER ADD " + constraint);

         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT_FK", isMultiDB, dialect));
         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT", isMultiDB, dialect));
         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT_NAME", isMultiDB, dialect));
         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "ITEM_PARENT_ID", isMultiDB, dialect));
         addScript
            .add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "VALUE_PROPERTY", isMultiDB, dialect));
         addScript.add(DBInitializerHelper.getObjectScript("JCR_IDX_" + multiDb + "REF_PROPERTY", isMultiDB, dialect));
      }

      return addScript;
   }

   /**
    * Validate name of constraint. For some DBs constrains name is limited.
    * 
    * @param string
    *          the constraint name
    * @param dialect
    *          String, dialect of DB
    * @return the constraint name accepted for specific DB
    */
   private static String validateConstraintName(String string, String dialect)
   {
      if (dialect.equals(DBConstants.DB_DIALECT_DB2) || dialect.equals(DBConstants.DB_DIALECT_DB2V8))
      {
         return string.substring(0, DB2_CONSTRAINT_NAME_LENGTH_LIMIT);
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE) && string.equals("JCR_PK_SCONTAINER"))
      {
         return "JCR_PK_MCONTAINER";
      }
      else
      {
         return string;
      }
   }

   /**
    * Return the command to drop primary or foreign key.  
    * 
    * @param isPrimaryKey
    *          boolean
    * @param dialect
    *          String, dialect of DB
    * @return String
    */
   protected static String dropCommand(boolean isPrimaryKey, String constraintName, String dialect)
   {
      if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         return isPrimaryKey == true ? "DROP PRIMARY KEY" : "DROP FOREIGN KEY " + constraintName;
      }
      else
      {
         return "DROP CONSTRAINT " + constraintName;
      }
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

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
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

         renameScripts.add("RENAME JCR_" + isMultiDB + "VALUE_SEQ TO JCR_" + isMultiDB + "VALUE_SEQ"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER TRIGGER BI_JCR_" + isMultiDB + "VALUE RENAME TO BI_JCR_" + isMultiDB + "VALUE"
            + OLD_OBJECT_SUFFIX);

         // JCR_[S,M]ITEM
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM RENAME TO JCR_" + isMultiDB + "ITEM"
            + OLD_OBJECT_SUFFIX);
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
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX
            + " RENAME CONSTRAINT JCR_PK_" + isMultiDB + "CONTAINER TO JCR_PK_" + isMultiDB + "CONTAINER"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "CONTAINER RENAME TO JCR_PK_" + isMultiDB + "CONTAINER"
            + OLD_OBJECT_SUFFIX);

         // JCR_[S,M]REF
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF RENAME TO JCR_" + isMultiDB + "REF"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " RENAME CONSTRAINT JCR_PK_"
            + isMultiDB + "REF TO JCR_PK_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER INDEX JCR_PK_" + isMultiDB + "REF RENAME TO JCR_PK_" + isMultiDB + "REF"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER INDEX JCR_IDX_" + isMultiDB + "REF_PROPERTY RENAME TO JCR_IDX_" + isMultiDB
            + "REF_PROPERTY" + OLD_OBJECT_SUFFIX);
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE RENAME TO JCR_" + isMultiDB + "VALUE"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM RENAME TO JCR_" + isMultiDB + "ITEM"
            + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF RENAME TO JCR_" + isMultiDB + "REF"
            + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER RENAME TO JCR_" + isMultiDB + "CONTAINER"
            + OLD_OBJECT_SUFFIX);
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
      {
         renameScripts.add("sp_rename JCR_" + isMultiDB + "VALUE, JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX);
         renameScripts.add("sp_rename JCR_" + isMultiDB + "ITEM, JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX);
         renameScripts.add("sp_rename JCR_" + isMultiDB + "CONTAINER, JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX);
         renameScripts.add("sp_rename JCR_" + isMultiDB + "REF, JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX);
         renameScripts.add("sp_rename JCR_FK_" + isMultiDB + "VALUE_PROPERTY, JCR_FK_" + isMultiDB + "VALUE_PROPERTY"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("sp_rename JCR_FK_" + isMultiDB + "ITEM_PARENT, JCR_FK_" + isMultiDB + "ITEM_PARENT_OLD");
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
      {
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE RENAME TO JCR_" + isMultiDB + "VALUE"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM RENAME TO JCR_" + isMultiDB + "ITEM"
            + OLD_OBJECT_SUFFIX);
         renameScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER RENAME TO JCR_" + isMultiDB + "CONTAINER"
            + OLD_OBJECT_SUFFIX);
         renameScripts
            .add("ALTER TABLE JCR_" + isMultiDB + "REF RENAME TO JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER TABLE  JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " DROP CONSTRAINT JCR_FK_"
            + isMultiDB + "VALUE_PROPERTY");

         renameScripts.add("ALTER TABLE  JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " DROP CONSTRAINT JCR_FK_"
            + isMultiDB + "ITEM_PARENT");

         renameScripts.add("ALTER TABLE  JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX
            + " DROP CONSTRAINT JCR_PK_" + isMultiDB + "CONTAINER");

         renameScripts.add("ALTER TABLE  JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " DROP CONSTRAINT JCR_PK_"
            + isMultiDB + "ITEM");

         renameScripts.add("ALTER TABLE  JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " DROP CONSTRAINT JCR_PK_"
            + isMultiDB + "VALUE");

         renameScripts.add("ALTER TABLE  JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " DROP CONSTRAINT JCR_PK_"
            + isMultiDB + "REF");

         renameScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "ITEM_PARENT RENAME TO JCR_IDX_" + isMultiDB
            + "ITEM_PARENT" + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "ITEM_PARENT_NAME RENAME TO JCR_IDX_" + isMultiDB
            + "ITEM_PARENT_NAME" + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "ITEM_PARENT_ID RENAME TO JCR_IDX_" + isMultiDB
            + "ITEM_PARENT_ID" + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "VALUE_PROPERTY RENAME TO JCR_IDX_" + isMultiDB
            + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX);

         renameScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "REF_PROPERTY RENAME TO JCR_IDX_" + isMultiDB
            + "REF_PROPERTY" + OLD_OBJECT_SUFFIX);
      }

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
    * @throws RepositoryConfigurationException 
    */
   protected static List<String> getRollbackRenamedScript(boolean multiDb, String dialect)
      throws RepositoryConfigurationException
   {
      final String isMultiDB = (multiDb ? "M" : "S");

      List<String> rollbackScripts = new ArrayList<String>();

      rollbackScripts.addAll(getDropTableScripts(multiDb, dialect));

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
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

         rollbackScripts.add("RENAME JCR_" + isMultiDB + "VALUE_SEQ" + OLD_OBJECT_SUFFIX + " TO JCR_" + isMultiDB
            + "VALUE_SEQ");
         rollbackScripts.add("ALTER TRIGGER BI_JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME TO BI_JCR_"
            + isMultiDB + "VALUE");
   
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
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8))
      {
         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
            + isMultiDB + "ITEM");

         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM ADD CONSTRAINT JCR_FK_" + isMultiDB
            + "ITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + isMultiDB + "ITEM(ID)");

         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
            + isMultiDB + "VALUE");
         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE ADD CONSTRAINT JCR_FK_" + isMultiDB
            + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + isMultiDB + "ITEM(ID)");

         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
            + isMultiDB + "CONTAINER");

         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_" + isMultiDB
            + "REF");
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
      {
         rollbackScripts.add("sp_rename JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + ", JCR_" + isMultiDB
            + "VALUE");
         rollbackScripts.add("sp_rename JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + ", JCR_" + isMultiDB + "ITEM");
         rollbackScripts.add("sp_rename JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX + ", JCR_" + isMultiDB
            + "CONTAINER");
         rollbackScripts.add("sp_rename JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + ", JCR_" + isMultiDB + "REF");

         rollbackScripts.add("sp_rename JCR_FK_" + isMultiDB + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX + ", JCR_FK_"
            + isMultiDB + "VALUE_PROPERTY");

         rollbackScripts.add("sp_rename JCR_FK_" + isMultiDB + "ITEM_PARENT" + OLD_OBJECT_SUFFIX + ", JCR_FK_"
            + isMultiDB + "ITEM_PARENT");
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_HSQLDB))
      {
         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "VALUE" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
            + isMultiDB + "VALUE");
         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "ITEM" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
            + isMultiDB + "ITEM");
         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "CONTAINER" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_"
            + isMultiDB + "CONTAINER");
         rollbackScripts.add("ALTER TABLE JCR_" + isMultiDB + "REF" + OLD_OBJECT_SUFFIX + " RENAME TO JCR_" + isMultiDB
            + "REF");

         rollbackScripts.add("ALTER TABLE  JCR_" + isMultiDB + "VALUE ADD CONSTRAINT JCR_FK_" + isMultiDB
            + "VALUE_PROPERTY FOREIGN KEY(PROPERTY_ID) REFERENCES JCR_" + isMultiDB + "ITEM(ID");

         rollbackScripts.add("ALTER TABLE  JCR_" + isMultiDB + "ITEM ADD CONSTRAINT JCR_FK_" + isMultiDB
            + "ITEM_PARENT FOREIGN KEY(PARENT_ID) REFERENCES JCR_" + isMultiDB + "ITEM(ID)");

         rollbackScripts.add("ALTER TABLE  JCR_" + isMultiDB + "CONTAINER ADD CONSTRAINT JCR_PK_" + isMultiDB
            + "CONTAINER PRIMARY KEY(VERSION)");

         rollbackScripts.add("ALTER TABLE  JCR_" + isMultiDB + "ITEM ADD CONSTRAINT JCR_PK_" + isMultiDB
            + "ITEM PRIMARY KEY(ID)");

         rollbackScripts.add("ALTER TABLE  JCR_" + isMultiDB + "VALUE ADD CONSTRAINT JCR_PK_" + isMultiDB
            + "VALUE PRIMARY KEY(ID)");

         rollbackScripts.add("ALTER TABLE  JCR_" + isMultiDB + "REF ADD CONSTRAINT JCR_PK_" + isMultiDB
            + "REF PRIMARY KEY(NODE_ID, PROPERTY_ID, ORDER_NUM)");

         rollbackScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "ITEM_PARENT" + OLD_OBJECT_SUFFIX
            + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT");

         rollbackScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "ITEM_PARENT_NAME" + OLD_OBJECT_SUFFIX
            + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT_NAME");

         rollbackScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "ITEM_PARENT_ID" + OLD_OBJECT_SUFFIX
            + " RENAME TO JCR_IDX_" + isMultiDB + "ITEM_PARENT_ID");

         rollbackScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "VALUE_PROPERTY" + OLD_OBJECT_SUFFIX
            + " RENAME TO JCR_IDX_" + isMultiDB + "VALUE_PROPERTY");

         rollbackScripts.add("ALTER INDEX  JCR_IDX_" + isMultiDB + "REF_PROPERTY" + OLD_OBJECT_SUFFIX
            + " RENAME TO JCR_IDX_" + isMultiDB + "REF_PROPERTY");
      }

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
      boolean isMultiDB =
         Boolean.parseBoolean(wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB));

      String dialect = DialectDetecter.detect(jdbcConn.getMetaData());

      if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI)
         || dialect.equals(DBConstants.DB_DIALECT_MYSQL) || dialect.equals(DBConstants.DB_DIALECT_MYSQL_UTF8)
         || dialect.equals(DBConstants.DB_DIALECT_SYBASE))
      {
         // Sybase doesn't allow DDL scripts inside transaction
         if (dialect.equals(DBConstants.DB_DIALECT_SYBASE))
         {
            if (!jdbcConn.getAutoCommit())
            {
               jdbcConn.setAutoCommit(true);
            }
         }
         else
         {
            jdbcConn.setAutoCommit(false);
         }

         ArrayList<String> dbCleanerScripts = new ArrayList<String>();
         dbCleanerScripts.addAll(getRenameScripts(isMultiDB, dialect));
         dbCleanerScripts.addAll(prepareInirializationScript(getInitializationDBScript(isMultiDB, dialect), isMultiDB,
            dialect));
         dbCleanerScripts.addAll(getPreTablesRestoreScript(isMultiDB, dialect));

         ArrayList<String> afterRestoreScript = new ArrayList<String>();
         afterRestoreScript.addAll(getAfterRestoreScript(isMultiDB, dialect));
         afterRestoreScript.addAll(getPostTablesRestoreScript(isMultiDB, dialect));

         return new DBCleaner(jdbcConn, dbCleanerScripts, getRollbackRenamedScript(isMultiDB, dialect),
            afterRestoreScript);
      }
      else
      {
         List<String> cleanScripts = new ArrayList<String>();
         
         cleanScripts.addAll(getDropTableScripts(isMultiDB, dialect));
         cleanScripts.addAll(getInitializationDBScript(isMultiDB, dialect));
         cleanScripts.addAll(getPreTablesRestoreScript(isMultiDB, dialect));

         return new DBCleaner(jdbcConn, cleanScripts, new ArrayList<String>(), getPostTablesRestoreScript(
            isMultiDB, dialect));
      }
   }
}
