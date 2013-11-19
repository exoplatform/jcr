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
package org.exoplatform.services.jcr.util;

import org.apache.commons.dbcp.BasicDataSourceFactory;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.RepositoryContainer;
import org.exoplatform.services.jcr.impl.RepositoryServiceImpl;
import org.exoplatform.services.jcr.impl.core.SessionRegistry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jcr.RepositoryException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: ConfigurationHelper.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TesterConfigurationHelper
{
   private static TesterConfigurationHelper instance;

   private TesterConfigurationHelper()
   {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.exoplatform.services.naming.SimpleContextFactory");
   }

   /**
    * Remove repository.
    * @throws RepositoryConfigurationException 
    */
   public void removeRepository(ExoContainer container, String repositoryName) throws RepositoryException,
      RepositoryConfigurationException
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      ManageableRepository mr = service.getRepository(repositoryName);
      for (String wsName : mr.getWorkspaceNames())
      {
         WorkspaceContainerFacade wc = mr.getWorkspaceContainer(wsName);
         SessionRegistry sessionRegistry = (SessionRegistry)wc.getComponent(SessionRegistry.class);
         sessionRegistry.closeSessions(wsName);
      }

      service.removeRepository(repositoryName);
   }

   /**
    * Add new workspace to repository.
    */
   public void addWorkspace(ManageableRepository repository, WorkspaceEntry workspaceEntry)
      throws RepositoryConfigurationException, RepositoryException
   {
      repository.configWorkspace(workspaceEntry);
      repository.createWorkspace(workspaceEntry.getName());
   }

   /**
    * Create new datasource. 
    * 
    * @return datasource name
    */
   public String createDatasource() throws Exception
   {
      String dsName = IdGenerator.generate();

      Properties properties = new Properties();
      properties.setProperty("driverClassName", "org.hsqldb.jdbcDriver");
      properties.setProperty("url", "jdbc:hsqldb:file:target/temp/data/" + dsName);
      properties.setProperty("username", "sa");
      properties.setProperty("password", "");

      DataSource ds = BasicDataSourceFactory.createDataSource(properties);

      new InitialContext().bind(dsName, ds);

      return dsName;

   }

   public RepositoryContainer getRepositoryContainer(ExoContainer container, String repositoryName) throws Exception
   {
      RepositoryServiceImpl service =
         (RepositoryServiceImpl)container.getComponentInstanceOfType(RepositoryService.class);
      return service.getRepositoryContainer(repositoryName);
   }

   public ManageableRepository createRepository(ExoContainer container, DatabaseStructureType dbStructureType,
      String dsName) throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryEntry repoEntry = createRepositoryEntry(dbStructureType, null, dsName, true);
      service.createRepository(repoEntry);
      service.getConfig().retain();

      return service.getRepository(repoEntry.getName());
   }

   public ManageableRepository createRepository(ExoContainer container, DatabaseStructureType dbStructureType,
      boolean cacheEnabled, boolean cacheShared) throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryEntry repoEntry = createRepositoryEntry(dbStructureType, null, null, cacheEnabled, cacheShared);
      service.createRepository(repoEntry);
      service.getConfig().retain();

      return service.getRepository(repoEntry.getName());
   }

   public ManageableRepository createRepository(ExoContainer container, DatabaseStructureType dbStructureType,
      boolean cacheEnabled) throws Exception
   {
      return createRepository(container, dbStructureType, cacheEnabled, false);
   }

   public ManageableRepository createRepository(ExoContainer container, RepositoryEntry repoEntry) throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      service.createRepository(repoEntry);

      return service.getRepository(repoEntry.getName());
   }

   /**
    * Create workspace entry. 
    */
   public RepositoryEntry createRepositoryEntry(DatabaseStructureType dbStructureType, String systemWSName,
      String dsName, boolean cacheEnabled) throws Exception
   {
      return createRepositoryEntry(dbStructureType, systemWSName, dsName, cacheEnabled, false);
   }

   /**
   * Create workspace entry. 
   */
   public RepositoryEntry createRepositoryEntry(DatabaseStructureType dbStructureType, String systemWSName,
      String dsName, boolean cacheEnabled, boolean cacheShared) throws Exception
   {
      String repositoryName = "repo-" + IdGenerator.generate();

      // create system workspace entry
      List<String> ids = new ArrayList<String>();
      ids.add("id");
      WorkspaceEntry wsEntry = createWorkspaceEntry(dbStructureType, dsName, ids, cacheEnabled, cacheShared);

      if (systemWSName != null)
      {
         wsEntry.setName(systemWSName);
      }

      wsEntry.setUniqueName(repositoryName + "_" + wsEntry.getName());

      RepositoryEntry repository = new RepositoryEntry();
      repository.setSystemWorkspaceName(wsEntry.getName());
      repository.setDefaultWorkspaceName(wsEntry.getName());
      repository.setName(repositoryName);
      repository.setSessionTimeOut(3600000);
      repository.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repository.setSecurityDomain("exo-domain");
      repository.addWorkspace(wsEntry);

      return repository;
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(DatabaseStructureType dbStructureType, String dsName) throws Exception
   {
      List<String> ids = new ArrayList<String>();
      ids.add("id");

      return createWorkspaceEntry(dbStructureType, dsName, ids, true);
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(DatabaseStructureType dbStructureType, String dsName,
      List<String> valueStorageIds, boolean cacheEnabled) throws Exception
   {
      return createWorkspaceEntry(dbStructureType, dsName, valueStorageIds, cacheEnabled, false);
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(DatabaseStructureType dbStructureType, String dsName,
      List<String> valueStorageIds, boolean cacheEnabled, boolean cacheShared) throws Exception
   {
      if (dsName == null)
      {
         dsName = createDatasource();
      }

      String id = IdGenerator.generate();
      String wsName = "ws-" + id;

      // container entry
      List<SimpleParameterEntry> params = new ArrayList<SimpleParameterEntry>();
      params.add(new SimpleParameterEntry("source-name", dsName));
      params.add(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE, dbStructureType.toString()));
      params.add(new SimpleParameterEntry("max-buffer-size", "204800"));
      params.add(new SimpleParameterEntry("dialect", "auto"));
      params.add(new SimpleParameterEntry("swap-directory", "target/temp/swap/" + wsName));

      ContainerEntry containerEntry =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer",
            params);
      containerEntry.setParameters(params);

      // value storage
      List<ValueStorageEntry> list = new ArrayList<ValueStorageEntry>();
      if (valueStorageIds != null)
      {
         for (String vsId : valueStorageIds)
         {
            List<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
            ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
            filterEntry.setPropertyType("Binary");
            vsparams.add(filterEntry);

            List<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
            spe.add(new SimpleParameterEntry("path", "target/temp/values/" + wsName + "-" + vsId));

            ValueStorageEntry valueStorageEntry =
                     new ValueStorageEntry("org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage",
                        spe);

            valueStorageEntry.setId(vsId);
            valueStorageEntry.setFilters(vsparams);

            // containerEntry.setValueStorages();
            containerEntry.setParameters(params);
            list.add(valueStorageEntry);
         }
      }

      containerEntry.setValueStorages(list);

      // Indexer
      params = new ArrayList<SimpleParameterEntry>();
      params.add(new SimpleParameterEntry("index-dir", "target/temp/index/" + wsName));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", params);

      // Cache
      CacheEntry cacheEntry = null;

      try
      {
         Class
            .forName("org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.ISPNCacheWorkspaceStorageCache");

         List<SimpleParameterEntry> cacheParams = new ArrayList<SimpleParameterEntry>();
         cacheParams.add(new SimpleParameterEntry("infinispan-configuration",
            "conf/standalone/test-infinispan-config.xml"));
         cacheEntry = new CacheEntry(cacheParams);
         cacheEntry.setEnabled(cacheEnabled);
         cacheEntry
            .setType("org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.ISPNCacheWorkspaceStorageCache");
      }
      catch (ClassNotFoundException e)
      {
         throw e;
      }

      LockManagerEntry lockManagerEntry = new LockManagerEntry();
      lockManagerEntry.putParameterValue("time-out", "15m");

      // ISPN Lock
      try
      {
         Class.forName("org.exoplatform.services.jcr.impl.core.lock.infinispan.ISPNCacheableLockManagerImpl");

         lockManagerEntry
            .setType("org.exoplatform.services.jcr.impl.core.lock.infinispan.ISPNCacheableLockManagerImpl");
         lockManagerEntry.putParameterValue("infinispan-configuration", "conf/standalone/test-infinispan-lock.xml");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.table.name", "lk");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.table.create", "true");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.table.drop", "false");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.id.column", "id");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.data.column", "data");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.timestamp.column", "timestamp");
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.datasource", dsName);
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.connectionFactory",
            "org.infinispan.loaders.jdbc.connectionfactory.ManagedConnectionFactory");
      }
      catch (ClassNotFoundException e)
      {
         throw e;
      }

      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setContainer(containerEntry);
      workspaceEntry.setCache(cacheEntry);
      workspaceEntry.setQueryHandler(qEntry);
      workspaceEntry.setLockManager(lockManagerEntry);
      workspaceEntry.setName(wsName);

      return workspaceEntry;
   }

   public RepositoryEntry copyRepositoryEntry(RepositoryEntry configuration)
   {
      List<WorkspaceEntry> workspases = new ArrayList<WorkspaceEntry>();

      for (WorkspaceEntry ws : configuration.getWorkspaceEntries())
      {
         workspases.add(copyWorkspaceEntry(ws));
      }

      RepositoryEntry repository = new RepositoryEntry();
      repository.setSystemWorkspaceName(configuration.getSystemWorkspaceName());
      repository.setDefaultWorkspaceName(configuration.getDefaultWorkspaceName());
      repository.setName(configuration.getName());
      repository.setSessionTimeOut(configuration.getSessionTimeOut());
      repository.setAuthenticationPolicy(configuration.getAuthenticationPolicy());
      repository.setSecurityDomain(configuration.getSecurityDomain());

      for (WorkspaceEntry ws : workspases)
      {
         repository.addWorkspace(ws);
      }

      return repository;
   }

   private WorkspaceEntry copyWorkspaceEntry(WorkspaceEntry wsEntry)
   {
      // container entry
      List<SimpleParameterEntry> params = new ArrayList<SimpleParameterEntry>();
      params.addAll(wsEntry.getContainer().getParameters());

      ContainerEntry containerEntry = new ContainerEntry(wsEntry.getContainer().getType(), params);
      containerEntry.setParameters(params);

      // value storage
      List<ValueStorageEntry> list = new ArrayList<ValueStorageEntry>();
      if (wsEntry.getContainer().getValueStorages() != null)
      {
         for (ValueStorageEntry vse : wsEntry.getContainer().getValueStorages())
         {
            List<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();

            for (ValueStorageFilterEntry vsfe : vse.getFilters())
            {
               ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
               filterEntry.setPropertyType(vsfe.getPropertyType());
               filterEntry.setPropertyName(vsfe.getPropertyName());
               filterEntry.setMinValueSize(vsfe.getMinValueSize());
               filterEntry.setAncestorPath(vsfe.getAncestorPath());
               vsparams.add(filterEntry);
            }

            List<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
            spe.addAll(vse.getParameters());
            ValueStorageEntry valueStorageEntry = new ValueStorageEntry(vse.getType(), spe);
            valueStorageEntry.setId(vse.getId());
            valueStorageEntry.setFilters(vsparams);

            // containerEntry.setValueStorages();
            containerEntry.setParameters(params);
            list.add(valueStorageEntry);
         }
      }

      containerEntry.setValueStorages(list);

      // Indexer
      params = new ArrayList<SimpleParameterEntry>();
      params.addAll(wsEntry.getQueryHandler().getParameters());
      QueryHandlerEntry qEntry = new QueryHandlerEntry(wsEntry.getQueryHandler().getType(), params);

      // Cache
      List<SimpleParameterEntry> cacheParams = new ArrayList<SimpleParameterEntry>();
      cacheParams.addAll(wsEntry.getCache().getParameters());
      CacheEntry cacheEntry = new CacheEntry(cacheParams);
      cacheEntry.setEnabled(wsEntry.getCache().getEnabled());
      cacheEntry.setType(wsEntry.getCache().getType());

      // Lock
      LockManagerEntry lockManagerEntry = new LockManagerEntry();
      lockManagerEntry.setType(wsEntry.getLockManager().getType());
      List<SimpleParameterEntry> lockParams = new ArrayList<SimpleParameterEntry>();
      lockParams.addAll(wsEntry.getLockManager().getParameters());
      lockManagerEntry.setParameters(lockParams);

      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setContainer(containerEntry);
      workspaceEntry.setCache(cacheEntry);
      workspaceEntry.setQueryHandler(qEntry);
      workspaceEntry.setLockManager(lockManagerEntry);
      workspaceEntry.setName(wsEntry.getName());
      workspaceEntry.setUniqueName(wsEntry.getUniqueName());

      return workspaceEntry;
   }

   public boolean ispnCacheEnabled()
   {
      try
      {
         Class.forName("org.exoplatform.services.jcr.impl.core.lock.infinispan.ISPNCacheableLockManagerImpl");
         return true;
      }
      catch (ClassNotFoundException e)
      {
         return false;
      }

   }

   public List<String> getValueStorageIds(List<ValueStorageEntry> entries)
   {
      List<String> ids = new ArrayList<String>();
      if (entries != null)
      {
         for (ValueStorageEntry entry : entries)
         {
            ids.add(entry.getId());
         }
      }

      return ids;
   }

   public static TesterConfigurationHelper getInstance()
   {
      if (instance == null)
      {
         instance = new TesterConfigurationHelper();
      }

      return instance;
   }
}
