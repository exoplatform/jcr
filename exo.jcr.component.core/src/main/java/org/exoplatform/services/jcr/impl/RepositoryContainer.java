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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.jmx.MX4JComponentAdapterFactory;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.NamingContext;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.access.AccessControlPolicy;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.NamespaceDataPersister;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.ScratchWorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.SessionFactory;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.core.WorkspaceInitializer;
import org.exoplatform.services.jcr.impl.core.access.DefaultAccessManagerImpl;
import org.exoplatform.services.jcr.impl.core.lock.LockManagerImpl;
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
import org.exoplatform.services.jcr.impl.storage.SystemDataContainerHolder;
import org.exoplatform.services.jcr.impl.storage.value.StandaloneStoragePluginProvider;
import org.exoplatform.services.jcr.impl.util.io.WorkspaceFileCleanerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.Collections;
import java.util.Comparator;
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
@NameTemplate({@Property(key = "container", value = "repository"), @Property(key = "name", value = "{Name}")})
@NamingContext(@Property(key = "repository", value = "{Name}"))
public class RepositoryContainer extends ExoContainer
{

   /**
    * Repository config.
    */
   private final RepositoryEntry config;

   /**
    * System workspace DataManager.
    */
   private LocalWorkspaceDataManagerStub systemDataManager = null;

