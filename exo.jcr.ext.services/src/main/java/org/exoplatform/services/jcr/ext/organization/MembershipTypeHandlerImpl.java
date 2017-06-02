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
import org.exoplatform.services.organization.CacheHandler;
import org.exoplatform.services.organization.CacheHandler.CacheType;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeEventListener;
import org.exoplatform.services.organization.MembershipTypeEventListenerHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.security.PermissionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: MembershipTypeHandlerImpl.java 79575 2012-02-17 13:23:37Z aplotnikov $
 */
public class MembershipTypeHandlerImpl extends JCROrgServiceHandler implements MembershipTypeHandler,
   MembershipTypeEventListenerHandler
{
   /**
    * The list of listeners to broadcast the events.
    */
   protected final List<MembershipTypeEventListener> listeners = new ArrayList<MembershipTypeEventListener>();

   /**
    * Class contains the names of membership type properties only.
    */
   public static class MembershipTypeProperties
   {
      /**
       * Membership type property that contains description.
       */
      public static final String JOS_DESCRIPTION = "jos:description";

      /**
       * Membership type property that contains created date.
       */
      public static final String EXO_DATE_CREATED  = "exo:dateCreated";

      /**
       * Membership type property that contains modified date.
       */
      public static final String EXO_DATE_MODIFIED = "exo:dateModified";
   }

   /**
    * MembershipTypeHandlerImpl constructor.
    */
   MembershipTypeHandlerImpl(JCROrganizationServiceImpl service)
   {
      super(service);
   }

   /**
    * {@inheritDoc}
    */
   public MembershipType createMembershipType(MembershipType mt, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return createMembershipType(session, (MembershipTypeImpl)mt, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Persists new membership type object.
    */
   private MembershipType createMembershipType(Session session, MembershipTypeImpl mt, boolean broadcast)
      throws Exception
   {
      Node storageTypesNode = utils.getMembershipTypeStorageNode(session);
      Node typeNode = storageTypesNode.addNode(mt.getName().equals(MembershipTypeHandler.ANY_MEMBERSHIP_TYPE)
         ? JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY : mt.getName());

      mt.setInternalId(typeNode.getUUID());

      if (broadcast)
      {
         preSave(mt, true);
      }

      writeMembershipType(mt, typeNode);
      session.save();

      putInCache(mt);

      if (broadcast)
      {
         postSave(mt, true);
      }

      return mt;
   }

   /**
    * {@inheritDoc}
    */
   public MembershipType createMembershipTypeInstance()
   {
      return new MembershipTypeImpl();
   }

   /**
    * {@inheritDoc}
    */
   public MembershipType findMembershipType(String name) throws Exception
   {
      MembershipType mt = getFromCache(name);
      if (mt != null)
      {
         return mt;
      }

      Session session = service.getStorageSession();
      try
      {
         return findMembershipType(session, name);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Find membership type.
    */
   private MembershipType findMembershipType(Session session, String name) throws Exception
   {
      Node membershipTypeNode;
      try
      {
         membershipTypeNode = utils.getMembershipTypeNode(session, name);
      }
      catch (PathNotFoundException e)
      {
         return null;
      }

      MembershipType mt = readMembershipType(membershipTypeNode);
      putInCache(mt);

      return mt;
   }

   /**
    * {@inheritDoc}
    */
   public Collection<MembershipType> findMembershipTypes() throws Exception
   {
      List<MembershipType> types = new ArrayList<MembershipType>();

      Session session = service.getStorageSession();
      try
      {
         NodeIterator membershipTypes = utils.getMembershipTypeStorageNode(session).getNodes();
         while (membershipTypes.hasNext())
         {
            MembershipType type = readMembershipType(membershipTypes.nextNode());
            types.add(type);
         }
         Collections.sort(types, MembershipTypeHandler.COMPARATOR);
      }
      finally
      {
         session.logout();
      }

      return types;
   }

   /**
    * {@inheritDoc}
    */
   public MembershipType removeMembershipType(String name, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeMembershipType(session, name, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Removing membership type and related membership entities.
    */
   private MembershipType removeMembershipType(Session session, String name, boolean broadcast)
      throws RepositoryException, Exception
   {
      Node membershipTypeNode = utils.getMembershipTypeNode(session, name);
      MembershipType type = readMembershipType(membershipTypeNode);

      if (broadcast)
      {
         preDelete(type);
      }

      removeMemberships(membershipTypeNode);
      membershipTypeNode.remove();
      session.save();

      removeFromCache(name);
      removeAllRelatedFromCache(name);

      if (broadcast)
      {
         postDelete(type);
      }

      return type;
   }

   /**
    * Removes related membership entity.
    */
   private void removeMemberships(Node membershipTypeNode) throws Exception
   {
      PropertyIterator refTypes = membershipTypeNode.getReferences();
      while (refTypes.hasNext())
      {
         Property refTypeProp = refTypes.nextProperty();

         Node refTypeNode = refTypeProp.getParent();
         Node refUserNode = refTypeNode.getParent();

         membershipHandler.removeMembership(refUserNode, refTypeNode);
      }
   }

   /**
    * {@inheritDoc}
    */
   public MembershipType saveMembershipType(MembershipType mt, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return saveMembershipType(session, (MembershipTypeImpl)mt, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Method for membership type migration.
    * @param oldMembershipTypeNode
    *         the node where membershipType properties are stored (from old structure)
    * @throws Exception
    */
   void migrateMembershipType(Node oldMembershipTypeNode) throws Exception
   {
      MembershipType membershipType = readMembershipType(oldMembershipTypeNode);

      if (findMembershipType(membershipType.getName()) != null)
      {
         removeMembershipType(membershipType.getName(), false);
      }

      createMembershipType(membershipType, false);
   }

   /**
    * Persists new membership type entity.
    */
   private MembershipType saveMembershipType(Session session, MembershipTypeImpl mType, boolean broadcast)
      throws Exception
   {
      Node mtNode = getOrCreateMembershipTypeNode(session, mType);

      boolean isNew = mtNode.isNew();

      if (broadcast)
      {
         preSave(mType, isNew);
      }

      String oldType =
         mtNode.getName().equals(JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY) ? ANY_MEMBERSHIP_TYPE : mtNode
            .getName();
      String newType = mType.getName();

      if (!oldType.equals(newType))
      {
         String oldPath = mtNode.getPath();
         String newPath = utils.getMembershipTypeNodePath(newType);

         session.move(oldPath, newPath);

         moveMembershipsInCache(oldType, newType);
         removeFromCache(oldType);
      }

      writeMembershipType(mType, mtNode);
      session.save();

      putInCache(mType);

      if (broadcast)
      {
         postSave(mType, isNew);
      }

      return mType;
   }

   /**
    * Creates and returns membership type node. If node already exists it will be returned 
    * otherwise the new one will be created.
    */
   private Node getOrCreateMembershipTypeNode(Session session, MembershipTypeImpl mType) throws Exception
   {
      try
      {
         return mType.getInternalId() != null ? session.getNodeByUUID(mType.getInternalId()) : utils
            .getMembershipTypeNode(session, mType.getName());
      }
      catch (ItemNotFoundException e)
      {
         return createNewMembershipTypeNode(session, mType);
      }
      catch (PathNotFoundException e)
      {
         return createNewMembershipTypeNode(session, mType);
      }
   }

   /**
    * Creates and returns new membership type node.
    */
   private Node createNewMembershipTypeNode(Session session, MembershipTypeImpl mType) throws Exception
   {
      Node storageTypesNode = utils.getMembershipTypeStorageNode(session);
      return storageTypesNode.addNode(mType.getName());
   }

   /**
    * Reads membership type from the node.
    * 
    * @param node
    *          the node where membership type properties are stored
    */
   private MembershipType readMembershipType(Node node) throws Exception
   {
      MembershipTypeImpl mt = new MembershipTypeImpl();
      mt.setName(node.getName().equals(JCROrganizationServiceImpl.JOS_MEMBERSHIP_TYPE_ANY) ? ANY_MEMBERSHIP_TYPE : node
         .getName());
      mt.setInternalId(node.getUUID());
      mt.setDescription(utils.readString(node, MembershipTypeProperties.JOS_DESCRIPTION));
      mt.setCreatedDate(utils.readDate(node, MembershipTypeProperties.EXO_DATE_CREATED));
      mt.setModifiedDate(utils.readDate(node, MembershipTypeProperties.EXO_DATE_MODIFIED));

      return mt;
   }

   /**
    * Writes membership type properties to the node.
    * 
    * @param membershipType
    *          the membership type to store
    * @param mtNode
    *          the node where membership type properties will be stored
    */
   private void writeMembershipType(MembershipType membershipType, Node mtNode) throws Exception
   {
      if (!mtNode.isNodeType("exo:datetime")) {
         mtNode.addMixin("exo:datetime");
      }
      mtNode.setProperty(MembershipTypeProperties.JOS_DESCRIPTION, membershipType.getDescription());
   }

   /**
    * Gets membership type from cache.
    */
   private MembershipType getFromCache(String name)
   {
      return (MembershipType)cache.get(name, CacheType.MEMBERSHIPTYPE);
   }

   /**
    * Removes membership type from cache.
    */
   private void removeFromCache(String name)
   {
      cache.remove(name, CacheType.MEMBERSHIPTYPE);
   }

   /**
    * Removes all related memberships from cache.
    */
   private void removeAllRelatedFromCache(String name)
   {
      cache.remove(CacheHandler.MEMBERSHIPTYPE_PREFIX + name, CacheType.MEMBERSHIP);
   }

   /**
    * Moves memberships in cache from old key to new one.
    */
   private void moveMembershipsInCache(String oldType, String newType)
   {
      cache.move(CacheHandler.MEMBERSHIPTYPE_PREFIX + oldType, CacheHandler.MEMBERSHIPTYPE_PREFIX + newType,
         CacheType.MEMBERSHIP);
   }

   /**
    * Puts membership type in cache.
    */
   private void putInCache(MembershipType mt)
   {
      cache.put(mt.getName(), mt, CacheType.MEMBERSHIPTYPE);
   }

   /**
    * Notifying listeners before membership type creation.
    * 
    * @param type 
    *          the membership which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new membership type, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preSave(MembershipType type, boolean isNew) throws Exception
   {
      for (MembershipTypeEventListener listener : listeners)
      {
         listener.preSave(type, isNew);
      }
   }

   /**
    * Notifying listeners after membership type creation.
    * 
    * @param type 
    *          the membership which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new membership type, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postSave(MembershipType type, boolean isNew) throws Exception
   {
      for (MembershipTypeEventListener listener : listeners)
      {
         listener.postSave(type, isNew);
      }
   }

   /**
    * Notifying listeners before membership type deletion.
    * 
    * @param type 
    *          the membership which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preDelete(MembershipType type) throws Exception
   {
      for (MembershipTypeEventListener listener : listeners)
      {
         listener.preDelete(type);
      }
   }

   /**
    * Notifying listeners after membership type deletion.
    * 
    * @param type 
    *          the membership which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postDelete(MembershipType type) throws Exception
   {
      for (MembershipTypeEventListener listener : listeners)
      {
         listener.postDelete(type);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void removeMembershipTypeEventListener(MembershipTypeEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void addMembershipTypeEventListener(MembershipTypeEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public List<MembershipTypeEventListener> getMembershipTypeListeners()
   {
      return Collections.unmodifiableList(listeners);
   }

}
