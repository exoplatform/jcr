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
package org.exoplatform.services.jcr.impl;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.config.LockManagerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.checker.RepositoryCheckController;
import org.exoplatform.services.jcr.impl.core.ItemImpl;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.CacheableLockManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.storage.jdbc.DBConstants;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCDataContainerConfig.DatabaseStructureType;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
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
import java.util.List;

import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
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

   private static boolean CACHE_ENABLED = true;

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
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
      //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   public void testLockUsecases() throws Exception
   {
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_ENABLED,
         NOT_SHARED_CACHE));
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_ENABLED,
         NOT_SHARED_CACHE));
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_ENABLED,
         SHARED_CACHE));
      checkConsistentLocksInDataBase(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_ENABLED,
         SHARED_CACHE));

      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_ENABLED,
         NOT_SHARED_CACHE));
      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_ENABLED,
         NOT_SHARED_CACHE));
      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.SINGLE, CACHE_ENABLED,
         SHARED_CACHE));
      checkInconsistentLocksInLockTable(helper.createRepository(container, DatabaseStructureType.MULTI, CACHE_ENABLED,
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

   public void testCheckIndex() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkIndex(), checkController.getLastReportPath(), true);
      //assertTrue(checkController.checkIndex().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   public void testCheckAll() throws Exception
   {
      TesterRepositoryCheckController checkController =
         new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      assertResult(checkController.checkAll(), checkController.getLastReportPath(), true);
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

         Node node = addTestNode(repository);
         PropertyImpl prop = (PropertyImpl)addTestProperty(repository, node);

         assertResult(checkController.checkDataBase(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

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
         //assertTrue(checkController.checkDataBase().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removePropertyInDB(repository, (PropertyImpl)node.getProperty("jcr:primaryType"));
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
         node.save();

         assertResult(checkController.checkValueStorage(), checkController.getLastReportPath(), true);
         //assertTrue(checkController.checkValueStorage().startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));

         removeFileFromVS(repository, prop.getInternalIdentifier());
         assertResult(checkController.checkValueStorage(), checkController.getLastReportPath(), false);
         //assertTrue(checkController.checkValueStorage().startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));

         checkController.repairValueStorage("yes");
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
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());

      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      node.addMixin("mix:referenceable");
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
            "DELETE FROM " + lockManagerEntry.getParameterValue("infinispan-cl-cache.jdbc.table.name") + "_" + "L"
               + workspaceEntry.getUniqueName().replace("_", "").replace("-", "_");
      }
      else
      {
         sourceName = lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_CL_DATASOURCE);

         if (lockManagerEntry.getParameterBoolean("jbosscache-shareable"))
         {
            queryStatement =
               "DELETE FROM " + lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_TABLE_NAME)
                  + " WHERE PARENT='/" + workspaceEntry.getUniqueName() + "/" + CacheableLockManagerImpl.LOCKS + "'";
         }
         else
         {
            queryStatement =
               "DELETE FROM " + lockManagerEntry.getParameterValue(CacheableLockManagerImpl.JBOSSCACHE_JDBC_TABLE_NAME)
                  + " WHERE PARENT='/" + CacheableLockManagerImpl.LOCKS + "'";
         }
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
      throws RepositoryConfigurationException, SQLException, NamingException, RepositoryException
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
      conn.prepareStatement("INSERT INTO " + vTable + " VALUES ('10000','data','1','" + propId + "',NULL)").execute();

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
}
