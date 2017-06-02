/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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

import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.MembershipTypeHandler;

import java.util.Date;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFormatException;

/**
 * Util class which contains common function for JCR organization service
 * implementation.
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br>
 * Date: 7 10 2008
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 */
public class Utils
{
   private JCROrganizationServiceImpl service;

   /**
    * Utils constructor.
    */
   Utils(JCROrganizationServiceImpl service)
   {
      this.service = service;
   }

   /**
    * Returns property value represented in {@link Date}.
    * 
    * @param node
    *          the parent node
    * @param propertyName
    *          the property name to read from
    * @return the date value if property exists or null otherwise
    * @throws OrganizationServiceException
    *           if unexpected exception is occurred during reading
    */
   Date readDate(Node node, String propertyName) throws OrganizationServiceException
   {
      try
      {
         return node.getProperty(propertyName).getDate().getTime();
      }
      catch (PathNotFoundException e)
      {
         return null;
      }
      catch (ValueFormatException e)
      {
         throw new OrganizationServiceException("Property " + propertyName + " contains wrong value format", e);
      }
      catch (RepositoryException e)
      {
         throw new OrganizationServiceException("Can not read property " + propertyName, e);
      }
   }

   /**
    * Returns property value represented in {@link String}.
    * 
    * @param node
    *          the parent node
    * @param propertyName
    *          the property name to read from
    * @return the string value if property exists or null otherwise
    * @throws OrganizationServiceException
    *           if unexpected exception is occurred during reading
    */
   String readString(Node node, String propertyName) throws OrganizationServiceException
   {
      try
      {
         return node.getProperty(propertyName).getString();
      }
      catch (PathNotFoundException e)
      {
         return null;
      }
      catch (ValueFormatException e)
      {
         throw new OrganizationServiceException("Property " + propertyName + " contains wrong value format", e);
      }
      catch (RepositoryException e)
      {
         throw new OrganizationServiceException("Can not read property " + propertyName, e);
      }
   }

   /**
    * Compose membership identifier. For more information see {@link MembershipImpl}
    */
   String composeMembershipId(Node groupNode, Node refUserNode, Node refTypeNode) throws RepositoryException
   {
      return groupNode.getUUID()
         + ','
         + refUserNode.getName()
         + ','
         + (refTypeNode.getName().equals(JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY)
            ? MembershipTypeHandler.ANY_MEMBERSHIP_TYPE : refTypeNode.getName());
   }

   /**
    * Returns the node where all users are stored.
    */
   Node getUsersStorageNode(Session session) throws PathNotFoundException, RepositoryException
   {
      return (Node)session.getItem(service.getStoragePath() + "/" + JCROrganizationServiceImpl.STORAGE_JOS_USERS);
   }

   /**
    * Returns the node where all membership types are stored.
    */
   Node getMembershipTypeStorageNode(Session session) throws PathNotFoundException, RepositoryException
   {
      return (Node)session.getItem(service.getStoragePath() + "/"
         + JCROrganizationServiceImpl.STORAGE_JOS_MEMBERSHIP_TYPES);
   }

   /**
    * Returns the node where defined user is stored.
    */
   Node getUserNode(Session session, String userName) throws PathNotFoundException, RepositoryException
   {
      return (Node)session.getItem(getUserNodePath(userName));
   }

   /**
    * Returns the node path where defined user is stored.
    */
   String getUserNodePath(String userName) throws RepositoryException
   {
      return service.getStoragePath() + "/" + JCROrganizationServiceImpl.STORAGE_JOS_USERS + "/" + userName;
   }

   /**
    * Returns the node where defined membership type is stored.
    */
   Node getMembershipTypeNode(Session session, String name) throws PathNotFoundException, RepositoryException
   {
      return (Node)session.getItem(getMembershipTypeNodePath(name));
   }

   /**
    * Returns the node where profile of defined user is stored.
    */
   Node getProfileNode(Session session, String userName) throws PathNotFoundException, RepositoryException
   {
      return (Node)session.getItem(service.getStoragePath() + "/" + JCROrganizationServiceImpl.STORAGE_JOS_USERS + "/"
         + userName + "/" + JCROrganizationServiceImpl.JOS_PROFILE);
   }

   /**
    * Returns the node where defined group is stored.
    */
   Node getGroupNode(Session session, Group group) throws PathNotFoundException, RepositoryException
   {
      return getGroupNode(session, group == null ? "" : group.getId());
   }

   /**
    * Returns the node where defined group is stored.
    */
   Node getGroupNode(Session session, String groupId) throws PathNotFoundException, RepositoryException
   {
      return (Node)session.getItem(service.getStoragePath() + "/" + JCROrganizationServiceImpl.STORAGE_JOS_GROUPS
         + groupId);
   }

   /**
    * Returns the node where all groups are stored.
    */
   String getGroupStoragePath() throws RepositoryException
   {
      return service.getStoragePath() + "/" + JCROrganizationServiceImpl.STORAGE_JOS_GROUPS;
   }

   /**
    * Returns the path where defined membership type is stored.
    */
   String getMembershipTypeNodePath(String name) throws RepositoryException
   {
      return service.getStoragePath()
         + "/"
         + JCROrganizationServiceImpl.STORAGE_JOS_MEMBERSHIP_TYPES
         + "/"
         + (name.equals(MembershipTypeHandler.ANY_MEMBERSHIP_TYPE)
            ? JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY : name);
   }

   /**
    * Evaluate the group identifier and parent group identifier based on group node path.
    */
   GroupIds getGroupIds(Node groupNode) throws RepositoryException
   {
      String storagePath = getGroupStoragePath();
      String nodePath = groupNode.getPath();

      String groupId = nodePath.substring(storagePath.length());
      String parentId = groupId.substring(0, groupId.lastIndexOf("/"));

      return new GroupIds(groupId, parentId);
   }

   /**
    * Splits membership identifier into several components.
    */
   IdComponents splitId(String id) throws IndexOutOfBoundsException
   {
      String[] membershipIDs = id.split(",");

      String groupNodeId = membershipIDs[0];
      String userName = membershipIDs[1];
      String type = membershipIDs[2];

      return new IdComponents(groupNodeId, userName, type);
   }

   /**
    * Wraps membership identifier components. 
    */
   class IdComponents
   {
      final String groupNodeId;

      final String userName;

      final String type;

      private IdComponents(String groupNodeId, String userName, String type)
      {
         this.groupNodeId = groupNodeId;
         this.userName = userName;
         this.type = type;
      }
   }

   /**
    * Wraps group identifier and parent group identifier in 
    * one common entity.
    */
   class GroupIds
   {
      final String groupId;

      final String parentId;

      private GroupIds(String groupId, String parentId)
      {
         this.groupId = groupId;
         this.parentId = parentId;
      }
   }
}
