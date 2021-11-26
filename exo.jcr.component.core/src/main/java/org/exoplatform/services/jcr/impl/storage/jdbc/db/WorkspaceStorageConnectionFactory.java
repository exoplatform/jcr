/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.storage.jdbc.db;

import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;

import java.sql.Connection;

import javax.jcr.RepositoryException;

/**
 * WorkspaceStorageConnectionFactory interface.
 * 
 * Describe methods contract of Workspace Connections Factory.
 * 
 */
public interface WorkspaceStorageConnectionFactory
{

   /**
    * Open connection to Workspace storage.
    * 
    * @return WorkspaceStorageConnection connection
    * @throws RepositoryException
    *           if error occurs
    */
   WorkspaceStorageConnection openConnection() throws RepositoryException;

   /**
    * Open connection to Workspace storage.
    * 
    * @param readOnly
    *          boolean, if true the Connection will be marked as read-only
    * 
    * @return WorkspaceStorageConnection connection
    * @throws RepositoryException
    *           if error occurs
    */
   WorkspaceStorageConnection openConnection(boolean readOnly) throws RepositoryException;

   /**
     * Return native JDBC Connection to workspace storage (JDBC specific).
     * 
     * @return java.sql.Connection connection
     * @throws RepositoryException
     *           if error occurs
     */
   Connection getJdbcConnection() throws RepositoryException;

   /**
    * Return native JDBC Connection to workspace storage (JDBC specific).
    * 
    * @param readOnly
    *          boolean, if true the JDBC Connection will be marked as read-only, see
    *          {@link java.sql.Connection#setReadOnly(boolean)}
    * 
    * @return java.sql.Connection connection
    * @throws RepositoryException
    *           if error occurs
    */
   Connection getJdbcConnection(boolean readOnly) throws RepositoryException;
}