   /**
    * Logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.RepositoryContainer");

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
   public RepositoryContainer(ExoContainer parent, RepositoryEntry config) throws RepositoryException,
      RepositoryConfigurationException
   {

      super(new MX4JComponentAdapterFactory(), parent);

      // Defaults:
      if (config.getAccessControl() == null)
         config.setAccessControl(AccessControlPolicy.OPTIONAL);

      this.config = config;

      registerComponents();
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
      return config.getName();
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

      try
      {
         final boolean isSystem = config.getSystemWorkspaceName().equals(wsConfig.getName());

         if (getWorkspaceContainer(wsConfig.getName()) != null)
            throw new RepositoryException("Workspace " + wsConfig.getName() + " already registred");

         WorkspaceContainer workspaceContainer = new WorkspaceContainer(this, wsConfig);

         registerComponentInstance(wsConfig.getName(), workspaceContainer);

         wsConfig.setUniqueName(getName() + "_" + wsConfig.getName());

         workspaceContainer.registerComponentInstance(wsConfig);

         workspaceContainer.registerComponentImplementation(StandaloneStoragePluginProvider.class);

         try
         {
            Class<?> containerType = Class.forName(wsConfig.getContainer().getType());
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
               + wsConfig.getUniqueName() + " : " + e);
         }

         // cache type
         try
         {
            String className = wsConfig.getCache().getType();
            if (className != null && className.length() > 0)
            {
               workspaceContainer.registerComponentImplementation(Class.forName(className));
            }
            else
               workspaceContainer.registerComponentImplementation(LinkedWorkspaceStorageCacheImpl.class);
         }
         catch (ClassNotFoundException e)
         {
            log.warn("Workspace cache class not found " + wsConfig.getCache().getType()
               + ", will use default. Error : " + e);
            workspaceContainer.registerComponentImplementation(LinkedWorkspaceStorageCacheImpl.class);
         }

         workspaceContainer.registerComponentImplementation(CacheableWorkspaceDataManager.class);
         workspaceContainer.registerComponentImplementation(LocalWorkspaceDataManagerStub.class);
         workspaceContainer.registerComponentImplementation(ObservationManagerRegistry.class);

         // Lock manager and Lock persister is a optional parameters
         if (wsConfig.getLockManager() != null && wsConfig.getLockManager().getPersister() != null)
         {
            try
            {
               Class<?> lockPersister = Class.forName(wsConfig.getLockManager().getPersister().getType());
               workspaceContainer.registerComponentImplementation(lockPersister);
            }
            catch (ClassNotFoundException e)
            {
               throw new RepositoryConfigurationException("Class not found for workspace lock persister "
                  + wsConfig.getLockManager().getPersister().getType() + ", container " + wsConfig.getUniqueName()
                  + " : " + e);
            }
         }

         if (wsConfig.getLockManager() != null && wsConfig.getLockManager().getType() != null)
         {
            try
            {
               Class<?> lockManagerType = Class.forName(wsConfig.getLockManager().getType());
               workspaceContainer.registerComponentImplementation(lockManagerType);
            }
            catch (ClassNotFoundException e)
            {
               throw new RepositoryConfigurationException("Class not found for workspace lock manager "
                  + wsConfig.getLockManager().getType() + ", container " + wsConfig.getUniqueName() + " : " + e);
            }
         }
         else
         {
            workspaceContainer.registerComponentImplementation(LockManagerImpl.class);
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
               Class<?> am = Class.forName(wsConfig.getAccessManager().getType());
               workspaceContainer.registerComponentImplementation(am);
            }
            catch (ClassNotFoundException e)
            {
               throw new RepositoryConfigurationException("Class not found for workspace access manager "
                  + wsConfig.getAccessManager().getType() + ", container " + wsConfig.getUniqueName() + " : " + e);
            }
         }

         // initializer
         Class<?> initilizerType;
         if (wsConfig.getInitializer() != null && wsConfig.getInitializer().getType() != null)
         {
            // use user defined
            try
            {
               initilizerType = Class.forName(wsConfig.getInitializer().getType());
            }
            catch (ClassNotFoundException e)
            {
               throw new RepositoryConfigurationException("Class not found for workspace initializer "
                  + wsConfig.getInitializer().getType() + ", container " + wsConfig.getUniqueName() + " : " + e);
            }
         }
         else
         {
            // use default
            initilizerType = ScratchWorkspaceInitializer.class;
         }
         workspaceContainer.registerComponentImplementation(initilizerType);
         workspaceContainer.registerComponentImplementation(SessionFactory.class);
         workspaceContainer.registerComponentImplementation(WorkspaceFileCleanerHolder.class);

         LocalWorkspaceDataManagerStub wsDataManager =
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

      }
      catch (RuntimeException e)
      {
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
   public void stop()
   {
      try
      {
         stopContainer();
      }
      catch (Exception e)
      {
         log.error(e.getLocalizedMessage(), e);
      }
      super.stop();
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
      wsInitializer.initWorkspace();
   }

   // ////// initialize --------------

   private void registerComponents() throws RepositoryConfigurationException, RepositoryException
   {

      registerComponentInstance(config);

      registerWorkspacesComponents();
      registerRepositoryComponents();
   }

   private void registerRepositoryComponents() throws RepositoryConfigurationException, RepositoryException
   {

      registerComponentImplementation(IdGenerator.class);

      registerComponentImplementation(RepositoryIndexSearcherHolder.class);

      registerComponentImplementation(WorkspaceFileCleanerHolder.class);
      registerComponentImplementation(LocationFactory.class);
      registerComponentImplementation(ValueFactoryImpl.class);

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
         registerComponentImplementation(Class.forName(config.getAuthenticationPolicy()));
      }
      catch (ClassNotFoundException e)
      {
         throw new RepositoryConfigurationException("Class not found for repository authentication policy: " + e);
      }

      // Repository
      RepositoryImpl repository = new RepositoryImpl(this);
      registerComponentInstance(repository);

   }

   private void registerWorkspacesComponents() throws RepositoryException, RepositoryConfigurationException
   {
      List<WorkspaceEntry> wsEntries = config.getWorkspaceEntries();
      Collections.sort(wsEntries, new WorkspaceOrderComparator(config.getSystemWorkspaceName()));
      for (int i = 0; i < wsEntries.size(); i++)
      {
         registerWorkspace(wsEntries.get(i));
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

   /**
    * Workspaces order comparator.
    * 
    */
   private static class WorkspaceOrderComparator implements Comparator<WorkspaceEntry>
   {
      private final String sysWs;

      private WorkspaceOrderComparator(String sysWs)
      {
         this.sysWs = sysWs;
      }

      public int compare(WorkspaceEntry o1, WorkspaceEntry o2)
      {
         String n1 = o1.getName();
         return n1.equals(sysWs) ? -1 : 0;
      }
   }
}
