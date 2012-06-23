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
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.lock.jbosscache;

import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.checker.DummyRepair;
import org.exoplatform.services.jcr.impl.checker.InspectionQuery;
import org.exoplatform.services.jcr.impl.core.lock.LockTableHandler;

import java.sql.SQLException;

import javax.sql.DataSource;

/**
 * Provides means for nodes' IDs extraction in case we use {@link CacheableLockManagerImpl}
 * as {@link LockManager} based on shareable JBoss Cache instance.
 * Equally applicable for single and multi db configurations.
 * 
 * @author <a href="mailto:dkuleshov@exoplatform.com">Dmitry Kuleshov</a>
 * @version $Id: JBCShareableLockTableHandler.java 34360 2009-07-22 23:58:59Z dkuleshov $
 *
 */
public class JBCShareableLockTableHandler extends JBCLockTableHandler implements LockTableHandler
{

   /**
    * JBCShareableLockTableHandler constructor.
    */
   public JBCShareableLockTableHandler(WorkspaceEntry workspaceEntry, DataSource ds)
   {
      super(workspaceEntry, ds);
   }

   /**
    * {@inheritDoc}
    */
   protected InspectionQuery getSelectQuery() throws SQLException
   {
      return new InspectionQuery("SELECT * FROM " + getTableName() + " WHERE " + getParentColumn() + "='/"
         + workspaceEntry.getUniqueName() + "/" + CacheableLockManagerImpl.LOCKS + "'", new String[]{getIdColumn()},
         "Locks table match", new DummyRepair());
   }

   protected InspectionQuery getDeleteQuery(String nodeId) throws SQLException
   {
      return new InspectionQuery("DELETE FROM " + getTableName() + " WHERE " + getIdColumn() + "='/"
         + workspaceEntry.getUniqueName() + "/" + CacheableLockManagerImpl.LOCKS + "/" + nodeId + "'", new String[]{},
         "", new DummyRepair());
   }
}
