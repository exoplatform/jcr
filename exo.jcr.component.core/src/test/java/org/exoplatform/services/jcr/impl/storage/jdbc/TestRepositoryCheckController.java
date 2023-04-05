/*
 * Copyright (C) 2011 eXo Platform SAS.
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

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.TesterRepositoryCheckController;
import org.exoplatform.services.jcr.impl.checker.InconsistencyRepair;
import org.exoplatform.services.jcr.impl.checker.NodeRemover;
import org.exoplatform.services.jcr.impl.checker.RepositoryCheckController;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.nodetype.NodeTypeDataManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.optimisation.db.SybaseJDBCConnectionHelper;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 10.10.2011 skarpenko $
 *
 */
public class TestRepositoryCheckController extends BaseStandaloneTest
{
   private static boolean SHARED_CACHE = true;

   private static boolean NOT_SHARED_CACHE = false;

   private static boolean CACHE_DISABLED = false;

   private final TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   protected String getRepositoryName()
   {
      return null;
   }

   public void tearDown() throws Exception
   {
      // remove generated reports
      for (File file : new File(".").listFiles())
      {
         if (file.getName().startsWith("report"))
         {
            file.delete();
         }
      }

      super.tearDown();
   }

   public void testCheckDataBase() throws Exception
   {
      ManageableRepository db1 = repositoryService.getRepository("db1");
      TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(db1);

      checkDatabase(checkController, 1);
   }

   public void testCheckDataBaseMultiThreading() throws Exception
   {
      ManageableRepository db1 = repositoryService.getRepository("db1");
      TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(db1);

      checkDatabase(checkController, 5);
   }

   private void checkDatabase(TesterRepositoryCheckController checkController, int nThreads) throws Exception
   {
      SessionImpl session = (SessionImpl)repository.login(credentials, "ws1");
      Node testRoot = session.getRootNode().addNode("testRoot");
      Node exoTrash = testRoot.addNode("exo:trash");
      Node exoTrash2 = testRoot.addNode("exo:trash2");
      exoTrash.addNode("node1");
      exoTrash.addNode("node2");
      Node node1 = exoTrash2.addNode("node1");
      Node node2 = exoTrash2.addNode("node2");

      session.save();

      assertResult(checkController.checkIndex(nThreads), checkController.getLastReportPath(), true);

      QueryManager qman = session.getWorkspace().getQueryManager();

      Query q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(5, q.execute().getNodes().getSize());

      node1.addMixin("exo:hiddenable");
      node2.addMixin("exo:nothiddenable");

      session.save();

      assertResult(checkController.checkIndex(nThreads), checkController.getLastReportPath(), true);

      q = qman.createQuery("SELECT * FROM nt:base WHERE jcr:path LIKE '/testRoot/%'", Query.SQL);
      assertEquals(4, q.execute().getNodes().getSize());

      testRoot.remove();
      session.save();
   }

