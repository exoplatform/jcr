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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * This factory needs to work with new version pgsql 9.1 because default configuration was changed in new version pgsql.
 * standard_conforming_strings parameter is on but in the older version this parameter is off.
 * 
 * @author <a href="mailto:aplotnikov@exoplatform.com">Andrey Plotnikov</a>
 * @version $Id: PostgreSCSConnectionFactory.java 34360 26 Sep 2012 andrew.plotnikov $
 */
public class PostgreSCSConnectionFactory extends PostgreConnectionFactory
{

   /**
    * PostgreConnectionFactory  constructor.
    */
   public PostgreSCSConnectionFactory(DataSource dbDataSource, JDBCDataContainerConfig containerConfig)
   {
      super(dbDataSource, containerConfig);
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
            return new PostgreSCSMultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);
         }

         return new PostgreSCSSingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);
      }
      catch (SQLException e)
      {
         throw new RepositoryException(e);
      }
   }

}
