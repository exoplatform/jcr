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

import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.util.EntityCollection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: ObservationManagerRegistry.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ObservationManagerRegistry
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.ObservationManagerRegistry");

   protected Map<EventListener, ListenerCriteria> listenersMap;

   protected ActionLauncher launcher;

   public ObservationManagerRegistry(WorkspacePersistentDataManager workspaceDataManager,
      SessionRegistry sessionRegistry)
   {

      this.listenersMap = new HashMap<EventListener, ListenerCriteria>();
      this.launcher = new ActionLauncher(this, workspaceDataManager, sessionRegistry);
   }

   public ObservationManagerImpl createObservationManager(SessionImpl session)
   {
      return new ObservationManagerImpl(this, session.getId());
   }

   public void addEventListener(EventListener listener, ListenerCriteria filter)
   {
      listenersMap.put(listener, filter);
   }

   public void removeEventListener(EventListener listener)
   {
      listenersMap.remove(listener);
   }

   public EventListenerIterator getEventListeners()
   {
      return new EntityCollection(listenersMap.keySet());
   }

   public ListenerCriteria getListenerFilter(EventListener listener)
   {
      return listenersMap.get(listener);
   }

   public void removeSessionEventListeners(SessionImpl session)
   {
      // Iterating without ConcurrentModificationException
      List<EventListener> eventsForRemove = new ArrayList<EventListener>();

      for (EventListener listener : listenersMap.keySet())
      {
         ListenerCriteria criteria = listenersMap.get(listener);
         if (criteria.getSessionId().equals(session.getId()))
         {
            eventsForRemove.add(listener);
         }
      }
      for (EventListener listener : eventsForRemove)
      {
         listenersMap.remove(listener);
      }
   }

}
