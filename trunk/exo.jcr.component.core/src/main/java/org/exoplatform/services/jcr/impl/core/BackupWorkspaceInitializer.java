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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.IllegalNameException;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.backup.JCRRestore;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;

import java.io.File;
import java.io.IOException;

import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLStreamException;

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
      AccessManager accessManager, FileCleanerHolder cleanerHolder) throws RepositoryConfigurationException,
      PathNotFoundException, RepositoryException
   {
      super(config, repConfig, dataManager, namespaceRegistry, locationFactory, nodeTypeManager, valueFactory,
               accessManager, cleanerHolder);

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

   @Override
   public NodeData initWorkspace() throws RepositoryException
   {

      if (isWorkspaceInitialized())
      {
         return (NodeData)dataManager.getItemData(Constants.ROOT_UUID);
      }

      try
      {
         long start = System.currentTimeMillis();

         // restore from full backup
         PlainChangesLog changes = read();

         TransactionChangesLog tLog = new TransactionChangesLog(changes);
         tLog.setSystemId(Constants.JCR_CORE_RESTORE_WORKSPACE_INITIALIZER_SYSTEM_ID); // mark changes

         dataManager.save(tLog);

         // restore from incremental backup
         JCRRestore restorer = new JCRRestore(dataManager, fileCleaner);
         for (File incrBackupFile : JCRRestore.getIncrementalFiles(new File(restoreDir)))
         {
            restorer.incrementalRestore(incrBackupFile);
         }

         final NodeData root = (NodeData)dataManager.getItemData(Constants.ROOT_UUID);

         log.info("Workspace " + workspaceName + " restored from file " + restorePath + " in "
            + (System.currentTimeMillis() - start) * 1d / 1000 + "sec");

         return root;
      }
      catch (XMLStreamException e)
      {
         throw new RepositoryException("The XML file is corrupted : " + restorePath, e);
      }
      catch (FactoryConfigurationError e)
      {
         throw new RepositoryException(e);
      }
      catch (IOException e)
      {
         throw new RepositoryException(e);
      }
      catch (IllegalNameException e)
      {
         throw new RepositoryException(e);
      }
      catch (ClassNotFoundException e)
      {
         throw new RepositoryException(e);
      }
   }
}
