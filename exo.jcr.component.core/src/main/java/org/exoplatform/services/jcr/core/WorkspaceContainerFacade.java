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

import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.ReadOnlySupport;
import org.exoplatform.services.jcr.impl.WorkspaceContainer;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;

import java.util.Collections;
import java.util.List;

import javax.jcr.RepositoryException;

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

   /**
    * Returns current workspace state.
    * 
    * @param state
    * @throws RepositoryException
    */
   public int getState()
   {
      boolean hasROComponents = false;
      boolean hasRWComponents = false;
      List<ReadOnlySupport> readOnlyComponents = getComponentInstancesOfType(ReadOnlySupport.class);
      for (ReadOnlySupport component : readOnlyComponents)
      {
         if (component.isReadOnly())
         {
            hasROComponents = true;
         }
         else
         {
            hasRWComponents = true;
         }
      }

      boolean hasSuspendedComponents = false;
      boolean hasResumedComponents = false;
      List<Suspendable> suspendableComponents = getComponentInstancesOfType(Suspendable.class);
      for (Suspendable component : suspendableComponents)
      {
         if (component.isSuspended())
         {
            hasSuspendedComponents = true;
         }
         else
         {
            hasResumedComponents = true;
         }
      }

      if (hasSuspendedComponents && !hasResumedComponents && !hasROComponents)
      {
         return ManageableRepository.SUSPENDED;
      }
      else if (hasROComponents && !hasRWComponents && !hasSuspendedComponents)
      {
         return ManageableRepository.READONLY;
      }
      else if (!hasSuspendedComponents && !hasROComponents)
      {
         return ManageableRepository.ONLINE;
      }
      else
      {
         return ManageableRepository.UNDEFINED;
      }
   }

   /**
    * Set new workspace state.
    * 
    * @param state
    * @throws RepositoryException
    */
   public void setState(int state) throws RepositoryException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }
      
      switch (state)
      {
         case ManageableRepository.ONLINE :
            setOnline();
            break;
         case ManageableRepository.OFFLINE :
            break;
         case ManageableRepository.READONLY :
            setReadOnly(true);
            break;
         case ManageableRepository.SUSPENDED :
            suspend();
            break;
         default :
            return;
      }
   }

   /**
    * Set all components readonly.
    */
   private void setReadOnly(boolean readOnly)
   {
      List<ReadOnlySupport> components = getComponentInstancesOfType(ReadOnlySupport.class);
      for (ReadOnlySupport component : components)
      {
         component.setReadOnly(readOnly);
      }
   }

   /**
    * Suspend all components in workspace.
    * 
    * @throws RepositoryException
    */
   private void suspend() throws RepositoryException
   {
      List<Suspendable> components = getComponentInstancesOfType(Suspendable.class);
      for (Suspendable component : components)
      {
         try
         {
            component.suspend();
         }
         catch (SuspendException e)
         {
            throw new RepositoryException("Can't suspend component", e);
         }
      }
   }

   /**
    * Suspend all components in workspace.
    * 
    * @throws RepositoryException
    */
   private void resume() throws RepositoryException
   {
      List<Suspendable> components = getComponentInstancesOfType(Suspendable.class);
      Collections.reverse(components);

      for (Suspendable component : components)
      {
         try
         {
            if (component.isSuspended())
            {
               component.resume();
            }
         }
         catch (ResumeException e)
         {
            throw new RepositoryException("Can't resume component", e);
         }
      }
   }

   /**
    * Set all components online.
    * 
    * @throws RepositoryException
    */
   private void setOnline() throws RepositoryException
   {
      setReadOnly(false);
      resume();
   }
}
