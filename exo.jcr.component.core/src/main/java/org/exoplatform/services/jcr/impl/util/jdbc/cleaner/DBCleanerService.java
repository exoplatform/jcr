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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * DBCleanerService deliver tools for clean workspace or repository data from database.
 * 
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id$
 */
public class DBCleanerService
{
   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleanerService");

   /**
    * Clean workspace data from database. 
    * Tables will be removed in case of multiDB, and only records will be removed in case of singleDB.
    * 
    * @param wsEntry 
    *          workspace configuration
    * @throws DBCleanerException 
    *          if any exception is occurred
    */
   public void cleanWorkspaceData(WorkspaceEntry wsEntry) throws DBCleanerException
   {
      String sourceName;
      try
      {
         sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);
         if (sourceName == null)
         {
            throw new RepositoryConfigurationException("Parameter " + JDBCWorkspaceDataContainer.SOURCE_NAME
               + " not found in workspace configuration " + wsEntry.getName());
         }
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanerException("Can't define " + JDBCWorkspaceDataContainer.SOURCE_NAME + " parameter", e);
      }

      boolean isMultiDb;
      try
      {
         String multiDb = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB);
         if (multiDb == null)
         {
            throw new RepositoryConfigurationException("Parameter " + JDBCWorkspaceDataContainer.MULTIDB
               + " not found in workspace configuration " + wsEntry.getName());
         }

         isMultiDb = Boolean.parseBoolean(multiDb);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanerException("Can't define " + JDBCWorkspaceDataContainer.MULTIDB + " parameter", e);
      }

      DataSource ds;
      try
      {
         ds = (DataSource)new InitialContext().lookup(sourceName);
      }
      catch (NamingException e)
      {
         throw new DBCleanerException("Can't define data source " + sourceName, e);
      }

      Connection conn = null;
      try
      {
         conn = ds.getConnection();
      }
      catch (SQLException e)
      {
         throw new DBCleanerException("Can't open JDBC connection", e);
      }

      String dbDialect;
      try
      {
         dbDialect = DialectDetecter.detect(conn.getMetaData());
      }
      catch (SQLException e)
      {
         throw new DBCleanerException("Can't define DB dialect", e);
      }

      DBCleaner dbCleaner;
      if (isMultiDb)
      {
         if (dbDialect == DBConstants.DB_DIALECT_ORACLEOCI || dbDialect == DBConstants.DB_DIALECT_ORACLE)
         {
            dbCleaner = new OracleMultiDBCleaner(wsEntry.getName(), conn);
         }
         else if (dbDialect == DBConstants.DB_DIALECT_PGSQL)
         {
            dbCleaner = new PgSQLMultiDBCleaner(wsEntry.getName(), conn);
         }
         else if (dbDialect == DBConstants.DB_DIALECT_INGRES)
         {
            dbCleaner = new IngresSQLMultiDBCleaner(wsEntry.getName(), conn);
         }
         else
         {
            dbCleaner = new MultiDBCleaner(wsEntry.getName(), conn);
         }
      }
      else
      {
         if (dbDialect == DBConstants.DB_DIALECT_ORACLEOCI || dbDialect == DBConstants.DB_DIALECT_ORACLE)
         {
            dbCleaner = new OracleSingleDBCleaner(wsEntry.getName(), conn);
         }
         else if (dbDialect == DBConstants.DB_DIALECT_PGSQL)
         {
            dbCleaner = new PgSQLSingleDBCleaner(wsEntry.getName(), conn);
         }
         else if (dbDialect == DBConstants.DB_DIALECT_INGRES)
         {
            dbCleaner = new IngresSQLSingleDBCleaner(wsEntry.getName(), conn);
         }
         else if (dbDialect == DBConstants.DB_DIALECT_HSQLDB)
         {
            dbCleaner = new HSQLSingleDBCleaner(wsEntry.getName(), conn);
         }
         else if (dbDialect == DBConstants.DB_DIALECT_MYSQL || dbDialect == DBConstants.DB_DIALECT_MYSQL_UTF8)
         {
            dbCleaner = new MySQLSingleDBCleaner(wsEntry.getName(), conn);
         }
         else
         {
            dbCleaner = new SingleDBCleaner(wsEntry.getName(), conn);
         }
      }

      // clean data
      dbCleaner.clean();
   }

   /**
    * Cleanup repository data from database.
    * 
    * @param repoEntry 
    *          repository configuration
    * @throws DBCleanerException 
    *          if any exception is occurred
    */
   public void cleanRepositoryData(RepositoryEntry repoEntry) throws DBCleanerException
   {
      for (WorkspaceEntry wsEntry : repoEntry.getWorkspaceEntries())
      {
         cleanWorkspaceData(wsEntry);
      }
   }
}
