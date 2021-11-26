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

import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCStorageConnection;
import org.exoplatform.services.jcr.impl.storage.jdbc.db.WorkspaceStorageConnectionFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

import javax.jcr.RepositoryException;

/**
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: EarlierVersionsRemover.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class EarlierVersionsRemover extends AbstractInconsistencyRepair
{

   /**
    * RemoveEarlierVersions constructor.
    */
   public EarlierVersionsRemover(WorkspaceStorageConnectionFactory connFactory, JDBCDataContainerConfig containerConfig)
   {
      super(connFactory, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   protected void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         PropertyData data = (PropertyData)conn.getItemData(getIdentifier(resultSet, DBConstants.COLUMN_ID));
         int maxVersion = conn.getMaxPropertyVersion(data);

         if (resultSet.getInt(DBConstants.COLUMN_VERSION) < maxVersion)
         {
            conn.delete(data, new SimpleChangedSizeHandler());
         }
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
}
