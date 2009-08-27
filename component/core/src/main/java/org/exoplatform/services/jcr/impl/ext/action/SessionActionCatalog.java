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
package org.exoplatform.services.jcr.impl.ext.action;

import javax.jcr.RepositoryException;
import javax.jcr.observation.Event;

import org.exoplatform.services.log.Log;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.command.action.Action;
import org.exoplatform.services.command.action.ActionCatalog;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.core.LocationFactory;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.observation.ExtendedEvent;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: SessionActionCatalog.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class SessionActionCatalog
   extends ActionCatalog
{

   private static Log log = ExoLogger.getLogger("jcr.SessionActionCatalog");

   private final LocationFactory locFactory;

   private final NodeTypeDataManager typeDataManager;

   public SessionActionCatalog(RepositoryService repService) throws RepositoryException
   {

      RepositoryImpl rep = (RepositoryImpl) repService.getCurrentRepository();
      this.locFactory = rep.getLocationFactory();
      this.typeDataManager = rep.getNodeTypeManager().getNodeTypesHolder();
   }

   public void addPlugin(ComponentPlugin plugin)
   {
      if (plugin instanceof AddActionsPlugin)
      {
         AddActionsPlugin cplugin = (AddActionsPlugin) plugin;
         for (ActionConfiguration ac : cplugin.getActions())
         {
            try
            {

               SessionEventMatcher matcher =
                        new SessionEventMatcher(getEventTypes(ac.getEventTypes()), getPaths(ac.getPath()), ac.isDeep(),
                                 getWorkspaces(ac.getWorkspace()), getNames(ac.getNodeTypes()), typeDataManager);

               Action action = (Action) Class.forName(ac.getActionClassName()).newInstance();
               addAction(matcher, action);
            }
            catch (Exception e)
            {
               e.printStackTrace();
            }
         }
      }
   }

   private InternalQName[] getNames(String names) throws RepositoryException
   {
      if (names == null)
         return null;

      String[] nameList = names.split(",");
      InternalQName[] qnames = new InternalQName[nameList.length];
      for (int i = 0; i < nameList.length; i++)
      {
         qnames[i] = locFactory.parseJCRName(nameList[i]).getInternalName();
      }
      return qnames;
   }

   private QPath[] getPaths(String paths) throws RepositoryException
   {
      if (paths == null)
         return null;

      String[] pathList = paths.split(",");
      QPath[] qpaths = new QPath[pathList.length];
      for (int i = 0; i < pathList.length; i++)
      {
         qpaths[i] = locFactory.parseAbsPath(pathList[i]).getInternalPath();
      }
      return qpaths;
   }

   private String[] getWorkspaces(String workspaces) throws RepositoryException
   {
      if (workspaces == null)
         return null;
      return workspaces.split(",");
   }

   private static int getEventTypes(String names)
   {
      if (names == null)
         return -1;

      String[] nameList = names.split(",");
      int res = 0;

      for (String name : nameList)
      {
         if (name.equalsIgnoreCase("addNode"))
         {
            res |= Event.NODE_ADDED;
         }
         else if (name.equalsIgnoreCase("addProperty"))
         {
            res |= Event.PROPERTY_ADDED;
         }
         else if (name.equalsIgnoreCase("changeProperty"))
         {
            res |= Event.PROPERTY_CHANGED;
         }
         else if (name.equalsIgnoreCase("addMixin"))
         {
            res |= ExtendedEvent.ADD_MIXIN;
         }
         else if (name.equalsIgnoreCase("removeProperty"))
         {
            res |= Event.PROPERTY_REMOVED;
         }
         else if (name.equalsIgnoreCase("removeNode"))
         {
            res |= Event.NODE_REMOVED;
         }
         else if (name.equalsIgnoreCase("removeMixin"))
         {
            res |= ExtendedEvent.REMOVE_MIXIN;
         }
         else if (name.equalsIgnoreCase("lock"))
         {
            res |= ExtendedEvent.LOCK;
         }
         else if (name.equalsIgnoreCase("unlock"))
         {
            res |= ExtendedEvent.UNLOCK;
         }
         else if (name.equalsIgnoreCase("checkin"))
         {
            res |= ExtendedEvent.CHECKIN;
         }
         else if (name.equalsIgnoreCase("checkout"))
         {
            res |= ExtendedEvent.CHECKOUT;
         }
         else if (name.equalsIgnoreCase("read"))
         {
            res |= ExtendedEvent.READ;
         }
         else
         {
            log.error("Unknown event type '" + name + "' ignored");
         }
      }
      return res;
   }

}
