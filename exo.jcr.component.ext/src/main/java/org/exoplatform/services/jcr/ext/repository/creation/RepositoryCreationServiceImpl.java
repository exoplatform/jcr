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
package org.exoplatform.services.jcr.ext.repository.creation;

import org.exoplatform.services.database.creator.DBConnectionInfo;
import org.exoplatform.services.database.creator.DBCreator;
import org.exoplatform.services.database.creator.DBCreatorException;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.rpc.RPCService;
import org.jboss.cache.util.concurrent.ConcurrentHashSet;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.naming.NamingException;
import javax.sql.DataSource;
import javax.xml.stream.XMLStreamException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: RepositoryCreationServiceImpl.java 111 2008-11-11 11:11:11Z serg $
 */
public class RepositoryCreationServiceImpl implements RepositoryCreationService
{
   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryCreationSynchronizer");

   private final RepositoryService repositoryService;

   /**
    * The RPC Service used to communicate with other nodes
    */
   private final RPCService rpcService;

   private final BackupManager backupManager;

   private final DBCreator dbCreator;

   private final InitialContextInitializer initialContextInitializer;

   private final Set<String> pendingRepositories = new ConcurrentHashSet<String>();

   private final Set<String> namesUnderNegotiation = new ConcurrentHashSet<String>();

   public RepositoryCreationServiceImpl(RepositoryService repositoryService, BackupManager backupManager,
      DBCreator dbCreator, InitialContextInitializer initialContextInitializer, final RPCService rpcService)
   {
      this.repositoryService = repositoryService;
      this.backupManager = backupManager;
      this.rpcService = rpcService;
      this.dbCreator = dbCreator;
      this.initialContextInitializer = initialContextInitializer;
   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry) throws RepositoryConfigurationException,
      RepositoryCreationException
   {
      reserveRepositoryName(rEntry.getName());
      //      1. check possibility to create repository locally
      //          - check existing, pending repository and datasources with same names

      //      2. reserve name and put additional information (ex. ip and port of current machine)
      //      3. check possibility to create repository on others nodes 
      //         - sending to all cluster nodes information about new repository and waiting for answers
      //         - all cluster nodes receive information and check possibility to create repository locally
      //         - send response 
      //      4. reserve name on all nodes of cluster

   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry, String rToken)
      throws RepositoryConfigurationException, RepositoryCreationException
   {
      // check does token registered
      if (!this.pendingRepositories.contains(rToken))
      {
         throw new RepositoryCreationException("Token " + rToken + " does not registered.");
      }

      // Prepare list of datasource names that must be binded to newly created databases.
      Set<String> dataSourceNames = new HashSet<String>();
      for (WorkspaceEntry wsEntry : rEntry.getWorkspaceEntries())
      {

         boolean isMultiDB =
            Boolean.parseBoolean(wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.MULTIDB));
         String dbSourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

         if (isMultiDB && dataSourceNames.contains(dbSourceName))
         {
            throw new RepositoryCreationException("RepositoryEntry for new " + rToken
               + " repository contains workspaces that marked as multiDB but have same datasource " + dbSourceName
               + ".");
         }

         try
         {
            DataSource ds = (DataSource)initialContextInitializer.getInitialContext().lookup(dbSourceName);
            if (ds != null)
            {
               throw new RepositoryConfigurationException("RepositoryEntry for new " + rToken
                  + " repository contains already bibded datasource " + dbSourceName + ".");
            }
         }
         catch (NamingException e)
         {
            throw new RepositoryConfigurationException(e.getMessage(), e);
         }

         dataSourceNames.add(dbSourceName);
      }

