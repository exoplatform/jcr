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

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.jmx.MX4JComponentAdapterFactory;
import org.exoplatform.management.annotations.Managed;
import org.exoplatform.management.annotations.ManagedDescription;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.NamingContext;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.SessionFactory;
import org.exoplatform.services.jcr.impl.core.WorkspaceInitializer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.security.PrivilegedAction;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov </a>
 * @version $Id: WorkspaceContainer.java 11907 2008-03-13 15:36:21Z ksm $
 */

@Managed
@NameTemplate(@Property(key = "workspace", value = "{Name}"))
@NamingContext(@Property(key = "workspace", value = "{Name}"))
public class WorkspaceContainer extends ExoContainer
{

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceContainer");

   private final String name;

   private final RepositoryContainer repositoryContainer;

   public WorkspaceContainer(RepositoryContainer parent, WorkspaceEntry config) throws RepositoryException,
      RepositoryConfigurationException
   {
      // Before repository instantiation
      super(new MX4JComponentAdapterFactory(), parent);

      repositoryContainer = parent;
      this.name = config.getName();
      SecurityHelper.doPrivilegedAction(new PrivilegedAction<Void>()
      {
         public Void run()
         {
            context.setName(repositoryContainer.getContext().getName() + "-" + name);
            return null;
         }
      });
   }

   // Components access methods -------

   @Managed
   @ManagedDescription("The workspace container name")
   public String getName()
   {
      return name;
   }

   public SessionFactory getSessionFactory()
   {
      return (SessionFactory)getComponentInstanceOfType(SessionFactory.class);
   }

   public WorkspaceInitializer getWorkspaceInitializer()
   {
      return (WorkspaceInitializer)getComponentInstanceOfType(WorkspaceInitializer.class);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public synchronized void stop()
   {
      super.stop();
      super.unregisterAllComponents();
   }
}
