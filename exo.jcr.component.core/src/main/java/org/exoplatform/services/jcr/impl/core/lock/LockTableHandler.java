/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core.lock;

import java.sql.SQLException;
import java.util.Set;

/**
 * Provides method for extraction of set of locked nodes' IDs from
 * {@link org.exoplatform.services.jcr.config.LockManagerEntry} persistant layer (database lock table),
 * which can be further used for consistency check.
 *  
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @LevelAPI Unsupported
 */
public interface LockTableHandler
{
   /**
    * Get a set of locked jcr nodes IDs contained in {@link org.exoplatform.services.jcr.config.LockManagerEntry} persistent layer (database table).
    * 
    * @return {@link Set} of node IDs
    * @throws SQLException
    */
   Set<String> getLockedNodesIds() throws SQLException;

   /**
    * Removes locked node directly from database.
    * 
    * @param nodeId
    *          node identifier
    * @throws SQLException
    */
   void removeLockedNode(String nodeId) throws SQLException;

   /**
    * Removes all locked node directly from database.
    *
    * @throws SQLException
    */
   void cleanLocks() throws SQLException;
}
