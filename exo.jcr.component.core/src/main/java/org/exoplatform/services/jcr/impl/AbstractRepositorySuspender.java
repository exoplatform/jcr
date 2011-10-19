/*
 * Copyright (C) 2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.security.JCRRuntimePermissions;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 4.10.2011 skarpenko $
 *
 */
public abstract class AbstractRepositorySuspender
{
   /**
    * The current repository.
    */
   private final ManageableRepository repository;

   /**
    * AbstractRepositorySuspender constructor.
    */
   public AbstractRepositorySuspender(ManageableRepository repository)
   {
      this.repository = repository;
   }

   /**
    * Suspend repository which means that allow only read operations. 
    * All writing threads will wait until resume operations invoked.
    */
   protected void suspendRepository() throws RepositoryException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }
      repository.setState(ManageableRepository.SUSPENDED);
   }

   /**
    * Resume repository. All previously suspended threads continue working.
    */
   protected void resumeRepository() throws RepositoryException
   {
      // Need privileges to manage repository.
      SecurityManager security = System.getSecurityManager();
      if (security != null)
      {
         security.checkPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);
      }
      repository.setState(ManageableRepository.ONLINE);
   }

   /**
    * Returns repository state title.
    */
   protected String getRepositoryStateTitle()
   {
      return repository.getStateTitle();
   }

   /**
    * Returns repository state.
    */
   protected int getRepositoryState()
   {
      return repository.getState();
   }

}
