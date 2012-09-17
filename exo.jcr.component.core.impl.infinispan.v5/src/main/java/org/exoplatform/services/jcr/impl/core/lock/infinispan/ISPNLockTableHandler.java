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
package org.exoplatform.services.jcr.impl.core.lock.infinispan;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.checker.DummyRepair;
import org.exoplatform.services.jcr.impl.checker.InspectionQuery;
import org.exoplatform.services.jcr.impl.core.lock.AbstractLockTableHandler;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Provides means for nodes' IDs extraction in case we use {@link ISPNCacheableLockManagerImpl}
 * as {@link LockManager} based on ISPN Cache instance.
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: ISPNLockTableHandler.java 34360 27.02.2012 12:41:39 dkuleshov $
 *
 */
public class ISPNLockTableHandler extends AbstractLockTableHandler
{

   protected static final Log LOG = ExoLogger
      .getLogger("exo.jcr.component.core.impl.infinispan.v5.ISPNLockTableHandler");

   /**
    * ISPNLockTableHandler constructor.
    */
   public ISPNLockTableHandler(WorkspaceEntry workspaceEntry, DataSource ds)
   {
      super(workspaceEntry, ds);
   }

   /**
    * {@inheritDoc}
    */
   protected InspectionQuery getSelectQuery() throws SQLException
   {
      return new InspectionQuery("SELECT * FROM " + getTableName(), new String[]{getIdColumn()}, "Locks table match",
         new DummyRepair());
   }

   /**
    * {@inheritDoc}
    */
   public InspectionQuery getDeleteQuery(String nodeId) throws SQLException
   {
      return new InspectionQuery("DELETE FROM " + getTableName() + " WHERE " + getIdColumn() + "='" + nodeId + "'",
         new String[]{}, "", new DummyRepair());
   }

   /**
    * Returns the column name which contain node identifier.
    */
   private String getIdColumn() throws SQLException
   {
      try
      {
         return lockManagerEntry.getParameterValue(ISPNCacheableLockManagerImpl.INFINISPAN_JDBC_CL_ID_COLUMN_NAME);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * Returns the name of LOCK table.
    */
   private String getTableName() throws SQLException
   {
      try
      {
         return "\"" + lockManagerEntry.getParameterValue(ISPNCacheableLockManagerImpl.INFINISPAN_JDBC_TABLE_NAME)
            + "_" + "L" + workspaceEntry.getUniqueName().replace("_", "").replace("-", "_") + "\"";
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
      return value;
   }
}