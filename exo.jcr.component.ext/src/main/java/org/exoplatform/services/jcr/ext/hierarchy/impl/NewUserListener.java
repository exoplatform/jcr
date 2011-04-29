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
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionManager;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionMode;
import org.exoplatform.services.jcr.ext.distribution.DataDistributionType;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.ext.hierarchy.impl.HierarchyConfig.JcrPath;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;

import java.util.List;
import java.util.Map;

import javax.jcr.Node;

/**
 * Created by The eXo Platform SAS Author : Dang Van Minh minh.dang@exoplatform.com Nov 15, 2007
 * 11:13:12 AM
 */
public class NewUserListener extends UserEventListener
{
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.ext.NewUserListener");

   private final HierarchyConfig config_;

   private final RepositoryService jcrService_;

   private final NodeHierarchyCreator nodeHierarchyCreatorService_;
   
   private final DataDistributionType dataDistributionType_;

   public NewUserListener(RepositoryService jcrService, NodeHierarchyCreator nodeHierarchyCreatorService,
      DataDistributionManager dataDistributionManager, InitParams params) throws Exception
   {
      jcrService_ = jcrService;
      dataDistributionType_ = dataDistributionManager.getDataDistributionType(DataDistributionMode.NONE);
      config_ = (HierarchyConfig)params.getObjectParamValues(HierarchyConfig.class).get(0);
      nodeHierarchyCreatorService.addPlugin(new AddPathPlugin(params));
      nodeHierarchyCreatorService_ = nodeHierarchyCreatorService;
   }

   public void preSave(User user, boolean isNew) throws Exception
   {
      if (isNew)
      {
         processUserStructure(jcrService_.getCurrentRepository(), user.getUserName());
      }
   }

   private void processUserStructure(ManageableRepository manageableRepository, String userName) throws Exception
   {
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      try 
      {
         Node userNode = nodeHierarchyCreatorService_.getUserNode(sessionProvider, userName);
         List<JcrPath> jcrPaths = config_.getJcrPaths();
         for (JcrPath jcrPath : jcrPaths)
         {
            createNode(userNode, jcrPath.getPath(), jcrPath.getNodeType(), jcrPath.getMixinTypes(),
               jcrPath.getPermissions(userName));
         }         
      }
      catch (Exception e)
      {
         log.error("An error occurs while initializing the user directory of '" + userName + "'", e);
      }
      finally
      {
         sessionProvider.close();
         sessionProvider = null;
      }
   }

   public void preDelete(User user)
   {
      // use a anonymous connection for the configuration as the user is not
      // authentified at that time
      SessionProvider sessionProvider = SessionProvider.createSystemProvider();
      try
      {
         nodeHierarchyCreatorService_.removeUserNode(sessionProvider, user.getUserName());
      }
      catch (Exception e)
      {
         log.error("An error occurs while removing the user directory of '" + user.getUserName() + "'", e);
      }
      finally
      {
         sessionProvider.close();
         sessionProvider = null;
      }
   }

   private void createNode(Node userNode, String path, String nodeType, List<String> mixinTypes,
            Map<String, String[]> permissions)
      throws Exception
   {
      dataDistributionType_.getOrCreateDataNode(userNode, path, nodeType, mixinTypes, permissions);
   }
}
