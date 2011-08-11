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
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
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
    * @return System session
    */
   public static SessionProvider createSystemProvider()
   {
      return new SessionProvider(true);
   }

   /**
    * Helper for creating Anonymous session provider.
    * 
    * @return System session
    */
   public static SessionProvider createAnonimProvider()
   {
      Identity id = new Identity(SystemIdentity.ANONIM, new HashSet<MembershipEntry>());
      return new SessionProvider(new ConversationState(id));
   }

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
    * Gets the session from internal cache or creates and caches new one.
    * 
    * @param workspaceName
    * @param repository
    * @return session
    * @throws LoginException
    * @throws NoSuchWorkspaceException
    * @throws RepositoryException
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
         throw new NullPointerException("Workspace Name is null");
      }

      ExtendedSession session = cache.get(key(repository, workspaceName));
      // create and cache new session

      if (session == null)
      {
         if (conversationState != null)
         {
            session = (ExtendedSession) repository.getDynamicSession(workspaceName, conversationState.getIdentity().getMemberships());
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
    * @param repository
    * @param workspaceName
    * @return
    */
   private String key(ManageableRepository repository, String workspaceName)
   {
      String repositoryName = repository.getConfiguration().getName();
      return repositoryName + workspaceName;
   }

   public ManageableRepository getCurrentRepository()
   {
      return currentRepository;
   }

   public String getCurrentWorkspace()
   {
      return currentWorkspace;
   }

   public void setCurrentRepository(ManageableRepository currentRepository)
   {
      this.currentRepository = currentRepository;
   }

   public void setCurrentWorkspace(String currentWorkspace)
   {
      this.currentWorkspace = currentWorkspace;
   }

}
