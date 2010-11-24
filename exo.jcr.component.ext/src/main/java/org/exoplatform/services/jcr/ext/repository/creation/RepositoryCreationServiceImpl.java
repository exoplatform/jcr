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
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.naming.InitialContextInitializer;
import org.exoplatform.services.rpc.RPCException;
import org.exoplatform.services.rpc.RPCService;
import org.exoplatform.services.rpc.RemoteCommand;
import org.exoplatform.ws.frameworks.json.JsonHandler;
import org.exoplatform.ws.frameworks.json.JsonParser;
import org.exoplatform.ws.frameworks.json.impl.BeanBuilder;
import org.exoplatform.ws.frameworks.json.impl.JsonDefaultHandler;
import org.exoplatform.ws.frameworks.json.impl.JsonException;
import org.exoplatform.ws.frameworks.json.impl.JsonGeneratorImpl;
import org.exoplatform.ws.frameworks.json.impl.JsonParserImpl;
import org.exoplatform.ws.frameworks.json.value.JsonValue;
import org.jboss.cache.util.concurrent.ConcurrentHashSet;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
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
    * The RPC Service used to communicate with other nodes.
    */
   private final RPCService rpcService;

   /**
    * BackupManager used to restore repository from backup.
    */
   private final BackupManager backupManager;

   /**
    * DBCreator used to create database. Only database not tables, indexes, etc.
    */
   private final DBCreator dbCreator;

   /**
    * InitalContextInitalizer used to bind new datasource.
    */
   private final InitialContextInitializer initialContextInitializer;

   /**
    * Store of reserved repository names.
    */
   private final Set<String> pendingRepositories = new ConcurrentHashSet<String>();

   private RemoteCommand reserveRepositoryName;

   private RemoteCommand createRepository;

   private RemoteCommand startRepository;

   public RepositoryCreationServiceImpl(RepositoryService repositoryService, BackupManager backupManager,
      DBCreator dbCreator, InitialContextInitializer initialContextInitializer, final RPCService rpcService)
   {
      this.repositoryService = repositoryService;
      this.backupManager = backupManager;
      this.rpcService = rpcService;
      this.dbCreator = dbCreator;
      this.initialContextInitializer = initialContextInitializer;

      if (rpcService == null)
      {
         LOG.warn("RepositoryCreationService initialized with null RPCService, so other cluser nodes will"
            + " not be notified about new repositories.");
      }
      else
      {
         // register commands
         reserveRepositoryName = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl-reserveRepositoryName";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               System.out.println("EXECUTED reserveRepositoryName");

               String repositoryName = (String)args[0];
               if (!pendingRepositories.contains(repositoryName))
               {
                  pendingRepositories.add(repositoryName);
               }
               else
               {
                  throw new RepositoryCreationException("Repository name " + repositoryName + " already reserved.");
               }
               return repositoryName;
            }
         });
         createRepository = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl-createRepository";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               System.out.println("EXECUTED createRepository");
               //String backupId, RepositoryEntry rEntry, String rToken
               String backupId = (String)args[0];
               String stringRepositoryEntry = (String)args[1];
               String rToken = (String)args[2];

               try
               {
                  RepositoryEntry rEntry =
                     (RepositoryEntry)(getObject(RepositoryEntry.class, stringRepositoryEntry
                        .getBytes(Constants.DEFAULT_ENCODING)));

                  createRepo(backupId, rEntry, rToken);

                  // execute startRepository at all cluster nodes (coordinator will ignore this command)
                  try
                  {
                     rpcService.executeCommandOnAllNodes(startRepository, false, stringRepositoryEntry);
                  }
                  catch (RPCException e)
                  {
                     Throwable cause = e.getCause();
                     if (cause instanceof RepositoryCreationException)
                     {
                        throw new RepositoryCreationException(
                           "Repository " + rEntry.getName()
                              + " created on coordinator, can not be started at other cluster node: "
                              + cause.getMessage(), cause);
                     }
                     else
                     {
                        throw new RepositoryCreationException("Repository " + rEntry.getName()
                           + " created on coordinator, can not be started at other cluster node: " + e.getMessage(), e);
                     }
                  }
                  return null;
               }
               finally
               {
                  // release tokens
                  pendingRepositories.remove(rToken);
               }
            }
         });

         startRepository = rpcService.registerCommand(new RemoteCommand()
         {

            public String getId()
            {
               return "org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl-startRepository";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               System.out.println("EXECUTED startRepository");
               // must not be executed on coordinator node, since coordinator node already created the repository
               if (!rpcService.isCoordinator())
               {
                  //RepositoryEntry (as String) rEntry
                  String stringRepositoryEntry = (String)args[0];
                  RepositoryEntry rEntry =
                     (RepositoryEntry)(getObject(RepositoryEntry.class, stringRepositoryEntry
                        .getBytes(Constants.DEFAULT_ENCODING)));

                  startRepository(rEntry);
               }
               return null;
            }
         });
      }

   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry) throws RepositoryConfigurationException,
      RepositoryCreationException
   {
      String rToken = reserveRepositoryName(rEntry.getName());
      createRepository(backupId, rEntry, rToken);
   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry, String rToken)
      throws RepositoryConfigurationException, RepositoryCreationException
   {

      if (rpcService != null)
      {
         //TODO is is correct to use Json code here
         String stringRepositoryEntry = null;
         try
         {
            JsonGeneratorImpl generatorImpl = new JsonGeneratorImpl();
            JsonValue json = generatorImpl.createJsonObject(rEntry);
            stringRepositoryEntry = json.toString();
         }
         catch (JsonException e)
         {
            throw new RepositoryCreationException("Can not serialize repository entry: " + e.getMessage(), e);
         }

         try
         {
            Object result =
               rpcService.executeCommandOnCoordinator(createRepository, true, backupId, stringRepositoryEntry, rToken);

            if (result != null)
            {
               throw new RepositoryCreationException("ReserveRepositoryName command returns unknown command.");
            }
         }
         catch (RPCException e)
         {
            Throwable cause = ((RPCException)e).getCause();
            if (cause instanceof RepositoryCreationException)
            {
               throw (RepositoryCreationException)cause;
            }
            else if (cause instanceof RepositoryConfigurationException)
            {
               throw (RepositoryConfigurationException)cause;
            }
            else
            {
               throw new RepositoryCreationException(e.getMessage(), e);
            }
         }
      }
      else
      {
         try
         {
            createRepo(backupId, rEntry, rToken);
         }
         finally
         {
            pendingRepositories.remove(rEntry);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public String reserveRepositoryName(String repositoryName) throws RepositoryCreationException
   {

      // check possibility to create repository locally
      //check does repository already created
      for (int i = 0; i < repositoryService.getConfig().getRepositoryConfigurations().size(); i++)
      {
         RepositoryEntry conf = repositoryService.getConfig().getRepositoryConfigurations().get(i);
         if (conf.getName().equals(repositoryName))
         {
            throw new RepositoryCreationException("Repository " + repositoryName + " already exists.");
         }
      }

      if (rpcService != null)
      {
         //ask other nodes does they have pending repositories
         try
         {
            Object result = rpcService.executeCommandOnCoordinator(reserveRepositoryName, true, repositoryName);

            if (result instanceof String)
            {
               return (String)result;
            }
            else
            {
               throw new RepositoryCreationException("ReserveRepositoryName command returns unknown type result.");
            }
         }
         catch (RPCException e)
         {
            Throwable cause = ((RPCException)e).getCause();
            if (cause instanceof RepositoryCreationException)
            {
               throw (RepositoryCreationException)cause;
            }
            else
            {
               throw new RepositoryCreationException(e.getMessage(), e);
            }
         }
      }
      else
      {
         pendingRepositories.add(repositoryName);
         return repositoryName;
      }
   }

   protected void createRepo(String backupId, RepositoryEntry rEntry, String rToken)
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
   }

   protected void startRepository(RepositoryEntry repositoryEntry) throws RepositoryCreationException
   {
      //TODO do we need bind datasource here?

      try
      {
         repositoryService.createRepository(repositoryEntry);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryCreationException("Repository created but can not be started on this node: "
            + e.getLocalizedMessage(), e);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryCreationException("Repository created but can not be started on this node: "
            + e.getLocalizedMessage(), e);
      }
   }

   /**
    * TODO make another serialization scheme or move it to dedicated object
    * Will be created the Object from JSON binary data.
    * 
    * @param cl
    *          Class
    * @param data
    *          binary data (JSON)
    * @return Object
    * @throws Exception
    *           will be generated Exception
    */
   private Object getObject(Class cl, byte[] data) throws Exception
   {
      JsonHandler jsonHandler = new JsonDefaultHandler();
      JsonParser jsonParser = new JsonParserImpl();
      InputStream inputStream = new ByteArrayInputStream(data);
      jsonParser.parse(inputStream, jsonHandler);
      JsonValue jsonValue = jsonHandler.getJsonObject();

      return new BeanBuilder().createObject(cl, jsonValue);
   }
}
