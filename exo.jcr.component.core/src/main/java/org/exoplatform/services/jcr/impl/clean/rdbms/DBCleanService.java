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
import org.exoplatform.services.database.utils.DialectConstants;
import org.exoplatform.services.database.utils.DialectDetecter;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.clean.rdbms.scripts.DBCleaningScripts;
import org.exoplatform.services.jcr.impl.clean.rdbms.scripts.DBCleaningScriptsFactory;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.InitialContext;
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
    * Cleans workspace data from database.
    * 
    * @param wsEntry
    *          workspace configuration
    * @throws DBCleanException         
    */
   public static void cleanWorkspaceData(WorkspaceEntry wsEntry) throws DBCleanException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      Connection jdbcConn = getConnection(wsEntry);
      boolean autoCommit = DialectConstants.DB_DIALECT_SYBASE.equalsIgnoreCase(resolveDialect(wsEntry));

      try
      {
         jdbcConn.setAutoCommit(autoCommit);

         DBCleanerTool dbCleaner = getWorkspaceDBCleaner(jdbcConn, wsEntry);
         doClean(dbCleaner);
      }
      catch (SQLException e)
      {
         throw new DBCleanException(e);
      }
      finally
      {
         try
         {
            jdbcConn.close();
         }
         catch (SQLException e)
         {
            LOG.error("Can not close connection", e);
         }
      }
   }

   /**
    * Cleans repository data from database.
    * 
    * @param rEntry
    *          the repository configuration
    * @throws DBCleanException          
    */
   public static void cleanRepositoryData(RepositoryEntry rEntry) throws DBCleanException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      WorkspaceEntry wsEntry = rEntry.getWorkspaceEntries().get(0);

      boolean multiDB = getMultiDbParameter(wsEntry);
      if (multiDB)
      {
         for (WorkspaceEntry entry : rEntry.getWorkspaceEntries())
         {
            cleanWorkspaceData(entry);
         }
      }
      else
      {
         Connection jdbcConn = getConnection(wsEntry);
         boolean autoCommit = DialectConstants.DB_DIALECT_SYBASE.equalsIgnoreCase(resolveDialect(wsEntry));

         try
         {
            jdbcConn.setAutoCommit(autoCommit);

            DBCleanerTool dbCleaner = getRepositoryDBCleaner(jdbcConn, rEntry);
            doClean(dbCleaner);
         }
         catch (SQLException e)
         {
            throw new DBCleanException(e);
         }
         finally
         {
            try
            {
               jdbcConn.close();
            }
            catch (SQLException e)
            {
               LOG.error("Can not close connection", e);
            }
         }
      }
   }

   /**
    * Returns database cleaner for repository. 
    * 
    * @param jdbcConn
    *          database connection which need to use
    * @param rEntry
    *          repository configuration
    * @return DBCleanerTool
    * @throws DBCleanException
    */
   public static DBCleanerTool getRepositoryDBCleaner(Connection jdbcConn, RepositoryEntry rEntry)
      throws DBCleanException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      WorkspaceEntry wsEntry = rEntry.getWorkspaceEntries().get(0);

      boolean multiDb = getMultiDbParameter(wsEntry);
      if (multiDb)
      {
         throw new DBCleanException(
            "It is not possible to create cleaner with common connection for multi database repository configuration");
      }

      String dialect = resolveDialect(wsEntry);
      boolean autoCommit = dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_SYBASE);

      DBCleaningScripts scripts = DBCleaningScriptsFactory.prepareScripts(dialect, rEntry);

      return new DBCleanerTool(jdbcConn, autoCommit, scripts.getCleaningScripts(), scripts.getCommittingScripts(),
         scripts.getRollbackingScripts());
   }

   /**
    * Returns database cleaner for workspace.  
    * 
    * @param jdbcConn
    *          database connection which need to use
    * @param wsEntry
    *          workspace configuration
    * @return DBCleanerTool
    * @throws DBCleanException
    */
   public static DBCleanerTool getWorkspaceDBCleaner(Connection jdbcConn, WorkspaceEntry wsEntry) throws DBCleanException
   {
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      boolean multiDb = getMultiDbParameter(wsEntry);

      String dialect = resolveDialect(wsEntry);
      boolean autoCommit = dialect.equalsIgnoreCase(DialectConstants.DB_DIALECT_SYBASE);
      
      DBCleaningScripts scripts = DBCleaningScriptsFactory.prepareScripts(dialect, wsEntry);

      return new DBCleanerTool(jdbcConn, autoCommit, scripts.getCleaningScripts(), scripts.getCommittingScripts(),
         scripts.getRollbackingScripts());
   }

   /**
    * Cleaning.
    * 
    * @throws SQLException 
    */
   private static void doClean(DBCleanerTool dbCleaner) throws DBCleanException, SQLException
   {
      Connection jdbcConn = dbCleaner.getConnection();
      try
      {
         dbCleaner.clean();
         dbCleaner.commit();

         jdbcConn.commit();
      }
      catch (SQLException e)
      {
         jdbcConn.rollback();

         dbCleaner.rollback();

         jdbcConn.commit();
      }
   }

   /**
    * Opens connection to database underlying a workspace.
    */
   private static Connection getConnection(WorkspaceEntry wsEntry) throws DBCleanException
   {
      String dsName = getSourceNameParameter(wsEntry);

      DataSource ds;
      try
      {
         ds = (DataSource)new InitialContext().lookup(dsName);
      }
      catch (NamingException e)
      {
         throw new DBCleanException(e);
      }

      if (ds == null)
      {
         throw new DBCleanException("Data source " + dsName + " not found");
      }

      final DataSource dsF = ds;

      Connection jdbcConn;
      try
      {
         jdbcConn = SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
         {
            public Connection run() throws Exception
            {
               return dsF.getConnection();
            }
         });
      }
      catch (SQLException e)
      {
         throw new DBCleanException(e);
      }

      return jdbcConn;
   }

   /**
    * Return {@link JDBCWorkspaceDataContainer#SOURCE_NAME} parameter from workspace configuration.
    */
   private static String getSourceNameParameter(WorkspaceEntry wsEntry) throws DBCleanException
   {
      try
      {
         return wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }
   }

   /**
    * Return {@link JDBCWorkspaceDataContainer#MULTIDB} parameter from workspace configuration.
    */
   private static boolean getMultiDbParameter(WorkspaceEntry wsEntry) throws DBCleanException
   {
      try
      {
         return Boolean.parseBoolean(wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB));
      }
      catch (RepositoryConfigurationException e)
      {
         throw new DBCleanException(e);
      }
   }

   /**
    * Resolves dialect which it is used in workspace configuration. First of all, 
    * method will try to get parameter {@link JDBCWorkspaceDataContainer#DB_DIALECT} from 
    * a configuration. And only then method will try to detect dialect using {@link DialectDetecter} in case 
    * if dialect is set as {@link DialectConstants#DB_DIALECT_AUTO}.  
    * 
    * @param wsEntry
    *          workspace configuration
    * @return dialect
    * @throws DBCleanException
    */
   private static String resolveDialect(WorkspaceEntry wsEntry) throws DBCleanException
   {
      String dialect =
         wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT, DBConstants.DB_DIALECT_AUTO);

      if (DBConstants.DB_DIALECT_AUTO.equalsIgnoreCase(dialect))
      {
         try
         {
            Connection jdbcConn = getConnection(wsEntry);
            dialect = DialectDetecter.detect(jdbcConn.getMetaData());
         }
         catch (SQLException e)
         {
            throw new DBCleanException(e);
         }
      }

      return dialect;
   }
}
