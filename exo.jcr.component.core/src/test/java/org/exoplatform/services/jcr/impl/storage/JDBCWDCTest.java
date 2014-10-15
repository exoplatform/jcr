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
package org.exoplatform.services.jcr.impl.storage;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.StandaloneStoragePluginProvider;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.LogConfigurationInitializer;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

/**
 * Created by The eXo Platform SAS.
 * 
 * Prerequisites: there should be "jdbcjcr" DataSource configured
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JDBCWDCTest.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class JDBCWDCTest extends TestCase
{

   protected static Log log = ExoLogger.getLogger("exo.jcr.component.core.JDBCWorkspaceDataContainer");

   protected WorkspaceEntry config;

   protected String sourceName = "jdbcjcr";

   JDBCWorkspaceDataContainer container;

   @Override
   protected void setUp() throws Exception
   {

      RepositoryEntry repositoryEntry = new RepositoryEntry();
      config = new WorkspaceEntry();
      config.setName("test");
      ContainerEntry containerEntry = new ContainerEntry();
      List params = new ArrayList();
      params.add(new SimpleParameterEntry("sourceName", sourceName));
      params.add(new SimpleParameterEntry("db-structure-type", "multi"));
      containerEntry.setParameters(params);
      config.setContainer(containerEntry);

      // Construct BasicDataSource reference
      Reference ref = new Reference("javax.sql.DataSource", "org.apache.commons.dbcp.BasicDataSourceFactory", null);

      // Reference ref = new Reference("org.hsqldb.jdbc.jdbcDataSource",
      // "org.hsqldb.jdbc.jdbcDataSourceFactory", null);

      ref.add(new StringRefAddr("driverClassName", "org.hsqldb.jdbcDriver"));

      ref.add(new StringRefAddr("url", "jdbc:hsqldb:file:target/data/test"));
      // ref.add(new StringRefAddr("url", "jdbc:hsqldb:mem:aname"));

      ref.add(new StringRefAddr("username", "sa"));
      ref.add(new StringRefAddr("password", ""));

      FileCleanerHolder cleanerHolder = new FileCleanerHolder();

      container =
         new JDBCWorkspaceDataContainer(config, repositoryEntry, null, new StandaloneStoragePluginProvider(
            repositoryEntry, config, cleanerHolder), null, cleanerHolder);

      Properties logProps = new Properties();
      logProps.put("org.apache.commons.logging.simplelog.defaultlog", "debug");

      new LogConfigurationInitializer("org.exoplatform.services.log.impl.BufferedSimpleLog",
         "org.exoplatform.services.log.impl.SimpleLogConfigurator", logProps);

   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();
   }

   public void testContainerStartUp() throws Exception
   {
      // log.info("Container "+container);
      InitialContext context = new InitialContext();
      DataSource ds = (DataSource)context.lookup(sourceName);
      assertNotNull(sourceName);

      Connection conn = ds.getConnection();
      assertNotNull(conn);
   }
}
