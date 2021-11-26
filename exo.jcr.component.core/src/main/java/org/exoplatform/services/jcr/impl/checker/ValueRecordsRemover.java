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

import org.exoplatform.services.jcr.datamodel.IllegalPathException;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.persistent.SimpleChangedSizeHandler;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
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
 * @version $Id: ValueRecordsRemover.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class ValueRecordsRemover extends AbstractInconsistencyRepair
{

   private final String containerName;

   private final boolean multiDb;

   /**
    * ValueRecordsRemover constructor.
    */
   public ValueRecordsRemover(WorkspaceStorageConnectionFactory connFactory, JDBCDataContainerConfig containerConfig)
   {
      super(connFactory, containerConfig);
      this.containerName = containerConfig.containerName;
      this.multiDb = containerConfig.dbStructureType.isMultiDatabase();
   }

   /**
    * {@inheritDoc}
    */
   void repairRow(JDBCStorageConnection conn, ResultSet resultSet) throws SQLException
   {
      try
      {
         String propertyId = getIdentifier(resultSet, DBConstants.COLUMN_VPROPERTY_ID);
         QPath path = QPath.parse("[]");

         PropertyData data = new TransientPropertyData(path, propertyId, 0, 0, null, false, new ArrayList<ValueData>());

         conn.delete(data, new SimpleChangedSizeHandler());
      }
      catch (IllegalPathException e)
      {
         throw new SQLException(e);
      }
      catch (UnsupportedOperationException e)
      {
         throw new SQLException(e);
      }
      catch (JCRInvalidItemStateException e)
      {
         // this is ok, since record is absent in ITEM table
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
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

   /**
    * {@inheritDoc}
    */
   protected String getIdentifier(ResultSet resultSet, String column) throws SQLException
   {
      return resultSet.getString(column).substring(multiDb ? 0 : containerName.length());
   }
}
