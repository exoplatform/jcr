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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.component.ThreadContext;
import org.exoplatform.container.component.ThreadContextHolder;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.nodetype.ExtendedNodeTypeManager;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.io.InputStream;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: RepositoryServiceImpl.java 13986 2008-05-08 10:48:43Z pnedonosko $
 */

public class RepositoryServiceImpl implements RepositoryService, Startable, ThreadContextHolder
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.RepositoryServiceImpl");

   private final RepositoryServiceConfiguration config;

   private final ThreadLocal<String> currentRepositoryName = new ThreadLocal<String>();

   private final ConcurrentHashMap<String, RepositoryContainer> repositoryContainers =
      new ConcurrentHashMap<String, RepositoryContainer>();

   private final List<ComponentPlugin> addNodeTypePlugins;

   private final List<ComponentPlugin> addNamespacesPlugins;

   private final ExoContainerContext containerContext;

   private ExoContainer parentContainer;

   public RepositoryServiceImpl(RepositoryServiceConfiguration configuration)
   {
      this(configuration, null);
   }

   public RepositoryServiceImpl(RepositoryServiceConfiguration configuration, ExoContainerContext context)
   {
      this(configuration, context, null);
   }

   /**
    * @param synchronizer This component is used to synchronize the creation of the repositories between
    * all the cluster nodes. If this component has been defined in the configuration, it has to
    * be started before the {@link RepositoryServiceImpl} so we have to enforce the dependency
    * with this component by adding it to the constructor
    */
   public RepositoryServiceImpl(RepositoryServiceConfiguration configuration, ExoContainerContext context,
      RepositoryCreationSynchronizer synchronizer)
   {
      this.config = configuration;
      addNodeTypePlugins = new ArrayList<ComponentPlugin>();
      addNamespacesPlugins = new ArrayList<ComponentPlugin>();
      containerContext = context;
      currentRepositoryName.set(config.getDefaultRepositoryName());
   }

   public void addPlugin(ComponentPlugin plugin)
   {
      if (plugin instanceof AddNodeTypePlugin)
      {
         addNodeTypePlugins.add(plugin);
      }
      else if (plugin instanceof AddNamespacesPlugin)
      {
         addNamespacesPlugins.add(plugin);
      }
   }

   /**
    * Create repository. <br>
    * Init worksapces for initial start or them load from persistence. <br>
    * Add namespaces and nodetypes from service plugins.
    * 
    */
   public void createRepository(RepositoryEntry rEntry) throws RepositoryConfigurationException, RepositoryException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      if (repositoryContainers.containsKey(rEntry.getName()))
      {
         throw new RepositoryConfigurationException("Repository container " + rEntry.getName() + " already started");
      }

      final RepositoryContainer repositoryContainer =
         new RepositoryContainer(parentContainer, rEntry, addNamespacesPlugins);

      // Storing and starting the repository container under
      // key=repository_name
      try
      {
         if (repositoryContainers.putIfAbsent(rEntry.getName(), repositoryContainer) == null)
         {
            SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
            {
               public Void run()
               {
                  repositoryContainer.start();
                  return null;
               }
            });
         }
         else
         {
            throw new RepositoryConfigurationException("Repository container " + rEntry.getName() + " already started");
         }
      }
      catch (Throwable t) //NOSONAR
      {
         repositoryContainers.remove(rEntry.getName());

         throw new RepositoryConfigurationException("Repository container " + rEntry.getName() + " was not started.", t);
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
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      return config;
   }

   /**
    * {@inheritDoc} 
    */
   public ThreadContext getThreadContext()
   {
      return new ThreadContext(currentRepositoryName);
   }

   /**
    * @return Name of current repository if exists or null in other case
    */
   public String getCurrentRepositoryName()
   {
      return currentRepositoryName.get();
   }

   public ManageableRepository getCurrentRepository() throws RepositoryException
   {
      if (currentRepositoryName.get() == null)
      {
         return getDefaultRepository();
      }
      return getRepository(currentRepositoryName.get());
   }

   public ManageableRepository getDefaultRepository() throws RepositoryException
   {
      return getRepository(config.getDefaultRepositoryName());
   }

   // ------------------- Startable ----------------------------

   public ManageableRepository getRepository(String name) throws RepositoryException
   {
      RepositoryContainer repositoryContainer = repositoryContainers.get(name);
      log.debug("RepositoryServiceimpl() getRepository " + name);
      if (repositoryContainer == null)
      {
         throw new RepositoryException("Repository '" + name + "' not found.");
      }

      return (ManageableRepository)repositoryContainer.getComponentInstanceOfType(ManageableRepository.class);
   }

   public RepositoryContainer getRepositoryContainer(String name)
   {
      return repositoryContainers.get(name);
   }

   public void setCurrentRepositoryName(String repositoryName) throws RepositoryConfigurationException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      if (!repositoryContainers.containsKey(repositoryName))
      {
         throw new RepositoryConfigurationException("Repository is not configured. Name " + repositoryName);
      }
      currentRepositoryName.set(repositoryName);
   }

   public void start()
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

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
      catch (Throwable e) //NOSONAR
      {
         log.error("Error start repository service", e);
         throw new RuntimeException(e);
      }
   }

   public void stop()
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      for (Entry<String, RepositoryContainer> entry : repositoryContainers.entrySet())
      {
         entry.getValue().stop();
      }
      repositoryContainers.clear();
      addNamespacesPlugins.clear();
      addNodeTypePlugins.clear();
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
               ntManager.registerNodeTypes(inXml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS,
                  NodeTypeDataManager.TEXT_XML);
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
                  ntManager.registerNodeTypes(inXml, ExtendedNodeTypeManager.IGNORE_IF_EXISTS,
                     NodeTypeDataManager.TEXT_XML);
                  log.info("Node types is registered (" + repositoryName + ") from xml-file " + nodeTypeFilesName);
               }
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeRepository(String name) throws RepositoryException
   {
      this.removeRepository(name, false);
   }

   /**
    * {@inheritDoc}
    */
   public void removeRepository(final String name, boolean forceRemove) throws RepositoryException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      if (!forceRemove && !canRemoveRepository(name))
      {
         throw new RepositoryException("Repository " + name + " in use. If you want to "
            + " remove repository close all open sessions");
      }

      try
      {
         final RepositoryEntry repconfig = config.getRepositoryConfiguration(name);
         final RepositoryImpl repo = (RepositoryImpl)getRepository(name);
         repo.setState(ManageableRepository.OFFLINE);
         SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws RepositoryException
            {
               List<WorkspaceEntry> workspaces = new ArrayList<WorkspaceEntry>(repconfig.getWorkspaceEntries());
               for (WorkspaceEntry entry : workspaces)
               {
                  repo.internalRemoveWorkspace(entry.getName());
               }
               RepositoryContainer repositoryContainer = repositoryContainers.get(name);
               repositoryContainer.stop();
               repositoryContainers.remove(name);
               config.getRepositoryConfigurations().remove(repconfig);
               parentContainer.unregisterComponent(repositoryContainer.getName());
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RepositoryException)
         {
            throw (RepositoryException)cause;
         }
         else
         {
            throw new RepositoryException(cause);
         }
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
    * {@inheritDoc}
    */
   public boolean canRemoveRepository(String name) throws RepositoryException
   {
      RepositoryImpl repo = (RepositoryImpl)getRepository(name);
      try
      {
         RepositoryEntry repconfig = config.getRepositoryConfiguration(name);

         for (WorkspaceEntry wsEntry : repconfig.getWorkspaceEntries())
         {
            // Check non system workspaces
            if (!repo.getSystemWorkspaceName().equals(wsEntry.getName()) && !repo.canRemoveWorkspace(wsEntry.getName()))
            {
               return false;
            }
         }
         // check system workspace
         RepositoryContainer repositoryContainer = repositoryContainers.get(name);
         SessionRegistry sessionRegistry =
            (SessionRegistry)repositoryContainer.getComponentInstance(SessionRegistry.class);
         if (sessionRegistry == null || sessionRegistry.isInUse(repo.getSystemWorkspaceName()))
         {
            return false;
         }

      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException(e);
      }

      return true;
   }
}
