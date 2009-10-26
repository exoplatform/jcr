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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.Permission;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 11:13:12 AM
 */
public class NewUserListener extends UserEventListener
{

   private HierarchyConfig config_;

   private RepositoryService jcrService_;

   private NodeHierarchyCreator nodeHierarchyCreatorService_;

   private String userPath_;

   final static private String USERS_PATH = "usersPath";

   final static private String NT_UNSTRUCTURED = "nt:unstructured".intern();

   public NewUserListener(RepositoryService jcrService, NodeHierarchyCreator nodeHierarchyCreatorService,
      InitParams params) throws Exception
   {
      jcrService_ = jcrService;
      nodeHierarchyCreatorService_ = nodeHierarchyCreatorService;
      config_ = (HierarchyConfig)params.getObjectParamValues(HierarchyConfig.class).get(0);
      nodeHierarchyCreatorService_.addPlugin(new AddPathPlugin(params));
      userPath_ = nodeHierarchyCreatorService.getJcrPath(USERS_PATH);
   }

   public void preSave(User user, boolean isNew) throws Exception
   {
      String userName = user.getUserName();
      List<RepositoryEntry> repositories = jcrService_.getConfig().getRepositoryConfigurations();
      // TODO [PN, 12.02.08] only default repository should contains user structure
      if (isNew)
      {
         for (RepositoryEntry repo : repositories)
         {
            processUserStructure(repo.getName(), userName);
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void processUserStructure(String repository, String userName) throws Exception
   {
      ManageableRepository manageableRepository = jcrService_.getRepository(repository);
      String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
      Session session = manageableRepository.getSystemSession(systemWorkspace);
      Node usersHome = (Node)session.getItem(userPath_);
      List<JcrPath> jcrPaths = config_.getJcrPaths();
      Node userNode = null;
      try
      {
         userNode = usersHome.getNode(userName);
      }
      catch (PathNotFoundException e)
      {
         userNode = usersHome.addNode(userName);
      }
      for (JcrPath jcrPath : jcrPaths)
      {
         createNode(userNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(), getPermissions(jcrPath
            .getPermissions(), userName));
      }
      session.save();
      session.logout();
   }

   public void preDelete(User user)
   {
      // use a anonymous connection for the configuration as the user is not
      // authentified at that time
      List<RepositoryEntry> repositories = jcrService_.getConfig().getRepositoryConfigurations();
      for (RepositoryEntry repo : repositories)
      {
         try
         {
            ManageableRepository manaRepo = jcrService_.getRepository(repo.getName());
            Session session = manaRepo.getSystemSession(manaRepo.getConfiguration().getDefaultWorkspaceName());
            Node usersHome = (Node)session.getItem(nodeHierarchyCreatorService_.getJcrPath(USERS_PATH));
            usersHome.getNode(user.getUserName()).remove();
            session.save();
            session.logout();
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void createNode(Node userNode, String path, String nodeType, List<String> mixinTypes, Map permissions)
      throws Exception
   {
      if (nodeType == null || nodeType.length() == 0)
         nodeType = NT_UNSTRUCTURED;
      try
      {
         userNode = userNode.getNode(path);
      }
      catch (PathNotFoundException e)
      {
         userNode = userNode.addNode(path, nodeType);
      }
      if (userNode.canAddMixin("exo:privilegeable"))
         userNode.addMixin("exo:privilegeable");
      if (permissions != null && !permissions.isEmpty())
         ((ExtendedNode)userNode).setPermissions(permissions);
      if (mixinTypes.size() > 0)
      {
         for (String mixin : mixinTypes)
         {
            if (userNode.canAddMixin(mixin))
               userNode.addMixin(mixin);
         }
      }
   }

   private Map getPermissions(List<Permission> permissions, String userId)
   {
      Map<String, String[]> permissionsMap = new HashMap<String, String[]>();
      permissionsMap.put(userId, PermissionType.ALL);
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
}
