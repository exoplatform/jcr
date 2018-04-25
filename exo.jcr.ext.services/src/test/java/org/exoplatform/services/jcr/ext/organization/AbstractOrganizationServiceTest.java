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

import org.exoplatform.services.jcr.ext.BaseStandaloneTest;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2011
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id: AbstractOrganizationServiceTest.java 111 2011-11-11 11:11:11Z rainf0x $
 */
public class AbstractOrganizationServiceTest extends BaseStandaloneTest
{

   protected GroupHandler gHandler;

   protected MembershipHandler mHandler;

   protected UserHandler uHandler;

   protected MembershipTypeHandler mtHandler;

   protected UserProfileHandler upHandler;

   protected String membershipType = "type";

   protected String userName = "user";

   protected String newUserName = "newUser";

   protected String groupName1 = "group1";

   protected String groupName2 = "group2";

   /**
    * The list of users which have been created during test. 
    * Will be removed in tearDown() method.
    */
   private List<String> users = new ArrayList<String>();

   /**
    * The list of membership types which have been created during test. 
    * Will be removed in tearDown() method.
    */
   private List<String> types = new ArrayList<String>();

   /**
    * The list of group which have been created during test. 
    * Will be removed in tearDown() method.
    */
   private List<String> groups = new ArrayList<String>();

   /**
    * {@inheritDoc}
    */
   public void setUp() throws Exception
   {
      super.setUp();

      OrganizationService organizationService =
               (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

      gHandler = organizationService.getGroupHandler();
      uHandler = organizationService.getUserHandler();
      mHandler = organizationService.getMembershipHandler();
      mtHandler = organizationService.getMembershipTypeHandler();
      upHandler = organizationService.getUserProfileHandler();

      users.add(userName);
      users.add(newUserName);

      groups.add("/" + groupName1);
      groups.add("/" + groupName1 + "/" + groupName2);

      types.add(membershipType);
   }

   /**
    * Create new user for test purpose only.
    */
   protected void createUser(String userName) throws Exception
   {
      User u = uHandler.createUserInstance(userName);
      u.setEmail("email@test");
      u.setFirstName("first");
      u.setLastLoginTime(Calendar.getInstance().getTime());
      u.setCreatedDate(Calendar.getInstance().getTime());
      u.setLastName("last");
      u.setPassword("pwd");

      uHandler.createUser(u, true);

      users.add(userName);
   }

   /**
    * Create user with profile.
    */
   protected void createUserProfile(String userName) throws Exception
   {
      UserProfile up = upHandler.createUserProfileInstance(userName);
      up.setAttribute("key1", "value1");
      up.setAttribute("key2", "value2");
      upHandler.saveUserProfile(up, true);
   }

   /**
    * Create membership type.
    */
   protected void createMembershipType(String type, String desc) throws Exception
   {
      MembershipType mt = mtHandler.createMembershipTypeInstance();
      mt.setName(type);
      mt.setDescription(desc);
      mtHandler.createMembershipType(mt, true);

      types.add(type);
   }

   /**
    * Create new group.
    */
   protected void createGroup(String parentId, String name, String label, String desc) throws Exception
   {
      Group parent = gHandler.findGroupById(parentId);

      Group child = gHandler.createGroupInstance();
      child.setGroupName(name);
      child.setLabel(label);
      child.setDescription(desc);
      gHandler.addChild(parent, child, true);
      
      groups.add((parent == null ? "" : parentId) + "/" + name);
   }

   /**
    * Create membership.
    */
   protected void createMembership(String userName, String groupName, String type) throws Exception
   {
      createUser(userName);
      createGroup(null, groupName, "lable", "desc");
      createMembershipType(type, "desc");

      // link membership
      mHandler.linkMembership(uHandler.findUserByName(userName), gHandler.findGroupById("/" + groupName), mtHandler
               .findMembershipType(type), true);
   }

   /**
    * {@inheritDoc}
    */
   public void tearDown() throws Exception
   {
      // remove all users
      Iterator<String> iter = users.iterator();
      while (iter.hasNext())
      {
         String userName = iter.next();

         if (uHandler.findUserByName(userName) != null)
         {
            uHandler.removeUser(userName, true);
         }
         iter.remove();
      }

      // remove all membership types
      iter = types.iterator();
      while (iter.hasNext())
      {
         String type = iter.next();

         if (mtHandler.findMembershipType(type) != null)
         {
            mtHandler.removeMembershipType(type, true);
         }
         iter.remove();
      }

      // remove all groups
      iter = groups.iterator();
      while (iter.hasNext())
      {
         String groupId = iter.next();

         removeGroups(groupId);
         iter.remove();
      }

      super.tearDown();
   }

   private void removeGroups(String parentId) throws Exception
   {
      Group group = gHandler.findGroupById(parentId);
      if (group != null)
      {

         Collection<Group> childs = gHandler.findGroups(group);
         for (Group child : childs)
         {
            removeGroups(child.getId());
         }

         gHandler.removeGroup(group, true);
      }
   }
}
