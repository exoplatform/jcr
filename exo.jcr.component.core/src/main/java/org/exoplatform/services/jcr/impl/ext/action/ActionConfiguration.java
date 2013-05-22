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
 * Created by The eXo Platform SAS</br>
 *
 * The ActionConfiguration bean
 * 
 * @author Gennady Azarenkov
 * @version $Id: ActionConfiguration.java 11907 2008-03-13 15:36:21Z ksm $
 * @LevelAPI Provisional
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
    * @return returns the full qualified name of the action
    */
   public String getActionClassName()
   {
      return actionClassName;
   }

   /**
    * @return returns a comma separated value corresponding to a list of event types
    * for which we expect the action to be triggered
    */
   public String getEventTypes()
   {
      return eventTypes;
   }

   /**
    * @return returns a comma separated value corresponding to a list of node types
    * for which we expect the action to be triggered
    */
   public String getNodeTypes()
   {
      return nodeTypes;
   }

   /**
    * @return returns a comma separated value corresponding to a list of paths
    * for which we expect the action to be triggered
    */
   public String getPath()
   {
      return path;
   }

   /**
    * @return returns the name of the workspace on which the action will be enabled
    */
   public String getWorkspace()
   {
      return workspace;
   }

   /**
    * Indicates whether we need to limit the scope of the action to the items located
    * directly under the list of provided paths or the descendants should be included too
    * 
    */
   public boolean isDeep()
   {
      return isDeep;
   }

   /**
    * @param actionClassName the full qualified name of the action to set
    */
   public void setActionClassName(String actionClassName)
   {
      this.actionClassName = actionClassName;
   }

   /**
    * Sets the flag indicating if the action must be applied to direct children of the
    * list of provided paths or to the the descendants too
    * 
    * @param isDeep if set to <code>true</code> the descendants will be included to the
    * scope of the action
    */
   public void setDeep(boolean isDeep)
   {
      this.isDeep = isDeep;
   }

   /**
    * @param eventTypes a comma separated value corresponding to a list of node types
    * for which we expect the action to be triggered
    */
   public void setEventTypes(String eventTypes)
   {
      this.eventTypes = eventTypes;
   }

   /**
    * @param nodeTypes a comma separated value corresponding to a list of node types
    * for which we expect the action to be triggered
    */
   public void setNodeTypes(String nodeTypes)
   {
      this.nodeTypes = nodeTypes;
   }

   /**
    * @param path a comma separated value corresponding to a list of paths
    * for which we expect the action to be triggered
    */
   public void setPath(String path)
   {
      this.path = path;
   }

   /**
    * @param workspace the name of the workspace on which the action will be enabled
    */
   public void setWorkspace(String workspace)
   {
      this.workspace = workspace;
   }
}
