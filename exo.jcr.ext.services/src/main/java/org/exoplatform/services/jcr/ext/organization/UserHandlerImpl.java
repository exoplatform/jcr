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

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.organization.CacheHandler;
import org.exoplatform.services.organization.CacheHandler.CacheType;
import org.exoplatform.services.organization.DisabledUserException;
import org.exoplatform.services.organization.ExtendedUserHandler;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserEventListenerHandler;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.PasswordEncrypter;
import org.exoplatform.services.security.PermissionConstants;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS. Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: UserHandlerImpl.java 80140 2012-03-07 11:08:07Z aplotnikov $
 */
public class UserHandlerImpl extends JCROrgServiceHandler implements UserHandler, UserEventListenerHandler,
   ExtendedUserHandler
{

   /**
    * The list of listeners to broadcast the events.
    */
   protected final List<UserEventListener> listeners = new ArrayList<UserEventListener>();

   /**
    * Class contains the names of user properties only.
    */
   public static class UserProperties
   {
      /**
       * The user property that contains the date of creation.
       */
      public static final String JOS_CREATED_DATE = "jos:createdDate";

      /**
       * The user property that contains email.
       */
      public static final String JOS_EMAIL = "jos:email";

      /**
       * The user property that contains fist name.
       */
      public static final String JOS_FIRST_NAME = "jos:firstName";

      /**
       * The user property that contains fist name.
       */
      public static final String JOS_USER_NAME = "jos:userName";

      /**
       * The user property that contain last login time.
       */
      public static final String JOS_LAST_LOGIN_TIME = "jos:lastLoginTime";

      /**
       * The user property that contains last name.
       */
      public static final String JOS_LAST_NAME = "jos:lastName";

      /**
       * The user property that contains the name to display.
       */
      public static final String JOS_DISPLAY_NAME = "jos:displayName";

      /**
       * The user property that contains password.
       */
      public static final String JOS_PASSWORD = "jos:password";
   }

   /**
    * UserHandlerImpl constructor.
    */
   UserHandlerImpl(JCROrganizationServiceImpl service)
   {
      super(service);
   }

   /**
    * {@inheritDoc}
    */
   public boolean authenticate(String username, String password) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return authenticate(session, username, password, null);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * {@inheritDoc}
    */
   public boolean authenticate(String userName, String password, PasswordEncrypter pe) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return authenticate(session, userName, password, pe);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Checks if credentials matches.
    */
   private boolean authenticate(Session session, String userName, String password, PasswordEncrypter pe)
      throws Exception
   {
      boolean authenticated;

      Node userNode;
      try
      {
         userNode = utils.getUserNode(session, userName);
      }
      catch (PathNotFoundException e)
      {
         return false;
      }
      boolean enabled = userNode.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED);
      if (!enabled)
      {
         throw new DisabledUserException(userName);
      }

      if (pe == null)
      {
         authenticated = utils.readString(userNode, UserProperties.JOS_PASSWORD).equals(password);
      }
      else
      {
         String encryptedPassword =
            new String(pe.encrypt(utils.readString(userNode, UserProperties.JOS_PASSWORD).getBytes()));
         authenticated = encryptedPassword.equals(password);
      }
      if (authenticated)
      {
         Calendar lastLoginTime = Calendar.getInstance();
         userNode.setProperty(UserProperties.JOS_LAST_LOGIN_TIME, lastLoginTime);
         session.save();
      }
      return authenticated;
   }

   /**
    * {@inheritDoc}
    */
   public void createUser(User user, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         createUser(session, (UserImpl)user, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Persists new user.
    */
   private void createUser(Session session, UserImpl user, boolean broadcast) throws Exception
   {
      Node userStorageNode = utils.getUsersStorageNode(session);
      Node userNode = userStorageNode.addNode(user.getUserName());

      if (user.getCreatedDate() == null)
      {
         Calendar calendar = Calendar.getInstance();
         user.setCreatedDate(calendar.getTime());
      }
      user.setInternalId(userNode.getUUID());

      if (broadcast)
      {
         preSave(user, true);
      }

      writeUser(user, userNode);
      session.save();

      putInCache(user);

      if (broadcast)
      {
         postSave(user, true);
      }
   }

   /**
    * {@inheritDoc}
    */
   public User createUserInstance()
   {
      return new UserImpl();
   }

   /**
    * {@inheritDoc}
    */
   public User createUserInstance(String username)
   {
      return new UserImpl(username);
   }

   /**
    * {@inheritDoc}
    */
   public User findUserByName(String userName) throws Exception
   {
      return findUserByName(userName, UserStatus.ENABLED);
   }

   /**
    * {@inheritDoc}
    */
   public LazyPageList<User> findUsers(org.exoplatform.services.organization.Query query) throws Exception
   {
      return query.isEmpty() ? new LazyPageList<User>(new SimpleJCRUserListAccess(service, UserStatus.ENABLED), 10)
         : new LazyPageList<User>(new UserByQueryJCRUserListAccess(service, query, UserStatus.ENABLED), 10);
   }

   /**
    * {@inheritDoc}
    */
   public LazyPageList<User> findUsersByGroup(String groupId) throws Exception
   {
      return new LazyPageList<User>(new UserByGroupJCRUserListAccess(service, groupId, UserStatus.ENABLED), 10);
   }

   /**
    * {@inheritDoc}
    */
   public LazyPageList<User> getUserPageList(int pageSize) throws Exception
   {
      return new LazyPageList<User>(new SimpleJCRUserListAccess(service, UserStatus.ENABLED), pageSize);
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<User> findAllUsers() throws Exception
   {
      return findAllUsers(UserStatus.ENABLED);
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<User> findUsersByGroupId(String groupId) throws Exception
   {
      return findUsersByGroupId(groupId, UserStatus.ENABLED);
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<User> findUsersByQuery(Query query) throws Exception
   {
      return findUsersByQuery(query, UserStatus.ENABLED);
   }

   /**
    * {@inheritDoc}
    */
   public User removeUser(String userName, boolean broadcast) throws Exception
   {
      Session session = service.getStorageSession();
      try
      {
         return removeUser(session, userName, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Remove user and related membership entities.
    */
   private User removeUser(Session session, String userName, boolean broadcast) throws Exception
   {
      Node userNode = utils.getUserNode(session, userName);
      User user = readUser(userNode);

      if (broadcast)
      {
         preDelete(user);
      }

      removeMemberships(userNode, broadcast);

      userNode.remove();
      session.save();

      removeFromCache(userName);
      removeAllRelatedFromCache(userName);

      if (broadcast)
      {
         postDelete(user);
      }

      return user;
   }

   /**
    * Removes membership entities related to current user.
    */
   private void removeMemberships(Node userNode, boolean broadcast) throws RepositoryException
   {
      PropertyIterator refUserProps = userNode.getReferences();
      while (refUserProps.hasNext())
      {
         Node refUserNode = refUserProps.nextProperty().getParent();
         refUserNode.remove();
      }
   }

   /**
    * {@inheritDoc}
    */
   public void saveUser(User user, boolean broadcast) throws Exception
   {
      if (user != null && !user.isEnabled())
         throw new DisabledUserException(user.getUserName());
      Session session = service.getStorageSession();
      try
      {
         saveUser(session, (UserImpl)user, broadcast);
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Persists user.
    */
   private void saveUser(Session session, UserImpl user, boolean broadcast) throws Exception
   {
      Node userNode = getUserNode(session, user);

      if (broadcast)
      {
         preSave(user, false);
      }

      String oldName = userNode.getName();
      String newName = user.getUserName();

      if (!oldName.equals(newName))
      {
         String oldPath = userNode.getPath();
         String newPath = utils.getUserNodePath(newName);

         session.move(oldPath, newPath);

         removeFromCache(oldName);
         moveMembershipsInCache(oldName, newName);
      }

      writeUser(user, userNode);
      session.save();

      putInCache(user);

      if (broadcast)
      {
         postSave(user, false);
      }
   }

   /**
    * Returns user node by internal identifier or by name.
    */
   private Node getUserNode(Session session, UserImpl user) throws RepositoryException
   {
      if (user.getInternalId() != null)
      {
         return session.getNodeByUUID(user.getInternalId());
      }
      else
      {
         return utils.getUserNode(session, user.getUserName());
      }
   }

   

   /**
    * {@inheritDoc}
    */
   public User setEnabled(String userName, boolean enabled, boolean broadcast) throws Exception,
      UnsupportedOperationException
   {
      User foundUser = findUserByName(userName, UserStatus.ANY);

      if (foundUser == null || foundUser.isEnabled() == enabled)
      {
         return foundUser;
      }
      synchronized (foundUser.getUserName())
      {
         Session session = service.getStorageSession();
         try
         {
            ((UserImpl)foundUser).setEnabled(enabled);
            if (broadcast)
               preSetEnabled(foundUser);
            Node node = getUserNode(session, (UserImpl)foundUser);
            if (enabled)
            {
               if (!node.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED))
               {
                  node.removeMixin(JCROrganizationServiceImpl.JOS_DISABLED);
                  PropertyIterator pi = node.getReferences();
                  while (pi.hasNext())
                  {
                     Node n = pi.nextProperty().getParent();
                     if (!n.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED))
                     {
                        n.removeMixin(JCROrganizationServiceImpl.JOS_DISABLED);
                     }
                  }
               }
            }
            else
            {
               if (node.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED))
               {
                  node.addMixin(JCROrganizationServiceImpl.JOS_DISABLED);
                  PropertyIterator pi = node.getReferences();
                  while (pi.hasNext())
                  {
                     Node n = pi.nextProperty().getParent();
                     if (n.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED))
                     {
                        n.addMixin(JCROrganizationServiceImpl.JOS_DISABLED);
                     }
                  }
               }
            }
            session.save();
            if (broadcast)
               postSetEnabled(foundUser);
            putInCache(foundUser);
         }
         finally
         {
            session.logout();
         }
         return foundUser;
      }
   }

   /**
    * {@inheritDoc}
    */
   public User findUserByName(String userName, UserStatus status) throws Exception
   {
      User user = getFromCache(userName);
      if (user != null)
         return status.matches(user.isEnabled()) ? user : null;

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
            return null;
         }

         user = readUser(userNode);
         putInCache(user);

         return status.matches(user.isEnabled()) ? user : null;
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<User> findUsersByGroupId(String groupId, UserStatus status) throws Exception
   {
      return new UserByGroupJCRUserListAccess(service, groupId, status);
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<User> findAllUsers(UserStatus status) throws Exception
   {
      return new SimpleJCRUserListAccess(service, status);
   }

   /**
    * {@inheritDoc}
    */
   public ListAccess<User> findUsersByQuery(Query query, UserStatus status) throws Exception
   {
      return query.isEmpty() ? new SimpleJCRUserListAccess(service, status) : new UserByQueryJCRUserListAccess(
         service, query, status);
   }

   /**
    * Read user properties from the node in the storage.
    * 
    * @param userNode
    *          the node where user properties are stored
    * @return {@link User}
    */
   public UserImpl readUser(Node userNode) throws Exception
   {
      UserImpl user = new UserImpl(userNode.getName());

      Date creationDate = utils.readDate(userNode, UserProperties.JOS_CREATED_DATE);
      Date lastLoginTime = utils.readDate(userNode, UserProperties.JOS_LAST_LOGIN_TIME);
      String email = utils.readString(userNode, UserProperties.JOS_EMAIL);
      String password = utils.readString(userNode, UserProperties.JOS_PASSWORD);
      String firstName = utils.readString(userNode, UserProperties.JOS_FIRST_NAME);
      String lastName = utils.readString(userNode, UserProperties.JOS_LAST_NAME);
      String displayName = utils.readString(userNode, UserProperties.JOS_DISPLAY_NAME);
      boolean enabled = userNode.canAddMixin(JCROrganizationServiceImpl.JOS_DISABLED);

      user.setInternalId(userNode.getUUID());
      user.setCreatedDate(creationDate);
      user.setLastLoginTime(lastLoginTime);
      user.setEmail(email);
      user.setPassword(password);
      user.setFirstName(firstName);
      user.setLastName(lastName);
      user.setDisplayName(displayName);
      user.setEnabled(enabled);

      return user;
   }

   /**
    * Write user properties from the node to the storage.
    * 
    * @param node
    *          the node where user properties are stored
    * @return {@link User}
    */
   private void writeUser(User user, Node node) throws Exception
   {
      node.setProperty(UserProperties.JOS_EMAIL, user.getEmail());
      node.setProperty(UserProperties.JOS_FIRST_NAME, user.getFirstName());
      node.setProperty(UserProperties.JOS_LAST_NAME, user.getLastName());
      node.setProperty(UserProperties.JOS_PASSWORD, user.getPassword());
      node.setProperty(UserProperties.JOS_DISPLAY_NAME, user.getDisplayName());
      node.setProperty(UserProperties.JOS_USER_NAME, node.getName());

      Calendar calendar = Calendar.getInstance();
      calendar.setTime(user.getCreatedDate());
      node.setProperty(UserProperties.JOS_CREATED_DATE, calendar);
   }

   /**
    * Method for user migration.
    * 
    * @param oldUserNode 
    *         the node where user properties are stored (from old structure)
    * @throws Exception
    */
   void migrateUser(Node oldUserNode) throws Exception
   {

      String userName = oldUserNode.getName();

      if (findUserByName(userName, UserStatus.ANY) != null)
      {
         removeUser(userName, false);
      }

      UserImpl user = readUser(oldUserNode);
      createUser(user, false);
   }

   /**
    * Notifying listeners before user creation.
    * 
    * @param user 
    *          the user which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new user, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preSave(User user, boolean isNew) throws Exception
   {
      for (UserEventListener listener : listeners)
      {
         listener.preSave(user, isNew);
      }
   }

   /**
    * Notifying listeners after user creation.
    * 
    * @param user 
    *          the user which is used in create operation
    * @param isNew 
    *          true, if we have a deal with new user, otherwise it is false
    *          which mean update operation is in progress
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postSave(User user, boolean isNew) throws Exception
   {
      for (UserEventListener listener : listeners)
      {
         listener.postSave(user, isNew);
      }
   }

   /**
    * Notifying listeners before user deletion.
    * 
    * @param user 
    *          the user which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preDelete(User user) throws Exception
   {
      for (UserEventListener listener : listeners)
      {
         listener.preDelete(user);
      }
   }

   /**
    * Notifying listeners after user deletion.
    * 
    * @param user 
    *          the user which is used in delete operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postDelete(User user) throws Exception
   {
      for (UserEventListener listener : listeners)
      {
         listener.postDelete(user);
      }
   }

   /**
    * Notifying listeners before enabling/disabling a user.
    * 
    * @param user 
    *          the user which is used in enabling/disabling operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void preSetEnabled(User user) throws Exception
   {
      for (UserEventListener listener : listeners)
         listener.preSetEnabled(user);
   }


   /**
    * Notifying listeners after enabling/disabling a user.
    * 
    * @param user 
    *          the user which is used in enabling/disabling operation
    * @throws Exception 
    *          if any listener failed to handle the event
    */
   private void postSetEnabled(User user) throws Exception
   {
      for (UserEventListener listener : listeners)
         listener.postSetEnabled(user);
   }

   /**
    * Remove user from cache.
    */
   private void removeFromCache(String userName)
   {
      cache.remove(userName, CacheType.USER);
   }

   /**
    * Remove user and related entities from cache.
    */
   private void removeAllRelatedFromCache(String userName)
   {
      cache.remove(userName, CacheType.USER_PROFILE);
      cache.remove(CacheHandler.USER_PREFIX + userName, CacheType.MEMBERSHIP);
   }

   /**
    * Get user from cache.
    */
   private User getFromCache(String userName)
   {
      return (User)cache.get(userName, CacheType.USER);
   }

   /**
    * Move memberships entities from old key to new one.
    */
   private void moveMembershipsInCache(String oldName, String newName)
   {
      cache.move(CacheHandler.USER_PREFIX + oldName, CacheHandler.USER_PREFIX + newName, CacheType.MEMBERSHIP);
   }

   /**
    * Put user in cache.
    */
   private void putInCache(User user)
   {
      cache.put(user.getUserName(), user, CacheType.USER);
   }

   /**
    * {@inheritDoc}
    */
   public void addUserEventListener(UserEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.add(listener);
   }

   /**
    * Remove registered listener.
    * 
    * @param listener The registered listener for remove
    */
   public void removeUserEventListener(UserEventListener listener)
   {
      SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
      listeners.remove(listener);
   }

   /**
    * {@inheritDoc}
    */
   public List<UserEventListener> getUserListeners()
   {
      return Collections.unmodifiableList(listeners);
   }
}
