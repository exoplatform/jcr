/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.ext.repository.creation;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: RepositoryCreationService.java 111 2008-11-11 11:11:11Z serg $
 */
public interface RepositoryCreationService
{
   /**
    * Reserves, validates and creates repository in a simplified form. 
    * 
    * 1. check possibility to create repository locally
    *    - check existing, pending repository and datasources with same names
    * 2. reserve name and put additional information (ex. ip and port of current machine)
    * 3. check possibility to create repository on others nodes 
    *    - sending to all cluster nodes information about new repository and waiting for answers
    *    - all cluster nodes receive information and check possibility to create repository locally
    *    - send response 
    * 4. reserve name on all nodes of cluster
    * 6. Check that name is reserved
    * 7. Create repository locally from backup
    *    - create related DB
    *    - bind datasources
    *    - restore repository from backup (in synchronous mode)
    * 8. If need to do the same in cluster then send requests to others cluster nodes to create repository and waits for responses
    * 9. On each others cluster nodes:
    *   - bind datasources
    *   - start repository   
    *   - send response
    * 10. Release lock (unreserve name)

    * @param rEntry
    * @param backupId
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    */
   void createRepository(String backupId, RepositoryEntry rEntry) throws RepositoryConfigurationException,
      RepositoryCreationException;

   /**
    * Reserve repository name to prevent repository creation with same name from other place in same time
    * via this service.
    * 
    * 1. check possibility to create repository locally
    *    - check existing, pending repository and datasources with same names
    * 2. reserve name and put additional information (repository name token)
    * 3. check possibility to create repository on others nodes 
    *    - sending to all cluster nodes information about new repository and waiting for answers
    *    - all cluster nodes receive information and check possibility to create repository locally
    *    - send response 
    * 4. reserve name on all nodes of cluster
    *
    * @param repositoryName
    * @return repository token. Anyone obtaining a token can later create a repository of reserved name.
    * @throws RepositoryCreationServiceException
    *          if can't reserve name
    */
   String reserveRepositoryName(String repositoryName) throws RepositoryCreationException;

   /**
    * Creates  repository, using token of already reserved repository name. Good for cases, when repository creation should be delayed or 
    * made asynchronously in dedicated thread. 
    *
    * 1. Check that name is reserved
    * 2. Create repository locally from backup
    *    - create related DB
    *    - bind datasources
    *    - restore repository from backup (in synchronous mode)
    * 3. If need to do the same in cluster then send requests to others cluster nodes to create repository and waits for responses
    * 4. On each others cluster nodes:
    *   - bind datasources
    *   - start repository   
    *   - send response
    * 5. Release lock (unreserve name)
    * 
    * @param rEntry
    * @param backupId
    * @param rToken
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    */
   void createRepository(String backupId, RepositoryEntry rEntry, String rToken)
      throws RepositoryConfigurationException, RepositoryCreationException;
}
