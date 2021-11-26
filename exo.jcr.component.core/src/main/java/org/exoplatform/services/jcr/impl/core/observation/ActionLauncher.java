/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.observation;

import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ExtendedWorkspace;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeData;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.CompositeChangesLog;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.ItemsPersistenceListener;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.util.EntityCollection;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.EventListenerIterator;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: ActionLauncher.java 15072 2008-06-02 13:01:26Z pnedonosko $
 */
public class ActionLauncher implements ItemsPersistenceListener
{

   public final int SKIP_EVENT = Integer.MIN_VALUE;

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.ActionLauncher");

   private final ObservationManagerRegistry observationRegistry;

   private final WorkspacePersistentDataManager workspaceDataManager;

   private final SessionRegistry sessionRegistry;

   public ActionLauncher(ObservationManagerRegistry registry, WorkspacePersistentDataManager workspaceDataManager,
      SessionRegistry sessionRegistry)
   {
      this.observationRegistry = registry;
      this.workspaceDataManager = workspaceDataManager;
      this.sessionRegistry = sessionRegistry;
      this.workspaceDataManager.addItemPersistenceListener(this);
   }

   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      EventListenerIterator eventListeners = observationRegistry.getEventListeners();

      while (eventListeners.hasNext())
      {

         EventListener listener = eventListeners.nextEventListener();
         if (listener == null)
         {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("The listener has probably been removed thus it will be skipped");
            }
            continue;
         }

         ListenerCriteria criteria = observationRegistry.getListenerFilter(listener);
         if (criteria == null)
         {
            if (LOG.isDebugEnabled())
            {
                LOG.debug("The criteria of the listener could not be found thus it will be skipped");
            }
            continue;
         }
         EntityCollection events = new EntityCollection();

