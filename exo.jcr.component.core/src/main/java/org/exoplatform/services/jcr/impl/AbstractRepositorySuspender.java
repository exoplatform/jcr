/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
   protected final ManageableRepository repository;

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
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

      repository.setState(ManageableRepository.SUSPENDED);
   }

   /**
    * Resume repository. All previously suspended threads continue working.
    */
   protected void resumeRepository() throws RepositoryException
   {
      // Need privileges to manage repository.
      SecurityHelper.validateSecurityPermission(JCRRuntimePermissions.MANAGE_REPOSITORY_PERMISSION);

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
