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
package org.exoplatform.services.jcr.api.importing;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.access.SystemIdentity;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.util.io.PrivilegedFileHelper;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.util.TraversingItemVisitor;
import javax.jcr.version.Version;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: $
 */
public class TestImport extends AbstractImportTest
{
   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.TestImport");

   private final Random random;

   private List<String> versionList = new ArrayList<String>();

   // private List<String> versionList = new ArrayList<String>();

   public TestImport()
   {
      super();
      random = new Random();
   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-872
    * 
    * @throws Exception
    */
   public void testAclImportDocumentView() throws Exception
   {
      AccessManager accessManager = ((SessionImpl)root.getSession()).getAccessManager();

      NodeImpl testRoot = (NodeImpl)root.addNode("TestRoot", "exo:article");

      testRoot.addMixin("exo:owneable");
      testRoot.addMixin("exo:privilegeable");
      testRoot.setProperty("exo:title", "test");

      session.save();
      assertTrue(accessManager.hasPermission(testRoot.getACL(), PermissionType.SET_PROPERTY, new Identity("exo")));

      testRoot.setPermission(testRoot.getSession().getUserID(), PermissionType.ALL);
      testRoot.setPermission("exo", new String[]{PermissionType.SET_PROPERTY});
      testRoot.removePermission(SystemIdentity.ANY);
      session.save();
      assertTrue(accessManager.hasPermission(testRoot.getACL(), PermissionType.SET_PROPERTY, new Identity("exo")));
      assertFalse(accessManager.hasPermission(testRoot.getACL(), PermissionType.READ, new Identity("exo")));

      File tmp = File.createTempFile("testAclImpormt", "tmp");
      tmp.deleteOnExit();
      serialize(testRoot, false, true, tmp);
      testRoot.remove();
      session.save();

      NodeImpl importRoot = (NodeImpl)root.addNode("ImportRoot");

      deserialize(importRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new BufferedInputStream(PrivilegedFileHelper.fileInputStream(tmp)));
      session.save();
      Node n1 = importRoot.getNode("TestRoot");
      assertTrue("Wrong ACL", accessManager.hasPermission(((NodeImpl)n1).getACL(), PermissionType.SET_PROPERTY,
         new Identity("exo")));
      assertFalse("Wrong ACL", accessManager.hasPermission(((NodeImpl)n1).getACL(), PermissionType.READ, new Identity(
         "exo")));
      importRoot.remove();
      session.save();
   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-872
    * 
    * @throws Exception
    */
   public void testAclImportSystemView() throws Exception
   {
      AccessManager accessManager = ((SessionImpl)root.getSession()).getAccessManager();

      NodeImpl testRoot = (NodeImpl)root.addNode("TestRoot", "exo:article");

      testRoot.addMixin("exo:owneable");
      testRoot.addMixin("exo:privilegeable");
      testRoot.setProperty("exo:title", "test");

      session.save();
      assertTrue(accessManager.hasPermission(testRoot.getACL(), PermissionType.SET_PROPERTY, new Identity("exo")));

      testRoot.setPermission(testRoot.getSession().getUserID(), PermissionType.ALL);
      testRoot.setPermission("exo", new String[]{PermissionType.SET_PROPERTY});
      testRoot.removePermission(SystemIdentity.ANY);
      session.save();
      assertTrue(accessManager.hasPermission(testRoot.getACL(), PermissionType.SET_PROPERTY, new Identity("exo")));
      assertFalse(accessManager.hasPermission(testRoot.getACL(), PermissionType.READ, new Identity("exo")));

      File tmp = File.createTempFile("testAclImpormt", "tmp");
      tmp.deleteOnExit();
      serialize(testRoot, true, true, tmp);
      testRoot.remove();
      session.save();

      NodeImpl importRoot = (NodeImpl)root.addNode("ImportRoot");

      deserialize(importRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new BufferedInputStream(PrivilegedFileHelper.fileInputStream(tmp)));
      session.save();
      Node n1 = importRoot.getNode("TestRoot");
      assertTrue("Wrong ACL", accessManager.hasPermission(((NodeImpl)n1).getACL(), PermissionType.SET_PROPERTY,
         new Identity("exo")));
      assertFalse("Wrong ACL", accessManager.hasPermission(((NodeImpl)n1).getACL(), PermissionType.READ, new Identity(
         "exo")));
      importRoot.remove();
      session.save();
   }

   /**
    * Test re import of versionable node. Without removing source node.
    * 
    * @throws Exception
    */
   public void testImportVersionable() throws Exception
   {
      // Create versionable node make some checkin and checkouts
      BeforeExportAction beforeExportAction = new BeforeExportAction(null, null)
      {

         private Node testRoot;

         @Override
         public void execute() throws RepositoryException
         {
            testRoot = testRootNode.addNode("testImportVersionable");
            testSession.save();
            testRoot.addMixin("mix:versionable");
            testSession.save();

            testRoot.checkin();
            testRoot.checkout();

            testRoot.addNode("node1");
            testRoot.addNode("node2").setProperty("prop1", "a property #1");
            testRoot.save();

            testRoot.checkin();
            testRoot.checkout();

            testRoot.getNode("node1").remove();
            testRoot.save();
         }

         @Override
         public Node getExportRoot()
         {
            return testRoot;
         }
      };
      // Before import remove versionable node
      BeforeImportAction beforeImportAction = new BeforeImportAction(null, null)
      {

         @Override
         public void execute() throws RepositoryException
         {
            Node testRoot2 = testRootNode.getNode("testImportVersionable");
            testRoot2.remove();
            testRootNode.save();
         }

         @Override
         public Node getImportRoot()
         {
            return testRootNode;
         }

      };

      // check correct work of imported node
      AfterImportAction afterImportAction = new AfterImportAction(null, null)
      {

         private Node testRoot2;

         @Override
         public void execute() throws RepositoryException
         {
            testRootNode.save();
            testRoot2 = testRootNode.getNode("testImportVersionable");
            assertTrue(testRoot2.isNodeType("mix:versionable"));

            testRoot2.checkin();
            testRoot2.checkout();

            testRoot2.addNode("node3");
            testRoot2.addNode("node4").setProperty("prop1", "a property #1");
            testRoot2.save();

            testRoot2.checkin();
            testRoot2.checkout();

            testRoot2.getNode("node3").remove();
            testRoot2.save();
         }

      };

      executeSingeleThreadImportTests(1, beforeExportAction.getClass(), beforeImportAction.getClass(),
         afterImportAction.getClass());

      executeMultiThreadImportTests(2, 5, beforeExportAction.getClass(), beforeImportAction.getClass(),
         afterImportAction.getClass());
   }

   /**
    * Test re import of versionable file node. With removing source node
    * 
    * @throws Exception
    */
   public void testImportVersionableFile() throws Exception
   {
      BeforeExportAction beforeExportAction = new BeforeExportAction(null, null)
      {

         @Override
         public Node getExportRoot() throws RepositoryException
         {
            Node testPdf = testRootNode.addNode("testPdf", "nt:file");
            Node contentTestPdfNode = testPdf.addNode("jcr:content", "nt:resource");

            byte[] buff = new byte[1024];
            random.nextBytes(buff);

            contentTestPdfNode.setProperty("jcr:data", new ByteArrayInputStream(buff));
            contentTestPdfNode.setProperty("jcr:mimeType", "application/octet-stream");
            contentTestPdfNode.setProperty("jcr:lastModified", session.getValueFactory().createValue(
               Calendar.getInstance()));
            testSession.save();
            testPdf.addMixin("mix:versionable");
            testSession.save();
            testPdf.checkin();
            testPdf.checkout();
            testPdf.checkin();
            return testPdf;
         }
      };
      BeforeImportAction beforeImportAction = new BeforeImportAction(null, null)
      {

         @Override
         public Node getImportRoot() throws RepositoryException
         {
            Node importRoot = testRootNode.addNode("ImportRoot");
            importRoot.addMixin("mix:versionable");
            testRootNode.save();
            return importRoot;
         }
      };

      // check correct work of imported node
      AfterImportAction afterImportAction = new AfterImportAction(null, null)
      {

         private Node testRoot2;

         @Override
         public void execute() throws RepositoryException
         {
            testRootNode.save();

            if (testRootNode.getNode("ImportRoot").hasNode("testPdf"))
               testRoot2 = testRootNode.getNode("ImportRoot").getNode("testPdf");
            else
               testRoot2 = testRootNode.getNode("testPdf");

            assertTrue(testRoot2.isNodeType("mix:versionable"));

            testRoot2.checkin();
            testRoot2.checkout();

            testRoot2.getNode("jcr:content").setProperty("jcr:lastModified",
               session.getValueFactory().createValue(Calendar.getInstance()));
            testRoot2.save();

            testRoot2.checkin();
            testRoot2.checkout();
            testRoot2.getNode("jcr:content").setProperty("jcr:lastModified",
               session.getValueFactory().createValue(Calendar.getInstance()));

            testRoot2.save();
         }
      };
      executeSingeleThreadImportTests(1, beforeExportAction.getClass(), beforeImportAction.getClass(),
         afterImportAction.getClass());

      executeMultiThreadImportTests(2, 5, beforeExportAction.getClass(), beforeImportAction.getClass(),
         afterImportAction.getClass());

   }

   /**
    * Test re import of versionable node. Without removing source node
    * 
    * @throws Exception
    */
   public void testImportVersionableNewNode() throws Exception
   {

      // Create versionable node make some checkin and checkouts
      BeforeExportAction beforeExportAction = new BeforeExportAction(null, null)
      {

         private Node testRoot;

         @Override
         public void execute() throws RepositoryException
         {
            testRoot = testRootNode.addNode("testImportVersionable");
            testRootNode.save();
            testRoot.addMixin("mix:versionable");
            testRootNode.save();

            testRoot.checkin();
            testRoot.checkout();

            testRoot.addNode("node1");
            testRoot.addNode("node2").setProperty("prop1", "a property #1");
            testRoot.save();

            testRoot.checkin();
            testRoot.checkout();

            testRoot.getNode("node1").remove();
            testRoot.save();
         }

         @Override
         public Node getExportRoot()
         {
            return testRoot;
         }
      };
      // Before import remove versionable node
      BeforeImportAction beforeImportAction = new BeforeImportAction(null, null)
      {

         @Override
         public Node getImportRoot() throws RepositoryException
         {
            Node importRoot = testRootNode.addNode("ImportRoot");
            importRoot.addMixin("mix:versionable");
            testRootNode.save();
            return importRoot;
         }

      };

      // check correct work of imported node
      AfterImportAction afterImportAction = new AfterImportAction(null, null)
      {

         private Node testRoot2;

         @Override
         public void execute() throws RepositoryException
         {
            testRootNode.save();

            if (testRootNode.getNode("ImportRoot").hasNode("testImportVersionable"))
               testRoot2 = testRootNode.getNode("ImportRoot").getNode("testImportVersionable");
            else
               testRoot2 = testRootNode.getNode("testImportVersionable");

            assertTrue(testRoot2.isNodeType("mix:versionable"));

            testRoot2.checkin();
            testRoot2.checkout();

            testRoot2.addNode("node3");
            testRoot2.addNode("node4").setProperty("prop1", "a property #1");
            testRoot2.save();

            testRoot2.checkin();
            testRoot2.checkout();

            testRoot2.getNode("node3").remove();
            testRoot2.save();
         }

      };

      executeSingeleThreadImportTests(1, beforeExportAction.getClass(), beforeImportAction.getClass(),
         afterImportAction.getClass());

      executeMultiThreadImportTests(2, 5, beforeExportAction.getClass(), beforeImportAction.getClass(),
         afterImportAction.getClass());

   }

   /**
    * Test import of the history of versuions.
    * 
    * @throws Exception
    */
   public void testImportVersionHistory() throws Exception
   {

      Node testRootNode = root.addNode("testRoot");
      Node testRoot = testRootNode.addNode("testImportVersionable");
      session.save();
      testRoot.addMixin("mix:versionable");
      testRootNode.save();

      testRoot.checkin();
      testRoot.checkout();

      testRoot.addNode("node1");
      testRoot.addNode("node2").setProperty("prop1", "a property #1");
      testRoot.save();

      testRoot.checkin();
      testRoot.checkout();

      testRoot.getNode("node1").remove();
      testRoot.save();

      assertEquals(3, testRoot.getVersionHistory().getAllVersions().getSize());

      String baseVersionUuid = testRoot.getBaseVersion().getUUID();

      Value[] values = testRoot.getProperty("jcr:predecessors").getValues();
      String[] predecessors = new String[values.length];
      for (int i = 0; i < values.length; i++)
      {
         predecessors[i] = values[i].getString();
      }
      String versionHistory = testRoot.getVersionHistory().getUUID();

      File versionableNodeContent = File.createTempFile("versionableNodeContent", "tmp");
      File vhNodeContent = File.createTempFile("vhNodeContent", "tmp");
      versionableNodeContent.deleteOnExit();
      vhNodeContent.deleteOnExit();
      serialize(testRoot, false, true, versionableNodeContent);
      serialize(testRoot.getVersionHistory(), false, true, vhNodeContent);

      testRoot.remove();
      session.save();

      deserialize(testRootNode, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new BufferedInputStream(PrivilegedFileHelper.fileInputStream(versionableNodeContent)));
      session.save();
      testRoot = testRootNode.getNode("testImportVersionable");
      assertTrue(testRoot.isNodeType("mix:versionable"));

      assertEquals(1, testRoot.getVersionHistory().getAllVersions().getSize());

      VersionHistoryImporter historyImporter =
         new VersionHistoryImporter((NodeImpl)testRoot, new BufferedInputStream(PrivilegedFileHelper
            .fileInputStream(vhNodeContent)), baseVersionUuid, predecessors, versionHistory);
      historyImporter.doImport();
      session.save();

      assertEquals(3, testRoot.getVersionHistory().getAllVersions().getSize());

      testRoot.addNode("node3");
      testRoot.addNode("node4").setProperty("prop1", "a property #1");
      testRoot.save();

      testRoot.checkin();
      testRoot.checkout();
      assertEquals(4, testRoot.getVersionHistory().getAllVersions().getSize());

   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-1247
    * 
    * @throws Exception
    */
   public void testJCR1247() throws Exception
   {

      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestJCR1247", "nt:file");
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      contentNode.setProperty("jcr:mimeType", "image/jpg");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      session.save();
      Node contentNodeBeforeAddVersion = fileNode.getNode("jcr:content");
      assertNotNull(contentNodeBeforeAddVersion.getProperty("jcr:lastModified"));
      if (fileNode.canAddMixin("mix:versionable"))
      {
         fileNode.addMixin("mix:versionable");
      }
      fileNode.save();
      fileNode.checkin();
      fileNode.checkout();
      session.save();

      String nodeDump = dumpVersionable(fileNode);
      // Export VersionHistory

      assertTrue(fileNode.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      byte[] versionableNode = serialize(fileNode, false, true);
      // version history
      byte[] versionHistory = serialize(fileNode.getVersionHistory(), false, true);
      System.out.println(new String(versionHistory));
      fileNode.remove();
      session.save();
      assertFalse(testRoot.hasNode("TestJCR1247"));

      // restore node content
      deserialize(testRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
         new ByteArrayInputStream(versionableNode));
      session.save();

      assertTrue(testRoot.hasNode("TestJCR1247"));

      Node fileImport = testRoot.getNode("TestJCR1247");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      fileImport.checkin();
      fileImport.checkout();
      session.save();
      // assertEquals(nodeDump, dumpVersionable(fileImport));

   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-1047
    * 
    * @throws Exception
    */
   public void testJCR1047_DocumentView() throws Exception
   {
      // create initial data
      Node aaa = root.addNode("AAA");

      Node hello = aaa.addNode("hello", "exo:article");
      hello.setProperty("exo:title", "hello");
      hello.addMixin("mix:versionable");

      session.save();

      // versions create
      hello.checkin();
      hello.checkout();
      session.save();

      hello.setProperty("exo:title", "hello2");
      session.save();

      hello.checkin();
      hello.checkout();
      session.save();

      String nodeDump = dumpVersionable(hello);

      // Export VersionHistory

      assertTrue(hello.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(hello);

      // node content
      byte[] versionableNode = serialize(hello, false, true);
      // version history
      byte[] versionHistory = serialize(hello.getVersionHistory(), false, true);

      aaa.remove();
      session.save();

      assertFalse(root.hasNode("AAA"));

      Node AAA = root.addNode("AAA");
      session.save();

      // restore node content
      deserialize(AAA, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
         new ByteArrayInputStream(versionableNode));
      session.save();
      assertTrue(AAA.hasNode("hello"));

      Node helloImport = AAA.getNode("hello");
      assertTrue(helloImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)helloImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();

      assertEquals(nodeDump, dumpVersionable(helloImport));

   }

   /**
    * Test for http://jira.exoplatform.org/browse/JCR-1047
    * 
    * @throws Exception
    */
   public void testJCR1047_SystemView() throws Exception
   {
      // create initial data
      Node aaa = root.addNode("AAA");

      Node hello = aaa.addNode("hello", "exo:article");
      hello.setProperty("exo:title", "hello");
      hello.addMixin("mix:versionable");

      session.save();

      // versions create
      hello.checkin();
      hello.checkout();
      session.save();

      hello.setProperty("exo:title", "hello2");
      session.save();

      hello.checkin();
      hello.checkout();
      session.save();

      String nodeDump = dumpVersionable(hello);

      // Export VersionHistory

      assertTrue(hello.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(hello);

      // node content
      byte[] versionableNode = serialize(hello, true, true);
      // version history
      byte[] versionHistory = serialize(hello.getVersionHistory(), true, true);

      aaa.remove();
      session.save();

      assertFalse(root.hasNode("AAA"));

      Node AAA = root.addNode("AAA");
      session.save();

      // restore node content
      deserialize(AAA, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW,
         new ByteArrayInputStream(versionableNode));
      session.save();
      assertTrue(AAA.hasNode("hello"));

      Node helloImport = AAA.getNode("hello");
      assertTrue(helloImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)helloImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();

      assertEquals(nodeDump, dumpVersionable(helloImport));

   }

   public void testPermissionAfterImport() throws Exception
   {
      Session session1 = repository.login(new CredentialsImpl("root", "exo".toCharArray()));
      InputStream importStream = BaseStandaloneTest.class.getResourceAsStream("/import-export/testPermdocview.xml");
      session1.importXML("/", importStream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      session1.save();
      // After import
      ExtendedNode testNode = (ExtendedNode)session1.getItem("/a");
      List<AccessControlEntry> permsList = testNode.getACL().getPermissionEntries();
      int permsListTotal = 0;
      for (AccessControlEntry ace : permsList)
      {
         String id = ace.getIdentity();
         String permission = ace.getPermission();
         if (id.equals("*:/platform/administrators") || id.equals("root"))
         {
            assertTrue(permission.equals(PermissionType.READ) || permission.equals(PermissionType.REMOVE)
               || permission.equals(PermissionType.SET_PROPERTY) || permission.equals(PermissionType.ADD_NODE));
            permsListTotal++;
         }
         else if (id.equals("validator:/platform/users"))
         {
            assertTrue(permission.equals(PermissionType.READ) || permission.equals(PermissionType.SET_PROPERTY));
            permsListTotal++;
         }
      }
      assertEquals(10, permsListTotal);
      testNode.remove();
      session1.save();
   }

   public void testImportAndExport() throws Exception
   {
      Node aaa = root.addNode("AAA");
      Node bbb = root.addNode("BBB");
      Node ccc = root.addNode("CCC");
      session.save();

      // Export Action
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      aaa.getSession().exportDocumentView(aaa.getPath(), bos, false, false);
      ByteArrayInputStream is = new ByteArrayInputStream(bos.toByteArray());
      bbb.getSession().importXML(bbb.getPath(), is, 1);
      session.save();

      String[] paths = new String[]{"AAA", "BBB", "BBB/AAA"};
      for (String path : paths)
      {
         if (bbb.hasNode(path))
         {
         }
      }

      Node hello = aaa.addNode("hello", "exo:article");
      hello.setProperty("exo:title", "hello");
      hello.addMixin("mix:versionable");
      session.save();

      Version version = hello.checkin();
      hello.checkout();

      session.save();

      /**
       * Before import this node has one version
       */
      Version rootVersion = hello.getVersionHistory().getRootVersion();
      getListVersion(rootVersion);
      assertEquals(1, versionList.size());

      // Export VersionHistory
      InputStream inputVersion = null;
      if (hello.isNodeType("mix:versionable"))
      {
         ByteArrayOutputStream bosVersion = new ByteArrayOutputStream();
         hello.getSession().exportDocumentView(hello.getVersionHistory().getPath(), bosVersion, false, false);
         inputVersion = new ByteArrayInputStream(bosVersion.toByteArray());
      }
      String versionHistory = hello.getProperty("jcr:versionHistory").getValue().getString();
      String baseVersion = hello.getProperty("jcr:baseVersion").getValue().getString();
      Value[] jcrPredecessors = hello.getProperty("jcr:predecessors").getValues();
      String[] predecessorsHistory;
      StringBuilder jcrPredecessorsBuilder = new StringBuilder();
      for (Value value : jcrPredecessors)
      {
         if (jcrPredecessorsBuilder.length() > 0)
            jcrPredecessorsBuilder.append(",");
         jcrPredecessorsBuilder.append(value.getString());
      }
      if (jcrPredecessorsBuilder.toString().indexOf(",") > -1)
      {
         predecessorsHistory = jcrPredecessorsBuilder.toString().split(",");
      }
      else
      {
         predecessorsHistory = new String[]{jcrPredecessorsBuilder.toString()};
      }

      // Export Action
      ByteArrayOutputStream bosHello = new ByteArrayOutputStream();
      hello.getSession().exportDocumentView(hello.getPath(), bosHello, false, false);
      ByteArrayInputStream isHello = new ByteArrayInputStream(bosHello.toByteArray());
      // Remove node "/aaa/hello" and it's version history, before importing it
      // again!
      // hello.remove();
      session.save();
      ccc.getSession().importXML(ccc.getPath(), isHello, 1);
      session.save();

      /**
       * Import VersionHistory After import version history, the node has no
       * version Errors: Lose version when import version history
       */
      Node helloImport = (Node)session.getItem("/CCC/hello");
      importHistory((NodeImpl)helloImport, inputVersion, baseVersion, predecessorsHistory, versionHistory);
      // versionList.clear();
      // Version rootVersionImport =
      // helloImport.getVersionHistory().getRootVersion();
      // getListVersion(rootVersionImport);
      // assertEquals(1, versionList.size());

      aaa = root.getNode("AAA");
      bbb = root.getNode("BBB");
      ccc = root.getNode("CCC");

      aaa.remove();
      bbb.remove();
      ccc.remove();
      session.save();
   }

   private String dumpVersionable(Node versionableNode) throws RepositoryException
   {
      String result;
      NodeAndValueDumpVisitor dumpVisitor = new NodeAndValueDumpVisitor();
      dumpVisitor.visit(versionableNode);
      result = dumpVisitor.getDump();
      dumpVisitor = new NodeAndValueDumpVisitor();
      dumpVisitor.visit(versionableNode.getVersionHistory());
      result += "\n" + dumpVisitor.getDump();
      return result;
   }

   private class NodeAndValueDumpVisitor extends TraversingItemVisitor
   {
      protected String dumpStr = "";

      @Override
      protected void entering(Node node, int level) throws RepositoryException
      {
         dumpStr += node.getPath() + "\n";
      }

      @Override
      protected void leaving(Property property, int level) throws RepositoryException
      {
      }

      @Override
      protected void leaving(Node node, int level) throws RepositoryException
      {
      }

      public String getDump()
      {
         return dumpStr;
      }

      /**
       * {@inheritDoc}
       */
      @Override
      protected void entering(Property property, int level) throws RepositoryException
      {
         dumpStr += " " + property.getPath() + "=" + valToString(property) + " \n";
      }

      private String valToString(Property property) throws ValueFormatException, IllegalStateException,
         RepositoryException
      {
         String prResult = "";
         if (property.getDefinition().isMultiple())
         {
            if (property.getValues().length < 2)
            {
               prResult += property.getValues()[0].getString();
            }
            else
            {
               prResult = property.getValues()[0].getString();
               for (int i = 1; i < property.getValues().length; i++)
               {
                  prResult += " " + property.getValues()[i].getString();
               }
            }
         }
         else
         {
            prResult += property.getValue().getString();
         }
         return prResult;
      }

   }

   private class VersionableNodeInfo
   {
      private final String versionHistory;

      private final String baseVersion;

      private final String[] predecessorsHistory;

      /**
       * @return the versionHistory
       */
      public String getVersionHistory()
      {
         return versionHistory;
      }

      /**
       * @return the baseVersion
       */
      public String getBaseVersion()
      {
         return baseVersion;
      }

      /**
       * @return the predecessorsHistory
       */
      public String[] getPredecessorsHistory()
      {
         return predecessorsHistory;
      }

      /**
       * @throws RepositoryException
       */
      public VersionableNodeInfo(Node versionableNode) throws RepositoryException
      {
         super();
         assertTrue(versionableNode.isNodeType("mix:versionable"));
         this.versionHistory = versionableNode.getProperty("jcr:versionHistory").getValue().getString();
         this.baseVersion = versionableNode.getProperty("jcr:baseVersion").getValue().getString();
         Value[] jcrPredecessors = versionableNode.getProperty("jcr:predecessors").getValues();

         this.predecessorsHistory = new String[jcrPredecessors.length];
         for (int i = 0; i < jcrPredecessors.length; i++)
         {
            this.predecessorsHistory[i] = jcrPredecessors[i].getString();
         }

      }
   }

   private void importHistory(NodeImpl versionableNode, InputStream versionHistoryStream, String baseVersionUuid,
      String[] predecessors, String versionHistory) throws RepositoryException, IOException
   {
      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter(versionableNode, versionHistoryStream, baseVersionUuid, predecessors,
            versionHistory);
      versionHistoryImporter.doImport();
   }

   public void getListVersion(Version version)
   {
      try
      {
         String uuid = version.getUUID();
         QueryManager queryManager = session.getWorkspace().getQueryManager();
         Query query =
            queryManager.createQuery("//element(*, nt:version)[@jcr:predecessors='" + uuid + "']", Query.XPATH);
         QueryResult queryResult = query.execute();
         NodeIterator iterate = queryResult.getNodes();
         while (iterate.hasNext())
         {
            Version version1 = (Version)iterate.nextNode();
            versionList.add(version1.getUUID());
            getListVersion(version1);
         }
      }
      catch (Exception e)
      {
      }
   }
}
