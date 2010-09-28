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
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: RepositoryServiceImpl.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */

public class RepositoryServiceImpl implements RepositoryService, Startable
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.RepositoryServiceImpl");

   private final RepositoryServiceConfiguration config;

   private final ThreadLocal<String> currentRepositoryName = new ThreadLocal<String>();

   private final HashMap<String, RepositoryContainer> repositoryContainers = new HashMap<String, RepositoryContainer>();

   private final List<ComponentPlugin> addNodeTypePlugins;

   private final List<ComponentPlugin> addNamespacesPlugins;

   private final ManagerStartChanges managerStartChanges;

   private final ExoContainerContext containerContext;

   private ExoContainer parentContainer;

   public RepositoryServiceImpl(RepositoryServiceConfiguration configuration)
   {
      this(configuration, null);
   }

   public RepositoryServiceImpl(RepositoryServiceConfiguration configuration, ExoContainerContext context)
   {
      this.config = configuration;
      addNodeTypePlugins = new ArrayList<ComponentPlugin>();
      addNamespacesPlugins = new ArrayList<ComponentPlugin>();
      containerContext = context;
      currentRepositoryName.set(config.getDefaultRepositoryName());
      managerStartChanges = new ManagerStartChanges();
   }

   public void addPlugin(ComponentPlugin plugin)
   {
      if (plugin instanceof AddNodeTypePlugin)
         addNodeTypePlugins.add(plugin);
      else if (plugin instanceof AddNamespacesPlugin)
         addNamespacesPlugins.add(plugin);
      else if (plugin instanceof RepositoryChangesListenerRegisterPlugin)
      {
         managerStartChanges.addPlugin((RepositoryChangesListenerRegisterPlugin)plugin);
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean canRemoveRepository(String name) throws RepositoryException
   {
      return canRemoveRepository(name, false);
   }

   /**
    * Create repository. <br>
    * Init worksapces for initial start or them load from persistence. <br>
    * Add namespaces and nodetypes from service plugins.
    * 
    */
   public synchronized void createRepository(RepositoryEntry rEntry) throws RepositoryConfigurationException,
      RepositoryException
   {
      if (repositoryContainers.containsKey(rEntry.getName()))
      {
         throw new RepositoryConfigurationException("Repository container " + rEntry.getName() + " already started");
      }

      RepositoryContainer repositoryContainer = new RepositoryContainer(parentContainer, rEntry, addNamespacesPlugins);

      // Storing and starting the repository container under
      // key=repository_name
      try
      {
         repositoryContainers.put(rEntry.getName(), repositoryContainer);
         managerStartChanges.registerListeners(repositoryContainer);

         repositoryContainer.start();
      }
      catch (Throwable t)
      {
         //TODO will be implemented unregistration in managerStartChanges
         //managerStartChanges.removeListeners(repositoryContainer);
         repositoryContainers.remove(rEntry.getName());

         throw new RepositoryConfigurationException("Repository conatainer " + rEntry.getName() + " was not started.",
            t);
      }

      if (!config.getRepositoryConfigurations().contains(rEntry))
      {
         config.getRepositoryConfigurations().add(rEntry);
      }

      registerNodeTypes(rEntry.getName());

      // turn on Repository ONLINE
      ManageableRepository mr =
         (ManageableRepository)repositoryContainer.getComponentInstanceOfType(ManageableRepository.class);
      mr.setState(ManageableRepository.ONLINE);
   }

   public RepositoryServiceConfiguration getConfig()
   {
      return config;
   }

   public ManageableRepository getCurrentRepository() throws RepositoryException
   {
      if (currentRepositoryName.get() == null)
         return getDefaultRepository();
      return getRepository(currentRepositoryName.get());
   }

   public ManageableRepository getDefaultRepository() throws RepositoryException
   {
      return getRepository(config.getDefaultRepositoryName());
   }

   /**
    * @deprecated use getDefaultRepository() instead
    */
   @Deprecated
   public ManageableRepository getRepository() throws RepositoryException
   {
      return getDefaultRepository();
   }

   // ------------------- Startable ----------------------------

   public ManageableRepository getRepository(String name) throws RepositoryException
   {
      RepositoryContainer repositoryContainer = repositoryContainers.get(name);
      log.debug("RepositoryServiceimpl() getRepository " + name);
      if (repositoryContainer == null)
         throw new RepositoryException("Repository '" + name + "' not found.");

      return (ManageableRepository)repositoryContainer.getComponentInstanceOfType(ManageableRepository.class);
   }

   /**
    * {@inheritDoc}
    */
   public void removeRepository(String name) throws RepositoryException
   {
      removeRepository(name, false);
   }

   public void setCurrentRepositoryName(String repositoryName) throws RepositoryConfigurationException
   {
      if (!repositoryContainers.containsKey(repositoryName))
         throw new RepositoryConfigurationException("Repository is not configured. Name " + repositoryName);
      currentRepositoryName.set(repositoryName);
   }

   public void start()
   {
      try
      {
         ExoContainer container = null;
         if (containerContext == null)
         {
            container = PortalContainer.getInstance();
         }
         else
         {
            container = containerContext.getContainer();
         }

         init(container);

      }
      catch (RepositoryException e)
      {
         log.error("Error start repository service", e);
      }
      catch (RepositoryConfigurationException e)
      {
         log.error("Error start repository service", e);
      }
      catch (Throwable e)
      {
         log.error("Error start repository service", e);
         throw new RuntimeException(e);
      }
   }

   public void stop()
   {
      for (Entry<String, RepositoryContainer> entry : repositoryContainers.entrySet())
      {
         entry.getValue().stop();
      }
      repositoryContainers.clear();
      addNamespacesPlugins.clear();
      addNodeTypePlugins.clear();
      managerStartChanges.cleanup();
   }

   /**
    * Remove default repository.
    * 
    * @throws RepositoryException
    *          if any Exception occurred during removing
    */
   public void removeDefaultRepository() throws RepositoryException
   {
      removeRepository(config.getDefaultRepositoryName(), true);
   }

   private void init(ExoContainer container) throws RepositoryConfigurationException, RepositoryException
   {
      this.parentContainer = container;
      List<RepositoryEntry> rEntries = config.getRepositoryConfigurations();
      for (int i = 0; i < rEntries.size(); i++)
      {
         RepositoryEntry rEntry = rEntries.get(i);
         // Making new repository container as portal's subcontainer
         createRepository(rEntry);
      }
   }

   private void registerNodeTypes(String repositoryName) throws RepositoryException
   {
      ConfigurationManager configService =
         (ConfigurationManager)parentContainer.getComponentInstanceOfType(ConfigurationManager.class);

      ExtendedNodeTypeManager ntManager = getRepository(repositoryName).getNodeTypeManager();
      //
      for (int j = 0; j < addNodeTypePlugins.size(); j++)
      {
         AddNodeTypePlugin plugin = (AddNodeTypePlugin)addNodeTypePlugins.get(j);
         List<String> autoNodeTypesFiles = plugin.getNodeTypesFiles(AddNodeTypePlugin.AUTO_CREATED);
         if (autoNodeTypesFiles != null && autoNodeTypesFiles.size() > 0)
         {
            for (String nodeTypeFilesName : autoNodeTypesFiles)
            {
               InputStream inXml;
               try
               {
                  inXml = configService.getInputStream(nodeTypeFilesName);
               }
               catch (Exception e)
               {
                  throw new RepositoryException(e);
               }

               if (log.isDebugEnabled())
               {
                  log.debug("Trying register node types from xml-file " + nodeTypeFilesName);
               }
               ntManager.registerNodeTypes(inXml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
               if (log.isDebugEnabled())
               {
                  log.debug("Node types is registered from xml-file " + nodeTypeFilesName);
               }
            }

            List<String> defaultNodeTypesFiles = plugin.getNodeTypesFiles(repositoryName);
            if (defaultNodeTypesFiles != null && defaultNodeTypesFiles.size() > 0)
            {
               for (String nodeTypeFilesName : defaultNodeTypesFiles)
               {
                  InputStream inXml;
                  try
                  {
                     inXml = configService.getInputStream(nodeTypeFilesName);
                  }
                  catch (Exception e)
                  {
                     throw new RepositoryException(e);
                  }

                  log.info("Trying register node types (" + repositoryName + ") from xml-file " + nodeTypeFilesName);
                  ntManager.registerNodeTypes(inXml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS);
                  log.info("Node types is registered (" + repositoryName + ") from xml-file " + nodeTypeFilesName);
               }
            }
         }
      }
   }

   /**
    * Remove repository with specific name.
    * 
    * @param name
    *          repository name
    * @param allowRemoveDefaultRepository
    *          allow to remove default repository
    * @throws RepositoryException
    *          if any Exception occurred
    */
   private void removeRepository(String name, boolean allowRemoveDefaultRepository) throws RepositoryException
   {
      if (!canRemoveRepository(name, allowRemoveDefaultRepository))
         throw new RepositoryException("Repository " + name + " in use. If you want to "
            + " remove repository close all open sessions");

      try
      {
         RepositoryEntry repconfig = config.getRepositoryConfiguration(name);
         RepositoryImpl repo = (RepositoryImpl)getRepository(name);
         for (WorkspaceEntry wsEntry : repconfig.getWorkspaceEntries())
         {
            repo.internalRemoveWorkspace(wsEntry.getName());
         }
         repconfig.getWorkspaceEntries().clear();
         RepositoryContainer repositoryContainer = repositoryContainers.get(name);
         repositoryContainer.stop();
         repositoryContainers.remove(name);
         config.getRepositoryConfigurations().remove(repconfig);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }
      catch (Exception e)
      {
         throw new RepositoryException(e);
      }
   }

   /**
    * Indicates if repository with specific name can be removed.
    * 
    * @param name
    *          repository name
    * @param allowRemoveDefaultRepository
    *          allow to remove default repository 
    * @return
    *          true if repository can be removed or false in other case
    * @throws RepositoryException
    *          if any Exception occurred
    */
   private boolean canRemoveRepository(String name, boolean allowRemoveDefaultRepository) throws RepositoryException
   {
      if (!allowRemoveDefaultRepository && name.equals(config.getDefaultRepositoryName()))
      {
         return false;
      }

      RepositoryImpl repo = (RepositoryImpl)getRepository(name);
      try
      {
         RepositoryEntry repconfig = config.getRepositoryConfiguration(name);

         for (WorkspaceEntry wsEntry : repconfig.getWorkspaceEntries())
         {
            // Check non system workspaces
            if (!repo.getSystemWorkspaceName().equals(wsEntry.getName()) && !repo.canRemoveWorkspace(wsEntry.getName()))
               return false;
         }
         // check system workspace
         RepositoryContainer repositoryContainer = repositoryContainers.get(name);
         SessionRegistry sessionRegistry =
            (SessionRegistry)repositoryContainer.getComponentInstance(SessionRegistry.class);
         if (sessionRegistry == null || sessionRegistry.isInUse(repo.getSystemWorkspaceName()))
            return false;

      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }

      return true;
   }

   /**
    * Manager start changes plugins.
    */
   public class ManagerStartChanges
   {

      private Map<StorageKey, ItemsPersistenceListener> startChangesListeners =
         new HashMap<StorageKey, ItemsPersistenceListener>();

      /**
       * Add new StartChangesPlugin to manager.
       * 
       * @param plugin
       *          The StartChangesPlugin
       */
      public void addPlugin(RepositoryChangesListenerRegisterPlugin plugin)
      {
         String repositoryName = plugin.getRepositoryName();
         String workspaces = plugin.getWorkspaces();
         String listenerClassName = plugin.getListenerClassName();

         if (repositoryName != null && workspaces != null && listenerClassName != null)
         {
            StringTokenizer listTokenizer = new StringTokenizer(workspaces, ",");
            while (listTokenizer.hasMoreTokens())
            {
               String wsName = listTokenizer.nextToken();
               startChangesListeners.put(new StorageKey(repositoryName, wsName, listenerClassName), null);
            }
         }
      }

      /**
       * Register listeners.
       * 
       * @param repositoryContainer
       * @throws ClassNotFoundException
       */
      public void registerListeners(RepositoryContainer repositoryContainer)
      {
         Iterator<StorageKey> storageKeys = startChangesListeners.keySet().iterator();
         while (storageKeys.hasNext())
         {
            StorageKey sk = storageKeys.next();
            if (sk.getRepositoryName().equals(repositoryContainer.getName()))
            {
               WorkspaceContainer wc = repositoryContainer.getWorkspaceContainer(sk.getWorkspaceName());

               try
               {
                  Class<?> listenerType = Class.forName(sk.getListenerClassName());
                  wc.registerComponentImplementation(listenerType);
                  ItemsPersistenceListener listener =
                     (ItemsPersistenceListener)wc.getComponentInstanceOfType(listenerType);
                  startChangesListeners.put(sk, listener);
               }
               catch (ClassNotFoundException e)
               {
                  log.error("Can not register listener " + e.getMessage());
               }
            }
         }
      }

      /**
       * Cleanup changes.
       * 
       */
      public void cleanup()
      {
         startChangesListeners.clear();
      }

      /**
       * Will be used as key for startChangesListeners.
       * 
       */
      private class StorageKey
      {
         private final String repositoryName;

         private final String workspaceName;

         private final String listenerClassName;

         /**
          * StorageKey constructor.
          * 
          * @param repositoryName
          *          The repository name
          * @param workspaceName
          *          The workspace name
          */
         public StorageKey(String repositoryName, String workspaceName, String listenerClassName)
         {
            this.repositoryName = repositoryName;
            this.workspaceName = workspaceName;
            this.listenerClassName = listenerClassName;
         }

         /**
          * {@inheritDoc}
          */
         @Override
         public boolean equals(Object o)
         {
            StorageKey k = (StorageKey)o;

            return repositoryName.equals(k.repositoryName) && workspaceName.equals(k.workspaceName)
               && listenerClassName.equals(k.listenerClassName);
         }

         /**
          * {@inheritDoc}
          */
         @Override
         public int hashCode()
         {
            return repositoryName.hashCode() ^ workspaceName.hashCode() ^ listenerClassName.hashCode();
         }

         /**
          * Return repository name.
          * 
          * @return The repository name
          */
         public String getRepositoryName()
         {
            return repositoryName;
         }

         /**
          * Return workspace name.
          * 
          * @return The workspace name
          */
         public String getWorkspaceName()
         {
            return workspaceName;
         }

         /**
          * Return listener class name.
          * 
          * @return The listener class name
          */
         public String getListenerClassName()
         {
            return listenerClassName;
         }
      }
   }
}
