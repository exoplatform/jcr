/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 19 mars 2010  
 */
public class DefaultOracleConnectionFactory extends GenericCQConnectionFactory
{
   /**
    * DefaultOracleConnectionFactory constructor.
    */
   public DefaultOracleConnectionFactory(DataSource dataSource, JDBCDataContainerConfig containerConfig)
   {
      super(dataSource, containerConfig);
   }

   /**
    * DefaultOracleConnectionFactory constructor.
    */
   public DefaultOracleConnectionFactory(JDBCDataContainerConfig containerConfig) throws RepositoryException
   {

      super(containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         if (containerConfig.useQueryHints)
         {
            if (this.containerConfig.dbStructureType.isSimpleTable())
            {
               return new OracleMultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly,
                  containerConfig);
            }

            return new OracleSingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly,
               containerConfig);
         }
         else
         {
            // use common CQ queries, since Oracle[Multi/Single]DbJDBCConnection contains only queries with hints
            return super.openConnection(readOnly);
         }

      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean isIDNeededForPaging()
   {
      return false;
   }
}
