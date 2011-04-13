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
package org.exoplatform.services.jcr.impl.backup;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Allows via JMX suspend and resume repository's components.
 * 
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RepositorySuspendController.java 34360 2009-07-22 23:58:59Z tolusha $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "RepositorySuspendController"))
public class RepositorySuspendController implements Startable
{
   /**
    * Repository ONLINE status.
    */
   private final String ONLINE = "online";

   /**
    * Repository SUSPENDED state.
    */
   private final String SUSPENDED = "suspended";

   /**
    * Undefined state. 
    */
   public final String UNDEFINED = "undefined";

   private final ManageableRepository repository;

   /**
    * Logger.
    */
   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.RepositorySuspendController");

   /**
    * RepositoryController constructor.
    */
   public RepositorySuspendController(ManageableRepository repository)
   {
      this.repository = repository;
   }

   /**
    * Suspend repository which means that allow only read operations. All writing threads will wait until resume operations invoked.
    * 
    * @return repository state
    */
   @Managed
   @ManagedDescription("Suspend repository which means that allow only read operations. All writing threads will wait until resume operations invoked.")
   public String suspend()
   {
      for (Suspendable component : getSuspendableComponents())
      {
         try
         {
            component.suspend();
         }
         catch (SuspendException e)
         {
            log.error("Can't suspend component", e);
         }
      }

      return getState();
   }

   /**
    * Resume repository. All previously suspended threads continue working.
    * 
    * @return repository state
    */
   @Managed
   @ManagedDescription("Resume repository. All previously suspended threads continue working.")
   public String resume()
   {
      List<Suspendable> components = getSuspendableComponents();
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
            log.error("Can't resume component", e);
         }
      }

      return getState();
   }

   /**
    * Returns repository state.
    */
   @Managed
   @ManagedDescription("Returns repository state.")
   public String getState()
   {
      String state = ONLINE;
      
      boolean hasSuspendedComponents = false;
      boolean hasOnlineComponents = false;

      for (Suspendable component : getSuspendableComponents())
      {
         if (component.isSuspended())
         {
            hasSuspendedComponents = true;

            if (hasOnlineComponents)
            {
               return UNDEFINED;
            }

            state = SUSPENDED;
         }
         else
         {
            hasOnlineComponents = true;
            if (hasSuspendedComponents)
            {
               return UNDEFINED;
            }
         }
      }

      return state;
   }

   /**
    * {@inheritDoc}
    */
   public void start()
   {
   }

   /**
    * {@inheritDoc}
    */
   public void stop()
   {
   }

   private List<Suspendable> getSuspendableComponents()
   {
      List<Suspendable> components = new ArrayList<Suspendable>();
      for (String workspaceName : repository.getWorkspaceNames())
      {
         components.addAll(repository.getWorkspaceContainer(workspaceName).getComponentInstancesOfType(
            Suspendable.class));
      }

      return components;
   }
}
