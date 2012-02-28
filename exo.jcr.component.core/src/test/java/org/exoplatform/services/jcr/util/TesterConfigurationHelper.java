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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

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
   private static Log log = ExoLogger.getLogger("exo.jcr.component.core.ConfigurationHelper");

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

   public ManageableRepository createRepository(ExoContainer container, boolean isMultiDb, String dsName)
      throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryEntry repoEntry = createRepositoryEntry(isMultiDb, null, dsName, true);
      service.createRepository(repoEntry);
      service.getConfig().retain();

      return service.getRepository(repoEntry.getName());
   }

   public ManageableRepository createRepository(ExoContainer container, boolean isMultiDb, boolean cacheEnabled,
      boolean cacheShared) throws Exception
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryEntry repoEntry = createRepositoryEntry(isMultiDb, null, null, cacheEnabled, cacheShared);
      service.createRepository(repoEntry);
      service.getConfig().retain();

      return service.getRepository(repoEntry.getName());
   }

   public ManageableRepository createRepository(ExoContainer container, boolean isMultiDb, boolean cacheEnabled)
      throws Exception
   {
      return createRepository(container, isMultiDb, cacheEnabled, false);
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
   public RepositoryEntry createRepositoryEntry(boolean isMultiDb, String systemWSName, String dsName,
      boolean cacheEnabled) throws Exception
   {
      return createRepositoryEntry(isMultiDb, systemWSName, dsName, cacheEnabled, false);
   }

   /**
   * Create workspace entry. 
   */
   public RepositoryEntry createRepositoryEntry(boolean isMultiDb, String systemWSName, String dsName,
      boolean cacheEnabled, boolean cacheShared) throws Exception
   {
      // create system workspace entry
      List<String> ids = new ArrayList<String>();
      ids.add("id");
      WorkspaceEntry wsEntry = createWorkspaceEntry(isMultiDb, dsName, ids, cacheEnabled, cacheShared);

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
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(boolean isMultiDb, String dsName) throws Exception
   {
      List<String> ids = new ArrayList<String>();
      ids.add("id");

      return createWorkspaceEntry(isMultiDb, dsName, ids, true);
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(boolean isMultiDb, String dsName, List<String> valueStorageIds,
      boolean cacheEnabled) throws Exception
   {
      return createWorkspaceEntry(isMultiDb, dsName, valueStorageIds, cacheEnabled, false);
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(boolean isMultiDb, String dsName, List<String> valueStorageIds,
      boolean cacheEnabled, boolean cacheShared) throws Exception
   {
      if (dsName == null)
      {
         dsName = createDatasource();
      }

      String id = IdGenerator.generate();
      String wsName = "ws-" + id;

      // container entry
      List params = new ArrayList();
      params.add(new SimpleParameterEntry("source-name", dsName));
      params.add(new SimpleParameterEntry("multi-db", isMultiDb ? "true" : "false"));
      params.add(new SimpleParameterEntry("max-buffer-size", "204800"));
      params.add(new SimpleParameterEntry("dialect", "auto"));
      params.add(new SimpleParameterEntry("swap-directory", "target/temp/swap/" + wsName));

      ContainerEntry containerEntry =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer",
            (ArrayList)params);
      containerEntry.setParameters(params);

      // value storage
      ArrayList list = new ArrayList();
      if (valueStorageIds != null)
      {
         for (String vsId : valueStorageIds)
         {
            ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
            ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
            filterEntry.setPropertyType("Binary");
            vsparams.add(filterEntry);

            ValueStorageEntry valueStorageEntry =
               new ValueStorageEntry("org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage",
                  vsparams);
            ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
            spe.add(new SimpleParameterEntry("path", "target/temp/values/" + wsName + "-" + vsId));
            valueStorageEntry.setId(vsId);
            valueStorageEntry.setParameters(spe);
            valueStorageEntry.setFilters(vsparams);

            // containerEntry.setValueStorages();
            containerEntry.setParameters(params);
            list.add(valueStorageEntry);
         }
      }

      containerEntry.setValueStorages(list);

      // Indexer
      params = new ArrayList();
      params.add(new SimpleParameterEntry("index-dir", "target/temp/index/" + wsName));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", params);

      // Cache
      ArrayList cacheParams = new ArrayList();
      cacheParams.add(new SimpleParameterEntry("maxSize", "2000"));
      cacheParams.add(new SimpleParameterEntry("liveTime", "20m"));
      CacheEntry cacheEntry = new CacheEntry(cacheParams);
      cacheEntry.setEnabled(cacheEnabled);
      cacheEntry.setType("org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl");

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
         // JBC Lock
         lockManagerEntry.setType("org.exoplatform.services.jcr.impl.core.lock.jbosscache.CacheableLockManagerImpl");
         lockManagerEntry.putParameterValue("jbosscache-configuration", "conf/standalone/test-jbosscache-lock.xml");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.table.name", "jcrlocks");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.table.create", "true");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.table.drop", "false");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.table.primarykey",
            "jcrlocks_" + IdGenerator.generate());
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.fqn.column", "fqn");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.node.column", "node");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.parent.column", "parent");
         lockManagerEntry.putParameterValue("jbosscache-cl-cache.jdbc.datasource", dsName);
         lockManagerEntry.putParameterValue("jbosscache-shareable", String.valueOf(cacheShared));
      }

      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setContainer(containerEntry);
      workspaceEntry.setCache(cacheEntry);
      workspaceEntry.setQueryHandler(qEntry);
      workspaceEntry.setLockManager(lockManagerEntry);
      workspaceEntry.setName(wsName);
      workspaceEntry.setUniqueName(wsName);

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

   public List<String> getValueStorageIds(ArrayList<ValueStorageEntry> entries)
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
