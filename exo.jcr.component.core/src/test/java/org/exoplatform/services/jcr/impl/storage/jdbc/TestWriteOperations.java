/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.storage.jdbc;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
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
      boolean isMultiDb = dataContainer.multiDb;
      repo = helper.createRepository(container, isMultiDb, null);
      wsEntry = helper.createWorkspaceEntry(isMultiDb, null, null, false, true);
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
      File file = new File(dataContainer.swapDirectory, con.getInternalId(property.getInternalIdentifier()) + "0.0");
      con.close();

      assertTrue(file.exists());

      s.logout();
      s = null;
      testNode = null;
      property = null;

      assertReleasedFile(file);
   }
   
   private void assertReleasedFile(File file) throws Exception
   {
      long purgeStartTime = System.currentTimeMillis();
      while (file.exists() && (System.currentTimeMillis() - purgeStartTime < 2 * 60 * 1000))
      {
         System.gc();
         try
         {
            Thread.sleep(500);
         }
         catch (InterruptedException e)
         {
         }
      }

      assertFalse(file.exists()); // file released and deleted
   }
}