   public void testLockUsecases() throws Exception
   {
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED,
         NOT_SHARED_CACHE));
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_DISABLED,
         NOT_SHARED_CACHE));
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED,
         SHARED_CACHE));
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_DISABLED,
         SHARED_CACHE));

      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED,
         NOT_SHARED_CACHE));
      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_DISABLED,
         NOT_SHARED_CACHE));
      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED,
         SHARED_CACHE));
      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_DISABLED,
         SHARED_CACHE));
   }

   private void checkConsistentLocksInDataBase(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         lockNode(node);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removePropertyInDB(repository, (PropertyImpl)node.getProperty("jcr:lockIsDeep"));
         removePropertyInDB(repository, (PropertyImpl)node.getProperty("jcr:lockOwner"));
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   private void checkInconsistentLocksInLockTable(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         lockNode(node);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         clearLockTable(repository);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   public void testCheckValueStorage() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkValueStorage(), checkController.getLastReportPath(), true);
      //assertTrue(checkController.checkValueStorage().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   public void testCheckValueStorageMultiThreading() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkValueStorage(5), checkController.getLastReportPath(), true);
    }

   public void testCheckIndex() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));


      assertResult(checkController.checkIndex(), checkController.getLastReportPath(), true);
   }

   public void testCheckIndexMultiThreading() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkIndex(5), checkController.getLastReportPath(), true);
   }

   public void testCheckAll() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkAll(), checkController.getLastReportPath(), true);
      //assertTrue(checkController.checkAll().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   public void testCheckAllMultiThreading() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkAll(5), checkController.getLastReportPath(), true);
        //assertTrue(checkController.checkAll().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   /**
    * Index contains documents that was already removed from DB.
    */
   public void testIndexUsecaseWrongDocumentId() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED);
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         assertResult(checkController.checkIndex(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removeNodeInDB(repository, node);
         assertResult(checkController.checkIndex(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Index contains multiple documents.
    */
   public void testIndexUsecaseMultipleDocuments() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED);
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         assertResult(checkController.checkIndex(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         indexNode(repository, node, ItemState.ADDED);
         assertResult(checkController.checkIndex(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Index doesn't contain document which stored in DB.
    */
   public void testIndexUsecaseDocumentNotExists() throws Exception
   {
      ManageableRepository repository = null;
      try
      {
         repository = helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED);
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         assertResult(checkController.checkIndex(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         indexNode(repository, node, ItemState.DELETED);
         assertResult(checkController.checkIndex(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Ensure index has pending deletions, then call optimize to get rid of it. 
    */
   public void testOptimizeIndexUsecase() throws Exception
   {
      ManageableRepository repository =
         helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED);

      makeIndexContaingDeletions(repository);

      boolean hasDeletions = hasDeletions(repository);
      if (hasDeletions)
      {
         optimize(repository);
         assertFalse(hasDeletions(repository));
      }

      helper.removeRepository(container, repository.getConfiguration().getName());
   }

   /**
    * Ensures index contains deletions.
    */
   private void makeIndexContaingDeletions(ManageableRepository repository) throws Exception
   {
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      Node testRoot = session.getRootNode().addNode("test");
      for (int i = 0; i < 200; i++)
      {
         Node node = testRoot.addNode("test" + i);
         node.addMixin("mix:versionable");
         session.save();

         node.checkin();
         node.checkout();
      }

      testRoot.remove();
      session.save();
   }

   /**
    * Checks if index has deletions.
    */
   private boolean hasDeletions(ManageableRepository repository)
   {
      boolean hasDeletions = false;

      for (String wsName : repository.getWorkspaceNames())
      {
         List<SearchManager> searches =
            repository.getWorkspaceContainer(wsName).getComponentInstancesOfType(SearchManager.class);

         for (SearchManager search : searches)
         {
            hasDeletions |= search.hasDeletions();
         }
      }
      return hasDeletions;
   }

   /**
    * Checks if index has deletions.
    */
   private void optimize(ManageableRepository repository)
   {
      for (String wsName : repository.getWorkspaceNames())
      {
         List<SearchManager> searches =
            repository.getWorkspaceContainer(wsName).getComponentInstancesOfType(SearchManager.class);

         for (SearchManager search : searches)
         {
            search.optimize();
         }
      }
   }

   /**
    *  Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesTheParentIdIsIdOfThisNode() throws Exception
   {
      checkDBUsecasesTheParentIdIsIdOfThisNode(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));
      checkDBUsecasesTheParentIdIsIdOfThisNode2(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));

      checkDBUsecasesTheParentIdIsIdOfThisNode(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
      checkDBUsecasesTheParentIdIsIdOfThisNode2(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesTheParentIdIsIdOfThisNode(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         assingItsOwnParent(repository, (ItemImpl)node);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   private void checkDBUsecasesTheParentIdIsIdOfThisNode2(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         Property prop = addTestProperty(repository, node);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         assingItsOwnParent(repository, (ItemImpl)prop);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    *  Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesSeveralVersionsOfSameItem() throws Exception
   {
      checkSeveralVersionsOfSameItem(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED));
      checkSeveralVersionsOfSameItem(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_DISABLED));
   }

   private void checkSeveralVersionsOfSameItem(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         NodeImpl node1 = (NodeImpl)addTestNode(repository);
         NodeImpl node2 = (NodeImpl)addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)addTestProperty(repository, node1);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         updateNodeRecord(repository, node2.getInternalIdentifier(), 1, 1);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);

         insertPropertyRecord(repository, prop.getInternalIdentifier(), prop.getParentIdentifier(), prop.getName());
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    *  Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesPropertyWithoutParent() throws Exception
   {
      checkDBUsecasesPropertyWithoutParent(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));
      checkDBUsecasesPropertyWithoutParent(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesPropertyWithoutParent(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         insertPropertyRecord(repository, IdGenerator.generate(), IdGenerator.generate(), "testName");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Usecase: Incorrect JCR_VALUE records.
    */
   public void testDBUsecasesIncorrectValueRecords() throws Exception
   {
      checkDBUsecasesIncorrectValueRecords(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));
      checkDBUsecasesIncorrectValueRecords(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesIncorrectValueRecords(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)addTestProperty(repository, node);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         updateValueRecord(repository, prop.getInternalIdentifier());
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Usecase: value records has no item record.
    */
   public void testDBUsecasesValueRecordHasNoItemRecord() throws Exception
   {
      checkDBUsecasesValueRecordHasNoItemRecord(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));
      checkDBUsecasesValueRecordHasNoItemRecord(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesValueRecordHasNoItemRecord(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)addTestProperty(repository, node);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removeItemRecord(repository, prop.getInternalIdentifier());
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Usecase: properties that have not value record.
    */
   public void testDBUsecasesPrimaryTypePropertyHasNoValueRecord() throws Exception
   {
      checkDBUsecasesPrimaryTypePropertyHasNoValueRecor(helper.createRepository(container,
         DatabaseStructureType.SINGLE, CACHE_DISABLED));
      checkDBUsecasesPrimaryTypePropertyHasNoValueRecor(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesPrimaryTypePropertyHasNoValueRecor(ManageableRepository repository) throws Exception
   {
      TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

      Node node = addTestNode(repository);
      PropertyImpl prop = (PropertyImpl)node.getProperty("jcr:primaryType");

      assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

      removeValueRecord(repository, prop.getInternalIdentifier());
      assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

      checkController.repairDataBase("yes");
      assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

      helper.removeRepository(container, repository.getConfiguration().getName());
   }

   /**
    * Usecase: properties that have not value record.
    */
   public void testDBUsecasesPropertiesHasNoValueRecord() throws Exception
   {
      checkDBUsecasesPropertiesHasNoSingleValueRecord(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));
      checkDBUsecasesPropertiesHasEmptyMultiValueRecord(helper.createRepository(container,
         DatabaseStructureType.SINGLE, CACHE_DISABLED));

      checkDBUsecasesPropertiesHasNoSingleValueRecord(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
      checkDBUsecasesPropertiesHasEmptyMultiValueRecord(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesPropertiesHasNoSingleValueRecord(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)addTestProperty(repository, node);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removeValueRecord(repository, prop.getInternalIdentifier());
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }

   }

   private void checkDBUsecasesPropertiesHasEmptyMultiValueRecord(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         node.setProperty("prop", new String[]{});
         node.save();

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Usecase: reference properties without reference records.
    */
   public void testDBUsecasesReferencePropertyWithoutReferenceRecord() throws Exception
   {
      checkDBUsecasesReferencePropertyWithoutReferenceRecord(helper.createRepository(container,
         DatabaseStructureType.SINGLE, CACHE_DISABLED));
      checkDBUsecasesReferencePropertyWithoutReferenceRecord(helper.createRepository(container,
         DatabaseStructureType.MULTI, CACHE_DISABLED));
   }

   private void checkDBUsecasesReferencePropertyWithoutReferenceRecord(ManageableRepository repository)
      throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         Node node2 = addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)node2.setProperty("prop", node);
         node2.save();

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removeReferenceRecord(repository, prop.getInternalIdentifier());
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Usecase when node doesn't have at primary type property.
    */
   public void testDBUsecasesNodeHasNoProperties() throws Exception
   {
      checkDBUsecasesNodeHasNotPrimaryTypeProperties(helper.createRepository(container, DatabaseStructureType.SINGLE,
         CACHE_DISABLED));
      checkDBUsecasesNodeHasNotPrimaryTypeProperties(helper.createRepository(container, DatabaseStructureType.MULTI,
         CACHE_DISABLED));
   }

   private void checkDBUsecasesNodeHasNotPrimaryTypeProperties(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);
         Node node = addTestNode(repository);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);

         removePropertyInDB(repository, (PropertyImpl)node.getProperty("jcr:primaryType"));
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);

         checkController.repairDataBase("yes");
         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   /**
    * Usecase when tree of nodes do't have primary type property.
    */
   public void testDBUsecasesTreeOfNodeHasNoProperties() throws Exception
   {
      checkDBUsecasesTreeOfNodeHasNotPrimaryTypeProperties1(helper.createRepository(container,
         DatabaseStructureType.SINGLE, CACHE_DISABLED));
      checkDBUsecasesTreeOfNodeHasNotPrimaryTypeProperties1(helper.createRepository(container,
         DatabaseStructureType.MULTI, CACHE_DISABLED));
   }

   private void checkDBUsecasesTreeOfNodeHasNotPrimaryTypeProperties1(ManageableRepository repository) throws Exception
   {
      TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);
      NodeImpl node1 = (NodeImpl)addTestNode(repository);
      NodeImpl node2 = (NodeImpl)addTestNode(repository, node1.getUUID());
      NodeImpl node3 = (NodeImpl)addTestNode(repository, node2.getUUID());

      assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);

      removePropertyInDB(repository, (PropertyImpl)node3.getProperty("jcr:primaryType"));
      removePropertyInDB(repository, (PropertyImpl)node2.getProperty("jcr:primaryType"));
      removePropertyInDB(repository, (PropertyImpl)node1.getProperty("jcr:primaryType"));
      assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), false);

      InconsistencyRepair repair = getNodeRemover(repository);

      // check correctness repairing child with parent without primaryType property
      Connection conn = getConnection(repository);
      ResultSet resultSet = getResultSetWithNode(repository, conn, node3);
      resultSet.next();

      repair.doRepair(resultSet);
      resultSet.close();
      conn.close();

      // check correctness repairing parent with child without primaryType property
      conn = getConnection(repository);
      resultSet = getResultSetWithNode(repository, conn, node1);
      resultSet.next();

      repair.doRepair(resultSet);
      resultSet.close();
      conn.close();

      // check correctness repairing node already removed in previous check
      Map<String, String> strFields = new HashMap<String, String>();
      strFields.put(DBConstants.COLUMN_PARENTID, "already-removed-parentId");
      strFields.put(DBConstants.COLUMN_NAME, "[]" + node2.getName());
      strFields.put(DBConstants.COLUMN_ID, node2.getIdentifier());

      Map<String, Integer> intFields = new HashMap<String, Integer>();
      intFields.put(DBConstants.COLUMN_NORDERNUM, 1);
      intFields.put(DBConstants.COLUMN_VERSION, 0);
      intFields.put(DBConstants.COLUMN_INDEX, 0);

      resultSet = new FakeResultSet(strFields, intFields);

      repair.doRepair(resultSet);

      assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);

      helper.removeRepository(container, repository.getConfiguration().getName());
   }

   /**
    * Usescase when STORAGE_DESC field in VALUE table is not empty but there is no file in the value storage.
    */
   public void testValueStorageUsecases() throws Exception
   {
      checkValueStorageUsecases(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_DISABLED));
      checkValueStorageUsecases(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_DISABLED));
   }

   private void checkValueStorageUsecases(ManageableRepository repository) throws Exception
   {
      try
      {
         TesterRepositoryCheckController checkController = new TesterRepositoryCheckController(repository);

         Node node = addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)node.setProperty("prop", new FileInputStream(createBLOBTempFile(300)));
         PropertyImpl propFileRenamed = (PropertyImpl)node.setProperty("prop2", new FileInputStream(createBLOBTempFile(300)));
         node.save();

         assertResult(checkController.checkValueStorage(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkValueStorage().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         renameFileInVS(repository, propFileRenamed.getInternalIdentifier());
         removeFileFromVS(repository, prop.getInternalIdentifier());
         assertResult(checkController.checkValueStorage(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkValueStorage().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         node.getSession().save();
         checkController.repairValueStorage("yes");
         node = (Node) node.getSession().getItem(node.getPath());

         assertEquals(307200, node.getProperty("prop2").getStream().available());
         assertEquals(0, node.getProperty("prop").getStream().available());

         assertResult(checkController.checkValueStorage(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkValueStorage().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         if (repository != null)
         {
            helper.removeRepository(container, repository.getConfiguration().getName());
         }
      }
   }

   private Node addTestNode(ManageableRepository repository) throws LoginException, NoSuchWorkspaceException,
      RepositoryException
   {
      return addTestNode(repository, Constants.ROOT_UUID);
   }

   private Node addTestNode(ManageableRepository repository, String parentId) throws LoginException,
      NoSuchWorkspaceException, RepositoryException
   {
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      Node parent = session.getNodeByIdentifier(parentId);

      NodeImpl node = (NodeImpl)parent.addNode("testNode");
      node.addMixin("mix:referenceable");
      node.addMixin("mix:versionable");
      session.save();

      return node;
   }

   private Property addTestProperty(ManageableRepository repository, Node node) throws LoginException,
      NoSuchWorkspaceException, RepositoryException
   {
      Property prop = node.setProperty("testProp", "value");
      node.save();

      return prop;
   }

   private void lockNode(Node node) throws LoginException, NoSuchWorkspaceException, RepositoryException
   {
      node.addMixin("mix:lockable");
      node.save();

      node.lock(false, false);
   }

   private void removeNodeInDB(ManageableRepository repository, Node node) throws SQLException,
      RepositoryConfigurationException, NamingException, UnsupportedRepositoryOperationException, RepositoryException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);

      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String vTable = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";
      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";

      String nodeId = (isMultiDb ? "" : wsEntry.getName()) + node.getUUID();

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      ResultSet resultSet =
         conn.prepareStatement("SELECT * FROM " + iTable + " WHERE PARENT_ID='" + nodeId + "'").executeQuery();
      while (resultSet.next())
      {
         String propertyId = resultSet.getString(DBConstants.COLUMN_ID);

         conn.prepareStatement("DELETE FROM " + vTable + " WHERE PROPERTY_ID = '" + propertyId + "'").execute();
         conn.prepareStatement("DELETE FROM " + iTable + " WHERE ID = '" + propertyId + "'").execute();
      }

      conn.prepareStatement("DELETE FROM " + iTable + " WHERE ID='" + nodeId + "'").execute();

      conn.commit();
      conn.close();
   }

   private void removePropertyInDB(ManageableRepository repository, PropertyImpl property) throws SQLException,
      RepositoryConfigurationException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);

      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String vTable = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";
      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";

      String propertyId = (isMultiDb ? "" : wsEntry.getName()) + property.getInternalIdentifier();

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement("DELETE FROM " + vTable + " WHERE PROPERTY_ID = '" + propertyId + "'").execute();
      conn.prepareStatement("DELETE FROM " + iTable + " WHERE ID = '" + propertyId + "'").execute();

      conn.commit();
      conn.close();
   }

   private ResultSet getResultSetWithNode(ManageableRepository repository, Connection conn, NodeImpl node)
      throws Exception
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);

      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();

      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";
      String nodeId = (isMultiDb ? "" : wsEntry.getName()) + node.getInternalIdentifier();

      return conn.prepareStatement("SELECT * FROM " + iTable + " WHERE ID = '" + nodeId + "'").executeQuery();
   }

   private Connection getConnection(ManageableRepository repository) throws Exception
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      return ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
   }

   private void removeValueRecord(ManageableRepository repository, String propId) throws SQLException,
      RepositoryConfigurationException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);

      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String vTable = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";

      propId = (isMultiDb ? "" : wsEntry.getName()) + propId;

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement("DELETE FROM " + vTable + " WHERE PROPERTY_ID = '" + propId + "'").execute();
      conn.prepareStatement(
         "ALTER TABLE " + vTable + " DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S") + "VALUE_PROPERTY").execute();

      conn.commit();
      conn.close();
   }

   private void removeReferenceRecord(ManageableRepository repository, String propId) throws SQLException,
      RepositoryConfigurationException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);

      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String rTable = "JCR_" + (isMultiDb ? "M" : "S") + "REF";

      propId = (isMultiDb ? "" : wsEntry.getName()) + propId;

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement("DELETE FROM " + rTable + " WHERE PROPERTY_ID = '" + propId + "'").execute();

      conn.commit();
      conn.close();
   }

   private void removeItemRecord(ManageableRepository repository, String propId) throws SQLException,
      RepositoryConfigurationException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);

      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String vTable = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";
      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";

      propId = (isMultiDb ? "" : wsEntry.getName()) + propId;

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement(
         "ALTER TABLE " + vTable + " DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S") + "VALUE_PROPERTY").execute();
      conn.prepareStatement(
         "ALTER TABLE " + iTable + " DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S") + "ITEM_PARENT").execute();

      conn.prepareStatement("DELETE FROM " + iTable + " WHERE ID = '" + propId + "'").execute();

      conn.commit();
      conn.close();
   }

   private void assingItsOwnParent(ManageableRepository repository, ItemImpl item) throws SQLException,
      RepositoryConfigurationException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();

      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";
      String itemId = (isMultiDb ? "" : wsEntry.getName()) + item.getInternalIdentifier();

      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement("DROP INDEX JCR_IDX_" + (isMultiDb ? "M" : "S") + "ITEM_PARENT").execute();
      conn.prepareStatement("DROP INDEX JCR_IDX_" + (isMultiDb ? "M" : "S") + "ITEM_PARENT_NAME").execute();

      conn.prepareStatement("UPDATE " + iTable + " SET PARENT_ID='" + itemId + "' WHERE ID='" + itemId + "'").execute();

      conn.commit();
      conn.close();

   }

   private void clearLockTable(ManageableRepository repository) throws RepositoryConfigurationException, SQLException,
      NamingException
   {
      WorkspaceEntry workspaceEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      LockManagerEntry lockManagerEntry = workspaceEntry.getLockManager();

      String sourceName = null;
      String queryStatement = null;

      if (helper.ispnCacheEnabled())
      {
         sourceName = lockManagerEntry.getParameterValue("infinispan-cl-cache.jdbc.datasource");

         queryStatement =
            "DELETE FROM \"" + lockManagerEntry.getParameterValue("infinispan-cl-cache.jdbc.table.name") + "_" + "L"
               + workspaceEntry.getUniqueName().replace("_", "").replace("-", "_") + "\"";
      }

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement(queryStatement).execute();
      conn.commit();
      conn.close();
   }

   private void indexNode(ManageableRepository repository, Node node, int state)
      throws UnsupportedRepositoryOperationException, RepositoryException
   {
      // Indexing one more document with same UUID
      List<SearchManager> searchManagers =
         repository.getWorkspaceContainer(repository.getConfiguration().getSystemWorkspaceName())
            .getComponentInstancesOfType(SearchManager.class);

      PlainChangesLog log = new PlainChangesLogImpl();

      NodeData data =
         new TransientNodeData(QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName("", "testNode")),
            node.getUUID(), -1, Constants.NT_UNSTRUCTURED, null, 0, null, new AccessControlList());

      TransientPropertyData primaryType =
         new TransientPropertyData(QPath.makeChildPath(data.getQPath(), Constants.JCR_PRIMARYTYPE),
            IdGenerator.generate(), -1, PropertyType.NAME, data.getIdentifier(), false, new TransientValueData(
               Constants.NT_UNSTRUCTURED));

      log.add(new ItemState(data, ItemState.ADDED, false, null));
      log.add(new ItemState(primaryType, ItemState.ADDED, false, null));

      for (SearchManager searchManager : searchManagers)
      {
         if (!(searchManager instanceof SystemSearchManager))
         {
            searchManager.onSaveItems(log);
            break;
         }
      }
   }

   private void insertPropertyRecord(ManageableRepository repository, String id, String parentId, String name)
      throws RepositoryConfigurationException, SQLException, NamingException,
      RepositoryException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();

      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";
      String vTable = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";

      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      // add another item with new persisted version
      String propId = (isMultiDb ? "" : wsEntry.getName()) + IdGenerator.generate();
      parentId = (isMultiDb ? "" : wsEntry.getName()) + parentId;

      conn.prepareStatement(
         "ALTER TABLE " + iTable + " DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S") + "ITEM_PARENT").execute();

      if (isMultiDb)
      {
         conn.prepareStatement(
            "INSERT INTO " + iTable + " VALUES ('" + propId + "','" + parentId + "','[]" + name
               + "',1,2,1,NULL,1,FALSE)").execute();
      }
      else
      {
         conn.prepareStatement(
            "INSERT INTO " + iTable + " VALUES ('" + propId + "','" + parentId + "','[]" + name + "',1,'"
               + wsEntry.getName() + "',2,1,NULL,1,FALSE)").execute();
      }
      conn.prepareStatement("INSERT INTO " + vTable + "(DATA,ORDER_NUM,PROPERTY_ID,STORAGE_DESC) VALUES (NULL,1,'" + propId + "',NULL)").execute();

      conn.commit();
      conn.close();
   }

   private void updateValueRecord(ManageableRepository repository, String propId)
      throws RepositoryConfigurationException, SQLException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();

      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String vTable = "JCR_" + (isMultiDb ? "M" : "S") + "VALUE";
      propId = (isMultiDb ? "" : wsEntry.getName()) + propId;

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "UPDATE " + vTable + " SET STORAGE_DESC = 'unexisted-desc' WHERE PROPERTY_ID = '" + propId + "'").execute();

      conn.commit();
      conn.close();
   }

   private void updateNodeRecord(ManageableRepository repository, String nodeId, int newPersistedVersion, int newIndex)
      throws RepositoryConfigurationException, SQLException, NamingException
   {
      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb =
         DatabaseStructureType.valueOf(
            wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.DB_STRUCTURE_TYPE)).isMultiDatabase();

      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String iTable = "JCR_" + (isMultiDb ? "M" : "S") + "ITEM";
      nodeId = (isMultiDb ? "" : wsEntry.getName()) + nodeId;

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "UPDATE " + iTable + " SET VERSION=" + newPersistedVersion + ", I_INDEX=" + newIndex + " WHERE ID = '"
            + nodeId + "'").execute();

      conn.commit();
      conn.close();
   }

   private void renameFileInVS(ManageableRepository repository, String propId)
      throws Exception
   {
      String vsPath =
         repository.getConfiguration().getWorkspaceEntries().get(0).getContainer().getValueStorages().get(0)
            .getParameterValue(FileValueStorage.PATH);

      File vsFile = new File(vsPath, propId + "0");
      assertTrue(vsFile.exists());
      assertTrue(vsFile.renameTo(new File(vsFile.getParentFile(), vsFile.getName() + ".FFFF")));
   }

   private void removeFileFromVS(ManageableRepository repository, String propId)
      throws RepositoryConfigurationException
   {
      String vsPath =
         repository.getConfiguration().getWorkspaceEntries().get(0).getContainer().getValueStorages().get(0)
            .getParameterValue(FileValueStorage.PATH);

      File vsFile = new File(vsPath, propId + "0");
      assertTrue(vsFile.exists());
      assertTrue(vsFile.delete());
   }

   private void assertResult(String reportMessage, String reportPath, boolean consistent)
   {
      String expected =
         consistent ? RepositoryCheckController.REPORT_CONSISTENT_MESSAGE
            : RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE;
      if (!reportMessage.startsWith(expected))
      {
         log.error("Expected result is:" + expected + "but report is:" + reportMessage);
         log.error("Full report content is ... " + getFileContent(reportPath) + "======================");
         fail("Expected result is \"" + expected + "\", but report is \"" + reportMessage + "\"");
      }
      // else - ok
   }

   private InconsistencyRepair getNodeRemover(ManageableRepository repository)
   {
      String wsName = repository.getConfiguration().getSystemWorkspaceName();
      WorkspaceContainerFacade wsContainer = repository.getWorkspaceContainer(wsName);

      NodeTypeDataManagerImpl nodeTypeManager =
         (NodeTypeDataManagerImpl)wsContainer.getComponent(NodeTypeDataManagerImpl.class);

      JDBCWorkspaceDataContainer jdbcDataContainer =
         (JDBCWorkspaceDataContainer)wsContainer.getComponent(JDBCWorkspaceDataContainer.class);

      InconsistencyRepair repair =
         new NodeRemover(jdbcDataContainer.getConnectionFactory(), jdbcDataContainer.containerConfig, nodeTypeManager);

      return repair;
   }

   private String getFileContent(String reportPath)
   {
      StringBuffer contents = new StringBuffer();
      BufferedReader reader = null;
      try
      {
         reader = new BufferedReader(new FileReader(reportPath));
         String text = null;

         // repeat until all lines is read
         while ((text = reader.readLine()) != null)
         {
            contents.append(text).append(System.getProperty("line.separator"));
         }
      }
      catch (FileNotFoundException e)
      {
         return "Report file (" + reportPath + ") not found.";
      }
      catch (IOException e)
      {
         return "IOException reading report file (" + reportPath + ").";
      }
      finally
      {
         try
         {
            if (reader != null)
            {
               reader.close();
            }
         }
         catch (IOException e)
         {
            e.printStackTrace();
         }
      }

      // show file contents here
      return contents.toString();
   }

   private class FakeResultSet extends SybaseJDBCConnectionHelper.EmptyResultSet
   {
      private final Map<String, String> strFields;

      private final Map<String, Integer> intFields;

      FakeResultSet(Map<String, String> strFields, Map<String, Integer> intFields)
      {
         this.strFields = strFields;
         this.intFields = intFields;
      }

      public String getString(String columnName) throws SQLException
      {
         String value = strFields.get(columnName);
         if (value == null)
         {
            throw new SQLException("Field not found");
         }

         return value;
      }

      public int getInt(String columnName) throws SQLException
      {
         Integer value = intFields.get(columnName);
         if (value == null)
         {
            throw new SQLException("Field not found");
         }

         return value;
      }
   }
}
