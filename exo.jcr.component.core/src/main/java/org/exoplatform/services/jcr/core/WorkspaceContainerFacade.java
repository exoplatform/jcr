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

package org.exoplatform.services.jcr.core;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;
import org.exoplatform.services.jcr.impl.WorkspaceContainer;
import org.exoplatform.services.jcr.impl.WorkspaceResumer;
import org.exoplatform.services.jcr.impl.backup.ResumeException;
import org.exoplatform.services.jcr.impl.backup.SuspendException;
import org.exoplatform.services.jcr.impl.backup.Suspendable;

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS .<br> An entry point to the implementation, used for extending
 * functionality
 * 
 * @author Gennady Azarenkov
 * @version $Id:$
 */

public final class WorkspaceContainerFacade
{

   private final String workspaceName;

   private final WorkspaceContainer container;

   private AtomicBoolean workspaceResumerInit = new AtomicBoolean();

   private WorkspaceResumer workspaceResumer;

   /**
    * @param workspaceName
    * @param container
    */
   public WorkspaceContainerFacade(String workspaceName, WorkspaceContainer container)
   {
      this.workspaceName = workspaceName;
      this.container = container;
   }

   private WorkspaceResumer getWorkspaceResumer()
   {
      if (workspaceResumerInit.get())
         return workspaceResumer;
      // The WorkspaceResumer can be null so to prevent being affected by the auto registration
      // we disable it as we know that it is not needed in that case
      this.workspaceResumer = container.getComponentInstanceOfType(WorkspaceResumer.class, false);
      workspaceResumerInit.set(true);
      return workspaceResumer;
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
    * @return List of Object
    */
   public <T> List<T> getComponentInstancesOfType(Class<T> componentType)
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
         return container.getComponentInstanceOfType((Class<?>)key);
      else
         return container.getComponentInstance(key);
   }

   public void addComponent(Object component)
   {
      if (component instanceof Class)
         container.registerComponentImplementation((Class<?>)component);
      else
         container.registerComponentInstance(component);
   }

   public void addComponent(Object key, Object component)
   {
      container.registerComponentInstance(key, component);
   }

   /**
    * Returns current workspace state.
    */
   public int getState()
   {
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

      if (hasSuspendedComponents && !hasResumedComponents)
      {
         return ManageableRepository.SUSPENDED;
      }
      else if (!hasSuspendedComponents)
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
   public void setState(final int state) throws RepositoryException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }

      try
      {
         SecurityHelper.doPrivilegedExceptionAction(new PrivilegedExceptionAction<Void>()
         {
            public Void run() throws RepositoryException
            {
               switch (state)
               {
                  case ManageableRepository.ONLINE :
                     resume();
                     break;
                  case ManageableRepository.OFFLINE :
                     suspend();
                     break;
                  case ManageableRepository.SUSPENDED :
                     suspend();
                     break;
                  default :
                     return null;
               }
               return null;
            }
         });
      }
      catch (PrivilegedActionException e)
      {
         Throwable cause = e.getCause();
         if (cause instanceof RepositoryException)
         {
            throw new RepositoryException(cause);
         }
         else
         {
            throw new RuntimeException(cause);
         }
      }
   }

   /**
    * Suspend all components in workspace.
    * 
    * @throws RepositoryException
    */
   private void suspend() throws RepositoryException
   {
      WorkspaceResumer workspaceResumer = getWorkspaceResumer();
      if (workspaceResumer != null)
      {
         workspaceResumer.onSuspend();
      }

      List<Suspendable> components = getComponentInstancesOfType(Suspendable.class);

      Comparator<Suspendable> c = new Comparator<Suspendable>()
      {
         public int compare(Suspendable s1, Suspendable s2)
         {
            return s2.getPriority() - s1.getPriority();
         };
      };
      Collections.sort(components, c);

      for (Suspendable component : components)
      {
         try
         {
            if (!component.isSuspended())
            {
               component.suspend();
            }
         }
         catch (SuspendException e)
         {
            throw new RepositoryException("Can't suspend component", e);
         }
      }
   }

   /**
    * Set all components online.
    * 
    * @throws RepositoryException
    */
   private void resume() throws RepositoryException
   {
      WorkspaceResumer workspaceResumer = getWorkspaceResumer();
      if (workspaceResumer != null)
      {
         workspaceResumer.onResume();
      }

      // components should be resumed in reverse order
      List<Suspendable> components = getComponentInstancesOfType(Suspendable.class);
      Comparator<Suspendable> c = new Comparator<Suspendable>()
      {
         public int compare(Suspendable s1, Suspendable s2)
         {
            return s1.getPriority() - s2.getPriority();
         };
      };
      Collections.sort(components, c);

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
            throw new RepositoryException("Can't set component online", e);
         }
      }
   }
}
