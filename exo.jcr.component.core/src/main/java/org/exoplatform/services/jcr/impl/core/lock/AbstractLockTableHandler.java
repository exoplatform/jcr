/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.checker.InspectionQuery;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCUtils;

import java.security.PrivilegedExceptionAction;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import javax.sql.DataSource;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AbstractLockTableHandler.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class AbstractLockTableHandler implements LockTableHandler
{

   protected final WorkspaceEntry workspaceEntry;

   protected final LockManagerEntry lockManagerEntry;

   /**
    * The data soure.
    */
   final DataSource ds;

   /**
    * AbstractLockTableHandler constructor.
    */
   public AbstractLockTableHandler(WorkspaceEntry workspaceEntry, DataSource ds)
   {
      this.workspaceEntry = workspaceEntry;
      this.lockManagerEntry = workspaceEntry.getLockManager();
      this.ds = ds;
   }

   /**
    * {@inheritDoc}
    */
   public Set<String> getLockedNodesIds() throws SQLException
   {
      Set<String> lockedNodesIds = new HashSet<String>();

      ResultSet resultSet = null;
      PreparedStatement preparedStatement = null;

      Connection jdbcConnection = openConnection();
      try
      {
         InspectionQuery query = getSelectQuery();

         preparedStatement = query.prepareStatement(jdbcConnection);
         resultSet = preparedStatement.executeQuery();

         while (resultSet.next())
         {
            String idColumn = query.getFieldNames()[0];
            String idValue = resultSet.getString(idColumn);

            lockedNodesIds.add(extractNodeId(idValue));
         }
      }
      finally
      {
         JDBCUtils.freeResources(resultSet, preparedStatement, jdbcConnection);
      }

      return lockedNodesIds;
   }

   /**
    * {@inheritDoc}
    */
   public void removeLockedNode(String nodeId) throws SQLException
   {
      ResultSet resultSet = null;
      PreparedStatement preparedStatement = null;

      Connection jdbcConnection = openConnection();
      try
      {
         InspectionQuery query = getDeleteQuery(nodeId);

         preparedStatement = query.prepareStatement(jdbcConnection);
         preparedStatement.executeUpdate();
      }
      finally
      {
         JDBCUtils.freeResources(resultSet, preparedStatement, jdbcConnection);
      }
   }

   /**
    * Opens connection to database.
    */
   protected Connection openConnection() throws SQLException
   {
      return SecurityHelper.doPrivilegedSQLExceptionAction(new PrivilegedExceptionAction<Connection>()
      {
         public Connection run() throws SQLException
         {
            return ds.getConnection();
         }
      });
   }

   /**
    * Returns node identifier from ID column's value {@link DataSource}.
    */
   protected abstract String extractNodeId(String value);

   /**
    * Returns {@link InspectionQuery} for removing row from LOCK table.
    */
   protected abstract InspectionQuery getDeleteQuery(String nodeId) throws SQLException;

   /**
    * Returns {@link InspectionQuery} for selecting all rows from LOCK table.
    */
   protected abstract InspectionQuery getSelectQuery() throws SQLException;

}
