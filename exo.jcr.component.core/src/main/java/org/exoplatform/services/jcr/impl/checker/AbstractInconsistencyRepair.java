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
package org.exoplatform.services.jcr.impl.checker;

import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
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

   protected final WorkspaceStorageConnectionFactory connFactory;

   AbstractInconsistencyRepair(WorkspaceStorageConnectionFactory connFactory)
   {
      this.connFactory = connFactory;
   }

   /**
    * {@inheritDoc}
    */
   public void doRepair(ResultSet resultSet) throws SQLException
   {
      WorkspaceStorageConnection conn = null;
      try
      {
         conn = connFactory.openConnection();
         if (!(conn instanceof JDBCStorageConnection))
         {
            throw new SQLException("Connection is instance of " + conn);
         }

         repairInternally((JDBCStorageConnection)conn, resultSet);

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

   abstract void repairInternally(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException;

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

   protected String exctractId(ResultSet resultSet) throws SQLException
   {
      String containerName = "";
      try
      {
         containerName = resultSet.getString(DBConstants.CONTAINER_NAME);
      }
      catch (SQLException e)
      {
      }

      return resultSet.getString(DBConstants.COLUMN_ID).substring(containerName.length());
   }
}
