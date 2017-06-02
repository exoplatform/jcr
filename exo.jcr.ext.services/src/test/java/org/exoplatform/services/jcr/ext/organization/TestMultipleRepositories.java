/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.services.organization.OrganizationService;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Dec 1, 2011  
 */
public class TestMultipleRepositories extends AbstractOrganizationServiceTest
{

   /**
    * Test checks that possible to create same users, groups and memberships for different repositories.
    * This test used to ensure that caches for different repositories are isolated   
    * 
    * @throws Exception
    */
   public void testCreateSameUsersMemberships() throws Exception
   {
      String currentRepo = repositoryService.getCurrentRepository().getConfiguration().getName();
      String userName = "testCreateSameUsersMemberships-User1";
      String groupName = "testCreateSameUsersMemberships-Group1";
      String type = "testCreateSameUsersMemberships-Type1";
      try
      {
         createMembership(userName, groupName, type);
         // warmup cache
         uHandler.findUserByName(userName);
         mHandler.findMembershipsByUser(userName);
         mHandler.findMembershipByUserGroupAndType(userName, groupName, type);

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         prepareRepository();
         assertNull(uHandler.findUserByName(userName));
         assertNull(mHandler.findMembershipByUserGroupAndType(userName, groupName, type));
         createMembership(userName, groupName, type);
      }
      finally
      {
         repositoryService.setCurrentRepositoryName(currentRepo);
      }
   }

   /**
    * Test checks that possible to create same users and removing it from one repository won't
    * have an influence on other.
    * This test used to ensure that caches for different repositories are isolated   
    * 
    * @throws Exception
    */
   public void testCreateDeleteSameUsers() throws Exception
   {
      String currentRepo = repositoryService.getCurrentRepository().getConfiguration().getName();
      String userName = "testCreateDeleteSameUsers-User1";
      try
      {
         createUser(userName);
         // warmup cache
         uHandler.findUserByName(userName);

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         prepareRepository();
         assertNull(uHandler.findUserByName(userName));
         createUser(userName);
         uHandler.findUserByName(userName);

         // <--> switch back to repository "db1"
         repositoryService.setCurrentRepositoryName(currentRepo);
         // delete user from "db1"
         uHandler.removeUser(userName, true);
         assertNull(uHandler.findUserByName(userName));

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         // user from "db2" must still exist.
         assertNotNull(uHandler.findUserByName(userName));

      }
      finally
      {
         repositoryService.setCurrentRepositoryName(currentRepo);
      }
   }

   /**
    * Test checks that possible to create same groups and removing it from one repository won't
    * have an influence on other.
    * This test used to ensure that caches for different repositories are isolated   
    * 
    * @throws Exception
    */
   public void testCreateDeleteSameGroups() throws Exception
   {
      String currentRepo = repositoryService.getCurrentRepository().getConfiguration().getName();
      String groupName = "testCreateDeleteSameGroups-Group1";
      try
      {
         createGroup(null, groupName, "lable", "desc");
         // warmup cache
         assertNotNull(gHandler.findGroupById("/" + groupName));

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         prepareRepository();
         assertNull(gHandler.findGroupById("/" + groupName));
         createGroup(null, groupName, "lable", "desc");
         gHandler.findGroupById("/" + groupName);

         // <--> switch back to repository "db1"
         repositoryService.setCurrentRepositoryName(currentRepo);
         // delete group from "db1"
         gHandler.removeGroup(gHandler.findGroupById("/" + groupName), true);
         assertNull(gHandler.findGroupById("/" + groupName));

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         // group from "db2" must still exist.
         assertNotNull(gHandler.findGroupById("/" + groupName));
      }
      finally
      {
         repositoryService.setCurrentRepositoryName(currentRepo);
      }
   }

   /**
    * Test checks that possible to create same Memberships and removing it from one repository won't
    * have an influence on other.
    * This test used to ensure that caches for different repositories are isolated    
    * 
    * @throws Exception
    */
   public void testCreateDeleteSameMemberships() throws Exception
   {
      String currentRepo = repositoryService.getCurrentRepository().getConfiguration().getName();
      String userName = "testCreateDeleteSameMemberships-User1";
      String groupName = "testCreateDeleteSameMemberships-Group1";
      String type = "testCreateDeleteSameMemberships-Type1";
      try
      {
         createMembership(userName, groupName, type);
         // warmup cache
         uHandler.findUserByName(userName);
         mHandler.findMembershipsByUser(userName);
         mHandler.findMembershipByUserGroupAndType(userName, groupName, type);

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         prepareRepository();
         assertNull(uHandler.findUserByName(userName));
         assertNull(mHandler.findMembershipByUserGroupAndType(userName, groupName, type));
         createMembership(userName, groupName, type);

         // <--> switch back to repository "db1"
         repositoryService.setCurrentRepositoryName(currentRepo);
         // delete Membership from "db1"
         mHandler.removeMembershipByUser(userName, true);
         assertNull(mHandler.findMembershipByUserGroupAndType(userName, groupName, type));

         // <--> switch to repository "db2"
         repositoryService.setCurrentRepositoryName("db2");
         // group from "db2" must still exist.
         assertNotNull(mHandler.findMembershipByUserGroupAndType(userName, "/" + groupName, type));
      }
      finally
      {
         repositoryService.setCurrentRepositoryName(currentRepo);
      }
   }

   private void prepareRepository() throws RepositoryException
   {
      JCROrganizationServiceImpl organizationService =
         (JCROrganizationServiceImpl)container.getComponentInstanceOfType(OrganizationService.class);
      Session storageSession = organizationService.getStorageSession();

      // Check repository not prepared
      if (!storageSession.getRootNode().hasNode(organizationService.getStoragePath().substring(1)))
      {
         organizationService.createStructure();

         storageSession.save();
      }
   }
}
