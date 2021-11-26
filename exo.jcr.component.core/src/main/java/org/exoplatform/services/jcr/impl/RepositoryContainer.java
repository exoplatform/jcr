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

package org.exoplatform.services.jcr.impl;

import org.exoplatform.commons.utils.ClassLoading;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.NamingContext;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.config.ExtendedMappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SystemParametersPersistenceConfigurator;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.checker.RepositoryCheckController;
import org.exoplatform.services.jcr.impl.core.AddNamespacePluginHolder;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceDataPersister;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.ScratchWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.SessionFactory;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.core.WorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.access.DefaultAccessManagerImpl;
import org.exoplatform.services.jcr.impl.core.lock.LockRemoverHolder;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDataManagerImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeManagerImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.registration.JCRNodeTypeDataPersister;
import org.exoplatform.services.jcr.impl.core.observation.ObservationManagerRegistry;
import org.exoplatform.services.jcr.impl.core.query.QueryManagerFactory;
import org.exoplatform.services.jcr.impl.core.query.RepositoryIndexSearcherHolder;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManagerHolder;
import org.exoplatform.services.jcr.impl.core.value.ValueFactoryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.LocalWorkspaceDataManagerStub;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;
import org.exoplatform.services.jcr.impl.quota.QuotaManager;
import org.exoplatform.services.jcr.impl.quota.RepositoryQuotaManager;
import org.exoplatform.services.jcr.impl.quota.WorkspaceQuotaManager;
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.value.StandaloneStoragePluginProvider;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCService;

import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.QueryManager;
import javax.naming.NameNotFoundException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: RepositoryContainer.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */
@Managed
@NameTemplate(@Property(key = "repository", value = "{Name}"))
@NamingContext(@Property(key = "repository", value = "{Name}"))
public class RepositoryContainer extends ExoContainer
{

   /**
    * The serial UID
    */
   private static final long serialVersionUID = -8441933562276408877L;

   /**
    * Repository config.
    */
   private final RepositoryEntry config;

   /**
    * Repository name.
    */
   private final String name;

   /**
    * System workspace DataManager.
    */
   private LocalWorkspaceDataManagerStub systemDataManager = null;

   /**
    * Logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.RepositoryContainer");

   /**
    * List of AddNamespacePlugin.
    */
   private List<ComponentPlugin> addNamespacePlugins;

