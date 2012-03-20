/*
 * CopSyright (C) 2009 eXo Platform SAS.
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
import org.exoplatform.services.jcr.config.ContainerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.ValueStorageEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.util.ArrayList;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestWorkspaceManagement.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestWorkspaceManagement extends JcrImplBaseTest
{
   private boolean isDefaultWsMultiDb = false;

   private final TesterConfigurationHelper helper;

   private WorkspaceEntry wsEntry;

   public TestWorkspaceManagement()
   {
      super();
      this.helper = TesterConfigurationHelper.getInstance();
   }

   // single db test only
   public void testAddWorkspaceWithNewDS() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, "not-existed-ds");
            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (Exception e)
         {
            // ok;
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

   public void testAddWorkspaceWithExistingName() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
            wsEntry.setName(repository.getConfiguration().getSystemWorkspaceName());

            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (RepositoryConfigurationException e)
         {
            // ok;
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

   public void testAddWorkspaceWithIvalidVs() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);

            ValueStorageEntry valueStorageEntry = wsEntry.getContainer().getValueStorages().get(0);

            ArrayList<SimpleParameterEntry> spe = new ArrayList<SimpleParameterEntry>();
            spe.add(new SimpleParameterEntry("path", "/unknown/path"));
            valueStorageEntry.setParameters(spe);

            wsEntry.getContainer().getValueStorages().set(0, valueStorageEntry);

            helper.addWorkspace(repository, wsEntry);
         }
         catch (RepositoryConfigurationException e)
         {
            // ok;
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

   public void testCreateWsNoConfig() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
            wsEntry.setContainer(new ContainerEntry(
               "org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer", new ArrayList()));

            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (Exception e)
         {
            // ok;
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

   public void testInitNewWS() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);

         SessionImpl session = null;
         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
            helper.addWorkspace(repository, wsEntry);

            session = (SessionImpl)repository.login(credentials, wsEntry.getName());
            assertNotNull(session.getRootNode());
         }
         catch (RepositoryException e)
         {
            e.printStackTrace();
            fail();
         }
         finally
         {
            session.logout();
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

   public void testMixMultiAndSingleDbWs() throws RepositoryConfigurationException, Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);

         try
         {
            WorkspaceEntry wsEntry = helper.createWorkspaceEntry(true, dsName);
            helper.addWorkspace(repository, wsEntry);
            fail();
         }
         catch (Exception e)
         {
            // ok;
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

   public void testRemoveSystemWorkspace() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);

         try
         {
            repository.removeWorkspace(repository.getConfiguration().getSystemWorkspaceName());
            fail();
         }
         catch (RepositoryException e)
         {
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

   public void testRemoveWorkspace() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         String dsName = helper.createDatasource();
         repository = helper.createRepository(container, false, dsName);
         WorkspaceEntry wsEntry = helper.createWorkspaceEntry(false, dsName);
   
         helper.addWorkspace(repository, wsEntry);
         assertEquals(2, repository.getWorkspaceNames().length);
   
         repository.removeWorkspace(wsEntry.getName());
         assertEquals(1, repository.getWorkspaceNames().length);
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
