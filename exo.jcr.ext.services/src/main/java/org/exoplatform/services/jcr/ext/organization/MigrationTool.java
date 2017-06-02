/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 04.07.2012
 * 
 * @author <a href="mailto:dvishinskiy@exoplatform.com">Dmitriy Vishinskiy</a>
 * @version $Id: JCROrganizationServiceMigration.java 76870 2012-07-04 10:38:54Z dkuleshov $
 */

public class MigrationTool
{
   private JCROrganizationServiceImpl service;

   /**
    * Path where old structure will be moved.
    */
   private String storagePathOld;

   /**
    * Path in old structure where user nodes stored.
    */
   private String usersStorageOld;

   /**
    * Path in old structure where group nodes stored.
    */
   private String groupsStorageOld;

   /**
    * Path in old structure where membershipTypes nodes stored.
    */
   private String membershipTypesStorageOld;

   /**
    * The child nodes of user node where memberships are stored (old structure).
    */
   public static final String JOS_USER_MEMBERSHIP = "jos:userMembership";

   /**
    * The property of user node where group node uuid is stored (old structure).
    */
   public static final String JOS_GROUP = "jos:group";

   /**
    * The child node of user node where attributes is stored (old structure).
    */
   public static final String JOS_ATTRIBUTES = "jos:attributes";

   /**
    * The nodetype of old organization structure root node.
    */
   public static final String JOS_ORGANIZATION_NODETYPE_OLD = "jos:organizationStorage";

   /**
    * The property of a group node where parent id is stored (old structure).
    */
   public static final String JOS_PARENT_ID = "jos:parentId";

   /**
    * The property of a membership node where group id is stored (old structure).
    */
   public static final String JOS_GROUP_ID = "jos:groupId";

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo-jcr-services.MigrationTool");

   /**
    * MigrationTool constructor.
    * @throws RepositoryException 
    */
   MigrationTool(JCROrganizationServiceImpl service) throws RepositoryException
   {
      this.service = service;

      storagePathOld = service.getStoragePath() + "-old";
      usersStorageOld = storagePathOld + "/" + JCROrganizationServiceImpl.STORAGE_JOS_USERS;
      groupsStorageOld = storagePathOld + "/" + JCROrganizationServiceImpl.STORAGE_JOS_GROUPS;
      membershipTypesStorageOld = storagePathOld + "/" + JCROrganizationServiceImpl.STORAGE_JOS_MEMBERSHIP_TYPES;
   }

   /**
    * Method that aggregates all needed migration operations in needed order.
    * @throws RepositoryException
    */
   void migrate() throws RepositoryException
   {
      try
      {
         LOG.info("Migration started.");

         moveOldStructure();
         service.createStructure();

         //Migration order is important due to removal of nodes.
         migrateGroups();
         migrateMembershipTypes();
         migrateUsers();
         migrateProfiles();
         migrateMemberships();

         removeOldStructure();

         LOG.info("Migration completed.");
      }
      catch (Exception e)//NOSONAR
      {
         throw new RepositoryException("Migration failed", e);
      }
   }

