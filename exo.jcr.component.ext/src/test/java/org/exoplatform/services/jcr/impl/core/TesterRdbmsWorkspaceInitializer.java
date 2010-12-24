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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RDBMSBackupInfoReader;
import org.exoplatform.services.jcr.ext.backup.impl.rdbms.RdbmsWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS
 * 
 * 04.12.2006
 * 
 * For testing purpose
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: SessionDataManagerTestWrapper.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TesterRdbmsWorkspaceInitializer extends RdbmsWorkspaceInitializer
{

   public TesterRdbmsWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory,
            AccessManager accessManager, RepositoryService repositoryService, FileCleanerHolder cleanerHolder)
            throws RepositoryConfigurationException,
      PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
               accessManager, repositoryService, cleanerHolder);
   }

   public void restoreValueFiles() throws RepositoryConfigurationException, IOException
   {
      super.restoreValueStorage();
   }

   public void restoreIndexFiles() throws RepositoryConfigurationException, IOException
   {
      super.restoreIndex();
   }

   public void restoreTables(Connection jdbcConn, int tableType, boolean isMultiDB, LockManagerEntry lockManagerEntry,
      String storageDir) throws RepositoryConfigurationException, IOException, SQLException
   {
      RDBMSBackupInfoReader backupInfo = new RDBMSBackupInfoReader(storageDir);
      RestoreTableHelper helper = new RestoreTableHelper(tableType, isMultiDB, backupInfo);

      super.restoreTable(jdbcConn, helper);
   }

}
