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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.database.creator.DBConnectionInfo;
import org.exoplatform.services.database.creator.DBCreator;
import org.exoplatform.services.database.creator.DBCreatorException;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.ext.backup.BackupConfigurationException;
import org.exoplatform.services.jcr.ext.backup.BackupManager;
import org.exoplatform.services.jcr.ext.backup.BackupOperationException;
import org.exoplatform.services.jcr.ext.backup.RepositoryBackupChainLog;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jdbc.impl.CloseableDataSource;
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
import org.picocontainer.Startable;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;
import javax.naming.NameNotFoundException;
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
public class RepositoryCreationServiceImpl implements RepositoryCreationService, Startable
{
   /**
    * The logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RepositoryCreationService");

   /**
    * The parameter name in service's configuration.
    */
   private final static String FACTORY_CLASSS_NAME_PARAM = "factory-class-name";

   /**
    * The default factory class name.
    */
   private static final String DEFAULT_DATA_SOURCE_FACTORY = "org.apache.commons.dbcp.BasicDataSourceFactory";

   /**
    * The factory class name to create object.
    */
   private String factoryClassName = DEFAULT_DATA_SOURCE_FACTORY;

   /**
    * The Repository service.
    */
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
    * Exo container context;
    */
   private ExoContainerContext context;


   /**
    * InitalContextInitalizer used to bind new datasource.
    */
   private final InitialContextInitializer initialContextInitializer;

   /**
    * Store of reserved repository names. {tokenname, repositoryname}
    */
   private final Map<String, String> pendingRepositories = new ConcurrentHashMap<String, String>();

   private RemoteCommand reserveRepositoryName;

   private RemoteCommand createRepository;

   private RemoteCommand startRepository;

   private RemoteCommand removeRepository;

   private RemoteCommand canRemoveRepository;

   /**
    * Constructor RepositoryCreationServiceImpl.
    */
   public RepositoryCreationServiceImpl(InitParams initParams, RepositoryService repositoryService,
      BackupManager backupManager, ExoContainerContext context, InitialContextInitializer initialContextInitializer)
   {
      this(initParams, repositoryService, backupManager, context, initialContextInitializer, null);
   }

