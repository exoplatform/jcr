/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.core;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

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
   private final TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void testRestore() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);

         WorkspaceEntry workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
         helper.addWorkspace(repository, workspaceEntry);

         InputStream is = TestWorkspaceManagement.class.getResourceAsStream("/import-export/db1_ws1-20071220_0430.xml");
         repository.importWorkspace(workspaceEntry.getName(), is);
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testRestoreBadXml() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, DatabaseStructureType.MULTI, null);

         WorkspaceEntry workspaceEntry = helper.createWorkspaceEntry(DatabaseStructureType.MULTI, null);
         helper.addWorkspace(repository, workspaceEntry);

         Session defSession = repository.login(this.credentials, workspaceEntry.getName());
         Node defRoot = defSession.getRootNode();

         Node node1 = defRoot.addNode("node1");
         node1.setProperty("p1", 2);
         defSession.save();

         File content = File.createTempFile("data", ".xml");
         content.deleteOnExit();
         OutputStream os = new BufferedOutputStream(new FileOutputStream(content));
         defSession.exportSystemView(node1.getPath(), os, false, false);
         os.close();
         defSession.logout();

         try
         {
            InputStream is =
               TestWorkspaceManagement.class.getResourceAsStream("/import-export/db1_ws1-20071220_0430.xml");
            repository.importWorkspace(workspaceEntry.getName(), new BufferedInputStream(new FileInputStream(content)));

            fail();
         }
         catch (RepositoryException e)
         {
            // ok
         }
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }
}
