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
package org.exoplatform.services.jcr.ext.hierarchy.impl;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * 
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 2:21:57 PM
 */
public class NodeHierarchyCreatorImpl implements NodeHierarchyCreator, Startable
{

   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.NodeHierarchyCreatorImpl");

   private static final String USERS_PATH = "usersPath";

   private static final String USER_APPLICATION = "userApplicationData";

   private static final String PUBLIC_APPLICATION = "eXoApplications";

   private static final String USER_PRIVATE = "userPrivate";

   private static final String USER_PUBLIC = "userPublic";

   private final RepositoryService jcrService_;

   private final DataDistributionManager dataDistributionManager_;

   private final List<AddPathPlugin> pathPlugins_ = new ArrayList<AddPathPlugin>();

   private final Map<String, String> paths_ = new HashMap<String, String>();

   private final boolean oldDistribution;

   public NodeHierarchyCreatorImpl(RepositoryService jcrService, InitParams params)
   {
      this(jcrService, null, params);
   }

   public NodeHierarchyCreatorImpl(RepositoryService jcrService, DataDistributionManager dataDistributionManager,
      InitParams params)
   {
      if (dataDistributionManager == null)
      {
         throw new IllegalArgumentException("The DataDistributionManager is now mandatory if you use the "
            + "NodeHierarchyCreator, so please define it in your configuration "
            + "as described in the JCR documentation");
      }
      jcrService_ = jcrService;
      dataDistributionManager_ = dataDistributionManager;
      oldDistribution =
         params != null && params.getValueParam("old-user-distribution") != null
            && Boolean.valueOf(params.getValueParam("old-user-distribution").getValue());
      if (PropertyManager.isDevelopping() && !oldDistribution)
      {
         log.info("The NodeHierarchyCreator is configured to use the new distribution mechanism for the"
            + " users directories, if you prefer to use the old mechanism set the value parameter "
            + "'old-user-distribution' to 'true'.");
      }
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
      try
      {
         processAddPathPlugin();
      }
      catch (Exception e)
      {
         log.error("An error occurs while processing the plugins", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void init(String repository) throws Exception
   {
      init();
   }

   /**
    * {@inheritDoc}
    */
   public void init() throws Exception
   {
      initBasePath();
   }

   private void createNode(Node rootNode, String path, String nodeType, List<String> mixinTypes,
      Map<String, String[]> permissions) throws Exception
   {
      dataDistributionManager_.getDataDistributionType(DataDistributionMode.NONE).getOrCreateDataNode(rootNode, path,
         nodeType, mixinTypes, permissions);
   }

   private void processAddPathPlugin() throws Exception
   {
      Session session = null;
      ManageableRepository currentRepo = jcrService_.getCurrentRepository();
      for (AddPathPlugin pathPlugin : pathPlugins_)
      {
         HierarchyConfig hierarchyConfig = pathPlugin.getPaths();
         if (hierarchyConfig == null)
         {
            continue;
         }
         List<JcrPath> jcrPaths = hierarchyConfig.getJcrPaths();
         if (jcrPaths == null)
         {
            continue;
         }
         Set<String> workspaceNames = new LinkedHashSet<String>();
         if (hierarchyConfig.getWorkspaces() != null)
         {
            workspaceNames.addAll(hierarchyConfig.getWorkspaces());
         }
         workspaceNames.add(currentRepo.getConfiguration().getDefaultWorkspaceName());
         for (String workspaceName : workspaceNames)
         {
            JcrPath currentjcrPath = null;
            try
            {
               session = currentRepo.getSystemSession(workspaceName);
               Node rootNode = session.getRootNode();
               for (JcrPath jcrPath : jcrPaths)
               {
                  currentjcrPath = jcrPath;
                  if (!jcrPath.getAlias().equals(USER_APPLICATION) && !jcrPath.getAlias().startsWith(USER_PRIVATE)
                     && !jcrPath.getAlias().startsWith(USER_PUBLIC))
                  {
                     createNode(rootNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(),
                        jcrPath.getPermissions(null));
                  }
               }
            }
            catch (Exception e)
            {
               log.error("An error occurs while processing the JCR path which alias is "
                  + (currentjcrPath == null ? null : currentjcrPath.getAlias()) + " with the workspace "
                  + workspaceName, e);
            }
            finally
            {
               if (session != null)
               {
                  session.logout();
                  session = null;
               }
            }
         }
      }
   }

   private void initBasePath() throws Exception
   {
      Session session = null;
      ManageableRepository manageableRepository = jcrService_.getCurrentRepository();
      String defaultWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      String systemWorkspace = manageableRepository.getConfiguration().getSystemWorkspaceName();
      boolean isSameWorksapce = defaultWorkspace.equalsIgnoreCase(systemWorkspace);
      String[] workspaceNames = manageableRepository.getWorkspaceNames();
      for (AddPathPlugin pathPlugin : pathPlugins_)
      {
         HierarchyConfig hierarchyConfig = pathPlugin.getPaths();
         if (hierarchyConfig == null)
         {
            continue;
         }
         List<JcrPath> jcrPaths = hierarchyConfig.getJcrPaths();
         if (jcrPaths == null)
         {
            continue;
         }
         for (String workspaceName : workspaceNames)
         {
            if (!isSameWorksapce && workspaceName.equalsIgnoreCase(systemWorkspace))
               continue;
            JcrPath currentjcrPath = null;
            try
            {
               session = manageableRepository.getSystemSession(workspaceName);
               Node rootNode = session.getRootNode();
               for (JcrPath jcrPath : jcrPaths)
               {
                  currentjcrPath = jcrPath;
                  if (!jcrPath.getAlias().equals(USER_APPLICATION) && !jcrPath.getAlias().startsWith(USER_PRIVATE)
                     && !jcrPath.getAlias().startsWith(USER_PUBLIC))
                  {
                     createNode(rootNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(),
                        jcrPath.getPermissions(null));
                  }
               }
            }
            catch (Exception e)
            {
               log.error("An error occurs while processing the JCR path which alias is "
                  + (currentjcrPath == null ? null : currentjcrPath.getAlias()) + " with the workspace "
                  + workspaceName, e);
            }
            finally
            {
               if (session != null)
               {
                  session.logout();
                  session = null;
               }
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public Node getUserApplicationNode(SessionProvider sessionProvider, String userName) throws Exception
   {
      Node userNode = getUserNode(sessionProvider, userName);
      return dataDistributionManager_.getDataDistributionType(DataDistributionMode.NONE).getOrCreateDataNode(userNode,
         getJcrPath(USER_APPLICATION));
   }

   /**
    * {@inheritDoc}
    */
   public Node getPublicApplicationNode(SessionProvider sessionProvider) throws Exception
   {
      Session session = getSession(sessionProvider);
      Node rootNode = session.getRootNode();
      return dataDistributionManager_.getDataDistributionType(DataDistributionMode.NONE).getDataNode(rootNode,
         getJcrPath(PUBLIC_APPLICATION));
   }

   /**
    * {@inheritDoc}
    */
   public Node getUserNode(SessionProvider sessionProvider, String userName) throws Exception
   {
      String userPath = getJcrPath(USERS_PATH);
      Session session = getSession(sessionProvider);
      Node usersNode = (Node)session.getItem(userPath);
      DataDistributionType type =
         dataDistributionManager_.getDataDistributionType(oldDistribution ? DataDistributionMode.NONE
            : DataDistributionMode.READABLE);
      return type.getOrCreateDataNode(usersNode, userName);
   }

   /**
    * {@inheritDoc}
    */
   public void removeUserNode(SessionProvider sessionProvider, String userName) throws Exception
   {
      String userPath = getJcrPath(USERS_PATH);
      Session session = getSession(sessionProvider);
      try
      {
         Node usersNode = (Node)session.getItem(userPath);
         DataDistributionType type =
            dataDistributionManager_.getDataDistributionType(oldDistribution ? DataDistributionMode.NONE
               : DataDistributionMode.READABLE);
         type.removeDataNode(usersNode, userName);
      }
      catch (PathNotFoundException e)
      {
         // ignore me
      }
   }

   private Session getSession(SessionProvider sessionProvider) throws RepositoryException
   {
      ManageableRepository repo = jcrService_.getCurrentRepository();
      return sessionProvider.getSession(repo.getConfiguration().getDefaultWorkspaceName(), repo);
   }

   /**
    * {@inheritDoc}
    */
   public String getJcrPath(String alias)
   {
      return paths_.get(alias);
   }

   /**
    * {@inheritDoc}
    */
   public void addPlugin(ComponentPlugin plugin)
   {
      if (plugin instanceof AddPathPlugin)
      {
         AddPathPlugin app = (AddPathPlugin)plugin;
         pathPlugins_.add(app);
         if (app.getPaths() != null && app.getPaths().getJcrPaths() != null)
         {
            for (JcrPath jcrPath : app.getPaths().getJcrPaths())
            {
               if (jcrPath.getAlias() != null && jcrPath.getPath() != null)
               {
                  paths_.put(jcrPath.getAlias(), jcrPath.getPath());
               }
            }
         }
      }
   }
}
