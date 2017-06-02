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
import org.exoplatform.services.organization.CacheHandler.CacheType;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;
import org.exoplatform.services.organization.UserProfileEventListenerHandler;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.security.PermissionConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: UserProfileHandlerImpl.java 33732 2009-07-08 15:00:43Z
 *          pnedonosko $
 */
public class UserProfileHandlerImpl extends JCROrgServiceHandler implements UserProfileHandler,
   UserProfileEventListenerHandler
{

   /**
    * The prefix for properties name which store profile attributes.
    */
   public static final String ATTRIBUTE_PREFIX = "attr.";

   /**
    * The list of listeners to broadcast events.
    */
   protected final List<UserProfileEventListener> listeners = new ArrayList<UserProfileEventListener>();

   /**
    * UserProfileHandlerImpl constructor.
    */
   UserProfileHandlerImpl(JCROrganizationServiceImpl service)
   {
      super(service);
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile createUserProfileInstance()
   {
      return new UserProfileImpl();
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile createUserProfileInstance(String userName)
   {
      return new UserProfileImpl(userName);
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile findUserProfileByName(String userName) throws Exception
   {
      UserProfile profile = getFromCache(userName);
      if (profile != null)
      {
         return profile;
      }

      Session session = service.getStorageSession();
      try
      {
         profile = readProfile(session, userName);
         if (profile != null)
         {
            putInCache(profile);
         }
      }
      finally
      {
         session.logout();
      }

      return profile;
   }

   /**
    * Reads user profile from storage based on user name.
    */
   private UserProfile readProfile(Session session, String userName) throws Exception
   {
      Node profileNode;
      try
      {
         profileNode = utils.getProfileNode(session, userName);
      }
      catch (PathNotFoundException e)
      {
         return null;
      }

      return readProfile(userName, profileNode);
   }

   /**
    * {@inheritDoc}
    */
   public Collection<UserProfile> findUserProfiles() throws Exception
   {
      List<UserProfile> profiles = new ArrayList<UserProfile>();

      Session session = service.getStorageSession();
      try
      {
         NodeIterator userNodes = utils.getUsersStorageNode(session).getNodes();
         while (userNodes.hasNext())
         {
            Node userNode = userNodes.nextNode();

            String userName = userNode.getName();

            Node profileNode;
            try
            {
               profileNode = userNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE);
            }
            catch (PathNotFoundException e)
            {
               continue;
            }

            UserProfile profile = readProfile(userName, profileNode);
            if (profile != null)
            {
               profiles.add(profile);
            }
         }
      }
      finally
      {
         session.logout();
      }

      return profiles;
   }

   /**
    * {@inheritDoc}
    */
   public UserProfile removeUserProfile(String userName, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeUserProfile(session, userName, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Remove user profile from storage.
    */
   private UserProfile removeUserProfile(Session session, String userName, boolean broadcast) throws Exception
   {
      Node profileNode;
      try
      {
         profileNode = utils.getProfileNode(session, userName);
      }
      catch (PathNotFoundException e)
      {
         return null;
      }

      UserProfile profile = readProfile(userName, profileNode);

      if (broadcast)
      {
         preDelete(profile, broadcast);
      }

      profileNode.remove();
      session.save();

      removeFromCache(profile);

      if (broadcast)
      {
         postDelete(profile);
      }

      return profile;
   }

   /**
    * {@inheritDoc}
    */
   public void saveUserProfile(UserProfile profile, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         saveUserProfile(session, profile, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Migrates user profile from old storage into new.
    * @param oldUserNode 
    *         the node where user properties are stored (from old structure)
    */
   void migrateProfile(Node oldUserNode) throws Exception
   {
      UserProfile userProfile = new UserProfileImpl(oldUserNode.getName());

      Node attrNode = null;
      try
      {
         attrNode = oldUserNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE + "/" + MigrationTool.JOS_ATTRIBUTES);
      }
      catch (PathNotFoundException e)
      {
         return;
      }
      PropertyIterator props = attrNode.getProperties();

      while (props.hasNext())
      {
         Property prop = props.nextProperty();

         // ignore system properties
         if (!(prop.getName()).startsWith("jcr:") && !(prop.getName()).startsWith("exo:")
            && !(prop.getName()).startsWith("jos:"))
         {
            userProfile.setAttribute(prop.getName(), prop.getString());
         }
      }

      if (findUserProfileByName(userProfile.getUserName()) != null)
      {
         removeUserProfile(userProfile.getUserName(), false);
      }

      saveUserProfile(userProfile, false);
   }

   /**
    * Persist user profile to the storage.
    */
   private void saveUserProfile(Session session, UserProfile profile, boolean broadcast) throws RepositoryException,
      Exception
   {
      Node userNode = utils.getUserNode(session, profile.getUserName());
      Node profileNode = getProfileNode(userNode);

      boolean isNewProfile = profileNode.isNew();

      if (broadcast)
      {
         preSave(profile, isNewProfile);
      }

      writeProfile(profile, profileNode);

      session.save();
      putInCache(profile);

      if (broadcast)
      {
         postSave(profile, isNewProfile);
      }
   }

   /**
    * Create new profile node. 
    */
   private Node getProfileNode(Node userNode) throws RepositoryException
   {
      try
      {
         return userNode.getNode(JCROrganizationServiceImpl.JOS_PROFILE);
      }
      catch (PathNotFoundException e)
      {
         return userNode.addNode(JCROrganizationServiceImpl.JOS_PROFILE);
      }
   }

   /**
    * Read user profile from storage.
    * 
    * @param profileNode
    *          the node which stores profile attributes as properties with
    *          prefix {@link #ATTRIBUTE_PREFIX}
    * @return {@link UserProfile} instance
    * @throws RepositoryException
    *          if unexpected exception is occurred during reading     
    */
   private UserProfile readProfile(String userName, Node profileNode) throws RepositoryException
   {
      UserProfile profile = createUserProfileInstance(userName);

      PropertyIterator attributes = profileNode.getProperties();
      while (attributes.hasNext())
      {
         Property prop = attributes.nextProperty();

         if (prop.getName().startsWith(ATTRIBUTE_PREFIX))
         {
            String name = prop.getName().substring(ATTRIBUTE_PREFIX.length());
            String value = prop.getString();

            profile.setAttribute(name, value);
         }
      }
      return profile;
   }

   /**
    * Write profile to storage. 
    * 
    * @param profileNode
    *          the node which stores profile attributes as properties with
    *          prefix {@link #ATTRIBUTE_PREFIX}
    * @param userProfile
    *          the profile to store
    * @throws RepositoryException
    *          if unexpected exception is occurred during writing     
    */
   private void writeProfile(UserProfile userProfile, Node profileNode) throws RepositoryException
   {
      for (Entry<String, String> attribute : userProfile.getUserInfoMap().entrySet())
      {
         profileNode.setProperty(ATTRIBUTE_PREFIX + attribute.getKey(), attribute.getValue());
      }
   }

   /**
    * Returns user profile from cache. Can return null.
    */
   private UserProfile getFromCache(String userName)
   {
      return (UserProfile)cache.get(userName, CacheType.USER_PROFILE);
   }

   /**
    * Putting user profile in cache if profile is not null.
    */
   private void putInCache(UserProfile profile)
   {
      cache.put(profile.getUserName(), profile, CacheType.USER_PROFILE);
   }

   /**
    * Removing user profile from cache.
    */
   private void removeFromCache(UserProfile profile)
   {
      cache.remove(profile.getUserName(), CacheType.USER_PROFILE);
   }

   /**
    * Notifying listeners before profile creation.
    * 
    * @param userProfile 
    *          the user profile which is used in save operation
    * @param isNew 
    *          true, if we have a deal with new profile, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preSave(UserProfile userProfile, boolean isNew) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
      {
         listener.preSave(userProfile, isNew);
      }
   }

   /**
    * Notifying listeners after profile creation.
    * 
    * @param userProfile 
    *          the user profile which is used in save operation
    * @param isNew 
    *          true if we have deal with new profile, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postSave(UserProfile userProfile, boolean isNew) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
      {
         listener.postSave(userProfile, isNew);
      }
   }

   /**
    * Notifying listeners before profile deletion.
    * 
    * @param userProfile 
    *          the user profile which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preDelete(UserProfile userProfile, boolean broadcast) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
      {
         listener.preDelete(userProfile);
      }
   }

   /**
    * Notifying listeners after profile deletion.
    * 
    * @param userProfile 
    *          the user profile which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postDelete(UserProfile userProfile) throws Exception
   {
      for (UserProfileEventListener listener : listeners)
      {
         listener.postDelete(userProfile);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addUserProfileEventListener(UserProfileEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.add(listener);
   }

   /**
    * {@inheritDoc}
    */
   public void removeUserProfileEventListener(UserProfileEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public List<UserProfileEventListener> getUserProfileListeners()
   {
      return Collections.unmodifiableList(listeners);
   }
}