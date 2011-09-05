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

      try
      {
         getDBCleaner(jdbcConn, wsEntry).clean();
         jdbcConn.commit();
      }
      catch (SQLException e)
      {
         jdbcConn.rollback();
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
    * Returns database cleaner for manual cleaning.  
    * 
    * @param jdbcConn
    *          database connection which need to use
    * @param wsEntry
    *          workspace configuration
    * @return
    * @throws SQLException
    * @throws RepositoryConfigurationException
    */
   public static DBClean getDBCleaner(Connection jdbcConn, WorkspaceEntry wsEntry) throws SQLException,
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
         cleanScripts.add("delete from JCR_MVALUE");
         cleanScripts.add("delete from JCR_MREF");

         if (cleanWithHelper)
         {
            cleanScripts.add("delete from JCR_MITEM where I_CLASS=2");

            String selectItems = "select ID from JCR_MITEM where I_CLASS=1 and PARENT_ID=?";
            String deleteItems = "delete from JCR_MITEM where I_CLASS=1 and PARENT_ID=?";

            return new DBClean(jdbcConn, cleanScripts, new DBCleanHelper(jdbcConn, selectItems, deleteItems));
         }

         cleanScripts.add("delete from JCR_MITEM where JCR_MITEM.name <> '" + Constants.ROOT_PARENT_NAME + "'");
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

            return new DBClean(jdbcConn, cleanScripts, new DBCleanHelper(jdbcConn, selectItems, deleteItems));
         }

         cleanScripts.add("delete from JCR_SITEM where CONTAINER_NAME='" + containerName + "'");
      }

      if (dialect.equals(DBConstants.DB_DIALECT_PGSQL))
      {
         return new PgSQLDBClean(jdbcConn, cleanScripts);
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_INGRES))
      {
         return new IngresSQLDBClean(jdbcConn, cleanScripts);
      }
      else if (dialect.equals(DBConstants.DB_DIALECT_ORACLE) || dialect.equals(DBConstants.DB_DIALECT_ORACLEOCI))
      {
         return new OracleDBClean(jdbcConn, cleanScripts);
      }
      else
      {
         return new DBClean(jdbcConn, cleanScripts);
      }
   }
}
