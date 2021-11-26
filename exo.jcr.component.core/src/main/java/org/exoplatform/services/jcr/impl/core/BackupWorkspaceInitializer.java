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

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.backup.JCRRestore;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;

import java.io.File;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua
 * 22.05.2008
 * 
 * @version $Id: BackupWorkspaceInitializer.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class BackupWorkspaceInitializer extends SysViewWorkspaceInitializer
{
   protected final String restoreDir;

   public BackupWorkspaceInitializer(WorkspaceEntry config, RepositoryEntry repConfig,
      CacheableWorkspaceDataManager dataManager, NamespaceRegistryImpl namespaceRegistry,
      LocationFactory locationFactory, NodeTypeManagerImpl nodeTypeManager, ValueFactoryImpl valueFactory,
      AccessManager accessManager) throws RepositoryConfigurationException,
      PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
         accessManager);

      restoreDir = restorePath;

      String fullBackupPath = PrivilegedFileHelper.getAbsolutePath(JCRRestore.getFullBackupFile(new File(restoreDir)));

      if (fullBackupPath == null)
      {
         throw new RepositoryException("Can't find full backup storage");
      }
      else
      {
         restorePath = fullBackupPath;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void doRestore() throws Throwable
   {
      super.doRestore();

      // restore from incremental backup
      JCRRestore restorer = new JCRRestore(dataManager, spoolConfig.fileCleaner);
      for (File incrBackupFile : JCRRestore.getIncrementalFiles(new File(restoreDir)))
      {
         restorer.incrementalRestore(incrBackupFile);
      }
   }
}
