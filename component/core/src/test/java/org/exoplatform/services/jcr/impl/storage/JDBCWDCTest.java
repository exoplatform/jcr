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

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.jcr.PropertyType;
import javax.naming.InitialContext;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.sql.DataSource;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.StandaloneStoragePluginProvider;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.log.LogConfigurationInitializer;

/**
 * Created by The eXo Platform SAS.
 * 
 * Prerequisites: there should be "jdbcjcr" DataSource configured
 * 
 * @author <a href="mailto:gennady.azarenkov@exoplatform.com">Gennady Azarenkov</a>
 * @version $Id: JDBCWDCTest.java 11907 2008-03-13 15:36:21Z ksm $
 */

public class JDBCWDCTest
   extends TestCase
{

   protected static Log log = ExoLogger.getLogger("jcr.JDBCWorkspaceDataContainer");

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
      params.add(new SimpleParameterEntry("multi-db", "true"));
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
      // ref.add(new StringRefAddr("maxActive", "10"));
      // ref.add(new StringRefAddr("maxWait", "10"));
      // ref.add(new StringRefAddr("database", "jdbc:hsqldb:file:data/test"));

      // SimpleJNDIContextInitializer.initialize(sourceName, ref);

      container =
               new JDBCWorkspaceDataContainer(config, repositoryEntry, null,
                        new StandaloneStoragePluginProvider(config));

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
      DataSource ds = (DataSource) context.lookup(sourceName);
      assertNotNull(sourceName);

      Connection conn = ds.getConnection();
      assertNotNull(conn);
      // conn = ds.getConnection();
      // conn = ds.getConnection();
      // conn = ds.getConnection();
      // conn = ds.getConnection();
      // conn = ds.getConnection();
      // conn = ds.getConnection();

      // // COMMONS-DBCP ///////
      // BasicDataSource bds = (BasicDataSource)ds;
      // System.out.println("getMaxActive: "+bds.getMaxActive());
      // System.out.println("getInitialSize: "+bds.getInitialSize());
      // System.out.println("getNumActive: "+bds.getNumActive());
      // System.out.println("getNumIdle: "+bds.getNumIdle());

      // System.out.println("getMaxWait: "+bds.getMaxWait());

      // //////////////

      // (conn instanceof PooledConnection)
      // System.out.println("CONN: "+conn);
      // System.out.println("Container "+container);

   }

   public void _testAddRoot() throws Exception
   {

      InternalQName nt = Constants.NT_UNSTRUCTURED;
      QPath rootPath = QPath.parse(Constants.ROOT_URI);
      WorkspaceStorageConnection conn = container.openConnection();
      NodeData node =
               new TransientNodeData(rootPath, Constants.ROOT_UUID, 1, nt, new InternalQName[0], 0, null,
                        new AccessControlList());
      TransientPropertyData ntProp =
               new TransientPropertyData(QPath.makeChildPath(rootPath, Constants.JCR_PRIMARYTYPE), "1", 1,
                        PropertyType.NAME, Constants.ROOT_UUID, false);
      ValueData vd = new TransientValueData(Constants.NT_UNSTRUCTURED.getAsString());
      ntProp.setValue(vd);
      conn.add(node);
      conn.add(ntProp);
      conn.commit();
      // assertNotNull(root);
      // assertEquals(Constants.ROOT_URI, root.getQPath().getAsString());
      // assertEquals("nt:unstructured",
      // locationFactory.createJCRName(root.getPrimaryTypeName()).getAsString());
   }

}
