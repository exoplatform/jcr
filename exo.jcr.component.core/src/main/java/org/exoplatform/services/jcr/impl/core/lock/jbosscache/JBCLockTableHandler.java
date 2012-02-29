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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.checker.InspectionQuery;
import org.exoplatform.services.jcr.impl.core.lock.AbstractLockTableHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.SQLException;

/**
 * Provides means for nodes' IDs extraction in case we use {@link CacheableLockManagerImpl}
 * as {@link LockManager} based on non shareable JBoss Cache instance.
 * Equally applicable for single and multi db configurations.
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: JBCLockTableHandler.java 34360 2009-07-22 23:58:59Z dkuleshov $
 */
public class JBCLockTableHandler extends AbstractLockTableHandler
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.JBCLockTableHandler");

   /**
    * JBCLockTableHandler constructor.
    */
   public JBCLockTableHandler(final WorkspaceEntry workspaceEntry)
   {
      super(workspaceEntry);
   }

   /**
    * {@inheritDoc}
    */
   protected InspectionQuery getSelectQuery() throws SQLException
   {
      return new InspectionQuery("SELECT * FROM " + getTableName() + " WHERE " + getParentColumn() + "='/"
         + CacheableLockManagerImpl.LOCKS + "'", new String[]{getIdColumn()}, "Locks table match");
   }

   /**
    * {@inheritDoc}
    */
   protected InspectionQuery getDeleteQuery(String nodeId) throws SQLException
   {
      return new InspectionQuery("DELETE FROM " + getTableName() + " WHERE " + getIdColumn() + "='/"
         + CacheableLockManagerImpl.LOCKS + "/" + nodeId + "'", new String[]{}, "");
   }

   /**
    * Returns the column name which contain node identifier.
    */
   protected String getIdColumn() throws SQLException
   {
      try
      {
         return lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_CL_FQN_COLUMN);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * Returns the name of parent column.
    */
   protected String getParentColumn() throws SQLException
   {
      try
      {
         return lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_CL_PARENT_COLUMN);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * Returns the name of LOCK table.
    */
   protected String getTableName() throws SQLException
   {
      try
      {
         return lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_TABLE_NAME);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected String getDataSourceName() throws SQLException
   {
      try
      {
         return lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_CL_DATASOURCE);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected String extractNodeId(String value)
   {
      return value.substring(value.lastIndexOf("/") + 1);
   }
}
