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
package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.util.ConfigurationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestWorkspaceRestore.java 11962 2008-03-16 16:31:14Z gazarenkov $
 */
public class TestWorkspaceRestore extends JcrImplBaseTest
{

   private static boolean isDefaultWsCreated = false;

   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestWorkspaceRestore");

   private final ConfigurationHelper helper = ConfigurationHelper.getInstence();

   private WorkspaceEntry wsEntry;

   private boolean isDefaultWsMultiDb;

   public void _testRestore() throws Exception
   {
      Session defSession = repository.login(this.credentials, "defWs");
      Node defRoot = defSession.getRootNode();

      Node node1 = defRoot.addNode("node1");
      node1.setProperty("p1", 2);
      defSession.save();

      File content = File.createTempFile("data", ".xml");
      content.deleteOnExit();
      OutputStream os = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(content));
      defSession.exportSystemView(defRoot.getPath(), os, false, false);
      os.close();
      defSession.logout();
      WorkspaceEntry workspaceEntry = null;
      workspaceEntry =
         helper.getNewWs("testRestore", isDefaultWsMultiDb, wsEntry.getContainer().getParameterValue(
            JDBCWorkspaceDataContainer.SOURCE_NAME), null, wsEntry.getContainer());
      assertNotNull(workspaceEntry);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep;

      defRep = (RepositoryImpl)service.getDefaultRepository();
      defRep.configWorkspace(workspaceEntry);

      defRep.importWorkspace(workspaceEntry.getName(), new BufferedInputStream(PrivilegedFileHelper.fileInputStream(content)));

      doTestOnWorkspace(workspaceEntry.getName());
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      wsEntry = (WorkspaceEntry)session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class);
      if ("true".equals(wsEntry.getContainer().getParameterValue("multi-db")))
      {
         isDefaultWsMultiDb = true;
      }
      if (!isDefaultWsCreated)
      {

         WorkspaceEntry workspaceEntry = null;
         workspaceEntry =
            helper.getNewWs("defWs", isDefaultWsMultiDb, wsEntry.getContainer().getParameterValue(
               JDBCWorkspaceDataContainer.SOURCE_NAME), null, wsEntry.getContainer());
         helper.createWorkspace(workspaceEntry, container);
         isDefaultWsCreated = true;
      }
   }

   public void testRestore() throws RepositoryConfigurationException, Exception
   {
      WorkspaceEntry workspaceEntry =
         helper.getNewWs("testResotore", isDefaultWsMultiDb, wsEntry.getContainer().getParameterValue(
            JDBCWorkspaceDataContainer.SOURCE_NAME), null, wsEntry.getContainer());

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep;

      defRep = (RepositoryImpl)service.getDefaultRepository();
      defRep.configWorkspace(workspaceEntry);
      InputStream is = TestWorkspaceManagement.class.getResourceAsStream("/import-export/db1_ws1-20071220_0430.xml");
      repository.importWorkspace("testResotore", is);
   }

   public void testRestoreBadXml() throws Exception
   {
      Session defSession = repository.login(this.credentials /* session.getCredentials() */, "defWs");
      Node defRoot = defSession.getRootNode();

      Node node1 = defRoot.addNode("node1");
      node1.setProperty("p1", 2);
      defSession.save();

      File content = File.createTempFile("data", ".xml");
      content.deleteOnExit();
      OutputStream os = new BufferedOutputStream(PrivilegedFileHelper.fileOutputStream(content));
      defSession.exportSystemView(node1.getPath(), os, false, false);
      os.close();
      defSession.logout();
      WorkspaceEntry workspaceEntry = null;
      workspaceEntry =
         helper.getNewWs("testRestoreBadXml", isDefaultWsMultiDb, wsEntry.getContainer().getParameterValue(
            JDBCWorkspaceDataContainer.SOURCE_NAME), null, wsEntry.getContainer());
      assertNotNull(workspaceEntry);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      RepositoryImpl defRep;

      defRep = (RepositoryImpl)service.getDefaultRepository();
      defRep.configWorkspace(workspaceEntry);

      try
      {
         defRep.importWorkspace(workspaceEntry.getName(), new BufferedInputStream(PrivilegedFileHelper.fileInputStream(content)));
         fail();
      }
      catch (RepositoryException e)
      {
         // ok
      }

   }

   private void doTestOnWorkspace(String wsName) throws RepositoryException, RepositoryConfigurationException
   {
      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      Session sess = service.getDefaultRepository().getSystemSession(wsName);

      Node root2 = sess.getRootNode();
      assertNotNull(root2);

      Node node1 = root2.getNode("node1");
      assertNotNull(node1);

      assertEquals("2", node1.getProperty("p1").getString());

      sess.logout();
   }
}
