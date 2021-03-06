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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.picocontainer.Startable;

import javax.jcr.RepositoryException;

/**
 * Allows via JMX suspend and resume repository's components.
 * 
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: RepositorySuspendController.java 34360 2009-07-22 23:58:59Z tolusha $
 */
@Managed
@NameTemplate(@Property(key = "service", value = "RepositorySuspendController"))
public class RepositorySuspendController extends AbstractRepositorySuspender implements Startable
{
   /**
    * Logger.
    */
   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.RepositorySuspendController");

   /**
    * RepositoryController constructor.
    */
   public RepositorySuspendController(ManageableRepository repository)
   {
      super(repository);
   }

   /**
    * Suspend repository which means that allow only read operations. 
    * All writing threads will wait until resume operations invoked.
    * 
    * @return repository state
    */
   @Managed
   @ManagedDescription("Suspend repository which means that allow only read operations. " +
            "All writing threads will wait until resume operations invoked.")
   public String suspend()
   {
      try
      {
         suspendRepository();
      }
      catch (RepositoryException e)
      {
         log.error("An exception occured: " + e.getMessage());
      }
      return getRepositoryStateTitle();
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
      try
      {
         resumeRepository();
      }
      catch (RepositoryException e)
      {
         log.error("An exception occured: " + e.getMessage());
      }
      return getRepositoryStateTitle();
   }

   /**
    * Returns repository state.
    */
   @Managed
   @ManagedDescription("Returns repository state.")
   public String getState()
   {
      return getRepositoryStateTitle();
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
}
