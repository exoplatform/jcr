/**
 * 
 */
/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.jcr.ext.organization;

import org.exoplatform.container.configuration.ConfigurationException;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.registry.RegistryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.BaseOrganizationService;
import org.picocontainer.Startable;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS. <br>
 * Initialization will be performed via OrganizationServiceJCRInitializer. <br>
 * Date: 24.07.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter
 *         Nedonosko</a>
 * @version $Id: JCROrganizationServiceImpl.java 33732 2009-07-08 15:00:43Z
 *          pnedonosko $
 */
public class JCROrganizationServiceImpl extends BaseOrganizationService implements Startable
{

   /**
    * The name of parameter that contains repository name.
    */
   public static final String REPOSITORY_NAME = "repository";

   /**
    * The name of parameter that contains storage path.
    */
   public static final String STORAGE_PATH = "storage-path";

   /**
    * The name of parameter that contains workspace name.
    */
   public static final String STORAGE_WORKSPACE = "storage-workspace";

   /**
    * The name of parameter that contains enable cache.
    */
   public static final String CACHE_ENABLED = "cache-enabled";

   /**
    * Default storage path.
    */
   public static final String STORAGE_PATH_DEFAULT = "/exo:organization";

   /**
    * Repository service.
    */
   protected RepositoryService repositoryService;

   /**
    * Registry service.
    */
   protected RegistryService registryService;

   /**
    * Contain passed value of storage path in parameters.
    */
   protected String storagePath;

   /**
    * Contain passed value of repository name in parameters.
    */
   protected String repositoryName;

   /**
    * Contain passed value of workspace name in parameters.
    */
   protected String storageWorkspace;

   /**
    * Contain passed value of cache enabled in parameters.
    */
   protected boolean cacheEnabled;

   /**
    * Cache for organization service entities.
    */
   protected JCRCacheHandler cacheHandler;

   /**
    * The node to store groups.
    */
   public static final String STORAGE_JOS_GROUPS = "jos:groups";

   /**
    * The node to store membership types.
    */
   public static final String STORAGE_JOS_MEMBERSHIP_TYPES = "jos:membershipTypes";

   /**
    * The node to storage users.
    */
   public static final String STORAGE_JOS_USERS = "jos:users";

   /**
    * The child node to store user additional information.
    */
   public static final String JOS_PROFILE = "jos:profile";

   /**
    * The child node of group node where memberships are stored.
    */
   public static final String JOS_MEMBERSHIP = "jos:memberships";

   /**
    * The node that identifies the membership type * or any.
    */
   public static final String JOS_MEMBERSHIP_TYPE_ANY = "jos:membershipTypeAny";

   /**
    * The node that identifies the membership type * or any.
    */
   private static final String JOS_DESCRIPTION_TYPE_ANY = "Any membership type";

   /**
    * The group nodetype.
    */
   public static final String JOS_HIERARCHY_GROUP_NODETYPE = "jos:hierarchyGroup-v2";

   /**
    * The users nodetype.
    */
   public static final String JOS_USERS_NODETYPE = "jos:user-v2";

   /**
    * The mixin type and property used as a marker to indicate that a node is disabled.
    */
   public static final String JOS_DISABLED = "jos:disabled";

   /**
    * The storage nodetype.
    */
   public static final String STORAGE_NODETYPE = "jos:organizationStorage-v2";

   /**
    * The users storage nodetype.
    */
   public static final String STORAGE_JOS_USERS_NODETYPE = "jos:organizationUsers-v2";

   /**
    * The groups storage nodetype.
    */
   public static final String STORAGE_JOS_GROUPS_NODETYPE = "jos:organizationGroups-v2";

   /**
    * The membership types storage nodetype.
    */
   public static final String STORAGE_JOS_MEMBERSHIP_TYPES_NODETYPE = "jos:organizationMembershipTypes-v2";

   /**
    * Default cache enabled.
    */
   public static final boolean CACHE_ENABLED_DEFAULT = true;

   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo-jcr-services.JCROrganizationService");

   /**
    * JCROrganizationServiceImpl constructor. Without registry service.
    */
   public JCROrganizationServiceImpl(InitParams params, RepositoryService repositoryService, CacheService cservice)
      throws ConfigurationException
   {
      this(params, repositoryService, null, cservice);
   }

