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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.access.DynamicIdentity;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.IdentityConstants;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.<br/> the factory for jcr Session
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SessionFactory.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */

public class SessionFactory
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SessionFactory");

   private final ExoContainer container;

   private final String workspaceName;

   /**
    * JCR Session factory.
    * 
    * @param config WorkspaceEntry
    * @param containerContext ExoContainerContext
    */
   public SessionFactory(WorkspaceEntry config, ExoContainerContext containerContext)
   {
      this.container = containerContext.getContainer();
      this.workspaceName = config.getName();

      boolean tracking =
         "true".equalsIgnoreCase(PrivilegedSystemHelper.getProperty("exo.jcr.session.tracking.active", "false"));

      if (tracking)
      {
         long maxAgeMillis = 0;

         String maxagevalue = PrivilegedSystemHelper.getProperty("exo.jcr.jcr.session.tracking.maxage");
         if (maxagevalue != null)
         {
            try
            {
               maxAgeMillis = Long.parseLong(maxagevalue) * 1000;
            }
            catch (NumberFormatException e)
            {
               if (LOG.isTraceEnabled())
               {
                  LOG.trace("An exception occurred: " + e.getMessage());
               }
            }
         }
         if (maxAgeMillis <= 0)
         {
            maxAgeMillis = 1000 * 60 * 2; // 2 mns
         }

         //
         try
         {
            SessionReference.start(maxAgeMillis);
         }
         catch (Exception e)
         {
            LOG.error(e.getLocalizedMessage(), e);
         }
      }

      if (config.getContainer().getParameterInteger(WorkspaceDataContainer.LAZY_NODE_ITERATOR_PAGE_SIZE,
         WorkspaceDataContainer.LAZY_NODE_ITERATOR_PAGE_SIZE_DEFAULT) < WorkspaceDataContainer.LAZY_NODE_ITERATOR_PAGE_SIZE_MIN)
      {
         // set proper value
         config.getContainer().putParameterValue(WorkspaceDataContainer.LAZY_NODE_ITERATOR_PAGE_SIZE,
            Integer.toString(WorkspaceDataContainer.LAZY_NODE_ITERATOR_PAGE_SIZE_MIN));
         LOG.warn("Value for \"lazy-node-iterator-page-size\" is too small. Using allowed minimum page size : "
            + WorkspaceDataContainer.LAZY_NODE_ITERATOR_PAGE_SIZE_MIN + ".");
      }
   }

   /**
    * Creates Session object by given Credentials
    * 
    * @param credentials
    * @return the SessionImpl corresponding to the given {@link ConversationState}
    * @throws RepositoryException
    */
   SessionImpl createSession(ConversationState user) throws RepositoryException, LoginException
   {
      if (IdentityConstants.SYSTEM.equals(user.getIdentity().getUserId()))
      {
         // Need privileges to get system session.
         SecurityManager security = System.getSecurityManager();
         if (security != null)
         {
            security.checkPermission(JCRRuntimePermissions.CREATE_SYSTEM_SESSION_PERMISSION);
         }
      }
      else if (DynamicIdentity.DYNAMIC.equals(user.getIdentity().getUserId()))
      {
         // Need privileges to get Dynamic session.
         SecurityManager security = System.getSecurityManager();
         if (security != null)
         {
            security.checkPermission(JCRRuntimePermissions.CREATE_DYNAMIC_SESSION_PERMISSION);
         }
      }
      if (SessionReference.isStarted())
      {
         return new TrackedSession(workspaceName, user, container);
      }
      else
      {
         return new SessionImpl(workspaceName, user, container);
      }
   }
}
