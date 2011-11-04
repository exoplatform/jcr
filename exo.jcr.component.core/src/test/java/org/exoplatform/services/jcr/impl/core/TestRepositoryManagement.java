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

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.config.JDBCConfigurationPersister;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: TestRepositoryManagement.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class TestRepositoryManagement extends JcrImplBaseTest
{

   public static int BINDED_DS_COUNT = 100;

   private static boolean isBinded = false;

   private final int lastDS = 0;

   private WorkspaceEntry wsEntry;

   private boolean isDefaultWsMultiDb;

   private final TesterConfigurationHelper helper;

   public TestRepositoryManagement()
   {
      super();
      this.helper = TesterConfigurationHelper.getInstance();
   }

   public void testAddNewRepository() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);
      assertNotNull(repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName()).getRootNode());
   }

   public void testMarshalUnmarshalRepositoryConfiguration() throws Exception
   {
      ManageableRepository newRepository = helper.createRepository(container, false, null);
      final long lockManagerTimeOut =
         newRepository.getConfiguration().getWorkspaceEntries().get(0).getLockManager().getTimeout();

      // 1st marshal configuration
      File tempFile = PrivilegedFileHelper.createTempFile("test-config", "xml");
      PrivilegedFileHelper.deleteOnExit(tempFile);

      IBindingFactory factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
      IMarshallingContext mctx = factory.createMarshallingContext();

      FileOutputStream saveStream = new FileOutputStream(tempFile);
      ArrayList<RepositoryEntry> repositoryEntries = new ArrayList<RepositoryEntry>();
      repositoryEntries.add(newRepository.getConfiguration());

      RepositoryServiceConfiguration newRepositoryServiceConfiguration =
         new RepositoryServiceConfiguration(repositoryService.getConfig().getDefaultRepositoryName(), repositoryEntries);
      mctx.marshalDocument(newRepositoryServiceConfiguration, "ISO-8859-1", null, saveStream);
      saveStream.close();

      // 1st unmarshal
      factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
      IUnmarshallingContext uctx = factory.createUnmarshallingContext();
      RepositoryServiceConfiguration conf =
         (RepositoryServiceConfiguration)uctx.unmarshalDocument(PrivilegedFileHelper.fileInputStream(tempFile), null);

      // 1st check
      RepositoryEntry unmarshledRepositoryEntry =
         conf.getRepositoryConfiguration(newRepository.getConfiguration().getName());
      assertEquals(lockManagerTimeOut, unmarshledRepositoryEntry.getWorkspaceEntries().get(0).getLockManager()
         .getTimeout());

      
      // 2nd marshal configuration
      tempFile = PrivilegedFileHelper.createTempFile("test-config", "xml");
      PrivilegedFileHelper.deleteOnExit(tempFile);

      factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
      mctx = factory.createMarshallingContext();
      saveStream = new FileOutputStream(tempFile);
      repositoryEntries = new ArrayList<RepositoryEntry>();
      repositoryEntries.add(newRepository.getConfiguration());

      newRepositoryServiceConfiguration =
         new RepositoryServiceConfiguration(repositoryService.getConfig().getDefaultRepositoryName(), repositoryEntries);
      mctx.marshalDocument(newRepositoryServiceConfiguration, "ISO-8859-1", null, saveStream);
      saveStream.close();
      
      // 2nd unmarshal
      factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
      uctx = factory.createUnmarshallingContext();
      conf = (RepositoryServiceConfiguration)uctx.unmarshalDocument(PrivilegedFileHelper.fileInputStream(tempFile), null);

      // 2nd check
      unmarshledRepositoryEntry =
         conf.getRepositoryConfiguration(newRepository.getConfiguration().getName());
      assertEquals(lockManagerTimeOut, unmarshledRepositoryEntry.getWorkspaceEntries().get(0).getLockManager()
         .getTimeout());
   }

   public void testAddNewRepositoryWithSameName() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      try
      {
         RepositoryEntry rEntry = helper.createRepositoryEntry(false, null, null);
         rEntry.setName(repository.getConfiguration().getName());

         helper.createRepository(container, rEntry);
         fail();
      }
      catch (Exception e)
      {
         // ok
      }
   }

   public void testCanRemove() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      assertFalse(service.canRemoveRepository(repository.getConfiguration().getName()));
      session.logout();
      assertTrue(service.canRemoveRepository(repository.getConfiguration().getName()));
   }

   public void testInitNameSpaces() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      assertEquals("http://www.apache.org/jackrabbit/test", session.getNamespaceURI("test"));
      assertEquals("http://www.exoplatform.org/jcr/test/1.0", session.getNamespaceURI("exojcrtest"));
   }

   public void testInitNodeTypes() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      // check if nt:folder nodetype exists
      session.getRootNode().addNode("folder", "nt:folder");
      session.save();
   }

   public void testRemove() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, null);

      RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
      service.removeRepository(repository.getConfiguration().getName());

      try
      {
         service.getRepository(repository.getConfiguration().getName());
      }
      catch (Exception e)
      {

      }
   }

   public void testAddNewRepositorMultiThreading() throws Exception
   {
      int theadsCount = 10;

      RepositoryCreationThread[] threads = new RepositoryCreationThread[theadsCount];
      CountDownLatch latcher = new CountDownLatch(1);

      for (int i = 0; i < theadsCount; i++)
      {
         threads[i] = new RepositoryCreationThread(latcher);
         threads[i].start();
      }

      latcher.countDown();

      for (int i = 0; i < theadsCount; i++)
      {
         threads[i].join();
      }

      PropertiesParam props = new PropertiesParam();
      props.setProperty("dialect", "auto");
      props.setProperty("source-name", "jdbcjcr");

      JDBCConfigurationPersister persiter = new JDBCConfigurationPersister();
      persiter.init(props);

      IBindingFactory factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
      IUnmarshallingContext uctx = factory.createUnmarshallingContext();
      RepositoryServiceConfiguration storedConf =
         (RepositoryServiceConfiguration)uctx.unmarshalDocument(persiter.read(), null);

      for (int i = 0; i < theadsCount; i++)
      {
         // test if respository has been created
         ManageableRepository repository = threads[i].getRepository();
         assertNotNull(repository);

         // check configuration in persiter
         storedConf.getRepositoryConfiguration(repository.getConfiguration().getName());
         
         // check configuration in RepositoryServic
         assertNotNull(repositoryService.getConfig().getRepositoryConfiguration(repository.getConfiguration().getName()));
         
         // login into newly created repository
         ManageableRepository newRepository = repositoryService.getRepository(repository.getConfiguration().getName());
         assertNotNull(repository.login(credentials, newRepository.getConfiguration().getSystemWorkspaceName())
            .getRootNode());
      }
   }

   private class RepositoryCreationThread extends Thread
   {
      private CountDownLatch latcher;

      private ManageableRepository repository;

      RepositoryCreationThread(CountDownLatch latcher)
      {
         this.latcher = latcher;
      }

      /**
       * {@inheritDoc}
       */
      public void run()
      {
         try
         {
            latcher.await();
            repository = helper.createRepository(container, false, null);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }

      public ManageableRepository getRepository()
      {
         return repository;
      }
   }
}
