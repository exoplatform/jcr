/*
 * Copyright (C) 2011 eXo Platform SAS.
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
package org.exoplatform.connectors.jcr.adapter;

import java.io.Serializable;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.resource.cci.ConnectionFactory;

/**
 * The equivalent of a {@link ConnectionFactory} for the JCR
 * 
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public interface SessionFactory  extends Serializable
{
   /**
    * Get a JCR session corresponding to the repository
    * defined in the configuration and the default workspace.
    * @return a JCR session corresponding to the criteria
    * @throws RepositoryException if the session could not be created
    */
   Session getSession() throws RepositoryException;

   /**
    * Get a JCR session corresponding to the repository
    * defined in the configuration and the default workspace, using
    * the given user name and password.
    * @param userName the user name to use for the authentication
    * @param password the password to use for the authentication
    * @return a JCR session corresponding to the criteria
    * @throws RepositoryException if the session could not be created
    */
   Session getSession(String userName, String password) throws RepositoryException;

   /**
    * Get a JCR session corresponding to the repository
    * defined in the configuration and the given workspace.
    * @param workspace the name of the expected workspace
    * @return a JCR session corresponding to the criteria
    * @throws RepositoryException if the session could not be created
    */
   Session getSession(String workspace) throws RepositoryException;

   /**
    * Get a JCR session corresponding to the repository
    * defined in the configuration and the given workspace, using
    * the given user name and password.
    * @param workspace the name of the expected workspace
    * @param userName the user name to use for the authentication
    * @param password the password to use for the authentication
    * @return a JCR session corresponding to the criteria
    * @throws RepositoryException if the session could not be created
    */
   Session getSession(String workspace, String userName, String password) throws RepositoryException;
}
