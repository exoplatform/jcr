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
package org.exoplatform.services.jcr.impl.core.lock;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;

import java.sql.SQLException;
import java.util.Set;

import javax.naming.NamingException;

/**
 * Provides method for extraction of set of locked nodes' IDs from
 * {@link LockManager} persistant layer (database lock table),
 * which can be further used for consistency check.
 *  
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 */
public interface LockTableHandler
{
   /**
    * Get a set of locked jcr nodes IDs contained in {@link LockManager} persistent layer (database table).
    * 
    * @return {@link Set} of node IDs
    * @throws NamingException
    * @throws RepositoryConfigurationException
    * @throws SQLException
    */
   Set<String> getLockedNodesIds() throws NamingException, RepositoryConfigurationException, SQLException;
}