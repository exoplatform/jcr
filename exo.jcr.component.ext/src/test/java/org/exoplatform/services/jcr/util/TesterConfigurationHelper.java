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
      RepositoryEntry repoEntry = createRepositoryEntry(isMultiDb, null, dsName);
      service.createRepository(repoEntry);

      return service.getRepository(repoEntry.getName());
   }

   /**
   * Create workspace entry. 
   */
   public RepositoryEntry createRepositoryEntry(boolean isMultiDb, String systemWSName, String dsName) throws Exception
   {
      // create system workspace entry
      List<String> ids = new ArrayList<String>();
      ids.add("id");
      WorkspaceEntry wsEntry = createWorkspaceEntry(isMultiDb, dsName, ids);

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
      ArrayList<SimpleParameterEntry> params = new ArrayList<SimpleParameterEntry>();
      params.addAll(copyList(baseWorkspaceEntry.getContainer().getParameters()));
      ContainerEntry containerEntry =
               new ContainerEntry(baseWorkspaceEntry.getContainer().getType(), params);
      containerEntry.setParameters(params);

      // value storage
      ArrayList<ValueStorageEntry> list = new ArrayList<ValueStorageEntry>();

      
      for (ValueStorageEntry baseValueStorageEntry : baseWorkspaceEntry.getContainer().getValueStorages())
      {
         ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();

         for (ValueStorageFilterEntry baseValueStorageFilterEntry : baseValueStorageEntry.getFilters())
         {
            ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
            filterEntry.setPropertyType(baseValueStorageFilterEntry.getPropertyType());
            filterEntry.setPropertyName(baseValueStorageFilterEntry.getPropertyName());
            filterEntry.setAncestorPath(baseValueStorageFilterEntry.getAncestorPath());
            filterEntry.setMinValueSize(baseValueStorageFilterEntry.getMinValueSize());

            vsparams.add(filterEntry);
         }

         ValueStorageEntry valueStorageEntry =
                  new ValueStorageEntry(baseValueStorageEntry.getType(),vsparams);
         ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
         spe.addAll(copyList(baseValueStorageEntry.getParameters()));
         valueStorageEntry.setId(baseValueStorageEntry.getId());
         valueStorageEntry.setParameters(spe);
         valueStorageEntry.setFilters(vsparams);

         // containerEntry.setValueStorages();
         list.add(valueStorageEntry);
      }

      containerEntry.setValueStorages(list);

      // Indexer
      params = new ArrayList<SimpleParameterEntry>();
      params.addAll(copyList(baseWorkspaceEntry.getQueryHandler().getParameters()));
      QueryHandlerEntry qEntry =
               new QueryHandlerEntry(baseWorkspaceEntry.getQueryHandler().getType(), params);

      // Cache
      params = new ArrayList<SimpleParameterEntry>();
      params.addAll(copyList(baseWorkspaceEntry.getCache().getParameters()));
      CacheEntry cacheEntry = new CacheEntry(params);
      cacheEntry.setType(baseWorkspaceEntry.getCache().getType());

      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setContainer(containerEntry);
      workspaceEntry.setCache(cacheEntry);
      workspaceEntry.setQueryHandler(qEntry);
      workspaceEntry.setName(baseWorkspaceEntry.getName());
      workspaceEntry.setUniqueName(baseWorkspaceEntry.getUniqueName());

      return workspaceEntry;

   }

   /**
    * Create copy of repository entry. 
    */
   public RepositoryEntry copyRepositoryEntry(RepositoryEntry baseRepositoryEntry) throws Exception
   {
      ArrayList<WorkspaceEntry> wsEntries = new ArrayList<WorkspaceEntry>();

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
   private List<SimpleParameterEntry> copyList(List<SimpleParameterEntry> baseArrayList)
   {
      ArrayList<SimpleParameterEntry> arrayList = new ArrayList<SimpleParameterEntry>();

      for (SimpleParameterEntry baseParameter : baseArrayList)
      {
         arrayList.add(new SimpleParameterEntry(baseParameter.getName(), baseParameter.getValue()));
      }

      return arrayList;
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(boolean isMultiDb, String dsName) throws Exception
   {
      List<String> ids = new ArrayList<String>();
      ids.add("id");

      return createWorkspaceEntry(isMultiDb, dsName, ids);
   }

   /**
    * Create workspace entry. 
    */
   public WorkspaceEntry createWorkspaceEntry(boolean isMultiDb, String dsName, List<String> valueStorageIds)
      throws Exception
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
      cacheEntry.setType("org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl");

      WorkspaceEntry workspaceEntry = new WorkspaceEntry();
      workspaceEntry.setContainer(containerEntry);
      workspaceEntry.setCache(cacheEntry);
      workspaceEntry.setQueryHandler(qEntry);
      workspaceEntry.setName(wsName);
      workspaceEntry.setUniqueName(wsName);

      return workspaceEntry;
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
