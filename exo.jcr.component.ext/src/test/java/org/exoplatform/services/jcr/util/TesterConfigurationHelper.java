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
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;

import java.util.ArrayList;
import java.util.Collection;
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

   public ManageableRepository createRepository(ExoContainer container, DatabaseStructureType dbStructureType, String dsName)
      throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryEntry repoEntry = createRepositoryEntry(dbStructureType, null, dsName);
      service.createRepository(repoEntry);

      return service.getRepository(repoEntry.getName());
   }

   /**
   * Create workspace entry. 
   */
   public RepositoryEntry createRepositoryEntry(DatabaseStructureType dbStructureType, String systemWSName, String dsName) throws Exception
   {
      // create system workspace entry
      List<String> ids = new ArrayList<String>();
      ids.add("id");
      WorkspaceEntry wsEntry = createWorkspaceEntry(dbStructureType, dsName, ids);

      if (systemWSName != null)
      {
         wsEntry.setName(systemWSName);
      }

      RepositoryEntry repository = new RepositoryEntry();
      repository.setSystemWorkspaceName(wsEntry.getName());
      repository.setDefaultWorkspaceName(wsEntry.getName());
      repository.setName("repo-" + IdGenerator.generate());
      repository.setSessionTimeOut(3600000);
      repository.setAuthenticationPolicy("org.exoplatform.services.jcr.impl.core.access.JAASAuthenticator");
      repository.setSecurityDomain("exo-domain");
      repository.addWorkspace(wsEntry);

      return repository;
   }
   
   /**
   * Create copy of workspace entry. 
   */
   public WorkspaceEntry copyWorkspaceEntry(WorkspaceEntry baseWorkspaceEntry) throws Exception
   {
      // container entry
      List<SimpleParameterEntry> params = new ArrayList<SimpleParameterEntry>();
      params.addAll(copyCollection(baseWorkspaceEntry.getContainer().getParameters()));
      ContainerEntry containerEntry =
               new ContainerEntry(baseWorkspaceEntry.getContainer().getType(), params);
      containerEntry.setParameters(params);

      // value storage
      List<ValueStorageEntry> list = new ArrayList<ValueStorageEntry>();

      
      for (ValueStorageEntry baseValueStorageEntry : baseWorkspaceEntry.getContainer().getValueStorages())
      {
         List<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();

         for (ValueStorageFilterEntry baseValueStorageFilterEntry : baseValueStorageEntry.getFilters())
         {
            ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
            filterEntry.setPropertyType(baseValueStorageFilterEntry.getPropertyType());
            filterEntry.setPropertyName(baseValueStorageFilterEntry.getPropertyName());
            filterEntry.setAncestorPath(baseValueStorageFilterEntry.getAncestorPath());
            filterEntry.setMinValueSize(baseValueStorageFilterEntry.getMinValueSize());

            vsparams.add(filterEntry);
         }

         List<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
         spe.addAll(copyCollection(baseValueStorageEntry.getParameters()));
         ValueStorageEntry valueStorageEntry = new ValueStorageEntry(baseValueStorageEntry.getType(), spe);
         valueStorageEntry.setId(baseValueStorageEntry.getId());
         valueStorageEntry.setFilters(vsparams);

         // containerEntry.setValueStorages();
         list.add(valueStorageEntry);
      }

      containerEntry.setValueStorages(list);

      // Indexer
      params = new ArrayList<SimpleParameterEntry>();
      params.addAll(copyCollection(baseWorkspaceEntry.getQueryHandler().getParameters()));
      QueryHandlerEntry qEntry =
               new QueryHandlerEntry(baseWorkspaceEntry.getQueryHandler().getType(), params);

      // Cache
      params = new ArrayList<SimpleParameterEntry>();
      params.addAll(copyCollection(baseWorkspaceEntry.getCache().getParameters()));
      CacheEntry cacheEntry = new CacheEntry(params);
      cacheEntry.setType(baseWorkspaceEntry.getCache().getType());

      // Lock
      LockManagerEntry lockManagerEntry = new LockManagerEntry();

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
         lockManagerEntry.putParameterValue("infinispan-cl-cache.jdbc.datasource", baseWorkspaceEntry.getContainer()
            .getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME));
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
      workspaceEntry.setName(baseWorkspaceEntry.getName());
      workspaceEntry.setUniqueName(baseWorkspaceEntry.getUniqueName());
      workspaceEntry.setLockManager(lockManagerEntry);

      return workspaceEntry;

   }

   /**
    * Create copy of repository entry. 
    */
   public RepositoryEntry copyRepositoryEntry(RepositoryEntry baseRepositoryEntry) throws Exception
   {
      List<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();

      for (WorkspaceEntry wsEntry : baseRepositoryEntry.getWorkspaceEntries())
      {
         WorkspaceEntry newWSEntry = copyWorkspaceEntry(wsEntry);

         wsEntries.add(newWSEntry);
      }

      RepositoryEntry newRepositoryEntry = new RepositoryEntry();

      newRepositoryEntry.setSystemWorkspaceName(baseRepositoryEntry.getSystemWorkspaceName());
      newRepositoryEntry.setAccessControl(baseRepositoryEntry.getAccessControl());
      newRepositoryEntry.setAuthenticationPolicy(baseRepositoryEntry.getAuthenticationPolicy());
      newRepositoryEntry.setDefaultWorkspaceName(baseRepositoryEntry.getDefaultWorkspaceName());
      newRepositoryEntry.setName(baseRepositoryEntry.getName());
      newRepositoryEntry.setSecurityDomain(baseRepositoryEntry.getSecurityDomain());
      newRepositoryEntry.setSessionTimeOut(baseRepositoryEntry.getSessionTimeOut());

      newRepositoryEntry.setWorkspaceEntries(wsEntries);
      return newRepositoryEntry;

   }

   /**
    * Create copy of list with SimpleParameterEntry-s
    */
   private Collection<SimpleParameterEntry> copyCollection(Collection<SimpleParameterEntry> base)
   {
      Collection<SimpleParameterEntry> result = new ArrayList<SimpleParameterEntry>();

      for (SimpleParameterEntry baseParameter : base)
      {
         result.add(new SimpleParameterEntry(baseParameter.getName(), baseParameter.getValue()));
      }

      return result;
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(DatabaseStructureType dbStructureType, String dsName) throws Exception
   {
      List<String> ids = new ArrayList<String>();
      ids.add("id");

      return createWorkspaceEntry(dbStructureType, dsName, ids);
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(DatabaseStructureType dbStructureType, String dsName, List<String> valueStorageIds)
      throws Exception
   {
      if (dsName == null)
      {
         dsName = createDatasource();
      }

      String id = IdGenerator.generate();
      String wsName = "ws_" + id;

      // container entry
      List<SimpleParameterEntry> params = new ArrayList<SimpleParameterEntry>();
      params.add(new SimpleParameterEntry("source-name", dsName));
      params.add(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE, dbStructureType.toString()));
      params.add(new SimpleParameterEntry("max-buffer-size", "204800"));
      params.add(new SimpleParameterEntry("dialect", "auto"));
      params.add(new SimpleParameterEntry("swap-directory", "target/temp/swap/" + wsName));

      ContainerEntry containerEntry =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", params);
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
               new ValueStorageEntry("org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage", spe);
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
      List<SimpleParameterEntry> cacheParams = new ArrayList<SimpleParameterEntry>();
      cacheParams.add(new SimpleParameterEntry("maxSize", "2000"));
      cacheParams.add(new SimpleParameterEntry("liveTime", "20m"));
      CacheEntry cacheEntry = new CacheEntry(cacheParams);
      cacheEntry.setType("org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl");

      // Lock
      LockManagerEntry lockManagerEntry = new LockManagerEntry();
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
      workspaceEntry.setLockManager(lockManagerEntry);
      workspaceEntry.setContainer(containerEntry);
      workspaceEntry.setCache(cacheEntry);
      workspaceEntry.setQueryHandler(qEntry);
      workspaceEntry.setName(wsName);
      workspaceEntry.setUniqueName(wsName);

      return workspaceEntry;
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
