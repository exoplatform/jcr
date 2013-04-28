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
package org.exoplatform.services.jcr.ext.common;

import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.DynamicIdentity;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS .<br/>
 * Provides JCR Session for client program. Usually it is per client thread object Session creates
 * with Repository.login(..) method and then can be stored in some cache if neccessary.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SessionProvider.java 34445 2009-07-24 07:51:18Z dkatayev $
 * @LevelAPI Platform
 */

public class SessionProvider implements SessionLifecycleListener
{

   /**
    * Constant for handlers.
    */
   public final static String SESSION_PROVIDER = "JCRsessionProvider";

   /**
    * Sessions cache.
    */
   private final Map<String, ExtendedSession> cache;

   /**
    * System session marker.
    */
   private boolean isSystem;

   private ManageableRepository currentRepository;

   private String currentWorkspace;

   private boolean closed;
   
   private ConversationState conversationState;

   /**
    * Creates SessionProvider for certain identity.
    * 
    * @param userState
    */
   public SessionProvider(ConversationState userState)
   {
      this(false);
      if (userState.getAttribute(SESSION_PROVIDER) == null)
         userState.setAttribute(SESSION_PROVIDER, this);
   }

   /**
    * Creates SessionProvider for a dynamic identity.
    * 
    * @param membershipEntries the expected memberships
    */
   private SessionProvider(HashSet<MembershipEntry> membershipEntries)
   {
      this(false);
      Identity id = new Identity(DynamicIdentity.DYNAMIC, membershipEntries);
      this.conversationState = new ConversationState(id);
   }

   /**
    * Internal constructor.
    * 
    * @param isSystem
    */
   private SessionProvider(boolean isSystem)
   {
      this.isSystem = isSystem;
      this.cache = new HashMap<String, ExtendedSession>();
      this.closed = false;
   }

   /**
    * Helper for creating System session provider.
    * 
    * @return a system session provider
    */
   public static SessionProvider createSystemProvider()
   {
      return new SessionProvider(true);
   }

   /**
    * Helper for creating Anonymous session provider.
    * 
    * @return an anonymous session provider
    */
   public static SessionProvider createAnonimProvider()
   {
      Identity id = new Identity(IdentityConstants.ANONIM, new HashSet<MembershipEntry>());
      return new SessionProvider(new ConversationState(id));
   }

   /**
     * Gives a {{code language=java}}{@include org.exoplatform.services.jcr.ext.common.SessionProvider}{{/code}}for a given list of
    * {{code language=java}}{@include org.exoplatform.services.jcr.access.AccessControlEntry}{{/code}}.
     *
     * @param accessList list of {{code language=java}}{@include org.exoplatform.services.jcr.access.AccessControlEntry}{{/code}}
     * @return a {{code language=java}}{@include org.exoplatform.services.jcr.ext.common.SessionProvider}{{/code}} allowing to provide sessions with the
     * corresponding ACL.
     */
   public static SessionProvider createProvider(List<AccessControlEntry> accessList)
   {
      if (accessList == null || accessList.isEmpty())
      {
         return createAnonimProvider();
      }
      else
      {
         HashSet<MembershipEntry> membershipEntries = new HashSet<MembershipEntry>();

         for (AccessControlEntry ace : accessList)
         {
            membershipEntries.add(ace.getMembershipEntry());
         }
         return new SessionProvider(membershipEntries);
      }

   }

   /**
    * Gets the session from an internal cache if a similar session has already been used
    * or creates a new session and puts it into the internal cache.
    * 
    * @param workspaceName the workspace name
    * @param repository the repository instance
    * @return a session corresponding to the given repository and workspace
    * @throws LoginException if an error occurs while trying to login to the workspace
    * @throws NoSuchWorkspaceException if the requested workspace doesn't exist
    * @throws RepositoryException if any error occurs
    */
   public synchronized Session getSession(String workspaceName, ManageableRepository repository) throws LoginException,
      NoSuchWorkspaceException, RepositoryException
   {

      if (closed)
      {
         throw new IllegalStateException("Session provider already closed");
      }

      if (workspaceName == null)
      {
         throw new IllegalArgumentException("Workspace Name is null");
      }

      ExtendedSession session = cache.get(key(repository, workspaceName));
      // create and cache new session

      if (session == null)
      {
         if (conversationState != null)
         {
            session =
                     (ExtendedSession) repository.getDynamicSession(workspaceName, conversationState.getIdentity()
                              .getMemberships());
         }
         else if (!isSystem)
         {
            session = (ExtendedSession)repository.login(workspaceName);
         }
         else
         {
            session = (ExtendedSession)repository.getSystemSession(workspaceName);
         }

         session.registerLifecycleListener(this);

         cache.put(key(repository, workspaceName), session);
      }

      return session;
   }

   /**
    * Calls logout() method for all cached sessions.
    * 
    * Session will be removed from cache by the listener (this provider) via
    * ExtendedSession.logout().
    */
   public synchronized void close()
   {

      if (closed)
      {
         throw new IllegalStateException("Session provider already closed");
      }

      closed = true;

      for (ExtendedSession session : (ExtendedSession[])cache.values().toArray(
         new ExtendedSession[cache.values().size()]))
         session.logout();

      // the cache already empty (logout listener work, see onCloseSession())
      // just to be sure
      cache.clear();
   }

   /*
    * (non-Javadoc)
    * @see
    * org.exoplatform.services.jcr.core.SessionLifecycleListener#onCloseSession(org.exoplatform.services
    * .jcr.core.ExtendedSession)
    */
   public synchronized void onCloseSession(ExtendedSession session)
   {
      this.cache.remove(key((ManageableRepository)session.getRepository(), session.getWorkspace().getName()));
   }

   /**
    * Key generator for sessions cache.
    * 
    * @param repository the repository instance
    * @param workspaceName the workspace name
    * @return
    */
   private String key(ManageableRepository repository, String workspaceName)
   {
      String repositoryName = repository.getConfiguration().getName();
      return repositoryName + workspaceName;
   }

   /**
     * @return returns the current Repository
     */
   public ManageableRepository getCurrentRepository()
   {
      return currentRepository;
   }

   /**
     * @return returns the current Workspace
     */
   public String getCurrentWorkspace()
   {
      return currentWorkspace;
   }

   /**
     * Sets the current repository Repository.
     * @param  currentRepository the current repository
     */
   public void setCurrentRepository(ManageableRepository currentRepository)
   {
      this.currentRepository = currentRepository;
   }

   /**
     * Sets the current Workspace
     * @param  currentWorkspace the current workspace
     */
   public void setCurrentWorkspace(String currentWorkspace)
   {
      this.currentWorkspace = currentWorkspace;
   }

}
