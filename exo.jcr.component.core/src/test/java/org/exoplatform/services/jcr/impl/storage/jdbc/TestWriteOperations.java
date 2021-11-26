/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.TestCleanableFileStreamValueData;
import org.exoplatform.services.jcr.impl.storage.jdbc.statistics.StatisticsJDBCStorageConnection;
import org.exoplatform.services.jcr.storage.WorkspaceStorageConnection;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import javax.jcr.Node;
import javax.jcr.Session;
import java.io.ByteArrayInputStream;
import java.io.File;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestWriteOperations extends JcrAPIBaseTest
{
   private TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   private ManageableRepository repo;

   private WorkspaceEntry wsEntry;

   public void setUp() throws Exception
   {
      super.setUp();
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer(repository.getSystemWorkspaceName());
      JDBCWorkspaceDataContainer dataContainer =
         (JDBCWorkspaceDataContainer)wsc.getComponent(JDBCWorkspaceDataContainer.class);
      JDBCDataContainerConfig.DatabaseStructureType dbStructureType = dataContainer.containerConfig.dbStructureType;
      repo = helper.createRepository(container, dbStructureType, null);
      wsEntry = helper.createWorkspaceEntry(dbStructureType, null, null, false, true);
      helper.addWorkspace(repo, wsEntry);

      Session s = (SessionImpl)repo.login(credentials, wsEntry.getName());
      s.getRootNode().addNode("TestWriteOperations");
      s.save();
   }

   public void tearDown() throws Exception
   {
      Session s = (SessionImpl)repo.login(credentials, wsEntry.getName());
      if (s.getRootNode().hasNode("TestWriteOperations"))
      {
         s.getRootNode().getNode("TestWriteOperations").remove();
         s.save();
      }
      helper.removeRepository(container, repo.getConfiguration().getName());
      super.tearDown();
   }

   public void testCleanupSwapDirectory() throws Exception
   {
      Session s = (SessionImpl)repo.login(credentials, wsEntry.getName());
      Node testNode = s.getRootNode().getNode("TestWriteOperations");
      PropertyImpl property = (PropertyImpl)testNode.setProperty("name", new ByteArrayInputStream("test".getBytes()));
      testNode.save();
      WorkspaceContainerFacade wsc = repo.getWorkspaceContainer(s.getWorkspace().getName());
      JDBCWorkspaceDataContainer dataContainer =
         (JDBCWorkspaceDataContainer)wsc.getComponent(JDBCWorkspaceDataContainer.class);
      WorkspaceStorageConnection wscon = dataContainer.openConnection();
      if (wscon instanceof StatisticsJDBCStorageConnection)
      {
         wscon = ((StatisticsJDBCStorageConnection)wscon).getNestedWorkspaceStorageConnection();
      }
      JDBCStorageConnection con = (JDBCStorageConnection) wscon;
      File file =
         new File(dataContainer.containerConfig.spoolConfig.tempDirectory, con.getInternalId(property
            .getInternalIdentifier()) + "0.0");
      con.close();

      assertTrue(file.exists());

      s.logout();
      s = null;
      testNode = null;
      property = null;

      TestCleanableFileStreamValueData.assertReleasedFile(file);
   }
}