         ChangesLogIterator logIterator = ((CompositeChangesLog)changesLog).getLogIterator();
         while (logIterator.hasNextLog())
         {

            PlainChangesLog subLog = logIterator.nextLog();
            String sessionId = subLog.getSessionId();

            ExtendedSession userSession;

            if (subLog.getSession() != null)
            {
               userSession = subLog.getSession();
            }
            else
            {
               userSession = sessionRegistry.getSession(sessionId);
            }

            if (userSession != null)
            {
               for (ItemState itemState : subLog.getAllStates())
               {
                  if (itemState.isEventFire())
                  {

                     ItemData item = itemState.getData();
                     try
                     {
                        int eventType = eventType(itemState);
                        if (eventType != SKIP_EVENT && isTypeMatch(criteria, eventType)
                           && isPathMatch(criteria, item, userSession) && isIdentifierMatch(criteria, item)
                           && isNodeTypeMatch(criteria, item, userSession, subLog)
                           && isSessionMatch(criteria, sessionId))
                        {

                           String path =
                              userSession.getLocationFactory().createJCRPath(item.getQPath()).getAsString(false);
                           if (eventType== ExtendedEvent.NODE_MOVED)
                           {
                               String oldPath =
                                       userSession.getLocationFactory().createJCRPath(itemState.getOldPath()).getAsString(false);
                               Map<String, String> info = new HashMap<String, String>();
                               if(itemState.isMoved())
                               {
                                   info.put(ExtendedEvent.SRC_ABS_PATH, oldPath);
                                   info.put(ExtendedEvent.DEST_ABS_PATH, path);
                               }
                               else if (itemState.isOrdered())
                               {
                                   info.put(ExtendedEvent.SRC_CHILD_REL_PATH, oldPath);
                                   info.put(ExtendedEvent.DEST_CHILD_REL_PATH, path);
                               }

                               events.add(new EventImpl(eventType, path, userSession.getUserID(), info));
                           }
                            else
                           {
                               events.add(new EventImpl(eventType, path, userSession.getUserID()));

                           }

                        }
                     }
                     catch (RepositoryException e)
                     {
                        LOG.error("Can not fire ActionLauncher.onSaveItems() for " + item.getQPath().getAsString()
                           + " reason: " + e.getMessage());
                     }
                  }
               }
            }
         }
         if (events.getSize() > 0)
         {
            // TCK says, no events - no onEvent() action
            listener.onEvent(events);
         }
      }
   }

   // ---------------------------------

   private boolean isTypeMatch(ListenerCriteria criteria, int state)
   {
      return (criteria.getEventTypes() & state) > 0;
   }

   private boolean isSessionMatch(ListenerCriteria criteria, String sessionId)
   {
      return !(criteria.getNoLocal() && criteria.getSessionId().equals(sessionId));
   }

   private boolean isPathMatch(ListenerCriteria criteria, ItemData item, ExtendedSession userSession)
   {
      if (criteria.getAbsPath() == null)
      {
         return true;
      }
      try
      {
         QPath cLoc = userSession.getLocationFactory().parseAbsPath(criteria.getAbsPath()).getInternalPath();

         // 8.3.3 Only events whose associated parent node is at absPath (or
         // within its subtree, if isDeep is true) will be received.

         QPath itemPath = item.getQPath();

         return itemPath.isDescendantOf(cLoc, !criteria.isDeep());
      }
      catch (RepositoryException e)
      {
         return false;
      }
   }

   private boolean isIdentifierMatch(ListenerCriteria criteria, ItemData item)
   {

      if (criteria.getIdentifier() == null)
      {
         return true;
      }

      // associated parent is node itself for node and parent for property ????
      for (int i = 0; i < criteria.getIdentifier().length; i++)
      {
         if (item.isNode() && criteria.getIdentifier()[i].equals(item.getIdentifier()))
         {
            return true;
         }
         else if (!item.isNode() && criteria.getIdentifier()[i].equals(item.getParentIdentifier()))
         {
            return true;
         }
      }
      return false;

   }

   private boolean isNodeTypeMatch(ListenerCriteria criteria, ItemData item, ExtendedSession userSession,
      PlainChangesLog changesLog) throws RepositoryException
   {
      if (criteria.getNodeTypeName() == null)
      {
         return true;
      }

      NodeData node = (NodeData)workspaceDataManager.getItemData(item.getParentIdentifier());
      if (node == null)
      {
         // check if parent exists in changes log
         List<ItemState> states = changesLog.getAllStates();
         for (int i = states.size() - 1; i >= 0; i--)
         {
            if (states.get(i).getData().getIdentifier().equals(item.getParentIdentifier()))
            {
               // parent found
               node = (NodeData)states.get(i).getData();
               break;
            }
         }

         if (node == null)
         {
            LOG.warn("Item's " + item.getQPath().getAsString() + " associated parent (" + item.getParentIdentifier()
               + ") can't be found nor in workspace nor in current changes. Nodetype filter is rejected.");
            return false;
         }
      }

      NodeTypeDataManager ntManager = ((ExtendedWorkspace)userSession.getWorkspace()).getNodeTypesHolder();
      LocationFactory locationFactory = userSession.getLocationFactory();
      for (int i = 0; i < criteria.getNodeTypeName().length; i++)
      {
         InternalQName name = locationFactory.parseJCRName(criteria.getNodeTypeName()[i]).getInternalName();
         NodeTypeData criteriaNT = ntManager.getNodeType(name);
         InternalQName[] testQNames;
         if (criteriaNT == null)
         {
            LOG.warn("The nodetype '{}' defined in the criteria of the listener is unknown.", criteria.getNodeTypeName()[i]);
            continue;
         }
         else if (criteriaNT.isMixin())
         {
            testQNames = node.getMixinTypeNames();
         }
         else
         {
            testQNames = new InternalQName[1];
            testQNames[0] = node.getPrimaryTypeName();
         }
         if (ntManager.isNodeType(criteriaNT.getName(), testQNames))
         {
            return true;
         }
      }
      return false;
   }

   private int eventType(ItemState state) throws RepositoryException
   {

      if (state.getData().isNode())
      {
         if (state.isAdded() || state.isRenamed() || state.isUpdated())
         {
            return Event.NODE_ADDED;
         }
         else if (state.isDeleted())
         {
            return Event.NODE_REMOVED;
         }
         else if (state.isMoved() || state.isOrdered())
         {
             return ExtendedEvent.NODE_MOVED;
         }
         else if (state.isUpdated())
         {
            return SKIP_EVENT;
         }
         else if (state.isUnchanged())
         {
            return SKIP_EVENT;
         }
      }
      else
      { // property
         if (state.isAdded())
         {
            return Event.PROPERTY_ADDED;
         }
         else if (state.isDeleted())
         {
            return Event.PROPERTY_REMOVED;
         }
         else if (state.isUpdated())
         {
            return Event.PROPERTY_CHANGED;
         }
         else if (state.isUnchanged())
         {
            return SKIP_EVENT;
         }
      }
      throw new RepositoryException("Unexpected ItemState for Node " + ItemState.nameFromValue(state.getState()) + " "
         + state.getData().getQPath().getAsString());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isTXAware()
   {
      return false;
   }
}
