/*
 * Copyright (C) 2012 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.ext.registry.RegistryService;

/**
 * @author <a href="dvishinskiy@exoplatform.com">Dmitriy Vishinskiy</a>
 * @version $Id:$
 */
public class TesterJCROrgService extends JCROrganizationServiceImpl
{

   private String originalWorkspaceName;

   /**
    * Overloaded parent constructor. Disables cache for testing purposes.
    * @param params
    * @param repositoryService
    * @param cservice
    * @throws ConfigurationException
    */
   public TesterJCROrgService(InitParams params, RepositoryService repositoryService, CacheService cservice)
      throws ConfigurationException
   {
      super(disableCache(params), repositoryService, cservice);
   }

   /**
    * Overloaded parent constructor. Disables cache for testing purposes.
    * @param initParams
    * @param repositoryService
    * @param registryService
    * @param cservice
    * @throws ConfigurationException
    */
   public TesterJCROrgService(InitParams initParams, RepositoryService repositoryService,
      RegistryService registryService, CacheService cservice) throws ConfigurationException
   {
      super(disableCache(initParams), repositoryService, registryService, cservice);
   }

   /**
    * Disables cache. For testing purposes only.
    * @param params InitParams.
    * @return InitParams with disabled cache.
    */
   private static InitParams disableCache(InitParams params)
   {
      ValueParam cacheParam = new ValueParam();
      cacheParam.setName(JCROrganizationServiceImpl.CACHE_ENABLED);
      cacheParam.setValue("false");
      params.addParam(cacheParam);
      return params;
   }

   /**
    * Saves current workspace used by OrganizationService.
    */
   public void saveStorageWorkspaceName()
   {
      this.originalWorkspaceName = this.storageWorkspace;
   }

   /**
    * Sets current workspace used by OrganizationService.
    * @param workspaceName
    */
   public void setStorageWorkspace(String workspaceName)
   {
      this.storageWorkspace = workspaceName;
   }

   /**
    * Restores previously saved workspace.
    */
   public void restoreStorageWorkspaceName()
   {
      this.storageWorkspace = this.originalWorkspaceName;
   }

}
