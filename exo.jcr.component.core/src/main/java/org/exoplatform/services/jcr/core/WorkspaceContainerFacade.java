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
package org.exoplatform.services.jcr.core;

import org.exoplatform.services.jcr.impl.WorkspaceContainer;

import java.util.List;

/**
 * Created by The eXo Platform SAS .<br/> An entry point to the implementation, used for extending
 * functionality
 * 
 * @author Gennady Azarenkov
 * @version $Id:$
 */

public final class WorkspaceContainerFacade
{

   private final String workspaceName;

   private final WorkspaceContainer container;

   /**
    * @param workspaceName
    * @param container
    */
   public WorkspaceContainerFacade(String workspaceName, WorkspaceContainer container)
   {
      this.workspaceName = workspaceName;
      this.container = container;
   }

   /**
    * @return workspace name
    */
   public final String getWorkspaceName()
   {
      return this.workspaceName;
   }

   /**
    * Returns list of components of specific type.
    * 
    * @param componentType
    *          component type
    * @return List<Object>
    */
   public List getComponentInstancesOfType(Class componentType)
   {
      return container.getComponentInstancesOfType(componentType);
   }

   /**
    * @param key
    *          - an internal key of internal component
    * @return the component
    */
   public Object getComponent(Object key)
   {
      if (key instanceof Class)
         return container.getComponentInstanceOfType((Class)key);
      else
         return container.getComponentInstance(key);
   }

   public void addComponent(Object component)
   {
      if (component instanceof Class)
         container.registerComponentImplementation((Class)component);
      else
         container.registerComponentInstance(component);
   }

   public void addComponent(Object key, Object component)
   {
      container.registerComponentInstance(key, component);
   }
}