   /**
    * JCROrganizationServiceImpl constructor.
    */
   public JCROrganizationServiceImpl(InitParams initParams, RepositoryService repositoryService,
      RegistryService registryService, CacheService cservice) throws ConfigurationException
   {
      this.repositoryService = repositoryService;
      this.registryService = registryService;
      initializeParameters(initParams);

      this.cacheHandler = new JCRCacheHandler(cservice, this, cacheEnabled);

      if (initParams == null)
      {
         throw new ConfigurationException("Initialization parameters expected !!!");
      }

      // create DAO object
      membershipDAO_ = new MembershipHandlerImpl(this);
      groupDAO_ = new GroupHandlerImpl(this);
      userDAO_ = new UserHandlerImpl(this);
      userProfileDAO_ = new UserProfileHandlerImpl(this);
      membershipTypeDAO_ = new MembershipTypeHandlerImpl(this);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void start()
   {
      try
      {
         MigrationTool migrationTool = new MigrationTool(this);
         if (migrationTool.migrationRequired())
         {
            LOG.info("Detected old organization service structure.");
            migrationTool.migrate();
         }

         Session session = getStorageSession();
         try
         {
            session.getItem(this.storagePath);
            // if found do nothing, the storage was initialized before.
         }
         catch (PathNotFoundException e)
         {
            createStructure();
         }
         finally
         {
            session.logout();
         }
      }
      catch (RepositoryException e)
      {
         throw new IllegalArgumentException("Can not configure storage", e);
      }

      super.start();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void stop()
   {
      super.stop();
   }

   /**
    * Returns cache.
    */
   JCRCacheHandler getCacheHandler()
   {
      return cacheHandler;
   }

   /**
    * Return org-service actual storage path.
    * 
    * @return org-service storage path
    * @throws RepositoryException if any Exception is occurred
    */
   String getStoragePath() throws RepositoryException
   {
      if (storagePath == null)
      {
         throw new RepositoryException("Can not get storage path because JCROrganizationService is not started");
      }

      return storagePath;
   }

   /**
    * Creates storage structure.
    * 
    * @throws RepositoryException if any Exception is occurred
    */
   void createStructure() throws RepositoryException
   {
      Session session = getStorageSession();
      try
      {
         Node storage = session.getRootNode().addNode(storagePath.substring(1), STORAGE_NODETYPE);

         storage.addNode(STORAGE_JOS_USERS, STORAGE_JOS_USERS_NODETYPE);
         storage.addNode(STORAGE_JOS_GROUPS, STORAGE_JOS_GROUPS_NODETYPE);
         Node storageTypesNode = storage.addNode(STORAGE_JOS_MEMBERSHIP_TYPES, STORAGE_JOS_MEMBERSHIP_TYPES_NODETYPE);
         Node anyNode = storageTypesNode.addNode(JOS_MEMBERSHIP_TYPE_ANY);
         anyNode.setProperty(MembershipTypeHandlerImpl.MembershipTypeProperties.JOS_DESCRIPTION, JOS_DESCRIPTION_TYPE_ANY);

         session.save(); // storage done configure
      }
      finally
      {
         session.logout();
      }
   }

   /**
    * Return system Session to org-service storage workspace. For internal use
    * only.
    * 
    * @return system session
    * @throws RepositoryException if any Exception is occurred
    */
   Session getStorageSession() throws RepositoryException
   {
      try
      {
         ManageableRepository repository = getWorkingRepository();

         String workspaceName = storageWorkspace;
         if (workspaceName == null)
         {
            workspaceName = repository.getConfiguration().getDefaultWorkspaceName();
         }

         return repository.getSystemSession(workspaceName);
      }
      catch (RepositoryConfigurationException e)
      {
         throw new RepositoryException("Can not get system session", e);
      }
   }

   /**
    * Read parameters from file.
    */
   private void initializeParameters(InitParams initParams)
   {
      ValueParam paramRepository = initParams.getValueParam(REPOSITORY_NAME);
      repositoryName = paramRepository != null ? paramRepository.getValue() : null;

      ValueParam paramStoragePath = initParams.getValueParam(STORAGE_PATH);
      storagePath = paramStoragePath != null ? paramStoragePath.getValue() : STORAGE_PATH_DEFAULT;

      ValueParam paramStorageWorkspace = initParams.getValueParam(STORAGE_WORKSPACE);
      storageWorkspace = paramStorageWorkspace != null ? paramStorageWorkspace.getValue() : null;

      ValueParam paramDisableCache = initParams.getValueParam(CACHE_ENABLED);
      cacheEnabled =
         paramDisableCache != null ? Boolean.parseBoolean(paramDisableCache.getValue()) : CACHE_ENABLED_DEFAULT;

      if (repositoryName != null)
      {
         LOG.info("Repository from configuration file: " + repositoryName);
      }

      if (storageWorkspace != null)
      {
         LOG.info("Workspace from configuration file: " + storageWorkspace);
      }

      if (storagePath != null)
      {
         LOG.info("Root node from configuration file: " + storagePath);
      }

      LOG.info("Cache is " + (cacheEnabled ? "enabled" : "disabled"));
   }

   /**
    * Returns working repository. If repository name is configured then it will be returned 
    * otherwise the current repository is used.
    */
   protected ManageableRepository getWorkingRepository() throws RepositoryException, RepositoryConfigurationException
   {
      return repositoryName != null ? repositoryService.getRepository(repositoryName) : repositoryService
         .getCurrentRepository();
   }
}
