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

import java.util.*;

import javax.jcr.*;
import javax.jcr.nodetype.NoSuchNodeTypeException;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.*;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.*;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrLink;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * 
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 2:21:57 PM
 */
public class NodeHierarchyCreatorImpl implements NodeHierarchyCreator, Startable
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.NodeHierarchyCreatorImpl");

   private static final String USERS_PATH = "usersPath";

   private static final String USER_APPLICATION = "userApplicationData";

   private static final String PUBLIC_APPLICATION = "eXoApplications";

   private static final String USER_PRIVATE = "userPrivate";

   private static final String USER_PUBLIC = "userPublic";

   private final RepositoryService jcrService_;

   private final DataDistributionManager dataDistributionManager_;

   private final List<AddPathPlugin> pathPlugins_ = new ArrayList<AddPathPlugin>();

   private final List<UserAddPathPlugin> userPathPlugins_ = new ArrayList<UserAddPathPlugin>();

   private final Map<String, String> paths_ = new HashMap<String, String>();

   private final boolean oldDistribution;

   private final boolean autoMigrate;

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
      autoMigrate =
         params != null && params.getValueParam("auto-migrate") != null
            && Boolean.valueOf(params.getValueParam("auto-migrate").getValue());
      
      if (PropertyManager.isDevelopping() && !oldDistribution)
      {
         LOG.info("The NodeHierarchyCreator is configured to use the new distribution mechanism for the"
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
         processAddPathPlugin(pathPlugins_, "/", null);
      }
      catch (Exception e)
      {
         LOG.error("An error occurs while processing the plugins", e);
      }

      if (isNeededToMigrate())
      {
         try
         {
            migrate();
         }
         catch (RepositoryException e)
         {
            LOG.error("An error occurs while upgrading JCR structure", e);
         }
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
      initBasePath(repository);
   }

   /**
    * {@inheritDoc}
    */
   public void init() throws Exception
   {
      initBasePath(null);
   }

   private Node createNode(Node rootNode, String path, String nodeType, List<String> mixinTypes,
      Map<String, String[]> permissions) throws Exception
   {
      return dataDistributionManager_.getDataDistributionType(DataDistributionMode.NONE).getOrCreateDataNode(rootNode, path,
         nodeType, mixinTypes, permissions);
   }

   private void processAddPathPlugin(List<? extends AddPathPlugin> pathPlugins, String parentPath, String userName) throws Exception
   {
      Session session = null;
      for (AddPathPlugin pathPlugin : pathPlugins)
      {
         HierarchyConfig hierarchyConfig = pathPlugin.getPaths();
         if (hierarchyConfig == null)
         {
            continue;
         }         
         List<JcrPath> jcrPaths = hierarchyConfig.getJcrPaths();
         List<JcrLink> jcrLinks = hierarchyConfig.getJcrLinks();
         if (jcrPaths == null && jcrLinks == null)
         {
            continue;
         }

         String repositoryName = hierarchyConfig.getRepository();
         ManageableRepository repository = repositoryName == null || repositoryName.isEmpty() ? 
               jcrService_.getCurrentRepository() : 
               jcrService_.getRepository(repositoryName);

         Set<String> workspaceNames = new LinkedHashSet<String>();
         if (hierarchyConfig.getWorkspaces() != null)
         {
            workspaceNames.addAll(hierarchyConfig.getWorkspaces());
         }

         for (String workspaceName : workspaceNames)
         {
            JcrPath currentjcrPath = null;
            try
            {
               session = repository.getSystemSession(workspaceName);
               if (!session.itemExists(parentPath)) {
                 LOG.warn("Parent path {} doesn't exist on workspace {}.", parentPath, workspaceName);
                 continue;
               }
               Node rootNode = (Node) session.getItem(parentPath);
               if (jcrPaths != null) {
                 for (JcrPath jcrPath : jcrPaths) {
                   currentjcrPath = jcrPath;
                   if (StringUtils.isNotBlank(userName) || (!jcrPath.getAlias().equals(USER_APPLICATION)
                       && !jcrPath.getAlias().startsWith(USER_PRIVATE) && !jcrPath.getAlias().startsWith(USER_PUBLIC))) {
                     NodeImpl addedNode = (NodeImpl) createNode(rootNode,
                                                                jcrPath.getPath(),
                                                                jcrPath.getNodeType(),
                                                                jcrPath.getMixinTypes(),
                                                                jcrPath.getPermissions(userName));
                     // Adjust user permissions to delete 'remove' permission for user
                     if (addedNode.canAddMixin("exo:privilegeable")) {
                       addedNode.addMixin("exo:privilegeable");
                       Map<String, String[]> permissions = buildPermissions(addedNode);
                       addedNode.setPermissions(permissions);
                       addedNode.save();
                     }
                     addedNode.removePermission(userName, PermissionType.REMOVE);
                     session.save();
                   }
                 }
               }
               if (jcrLinks != null) {
                 for (JcrLink jcrLink : jcrLinks) {
                   if (!rootNode.hasNode(jcrLink.getSourcePath())) {
                     LOG.warn("Can't create symlink {}. path not found {} under parent path {}",
                             jcrLink.getTargetPath(),
                             jcrLink.getSourcePath(),
                             rootNode.getPath());
                     continue;
                   }
                   if (rootNode.hasNode(jcrLink.getTargetPath())) {
                     continue;
                   }
                   NodeImpl sourceNode = (NodeImpl) rootNode.getNode(jcrLink.getSourcePath());
                   createLink(rootNode, sourceNode, jcrLink.getTargetPath());
                   session.save();
                 }
               }
            }
            catch (RepositoryException e)
            {
               LOG.error("An error occurs while processing the JCR path which alias is "
                  + (currentjcrPath == null ? null : currentjcrPath.getAlias()) + " with the workspace "
                  + workspaceName, e);
            }
         }
      }
   }

   private Map<String, String[]> buildPermissions(NodeImpl node) throws RepositoryException{
     Map<String, String[]> permissions = new HashMap<String, String[]>();
     List<String> permsList = new ArrayList<String>();
     String key = null;
     for (AccessControlEntry entry : node.getACL().getPermissionEntries()) {
       key = entry.getIdentity();
       if(!permissions.containsKey(key)) {
         permsList = node.getACL().getPermissions(key);
         permissions.put(key, permsList.toArray(new String[permsList.size()]));
       }
     }
     return permissions;
   }

   private void initBasePath(String repositoryName) throws Exception
   {
      Session session = null;
      ManageableRepository manageableRepository =
         repositoryName == null || repositoryName.isEmpty() ? jcrService_.getCurrentRepository() : jcrService_
            .getRepository(repositoryName);
      String defaultWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      String systemWorkspace = manageableRepository.getConfiguration().getSystemWorkspaceName();
      boolean isSameWorkspace = defaultWorkspace.equalsIgnoreCase(systemWorkspace);
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
            if (!isSameWorkspace && workspaceName.equalsIgnoreCase(systemWorkspace))
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
               LOG.error("An error occurs while processing the JCR path which alias is "
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
      Session session = getSession(sessionProvider);

      Node usersNode = getRootOfUsersNodes(session);

      DataDistributionType type =
         dataDistributionManager_.getDataDistributionType(oldDistribution ? DataDistributionMode.NONE
            : DataDistributionMode.READABLE);
      Node userHomeNode;
      try {
        userHomeNode = type.getDataNode(usersNode, userName);
      } catch (PathNotFoundException e) {
        userHomeNode = null;
      }
      if (userHomeNode == null) {
        userHomeNode = type.getOrCreateDataNode(usersNode, userName);
        session.save();
        try {
          if (userHomeNode.canAddMixin("exo:userFolder")) {
            userHomeNode.addMixin("exo:userFolder");
            session.save();
          }
        } catch (NoSuchNodeTypeException e) {
          LOG.trace("can't add mixin 'exo:userFolder' to folder " + userHomeNode.getPath(), e);
        }
        processAddPathPlugin(userPathPlugins_, userHomeNode.getPath(), userName);
      }
      return userHomeNode;
   }

   /**
    * {@inheritDoc}
    */
   public void removeUserNode(SessionProvider sessionProvider, String userName) throws Exception
   {
      Session session = getSession(sessionProvider);
      try
      {
         Node usersNode = getRootOfUsersNodes(session);

         DataDistributionType type =
            dataDistributionManager_.getDataDistributionType(oldDistribution ? DataDistributionMode.NONE
               : DataDistributionMode.READABLE);
         type.removeDataNode(usersNode, userName);
      }
      catch (PathNotFoundException e)
      {
         if (LOG.isTraceEnabled())
         {
            LOG.trace("An exception occurred: " + e.getMessage());
         }
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
      if(plugin instanceof UserAddPathPlugin) {
        UserAddPathPlugin app = (UserAddPathPlugin)plugin;
        userPathPlugins_.add(app);
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
      } else if (plugin instanceof AddPathPlugin) {
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

   private boolean isNeededToMigrate()
   {
      return !oldDistribution && autoMigrate;
   }

   private void migrate() throws RepositoryException
   {
      Session session = getSession(SessionProvider.createSystemProvider());

      try
      {
         Node rootNode = getRootOfUsersNodes(session);
         dataDistributionManager_.getDataDistributionType(DataDistributionMode.READABLE).migrate(rootNode);
      }
      finally
      {
         session.logout();
      }
   }

   private Node getRootOfUsersNodes(Session session) throws PathNotFoundException, RepositoryException
   {
      String usersPath = getJcrPath(USERS_PATH);
      return (Node)session.getItem(usersPath);
   }


   private Node createLink(Node parent, NodeImpl target, String linkName) throws RepositoryException {
     if (!target.isNodeType("exo:symlink")) {
       if (target.canAddMixin("mix:referenceable")) {
         target.addMixin("mix:referenceable");
         target.getSession().save();
       }
       if (linkName == null || linkName.trim().length() == 0)
         linkName = target.getName();
       NodeImpl linkNode = (NodeImpl) parent.addNode(linkName, "exo:symlink");
       if (linkNode.canAddMixin("exo:privilegeable")) {
         linkNode.addMixin("exo:privilegeable");
       }
       try {
         linkNode.setPermissions(buildPermissions(target));
       } catch (Exception e) {
         if (LOG.isErrorEnabled()) {
           LOG.error("CAN NOT UPDATE ACCESS PERMISSIONS FROM TARGET NODE TO LINK NODE", e);
         }
       }
       linkNode.setProperty("exo:workspace", target.getSession().getWorkspace().getName());
       linkNode.setProperty("exo:primaryType", target.getPrimaryNodeType().getName());
       linkNode.setProperty("exo:uuid", target.getUUID());
       linkNode.getSession().save();
       return linkNode;
     }
     return null;
   }
}