   /**
    * RepositoryContainer constructor.
    * 
    * @param parent
    *          container
    * @param config
    *          Repository configuration
    * @param addNamespacePlugins
    *          list of addNamespacePlugin
    * @throws RepositoryException
    *           container initialization error
    * @throws RepositoryConfigurationException
    *           configuration error
    */
   public RepositoryContainer(final ExoContainer parent, RepositoryEntry config, List<ComponentPlugin> addNamespacePlugins)
      throws RepositoryException, RepositoryConfigurationException
   {

      super(parent);

      // Defaults:
      if (config.getAccessControl() == null)
         config.setAccessControl(AccessControlPolicy.OPTIONAL);

      this.config = config;
      this.addNamespacePlugins = addNamespacePlugins;
      this.name = config.getName();

      try
      {
         SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws RepositoryConfigurationException
            {
               context.setName(parent.getContext().getName() + "-" + name);
               try
               {
                  parent.registerComponentInstance(name, RepositoryContainer.this);
                  initAllWorkspaceComponentEntries(parent);
                  registerComponents();
               }
               catch (Throwable t) // NOSONAR
               {
                  unregisterAllComponents();
                  parent.unregisterComponent(name);
                  throw new RepositoryConfigurationException("Can not register repository container " + name
                     + " in parent container.", t);
               }
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable ex = e.getCause();
         if (ex instanceof RepositoryConfigurationException)
         {
            throw (RepositoryConfigurationException)ex;
         }
         else
         {
            throw new RepositoryConfigurationException(ex.getMessage(), ex);
         }
      }
   }

   private void initAllWorkspaceComponentEntries(ExoContainer parent)
   {
      SystemParametersPersistenceConfigurator sppc =
         (SystemParametersPersistenceConfigurator)parent
            .getComponentInstanceOfType(SystemParametersPersistenceConfigurator.class);

      for (WorkspaceEntry workspaceEntry : config.getWorkspaceEntries())
      {
         initWorkspaceComponentEntries(sppc, workspaceEntry);
      }
   }

   public void initWorkspaceComponentEntries(SystemParametersPersistenceConfigurator sppc, WorkspaceEntry workspaceEntry)
   {
      workspaceEntry.setUniqueName(getName() + "_" + workspaceEntry.getName());
      if (sppc != null)
      {
         for (ExtendedMappedParametrizedObjectEntry entry : getWorkspaceComponentEntries(workspaceEntry))
         {
            entry.initSystemParameterUpdater(workspaceEntry, sppc);
         }
      }
   }

   private List<ExtendedMappedParametrizedObjectEntry> getWorkspaceComponentEntries(WorkspaceEntry workspaceEntry)
   {
      List<ExtendedMappedParametrizedObjectEntry> entries = new ArrayList<ExtendedMappedParametrizedObjectEntry>();
      if (workspaceEntry.getAccessManager() != null)
      {
         entries.add(workspaceEntry.getAccessManager());
      }

      if (workspaceEntry.getCache() != null)
      {
         entries.add(workspaceEntry.getCache());
      }

      if (workspaceEntry.getInitializer() != null)
      {
         entries.add(workspaceEntry.getInitializer());
      }

      if (workspaceEntry.getLockManager() != null)
      {
         entries.add(workspaceEntry.getLockManager());
      }

      if (workspaceEntry.getQueryHandler() != null)
      {
         entries.add(workspaceEntry.getQueryHandler());
      }

      if (workspaceEntry.getContainer() != null)
      {
         entries.add(workspaceEntry.getContainer());
         if (workspaceEntry.getContainer().getValueStorages() != null)
         {
            for (ValueStorageEntry valueStorageEntry : workspaceEntry.getContainer().getValueStorages())
            {
               entries.add(valueStorageEntry);
            }
         }
      }

      return entries;
   }

   /**
    * RepositoryContainer constructor.
    * 
    * @param parent
    *          container
    * @param config
    *          Repository configuration
    * @throws RepositoryException
    *           container initialization error
    * @throws RepositoryConfigurationException
    *           configuration error
    */
   public RepositoryContainer(final ExoContainer parent, RepositoryEntry config) throws RepositoryException,
      RepositoryConfigurationException
   {
      this(parent, config, new ArrayList<ComponentPlugin>());
   }

   public LocationFactory getLocationFactory()
   {
      return (LocationFactory)getComponentInstanceOfType(LocationFactory.class);
   }

   /**
    * @return Returns the name.
    */
   @Managed
   @ManagedDescription("The repository container name")
   public String getName()
   {
      return name;
   }

   public NamespaceRegistry getNamespaceRegistry()
   {
      return (NamespaceRegistry)getComponentInstanceOfType(NamespaceRegistry.class);
   }

   public ExtendedNodeTypeManager getNodeTypeManager()
   {
      return (ExtendedNodeTypeManager)getComponentInstanceOfType(NodeTypeManager.class);
   }

   /**
    * Get workspace Container by name.
    * 
    * @param workspaceName
    *          name
    * @return WorkspaceContainer
    */
   public WorkspaceContainer getWorkspaceContainer(String workspaceName)
   {
      Object comp = getComponentInstance(workspaceName);
      return comp != null && comp instanceof WorkspaceContainer ? (WorkspaceContainer)comp : null;
   }

   /**
    * Get workspace configuration entry by name.
    * 
    * @param wsName
    *          workspace name
    * @return WorkspaceEntry
    */
   public WorkspaceEntry getWorkspaceEntry(String wsName)
   {
      for (WorkspaceEntry entry : config.getWorkspaceEntries())
      {
         if (entry.getName().equals(wsName))
            return entry;
      }
      return null;
   }

   /**
    * Register workspace from configuration.
    * 
    * @param wsConfig
    *          configuration
    * @throws RepositoryException
    *           initialization error
    * @throws RepositoryConfigurationException
    *           configuration error
    */
   public void registerWorkspace(final WorkspaceEntry wsConfig) throws RepositoryException,
      RepositoryConfigurationException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      try
      {
         SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws RepositoryException, RepositoryConfigurationException
            {
               final boolean isSystem = config.getSystemWorkspaceName().equals(wsConfig.getName());

               if (getWorkspaceContainer(wsConfig.getName()) != null)
                  throw new RepositoryException("Workspace " + wsConfig.getName() + " already registered");

               final WorkspaceContainer workspaceContainer = new WorkspaceContainer(RepositoryContainer.this, wsConfig);
               registerComponentInstance(wsConfig.getName(), workspaceContainer);

               workspaceContainer.registerComponentInstance(wsConfig);

               workspaceContainer.registerComponentImplementation(StandaloneStoragePluginProvider.class);
               try
               {
                  final Class<?> containerType =
                     ClassLoading.forName(wsConfig.getContainer().getType(), RepositoryContainer.class);
                  workspaceContainer.registerComponentImplementation(containerType);
                  if (isSystem)
                  {
                     registerComponentInstance(new SystemDataContainerHolder((WorkspaceDataContainer)workspaceContainer
                        .getComponentInstanceOfType(WorkspaceDataContainer.class)));
                  }
               }
               catch (ClassNotFoundException e)
               {
                  throw new RepositoryConfigurationException("Class not found for workspace data container "
                     + wsConfig.getUniqueName() + " : " + e, e);
               }

               // cache type
               try
               {
                  String className = wsConfig.getCache().getType();
                  if (className != null && className.length() > 0)
                  {
                     workspaceContainer.registerComponentImplementation(ClassLoading.forName(className,
                        RepositoryContainer.class));
                  }
                  else
                     workspaceContainer.registerComponentImplementation(LinkedWorkspaceStorageCacheImpl.class);
               }
               catch (ClassNotFoundException e)
               {
                  log.warn("Workspace cache class not found " + wsConfig.getCache().getType()
                     + ", will use default. Error : " + e.getMessage());
                  workspaceContainer.registerComponentImplementation(LinkedWorkspaceStorageCacheImpl.class);
               }

               if (workspaceContainer.getComponentInstanceOfType(RPCService.class) != null)
               {
                  workspaceContainer.registerComponentImplementation(WorkspaceResumer.class);
               }

               if (workspaceContainer.getComponentInstanceOfType(QuotaManager.class) != null)
               {
                  workspaceContainer.registerComponentImplementation(WorkspaceQuotaManager.class);
               }

               workspaceContainer.registerComponentImplementation(CacheableWorkspaceDataManager.class);
               workspaceContainer.registerComponentImplementation(LocalWorkspaceDataManagerStub.class);
               workspaceContainer.registerComponentImplementation(ObservationManagerRegistry.class);

               if (wsConfig.getLockManager() != null && wsConfig.getLockManager().getType() != null)
               {
                  try
                  {
                     final Class<?> lockManagerType =
                        ClassLoading.forName(wsConfig.getLockManager().getType(), RepositoryContainer.class);
                     workspaceContainer.registerComponentImplementation(lockManagerType);
                  }
                  catch (ClassNotFoundException e)
                  {
                     throw new RepositoryConfigurationException("Class not found for workspace lock manager "
                        + wsConfig.getLockManager().getType() + ", container " + wsConfig.getUniqueName() + " : " + e,
                        e);
                  }
               }
               else
               {
                  throw new RepositoryConfigurationException(
                     "The configuration of lock manager is expected in container " + wsConfig.getUniqueName());
               }

               // Query handler
               if (wsConfig.getQueryHandler() != null)
               {
                  workspaceContainer.registerComponentImplementation(SearchManager.class);
                  workspaceContainer.registerComponentImplementation(QueryManager.class);
                  workspaceContainer.registerComponentImplementation(QueryManagerFactory.class);
                  workspaceContainer.registerComponentInstance(wsConfig.getQueryHandler());
                  if (isSystem)
                  {
                     workspaceContainer.registerComponentImplementation(SystemSearchManager.class);
                  }
               }

               // access manager
               if (wsConfig.getAccessManager() != null && wsConfig.getAccessManager().getType() != null)
               {
                  try
                  {
                     final Class<?> am = ClassLoading.forName(wsConfig.getAccessManager().getType(), RepositoryContainer.class);
                     workspaceContainer.registerComponentImplementation(am);
                  }
                  catch (ClassNotFoundException e)
                  {
                     throw new RepositoryConfigurationException(
                        "Class not found for workspace access manager " + wsConfig.getAccessManager().getType()
                           + ", container " + wsConfig.getUniqueName() + " : " + e, e);
                  }
               }

               // initializer
               final Class<?> initilizerType;
               if (wsConfig.getInitializer() != null && wsConfig.getInitializer().getType() != null)
               {
                  // use user defined
                  try
                  {
                     initilizerType = ClassLoading.forName(wsConfig.getInitializer().getType(), RepositoryContainer.class);
                  }
                  catch (ClassNotFoundException e)
                  {
                     throw new RepositoryConfigurationException("Class not found for workspace initializer "
                        + wsConfig.getInitializer().getType() + ", container " + wsConfig.getUniqueName() + " : " + e,
                        e);
                  }
               }
               else
               {
                  // use default
                  initilizerType = ScratchWorkspaceInitializer.class;
               }
               workspaceContainer.registerComponentImplementation(initilizerType);
               workspaceContainer.registerComponentImplementation(TransactionableResourceManager.class);
               workspaceContainer.registerComponentImplementation(SessionFactory.class);
               final LocalWorkspaceDataManagerStub wsDataManager =
                  (LocalWorkspaceDataManagerStub)workspaceContainer
                     .getComponentInstanceOfType(LocalWorkspaceDataManagerStub.class);

               if (isSystem)
               {
                  // system workspace
                  systemDataManager = wsDataManager;
                  registerComponentInstance(systemDataManager);
               }

               wsDataManager.setSystemDataManager(systemDataManager);

               if (!config.getWorkspaceEntries().contains(wsConfig))
                  config.getWorkspaceEntries().add(wsConfig);
               return null;
            }
         });

      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof RepositoryConfigurationException)
         {
            throw (RepositoryConfigurationException)cause;
         }
         else if (cause instanceof RepositoryException)
         {
            throw (RepositoryException)cause;
         }
         else if (cause instanceof RuntimeException)
         {
            RuntimeException e = (RuntimeException)cause;
            int depth = 0;
            Throwable retval = e;
            while (retval.getCause() != null && depth < 100)
            {
               retval = retval.getCause();
               if (retval instanceof RepositoryException)
               {
                  throw new RepositoryException(retval.getMessage(), e);
               }
               else if (retval instanceof RepositoryConfigurationException)
               {
                  throw new RepositoryConfigurationException(retval.getMessage(), e);
               }
               else if (retval instanceof NameNotFoundException)
               {
                  throw new RepositoryException(retval.getMessage(), e);
               }
               depth++;
            }
            throw e;            
         }
         else
         {
            throw new RepositoryException(cause);
         }
      }      
   }

   // Components access methods -------

   /**
    * {@inheritDoc}
    */
   @Override
   public void start()
   {

      try
      {
         init();
         load();
      }
      catch (RepositoryException e)
      {
         log.error("Repository error", e);
         throw new RuntimeException(e);
      }
      catch (RepositoryConfigurationException e)
      {
         log.error("Configuration error", e);
         throw new RuntimeException(e);
      }

      super.start();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public synchronized void stop()
   {
      RepositoryImpl repository = (RepositoryImpl)getComponentInstanceOfType(RepositoryImpl.class);

      try
      {
         repository.setState(ManageableRepository.OFFLINE);
      }
      catch (RepositoryException e)
      {
         log.error("Can not switch repository to OFFLINE", e);
      }

      super.stop();
      super.unregisterAllComponents();
   }

   /**
    * Initialize worspaces (root node and jcr:system for system workspace).
    * <p>
    * Runs on container start.
    * 
    * @throws RepositoryException
    * @throws RepositoryConfigurationException
    */
   private void init() throws RepositoryException, RepositoryConfigurationException
   {
      List<WorkspaceEntry> wsEntries = config.getWorkspaceEntries();

      NodeTypeDataManager typeManager = (NodeTypeDataManager)this.getComponentInstanceOfType(NodeTypeDataManager.class);
      NamespaceRegistryImpl namespaceRegistry =
         (NamespaceRegistryImpl)this.getComponentInstanceOfType(NamespaceRegistry.class);

      for (WorkspaceEntry ws : wsEntries)
      {
         initWorkspace(ws);
         WorkspaceContainer workspaceContainer = getWorkspaceContainer(ws.getName());
         SearchManager searchManager =
            (SearchManager)workspaceContainer.getComponentInstanceOfType(SearchManager.class);
         //         if (searchManager != null)
         //         {
         //            typeManager.addQueryHandler(searchManager.getHandler());
         //            namespaceRegistry.addQueryHandler(searchManager.getHandler());
         //         }
         //         else
         //         {
         //            log.warn("Search manager not configured for " + ws.getName());
         //         }
      }

      SystemSearchManagerHolder searchManager =
         (SystemSearchManagerHolder)this.getComponentInstanceOfType(SystemSearchManagerHolder.class);
      //      if (searchManager != null)
      //      {
      //         typeManager.addQueryHandler(searchManager.get().getHandler());
      //         namespaceRegistry.addQueryHandler(searchManager.get().getHandler());
      //      }
      //      else
      //      {
      //         log.warn("System search manager not configured ");
      //      }

   }

   /**
    * Init workspace root node. If it's the system workspace init jcr:system too.
    * 
    * @param wsConfig
    * @throws RepositoryException
    */
   private void initWorkspace(WorkspaceEntry wsConfig) throws RepositoryException
   {

      WorkspaceContainer workspaceContainer = getWorkspaceContainer(wsConfig.getName());

      // touch independent components
      workspaceContainer.getComponentInstanceOfType(IdGenerator.class);

      // Init Root and jcr:system if workspace is system workspace
      WorkspaceInitializer wsInitializer =
         (WorkspaceInitializer)workspaceContainer.getComponentInstanceOfType(WorkspaceInitializer.class);
      RepositoryCreationSynchronizer synchronizer =
         (RepositoryCreationSynchronizer)getComponentInstanceOfType(RepositoryCreationSynchronizer.class);
      // The synchronizer will be used to synchronize all the cluster
      // nodes to prevent any concurrent jcr initialization i.e. EXOJCR-887
      synchronizer.waitForApproval(wsInitializer.isWorkspaceInitialized());

      SystemParametersPersistenceConfigurator sppc =
         (SystemParametersPersistenceConfigurator)parent
            .getComponentInstanceOfType(SystemParametersPersistenceConfigurator.class);

      if (sppc != null)
      {
         setInitializerAndValidateOverriddenParameters(wsConfig, wsInitializer);
      }

      wsInitializer.initWorkspace();
   }

   public void setInitializerAndValidateOverriddenParameters(WorkspaceEntry workspaceEntry,
      WorkspaceInitializer workspaceInitializer)
   {
      setInitializerForWorkspaceComponents(workspaceEntry, workspaceInitializer);
      validateOverriddenParametersOfWorkspaceComponents(workspaceEntry);
   }

   private void setInitializerForWorkspaceComponents(WorkspaceEntry workspaceEntry,
      WorkspaceInitializer workspaceInitializer)
   {
      for (ExtendedMappedParametrizedObjectEntry entry : getWorkspaceComponentEntries(workspaceEntry))
      {
         entry.getSystemParameterUpdater().setWorkspaceInitializer(workspaceInitializer);
      }
   }

   private void validateOverriddenParametersOfWorkspaceComponents(WorkspaceEntry workspaceEntry)
   {
      for (ExtendedMappedParametrizedObjectEntry entry : getWorkspaceComponentEntries(workspaceEntry))
      {
         entry.getSystemParameterUpdater().validateOverriddenParameters();
      }
   }

   // ////// initialize --------------

   private void registerComponents() throws RepositoryConfigurationException, RepositoryException
   {
      SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            registerComponentInstance(config);
            registerComponentImplementation(FileCleanerHolder.class);
            registerComponentImplementation(LockRemoverHolder.class);
            return null;
         }
      });
      registerWorkspacesComponents();
      registerRepositoryComponents();
   }

   private void registerRepositoryComponents() throws RepositoryConfigurationException, RepositoryException
   {
      try
      {
         SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws RepositoryConfigurationException, RepositoryException
            {
               if (getComponentInstanceOfType(QuotaManager.class) != null)
               {
                  registerComponentImplementation(RepositoryQuotaManager.class);
               }

               registerComponentImplementation(RepositorySuspendController.class);
               registerComponentImplementation(RepositoryCheckController.class);
               registerComponentImplementation(IdGenerator.class);

               registerComponentImplementation(RepositoryIndexSearcherHolder.class);

               registerComponentImplementation(LocationFactory.class);
               registerComponentImplementation(ValueFactoryImpl.class);

               registerComponentInstance(new AddNamespacePluginHolder(addNamespacePlugins));

               registerComponentImplementation(JCRNodeTypeDataPersister.class);
               registerComponentImplementation(NamespaceDataPersister.class);
               registerComponentImplementation(NamespaceRegistryImpl.class);

               registerComponentImplementation(NodeTypeManagerImpl.class);
               registerComponentImplementation(NodeTypeDataManagerImpl.class);

               registerComponentImplementation(DefaultAccessManagerImpl.class);

               registerComponentImplementation(SessionRegistry.class);

               String systemWsname = config.getSystemWorkspaceName();
               WorkspaceEntry systemWsEntry = getWorkspaceEntry(systemWsname);

               if (systemWsEntry != null && systemWsEntry.getQueryHandler() != null)
               {
                  SystemSearchManager systemSearchManager =
                     (SystemSearchManager)getWorkspaceContainer(systemWsname).getComponentInstanceOfType(
                        SystemSearchManager.class);
                  registerComponentInstance(new SystemSearchManagerHolder(systemSearchManager));
               }
               try
               {
                  final Class<?> authenticationPolicyClass =
                     ClassLoading.forName(config.getAuthenticationPolicy(), RepositoryContainer.class);
                  registerComponentImplementation(authenticationPolicyClass);
               }
               catch (ClassNotFoundException e)
               {
                  throw new RepositoryConfigurationException("Class not found for repository authentication policy: "
                     + e, e);
               }

               // Repository
               final RepositoryImpl repository = new RepositoryImpl(RepositoryContainer.this);
               registerComponentInstance(repository);
               return null;
            }
         });
      }
      catch (PrivilegedActionException pae)
      {
         Throwable cause = pae.getCause();
         if (cause instanceof RepositoryConfigurationException)
         {
            throw (RepositoryConfigurationException)cause;
         }
         else if (cause instanceof RepositoryException)
         {
            throw (RepositoryException)cause;
         }
         else
         {
            throw new RepositoryException(cause);
         }
      }
   }

   private void registerWorkspacesComponents() throws RepositoryException, RepositoryConfigurationException
   {
      // System workspace should be first initialized.
      for (WorkspaceEntry we : config.getWorkspaceEntries())
      {
         if (we.getName().equals(config.getSystemWorkspaceName()))
         {
            registerWorkspace(we);
         }
      }

      // Initialize other (non system) workspaces.
      for (WorkspaceEntry we : config.getWorkspaceEntries())
      {
         if (!we.getName().equals(config.getSystemWorkspaceName()))
         {
            registerWorkspace(we);
         }
      }
   }

   /**
    * Load namespaces and nodetypes from persistent repository.
    * 
    * <p>
    * Runs on container start.
    * 
    * @throws RepositoryException
    */
   private void load() throws RepositoryException
   {
      //Namespaces first
      NamespaceDataPersister namespacePersister =
         (NamespaceDataPersister)this.getComponentInstanceOfType(NamespaceDataPersister.class);
      NamespaceRegistryImpl nsRegistry = (NamespaceRegistryImpl)getNamespaceRegistry();

      namespacePersister.start();
      nsRegistry.start();

      //Node types now.
      JCRNodeTypeDataPersister nodeTypePersister =
         (JCRNodeTypeDataPersister)this.getComponentInstanceOfType(JCRNodeTypeDataPersister.class);

      NodeTypeDataManagerImpl ntManager =
         (NodeTypeDataManagerImpl)this.getComponentInstanceOfType(NodeTypeDataManagerImpl.class);

      nodeTypePersister.start();
      ntManager.start();

   }

}
