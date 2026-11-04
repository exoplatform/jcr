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

import org.exoplatform.services.database.utils.DialectDetecter;
import org.exoplatform.services.database.utils.JDBCUtils;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.checker.DummyRepair;
import org.exoplatform.services.jcr.impl.checker.InspectionQuery;
import org.exoplatform.services.jcr.impl.core.lock.AbstractLockTableHandler;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.persistence.jdbc.configuration.TableManipulationConfiguration;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Provides means for nodes' IDs extraction in case we use {@link ISPNCacheableLockManagerImpl}
 * as {@link org.infinispan.util.concurrent.locks.LockManager} based on ISPN Cache instance.
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: ISPNLockTableHandler.java 34360 27.02.2012 12:41:39 dkuleshov $
 *
 */
public class ISPNLockTableHandler extends AbstractLockTableHandler
{

   protected static final Log LOG = ExoLogger
      .getLogger("exo.jcr.component.core.ISPNLockTableHandler");

   /**
    * The table of the JDBC store of the locks, <code>null</code> when the names come from the lock manager parameters
    */
   private final TableManipulationConfiguration table;

   /**
    * ISPNLockTableHandler constructor, which reads the names of the table and of its id column in the lock manager
    * parameters.
    */
   public ISPNLockTableHandler(WorkspaceEntry workspaceEntry, DataSource ds)
   {
      this(workspaceEntry, ds, null);
   }

   /**
    * @param workspaceEntry the workspace
    * @param ds the data source of the lock table
    * @param table the table of the JDBC store of the locks, whose configuration names the table and its id column
    * even when a template gives them a default the lock manager parameters do not hold; <code>null</code> to read them
    * in the lock manager parameters
    */
   public ISPNLockTableHandler(WorkspaceEntry workspaceEntry, DataSource ds, TableManipulationConfiguration table)
   {
      super(workspaceEntry, ds);
      this.table = table;
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

   @Override
   protected InspectionQuery getDeleteAllQuery() throws SQLException
   {
      return new InspectionQuery("DELETE FROM " + getTableName(), new String[]{}, "", new DummyRepair());
   }

   /**
    * Returns the column name which contain node identifier.
    */
   private String getIdColumn() throws SQLException
   {
      if (table != null)
      {
         return table.idColumnName();
      }
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
   protected String getTableName() throws SQLException
   {
      return getTableName("L");
   }

   /**
    * Returns the name of a table of the workspace, quoted: the prefix of the lock tables, then the type of the table
    * and the workspace.
    */
   protected String getTableName(String type) throws SQLException
   {
      try
      {
         String dialect = getDialect();
         String quote = "\"";
         if (dialect.startsWith(DBConstants.DB_DIALECT_MYSQL))
            quote = "`";
         String prefix = table == null
            ? lockManagerEntry.getParameterValue(ISPNCacheableLockManagerImpl.INFINISPAN_JDBC_TABLE_NAME)
            : table.tableNamePrefix();
         return quote + prefix + "_" + type + workspaceEntry.getUniqueName().replace("_", "").replace("-", "_") + quote;
      }
      catch (RepositoryConfigurationException e)
      {
         throw new SQLException(e);
      }
   }

   /**
    * Check store table existing
    */
   protected boolean tableExists()
   {
      try (Connection connection = openConnection())
      {
         return JDBCUtils.tableExists(getTableName(), connection);
      }
      catch (SQLException e)
      {
         return false;
      }
   }

   private String getDialect() throws SQLException
   {
      Connection jdbcConn = null;
      try
      {
         jdbcConn = openConnection();
         return DialectDetecter.detect(jdbcConn.getMetaData());
      }
      finally
      {
         if (jdbcConn != null)
         {
            jdbcConn.close();
         }
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