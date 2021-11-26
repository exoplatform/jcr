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

package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: H2ConnectionFactory.java 34360 23 Oct 2012 andrew.plotnikov $
 *
 */
public class H2ConnectionFactory extends GenericCQConnectionFactory
{

   /**
    * H2ConnectionFactory constructor.
    */
   public H2ConnectionFactory(DataSource dataSource, JDBCDataContainerConfig containerConfig)
   {
      super(dataSource, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {

         if (this.containerConfig.dbStructureType.isMultiDatabase())
         {
            return new H2MultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);
         }

         return new H2SingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);

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
   public boolean isReindexingSupported()
   {
      return false;
   }

}
