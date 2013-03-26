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
package org.exoplatform.services.jcr.impl.core.observation;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.SessionLifecycleListener;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.util.EntityCollection;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;
import javax.jcr.observation.ObservationManager;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: ObservationManagerImpl.java 12096 2008-03-19 11:42:40Z gazarenkov $
 */

public class ObservationManagerImpl implements ObservationManager, SessionLifecycleListener
{

   protected String sessionId;

   private List<EventListener> sessionListeners = new ArrayList<EventListener>();

   private ObservationManagerRegistry registry;

   /**
    * Protected constructor for subclasses
    * 
    * @param session
    */
   ObservationManagerImpl(ObservationManagerRegistry registry, String sessionId)
   {
      this.sessionId = sessionId;
      this.registry = registry;
   }

   /**
    * @see javax.jcr.observation.ObservationManager#addEventListener
    */
   public void addEventListener(EventListener listener, int eventTypes, String absPath, boolean isDeep, String[] uuid,
      String[] nodeTypeName, boolean noLocal) throws RepositoryException
   {

      registry.addEventListener(listener, new ListenerCriteria(eventTypes, absPath, isDeep, uuid, nodeTypeName,
         noLocal, sessionId));

      sessionListeners.add(listener);
   }

   /**
    * @see javax.jcr.observation.ObservationManager#removeEventListener
    */
   public void removeEventListener(EventListener listener) throws RepositoryException {

       ListenerCriteria list = registry.getListenerFilter(listener);
       if (list != null && !this.sessionId.equals(list.getSessionId())) {
           SessionRegistry sessionRegistry = registry.getSessionRegistry();
           SessionImpl session = sessionRegistry.getSession(list.getSessionId());
           if (session != null) {
               session.getWorkspace().getObservationManager().removeEventListener(listener);
           }
       }
       sessionListeners.remove(listener);
       registry.removeEventListener(listener);
   }

   /**
    * @see javax.jcr.observation.ObservationManager#getRegisteredEventListeners
    */
   public EventListenerIterator getRegisteredEventListeners() throws RepositoryException
   {
      // return a personal copy of registered listeners, no concurrent modification exc will found
      return new EntityCollection(new ArrayList<EventListener>(sessionListeners));
   }

   // ************** SessionLifecycleListener ****************

   /*
    * @see
    * org.exoplatform.services.jcr.impl.core.SessionLifecycleListener#onCloseSession(org.exoplatform
    * .services.jcr.impl.core.SessionImpl)
    */
   public void onCloseSession(ExtendedSession targetSession)
   {
      // do nothing, as we need to listen events after the session was logout
   }

}