   /**
    * Constructor RepositoryCreationServiceImpl.
    */
   public RepositoryCreationServiceImpl(InitParams initParams, final RepositoryService repositoryService,
      BackupManager backupManager, ExoContainerContext context, InitialContextInitializer initialContextInitializer,
      final RPCService rpcService)
   {
      if (initParams != null)
      {
         // set reference class name for datasource binding from initialization parameters
         factoryClassName = initParams.getValueParam(FACTORY_CLASSS_NAME_PARAM).getValue();
      }
      
      this.repositoryService = repositoryService;
      this.backupManager = backupManager;
      this.rpcService = rpcService;
      this.context = context;
      this.initialContextInitializer = initialContextInitializer;

      if (rpcService != null)
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
               String repositoryName = (String)args[0];
               return reserveRepositoryNameLocally(repositoryName);
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
               String backupId = (String)args[0];
               String stringRepositoryEntry = (String)args[1];
               String rToken = (String)args[2];
               DBCreationProperties creationProps = (DBCreationProperties)args[3];

               try
               {
                  RepositoryEntry rEntry =
                     (RepositoryEntry)(getObject(RepositoryEntry.class,
                        stringRepositoryEntry.getBytes(Constants.DEFAULT_ENCODING)));

                  createRepositoryLocally(backupId, rEntry, rToken, creationProps);
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
               // must not be executed on coordinator node, since coordinator node already created the repository
               if (!rpcService.isCoordinator())
               {
                  //RepositoryEntry (as String) rEntry
                  String stringRepositoryEntry = (String)args[0];
                  RepositoryEntry rEntry =
                     (RepositoryEntry)(getObject(RepositoryEntry.class,
                        stringRepositoryEntry.getBytes(Constants.DEFAULT_ENCODING)));

                  DBCreationProperties creationProps = (DBCreationProperties)args[1];

                  startRepository(rEntry, creationProps);
               }
               return null;
            }
         });

         removeRepository = rpcService.registerCommand(new RemoteCommand()
         {
            public String getId()
            {
               return "org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl-removeRepository";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               String repositoryName = (String)args[0];

               removeRepositoryLocally(repositoryName);

               return null;
            }
         });

         canRemoveRepository = rpcService.registerCommand(new RemoteCommand()
         {
            public String getId()
            {
               return "org.exoplatform.services.jcr.ext.repository.creation.RepositoryCreationServiceImpl-checkRepositoryInUse";
            }

            public Serializable execute(Serializable[] args) throws Throwable
            {
               String repositoryName = (String)args[0];
               
               return new Boolean(repositoryService.canRemoveRepository(repositoryName));
            }
         });
      }
      else
      {
         LOG.warn("RepositoryCreationService initialized without RPCService, so other cluser nodes will"
            + " not be notified about new repositories.");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry) throws RepositoryConfigurationException,
      RepositoryCreationException
   {
      String rToken = reserveRepositoryName(rEntry.getName());
      createRepositoryInternally(backupId, rEntry, rToken, null);
   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry, StorageCreationProperties creationProps)
      throws RepositoryConfigurationException, RepositoryCreationException
   {
      String rToken = reserveRepositoryName(rEntry.getName());

      if (creationProps instanceof DBCreationProperties)
      {
         createRepositoryInternally(backupId, rEntry, rToken, (DBCreationProperties)creationProps);
      }
      else
      {
         throw new RepositoryCreationException("creationProps should be the instance of DBCreationProperties");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry, String rToken)
      throws RepositoryConfigurationException, RepositoryCreationException
   {
      createRepositoryInternally(backupId, rEntry, rToken, null);
   }
   
   /**
    * {@inheritDoc}
    */
   public void createRepository(String backupId, RepositoryEntry rEntry, String rToken,
      StorageCreationProperties creationProps) throws RepositoryConfigurationException, RepositoryCreationException
   {
      if (creationProps instanceof DBCreationProperties)
      {
         createRepositoryInternally(backupId, rEntry, rToken, (DBCreationProperties)creationProps);
      }
      else
      {
         throw new RepositoryCreationException("creationProps should be the instance of DBCreationProperties");
      }

   }

   /**
    * Create repository internally. serverUrl and connProps contain specific properties for db creation.
    */
   protected void createRepositoryInternally(String backupId, RepositoryEntry rEntry, String rToken,
      DBCreationProperties creationProps) throws RepositoryConfigurationException, RepositoryCreationException
   {
      if (rpcService != null)
      {
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

         // notify coordinator node to create repository
         try
         {
            Object result =
               rpcService.executeCommandOnCoordinator(createRepository, true, backupId, stringRepositoryEntry, rToken,
                  creationProps);

            if (result != null)
            {
               if (result instanceof Throwable)
               {
                  throw new RepositoryCreationException("Can't create repository " + rEntry.getName(),
                     (Throwable)result);
               }
               else
               {
                  throw new RepositoryCreationException("createRepository command returned uknown result type.");
               }
            }
         }
         catch (RPCException e)
         {
            Throwable cause = (e).getCause();
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

         // execute startRepository at all cluster nodes (coordinator will ignore this command)
         try
         {
            List<Object> results =
               rpcService.executeCommandOnAllNodes(startRepository, true, stringRepositoryEntry, creationProps);

            for (Object result : results)
            {
               if (result != null)
               {
                  if (result instanceof Throwable)
                  {
                     throw new RepositoryCreationException("Repository " + rEntry.getName()
                        + " created on coordinator, but can not be started at other cluster nodes", ((Throwable)result));
                  }
                  else
                  {
                     throw new RepositoryCreationException("startRepository command returns uknown result type");
                  }
               }
            }
         }
         catch (RPCException e)
         {
            throw new RepositoryCreationException("Repository " + rEntry.getName()
               + " created on coordinator, can not be started at other cluster node: " + e.getMessage(), e);
         }
      }
      else
      {
         try
         {
            createRepositoryLocally(backupId, rEntry, rToken, creationProps);
         }
         finally
         {
            pendingRepositories.remove(rToken);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public String reserveRepositoryName(String repositoryName) throws RepositoryCreationException
   {
      if (rpcService != null)
      {
         // reserve RepositoryName at coordinator-node
         try
         {
            Object result = rpcService.executeCommandOnCoordinator(reserveRepositoryName, true, repositoryName);

            if (result instanceof String)
            {
               return (String)result;
            }
            else if (result instanceof Throwable)
            {
               throw new RepositoryCreationException("Can't reserve repository " + repositoryName, (Throwable)result);
            }
            else
            {
               throw new RepositoryCreationException("ReserveRepositoryName command returns unknown type result.");
            }
         }
         catch (RPCException e)
         {
            Throwable cause = (e).getCause();
            if (cause instanceof RepositoryCreationException)
            {
               throw (RepositoryCreationException)cause;
            }
            else
            {
               throw new RepositoryCreationException("Can not reserve repository name " + repositoryName + " since: "
                  + e.getMessage(), e);
            }
         }
      }
      else
      {
         return reserveRepositoryNameLocally(repositoryName);
      }
   }

   protected String reserveRepositoryNameLocally(String repositoryName) throws RepositoryCreationException
   {
      // check does repository already created
      try
      {
         if (repositoryService.getRepository(repositoryName) != null)
         {
            throw new RepositoryCreationException("Repository " + repositoryName + " already exists.");
         }
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryCreationException("Can not check does repository " + repositoryName + " exists: "
            + e.getMessage(), e);
      }
      catch (RepositoryException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
      }

      // check does this repository name already reserved, otherwise generate and return token
      if (!pendingRepositories.containsValue(repositoryName))
      {
         String rToken = repositoryName + IdGenerator.generate();
         pendingRepositories.put(rToken, repositoryName);
         return rToken;
      }
      else
      {
         throw new RepositoryCreationException("Repository name " + repositoryName + " already reserved.");
      }
   }

   protected void createRepositoryLocally(String backupId, RepositoryEntry rEntry, String rToken,
      DBCreationProperties creationProps) throws RepositoryConfigurationException, RepositoryCreationException
   {
      // check does token registered
      if (!this.pendingRepositories.containsKey(rToken))
      {
         throw new RepositoryCreationException("Token " + rToken + " does not registered.");
      }

      // Prepare list of data-source names that must be binded to newly created databases.
      Set<String> dataSourceNames = extractDataSourceNames(rEntry, true);

      // create and bind related database to each data-source name
      for (String dataSource : dataSourceNames)
      {
         // create related DB
         Map<String, String> refAddr = null;
         try
         {
            // db name will be the same as repository name if only one datasource exists
            String dbName = rEntry.getName() + (dataSourceNames.size() == 1 ? "" : "_" + dataSource);

            // get DBCreator
            DBCreator dbCreator = getDBCreator(creationProps);
            
            // create database
            DBConnectionInfo dbConnectionInfo = dbCreator.createDatabase(dbName);

            refAddr = dbConnectionInfo.getProperties();
         }
         catch (DBCreatorException e)
         {
            throw new RepositoryCreationException("Can not create new database for " + rEntry.getName()
               + " repository.", e);
         }
         catch (ConfigurationException e)
         {
            throw new RepositoryCreationException("Can not get instance of DBCreator", e);
         }

         // bind data-source
         try
         {
            initialContextInitializer.getInitialContextBinder().bind(dataSource, "javax.sql.DataSource",
               factoryClassName, null, refAddr);
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

      // restore repository from backup
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
      if (backLog != null && PrivilegedFileHelper.exists(backLog))
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
            + (backLog != null ? (" and file path=" + PrivilegedFileHelper.getAbsolutePath(backLog)) : "")
            + " do not exists.");
      }
   }

   protected void startRepository(RepositoryEntry repositoryEntry, DBCreationProperties creationProps)
      throws RepositoryCreationException
   {
      try
      {
         // Prepare list of data-source names that must be binded
         Set<String> dataSourceNames = extractDataSourceNames(repositoryEntry, false);

         for (String dataSource : dataSourceNames)
         {
            // get data base info 
            Map<String, String> refAddr = null;
            try
            {
               // db name will be the same as repository name if only one datasource exists
               String dbName = repositoryEntry.getName() + (dataSourceNames.size() == 1 ? "" : "_" + dataSource);

               // get DBCreator
               DBCreator dbCreator = getDBCreator(creationProps);

               // get connection info
               DBConnectionInfo dbConnectionInfo = dbCreator.getDBConnectionInfo(dbName);

               refAddr = dbConnectionInfo.getProperties();
            }
            catch (DBCreatorException e)
            {
               throw new RepositoryCreationException("Can not fetch database information associated with "
                  + repositoryEntry.getName() + " repository and " + dataSource + " datasource.", e);
            }
            catch (ConfigurationException e)
            {
               throw new RepositoryCreationException("Can't get instance of DBCreator", e);
            }

            // bind data-source
            try
            {
               initialContextInitializer.getInitialContextBinder().bind(dataSource, "javax.sql.DataSource",
                  factoryClassName, null, refAddr);
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

         repositoryService.createRepository(repositoryEntry);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryCreationException(e.getMessage(), e);
      }
      catch (RepositoryException e)
      {
         throw new RepositoryCreationException(e.getMessage(), e);
      }
   }

   private Set<String> extractDataSourceNames(RepositoryEntry repositoryEntry, boolean checkDataSourceExistance)
      throws RepositoryConfigurationException, RepositoryCreationException
   {
      Set<String> dataSourceNames = new HashSet<String>();
      for (WorkspaceEntry wsEntry : repositoryEntry.getWorkspaceEntries())
      {
    	 boolean isMultiDB = DatabaseStructureType.valueOf(wsEntry.getContainer().getParameterValue(
  				JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();
         String dbSourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

         if (isMultiDB && dataSourceNames.contains(dbSourceName))
         {
            throw new RepositoryCreationException("RepositoryEntry for new " + repositoryEntry.getName()
               + " repository contains workspaces that marked as multiDB but have same datasource " + dbSourceName
               + ".");
         }

         if (checkDataSourceExistance)
         {
            try
            {
               DataSource ds = (DataSource)initialContextInitializer.getInitialContext().lookup(dbSourceName);
               if (ds != null)
               {
                  throw new RepositoryConfigurationException("RepositoryEntry for new " + repositoryEntry.getName()
                     + " repository contains already binded datasource " + dbSourceName + ".");
               }
            }
            catch (NameNotFoundException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
            catch (NamingException e)
            {
               throw new RepositoryConfigurationException(e.getMessage(), e);
            }
         }

         dataSourceNames.add(dbSourceName);
      }
      return dataSourceNames;
   }

   /**
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

   /**
    * {@inheritDoc}
    */
   public void start()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
      if (this.rpcService != null)
      {
         this.rpcService.unregisterCommand(reserveRepositoryName);
         this.rpcService.unregisterCommand(createRepository);
         this.rpcService.unregisterCommand(startRepository);
         this.rpcService.unregisterCommand(removeRepository);
         this.rpcService.unregisterCommand(canRemoveRepository);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeRepository(String repositoryName, boolean forceRemove) throws RepositoryCreationException
   {
      if (rpcService != null)
      {
         try
         {
            if (!forceRemove)
            {
               List<Object> results = rpcService.executeCommandOnAllNodes(canRemoveRepository, true, repositoryName);
               for (Object result : results)
               {
                  if (result != null)
                  {
                     if (result instanceof Throwable)
                     {
                        throw new RepositoryCreationException("It is not possible to check is repository "
                           + repositoryName + " in usage or not", (Throwable)result);
                     }
                     else if (result instanceof Boolean)
                     {
                        if (!(Boolean)result)
                        {
                           throw new RepositoryCreationException("Can't remove repository " + repositoryName
                              + ". The repository in use.");
                        }
                     }
                     else
                     {
                        throw new RepositoryCreationException(
                           "checkRepositoryInUse command returned uknown result type");
                     }
                  }
               }
            }

            List<Object> results = rpcService.executeCommandOnAllNodes(removeRepository, true, repositoryName);
            for (Object result : results)
            {
               if (result != null)
               {
                  if (result instanceof Throwable)
                  {
                     throw new RepositoryCreationException("Can't remove repository " + repositoryName,
                        (Throwable)result);
                  }
                  else
                  {
                     throw new RepositoryCreationException("removeRepository command returned uknown result type");
                  }
               }
            }
         }
         catch (RPCException e)
         {
            throw new RepositoryCreationException("Can't remove repository " + repositoryName, e);
         }
      }
      else
      {
         if (!forceRemove)
         {
            try
            {
               if (!repositoryService.canRemoveRepository(repositoryName))
               {
                  throw new RepositoryCreationException("Can't remove repository " + repositoryName
                     + ". The repository in use.");
               }
            }
            catch (RepositoryException e)
            {
               throw new RepositoryCreationException("It is not possible to check is repository " + repositoryName
                  + " in usage or not", e);
            }
         }

         removeRepositoryLocally(repositoryName);
      }
   }

   /**
    * Remove repository locally.
    * 
    * @param repositoryName
    *          the repository name
    * @throws RepositoryCreationException
    */
   protected void removeRepositoryLocally(String repositoryName) throws RepositoryCreationException
   {
      try
      {
         // extract list of all datasources
         ManageableRepository repositorty = repositoryService.getRepository(repositoryName);
         Set<String> datasources = extractDataSourceNames(repositorty.getConfiguration(), false);

         // close all opened sessions
         for (String workspaceName : repositorty.getWorkspaceNames())
         {
            WorkspaceContainerFacade wc = repositorty.getWorkspaceContainer(workspaceName);
            SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);

            sessionRegistry.closeSessions(workspaceName);
         }

         // remove repository from configuration
         repositoryService.removeRepository(repositoryName);
         repositoryService.getConfig().retain();
         
         // unbind datasource and close connections
         for (String dsName : datasources)
         {
            try
            {
               // we suppose that lookup() method returns the same instance of datasource by the same name
               DataSource ds = (DataSource)initialContextInitializer.getInitialContext().lookup(dsName);
               initialContextInitializer.getInitialContextBinder().unbind(dsName);

               // close datasource
               if (ds instanceof CloseableDataSource)
               {
                  ((CloseableDataSource)ds).close();
               }
            }
            catch (NamingException e)
            {
               LOG.error("Can't unbind datasource " + dsName, e);
            }
            catch (FileNotFoundException e)
            {
               LOG.error("Can't unbind datasource " + dsName, e);
            }
            catch (XMLStreamException e)
            {
               LOG.error("Can't unbind datasource " + dsName, e);
            }
         }
      }
      catch (RepositoryException e)
      {
         throw new RepositoryCreationException("Can't remove repository", e);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryCreationException("Can't remove repository", e);
      }
   }

   private DBCreator getDBCreator(DBCreationProperties creationProps) throws ConfigurationException
   {
      if (creationProps == null)
      {
         return (DBCreator)context.getContainer().getComponentInstanceOfType(DBCreator.class);
      }

      ConfigurationManager cm =
         (ConfigurationManager)context.getContainer().getComponentInstanceOfType(ConfigurationManager.class);

      return new DBCreator(creationProps.getServerUrl(), creationProps.getConnProps(), creationProps.getDBScriptPath(),
         creationProps.getDBUserName(), creationProps.getDBPassword(), cm);
   }
}
