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
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.RepositoryCheckController.DataStorage;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.lock.jbosscache.CacheableLockManagerImpl;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.SystemSearchManager;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.impl.storage.jdbc.JDBCWorkspaceDataContainer;
import org.exoplatform.services.jcr.impl.storage.value.fs.FileValueStorage;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.jcr.util.TesterConfigurationHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * @author <a href="mailto:skarpenko@exoplatform.com">Sergiy Karpenko</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 10.10.2011 skarpenko $
 *
 */
public class TestRepositoryCheckController extends BaseStandaloneTest
{

   private TesterRepositoryCheckController checkController;

   private static boolean SHARED_CACHE = true;

   private static boolean NOT_SHARED_CACHE = false;

   private static boolean MULTI_DB = true;

   private static boolean SINGLE_DB = false;

   private static boolean CACHE_ENABLED = true;

   private final TesterConfigurationHelper helper = TesterConfigurationHelper.getInstance();

   /**
    * @see org.exoplatform.services.jcr.BaseStandaloneTest#getRepositoryName()
    */
   @Override
   protected String getRepositoryName()
   {
      String repName = System.getProperty("test.repository");
      if (repName == null)
      {
         throw new RuntimeException(
            "Test repository is undefined. Set test.repository system property "
               + "(For maven: in project.properties: maven.junit.sysproperties=test.repository\ntest.repository=<rep-name>)");
      }
      return repName;

   }

   public void setUp() throws Exception
   {
      super.setUp();
   }

