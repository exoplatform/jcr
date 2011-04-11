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
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SARL Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 11:13:25 AM
 */
public class NewGroupListener extends GroupEventListener
{
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.NewGroupListener");

   private static final String GROUPS_PATH = "groupsPath";

   private final HierarchyConfig config_;

   private final RepositoryService jcrService_;
   
   private final DataDistributionType dataDistributionType_;
   
   private final String groupsPath_;

   public NewGroupListener(RepositoryService jcrService, NodeHierarchyCreator nodeHierarchyCreatorService,
      DataDistributionManager dataDistributionManager, InitParams params) throws Exception
   {
      jcrService_ = jcrService;
      config_ = (HierarchyConfig)params.getObjectParamValues(HierarchyConfig.class).get(0);
      dataDistributionType_ = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
      groupsPath_ = nodeHierarchyCreatorService.getJcrPath(GROUPS_PATH);
   }

   /**
    * {@inheritDoc}
    */
   public void preSave(Group group, boolean isNew) throws Exception
   {
      String groupId = null;
      if (group.getId() != null)
      {
         groupId = group.getId();
      }
      else
      {
         String parentId = group.getParentId();
         if (parentId == null || parentId.length() == 0)
            groupId = "/" + group.getGroupName();
         else
            groupId = parentId + "/" + group.getGroupName();
      }
      if (isNew)
      {
         buildGroupStructure(jcrService_.getCurrentRepository(), groupId);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void preDelete(Group group) throws Exception
   {
      String groupId = null;
      if (group.getId() != null)
      {
         groupId = group.getId();
      }
      else
      {
         String parentId = group.getParentId();
         if (parentId == null || parentId.length() == 0)
            groupId = "/" + group.getGroupName();
         else
            groupId = parentId + "/" + group.getGroupName();
      }
      removeGroup(jcrService_.getCurrentRepository(), groupId);
   }

   private void removeGroup(ManageableRepository manageableRepository, String groupId) throws Exception
   {
      Session session = null;
      try
      {
         String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
         session = manageableRepository.getSystemSession(systemWorkspace);
         Node groupsHome = (Node)session.getItem(groupsPath_);
         dataDistributionType_.removeDataNode(groupsHome, groupId);
      }
      catch (Exception e)
      {
         log.error("An error occurs while removing the group directory of '" + groupId + "'", e);
      }
      finally
      {
         if (session != null)
         {
            session.logout();            
         }
      }
   }

   @SuppressWarnings("unchecked")
   private void buildGroupStructure(ManageableRepository manageableRepository, String groupId) throws Exception
   {
      Session session = null;
      try
      {
         String systemWorkspace = manageableRepository.getConfiguration().getDefaultWorkspaceName();
         session = manageableRepository.getSystemSession(systemWorkspace);
         Node groupsHome = (Node)session.getItem(groupsPath_);
         Node groupNode = dataDistributionType_.getOrCreateDataNode(groupsHome, groupId);
         @SuppressWarnings("rawtypes")
         List jcrPaths = config_.getJcrPaths();
         for (JcrPath jcrPath : (List<JcrPath>)jcrPaths)
         {
            createNode(groupNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(),
               jcrPath.getPermissions("*:".concat(groupId)));
         }
      }
      catch (Exception e)
      {
         log.error("An error occurs while initializing the group directory of '" + groupId + "'", e);
      }
      finally
      {
         if (session != null)
         {
            session.logout();            
         }
      }
   }

   private void createNode(Node groupNode, String path, String nodeType, List<String> mixinTypes, Map<String, String[]> permissions)
      throws Exception
   {
      dataDistributionType_.getOrCreateDataNode(groupNode, path, nodeType, mixinTypes, permissions);
   }
}
