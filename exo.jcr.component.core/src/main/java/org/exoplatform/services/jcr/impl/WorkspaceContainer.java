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

package org.exoplatform.services.jcr.impl;

import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.container.ExoContainer;
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

   /**
    * The serial version UID
    */
   private static final long serialVersionUID = 6960318261888349500L;

   protected static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.WorkspaceContainer");

   private final String name;

   private final RepositoryContainer repositoryContainer;

   public WorkspaceContainer(RepositoryContainer parent, WorkspaceEntry config) throws RepositoryException,
      RepositoryConfigurationException
   {
      // Before repository instantiation
      super(parent);

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
      return (SessionFactory)getComponentInstanceOfType(SessionFactory.class, false);
   }

   public WorkspaceInitializer getWorkspaceInitializer()
   {
      return (WorkspaceInitializer)getComponentInstanceOfType(WorkspaceInitializer.class, false);
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
