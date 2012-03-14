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
package org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db;

import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.sql.SQLException;

import javax.jcr.RepositoryException;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 8 02  2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: MSSQLConnectionFactory.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class MSSQLConnectionFactory extends GenericCQConnectionFactory
{

   /**
    * MSSQLConnectionFactory  constructor.
    */
   public MSSQLConnectionFactory(DataSource dbDataSource, JDBCDataContainerConfig containerConfig)
   {
      super(dbDataSource, containerConfig);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException
   {
      try
      {
         if (this.containerConfig.dbStructureType.isMultiDatabase())
         {
            return new MSSQLMultiDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);
         }

         return new MSSQLSingleDbJDBCConnection(getJdbcConnection(readOnly), readOnly, containerConfig);

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
      return true;
   }
}
