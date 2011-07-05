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
    * @param rEntry - repository Entry - note that datasource must not exist.
    * @param backupId - backup id
    * @param creationProps - storage creation properties 
    * @throws RepositoryConfigurationException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    */
   void createRepository(String backupId, RepositoryEntry rEntry, StorageCreationProperties creationProps)
      throws RepositoryConfigurationException, RepositoryCreationException;

   /**
    * Reserves, validates and creates repository in a simplified form. 
    * 
    * @param rEntry - repository Entry - note that datasource must not exist.
    * @param backupId - backup id
    * @throws RepositoryConfigurationException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    */
   void createRepository(String backupId, RepositoryEntry rEntry) throws RepositoryConfigurationException,
      RepositoryCreationException;

   /**
    * Reserve repository name to prevent repository creation with same name from other place in same time
    * via this service.
    * 
    * @param repositoryName - repositoryName
    * @return repository token. Anyone obtaining a token can later create a repository of reserved name.
    * @throws RepositoryCreationServiceException if can't reserve name
    */
   String reserveRepositoryName(String repositoryName) throws RepositoryCreationException;

   /**
    * Creates repository, using token of already reserved repository name. 
    * Good for cases, when repository creation should be delayed or made asynchronously in dedicated thread. 
    * 
    * @param rEntry - repository entry - note, that datasource must not exist
    * @param backupId - backup id
    * @param rToken - token
    * @param creationProps - storage creation properties
    * @throws RepositoryConfigurationException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    */
   void createRepository(String backupId, RepositoryEntry rEntry, String rToken, StorageCreationProperties creationProps)
      throws RepositoryConfigurationException, RepositoryCreationException;

   /**
    * Creates repository, using token of already reserved repository name. Good for cases, 
    * when repository creation should be delayed or made asynchronously in dedicated thread. 
    * 
    * @param rEntry - repository entry - note, that datasource must not exist
    * @param backupId - backup id
    * @param rToken - token
    * @throws RepositoryConfigurationException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository creation or repository name is absent in reserved list
    */
   void createRepository(String backupId, RepositoryEntry rEntry, String rToken)
      throws RepositoryConfigurationException, RepositoryCreationException;

   /**
    * Remove previously created repository. 
    * 
    * @param repositoryName - the repository name to delete
    * @param forceCloseSessions - indicates if need to close sessions before repository removing, if
    * sessions are opened is it not possbile to remove repository and exception will be thrown
    * @throws RepositoryCreationServiceException
    *          if some exception occurred during repository removing occurred
    */
   void removeRepository(String repositoryName, boolean forceCloseSessions) throws RepositoryCreationException;
}
