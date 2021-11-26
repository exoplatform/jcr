/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.checker;

import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.RepositoryException;


/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AbstractInconsistencyRepair.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public abstract class AbstractInconsistencyRepair implements InconsistencyRepair
{
   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.AbstractInconsistencyRepair");

   private final WorkspaceStorageConnectionFactory connFactory;

   /**
    * AbstractInconsistencyRepair constructor.
    */
   AbstractInconsistencyRepair(WorkspaceStorageConnectionFactory connFactory, JDBCDataContainerConfig containerConfig)
   {
      this.connFactory = connFactory;
   }

   /**
    * {@inheritDoc}
    */
   final public void doRepair(ResultSet resultSet) throws SQLException
   {
      WorkspaceStorageConnection conn = null;
      try
      {
         conn = connFactory.openConnection(false);
         if (!(conn instanceof JDBCStorageConnection))
         {
            throw new SQLException("Connection is instance of " + conn.getClass());
         }

         repairRow((JDBCStorageConnection)conn, resultSet);

         conn.commit();
      }
      catch (RepositoryException e)
      {
         rollback(conn);
         throw new SQLException(e);
      }
      catch (SQLException e)
      {
         rollback(conn);
         throw e;
      }
   }

   /**
    * Do repair current row. 
    */
   abstract void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException;

   /**
    * Rollback data.
    */
   protected void rollback(WorkspaceStorageConnection conn)
   {
      try
      {
         if (conn != null)
         {
            conn.rollback();
         }
      }
      catch (IllegalStateException e)
      {
         LOG.error("Can not rollback connection", e);
      }
      catch (RepositoryException e)
      {
         LOG.error("Can not rollback connection", e);
      }
   }

   /**
    * Returns internal identifier (container name plus identifier) of item placed
    * in {@link ResultSet}. 
    */
   protected String getIdentifier(ResultSet resultSet, String column) throws SQLException
   {
      String containerName = "";
      try
      {
         containerName = resultSet.getString(DBConstants.CONTAINER_NAME);
      }
      catch (SQLException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("Can't get container name: " + e.getMessage());
         }
      }

      return resultSet.getString(column).substring(containerName.length());
   }

   /**
    * Returns {@link QPathEntry} of item placed in {@link ResultSet}.
    */
   protected QPathEntry getQPathEntry(ResultSet resultSet) throws SQLException, IllegalNameException
   {
      return new QPathEntry(InternalQName.parse(resultSet.getString(DBConstants.COLUMN_NAME)),
         resultSet.getInt(DBConstants.COLUMN_INDEX));
   }
}
