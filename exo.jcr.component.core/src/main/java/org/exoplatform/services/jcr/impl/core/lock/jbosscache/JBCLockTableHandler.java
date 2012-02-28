/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.lock.LockTableHandler;
import org.exoplatform.services.jcr.impl.storage.jdbc.InspectionQuery;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * Provides means for nodes' IDs extraction in case we use {@link CacheableLockManagerImpl}
 * as {@link LockManager} based on non shareable JBoss Cache instance.
 * Equally applicable for single and multi db configurations.
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: JBCLockTableHandler.java 34360 2009-07-22 23:58:59Z dkuleshov $
 */
public class JBCLockTableHandler implements LockTableHandler
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JBCLockTableHandler");

   protected final WorkspaceEntry workspaceEntry;

   protected final LockManagerEntry lockManagerEntry;

   /**
    * JBCLockTableHandler constructor.
    */
   public JBCLockTableHandler(final WorkspaceEntry workspaceEntry)
   {
      this.workspaceEntry = workspaceEntry;
      this.lockManagerEntry = workspaceEntry.getLockManager();
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getLockedNodesIds() throws NamingException, RepositoryConfigurationException, SQLException
   {
      String fqnColumn = lockManagerEntry.getParameterValue("jbosscache-cl-cache.jdbc.fqn.column");

      Set<String> lockedNodesIds = new HashSet<String>();

      ResultSet resultSet = null;
      PreparedStatement preparedStatement = null;
      
      Connection jdbcConnection = openConnection();
      try
      {
         InspectionQuery inspectionQuery = getQuery();

         preparedStatement = inspectionQuery.prepareStatement(jdbcConnection);
         resultSet = preparedStatement.executeQuery();

         while (resultSet.next())
         {
            String fqn = resultSet.getString(fqnColumn);
            
            lockedNodesIds.add(fqn.substring(fqn.lastIndexOf("/") + 1));
         }
      }
      finally
      {
         if (resultSet != null)
         {
            try
            {
               resultSet.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
            }
         }
         if (preparedStatement != null)
         {
            try
            {
               preparedStatement.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
            }
         }
         if (jdbcConnection != null)
         {
            try
            {
               jdbcConnection.close();
            }
            catch (SQLException e)
            {
               LOG.error(e.getMessage(), e);
            }
         }
      }

      return lockedNodesIds;
   }

   protected InspectionQuery getQuery() throws RepositoryConfigurationException
   {
      return new InspectionQuery("SELECT * FROM "
         + lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_TABLE_NAME) + " WHERE PARENT='/"
         + CacheableLockManagerImpl.LOCKS + "'", new String[]{}, "Locks table match");
   }

   private Connection openConnection() throws NamingException, RepositoryConfigurationException, SQLException
   {
      final DataSource ds =
         (DataSource)new InitialContext().lookup(lockManagerEntry
            .getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_CL_DATASOURCE));

      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
      {
         public Connection run() throws SQLException
         {
            return ds.getConnection();
         }
      });
   }
}