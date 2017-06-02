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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.organization.CacheHandler;
import org.exoplatform.services.organization.CacheHandler.CacheType;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.GroupEventListenerHandler;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.security.PermissionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: GroupHandlerImpl.java 79575 2012-02-17 13:23:37Z aplotnikov $
 */
public class GroupHandlerImpl extends JCROrgServiceHandler implements GroupHandler, GroupEventListenerHandler
{

   /**
    * The list of listeners to broadcast events.
    */
   protected final List<GroupEventListener> listeners = new ArrayList<GroupEventListener>();

   /**
    * Class contains names of group properties.
    */
   public static class GroupProperties
   {
      /**
       * The group property that contain description.
       */
      public static final String JOS_DESCRIPTION = "jos:description";

      /**
       * The group property that contain label.
       */
      public static final String JOS_LABEL = "jos:label";

   }

   /**
    * GroupHandlerImpl constructor.
    */
   GroupHandlerImpl(JCROrganizationServiceImpl service)
   {
      super(service);
   }

   /**
    * {@inheritDoc}
    */
   public void addChild(Group parent, Group child, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         addChild(session, (GroupImpl)parent, (GroupImpl)child, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Adds child group.
    */
   private void addChild(Session session, GroupImpl parent, GroupImpl child, boolean broadcast) throws Exception
   {
      Node parentNode = utils.getGroupNode(session, parent);
      Node groupNode =
         parentNode.addNode(child.getGroupName(), JCROrganizationServiceImpl.JOS_HIERARCHY_GROUP_NODETYPE);

      String parentId = parent == null ? null : parent.getId();

      child.setParentId(parentId);
      child.setInternalId(groupNode.getUUID());

      if (broadcast)
      {
         preSave(child, true);
      }

      writeGroup(child, groupNode);
      session.save();

      putInCache(child);

      if (broadcast)
      {
         postSave(child, true);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void createGroup(Group group, boolean broadcast) throws Exception
   {
      addChild(null, group, broadcast);
   }

   /**
    * {@inheritDoc}
    */
   public Group createGroupInstance()
   {
      return new GroupImpl();
   }

   /**
    * {@inheritDoc}
    */
   public Group findGroupById(String groupId) throws Exception
   {
      Group group = getFromCache(groupId);
      if (group != null)
      {
         return group;
      }

      Session session = service.getStorageSession();
      try
      {
         Node groupNode;
         try
         {
            groupNode = utils.getGroupNode(session, groupId);
         }
         catch (PathNotFoundException e)
         {
            return null;
         }

         group = readGroup(groupNode);
         putInCache(group);
      }
      finally
      {
         session.logout();
      }

      return group;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Group> findGroupByMembership(String userName, String membershipType) throws Exception
   {
      List<Group> groups = new ArrayList<Group>();

      Session session = service.getStorageSession();
      try
      {
         Node userNode;
         try
         {
            userNode = utils.getUserNode(session, userName);
         }
         catch (PathNotFoundException e)
         {
            return new ArrayList<Group>();
         }

         PropertyIterator refUserProps = userNode.getReferences();
         while (refUserProps.hasNext())
         {
            Node refUserNode = refUserProps.nextProperty().getParent();
            if (membershipType == null
               || refUserNode.hasNode(membershipType.equals(MembershipTypeHandler.ANY_MEMBERSHIP_TYPE)
                  ? JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY : membershipType))
            {
               Node groupNode = refUserNode.getParent().getParent();
               groups.add(readGroup(groupNode));
            }
         }
      }
      finally
      {
         session.logout();
      }

      return groups;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Group> resolveGroupByMembership(String userName, String membershipType) throws Exception
   {
      List<Group> groups = new ArrayList<Group>();

      Session session = service.getStorageSession();
      try
      {
         Node userNode;
         try
         {
            userNode = utils.getUserNode(session, userName);
         }
         catch (PathNotFoundException e)
         {
            return new ArrayList<Group>();
         }

         PropertyIterator refUserProps = userNode.getReferences();
         while (refUserProps.hasNext())
         {
            Node refUserNode = refUserProps.nextProperty().getParent();
            if (membershipType == null || refUserNode.hasNode(membershipType)
               || refUserNode.hasNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY))
            {
               Node groupNode = refUserNode.getParent().getParent();
               groups.add(readGroup(groupNode));
            }
         }
      }
      finally
      {
         session.logout();
      }

      return groups;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Group> findGroups(Group parent) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findGroups(session, parent, false);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Find children groups. Parent group is not included in result.
    * 
    * @param recursive
    *          if true, the search should be recursive. It means go down to the tree 
    *          until bottom will reach
    */
   private Collection<Group> findGroups(Session session, Group parent, boolean recursive) throws Exception
   {
      List<Group> groups = new ArrayList<Group>();
      String parentId = parent == null ? "" : parent.getId();

      NodeIterator childNodes = utils.getGroupNode(session, parentId).getNodes();

      while (childNodes.hasNext())
      {
         Node groupNode = childNodes.nextNode();
         if (!groupNode.getName().startsWith(JCROrganizationServiceImpl.JOS_MEMBERSHIP))
         {
            Group group = readGroup(groupNode);
            groups.add(group);

            if (recursive)
            {
               groups.addAll(findGroups(session, group, recursive));
            }
         }
      }

      return groups;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Group> findGroupsOfUser(String user) throws Exception
   {
      return findGroupByMembership(user, null);
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Group> getAllGroups() throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findGroups(session, null, true);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Group removeGroup(Group group, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeGroup(session, group, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Removes group and all related membership entities. Throws exception if children exist.
    */
   private Group removeGroup(Session session, Group group, boolean broadcast) throws Exception
   {
      if (group == null)
      {
         throw new OrganizationServiceException("Can not remove group, since it is null");
      }

      Node groupNode = utils.getGroupNode(session, group);

      // need to minus one because of child "jos:memberships" node
      long childrenCount = ((ExtendedNode)groupNode).getNodesLazily(1).getSize() - 1;

      if (childrenCount > 0)
      {
         throw new OrganizationServiceException("Can not remove group till children exist");
      }

      if (broadcast)
      {
         preDelete(group);
      }

      removeMemberships(groupNode, broadcast);
      groupNode.remove();
      session.save();

      removeFromCache(group.getId());
      removeAllRelatedFromCache(group.getId());

      if (broadcast)
      {
         postDelete(group);
      }

      return group;
   }

   /**
    * Remove all membership entities related to current group.
    */
   private void removeMemberships(Node groupNode, boolean broadcast) throws RepositoryException
   {
      NodeIterator refUsers = groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP).getNodes();
      while (refUsers.hasNext())
      {
         refUsers.nextNode().remove();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void saveGroup(Group group, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         Node groupNode = utils.getGroupNode(session, group);

         if (broadcast)
         {
            preSave(group, false);
         }

         writeGroup(group, groupNode);
         session.save();

         putInCache(group);

         if (broadcast)
         {
            postSave(group, false);
         }
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for group migration.
    * @param oldGroupNode 
    *         the node where group properties are stored (from old structure)
    */
   void migrateGroup(Node oldGroupNode) throws Exception
   {
      String groupName = oldGroupNode.getName();
      String desc = utils.readString(oldGroupNode, GroupProperties.JOS_DESCRIPTION);
      String label = utils.readString(oldGroupNode, GroupProperties.JOS_LABEL);
      String parentId = utils.readString(oldGroupNode, MigrationTool.JOS_PARENT_ID);

      GroupImpl group = new GroupImpl(groupName, parentId);
      group.setDescription(desc);
      group.setLabel(label);

      Group parentGroup = findGroupById(group.getParentId());

      if (findGroupById(group.getId()) != null)
      {
         removeGroup(group, false);
      }

      addChild(parentGroup, group, false);
   }

   /**
    * Read group properties from node.
    * 
    * @param groupNode
    *          the node where group properties are stored
    * @return {@link Group}
    * @throws OrganizationServiceException
    *          if unexpected exception is occurred during reading
    */
   private Group readGroup(Node groupNode) throws Exception
   {
      String groupName = groupNode.getName();
      String desc = utils.readString(groupNode, GroupProperties.JOS_DESCRIPTION);
      String label = utils.readString(groupNode, GroupProperties.JOS_LABEL);
      String parentId = utils.getGroupIds(groupNode).parentId;

      GroupImpl group = new GroupImpl(groupName, parentId);
      group.setInternalId(groupNode.getUUID());
      group.setDescription(desc);
      group.setLabel(label);

      return group;
   }

   /**
    * Write group properties to the node.
    * 
    * @param groupNode
    *          the node where group properties are stored
    * @return {@link Group}
    * @throws OrganizationServiceException
    *          if unexpected exception is occurred during writing
    */
   private void writeGroup(Group group, Node node) throws OrganizationServiceException
   {
      try
      {
         node.setProperty(GroupProperties.JOS_LABEL, group.getLabel());
         node.setProperty(GroupProperties.JOS_DESCRIPTION, group.getDescription());
      }
      catch (RepositoryException e)
      {
         throw new OrganizationServiceException("Can not write group properties", e);
      }
   }

   /**
    * Reads group from cache. 
    */
   private Group getFromCache(String groupId)
   {
      return (Group)cache.get(groupId, CacheType.GROUP);
   }

   /**
    * Puts group in cache. 
    */
   private void putInCache(Group group)
   {
      cache.put(group.getId(), group, CacheType.GROUP);
   }

   /**
    * Removes group from cache.
    */
   private void removeFromCache(String groupId)
   {
      cache.remove(groupId, CacheType.GROUP);
   }

   /**
    * Removes related entities from cache.
    */
   private void removeAllRelatedFromCache(String groupId)
   {
      cache.remove(CacheHandler.GROUP_PREFIX + groupId, CacheType.MEMBERSHIP);
   }

   /**
    * Notifying listeners before group creation.
    * 
    * @param group 
    *          the group which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new group, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preSave(Group group, boolean isNew) throws Exception
   {
      for (GroupEventListener listener : listeners)
      {
         listener.preSave(group, isNew);
      }
   }

   /**
    * Notifying listeners after group creation.
    * 
    * @param group 
    *          the group which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new group, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postSave(Group group, boolean isNew) throws Exception
   {
      for (GroupEventListener listener : listeners)
      {
         listener.postSave(group, isNew);
      }
   }

   /**
    * Notifying listeners before group deletion.
    * 
    * @param group 
    *          the group which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preDelete(Group group) throws Exception
   {
      for (GroupEventListener listener : listeners)
      {
         listener.preDelete(group);
      }
   }

   /**
    * Notifying listeners after group deletion.
    * 
    * @param group 
    *          the group which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postDelete(Group group) throws Exception
   {
      for (GroupEventListener listener : listeners)
      {
         listener.postDelete(group);
      }
   }

   /**
    * Remove registered listener.
    * 
    * @param listener The registered listener for removing
    */
   public void removeGroupEventListener(GroupEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void addGroupEventListener(GroupEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public List<GroupEventListener> getGroupListeners()
   {
      return Collections.unmodifiableList(listeners);
   }
}
