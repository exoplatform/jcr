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
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import java.io.ByteArrayInputStream;
import java.io.File;

import javax.jcr.Node;
import javax.jcr.Session;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestWriteOperations extends JcrAPIBaseTest
{
   private static final int CLEANER_TIMEOUT = 4000; // 4sec

   private ManageableRepository repo;
   
   public void setUp() throws Exception
   {
      super.setUp();

      repo = repositoryService.getRepository("db2");
      Session s = (SessionImpl)repo.login(credentials, "ws-no-cache-n-vs");
      s.getRootNode().addNode("TestWriteOperations");
      s.save();
   }

   public void tearDown() throws Exception
   {
      Session s = (SessionImpl)repo.login(credentials, "ws-no-cache-n-vs");
      if (s.getRootNode().hasNode("TestWriteOperations"))
      {
         s.getRootNode().getNode("TestWriteOperations").remove();
         s.save();
      }

      super.tearDown();
   }

   public void testCleanupSwapDirectory() throws Exception
   {
      Session s = (SessionImpl)repo.login(credentials, "ws-no-cache-n-vs");
      Node testNode = s.getRootNode().getNode("TestWriteOperations");
      PropertyImpl property = (PropertyImpl)testNode.setProperty("name", new ByteArrayInputStream("test".getBytes()));
      testNode.save();
      WorkspaceContainerFacade wsc = repo.getWorkspaceContainer(s.getWorkspace().getName());
      JDBCWorkspaceDataContainer dataContainer =
         (JDBCWorkspaceDataContainer)wsc.getComponent(JDBCWorkspaceDataContainer.class);
      JDBCStorageConnection con = (JDBCStorageConnection)dataContainer.openConnection(true);
      File file = new File(dataContainer.swapDirectory, con.getInternalId(property.getInternalIdentifier()) + "0.0");
      con.close();

      assertTrue(file.exists());

      s.logout();
      s = null;
      testNode = null;
      property = null;

      // allows GC to call finalize on StreamPersistedValueData
      System.gc();
      Thread.sleep(CLEANER_TIMEOUT + 500);
      Thread.yield();
      System.gc();

      assertFalse(file.exists());
   }
}