   public void testDB() throws Exception
   {
      checkController = new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB});
      assertNotNull(result);
      assertTrue("Repository data is not consistent, result: " + result,
         result.startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   public void testConsistentLocksInDataBase() throws Exception
   {
      checkConsistentLocksInDataBase(NOT_SHARED_CACHE, SINGLE_DB);
      checkConsistentLocksInDataBase(NOT_SHARED_CACHE, MULTI_DB);
   }

   public void testConsistentLocksInDataBaseSharedCache() throws Exception
   {
      checkConsistentLocksInDataBase(SHARED_CACHE, SINGLE_DB);
      checkConsistentLocksInDataBase(SHARED_CACHE, MULTI_DB);
   }

   private void checkConsistentLocksInDataBase(boolean isCacheShared, boolean isMultiDb) throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, isMultiDb, CACHE_ENABLED, isCacheShared);
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      node.addMixin("mix:lockable");
      session.save();
      node.lock(false, false);

      checkController = new TesterRepositoryCheckController(repository);
      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB});
      assertNotNull(result);

      assertTrue("Repository data is not consistent, result: " + result,
         result.startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   public void testInconsistentLocksInDataBase() throws Exception
   {
      checkInconsistentLocksInItemTable(NOT_SHARED_CACHE, SINGLE_DB);
      checkInconsistentLocksInItemTable(NOT_SHARED_CACHE, MULTI_DB);

      checkInconsistentLocksInLockTable(NOT_SHARED_CACHE, SINGLE_DB);
      checkInconsistentLocksInLockTable(NOT_SHARED_CACHE, MULTI_DB);
   }

   public void testInconsistentLocksInDataBaseWithSharedCache() throws Exception
   {
      checkInconsistentLocksInItemTable(SHARED_CACHE, SINGLE_DB);
      checkInconsistentLocksInItemTable(SHARED_CACHE, MULTI_DB);

      checkInconsistentLocksInLockTable(SHARED_CACHE, SINGLE_DB);
      checkInconsistentLocksInLockTable(SHARED_CACHE, MULTI_DB);
   }

   private void checkInconsistentLocksInItemTable(boolean cacheShared, boolean isMultiDb) throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, isMultiDb, CACHE_ENABLED, cacheShared);
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      node.addMixin("mix:lockable");
      session.save();
      node.lock(false, false);

      WorkspaceEntry workspaceEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      String sourceName = workspaceEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      String multiDbQueryStatement =
         "DELETE FROM JCR_MITEM WHERE I_CLASS=2 "
            + "AND (NAME='[http://www.jcp.org/jcr/1.0]lockOwner' OR NAME='[http://www.jcp.org/jcr/1.0]lockIsDeep')";
      String singleDbQueryStatement =
         "DELETE FROM JCR_SITEM WHERE CONTAINER_NAME='"
            + workspaceEntry.getName()
            + "' AND I_CLASS=2 AND (NAME='[http://www.jcp.org/jcr/1.0]lockOwner' OR NAME='[http://www.jcp.org/jcr/1.0]lockIsDeep')";

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      // remove constraint
      conn.prepareStatement(
         "ALTER TABLE JCR_" + (isMultiDb ? "M" : "S") + "ITEM DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S")
            + "VALUE_PROPERTY").execute();
      // delete properties (this should cause inconsistency)
      conn.prepareStatement(isMultiDb ? multiDbQueryStatement : singleDbQueryStatement).execute();

      // remove constriant
      conn.prepareStatement(
         "ALTER TABLE JCR_" + (isMultiDb ? "M" : "S") + "VALUE DROP CONSTRAINT JCR_PK_" + (isMultiDb ? "M" : "S")
            + "VALUE").execute();

      // clean up properties value to avoid another (except needed) cause of inconsistency
      String lockOwnerPropertyId =
         (isMultiDb ? "" : workspaceEntry.getName())
            + ((PropertyImpl)node.getProperty("jcr:lockIsDeep")).getInternalIdentifier();
      String lockIsDeepPropertyId =
         (isMultiDb ? "" : workspaceEntry.getName())
            + ((PropertyImpl)node.getProperty("jcr:lockOwner")).getInternalIdentifier();

      conn.prepareStatement(
         "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "VALUE WHERE PROPERTY_ID = '" + lockOwnerPropertyId
            + "' OR PROPERTY_ID = '" + lockIsDeepPropertyId + "'").execute();
      conn.commit();
      conn.close();

      checkController = new TesterRepositoryCheckController(repository);
      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB});
      assertNotNull(result);
      assertTrue("Repository data is consistent, result: " + result,
         result.startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   private void checkInconsistentLocksInLockTable(boolean cacheShared, boolean isMultiDb) throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, isMultiDb, true, cacheShared);
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      node.addMixin("mix:lockable");
      session.save();
      node.lock(false, false);

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

         if (cacheShared)
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

      checkController = new TesterRepositoryCheckController(repository);
      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB});
      assertNotNull(result);
      assertTrue("Repository data is consistent, result: " + result,
         result.startsWith(RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   public void testValueStorage() throws Exception
   {
      checkController = new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      File f = this.createBLOBTempFile(20);
      InputStream is = new FileInputStream(f);
      try
      {
         Node n = root.addNode("node");
         n.setProperty("prop", is);

         root.save();

         String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.VALUE_STORAGE});
         assertNotNull(result);
         assertTrue("Repository data is not consistent, result: " + result,
            result.startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      }
      finally
      {
         is.close();
         f.delete();
      }
   }

   public void testSearchIndex() throws Exception
   {
      checkController = new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      String result = checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.LUCENE_INDEX});
      assertNotNull(result);
      assertTrue("Repository data is not consistent, result: " + result,
         result.startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   public void testAll() throws Exception
   {
      checkController = new TesterRepositoryCheckController(repositoryService.getRepository("db1"));

      String result =
         checkController.checkRepositoryDataConsistency(new DataStorage[]{DataStorage.DB, DataStorage.VALUE_STORAGE,
            DataStorage.LUCENE_INDEX});
      checkController.getLastLogFile().delete();

      assertNotNull(result);
      assertTrue("Repository data is not consistent, result: " + result,
         result.startsWith(RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
   }

   /**
    * Index contains documents that was already removed from DB.
    */
   public void testIndexUsecaseWrongDocumentId() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, false);

      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositorySearchIndexConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // Remove node from DB
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement(
         "ALTER TABLE JCR_" + (isMultiDb ? "M" : "S") + "ITEM DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S")
            + "ITEM_PARENT").execute();
      conn.prepareStatement(
         "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "ITEM WHERE ID = '" + (isMultiDb ? "" : wsEntry.getName())
            + node.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositorySearchIndexConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Index contains multiple documents.
    */
   public void testIndexUsecaseMultipleDocuments() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, false);

      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      String nodeIdentifier = node.getIdentifier();
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositorySearchIndexConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // Indexing one more document with same UUID
      List<SearchManager> searchManagers =
         repository.getWorkspaceContainer(repository.getConfiguration().getSystemWorkspaceName())
            .getComponentInstancesOfType(SearchManager.class);

      PlainChangesLog log = new PlainChangesLogImpl();

      NodeData data =
         new TransientNodeData(QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName("", "testNode")),
            nodeIdentifier, -1, Constants.NT_UNSTRUCTURED, null, 0, null, new AccessControlList());

      TransientPropertyData primaryType =
         new TransientPropertyData(QPath.makeChildPath(data.getQPath(), Constants.JCR_PRIMARYTYPE),
            IdGenerator.generate(), -1, PropertyType.NAME, data.getIdentifier(), false, new TransientValueData(
               Constants.NT_UNSTRUCTURED));

      log.add(new ItemState(data, ItemState.ADDED, false, null));
      log.add(new ItemState(primaryType, ItemState.ADDED, false, null));

      SearchManager sm = null;
      for (SearchManager searchManager : searchManagers)
      {
         if (!(searchManager instanceof SystemSearchManager))
         {
            sm = searchManager;
            break;
         }
      }

      assertNotNull(sm);
      sm.onSaveItems(log);

      // repository is inconsistent
      assertTrue(checkController.checkRepositorySearchIndexConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Index doesn't contain document which stored in DB.
    */
   public void testIndexUsecaseDocumentNotExists() throws Exception
   {
      ManageableRepository repository = helper.createRepository(container, false, false);

      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      String nodeIdentifier = node.getIdentifier();
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositorySearchIndexConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // Indexing one more document with same UUID
      List<SearchManager> searchManagers =
         repository.getWorkspaceContainer(repository.getConfiguration().getSystemWorkspaceName())
            .getComponentInstancesOfType(SearchManager.class);

      PlainChangesLog log = new PlainChangesLogImpl();

      NodeData data =
         new TransientNodeData(QPath.makeChildPath(Constants.ROOT_PATH, new InternalQName("", "testNode")),
            nodeIdentifier, -1, Constants.NT_UNSTRUCTURED, null, 0, null, new AccessControlList());

      TransientPropertyData primaryType =
         new TransientPropertyData(QPath.makeChildPath(data.getQPath(), Constants.JCR_PRIMARYTYPE),
            IdGenerator.generate(), -1, PropertyType.NAME, data.getIdentifier(), false, new TransientValueData(
               Constants.NT_UNSTRUCTURED));

      log.add(new ItemState(primaryType, ItemState.DELETED, false, null));
      log.add(new ItemState(data, ItemState.DELETED, false, null));

      SearchManager sm = null;
      for (SearchManager searchManager : searchManagers)
      {
         if (!(searchManager instanceof SystemSearchManager))
         {
            sm = searchManager;
            break;
         }
      }

      assertNotNull(sm);
      sm.onSaveItems(log);

      // repository is inconsistent
      assertTrue(checkController.checkRepositorySearchIndexConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    *  Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesTheParentIdIsIdOfThisNodeSingleDB() throws Exception
   {
      checkDBUsecasesTheParentIdIsIdOfThisNode(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesTheParentIdIsIdOfThisNodeMultiDB() throws Exception
   {
      checkDBUsecasesTheParentIdIsIdOfThisNode(helper.createRepository(container, true, false));
   }

   private void checkDBUsecasesTheParentIdIsIdOfThisNode(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      PropertyImpl prop = (PropertyImpl)node.setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // change ITEM table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement("DROP INDEX JCR_IDX_" + (isMultiDb ? "M" : "S") + "ITEM_PARENT").execute();
      conn.prepareStatement(
         "UPDATE JCR_" + (isMultiDb ? "M" : "S") + "ITEM SET PARENT_ID = '" + (isMultiDb ? "" : wsEntry.getName())
            + node.getInternalIdentifier() + "' WHERE ID='" + (isMultiDb ? "" : wsEntry.getName())
            + node.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    *  Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesSeveralVersionsOfSameItemSingleDB() throws Exception
   {
      checkSeveralVersionsOfSameItem(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesSeveralVersionsOfSameItemMultiDB() throws Exception
   {
      checkSeveralVersionsOfSameItem(helper.createRepository(container, true, false));
   }

   private void checkSeveralVersionsOfSameItem(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      PropertyImpl prop = (PropertyImpl)node.setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // change ITEM table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      // add another item with new persisted version
      if (isMultiDb)
      {
         String propId = IdGenerator.generate();
         conn.prepareStatement(
            "INSERT INTO JCR_MITEM VALUES ('" + propId + "','" + prop.getParentIdentifier()
               + "','[]prop',1,2,1,NULL,1,FALSE)").execute();
         conn.prepareStatement("ALTER TABLE JCR_MVALUE DROP CONSTRAINT JCR_PK_MVALUE").execute();
         conn.prepareStatement("INSERT INTO JCR_MVALUE VALUES ('100','data','1','" + propId + "',NULL)").execute();
      }
      else
      {
         String propId = wsEntry.getName() + IdGenerator.generate();
         conn.prepareStatement(
            "INSERT INTO JCR_SITEM VALUES ('" + propId + "','" + wsEntry.getName() + prop.getParentIdentifier()
               + "','[]prop',1,'" + wsEntry.getName() + "',2,1,NULL,1,FALSE)").execute();
         conn.prepareStatement("ALTER TABLE JCR_SVALUE DROP CONSTRAINT JCR_PK_SVALUE").execute();
         conn.prepareStatement("INSERT INTO JCR_SVALUE VALUES ('100','data','1','" + propId + "',NULL)").execute();
      }

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    *  Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesPropertyWithoutParentSingleDB() throws Exception
   {
      checkDBUsecasesPropertyWithoutParent(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: property doens't have have parent node.
    */
   public void testDBUsecasesPropertyWithoutParentMultiDB() throws Exception
   {
      checkDBUsecasesPropertyWithoutParent(helper.createRepository(container, true, false));
   }

   private void checkDBUsecasesPropertyWithoutParent(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      PropertyImpl prop = (PropertyImpl)node.setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // change ITEM table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();

      conn.prepareStatement(
         "ALTER TABLE JCR_" + (isMultiDb ? "M" : "S") + "ITEM DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S")
            + "ITEM_PARENT").execute();
      conn.prepareStatement(
         "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "ITEM WHERE ID = '" + (isMultiDb ? "" : wsEntry.getName())
            + node.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Usecase: Incorrect JCR_VALUE records.
    */
   public void testDBUsecasesIncorrectValueRecordsSingleDB() throws Exception
   {
      checkDBUsecasesIncorrectValueRecords(helper.createRepository(container, false, false));
      checkDBUsecasesIncorrectValueRecords2(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: Incorrect JCR_VALUE records.
    */
   public void testDBUsecasesIncorrectValueRecordsMultiDB() throws Exception
   {
      checkDBUsecasesIncorrectValueRecords(helper.createRepository(container, true, false));
      checkDBUsecasesIncorrectValueRecords2(helper.createRepository(container, true, false));
   }

   private void checkDBUsecasesIncorrectValueRecords(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      PropertyImpl prop = (PropertyImpl)session.getRootNode().addNode("testNode").setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // change VALUE table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "UPDATE JCR_" + (isMultiDb ? "M" : "S") + "VALUE SET STORAGE_DESC = 'unexisted-desc' WHERE PROPERTY_ID = '"
            + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   private void checkDBUsecasesIncorrectValueRecords2(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      PropertyImpl prop = (PropertyImpl)session.getRootNode().addNode("testNode").setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // change VALUE table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "UPDATE JCR_" + (isMultiDb ? "M" : "S") + "VALUE SET DATA = NULL WHERE PROPERTY_ID = '"
            + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Usecase: value records has no item record.
    */
   public void testDBUsecasesValueRecordHasNoItemRecordSingleDB() throws Exception
   {
      checkDBUsecasesValueRecordHasNoItemRecord(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: value records has no item record.
    */
   public void testDBUsecasesValueRecordHasNoItemRecordMultiDB() throws Exception
   {
      checkDBUsecasesValueRecordHasNoItemRecord(helper.createRepository(container, true, false));
   }

   private void checkDBUsecasesValueRecordHasNoItemRecord(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      PropertyImpl prop = (PropertyImpl)session.getRootNode().addNode("testNode").setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // remove records from item table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "ALTER TABLE JCR_" + (isMultiDb ? "M" : "S") + "VALUE DROP CONSTRAINT JCR_FK_" + (isMultiDb ? "M" : "S")
            + "VALUE_PROPERTY").execute();
      conn.prepareStatement(
         "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "ITEM WHERE ID = '" + (isMultiDb ? "" : wsEntry.getName())
            + prop.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Usecase: properties that have not value record.
    */
   public void testDBUsecasesPropertiesHasNoValueRecordSingleDB() throws Exception
   {
      checkDBUsecasesPropertiesHasNoSingleValueRecord(helper.createRepository(container, false, false));
      checkDBUsecasesPropertiesHasEmptyMultiValueRecord(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: properties that have not value record. 
    */
   public void testDBUsecasesPropertiesHasNoValueRecordMultiDB() throws Exception
   {
      checkDBUsecasesPropertiesHasNoSingleValueRecord(helper.createRepository(container, true, false));
      checkDBUsecasesPropertiesHasEmptyMultiValueRecord(helper.createRepository(container, false, false));
   }

   private void checkDBUsecasesPropertiesHasNoSingleValueRecord(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      PropertyImpl prop = (PropertyImpl)session.getRootNode().addNode("testNode").setProperty("prop", "test");
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // remove records from value table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "VALUE WHERE PROPERTY_ID = '"
            + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   private void checkDBUsecasesPropertiesHasEmptyMultiValueRecord(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      PropertyImpl prop = (PropertyImpl)session.getRootNode().addNode("testNode").setProperty("prop", new String[]{});
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Usecase: reference properties without reference records.
    */
   public void testDBUsecasesReferencePropertyWithoutReferenceRecordSingleDB() throws Exception
   {
      checkDBUsecasesReferencePropertyWithoutReferenceRecord(helper.createRepository(container, false, false));
   }

   /**
    * Usecase: reference properties without reference records.
    */
   public void testDBUsecasesReferencePropertyWithoutReferenceRecordMultiDB() throws Exception
   {
      checkDBUsecasesReferencePropertyWithoutReferenceRecord(helper.createRepository(container, true, false));
   }

   private void checkDBUsecasesReferencePropertyWithoutReferenceRecord(ManageableRepository repository)
      throws Exception
   {
      // create repository and add ref property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl refNode = (NodeImpl)session.getRootNode().addNode("refNode");
      refNode.addMixin("mix:referenceable");
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      PropertyImpl prop = (PropertyImpl)node.setProperty("refProp", refNode);
      session.save();
      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // remove records from ref table
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      conn.prepareStatement(
         "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "REF WHERE PROPERTY_ID = '"
            + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();

      conn.commit();
      conn.close();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Usecase when node doesn't have at least one property.
    */
   public void testDBUsecasesNodeHasNoPropertiesSingleDB() throws Exception
   {
      checkDBUsecasesNodeHasNoProperties(helper.createRepository(container, false, false));
      checkDBUsecasesNodeHasPrimaryTypeProperties(helper.createRepository(container, false, false));
   }

   /**
    * Usecase when node doesn't have at least one property.
    */
   public void testDBUsecasesNodeHasNoPropertiesMultiDB() throws Exception
   {
      checkDBUsecasesNodeHasNoProperties(helper.createRepository(container, true, false));
      checkDBUsecasesNodeHasPrimaryTypeProperties(helper.createRepository(container, false, false));
   }

   private void checkDBUsecasesNodeHasNoProperties(ManageableRepository repository) throws Exception
   {
      // create repository and add node
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      session.save();

      PropertyIterator iter = node.getProperties();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // remove all properties
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      while (iter.hasNext())
      {
         PropertyImpl prop = (PropertyImpl)iter.nextProperty();

         conn.prepareStatement(
            "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "VALUE WHERE PROPERTY_ID = '"
               + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();
         conn.prepareStatement(
            "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "ITEM WHERE ID = '" + (isMultiDb ? "" : wsEntry.getName())
               + prop.getInternalIdentifier() + "'").execute();
      }

      conn.commit();
      conn.close();

      assertFalse(node.getProperties().hasNext());

      session.logout();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   private void checkDBUsecasesNodeHasPrimaryTypeProperties(ManageableRepository repository) throws Exception
   {
      // create repository and add node
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      NodeImpl node = (NodeImpl)session.getRootNode().addNode("testNode");
      session.save();

      PropertyIterator iter = node.getProperties();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      WorkspaceEntry wsEntry = repository.getConfiguration().getWorkspaceEntries().get(0);
      boolean isMultiDb = wsEntry.getContainer().getParameterBoolean(JDBCWorkspaceDataContainer.MULTIDB);

      // remove all properties
      String sourceName = wsEntry.getContainer().getParameterValue(JDBCWorkspaceDataContainer.SOURCE_NAME);

      Connection conn = ((DataSource)new InitialContext().lookup(sourceName)).getConnection();
      while (iter.hasNext())
      {
         PropertyImpl prop = (PropertyImpl)iter.nextProperty();

         if (!prop.getName().equals("jcr:primaryType"))
         {
            conn.prepareStatement(
               "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "VALUE WHERE PROPERTY_ID = '"
                  + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();
            
            conn.prepareStatement(
               "DELETE FROM JCR_" + (isMultiDb ? "M" : "S") + "ITEM WHERE ID = '"
                  + (isMultiDb ? "" : wsEntry.getName()) + prop.getInternalIdentifier() + "'").execute();
         }
      }

      conn.commit();
      conn.close();

      assertTrue(node.getProperties().hasNext());

      session.logout();

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryDataBaseConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }

   /**
    * Usescase when STORAGE_DESC field in JCR_SVALUE table is not empty but there is no file in the value storage.
    */
   public void testValueStorageUsecasesSingleDb() throws Exception
   {
      checkValueStorageUsecases(helper.createRepository(container, false, false));
   }

   /**
    * Usescase when STORAGE_DESC field in JCR_MVALUE table is not empty but there is no file in the value storage.
    */
   public void testValueStorageUsecasesMultiDb() throws Exception
   {
      checkValueStorageUsecases(helper.createRepository(container, true, false));
   }

   private void checkValueStorageUsecases(ManageableRepository repository) throws Exception
   {
      // create repository and add property
      SessionImpl session =
         (SessionImpl)repository.login(credentials, repository.getConfiguration().getSystemWorkspaceName());
      InputStream in = new FileInputStream(createBLOBTempFile(300));
      PropertyImpl prop = (PropertyImpl)session.getRootNode().addNode("testNode").setProperty("testProperty", in);
      session.save();
      in.close();

      session.logout();

      // repository is consistent
      checkController = new TesterRepositoryCheckController(repository);
      assertTrue(checkController.checkRepositoryValueStorageConsistency().startsWith(
         RepositoryCheckController.REPORT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();

      // remove the file from the value storage
      String vsPath =
         repository.getConfiguration().getWorkspaceEntries().get(0).getContainer().getValueStorages().get(0)
            .getParameterValue(FileValueStorage.PATH);

      File vsFile = new File(vsPath, prop.getInternalIdentifier() + "0");
      assertTrue(vsFile.exists());
      assertTrue(vsFile.delete());

      // repository is inconsistent
      assertTrue(checkController.checkRepositoryValueStorageConsistency().startsWith(
         RepositoryCheckController.REPORT_NOT_CONSISTENT_MESSAGE));
      checkController.getLastLogFile().delete();
   }
}
