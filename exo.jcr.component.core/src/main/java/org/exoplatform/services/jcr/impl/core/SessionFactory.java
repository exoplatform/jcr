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

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.dataflow.session.TransactionableResourceManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.transaction.TransactionService;

import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.transaction.xa.XAException;

/**
 * Created by The eXo Platform SAS.<br/> the factory for jcr Session
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: SessionFactory.java 14100 2008-05-12 10:53:47Z gazarenkov $
 */

public class SessionFactory
{

   protected static Log LOG = ExoLogger.getLogger("jcr.SessionFactory");

   private final ExoContainer container;

   private final TransactionService tService;

   private final String workspaceName;

   private final TransactionableResourceManager txResourceManager;

   /**
    * JCR Session factory.
    * 
    * @param tService TransactionService
    * @param config WorkspaceEntry
    * @param containerContext ExoContainerContext
    */
   public SessionFactory(TransactionService tService, WorkspaceEntry config, ExoContainerContext containerContext)
   {

      this.container = containerContext.getContainer();
      this.workspaceName = config.getName();
      this.tService = tService;
      this.txResourceManager = new TransactionableResourceManager();

      boolean tracking = "true".equalsIgnoreCase(System.getProperty("exo.jcr.session.tracking.active", "false"));
      if (tracking)
      {
         long maxAgeMillis = 0;

         String maxagevalue = System.getProperty("exo.jcr.jcr.session.tracking.maxage");
         if (maxagevalue != null)
         {
            try
            {
               maxAgeMillis = Long.parseLong(maxagevalue) * 1000;
            }
            catch (NumberFormatException e)
            {
               //
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
            e.printStackTrace();
         }
      }
   }

   /**
    * JCR Session factory.
    * 
    * @param config WorkspaceEntry
    * @param containerContext ExoContainerContext
    */
   public SessionFactory(WorkspaceEntry config, ExoContainerContext containerContext)
   {
      this((TransactionService)null, config, containerContext);
   }

   /**
    * Creates Session object by given Credentials
    * 
    * @param credentials
    * @return XASessionImpl if TransactionService present or SessionImpl otherwice
    * @throws RepositoryException
    */
   SessionImpl createSession(ConversationState user) throws RepositoryException, LoginException
   {
      if (tService == null)
      {
         if (SessionReference.isStarted())
         {
            return new TrackedSession(workspaceName, user, container);
         }
         else
         {
            return new SessionImpl(workspaceName, user, container);
         }
      }

      if (SessionReference.isStarted())
      {
         return new TrackedXASession(workspaceName, user, container, tService, txResourceManager);
      }
      else
      {
         return new XASessionImpl(workspaceName, user, container, tService, txResourceManager);
      }
   }

}
