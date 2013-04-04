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
 * @LevelAPI Platform
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
    * @return returns the action instance
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
   /**
    * @return returns the action ClassName
    */
   public String getActionClassName()
   {
      return actionClassName;
   }
   /**
    * @return returns the event type
    */
   public String getEventTypes()
   {
      return eventTypes;
   }
   /**
    * @return returns the node type
    */
   public String getNodeTypes()
   {
      return nodeTypes;
   }
   /**
    * @return returns the action configuration path
    */
   public String getPath()
   {
      return path;
   }
   /**
    * @return returns the associated workspace
    */
   public String getWorkspace()
   {
      return workspace;
   }

   public boolean isDeep()
   {
      return isDeep;
   }
   /**
    * @param actionClassName the action ClassName to set
    */
   public void setActionClassName(String actionClassName)
   {
      this.actionClassName = actionClassName;
   }

   public void setDeep(boolean isDeep)
   {
      this.isDeep = isDeep;
   }
   /**
    * @param eventTypes the event type to set
    */
   public void setEventTypes(String eventTypes)
   {
      this.eventTypes = eventTypes;
   }
   /**
    * @param nodeTypes the node type to set
    */
   public void setNodeTypes(String nodeTypes)
   {
      this.nodeTypes = nodeTypes;
   }
   /**
    * @param path the the action configuration path
    */
   public void setPath(String path)
   {
      this.path = path;
   }
   /**
    * @param workspace the associated workspace to set
    */
   public void setWorkspace(String workspace)
   {
      this.workspace = workspace;
   }
}
