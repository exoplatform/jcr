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
import org.exoplatform.services.jcr.config.CacheEntry;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.RepositoryServiceConfiguration;
import org.exoplatform.services.jcr.config.SimpleParameterEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.impl.RepositoryContainer;
import org.exoplatform.services.jcr.impl.config.JDBCConfigurationPersister;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;
import org.jibx.runtime.BindingDirectory;
import org.jibx.runtime.IBindingFactory;
import org.jibx.runtime.IMarshallingContext;
import org.jibx.runtime.IUnmarshallingContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.CountDownLatch;

import javax.jcr.PathNotFoundException;
import javax.jcr.Session;

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
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         Session session = null;
         try
         {
            session = repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
            assertNotNull(session.getRootNode());
         }
         finally
         {
            if (session != null)
            {
               session.logout();
            }
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

   public void testMarshalUnmarshalRepositoryConfiguration() throws Exception
   {

      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         final long lockManagerTimeOut =
            repository.getConfiguration().getWorkspaceEntries().get(0).getLockManager().getTimeout();

         // 1st marshal configuration
         File tempFile = PrivilegedFileHelper.createTempFile("test-config", "xml");
         PrivilegedFileHelper.deleteOnExit(tempFile);

         IBindingFactory factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         IMarshallingContext mctx = factory.createMarshallingContext();

         FileOutputStream saveStream = new FileOutputStream(tempFile);
         ArrayList<RepositoryEntry> repositoryEntries = new ArrayList<RepositoryEntry>();
         repositoryEntries.add(repository.getConfiguration());

         RepositoryServiceConfiguration newRepositoryServiceConfiguration =
            new RepositoryServiceConfiguration(repositoryService.getConfig().getDefaultRepositoryName(),
               repositoryEntries);
         mctx.marshalDocument(newRepositoryServiceConfiguration, "ISO-8859-1", null, saveStream);
         saveStream.close();

         // 1st unmarshal
         factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         IUnmarshallingContext uctx = factory.createUnmarshallingContext();
         RepositoryServiceConfiguration conf =
            (RepositoryServiceConfiguration)uctx
               .unmarshalDocument(PrivilegedFileHelper.fileInputStream(tempFile), null);

         // 1st check
         RepositoryEntry unmarshledRepositoryEntry =
            conf.getRepositoryConfiguration(repository.getConfiguration().getName());
         assertEquals(lockManagerTimeOut, unmarshledRepositoryEntry.getWorkspaceEntries().get(0).getLockManager()
            .getTimeout());

         // 2nd marshal configuration
         tempFile = PrivilegedFileHelper.createTempFile("test-config", "xml");
         PrivilegedFileHelper.deleteOnExit(tempFile);

         factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         mctx = factory.createMarshallingContext();
         saveStream = new FileOutputStream(tempFile);
         repositoryEntries = new ArrayList<RepositoryEntry>();
         repositoryEntries.add(repository.getConfiguration());

         newRepositoryServiceConfiguration =
            new RepositoryServiceConfiguration(repositoryService.getConfig().getDefaultRepositoryName(),
               repositoryEntries);
         mctx.marshalDocument(newRepositoryServiceConfiguration, "ISO-8859-1", null, saveStream);
         saveStream.close();

         // 2nd unmarshal
         factory = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         uctx = factory.createUnmarshallingContext();
         conf =
            (RepositoryServiceConfiguration)uctx
               .unmarshalDocument(PrivilegedFileHelper.fileInputStream(tempFile), null);

         // 2nd check
         unmarshledRepositoryEntry = conf.getRepositoryConfiguration(repository.getConfiguration().getName());
         assertEquals(lockManagerTimeOut, unmarshledRepositoryEntry.getWorkspaceEntries().get(0).getLockManager()
            .getTimeout());
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testBackupFilesRepositoryConfiguration() throws Exception
   {
      RepositoryServiceConfiguration repositoryServiceConfiguration = repositoryService.getConfig();
      final String path = "conf/standalone";
      final ClassLoader cl = Thread.currentThread().getContextClassLoader();
      File configPath = new File(cl.getResource(path).toURI());

      for (int i = 1; i <= 10; i++)
      {
         repositoryServiceConfiguration.retain();
      }

      String[] files = configPath.list(new FilenameFilter()
      {
         public boolean accept(File dir, String name)
         {
            return name.startsWith("test-jcr-config-jbc.xml.");
         }
      });

      assertEquals(5, files.length);
   }

   public void testAddNewRepositoryWithSameName() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         try
         {
            RepositoryEntry rEntry = helper.createRepositoryEntry(false, null, null, true);
            rEntry.setName(repository.getConfiguration().getName());

            helper.createRepository(container, rEntry);
            fail();
         }
         catch (Exception e)
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

   public void testCanRemove() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);

         SessionImpl session =
            (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

         assertFalse(service.canRemoveRepository(repository.getConfiguration().getName()));
         session.logout();
         assertTrue(service.canRemoveRepository(repository.getConfiguration().getName()));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testInitNameSpaces() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         SessionImpl session = null;
         try
         {
            session =
               (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

            assertEquals("http://www.apache.org/jackrabbit/test", session.getNamespaceURI("test"));
            assertEquals("http://www.exoplatform.org/jcr/test/1.0", session.getNamespaceURI("exojcrtest"));
         }
         finally
         {
            if (session != null)
            {
               session.logout();
            }
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

   public void testInitNodeTypes() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, false, null);

         SessionImpl session = null;
         try
         {
            session =
               (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

            // check if nt:folder nodetype exists
            session.getRootNode().addNode("folder", "nt:folder");
            session.save();
         }
         finally
         {
            if (session != null)
            {
               session.logout();
            }
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

      try
      {
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

         IBindingFactory bfact = BindingDirectory.getFactory(RepositoryServiceConfiguration.class);
         IMarshallingContext mctx = bfact.createMarshallingContext();
         OutputStream saveStream = new ByteArrayOutputStream();
         mctx.marshalDocument(repositoryService.getConfig(), "ISO-8859-1", null, saveStream);
         saveStream.close();

         persiter.write(new ByteArrayInputStream(((ByteArrayOutputStream)saveStream).toByteArray()));

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
            assertNotNull(repositoryService.getConfig().getRepositoryConfiguration(
               repository.getConfiguration().getName()));

            // login into newly created repository
            ManageableRepository newRepository =
               repositoryService.getRepository(repository.getConfiguration().getName());

            Session session = null;
            try
            {
               session = repository.login(credentials, newRepository.getConfiguration().getSystemWorkspaceName());
               assertNotNull(session.getRootNode());
            }
            finally
            {
               if (session != null)
               {
                  session.logout();
               }
            }
         }
      }
      finally
      {
         for (int i = 0; i < theadsCount; i++)
         {
            helper.removeRepository(container, threads[i].getRepository().getConfiguration().getName());
         }
      }
   }

   private class RepositoryCreationThread extends Thread
   {
      private CountDownLatch latcher;

      private ManageableRepository tRrepository;

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
            tRrepository = helper.createRepository(container, false, null);
         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }

      public ManageableRepository getRepository()
      {
         return tRrepository;
      }
   }

   public void testCreateAterRemoveCheckOldContent() throws Exception
   {
      ManageableRepository newRepository = null;
      try
      {
         RepositoryService service = (RepositoryService)container.getComponentInstanceOfType(RepositoryService.class);
         RepositoryEntry repoEntry = helper.createRepositoryEntry(false, null, null, true);

         try
         {
            Class
               .forName("org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.ISPNCacheWorkspaceStorageCache");

            ArrayList cacheParams = new ArrayList();
            cacheParams.add(new SimpleParameterEntry("infinispan-configuration",
               "conf/standalone/cluster/test-infinispan-config.xml"));
            cacheParams.add(new SimpleParameterEntry("jgroups-configuration", "conf/udp-mux-v3.xml"));
            cacheParams.add(new SimpleParameterEntry("infinispan-cluster-name", "JCR-cluster-Test"));
            cacheParams.add(new SimpleParameterEntry("use-distributed-cache", "false"));
            CacheEntry cacheEntry = new CacheEntry(cacheParams);
            cacheEntry
               .setType("org.exoplatform.services.jcr.impl.dataflow.persistent.infinispan.ISPNCacheWorkspaceStorageCache");
            cacheEntry.setEnabled(true);

            ArrayList<WorkspaceEntry> wsList = repoEntry.getWorkspaceEntries();

            wsList.get(0).setCache(cacheEntry);
            repoEntry.setWorkspaceEntries(wsList);
         }
         catch (ClassNotFoundException e)
         {
         }

         service.createRepository(repoEntry);
         service.getConfig().retain();

         ManageableRepository repository = service.getRepository(repoEntry.getName());

         // add content
         Session session =
            repository.login(new CredentialsImpl("admin", "admin".toCharArray()), repository.getConfiguration()
               .getSystemWorkspaceName());
         session.getRootNode().addNode("test");
         session.save();
         session.logout();

         // copy repository configuration
         RepositoryEntry repositoryEntry = helper.copyRepositoryEntry(repository.getConfiguration());

         String newDatasourceName = helper.createDatasource();

         for (WorkspaceEntry ws : repositoryEntry.getWorkspaceEntries())
         {
            List<SimpleParameterEntry> parameters = ws.getContainer().getParameters();
            for (int i = 0; i <= parameters.size(); i++)
            {
               SimpleParameterEntry spe = parameters.get(i);
               if (spe.getName().equals("source-name"))
               {
                  parameters.add(i, new SimpleParameterEntry(spe.getName(), newDatasourceName));
                  break;
               }
            }
         }

         service.removeRepository(repository.getConfiguration().getName());

         try
         {
            service.getRepository(repository.getConfiguration().getName());
            fail();
         }
         catch (Exception e)
         {
         }

         // create new repository 
         service.createRepository(repositoryEntry);
         service.getConfig().retain();

         newRepository = service.getRepository(repositoryEntry.getName());

         Session newSession = null;
         try
         {
            newSession =
               newRepository.login(new CredentialsImpl("admin", "admin".toCharArray()), newRepository
                  .getConfiguration().getSystemWorkspaceName());

            try
            {
               newSession.getRootNode().getNode("test");
               fail("Node 'test' should not exists after remove repository and recreate new.");
            }
            catch (PathNotFoundException e)
            {
               //ok
            }
         }
         finally
         {
            if (newSession != null)
            {
               newSession.logout();
            }
         }
      }
      finally
      {
         if (newRepository != null)
         {
            helper.removeRepository(container, newRepository.getConfiguration().getName());
         }
      }
   }
   
   public void testRepositoryContainerGCedAfterStop() throws Exception
   {
      int numberOfRepositories = 3;
      int GCTimeoutUntilTenuredCleaned = 2 * 60 * 1000; // test timeout 
      WeakHashMap<RepositoryContainer, Object> repositoryContainersInMemory =
         new WeakHashMap<RepositoryContainer, Object>();

      for (int i = 0; i < numberOfRepositories; i++)
      {
         ManageableRepository repository = null;
         try
         {
            repository = createRepositoryWithJBCorISPNQueryHandler();
            RepositoryContainer repositoryContainer =
               helper.getRepositoryContainer(container, repository.getConfiguration().getName());
            repositoryContainersInMemory.put(repositoryContainer, null);
            SessionImpl session =
               (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
            session.logout();
         }
         finally
         {
            if (repository != null)
            {
               helper.removeRepository(container, repository.getConfiguration().getName());
            }
         }
      }
      
      long purgeStartTime = System.currentTimeMillis();
      while (repositoryContainersInMemory.size() > 0
         && (System.currentTimeMillis() - purgeStartTime < GCTimeoutUntilTenuredCleaned))
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
      if (repositoryContainersInMemory.size() > 0)
      {
         fail("Memmory leak spotted. Please check. No RepositoryContainer instances should be in the memory. But "
            + repositoryContainersInMemory.size() + " found.");
      }
   }

   private ManageableRepository createRepositoryWithJBCorISPNQueryHandler() throws Exception
   {
      RepositoryEntry repoEntry = helper.createRepositoryEntry(false, null, null, true);
      // modify configuration
      WorkspaceEntry workspaceEntry = repoEntry.getWorkspaceEntries().get(0);
      QueryHandlerEntry queryHandler = workspaceEntry.getQueryHandler();
      List<SimpleParameterEntry> parameters = queryHandler.getParameters();

      if (!helper.ispnCacheEnabled())
      {
         // Use JBossCache components for core project
         parameters.add(new SimpleParameterEntry("changesfilter-class",
            "org.exoplatform.services.jcr.impl.core.query.jbosscache.JBossCacheIndexChangesFilter"));
         parameters.add(new SimpleParameterEntry("jbosscache-configuration",
            "conf/standalone/cluster/test-jbosscache-indexer.xml"));
         parameters.add(new SimpleParameterEntry("jgroups-configuration", "jar:/conf/standalone/cluster/udp-mux.xml"));
         parameters.add(new SimpleParameterEntry("jgroups-multiplexer-stack", "false"));
         parameters.add(new SimpleParameterEntry("jbosscache-shareable", "true"));
         parameters.add(new SimpleParameterEntry("jbosscache-cluster-name", "JCR-cluster-indexer"));
      }
      else
      {
         // Use Infinispan components for core.ispn project
         parameters.add(new SimpleParameterEntry("changesfilter-class",
            "org.exoplatform.services.jcr.impl.core.query.ispn.ISPNIndexChangesFilter"));
         parameters.add(new SimpleParameterEntry("infinispan-configuration",
            "conf/standalone/cluster/test-infinispan-indexer.xml"));
         parameters.add(new SimpleParameterEntry("jgroups-configuration", "jar:/conf/standalone/cluster/udp-mux-v3.xml"));
         parameters.add(new SimpleParameterEntry("infinispan-cluster-name", "JCR-cluster"));
      }

      return helper.createRepository(container, repoEntry);
   }
}
