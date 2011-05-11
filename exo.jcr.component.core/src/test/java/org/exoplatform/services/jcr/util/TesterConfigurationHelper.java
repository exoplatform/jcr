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
import org.exoplatform.services.jcr.config.LockPersisterEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.ValueStorageFilterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.core.RepositoryImpl;
import org.exoplatform.services.jcr.impl.storage.jdbc.DialectDetecter;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

   private static TesterConfigurationHelper instence;

   private TesterConfigurationHelper()
   {
      System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.exoplatform.services.naming.SimpleContextFactory");
   }

   public void createWorkspace(WorkspaceEntry workspaceEntry, ExoContainer container)
      throws RepositoryConfigurationException, RepositoryException
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep;

      defRep = (RepositoryImpl)service.getDefaultRepository();
      defRep.configWorkspace(workspaceEntry);
      defRep.createWorkspace(workspaceEntry.getName());

   }

   public String getNewDataSource(String type) throws Exception
   {

      String newDS = IdGenerator.generate();
      Properties properties = new Properties();

      properties.setProperty("driverClassName", "org.hsqldb.jdbcDriver");
      String newurl = "jdbc:hsqldb:file:target/temp/data/" + newDS;

      log.info("New url " + newurl);

      properties.setProperty("url", newurl);
      properties.setProperty("username", "sa");
      properties.setProperty("password", "");
      DataSource bds = BasicDataSourceFactory.createDataSource(properties);
      if (!newurl.contains("hsqldb"))
      {
         createDatabase(bds, newDS);
      }

      new InitialContext().bind(newDS, bds);
      return newDS;

   }

   public WorkspaceEntry getNewWs(String wsName, boolean isMultiDb, String dsName, String vsPath, ContainerEntry entry)
      throws Exception
   {
      return getNewWs(wsName, isMultiDb, dsName, vsPath, entry, true);
   }

   public WorkspaceEntry getNewWs(String wsName, boolean isMultiDb, String dsName, String vsPath, ContainerEntry entry,
      boolean newMultiDbDS) throws Exception
   {

      String dbDialect = null;
      if (dsName != null)
      {
         DataSource ds = (DataSource)new InitialContext().lookup(dsName);
         if (ds != null)
         {
            Connection jdbcConn = null;

            jdbcConn = ds.getConnection();
            dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());

         }
      }

      if (newMultiDbDS && (isMultiDb || dsName == null))
      {
         dsName = getNewDataSource("");
      }

      List params = new ArrayList();

      params.add(new SimpleParameterEntry("source-name", dsName));
      params.add(new SimpleParameterEntry("db-type", "generic"));
      params.add(new SimpleParameterEntry("multi-db", isMultiDb ? "true" : "false"));
      params.add(new SimpleParameterEntry("update-storage", "true"));
      params.add(new SimpleParameterEntry("max-buffer-size", "204800"));

      if (dbDialect != null)
      {
         params.add(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_DIALECT, dbDialect));
      }
      else if (entry.getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT) != null)
      {
         params.add(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_DIALECT, entry
            .getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT)));
      }

      String oldSwap = entry.getParameterValue("swap-directory");
      String newSwap = oldSwap.substring(0, oldSwap.lastIndexOf('/')) + '/' + wsName;

      params.add(new SimpleParameterEntry("swap-directory", newSwap));

      ContainerEntry containerEntry =
         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer",
            (ArrayList)params);
      containerEntry.setParameters(params);

      if (vsPath != null)
      {

         ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
         ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
         filterEntry.setPropertyType("Binary");
         vsparams.add(filterEntry);

         ValueStorageEntry valueStorageEntry =
            new ValueStorageEntry("org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage", vsparams);
         ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
         spe.add(new SimpleParameterEntry("path", vsPath));
         valueStorageEntry.setId(IdGenerator.generate());
         valueStorageEntry.setParameters(spe);
         valueStorageEntry.setFilters(vsparams);

         // containerEntry.setValueStorages();
         containerEntry.setParameters(params);
         ArrayList list = new ArrayList(1);
         list.add(valueStorageEntry);

         containerEntry.setValueStorages(list);

      }

      // Indexer
      ArrayList qParams = new ArrayList();
      qParams.add(new SimpleParameterEntry("indexDir", "../temp/index/" + IdGenerator.generate()));
      QueryHandlerEntry qEntry =
         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);

      WorkspaceEntry workspaceEntry =
         new WorkspaceEntry(wsName != null ? wsName : IdGenerator.generate(), "nt:unstructured");
      workspaceEntry.setContainer(containerEntry);

      ArrayList cacheParams = new ArrayList();

      cacheParams.add(new SimpleParameterEntry("maxSize", "2000"));
      cacheParams.add(new SimpleParameterEntry("liveTime", "20m"));
      CacheEntry cacheEntry = new CacheEntry(cacheParams);
      cacheEntry.setType("org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl");

      workspaceEntry.setCache(cacheEntry);

      workspaceEntry.setQueryHandler(qEntry);

      LockManagerEntry lockManagerEntry = new LockManagerEntry();
      lockManagerEntry.setTimeout(900000);
      LockPersisterEntry persisterEntry = new LockPersisterEntry();
      persisterEntry.setType("org.exoplatform.services.jcr.impl.core.lock.FileSystemLockPersister");
      ArrayList lpParams = new ArrayList();
      lpParams.add(new SimpleParameterEntry("path", "../temp/lock"));
      persisterEntry.setParameters(lpParams);
      lockManagerEntry.setPersister(persisterEntry);
      workspaceEntry.setLockManager(lockManagerEntry);

      // workspaceEntry
      return workspaceEntry;
   }

   //   public WorkspaceEntry getNewWsOnDataSource(String wsName, boolean isMultiDb, String dsName, String vsPath,
   //      ContainerEntry entry) throws Exception
   //   {
   //
   //      String dbDialect = null;
   //      if (dsName != null)
   //      {
   //         DataSource ds = (DataSource)new InitialContext().lookup(dsName);
   //         if (ds != null)
   //         {
   //            Connection jdbcConn = null;
   //
   //            jdbcConn = ds.getConnection();
   //            dbDialect = DialectDetecter.detect(jdbcConn.getMetaData());
   //         }
   //      }
   //
   //      List params = new ArrayList();
   //
   //      if (isMultiDb && dsName == null)
   //      {
   //         dsName = getNewDataSource("");
   //      }
   //
   //      params.add(new SimpleParameterEntry("sourceName", dsName));
   //      params.add(new SimpleParameterEntry("db-type", "generic"));
   //      params.add(new SimpleParameterEntry("multi-db", isMultiDb ? "true" : "false"));
   //      params.add(new SimpleParameterEntry("update-storage", "true"));
   //      params.add(new SimpleParameterEntry("max-buffer-size", "204800"));
   //
   //      if (dbDialect != null)
   //      {
   //         params.add(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_DIALECT, dbDialect));
   //      }
   //      else if (entry.getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT) != null)
   //      {
   //         params.add(new SimpleParameterEntry(JDBCWorkspaceDataContainer.DB_DIALECT, entry
   //            .getParameterValue(JDBCWorkspaceDataContainer.DB_DIALECT)));
   //      }
   //
   //      String oldSwap = entry.getParameterValue("swap-directory");
   //      String newSwap = oldSwap.substring(0, oldSwap.lastIndexOf('/')) + '/' + wsName;
   //
   //      params.add(new SimpleParameterEntry("swap-directory", newSwap));
   //
   //      ContainerEntry containerEntry =
   //         new ContainerEntry("org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer",
   //            (ArrayList)params);
   //      containerEntry.setParameters(params);
   //
   //      if (vsPath != null)
   //      {
   //
   //         ArrayList<ValueStorageFilterEntry> vsparams = new ArrayList<ValueStorageFilterEntry>();
   //         ValueStorageFilterEntry filterEntry = new ValueStorageFilterEntry();
   //         filterEntry.setPropertyType("Binary");
   //         vsparams.add(filterEntry);
   //
   //         ValueStorageEntry valueStorageEntry =
   //            new ValueStorageEntry("org.exoplatform.services.jcr.impl.storage.value.fs.SimpleFileValueStorage", vsparams);
   //         ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
   //         spe.add(new SimpleParameterEntry("path", vsPath));
   //         valueStorageEntry.setId(IdGenerator.generate());
   //         valueStorageEntry.setParameters(spe);
   //         valueStorageEntry.setFilters(vsparams);
   //
   //         // containerEntry.setValueStorages();
   //         containerEntry.setParameters(params);
   //         ArrayList list = new ArrayList(1);
   //         list.add(valueStorageEntry);
   //
   //         containerEntry.setValueStorages(list);
   //
   //      }
   //
   //      // Indexer
   //      ArrayList qParams = new ArrayList();
   //      qParams.add(new SimpleParameterEntry("indexDir", "../temp/index/" + IdGenerator.generate()));
   //      QueryHandlerEntry qEntry =
   //         new QueryHandlerEntry("org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex", qParams);
   //
   //      WorkspaceEntry workspaceEntry =
   //         new WorkspaceEntry(wsName != null ? wsName : IdGenerator.generate(), "nt:unstructured");
   //      workspaceEntry.setContainer(containerEntry);
   //
   //      ArrayList cacheParams = new ArrayList();
   //
   //      cacheParams.add(new SimpleParameterEntry("maxSize", "2000"));
   //      cacheParams.add(new SimpleParameterEntry("liveTime", "20m"));
   //      CacheEntry cacheEntry = new CacheEntry(cacheParams);
   //      cacheEntry.setType("org.exoplatform.services.jcr.impl.dataflow.persistent.LinkedWorkspaceStorageCacheImpl");
   //
   //      workspaceEntry.setCache(cacheEntry);
   //
   //      workspaceEntry.setQueryHandler(qEntry);
   //
   //      LockManagerEntry lockManagerEntry = new LockManagerEntry();
   //      lockManagerEntry.setTimeout(900000);
   //      LockPersisterEntry persisterEntry = new LockPersisterEntry();
   //      persisterEntry.setType("org.exoplatform.services.jcr.impl.core.lock.FileSystemLockPersister");
   //      ArrayList lpParams = new ArrayList();
   //      lpParams.add(new SimpleParameterEntry("path", "../temp/lock"));
   //      persisterEntry.setParameters(lpParams);
   //      lockManagerEntry.setPersister(persisterEntry);
   //      workspaceEntry.setLockManager(lockManagerEntry);
   //
   //      // workspaceEntry
   //      return workspaceEntry;
   //   }

   private void createDatabase(DataSource ds, String dbName) throws SQLException
   {
      Connection connection = ds.getConnection();
      PreparedStatement st = connection.prepareStatement("create database " + dbName);
      st.executeQuery();
   }

   public static TesterConfigurationHelper getInstence()
   {
      if (instence == null)
      {
         instence = new TesterConfigurationHelper();
      }

      return instence;
   }
}
