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

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.organization.Utils.IdComponents;
import org.exoplatform.services.organization.CacheHandler;
import org.exoplatform.services.organization.CacheHandler.CacheType;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.MembershipEventListenerHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.impl.mock.SimpleMembershipListAccess;
import org.exoplatform.services.security.PermissionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.naming.InvalidNameException;

/**
 * The most important thing is how memberships are stored in JCR. Once developer
 * invokes one of {@link #createMembership(Membership, boolean)} or 
 * {@link #linkMembership(User, Group, MembershipType, boolean)} methods the membership will be
 * represented in JCR through several nodes and properties. Every group node has mandatory 
 * <code>{@link JCROrganizationServiceImpl#JOS_MEMBERSHIP} node</code> to where adding the node
 * with user name and reference property pointed to user node. Than new node with name of membership type
 * with reference property is added to this node the same way and is pointed to membership type node.
 * This adds the ability to manage finding memberships by different filters in the most simple way possible.
 * 
 * <br>Created by The eXo Platform SAS. NOTE: Check if nodetypes and/or existing
 * interfaces of API don't relate one to other. Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: MembershipHandlerImpl.java 79575 2012-02-17 13:23:37Z aplotnikov $
 */
public class MembershipHandlerImpl extends JCROrgServiceHandler implements MembershipHandler,
   MembershipEventListenerHandler
{

   /**
    * Merely contains membership related properties.
    */
   public static class MembershipProperties
   {
      /**
       * The property name that contains reference to linked membership type.
       */
      public static final String JOS_MEMBERSHIP_TYPE = "jos:membershipType";

      /**
       * The property name that contains reference to linked user.
       */
      public static final String JOS_USER = "jos:user";
   }

   /**
    * The list of listeners to broadcast the events.
    */
   protected final List<MembershipEventListener> listeners = new ArrayList<MembershipEventListener>();

   /**
    * MembershipHandlerImpl constructor.
    */
   MembershipHandlerImpl(JCROrganizationServiceImpl service)
   {
      super(service);
   }

   /**
    * {@inheritDoc}
    */
   public void createMembership(Membership m, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         createMembership(session, (MembershipImpl)m, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Persist new membership.
    */
   private void createMembership(Session session, MembershipImpl membership, boolean broadcast)
      throws InvalidNameException, Exception
   {
      Node userNode;
      try
      {
         userNode = utils.getUserNode(session, membership.getUserName());
      }
      catch (PathNotFoundException e)
      {
         throw new InvalidNameException("The user " + membership.getUserName() + " does not exist");
      }

      Node groupNode;
      try
      {
         groupNode = utils.getGroupNode(session, membership.getGroupId());
      }
      catch (PathNotFoundException e)
      {
         throw new InvalidNameException("The group " + membership.getGroupId() + " does not exist");
      }

      Node typeNode;
      String membershipType =
         membership.getMembershipType().equals(MembershipTypeHandler.ANY_MEMBERSHIP_TYPE)
            ? JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY : membership.getMembershipType();
      try
      {
         typeNode = utils.getMembershipTypeNode(session, membershipType);
      }
      catch (PathNotFoundException e)
      {
         throw new InvalidNameException("The membership type " + membership.getMembershipType() + " does not exist");
      }

      Node membershipStorageNode = groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP);

      Node refUserNode;
      try
      {
         refUserNode = membershipStorageNode.addNode(membership.getUserName());
         refUserNode.setProperty(MembershipProperties.JOS_USER, userNode);
      }
      catch (ItemExistsException e)
      {
         refUserNode = membershipStorageNode.getNode(membership.getUserName());
      }

      Node refTypeNode;
      try
      {
         refTypeNode = refUserNode.addNode(membershipType);
         refTypeNode.setProperty(MembershipProperties.JOS_MEMBERSHIP_TYPE, typeNode);
      }
      catch (ItemExistsException e)
      {
         // the membership already exists
         return;
      }

      String id = utils.composeMembershipId(groupNode, refUserNode, refTypeNode);
      membership.setId(id);

      if (broadcast)
      {
         preSave(membership, true);
      }

      session.save();
      putInCache(membership);

      if (broadcast)
      {
         postSave(membership, true);
      }
   }

   /**
    * {@inheritDoc}
    */
   public Membership createMembershipInstance()
   {
      return new MembershipImpl();
   }

   /**
    * {@inheritDoc}
    */
   public Membership findMembership(String id) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembership(session, id).membership;
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to search for an membership record with the given id.
    */
   private MembershipByUserGroupTypeWrapper findMembership(Session session, String id) throws Exception
   {
      IdComponents ids;
      try
      {
         ids = utils.splitId(id);
      }
      catch (IndexOutOfBoundsException e)
      {
         throw new ItemNotFoundException("Can not find membership by id=" + id, e);
      }

      Node groupNode = session.getNodeByUUID(ids.groupNodeId);
      Node refUserNode = groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP).getNode(ids.userName);
      Node refTypeNode =
         refUserNode.getNode(ids.type.equals(MembershipTypeHandler.ANY_MEMBERSHIP_TYPE)
            ? JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY : ids.type);

      String groupId = utils.getGroupIds(groupNode).groupId;

      MembershipImpl membership = new MembershipImpl();
      membership.setId(id);
      membership.setGroupId(groupId);
      membership.setMembershipType(ids.type);
      membership.setUserName(ids.userName);

      putInCache(membership);

      return new MembershipByUserGroupTypeWrapper(membership, refUserNode, refTypeNode);
   }

   /**
    * {@inheritDoc}
    */
   public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipByUserGroupAndType(session, userName, groupId, type);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to search for a specific membership type of an user in a group.
    */
   private Membership findMembershipByUserGroupAndType(Session session, String userName, String groupId, String type)
      throws Exception
   {
      MembershipImpl membership = getFromCache(userName, groupId, type);
      if (membership != null)
      {
         return membership;
      }

      try
      {
         Node groupNode = utils.getGroupNode(session, groupId);

         Node refUserNode = groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP).getNode(userName);
         Node refTypeNode =
            refUserNode.getNode(type.equals(MembershipTypeHandler.ANY_MEMBERSHIP_TYPE)
               ? JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY : type);

         String id = utils.composeMembershipId(groupNode, refUserNode, refTypeNode);

         membership = new MembershipImpl();
         membership.setGroupId(groupId);
         membership.setUserName(userName);
         membership.setMembershipType(type);
         membership.setId(id);

         putInCache(membership);

         return membership;
      }
      catch (PathNotFoundException e)
      {
         return null;
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Membership> findMembershipsByGroup(Group group) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipsByGroup(session, group);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to find all the membership in a group. 
    */
   private Collection<Membership> findMembershipsByGroup(Session session, Group group) throws Exception
   {
      Node groupNode;
      NodeIterator refUsers;
      try
      {
         groupNode = utils.getGroupNode(session, group);
         refUsers = groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP).getNodes();
      }
      catch (PathNotFoundException e)
      {
         return new ArrayList<Membership>();
      }

      List<Membership> memberships = new ArrayList<Membership>();
      while (refUsers.hasNext())
      {
         Node refUserNode = refUsers.nextNode();
         memberships.addAll(findMembershipsByUserAndGroup(session, refUserNode, groupNode));
      }

      return memberships;
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<Membership> findAllMembershipsByGroup(Group group) throws Exception
   {
      return new SimpleMembershipListAccess(findMembershipsByGroup(group));
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Membership> findMembershipsByUser(String userName) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipsByUser(session, userName).memberships;
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to find all the memberships of an user in any group.
    */
   private MembershipsByUserWrapper findMembershipsByUser(Session session, String userName) throws Exception
   {
      Node userNode;
      try
      {
         userNode = utils.getUserNode(session, userName);
      }
      catch (PathNotFoundException e)
      {
         return new MembershipsByUserWrapper(new ArrayList<Membership>(), new ArrayList<Node>());
      }

      List<Membership> memberships = new ArrayList<Membership>();
      List<Node> refUserNodes = new ArrayList<Node>();

      PropertyIterator refUserProps = userNode.getReferences();
      while (refUserProps.hasNext())
      {
         Node refUserNode = refUserProps.nextProperty().getParent();
         Node groupNode = refUserNode.getParent().getParent();

         memberships.addAll(findMembershipsByUserAndGroup(session, refUserNode, groupNode));
         refUserNodes.add(refUserNode);
      }

      return new MembershipsByUserWrapper(memberships, refUserNodes);
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Membership> findMembershipsByUserAndGroup(String userName, String groupId) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return findMembershipsByUserAndGroup(session, userName, groupId);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Use this method to find all the memberships of an user in a group.
    */
   private Collection<Membership> findMembershipsByUserAndGroup(Session session, String userName, String groupId) throws Exception
   {
      Node groupNode;
      Node refUserNode;

      try
      {
         groupNode = utils.getGroupNode(session, groupId);
         refUserNode = groupNode.getNode(JCROrganizationServiceImpl.JOS_MEMBERSHIP).getNode(userName);
      }
      catch (PathNotFoundException e)
      {
         return new ArrayList<Membership>();
      }

      return findMembershipsByUserAndGroup(session, refUserNode, groupNode);
   }

   /**
    * Use this method to find all the memberships of an user in a group.
    */
   private Collection<Membership> findMembershipsByUserAndGroup(Session session, Node refUserNode, Node groupNode) throws Exception
   {
      List<Membership> memberships = new ArrayList<Membership>();

      NodeIterator refTypes = refUserNode.getNodes();
      while (refTypes.hasNext())
      {
         Node refTypeNode = refTypes.nextNode();

         String id = utils.composeMembershipId(groupNode, refUserNode, refTypeNode);
         String groupId = utils.getGroupIds(groupNode).groupId;

         MembershipImpl membership = new MembershipImpl();
         membership.setUserName(refUserNode.getName());
         membership.setMembershipType((refTypeNode.getName().equals(JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY)
            ? MembershipTypeHandler.ANY_MEMBERSHIP_TYPE : refTypeNode.getName()));
         membership.setGroupId(groupId);
         membership.setId(id);

         memberships.add(membership);
      }

      return memberships;
   }

   /**
    * {@inheritDoc}
    */
   public void linkMembership(User user, Group group, MembershipType m, boolean broadcast) throws Exception
   {
      if (user == null)
      {
         throw new InvalidNameException("Can not create membership record because user is null");
      }

      if (group == null)
      {
         throw new InvalidNameException("Can not create membership record because group is null");
      }

      if (m == null)
      {
         throw new InvalidNameException("Can not create membership record because membership type is null");
      }
      Session session = service.getStorageSession();
      try
      {
         MembershipImpl membership = new MembershipImpl();
         membership.setMembershipType(m.getName());
         membership.setGroupId(group.getId());
         membership.setUserName(user.getUserName());

         createMembership(session, membership, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Migrates user memberships from old storage into new.
    * @param oldUserNode 
    *         the node where user properties are stored (from old structure)
    * @throws Exception
    */
   void migrateMemberships(Node oldUserNode) throws Exception
   {
      Session session = oldUserNode.getSession();
      NodeIterator iterator = ((ExtendedNode)oldUserNode).getNodesLazily();

      while (iterator.hasNext())
      {
         Node oldMembershipNode = iterator.nextNode();

         if (oldMembershipNode.isNodeType(MigrationTool.JOS_USER_MEMBERSHIP))
         {
            String oldGroupUUID = utils.readString(oldMembershipNode, MigrationTool.JOS_GROUP);
            String oldMembershipTypeUUID =
               utils.readString(oldMembershipNode, MembershipProperties.JOS_MEMBERSHIP_TYPE);
            String userName = oldUserNode.getName();
            String groupId = utils.readString(session.getNodeByUUID(oldGroupUUID), MigrationTool.JOS_GROUP_ID);
            String membershipTypeName = session.getNodeByUUID(oldMembershipTypeUUID).getName();

            User user = service.getUserHandler().findUserByName(userName);
            Group group = service.getGroupHandler().findGroupById(groupId);
            MembershipType mt = service.getMembershipTypeHandler().findMembershipType(membershipTypeName);

            Membership existingMembership = findMembershipByUserGroupAndType(userName, groupId, membershipTypeName);
            if (existingMembership != null)
            {
               removeMembership(existingMembership.getId(), false);
            }
            linkMembership(user, group, mt, false);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public Membership removeMembership(String id, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeMembership(session, id, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Remove memberships entity by identifier.
    */
   private Membership removeMembership(Session session, String id, boolean broadcast) throws Exception
   {
      MembershipByUserGroupTypeWrapper mWrapper;
      try
      {
         mWrapper = findMembership(session, id);
      }
      catch (ItemNotFoundException e)
      {
         return null;
      }
      catch (PathNotFoundException e)
      {
         return null;
      }

      if (broadcast)
      {
         preDelete(mWrapper.membership);
      }

      removeMembership(mWrapper.refUserNode, mWrapper.refTypeNode);
      session.save();

      removeFromCache(mWrapper.membership);

      if (broadcast)
      {
         postDelete(mWrapper.membership);
      }

      return mWrapper.membership;
   }

   /**
    * Remove membership record.
    */
   void removeMembership(Node refUserNode, Node refTypeNode) throws Exception
   {
      refTypeNode.remove();
      if (!refUserNode.hasNodes())
      {
         refUserNode.remove();
      }
   }

   /**
    * {@inheritDoc}
    */
   public Collection<Membership> removeMembershipByUser(String userName, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeMembershipByUser(session, userName, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Remove memberships entities related to current user.
    */
   private Collection<Membership> removeMembershipByUser(Session session, String userName, boolean broadcast) throws Exception
   {
      MembershipsByUserWrapper mWrapper = findMembershipsByUser(session, userName);

      if (broadcast)
      {
         for (Membership m : mWrapper.memberships)
         {
            preDelete(m);
         }
      }

      for (Node refUserNode : mWrapper.refUserNodes)
      {
         refUserNode.remove();
      }
      session.save();

      removeFromCache(CacheHandler.USER_PREFIX + userName);

      if (broadcast)
      {
         for (Membership m : mWrapper.memberships)
         {
            postDelete(m);
         }
      }

      return mWrapper.memberships;
   }

   /**
    * Gets membership entity from cache.
    */
   private MembershipImpl getFromCache(String userName, String groupId, String type)
   {
      return (MembershipImpl)cache.get(cache.getMembershipKey(userName, groupId, type), CacheType.MEMBERSHIP);
   }

   /**
    * Removes membership entities from cache.
    */
   private void removeFromCache(Membership membership)
   {
      cache.remove(cache.getMembershipKey(membership), CacheType.MEMBERSHIP);
   }

   /**
    * Removes membership entities from cache.
    */
   private void removeFromCache(String key)
   {
      cache.remove(key, CacheType.MEMBERSHIP);
   }

   /**
    * Adds membership entity into cache.
    */
   private void putInCache(MembershipImpl membership)
   {
      cache.put(cache.getMembershipKey(membership), membership, CacheType.MEMBERSHIP);
   }

   /**
    * Notifying listeners before membership creation.
    * 
    * @param membership 
    *          the membership which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new membership, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preSave(Membership membership, boolean isNew) throws Exception
   {
      for (MembershipEventListener listener : listeners)
      {
         listener.preSave(membership, isNew);
      }
   }

   /**
    * Notifying listeners after membership creation.
    * 
    * @param membership 
    *          the membership which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new membership, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postSave(Membership membership, boolean isNew) throws Exception
   {
      for (MembershipEventListener listener : listeners)
      {
         listener.postSave(membership, isNew);
      }
   }

   /**
    * Notifying listeners before membership deletion.
    * 
    * @param membership 
    *          the membership which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preDelete(Membership membership) throws Exception
   {
      for (MembershipEventListener listener : listeners)
      {
         listener.preDelete(membership);
      }
   }

   /**
    * Notifying listeners after membership deletion.
    * 
    * @param membership 
    *          the membership which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postDelete(Membership membership) throws Exception
   {
      for (MembershipEventListener listener : listeners)
      {
         listener.postDelete(membership);
      }
   }

   /**
    * Remove registered listener.
    * 
    * @param listener The registered listener
    */
   public void removeMembershipEventListener(MembershipEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void addMembershipEventListener(MembershipEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public List<MembershipEventListener> getMembershipListeners()
   {
      return Collections.unmodifiableList(listeners);
   }

   /**
    * Class wrapper, which wraps membership entity and nodes from which it was extracted (represented).
    */
   private class MembershipByUserGroupTypeWrapper
   {
      final Membership membership;

      final Node refUserNode;

      final Node refTypeNode;

      private MembershipByUserGroupTypeWrapper(Membership membership, Node refUserNode, Node refTypeNode)
      {
         this.membership = membership;
         this.refTypeNode = refTypeNode;
         this.refUserNode = refUserNode;
      }
   }

   /**
    * Class wrapper, which wraps memberships and user nodes from which they were extracted.
    */
   private class MembershipsByUserWrapper
   {
      final Collection<Membership> memberships;

      final Collection<Node> refUserNodes;

      private MembershipsByUserWrapper(Collection<Membership> memberships, Collection<Node> refUserNodes)
      {
         this.memberships = memberships;
         this.refUserNodes = refUserNodes;
      }
   }
}
