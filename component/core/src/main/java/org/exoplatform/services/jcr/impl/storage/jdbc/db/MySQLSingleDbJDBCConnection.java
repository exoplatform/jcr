/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePluginProvider;

/**
 * Created by The eXo Platform SAS
 * 
 * 20.03.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: MySQLSingleDbJDBCConnection.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class MySQLSingleDbJDBCConnection
   extends SingleDbJDBCConnection
{

   /**
    * MySQL Singledatabase JDBC Connection constructor.
    * 
    * @param dbConnection
    *          JDBC connection, shoudl be opened before
    * @param containerName
    *          Workspace Storage Container name (see configuration)
    * @param valueStorageProvider
    *          External Value Storages provider
    * @param maxBufferSize
    *          Maximum buffer size (see configuration)
    * @param swapDirectory
    *          Swap directory File (see configuration)
    * @param swapCleaner
    *          Swap cleaner (internal FileCleaner).
    * @throws SQLException
    * 
    * @see org.exoplatform.services.jcr.impl.util.io.FileCleaner
    */
   public MySQLSingleDbJDBCConnection(Connection dbConnection, String containerName,
            ValueStoragePluginProvider valueStorageProvider, int maxBufferSize, File swapDirectory,
            FileCleaner swapCleaner) throws SQLException
   {

      super(dbConnection, containerName, valueStorageProvider, maxBufferSize, swapDirectory, swapCleaner);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addNodeRecord(NodeData data) throws SQLException
   {
      // check if parent exists
      if (data.getParentIdentifier() != null)
      {
         ResultSet item = findItemByIdentifier(getInternalId(data.getParentIdentifier()));
         try
         {
            if (!item.next())
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
         }
         finally
         {
            item.close();
         }
      }
      return super.addNodeRecord(data);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected int addPropertyRecord(PropertyData data) throws SQLException
   {
      // check if parent exists
      if (data.getParentIdentifier() != null)
      {
         ResultSet item = findItemByIdentifier(getInternalId(data.getParentIdentifier()));
         try
         {
            if (!item.next())
               throw new SQLException("Parent is not found. Behaviour of " + JCR_FK_ITEM_PARENT);
         }
         finally
         {
            item.close();
         }
      }
      return super.addPropertyRecord(data);
   }

}
