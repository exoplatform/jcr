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
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.QPathEntry;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import javax.jcr.InvalidItemStateException;
import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: AssignRootAsParentRepair.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class RootAsParentAssigner extends AbstractInconsistencyRepair
{

   public RootAsParentAssigner(WorkspaceStorageConnectionFactory connFactory, JDBCDataContainerConfig containerConfig)
   {
      super(connFactory, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   protected void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      if (resultSet.getInt(DBConstants.COLUMN_CLASS) == 1)
      {
         assignRootAsParent(conn, resultSet);
      }
      else
      {
         deleteProperty(conn, resultSet);
      }
   }

   private void deleteProperty(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         String propertyId = getIdentifier(resultSet, DBConstants.COLUMN_ID);
         QPath path = QPath.parse(resultSet.getString(DBConstants.COLUMN_NAME));

         PropertyData data = new TransientPropertyData(path, propertyId, 0, 0, null, false, new ArrayList<ValueData>());

         conn.delete(data, new SimpleChangedSizeHandler());
      }
      catch (UnsupportedOperationException e)
      {
         throw new SQLException(e);
      }
      catch (InvalidItemStateException e)
      {
         throw new SQLException(e);
      }
      catch (IllegalStateException e)
      {
         throw new SQLException(e);
      }
      catch (RepositoryException e)
      {
         throw new SQLException(e);
      }
   }

   private void assignRootAsParent(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         String nodeId = getIdentifier(resultSet, DBConstants.COLUMN_ID);
         int orderNum = resultSet.getInt(DBConstants.COLUMN_NORDERNUM);
         int version = resultSet.getInt(DBConstants.COLUMN_VERSION);
         QPath path = new QPath(new QPathEntry[]{getQPathEntry(resultSet)});

         NodeData data = new TransientNodeData(path, nodeId, version, null, null, orderNum, Constants.ROOT_UUID, null);

         conn.rename(data);
      }
      catch (IllegalStateException e)
      {
         throw new SQLException(e);
      }
      catch (RepositoryException e)
      {
         throw new SQLException(e);
      }
      catch (IllegalNameException e)
      {
         throw new SQLException(e);
      }
   }
}
