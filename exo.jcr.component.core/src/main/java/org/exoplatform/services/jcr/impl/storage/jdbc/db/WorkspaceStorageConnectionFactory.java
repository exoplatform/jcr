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
