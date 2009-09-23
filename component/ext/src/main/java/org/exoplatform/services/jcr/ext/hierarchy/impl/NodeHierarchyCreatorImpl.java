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

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.Permission;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 2:21:57 PM
 */
public class NodeHierarchyCreatorImpl implements NodeHierarchyCreator, Startable
{

   final static private String NT_UNSTRUCTURED = "nt:unstructured".intern();

   final static private String USERS_PATH = "usersPath";

   final static private String USER_APPLICATION = "userApplicationData";

   final static private String PUBLIC_APPLICATION = "eXoApplications";

   final static private String USER_PRIVATE = "userPrivate";

   final static private String USER_PUBLIC = "userPublic";

   private RepositoryService jcrService_;

   List<AddPathPlugin> pathPlugins_ = new ArrayList<AddPathPlugin>();

   public NodeHierarchyCreatorImpl(RepositoryService jcrService) throws Exception
   {
      jcrService_ = jcrService;
   }

   public void start()
   {
      try
      {
         processAddPathPlugin();
      }
      catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   public void stop()
   {
   }

   public void init(String repository) throws Exception
   {
      initBasePath(repository);
   }

   @SuppressWarnings("unchecked")
   private void createNode(Node rootNode, String path, String nodeType, List<String> mixinTypes, Map permissions)
      throws Exception
   {
      Node node = rootNode;
      for (String token : path.split("/"))
      {
         if (token.length() == 0) 
         {
            continue;
         }
         try
         {
            node = node.getNode(token);
         }
         catch (PathNotFoundException e)
         {
            if (nodeType == null || nodeType.length() == 0)
               nodeType = NT_UNSTRUCTURED;
            node = node.addNode(token, nodeType);
            if (node.canAddMixin("exo:privilegeable"))
               node.addMixin("exo:privilegeable");
            if (permissions != null && !permissions.isEmpty())
               ((ExtendedNode)node).setPermissions(permissions);
            if (mixinTypes.size() > 0)
            {
               for (String mixin : mixinTypes)
               {
                  if (node.canAddMixin(mixin))
                     node.addMixin(mixin);
               }
            }
         }
      }
   }

   public Map getPermissions(List<Permission> permissions)
   {
      Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
      for (Permission permission : permissions)
      {
         StringBuilder strPer = new StringBuilder();
         if ("true".equals(permission.getRead()))
            strPer.append(PermissionType.READ);
         if ("true".equals(permission.getAddNode()))
            strPer.append(",").append(PermissionType.ADD_NODE);
         if ("true".equals(permission.getSetProperty()))
            strPer.append(",").append(PermissionType.SET_PROPERTY);
         if ("true".equals(permission.getRemove()))
            strPer.append(",").append(PermissionType.REMOVE);
         permissionsMap.put(permission.getIdentity(), strPer.toString().split(","));
      }
      return permissionsMap;
   }

   @SuppressWarnings("unchecked")
   private void processAddPathPlugin() throws Exception
   {
      Session session = null;
      for (AddPathPlugin pathPlugin : pathPlugins_)
      {
         HierarchyConfig hierarchyConfig = pathPlugin.getPaths();
         String repository = hierarchyConfig.getRepository();
         List<JcrPath> jcrPaths = hierarchyConfig.getJcrPaths();
         for (String workspaceName : hierarchyConfig.getWorkspaces())
         {
            session = jcrService_.getRepository(repository).getSystemSession(workspaceName);
            Node rootNode = session.getRootNode();
            for (JcrPath jcrPath : jcrPaths)
            {
               String nodeType = jcrPath.getNodeType();
               if (nodeType == null || nodeType.length() == 0)
                  nodeType = NT_UNSTRUCTURED;
               List<String> mixinTypes = jcrPath.getMixinTypes();
               if (mixinTypes == null)
                  mixinTypes = new ArrayList<String>();
               if (!jcrPath.getAlias().equals(USER_APPLICATION) && !jcrPath.getAlias().startsWith(USER_PRIVATE)
                  && !jcrPath.getAlias().startsWith(USER_PUBLIC))
               {
                  createNode(rootNode, jcrPath.getPath(), nodeType, mixinTypes,
                     getPermissions(jcrPath.getPermissions()));
               }
            }
            session.save();
            session.logout();
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void initBasePath(String repository) throws Exception
   {
      Session session = null;
      ManageableRepository manageableRepository = jcrService_.getRepository(repository);
      String defaultWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      String systemWorkspace = manageableRepository.getConfiguration().getSystemWorkspaceName();
      boolean isSameWorksapce = defaultWorkspace.equalsIgnoreCase(systemWorkspace);
      String[] workspaceNames = manageableRepository.getWorkspaceNames();
      for (AddPathPlugin pathPlugin : pathPlugins_)
      {
         HierarchyConfig hierarchyConfig = pathPlugin.getPaths();
         List<JcrPath> jcrPaths = hierarchyConfig.getJcrPaths();
         for (String workspaceName : workspaceNames)
         {
            if (!isSameWorksapce && workspaceName.equalsIgnoreCase(systemWorkspace))
               continue;
            session = manageableRepository.getSystemSession(workspaceName);
            Node rootNode = session.getRootNode();
            for (JcrPath jcrPath : jcrPaths)
            {
               String nodeType = jcrPath.getNodeType();
               if (nodeType == null || nodeType.length() == 0)
                  nodeType = NT_UNSTRUCTURED;
               List<String> mixinTypes = jcrPath.getMixinTypes();
               if (mixinTypes == null)
                  mixinTypes = new ArrayList<String>();
               if (!jcrPath.getAlias().equals(USER_APPLICATION) && !jcrPath.getAlias().startsWith(USER_PRIVATE)
                  && !jcrPath.getAlias().startsWith(USER_PUBLIC))
               {
                  createNode(rootNode, jcrPath.getPath(), nodeType, mixinTypes,
                     getPermissions(jcrPath.getPermissions()));
               }
            }
            session.save();
            session.logout();
         }
      }
   }

   public Node getUserApplicationNode(SessionProvider sessionProvider, String userName) throws Exception
   {
      Node userNode = getUserNode(sessionProvider, userName);
      Node userAppNode = null;
      try
      {
         userAppNode = userNode.getNode(getJcrPath(USER_APPLICATION));
      }
      catch (PathNotFoundException e)
      {
         userAppNode = userNode.addNode(getJcrPath(USER_APPLICATION));
         userNode.save();
      }
      return userAppNode;
   }

   public Node getPublicApplicationNode(SessionProvider sessionProvider) throws Exception
   {
      ManageableRepository currentRepo = jcrService_.getCurrentRepository();
      Session session =
         getSession(sessionProvider, currentRepo, currentRepo.getConfiguration().getDefaultWorkspaceName());
      Node rootNode = session.getRootNode();
      String publicApplication = getJcrPath(PUBLIC_APPLICATION);
      session.logout();
      return rootNode.getNode(publicApplication.substring(1, publicApplication.length()));
   }

   public Node getUserNode(SessionProvider sessionProvider, String userName) throws Exception
   {
      String userPath = getJcrPath(USERS_PATH);
      ManageableRepository currentRepo = jcrService_.getCurrentRepository();
      Session session =
         getSession(sessionProvider, currentRepo, currentRepo.getConfiguration().getDefaultWorkspaceName());
      Node usersNode = (Node)session.getItem(userPath);
      Node userNode = null;
      try
      {
         userNode = usersNode.getNode(userName);
      }
      catch (PathNotFoundException e)
      {
         userNode = usersNode.addNode(userName);
         usersNode.save();
      }
      finally
      {
         session.logout();
      }
      return userNode;
   }

   private Session getSession(SessionProvider sessionProvider, ManageableRepository repo, String defaultWorkspace)
      throws RepositoryException
   {
      return sessionProvider.getSession(defaultWorkspace, repo);
   }

   public String getJcrPath(String alias)
   {
      for (int j = 0; j < pathPlugins_.size(); j++)
      {
         HierarchyConfig config = pathPlugins_.get(j).getPaths();
         List jcrPaths = config.getJcrPaths();
         for (Iterator iter = jcrPaths.iterator(); iter.hasNext();)
         {
            HierarchyConfig.JcrPath jcrPath = (HierarchyConfig.JcrPath)iter.next();
            if (jcrPath.getAlias().equals(alias))
            {
               return jcrPath.getPath();
            }
         }
      }
      return null;
   }

   public void addPlugin(ComponentPlugin plugin)
   {
      if (plugin instanceof AddPathPlugin)
         pathPlugins_.add((AddPathPlugin)plugin);
   }

   @SuppressWarnings("unused")
   public ComponentPlugin removePlugin(String name)
   {
      return null;
   }

   public Collection getPlugins()
   {
      return null;
   }
}