   /**
    * Method to know if migration is need.
    * @return true if migration is need false otherwise.
    * @throws RepositoryException 
    */
   boolean migrationRequired() throws RepositoryException
   {
      Session session = service.getStorageSession();

      try
      {
         if (session.itemExists(storagePathOld))
         {
            return true;
         }

         try
         {
            Node node = (Node)session.getItem(service.getStoragePath());
            return node.isNodeType(JOS_ORGANIZATION_NODETYPE_OLD);
         }
         catch (PathNotFoundException e)
         {
            return false;
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for moving old storage into temporary location.
    * @throws Exception
    */
   private void moveOldStructure() throws Exception
   {
      ExtendedSession session = (ExtendedSession)service.getStorageSession();
      try
      {
         if (session.itemExists(storagePathOld))
         {
            return;
         }
         else
         {
            session.move(service.getStoragePath(), storagePathOld, false);
            session.save();
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for removing old storage from temporary location.
    * @throws RepositoryException
    */
   private void removeOldStructure() throws RepositoryException
   {
      ExtendedSession session = (ExtendedSession)service.getStorageSession();
      try
      {
         if (session.itemExists(storagePathOld))
         {
            NodeIterator usersIter = ((ExtendedNode)session.getItem(usersStorageOld)).getNodesLazily();
            while (usersIter.hasNext())
            {
               Node currentUser = usersIter.nextNode();
               currentUser.remove();
               session.save();
            }

            NodeIterator groupsIter = ((ExtendedNode)session.getItem(groupsStorageOld)).getNodesLazily();
            while (groupsIter.hasNext())
            {
               Node currentGroup = groupsIter.nextNode();
               currentGroup.remove();
               session.save();
            }

            NodeIterator membershipTypesIter =
               ((ExtendedNode)session.getItem(membershipTypesStorageOld)).getNodesLazily();
            while (membershipTypesIter.hasNext())
            {
               Node currentMembershipType = membershipTypesIter.nextNode();
               currentMembershipType.remove();
               session.save();
            }

            session.getItem(storagePathOld).remove();
            session.save();
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for users migration.
    * @throws Exception
    */
   private void migrateUsers() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         if (session.itemExists(usersStorageOld))
         {
            NodeIterator iterator = ((ExtendedNode)session.getItem(usersStorageOld)).getNodesLazily();
            UserHandlerImpl uh = ((UserHandlerImpl)service.getUserHandler());
            while (iterator.hasNext())
            {
               uh.migrateUser(iterator.nextNode());
            }
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for groups migration. Must be run after users and membershipTypes migration.
    * @throws Exception
    */
   private void migrateGroups() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         if (session.itemExists(groupsStorageOld))
         {
            NodeIterator iterator = ((ExtendedNode)session.getItem(groupsStorageOld)).getNodesLazily();
            GroupHandlerImpl gh = ((GroupHandlerImpl)service.getGroupHandler());
            while (iterator.hasNext())
            {
               Node oldGroupNode = iterator.nextNode();
               gh.migrateGroup(oldGroupNode);
               migrateGroups(oldGroupNode);
            }
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for groups migration.
    * @throws Exception
    */
   private void migrateGroups(Node startNode) throws Exception
   {
      NodeIterator iterator = ((ExtendedNode)startNode).getNodesLazily();
      GroupHandlerImpl gh = ((GroupHandlerImpl)service.getGroupHandler());
      while (iterator.hasNext())
      {
         Node oldGroupNode = iterator.nextNode();
         gh.migrateGroup(oldGroupNode);

         migrateGroups(oldGroupNode);
      }
   }

   /**
    * Method for membershipTypes migration.
    * @throws Exception
    */
   private void migrateMembershipTypes() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         if (session.itemExists(membershipTypesStorageOld))
         {
            NodeIterator iterator = ((ExtendedNode)session.getItem(membershipTypesStorageOld)).getNodesLazily();
            MembershipTypeHandlerImpl mth = ((MembershipTypeHandlerImpl)service.getMembershipTypeHandler());
            while (iterator.hasNext())
            {
               Node oldTypeNode = iterator.nextNode();
               mth.migrateMembershipType(oldTypeNode);
            }
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for profiles migration.
    * @throws Exception
    */
   private void migrateProfiles() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         if (session.itemExists(usersStorageOld))
         {
            NodeIterator iterator = ((ExtendedNode)session.getItem(usersStorageOld)).getNodesLazily();
            UserProfileHandlerImpl uph = ((UserProfileHandlerImpl)service.getUserProfileHandler());
            while (iterator.hasNext())
            {
               Node oldUserNode = iterator.nextNode();
               uph.migrateProfile(oldUserNode);
            }
         }
      }
      finally
      {
         session.logout();
      }

   }

   /**
    * Method for memberships migration.
    * @throws Exception
    */
   private void migrateMemberships() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         if (session.itemExists(usersStorageOld))
         {
            NodeIterator iterator = ((ExtendedNode)session.getItem(usersStorageOld)).getNodesLazily();
            MembershipHandlerImpl mh = ((MembershipHandlerImpl)service.getMembershipHandler());
            while (iterator.hasNext())
            {
               Node oldUserNode = iterator.nextNode();
               mh.migrateMemberships(oldUserNode);
               oldUserNode.remove();
               session.save();
            }
         }
      }
      finally
      {
         session.logout();
      }
   }

}
