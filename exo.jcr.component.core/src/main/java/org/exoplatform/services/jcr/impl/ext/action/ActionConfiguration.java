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

import org.exoplatform.services.command.action.Action;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: ActionConfiguration.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class ActionConfiguration
{

   private String actionClassName;

   private String eventTypes;

   private String path;

   private boolean isDeep;

   private String nodeTypes;

   private String workspace;

   private Action action;

   public ActionConfiguration()
   {
   }

   public ActionConfiguration(String actionClassName, String eventTypes, String path, boolean isDeep, String workspace,
      String nodeTypes, Action action)
   {
      this.actionClassName = actionClassName;
      this.action = action;
      this.eventTypes = eventTypes;
      this.path = path;
      this.isDeep = isDeep;
      this.workspace = workspace;
      this.nodeTypes = nodeTypes;
   }

   /**
    * @return the action
    */
   public Action getAction()
   {
      return action;
   }

   /**
    * @param action the action to set
    */
   public void setAction(Action action)
   {
      this.action = action;
   }

   public String getActionClassName()
   {
      return actionClassName;
   }

   public String getEventTypes()
   {
      return eventTypes;
   }

   public String getNodeTypes()
   {
      return nodeTypes;
   }

   public String getPath()
   {
      return path;
   }

   public String getWorkspace()
   {
      return workspace;
   }

   public boolean isDeep()
   {
      return isDeep;
   }

   public void setActionClassName(String actionClassName)
   {
      this.actionClassName = actionClassName;
   }

   public void setDeep(boolean isDeep)
   {
      this.isDeep = isDeep;
   }

   public void setEventTypes(String eventTypes)
   {
      this.eventTypes = eventTypes;
   }

   public void setNodeTypes(String nodeTypes)
   {
      this.nodeTypes = nodeTypes;
   }

   public void setPath(String path)
   {
      this.path = path;
   }

   public void setWorkspace(String workspace)
   {
      this.workspace = workspace;
   }
}