      // create and bind related database to each datasource name
      for (String dataSource : dataSourceNames)
      {
         // 1) create related DB
         Map<String, String> refAddr = new HashMap<String, String>();
         try
         {
            DBConnectionInfo dbConnectionInfo = dbCreator.createDatabase(rToken + "_" + dataSource);
            refAddr.put("driverClassName", dbConnectionInfo.getDriver());
            refAddr.put("url", dbConnectionInfo.getUrl());
            refAddr.put("username", dbConnectionInfo.getUsername());
            refAddr.put("password", dbConnectionInfo.getPassword());
         }
         catch (DBCreatorException e)
         {
            throw new RepositoryCreationException("Can not create new database for " + rToken + " repository.", e);
         }

         // 2) bind data-source
         try
         {
            //bind new data-source
            initialContextInitializer.bind(dataSource, "javax.sql.DataSource",
               "org.apache.commons.dbcp.BasicDataSourceFactory", null, refAddr);
         }
         catch (NamingException e)
         {
            throw new RepositoryCreationException(e.getMessage(), e);
         }
         catch (FileNotFoundException e)
         {
            throw new RepositoryCreationException(e.getMessage(), e);
         }
         catch (XMLStreamException e)
         {
            throw new RepositoryCreationException(e.getMessage(), e);
         }
      }

      //3) restore repository from backup
      RepositoryBackupChainLog backupChain = null;
      for (RepositoryBackupChainLog chainLog : backupManager.getRepositoryBackupsLogs())
      {
         if (chainLog.getBackupId().equals(backupId))
         {
            backupChain = chainLog;
            break;
         }
      }

      if (backupChain == null)
      {
         throw new RepositoryCreationException("BackupChain by id " + backupId + " does not exists.");
      }

      File backLog = new File(backupChain.getLogFilePath());
      if (backLog != null && backLog.exists())
      {
         try
         {
            backupManager.restore(backupChain, rEntry, false);
         }
         catch (BackupOperationException e)
         {
            throw new RepositoryCreationException(e.getLocalizedMessage(), e);
         }
         catch (BackupConfigurationException e)
         {
            throw new RepositoryCreationException(e.getLocalizedMessage(), e);
         }
         catch (RepositoryException e)
         {
            throw new RepositoryCreationException(e.getLocalizedMessage(), e);
         }
      }
      else
      {
         throw new RepositoryCreationException("Backup log file by id " + backupId
            + (backLog != null ? (" and file path=" + backLog.getAbsolutePath()) : "") + " do not exists.");
      }
      //TODO execute "clone repository" on other cluster nodes
      // release tokens
   }

   /**
    * {@inheritDoc}
    */
   public String reserveRepositoryName(String repositoryName) throws RepositoryCreationException
   {
      //      1. check possibility to create repository locally
      //         - check existing, pending repository and datasources with same names

      for (int i = 0; i < repositoryService.getConfig().getRepositoryConfigurations().size(); i++)
      {
         RepositoryEntry conf = repositoryService.getConfig().getRepositoryConfigurations().get(i);
         if (conf.getName().equals(repositoryName))
         {
            throw new RepositoryCreationException("Repository " + repositoryName + " already exists.");
         }

      }

      //      try
      //      {
      //         ManageableRepository repo = repositoryService.getRepository(repositoryName);
      //         if (repo != null)
      //         {
      //            throw new RepositoryCreationException("Repository " + repositoryName + " already exists.");
      //         }
      //      }
      //      catch (RepositoryException e)
      //      {
      //         throw new RepositoryCreationException(e.getMessage(), e);
      //      }
      //      catch (RepositoryConfigurationException e)
      //      {
      //         throw new RepositoryCreationException(e.getMessage(), e);
      //      }

      if (namesUnderNegotiation.contains(repositoryName) || pendingRepositories.contains(repositoryName))
      {
         throw new RepositoryCreationException("Repository name " + repositoryName + " already reserved.");
      }

      //      2. reserve name and put additional information (ex. ip and port of current machine)
      namesUnderNegotiation.add(repositoryName);

      // TODO ask other nodes does thay have pending repositories

      pendingRepositories.add(repositoryName);
      namesUnderNegotiation.remove(repositoryName);
      // TODO register repositoryName on other nodes

      //     3. check possibility to create repository on others nodes 
      //         - sending to all cluster nodes information about new repository and waiting for answers
      //         - all cluster nodes receive information and check possibility to create repository locally
      //         - send response 
      //      4. reserve name on all nodes of cluster

      return repositoryName;
   }

}
