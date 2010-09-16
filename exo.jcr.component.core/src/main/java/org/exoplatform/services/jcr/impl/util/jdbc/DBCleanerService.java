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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: DBCleanerService.java 111 2008-11-11 11:11:11Z serg $
 */
public class DBCleanerService
{
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DBCleanerService");

   public static void removeWorkspaceData(WorkspaceEntry wsConfig) throws RepositoryConfigurationException,
      NamingException, RepositoryException, IOException
   {
      JDBCConfiguration wsJDBCConfig = new JDBCConfiguration(wsConfig);

      DataSource ds = (DataSource)new InitialContext().lookup(wsJDBCConfig.getDbSourceName());
      Connection conn = null;
      try
      {
         conn =
            ds != null ? ds.getConnection() : (wsJDBCConfig.getDbUserName() != null ? DriverManager.getConnection(
               wsJDBCConfig.getDbUrl(), wsJDBCConfig.getDbUserName(), wsJDBCConfig.getDbPassword()) : DriverManager
               .getConnection(wsJDBCConfig.getDbUrl()));
      }
      catch (SQLException e)
      {
         String err =
            "Error of JDBC connection open. SQLException: " + e.getMessage() + ", SQLState: " + e.getSQLState()
               + ", VendorError: " + e.getErrorCode();
         throw new RepositoryException(err, e);
      }

      final String sqlPath = "/conf/storage/cleanup/jcr-" + (wsJDBCConfig.isMultiDb() ? "m" : "s") + "jdbc.sql";
      PrivilegedAction<InputStream> action = new PrivilegedAction<InputStream>()
      {
         public InputStream run()
         {
            return this.getClass().getResourceAsStream(sqlPath);
         }
      };
      InputStream is = AccessController.doPrivileged(action);

      DBCleaner cleaner;
      if (wsJDBCConfig.isMultiDb())
      {
         cleaner = new MultiDBCleaner(conn, is);
      }
      else
      {
         cleaner = new SingleDBCleaner(conn, is, wsJDBCConfig.getContainerName());
      }

      try
      {
         cleaner.cleanWorkspace();
      }
      catch (DBCleanerException e)
      {
         throw new RepositoryException(e.getMessage(), e);
      }
   }

   public static void removeRepositoryData(RepositoryEntry repoConfig) throws RepositoryConfigurationException,
      NamingException, RepositoryException, IOException
   {
      for (WorkspaceEntry wsEntry : repoConfig.getWorkspaceEntries())
      {
         removeWorkspaceData(wsEntry);
      }
   }
}
