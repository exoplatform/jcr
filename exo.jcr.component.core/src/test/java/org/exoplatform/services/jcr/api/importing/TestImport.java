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

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessManager;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.util.VersionHistoryImporter;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyType;
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
      testRoot.removePermission(IdentityConstants.ANY);
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
         new BufferedInputStream(new FileInputStream(tmp)));
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
      testRoot.removePermission(IdentityConstants.ANY);
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
         new BufferedInputStream(new FileInputStream(tmp)));
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

         public Node getExportRoot()
         {
            return testRoot;
         }
      };
      // Before import remove versionable node
      BeforeImportAction beforeImportAction = new BeforeImportAction(null, null)
      {

         public void execute() throws RepositoryException
         {
            Node testRoot2 = testRootNode.getNode("testImportVersionable");
            testRoot2.remove();
            testRootNode.save();
         }

         public Node getImportRoot()
         {
            return testRootNode;
         }

      };

      // check correct work of imported node
      AfterImportAction afterImportAction = new AfterImportAction(null, null)
      {

         private Node testRoot2;

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

         public Node getExportRoot()
         {
            return testRoot;
         }
      };
      // Before import remove versionable node
      BeforeImportAction beforeImportAction = new BeforeImportAction(null, null)
      {

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
         new BufferedInputStream(new FileInputStream(versionableNodeContent)));
      session.save();
      testRoot = testRootNode.getNode("testImportVersionable");
      assertTrue(testRoot.isNodeType("mix:versionable"));

      assertEquals(1, testRoot.getVersionHistory().getAllVersions().getSize());

      VersionHistoryImporter historyImporter =
         new VersionHistoryImporter((NodeImpl)testRoot, new BufferedInputStream(new FileInputStream(vhNodeContent)),
            baseVersionUuid, predecessors, versionHistory);
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
      //System.out.println(new String(versionHistory));
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

      protected void entering(Node node, int level) throws RepositoryException
      {
         dumpStr += node.getPath() + "\n";
      }

      protected void leaving(Property property, int level) throws RepositoryException
      {
      }

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
   

   /**
    * https://jira.jboss.org/browse/EXOJCR-865
    * 
    * @throws Exception
    */
   public void testEXOJCR865_Doc() throws Exception
   {

      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestEXOJCR865", "nt:file");
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      contentNode.setProperty("jcr:mimeType", "image/jpg");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node contentNodeBeforeAddVersion = fileNode.getNode("jcr:content");
      assertNotNull(contentNodeBeforeAddVersion.getProperty("jcr:lastModified"));
      if (fileNode.canAddMixin("mix:versionable"))
      {
         fileNode.addMixin("mix:versionable");
      }
      fileNode.save();
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();

      String nodeDump = dumpVersionable(fileNode);
      // Export VersionHistory

      assertTrue(fileNode.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      byte[] versionableNode = serialize(fileNode, false, true);
      // version history
      byte[] versionHistory = serialize(fileNode.getVersionHistory(), false, true);
      //System.out.println(new String(versionHistory));
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRoot");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();

      assertTrue(restoreRoot.hasNode("TestEXOJCR865"));

      Node fileImport = restoreRoot.getNode("TestEXOJCR865");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      Property property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("3", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   /**
    * https://jira.jboss.org/browse/EXOJCR-865
    * 
    * @throws Exception
    */
   public void testEXOJCR865_Sys() throws Exception
   {

      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestEXOJCR865", "nt:file");
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      contentNode.setProperty("jcr:mimeType", "image/jpg");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node contentNodeBeforeAddVersion = fileNode.getNode("jcr:content");
      assertNotNull(contentNodeBeforeAddVersion.getProperty("jcr:lastModified"));
      if (fileNode.canAddMixin("mix:versionable"))
      {
         fileNode.addMixin("mix:versionable");
      }
      fileNode.save();
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();

      String nodeDump = dumpVersionable(fileNode);
      // Export VersionHistory

      assertTrue(fileNode.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      byte[] versionableNode = serialize(fileNode, true, true);
      // version history
      byte[] versionHistory = serialize(fileNode.getVersionHistory(), true, true);
      //System.out.println(new String(versionHistory));
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRoot");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();

      assertTrue(restoreRoot.hasNode("TestEXOJCR865"));

      Node fileImport = restoreRoot.getNode("TestEXOJCR865");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      Property property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("3", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   public void testEXOJCR865_Doc_exo_webContent_1() throws Exception
   {
      // node content
      Node testRoot = root.addNode("testRoot");
      root.save();
      
      Node web = testRoot.addNode("web", "exo:webContent");
      
      web.addMixin("exo:datetime");
      web.addMixin("exo:owneable");
      web.addMixin("exo:modify");
      web.addMixin("mix:votable");
      web.addMixin("mix:commentable");
      web.addMixin("publication:stateAndVersionBasedPublication");
      web.addMixin("mix:versionable");
      
      web.setProperty("exo:dateCreated", Calendar.getInstance());
      web.setProperty("exo:dateModified", Calendar.getInstance());
      web.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      web.setProperty("exo:lastModifier", "root");
      web.setProperty("exo:summary", "text summary");
      web.setProperty("exo:title", "web title");
      web.setProperty("exo:voteTotal", "1");
      web.setProperty("exo:voteTotalOfLang", "1");
      web.setProperty("exo:votingRate", "1");
      web.setProperty("publication:currentState", "draft");
      web.setProperty("publication:history", new String[] {"13","12", "14"});
      web.setProperty("publication:lifecycleName", "lf_name");
      web.setProperty("publication:revisionData", new String[] {"r_data_1", "r_data_2"});
      
      Node defHtml = web.addNode("default.html", "nt:file");
      defHtml.addMixin("exo:datetime");
      defHtml.addMixin("exo:owneable");
      defHtml.addMixin("exo:modify");
      defHtml.addMixin("exo:htmlFile");
      
      defHtml.setProperty("exo:dateCreated", Calendar.getInstance());
      defHtml.setProperty("exo:dateModified", Calendar.getInstance());
      defHtml.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defHtml.setProperty("exo:lastModifier", "root");
      
      Node contentDefHtml = defHtml.addNode("jcr:content", "nt:resource");
      contentDefHtml.addMixin("exo:datetime");
      contentDefHtml.addMixin("exo:owneable");
      contentDefHtml.addMixin("dc:elementSet");
      
      contentDefHtml.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefHtml.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:data", "def_html_data");
      contentDefHtml.setProperty("jcr:encoding", "UTF-8");
      contentDefHtml.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:mimeType", "text/html");
      
      Node css = web.addNode("css", "exo:cssFolder");
      css.addMixin("exo:datetime");
      css.addMixin("exo:owneable");
      css.addMixin("exo:modify");
      
      css.setProperty("exo:dateCreated", Calendar.getInstance());
      css.setProperty("exo:dateModified", Calendar.getInstance());
      css.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      css.setProperty("exo:lastModifier", "root");
      
      Node defCss = css.addNode("default.css", "nt:file");
      
      defCss.addMixin("exo:datetime");
      defCss.addMixin("exo:owneable");
      defCss.addMixin("exo:modify");
      
      defCss.setProperty("exo:dateCreated", Calendar.getInstance());
      defCss.setProperty("exo:dateModified", Calendar.getInstance());
      defCss.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defCss.setProperty("exo:lastModifier", "root");
      
      Node contentDefCss = defCss.addNode("jcr:content", "nt:resource");
      contentDefCss.addMixin("exo:datetime");
      contentDefCss.addMixin("exo:owneable");
      contentDefCss.addMixin("dc:elementSet");
      
      contentDefCss.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefCss.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefCss.setProperty("jcr:data", "def_css_data");
      contentDefCss.setProperty("jcr:encoding", "UTF-8");
      contentDefCss.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefCss.setProperty("jcr:mimeType", "text/css");
      
      Node medias = web.addNode("medias", "exo:multimediaFolder");
      medias.addMixin("exo:datetime");
      medias.addMixin("exo:owneable");
      medias.addMixin("exo:modify");
      
      medias.setProperty("exo:dateCreated", Calendar.getInstance());
      medias.setProperty("exo:dateModified", Calendar.getInstance());
      medias.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      medias.setProperty("exo:lastModifier", "root");
      
      Node videos = medias.addNode("videos", "nt:folder");
      videos.addMixin("exo:datetime");
      videos.addMixin("exo:owneable");
      videos.addMixin("exo:modify");
      
      videos.setProperty("exo:dateCreated", Calendar.getInstance());
      videos.setProperty("exo:dateModified", Calendar.getInstance());
      videos.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      videos.setProperty("exo:lastModifier", "root");
      
      Node images = medias.addNode("images", "nt:folder");
      images.addMixin("exo:datetime");
      images.addMixin("exo:owneable");
      images.addMixin("exo:modify");
      
      images.setProperty("exo:dateCreated", Calendar.getInstance());
      images.setProperty("exo:dateModified", Calendar.getInstance());
      images.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      images.setProperty("exo:lastModifier", "root");
      
      Node illustration= images.addNode("illustration", "nt:file");
      illustration.addMixin("exo:datetime");
      illustration.addMixin("exo:owneable");
      illustration.addMixin("exo:modify");
      illustration.addMixin("mix:referenceable");
      
      illustration.setProperty("exo:dateCreated", Calendar.getInstance());
      illustration.setProperty("exo:dateModified", Calendar.getInstance());
      illustration.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      illustration.setProperty("exo:lastModifier", "root");
      
      Node contentIllustration = illustration.addNode("jcr:content", "nt:resource");
      contentIllustration.addMixin("exo:datetime");
      contentIllustration.addMixin("exo:owneable");
      contentIllustration.addMixin("dc:elementSet");
      
      contentIllustration.setProperty("exo:dateCreated", Calendar.getInstance());
      contentIllustration.setProperty("exo:dateModified", Calendar.getInstance());
      contentIllustration.setProperty("jcr:data", "illustration_data");
      contentIllustration.setProperty("jcr:encoding", "UTF-8");
      contentIllustration.setProperty("jcr:lastModified", Calendar.getInstance());
      contentIllustration.setProperty("jcr:mimeType", "text/jpeg");
      
      Node documents = web.addNode("documents", "nt:unstructured");
      documents.addMixin("exo:datetime");
      documents.addMixin("exo:owneable");
      documents.addMixin("exo:modify");
      documents.addMixin("exo:documentFolder");
      
      documents.setProperty("exo:dateCreated", Calendar.getInstance());
      documents.setProperty("exo:dateModified", Calendar.getInstance());
      documents.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      documents.setProperty("exo:lastModifier", "root");
      
      Node js = web.addNode("js", "exo:jsFolder");
      js.addMixin("exo:datetime");
      js.addMixin("exo:owneable");
      js.addMixin("exo:modify");
      
      js.setProperty("exo:dateCreated", Calendar.getInstance());
      js.setProperty("exo:dateModified", Calendar.getInstance());
      js.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      js.setProperty("exo:lastModifier", "root");
      
      Node defJs = js.addNode("default.js", "nt:file");
      defJs.addMixin("exo:datetime");
      defJs.addMixin("exo:owneable");
      defJs.addMixin("exo:modify");
      
      defJs.setProperty("exo:dateCreated", Calendar.getInstance());
      defJs.setProperty("exo:dateModified", Calendar.getInstance());
      defJs.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defJs.setProperty("exo:lastModifier", "root");
      
      Node contentDefJs = defJs.addNode("jcr:content", "nt:resource");
      contentDefJs.addMixin("exo:datetime");
      contentDefJs.addMixin("exo:owneable");
      contentDefJs.addMixin("dc:elementSet");
      
      contentDefJs.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefJs.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefJs.setProperty("jcr:data", "def_js_data");
      contentDefJs.setProperty("jcr:encoding", "UTF-8");
      contentDefJs.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefJs.setProperty("jcr:mimeType", "text/js");
      
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(testRoot.getNode("web"));
      
      // node content
      byte[] versionableNode = serialize(web, false, true);
      // version history
      byte[] versionHistory = serialize(web.getVersionHistory(), false, true);
      //System.out.println(new String(versionHistory));
      
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRootWeb");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();
      
      assertTrue(restoreRoot.hasNode("web"));
      
      Node fileImport = restoreRoot.getNode("web");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      
      Property property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("1", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   public void testEXOJCR865_Sys_exo_webContent_1() throws Exception
   {
      // node content
      Node testRoot = root.addNode("testRoot");
      root.save();
      
      Node web = testRoot.addNode("web", "exo:webContent");
      
      web.addMixin("exo:datetime");
      web.addMixin("exo:owneable");
      web.addMixin("exo:modify");
      web.addMixin("mix:votable");
      web.addMixin("mix:commentable");
      web.addMixin("publication:stateAndVersionBasedPublication");
      web.addMixin("mix:versionable");
      
      web.setProperty("exo:dateCreated", Calendar.getInstance());
      web.setProperty("exo:dateModified", Calendar.getInstance());
      web.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      web.setProperty("exo:lastModifier", "root");
      web.setProperty("exo:summary", "text summary");
      web.setProperty("exo:title", "web title");
      web.setProperty("exo:voteTotal", "1");
      web.setProperty("exo:voteTotalOfLang", "1");
      web.setProperty("exo:votingRate", "1");
      web.setProperty("publication:currentState", "draft");
      web.setProperty("publication:history", new String[] {"13","12", "14"});
      web.setProperty("publication:lifecycleName", "lf_name");
      web.setProperty("publication:revisionData", new String[] {"r_data_1", "r_data_2"});
      
      Node defHtml = web.addNode("default.html", "nt:file");
      defHtml.addMixin("exo:datetime");
      defHtml.addMixin("exo:owneable");
      defHtml.addMixin("exo:modify");
      defHtml.addMixin("exo:htmlFile");
      
      defHtml.setProperty("exo:dateCreated", Calendar.getInstance());
      defHtml.setProperty("exo:dateModified", Calendar.getInstance());
      defHtml.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defHtml.setProperty("exo:lastModifier", "root");
      
      Node contentDefHtml = defHtml.addNode("jcr:content", "nt:resource");
      contentDefHtml.addMixin("exo:datetime");
      contentDefHtml.addMixin("exo:owneable");
      contentDefHtml.addMixin("dc:elementSet");
      
      contentDefHtml.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefHtml.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:data", "def_html_data");
      contentDefHtml.setProperty("jcr:encoding", "UTF-8");
      contentDefHtml.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:mimeType", "text/html");
      
      Node css = web.addNode("css", "exo:cssFolder");
      css.addMixin("exo:datetime");
      css.addMixin("exo:owneable");
      css.addMixin("exo:modify");
      
      css.setProperty("exo:dateCreated", Calendar.getInstance());
      css.setProperty("exo:dateModified", Calendar.getInstance());
      css.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      css.setProperty("exo:lastModifier", "root");
      
      Node defCss = css.addNode("default.css", "nt:file");
      
      defCss.addMixin("exo:datetime");
      defCss.addMixin("exo:owneable");
      defCss.addMixin("exo:modify");
      
      defCss.setProperty("exo:dateCreated", Calendar.getInstance());
      defCss.setProperty("exo:dateModified", Calendar.getInstance());
      defCss.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defCss.setProperty("exo:lastModifier", "root");
      
      Node contentDefCss = defCss.addNode("jcr:content", "nt:resource");
      contentDefCss.addMixin("exo:datetime");
      contentDefCss.addMixin("exo:owneable");
      contentDefCss.addMixin("dc:elementSet");
      
      contentDefCss.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefCss.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefCss.setProperty("jcr:data", "def_css_data");
      contentDefCss.setProperty("jcr:encoding", "UTF-8");
      contentDefCss.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefCss.setProperty("jcr:mimeType", "text/css");
      
      Node medias = web.addNode("medias", "exo:multimediaFolder");
      medias.addMixin("exo:datetime");
      medias.addMixin("exo:owneable");
      medias.addMixin("exo:modify");
      
      medias.setProperty("exo:dateCreated", Calendar.getInstance());
      medias.setProperty("exo:dateModified", Calendar.getInstance());
      medias.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      medias.setProperty("exo:lastModifier", "root");
      
      Node videos = medias.addNode("videos", "nt:folder");
      videos.addMixin("exo:datetime");
      videos.addMixin("exo:owneable");
      videos.addMixin("exo:modify");
      
      videos.setProperty("exo:dateCreated", Calendar.getInstance());
      videos.setProperty("exo:dateModified", Calendar.getInstance());
      videos.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      videos.setProperty("exo:lastModifier", "root");
      
      Node images = medias.addNode("images", "nt:folder");
      images.addMixin("exo:datetime");
      images.addMixin("exo:owneable");
      images.addMixin("exo:modify");
      
      images.setProperty("exo:dateCreated", Calendar.getInstance());
      images.setProperty("exo:dateModified", Calendar.getInstance());
      images.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      images.setProperty("exo:lastModifier", "root");
      
      Node illustration= images.addNode("illustration", "nt:file");
      illustration.addMixin("exo:datetime");
      illustration.addMixin("exo:owneable");
      illustration.addMixin("exo:modify");
      illustration.addMixin("mix:referenceable");
      
      illustration.setProperty("exo:dateCreated", Calendar.getInstance());
      illustration.setProperty("exo:dateModified", Calendar.getInstance());
      illustration.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      illustration.setProperty("exo:lastModifier", "root");
      
      Node contentIllustration = illustration.addNode("jcr:content", "nt:resource");
      contentIllustration.addMixin("exo:datetime");
      contentIllustration.addMixin("exo:owneable");
      contentIllustration.addMixin("dc:elementSet");
      
      contentIllustration.setProperty("exo:dateCreated", Calendar.getInstance());
      contentIllustration.setProperty("exo:dateModified", Calendar.getInstance());
      contentIllustration.setProperty("jcr:data", "illustration_data");
      contentIllustration.setProperty("jcr:encoding", "UTF-8");
      contentIllustration.setProperty("jcr:lastModified", Calendar.getInstance());
      contentIllustration.setProperty("jcr:mimeType", "text/jpeg");
      
      Node documents = web.addNode("documents", "nt:unstructured");
      documents.addMixin("exo:datetime");
      documents.addMixin("exo:owneable");
      documents.addMixin("exo:modify");
      documents.addMixin("exo:documentFolder");
      
      documents.setProperty("exo:dateCreated", Calendar.getInstance());
      documents.setProperty("exo:dateModified", Calendar.getInstance());
      documents.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      documents.setProperty("exo:lastModifier", "root");
      
      Node js = web.addNode("js", "exo:jsFolder");
      js.addMixin("exo:datetime");
      js.addMixin("exo:owneable");
      js.addMixin("exo:modify");
      
      js.setProperty("exo:dateCreated", Calendar.getInstance());
      js.setProperty("exo:dateModified", Calendar.getInstance());
      js.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      js.setProperty("exo:lastModifier", "root");
      
      Node defJs = js.addNode("default.js", "nt:file");
      defJs.addMixin("exo:datetime");
      defJs.addMixin("exo:owneable");
      defJs.addMixin("exo:modify");
      
      defJs.setProperty("exo:dateCreated", Calendar.getInstance());
      defJs.setProperty("exo:dateModified", Calendar.getInstance());
      defJs.setProperty("exo:lastModifiedDate", Calendar.getInstance());
      defJs.setProperty("exo:lastModifier", "root");
      
      Node contentDefJs = defJs.addNode("jcr:content", "nt:resource");
      contentDefJs.addMixin("exo:datetime");
      contentDefJs.addMixin("exo:owneable");
      contentDefJs.addMixin("dc:elementSet");
      
      contentDefJs.setProperty("exo:dateCreated", Calendar.getInstance());
      contentDefJs.setProperty("exo:dateModified", Calendar.getInstance());
      contentDefJs.setProperty("jcr:data", "def_js_data");
      contentDefJs.setProperty("jcr:encoding", "UTF-8");
      contentDefJs.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefJs.setProperty("jcr:mimeType", "text/js");
      
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(testRoot.getNode("web"));
      
      // node content
      byte[] versionableNode = serialize(web, true, true);
      // version history
      byte[] versionHistory = serialize(web.getVersionHistory(), true, true);
      //System.out.println(new String(versionHistory));
      
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRootWeb");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();
      
      assertTrue(restoreRoot.hasNode("web"));
      
      Node fileImport = restoreRoot.getNode("web");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      
      Property property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("1", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   public void testEXOJCR865_Doc_exo_webContent_2() throws Exception
   {
      // node content
      Node testRoot = root.addNode("testRoot");
      root.save();
      
      Node web = testRoot.addNode("web", "exo:webContent");
      
      web.addMixin("mix:versionable");
      
      web.setProperty("exo:summary", "text summary");
      web.setProperty("exo:title", "web title");
      
      Node defHtml = web.addNode("default.html", "nt:file");
      defHtml.addMixin("mix:referenceable");
      
      Node contentDefHtml = defHtml.addNode("jcr:content", "nt:resource");

      contentDefHtml.setProperty("jcr:data", "def_html_data");
      contentDefHtml.setProperty("jcr:encoding", "UTF-8");
      contentDefHtml.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:mimeType", "text/html");
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      web.restore("1", true);
      root.save();
      
      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(testRoot.getNode("web"));
      
      // node content
      byte[] versionableNode = serialize(web, false, true);
      // version history
      byte[] versionHistory = serialize(web.getVersionHistory(), false, true);
      //System.out.println(new String(versionHistory));
      
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRootWeb");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();
      
      assertTrue(restoreRoot.hasNode("web"));
      
      Node fileImport = restoreRoot.getNode("web");
      Node dHtml = fileImport.getNode("default.html"); fileImport.getNode("default.html").getProperty("jcr:uuid").getString();
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      Property property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("1", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   public void testEXOJCR865_Sys_exo_webContent_2() throws Exception
   {
      // node content
      Node testRoot = root.addNode("testRoot");
      root.save();
      
      Node web = testRoot.addNode("web", "exo:webContent");
      
      web.addMixin("mix:versionable");
      
      web.setProperty("exo:summary", "text summary");
      web.setProperty("exo:title", "web title");
      
      Node defHtml = web.addNode("default.html", "nt:file");
      defHtml.addMixin("mix:referenceable");
      
      Node contentDefHtml = defHtml.addNode("jcr:content", "nt:resource");

      contentDefHtml.setProperty("jcr:data", "def_html_data");
      contentDefHtml.setProperty("jcr:encoding", "UTF-8");
      contentDefHtml.setProperty("jcr:lastModified", Calendar.getInstance());
      contentDefHtml.setProperty("jcr:mimeType", "text/html");
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      web.checkin();
      web.checkout();
      root.save();
      
      web.restore("1", true);
      root.save();
      
      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(testRoot.getNode("web"));
      
      // node content
      byte[] versionableNode = serialize(web, true, true);
      // version history
      byte[] versionHistory = serialize(web.getVersionHistory(), true, true);
      //System.out.println(new String(versionHistory));
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRootWeb");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();
      
      assertTrue(restoreRoot.hasNode("web"));
      
      Node fileImport = restoreRoot.getNode("web");
      Node dHtml = fileImport.getNode("default.html"); fileImport.getNode("default.html").getProperty("jcr:uuid").getString();
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      
      Property property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("1", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   /**
    * https://jira.jboss.org/browse/EXOJCR-865
    * 
    * @throws Exception
    */
   public void testEXOJCR865_Doc_exo_links() throws Exception
   {

      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestEXOJCR865_exo_links", "nt:file");
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      contentNode.setProperty("jcr:mimeType", "image/jpg");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node contentNodeBeforeAddVersion = fileNode.getNode("jcr:content");
      assertNotNull(contentNodeBeforeAddVersion.getProperty("jcr:lastModified"));
      if (fileNode.canAddMixin("mix:versionable"))
      {
         fileNode.addMixin("mix:versionable");
      }
      
      fileNode.addMixin("exo:linkable");
      
      fileNode.setProperty("exo:links", new String[] {"1"});
      
      fileNode.save();
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();

      String nodeDump = dumpVersionable(fileNode);
      // Export VersionHistory

      assertTrue(fileNode.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      byte[] versionableNode = serialize(fileNode, false, true);
      // version history
      byte[] versionHistory = serialize(fileNode.getVersionHistory(), false, true);
      //System.out.println(new String(versionHistory));
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRoot");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();

      assertTrue(restoreRoot.hasNode("TestEXOJCR865_exo_links"));

      Node fileImport = restoreRoot.getNode("TestEXOJCR865_exo_links");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      Property property = fileImport.getProperty("exo:links");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("2", true);
      root.save();
      
      property = fileImport.getProperty("exo:links");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   /**
    * https://jira.jboss.org/browse/EXOJCR-865
    * 
    * @throws Exception
    */
   public void testEXOJCR865_Sys_exo_links() throws Exception
   {

      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestEXOJCR865_exo_links", "nt:file");
      Node contentNode = fileNode.addNode("jcr:content", "nt:resource");
      contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
      contentNode.setProperty("jcr:mimeType", "image/jpg");
      contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
      root.save();
      Node contentNodeBeforeAddVersion = fileNode.getNode("jcr:content");
      assertNotNull(contentNodeBeforeAddVersion.getProperty("jcr:lastModified"));
      if (fileNode.canAddMixin("mix:versionable"))
      {
         fileNode.addMixin("mix:versionable");
      }
      
      fileNode.addMixin("exo:linkable");
      
      fileNode.setProperty("exo:links", new String[] {"1"});
      
      fileNode.save();
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();

      String nodeDump = dumpVersionable(fileNode);
      // Export VersionHistory

      assertTrue(fileNode.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      byte[] versionableNode = serialize(fileNode, true, true);
      // version history
      byte[] versionHistory = serialize(fileNode.getVersionHistory(), true, true);
      //System.out.println(new String(versionHistory));
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRoot");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();

      assertTrue(restoreRoot.hasNode("TestEXOJCR865_exo_links"));

      Node fileImport = restoreRoot.getNode("TestEXOJCR865_exo_links");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      Property property = fileImport.getProperty("exo:links");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.restore("2", true);
      root.save();
      
      property = fileImport.getProperty("jcr:predecessors");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      property = fileImport.getProperty("exo:links");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }
   
   /**
    * https://jira.jboss.org/browse/EXOJCR-933
    * 
    * @throws Exception
    */
   public void testEXOJCR933_Doc_exo_datetime() throws Exception
   {

      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestEXOJCR933_exo_datetime");
      fileNode.setProperty("exo:datetime", Calendar.getInstance());
      
      if (fileNode.canAddMixin("mix:versionable"))
      {
         fileNode.addMixin("mix:versionable");
      }
      
      fileNode.addMixin("exo:datetime");
      
      fileNode.setProperty("exo:dateCreated", Calendar.getInstance());
      fileNode.setProperty("exo:dateModified", Calendar.getInstance());
      
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();

      String nodeDump = dumpVersionable(fileNode);
      // Export VersionHistory

      assertTrue(fileNode.isNodeType("mix:versionable"));

      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      byte[] versionableNode = serialize(fileNode, false, true);
      // version history
      byte[] versionHistory = serialize(fileNode.getVersionHistory(), false, true);
      //System.out.println(new String(versionHistory));
      
      // restore node content
      Node restoreRoot = testRoot.addNode("restRoot");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new ByteArrayInputStream(versionableNode));
      root.save();

      assertTrue(restoreRoot.hasNode("TestEXOJCR933_exo_datetime"));

      Node fileImport = restoreRoot.getNode("TestEXOJCR933_exo_datetime");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new ByteArrayInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
      
      Property property = fileImport.getProperty("exo:dateCreated");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      assertEquals(PropertyType.DATE, property.getType());
      
      property = fileImport.getProperty("exo:dateModified");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      assertEquals(PropertyType.DATE, property.getType());
      
      fileImport.restore("2", true);
      root.save();
      
      property = fileImport.getProperty("exo:dateCreated");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      assertEquals(PropertyType.DATE, property.getType());
      
      property = fileImport.getProperty("exo:dateModified");
      assertNotNull(property);
      assertNotNull(property.getDefinition());
      assertEquals(PropertyType.DATE, property.getType());
      
      fileImport.checkin();
      fileImport.checkout();
      root.save();
   }

   public void testBigRestore_Sys() throws Exception
   {
      File file = createBLOBTempFile(1000);
      int depth = 1;
      
      Node testRoot = root.addNode("testRoot");
      Node fileNode = testRoot.addNode("TestBigRestore_Sys");
      
      for (int i=0; i<depth; i++)
      {
         Node l1 = fileNode.addNode("node_l1_" + i);
         for (int j=0; j<depth; j++)
         {
            Node l2 = l1.addNode("node_l2_" + j);
            for (int k=0; k<depth; k++)
            {
               Node l3 = l2.addNode("node_l3_" + k, "nt:file");
               
               Node contentNode = l3.addNode("jcr:content", "nt:resource");
               contentNode.setProperty("jcr:data", new FileInputStream(file));
               contentNode.setProperty("jcr:mimeType", "image/jpg");
               contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
               
               l3.addMixin("exo:linkable");
               l3.setProperty("exo:links", new String[] {"http://ya.ru"});
               
               l3.addMixin("exo:datetime");
               l3.setProperty("exo:dateCreated", Calendar.getInstance());
               l3.setProperty("exo:dateModified", Calendar.getInstance());
            }
            root.save();
         }
      }
      
      root.save();
      
      fileNode.addMixin("mix:versionable");
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      fileNode.checkin();
      fileNode.checkout();
      root.save();
      
      //export
      VersionableNodeInfo nodeInfo = new VersionableNodeInfo(fileNode);

      // node content
      File versionableNode = serializeToFile(fileNode, true, true);
      // version history
      File versionHistory = serializeToFile(fileNode.getVersionHistory(), true, true);
      
      versionableNode.deleteOnExit();
      versionHistory.deleteOnExit();

      // restore node content
      Node restoreRoot = testRoot.addNode("restRoot");
      testRoot.save();
      
      deserialize(restoreRoot, XmlSaveType.SESSION, true, ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING,
         new FileInputStream(versionableNode));
      root.save();

      assertTrue(restoreRoot.hasNode("TestBigRestore_Sys"));

      Node fileImport = restoreRoot.getNode("TestBigRestore_Sys");
      assertTrue(fileImport.isNodeType("mix:versionable"));

      VersionHistoryImporter versionHistoryImporter =
         new VersionHistoryImporter((NodeImpl)fileImport, new FileInputStream(versionHistory), nodeInfo
            .getBaseVersion(), nodeInfo.getPredecessorsHistory(), nodeInfo.getVersionHistory());
      versionHistoryImporter.doImport();
      root.save();
   }
   
   
   /**
    * Test for issue https://jira.exoplatform.org/browse/JCR-1831 
    */
   public void testJCRContentWithCustomPrivilegeSys() throws Exception
   {
      contentWithCustomPrivilege(true);
   }
   
   /**
    * Test for issue https://jira.exoplatform.org/browse/JCR-1831 
    */
   public void testJCRContentWithCustomPrivilegeDoc() throws Exception
   {
      contentWithCustomPrivilege(false);
   }
   
   private void contentWithCustomPrivilege(boolean isSystemViewExport) throws Exception
   {
      // prepare content 
      NodeImpl testRoot = (NodeImpl)root.addNode("restricted", "nt:unstructured");
      testRoot.addMixin("exo:privilegeable");
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("*:/platform/administrators", new String[]{"read", "add_node", "set_property", "remove"});
      ((ExtendedNode)testRoot).setPermissions(perm);
      session.save();
      
      Node file = testRoot.addNode("accept.gif", "nt:file");
      file.addMixin("exo:privilegeable");
      perm = new HashMap<String, String[]>();
      perm.put("*:/platform/administrators", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("root", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("*:/organization/management/executive-board", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("/platform/administrators", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("any", new String[]{"read"});
      ((ExtendedNode)file).setPermissions(perm);
           
      Node cont = file.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", new FileInputStream(createBLOBTempFile(1)));
      session.save();
      
      Credentials cred = new CredentialsImpl("demo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");
      assertNotNull(sess.getItem("/restricted/accept.gif"));
      assertNotNull(sess.getItem("/restricted/accept.gif/jcr:content"));
      sess.logout();
      
      // system view export
      File exportFile = isSystemViewExport ? File.createTempFile("sys-export", ".xml") : File.createTempFile("doc-export", ".xml");
      exportFile.deleteOnExit();
      if (isSystemViewExport)
      {
         session.exportSystemView(file.getPath(), new FileOutputStream(exportFile), false, false);
      }
      else
      {
         session.exportDocumentView(file.getPath(), new FileOutputStream(exportFile), false, false);
      }
      
      // remove existed node
      file.remove();
      session.save();
      
      try
      {
         testRoot.getNode("accept.gif");
         fail();
      }
      catch (PathNotFoundException e) {
         //ok
      }
         
      
      // check import
      session.importXML("/restricted", new FileInputStream(exportFile), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
      session.save();
      Credentials newCredentials = new CredentialsImpl("demo", "exo".toCharArray());
      Session newSession = repository.login(newCredentials, "ws");
      assertNotNull(newSession.getItem("/restricted/accept.gif"));
      assertNotNull(newSession.getItem("/restricted/accept.gif/jcr:content"));
   }
   
   public void testJCRContentWithCustomOwnerAndPrivilegeSys() throws Exception
   {
      contentWithCustomOwnerAndPrivilege(true);
   }
   
   public void testJCRContentWithCustomOwnerAndPrivilegeDoc() throws Exception
   {
      contentWithCustomOwnerAndPrivilege(false);
   }
 
   private void contentWithCustomOwnerAndPrivilege(boolean isSystemViewExport) throws Exception
   {
   // prepare content 
      NodeImpl testRoot = (NodeImpl)root.addNode("restricted", "nt:unstructured");
      testRoot.addMixin("exo:privilegeable");
      HashMap<String, String[]> perm = new HashMap<String, String[]>();
      perm.put("john", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("*:/platform/administrators", new String[]{"read", "add_node", "set_property", "remove"});
      ((ExtendedNode)testRoot).setPermissions(perm);
      session.save();
      
      testRoot.addMixin("exo:owneable");
      assertEquals("admin", testRoot.getProperty("exo:owner").getString());
      assertEquals("admin", testRoot.getACL().getOwner());
      
      Session sessJohn = repository.login(new CredentialsImpl("john", "exo".toCharArray()), "ws");
      Node file = sessJohn.getRootNode().getNode("restricted").addNode("accept.gif", "nt:file");
      file.addMixin("exo:privilegeable");
      perm = new HashMap<String, String[]>();
      perm.put("*:/platform/administrators", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("root", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("*:/organization/management/executive-board", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("/platform/administrators", new String[]{"read", "add_node", "set_property", "remove"});
      perm.put("any", new String[]{"read"});
      ((ExtendedNode)file).setPermissions(perm);
      
      file.addMixin("exo:owneable");
      assertEquals("john", file.getProperty("exo:owner").getString());
      assertEquals("john", ((ExtendedNode)file).getACL().getOwner());
      
           
      Node cont = file.addNode("jcr:content", "nt:resource");
      cont.setProperty("jcr:mimeType", "text/plain");
      cont.setProperty("jcr:lastModified", Calendar.getInstance());
      cont.setProperty("jcr:data", new FileInputStream(createBLOBTempFile(1)));
      sessJohn.save();
      
       assertEquals("john", ((ExtendedNode)cont).getACL().getOwner());
      
      Credentials cred = new CredentialsImpl("demo", "exo".toCharArray());
      Session sess = repository.login(cred, "ws");
      assertNotNull(sess.getItem("/restricted/accept.gif"));
      assertNotNull(sess.getItem("/restricted/accept.gif/jcr:content"));
      sess.logout();
      
      // export
      File exportFile = isSystemViewExport ? File.createTempFile("sys-export", ".xml") : File.createTempFile("doc-export", ".xml");
      exportFile.deleteOnExit();
      if (isSystemViewExport)
      {
         session.exportSystemView(file.getPath(), new FileOutputStream(exportFile), false, false);
      }
      else
      {
         session.exportDocumentView(file.getPath(), new FileOutputStream(exportFile), false, false);
      }
      
      // remove existed node
      file.remove();
      sessJohn.save();
      
      try
      {
         testRoot.getNode("accept.gif");
         fail();
      }
      catch (PathNotFoundException e) {
         //ok
      }
         
      
      // check import
      session.importXML("/restricted", new FileInputStream(exportFile), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
      session.save();
      
      assertEquals("admin", ((ExtendedNode)session.getItem("/restricted")).getACL().getOwner());
      assertEquals("admin", ((ExtendedNode)session.getItem("/restricted")).getProperty("exo:owner").getString());
      
      assertEquals("john", ((ExtendedNode)session.getItem("/restricted/accept.gif")).getACL().getOwner());
      assertEquals("john", ((ExtendedNode)session.getItem("/restricted/accept.gif")).getProperty("exo:owner").getString());
      
      assertEquals("john", ((ExtendedNode)session.getItem("/restricted/accept.gif/jcr:content")).getACL().getOwner());
      
      Credentials newCredentials = new CredentialsImpl("demo", "exo".toCharArray());
      Session newSession = repository.login(newCredentials, "ws");
      assertNotNull(newSession.getItem("/restricted/accept.gif"));
      assertNotNull(newSession.getItem("/restricted/accept.gif/jcr:content"));
   }
   
   public void testJCRContentWithCustomOwnerSys() throws Exception
   {
      contentWithCustomOwner(true);
   }
   
   public void testJCRContentWithCustomOwnerDoc() throws Exception
   {
      contentWithCustomOwner(false);
   }
   
   private void contentWithCustomOwner(boolean isSystemViewExport) throws Exception
   {
      ExtendedNode testRoot = (ExtendedNode)root.addNode("testRoot");
      testRoot.addMixin("exo:privilegeable");
      testRoot.setPermission("john", new String[]{PermissionType.READ, PermissionType.ADD_NODE,
         PermissionType.SET_PROPERTY});
      testRoot.setPermission(root.getSession().getUserID(), PermissionType.ALL);
      testRoot.removePermission("any");

      ExtendedNode subRoot = (ExtendedNode)testRoot.addNode("subroot");
      root.getSession().save();
      
      testRoot.addMixin("exo:owneable");
      assertEquals("admin", testRoot.getProperty("exo:owner").getString());
      assertEquals("admin", testRoot.getACL().getOwner());
      
      Session session1 = repository.login(new CredentialsImpl("john", "exo".toCharArray()));
      Node testRoot1 = session1.getRootNode().getNode("testRoot");

      ExtendedNode subRoot1 = (ExtendedNode)testRoot1.getNode("subroot");
      
      subRoot1.addMixin("exo:owneable");
      assertEquals("john", subRoot1.getProperty("exo:owner").getString());
      assertEquals("john", subRoot1.getACL().getOwner());
      
      Node testNode = subRoot1.addNode("node");
      assertEquals("john", ((ExtendedNode)testNode).getACL().getOwner());
      session1.save();
      
      // export
      File exportFile = isSystemViewExport ? File.createTempFile("sys-export", ".xml") : File.createTempFile("doc-export", ".xml");
      exportFile.deleteOnExit();
      
      if (isSystemViewExport)
      {
         session1.exportSystemView(subRoot1.getPath(), new FileOutputStream(exportFile), false, false);
      }
      else
      {
         session1.exportDocumentView(subRoot1.getPath(), new FileOutputStream(exportFile), false, false);
      }
      
      // remove existed node
      subRoot1.remove();
      session1.save();
      
      try
      {
         testRoot1.getNode("subroot");
         fail();
      }
      catch (PathNotFoundException e) {
         //ok
      }
      
      session.importXML("/testRoot", new FileInputStream(exportFile), ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING);
      session.save();
      
      assertEquals("admin", ((ExtendedNode)session.getItem("/testRoot")).getACL().getOwner());
      assertEquals("admin", ((ExtendedNode)session.getItem("/testRoot")).getProperty("exo:owner").getString());
      
      assertEquals("john", ((ExtendedNode)session.getItem("/testRoot/subroot")).getACL().getOwner());
      assertEquals("john", ((ExtendedNode)session.getItem("/testRoot/subroot")).getProperty("exo:owner").getString());
      
      assertEquals("john", ((ExtendedNode)session.getItem("/testRoot/subroot/node")).getACL().getOwner());
   }

   
   public void testImportCreateNew() throws Exception
   {
      for (int i = 0; i < 4; i++)
      {
         if (PropertyManager.isDevelopping())
            System.out.println("System IMPORT_UUID_CREATE_NEW " + i);
         testImport(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, true, i);
         if (PropertyManager.isDevelopping())
            System.out.println("Document IMPORT_UUID_CREATE_NEW " + i);
         testImport(ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW, false, i);
      }
   }

   public void testImportRemoveExisting() throws Exception
   {
      for (int i = 0; i < 4; i++)
      {
         if (PropertyManager.isDevelopping())
            System.out.println("System IMPORT_UUID_COLLISION_REMOVE_EXISTING " + i);
         testImport(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING, true, i);
         if (PropertyManager.isDevelopping())
            System.out.println("Document IMPORT_UUID_COLLISION_REMOVE_EXISTING " + i);
         testImport(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING, false, i);
      }
   }

   public void testImportReplaceExisting() throws Exception
   {
      for (int i = 0; i < 4; i++)
      {
         if (PropertyManager.isDevelopping())
            System.out.println("System IMPORT_UUID_COLLISION_REPLACE_EXISTING " + i);
         testImport(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING, true, i);
         if (PropertyManager.isDevelopping())
            System.out.println("Document IMPORT_UUID_COLLISION_REPLACE_EXISTING " + i);
         testImport(ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING, false, i);
      }
   }

   private void testImport(int uuidBehavior, boolean system, int preLoadedNodes) throws Exception
   {
      Node testImport = (ExtendedNode)session.getRootNode().addNode("testImport");
      session.save();
      Node node = createNodeJCR2125(testImport);
      if (preLoadedNodes == 3)
      {
         node.addMixin("mix:referenceable");
         testImport.save();
      }
      ByteArrayOutputStream outBefore = new ByteArrayOutputStream();
      if (system)
      {
         testImport.getSession().exportSystemView(node.getPath(), outBefore, false, false);
      }
      else
      {
         testImport.getSession().exportDocumentView(node.getPath(), outBefore, false, false);
      }
      outBefore.close();
      setNewValuesJCR2125(node);
      ByteArrayOutputStream outAfter = new ByteArrayOutputStream();
      if (system)
      {
         testImport.getSession().exportSystemView(node.getPath(), outAfter, false, false);
      }
      else
      {
         testImport.getSession().exportDocumentView(node.getPath(), outAfter, false, false);
      }
      outAfter.close();
      if (preLoadedNodes == 3)
      {
         for (NodeIterator ni = node.getNodes(); ni.hasNext();)
         {
            ni.nextNode().remove();
         }
         node.addNode("Node-3");
         node.addNode("Node-1");
      }
      else
      {
         node.remove();
      }
      for (int i = 0; i < preLoadedNodes && i < 2; i++)
      {
         Node root = testImport.addNode("JCR-2125");
         root.addNode("Node-3");
         root.addNode("Node-1");
      }
      Node temp = testImport.addNode("temp");
      for (int i = 0; i < preLoadedNodes && i < 2; i++)
      {
         Node root = temp.addNode("JCR-2125");
         root.addNode("Node-3");
         root.addNode("Node-1");
      }
      testImport.save();
      InputStream in = new ByteArrayInputStream(outBefore.toByteArray());
      testImport.getSession().importXML(temp.getPath(), in, uuidBehavior);
      in.close();
      testImport.save();

      in = new ByteArrayInputStream(outAfter.toByteArray());
      testImport.getSession().importXML(testImport.getPath(), in, uuidBehavior);
      in.close();
      testImport.save();

      Session session2 = (SessionImpl)repository.login(credentials, "ws");
      Node accessTestRoot2 = (Node)session2.getItem(testImport.getPath());

      if (PropertyManager.isDevelopping())
         showTree(accessTestRoot2, 0);
      
      checkTestImport(accessTestRoot2, uuidBehavior, preLoadedNodes);
      session2.logout();
   }

   
   private Node createNodeJCR2125(Node parentNode) throws Exception
   {
      Node root = parentNode.addNode("JCR-2125");
      Node n = root.addNode("Node-1");
      n.setProperty("name", "old value 1");
      n.addNode("SubNode-1").addNode("SubNode-1");
      n = root.addNode("Node-2");
      n.addMixin("mix:referenceable");
      n.setProperty("name", "old value 2");
      n.addNode("SubNode-2").addNode("SubNode-2");
      n = root.addNode("Node-3");
      n.setProperty("name", "old value 3-1");
      n.addNode("SubNode-3-1").addNode("SubNode-3-1");
      n = root.addNode("Node-3");
      n.addMixin("mix:referenceable");
      n.setProperty("name", "old value 3-2");
      n.addNode("SubNode-3-2").addNode("SubNode-3-2");
      n = root.addNode("Node-3");
      n.setProperty("name", "old value 3-3");
      n.addNode("SubNode-3-3").addNode("SubNode-3-3");
      n = root.addNode("Node-3");
      n.addMixin("mix:referenceable");
      n.setProperty("name", "old value 3-4");
      n.addNode("SubNode-3-4").addNode("SubNode-3-4");
      n = root.addNode("Node-3");
      n.setProperty("name", "old value 3-5");
      n.addNode("SubNode-3-5").addNode("SubNode-3-5");
      n = root.addNode("Node-4");
      n.setProperty("name", "old value 4");
      n.addNode("SubNode-4").addNode("SubNode-4");
      session.save();
      return root;
   }

   private void setNewValuesJCR2125(Node root) throws Exception
   {
      root.getNode("Node-1").setProperty("name", "new value 1");
      root.getNode("Node-2").setProperty("name", "new value 2");
      root.getNode("Node-3").setProperty("name", "new value 3-1");
      Node n = root.getNode("Node-3[2]");
      n.setProperty("name", "new value 3-4");
      n.getNode("SubNode-3-2").remove();
      n.addNode("SubNode-3-4").addNode("SubNode-3-4");
      root.getNode("Node-3[3]").setProperty("name", "new value 3-3");
      n = root.getNode("Node-3[4]");
      n.setProperty("name", "new value 3-2");
      n.getNode("SubNode-3-4").remove();
      n.addNode("SubNode-3-2").addNode("SubNode-3-2");      
      root.getNode("Node-3[5]").setProperty("name", "new value 3-5");
      root.getNode("Node-4").setProperty("name", "new value 4");
      session.save();
      root.orderBefore("Node-3[4]", "Node-3[2]");
      session.save();
      root.orderBefore("Node-3[3]", "Node-3[5]");
      session.save();
   }

   private void showTree(Node node, int depth) throws Exception
   {
      for (int i = 0; i <= depth; i++)
         System.out.print(">>>>");
      System.out.println(node.getName() + "[" + node.getIndex() + "]-" + ((NodeImpl)node).getInternalIdentifier()
         + (node.hasProperty("name") ? (": name = " + node.getProperty("name").getString()) : ""));
      NodeIterator ni = node.getNodes();
      while (ni.hasNext())
      {
         Node subNode = ni.nextNode();
         showTree(subNode, depth + 1);
      }
   }

   private void checkTestImport(Node node, int uuidBehavior, int preLoadedNodes) throws Exception
   {
      assertEquals("testImport", node.getName());
      NodeIterator ni = node.getNodes();
      if ((uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING ||
          uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
         && preLoadedNodes == 3)
      {
         assertEquals(4, ni.getSize());
      }
      else
      {
         assertEquals(2 + preLoadedNodes, ni.getSize());
      }
      int countNodeJCR2125 = 0;
      int countNonImportedNodeJCR2125 = 0;
      boolean hasTemp = false;
      while (ni.hasNext())
      {
         Node subNode = ni.nextNode();
         assertTrue(subNode.getPath().startsWith(node.getPath()));
         if (subNode.getName().equals("temp"))
         {
            if (hasTemp)
            {
               fail("Only one temp node is expected");
            }
            hasTemp = true;
            checkTemp(subNode, uuidBehavior, preLoadedNodes);
         }
         else if (subNode.getName().equals("JCR-2125"))
         {
            countNodeJCR2125++;
            if (!checkTestJCR2125(subNode, uuidBehavior, preLoadedNodes, false))
            {
               countNonImportedNodeJCR2125++;
            }
         }
         else
         {
            fail("Unexpected node: " + subNode.getName());
         }
      }
      assertTrue(hasTemp);
      if ((uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING || 
           uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
         && preLoadedNodes == 3)
      {
         assertEquals(preLoadedNodes, countNodeJCR2125);
         assertEquals(2, countNonImportedNodeJCR2125);
      }
      else
      {
         assertEquals(preLoadedNodes + 1, countNodeJCR2125);
         assertEquals(preLoadedNodes, countNonImportedNodeJCR2125);
      }
   }

   private void checkTemp(Node node, int uuidBehavior, int preLoadedNodes) throws Exception
   {
      NodeIterator ni = node.getNodes();
      if ((uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING || 
           uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
         && preLoadedNodes == 3)
      {
         assertEquals(2, ni.getSize());
      }
      else
      {
         assertEquals(1 + Math.min(preLoadedNodes, 2), ni.getSize());
      }
      int countNodeJCR2125 = 0;
      int countNonImportedNodeJCR2125 = 0;
      while (ni.hasNext())
      {
         Node subNode = ni.nextNode();
         assertTrue(subNode.getPath().startsWith(node.getPath()));
         if (subNode.getName().equals("JCR-2125"))
         {
            countNodeJCR2125++;
            if (!checkTestJCR2125(subNode, uuidBehavior, preLoadedNodes, true))
            {
               countNonImportedNodeJCR2125++;
            }
         }
         else
         {
            fail("Unexpected node: " + subNode.getName());
         }
      }
      if ((uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING || 
           uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
         && preLoadedNodes == 3)
      {
         assertEquals(2, countNodeJCR2125);
      }
      else
      {
         assertEquals(Math.min(preLoadedNodes, 2) + 1, countNodeJCR2125);
      }
      assertEquals(Math.min(preLoadedNodes, 2), countNonImportedNodeJCR2125);
   }

   private boolean checkTestJCR2125(Node node, int uuidBehavior, int preLoadedNodes, boolean inTemp) throws Exception
   {
      boolean isImportedNode = false;
      NodeIterator ni = node.getNodes();
      int totalNode3 = 5;
      int totalOtherNodes = 3;
      if (inTemp)
      {
         if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING)
         {
            totalNode3 = 3;
            totalOtherNodes = 2;
         }
      }
      else
      {
         if (uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING && preLoadedNodes != 3)
         {
            totalNode3 = 3;
            totalOtherNodes = 2;
         }         
      }
      if (ni.getSize() == 2)
      {
         assertTrue(node.hasNode("Node-1"));
         assertTrue(node.getNode("Node-1").getPath().startsWith(node.getPath()));
         assertTrue(node.hasNode("Node-3"));
         assertTrue(node.getNode("Node-3").getPath().startsWith(node.getPath()));
      }
      else if (ni.getSize() == (totalNode3 + totalOtherNodes))
      {
         assertTrue(node.hasNode("Node-1"));
         assertEquals(
            !((inTemp && uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) || 
             (!inTemp && uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING && preLoadedNodes != 3)),
            node.hasNode("Node-2"));
         assertTrue(node.hasNode("Node-4"));
         NodeIterator sni = node.getNodes("Node-3");
         assertEquals(totalNode3, sni.getSize());
         while (ni.hasNext())
         {
            Node subNode = ni.nextNode();
            assertTrue(subNode.getPath().startsWith(node.getPath()));
            assertTrue(subNode.hasProperty("name"));
            if (subNode.getName().equals("Node-3"))
            {
               if ((inTemp && uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING) ||
                   (!inTemp && uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING && preLoadedNodes != 3))
               {
                  int suffix = -1;
                  if (subNode.getIndex() == 1)
                  {
                     suffix = 1;
                  }
                  else if (subNode.getIndex() == 2)
                  {
                     suffix = 3;
                  }
                  else if (subNode.getIndex() == 3)
                  {
                     suffix = 5;
                  }
                  else
                  {
                     fail("Forbidden index " + subNode.getIndex());
                  }
                  assertEquals((inTemp ? "old value " : "new value ") + subNode.getName().substring(5) + "-" + suffix, subNode
                     .getProperty("name").getString());
                  assertTrue(subNode.hasNode("SubNode-3-" + suffix));
                  Node n = subNode.getNode("SubNode-3-" + suffix);
                  assertTrue(subNode.getPath() + " should be a sub path of " + n.getPath(), n.getPath().startsWith(subNode.getPath()));
                  assertTrue(subNode.hasNode("SubNode-3-" + suffix + "/SubNode-3-" + suffix));
                  assertTrue(n.getNode("SubNode-3-" + suffix).getPath().startsWith(n.getPath()));
               }
               else if (inTemp && uuidBehavior == ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING)
               {
                  int suffix = -1;
                  boolean oldValue = true;
                  if (subNode.getIndex() == 1)
                  {
                     suffix = 1;
                  }
                  else if (subNode.getIndex() == 2)
                  {
                     suffix = 3;
                  }
                  else if (subNode.getIndex() == 3)
                  {
                     suffix = 5;
                  }
                  else if (subNode.getIndex() == 4)
                  {
                     suffix = 2;
                     oldValue = false;
                  }                  
                  else if (subNode.getIndex() == 5)
                  {
                     suffix = 4;
                     oldValue = false;
                  }
                  else
                  {
                     fail("Forbidden index " + subNode.getIndex());
                  }
                  assertEquals((oldValue ? "old value " : "new value ") + subNode.getName().substring(5) + "-" + suffix,
                     subNode.getProperty("name").getString());
                  assertTrue(subNode.hasNode("SubNode-3-" + suffix));
                  Node n = subNode.getNode("SubNode-3-" + suffix);
                  assertTrue(subNode.getPath() + " should be a sub path of " + n.getPath(), n.getPath().startsWith(subNode.getPath()));
                  assertTrue(subNode.hasNode("SubNode-3-" + suffix + "/SubNode-3-" + suffix));
                  assertTrue(n.getNode("SubNode-3-" + suffix).getPath().startsWith(n.getPath()));
               }
               else
               {
                  assertEquals(
                     (inTemp ? "old value " : "new value ") + subNode.getName().substring(5) + "-" + subNode.getIndex(),
                     subNode.getProperty("name").getString());
                  assertTrue(subNode.hasNode("SubNode-3-" + subNode.getIndex()));
                  Node n = subNode.getNode("SubNode-3-" + subNode.getIndex());
                  assertTrue(n.getPath().startsWith(subNode.getPath()));
                  assertTrue(subNode.hasNode("SubNode-3-" + subNode.getIndex() + "/SubNode-3-" + subNode.getIndex()));
                  assertTrue(n.getNode("SubNode-3-" + subNode.getIndex()).getPath().startsWith(n.getPath()));
               }
            }
            else
            {
               assertEquals((inTemp
                  && (uuidBehavior != ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING || !subNode.getName()
                     .equals("Node-2")) ? "old value " : "new value ")
                  + subNode.getName().substring(5), subNode.getProperty("name").getString());
               assertTrue(subNode.hasNode("SubNode-" + subNode.getName().substring(5)));
               Node n = subNode.getNode("SubNode-" + subNode.getName().substring(5));
               assertTrue(n.getPath().startsWith(subNode.getPath()));
               assertTrue(subNode.hasNode("SubNode-" + subNode.getName().substring(5) + "/SubNode-" + subNode.getName().substring(5)));
               assertTrue(n.getNode("SubNode-" + subNode.getName().substring(5)).getPath().startsWith(n.getPath()));
            }
         }
         isImportedNode = true;
      }
      else
      {
         fail("Unexpected total amount of sub nodes: " + ni.getSize());
      }
      return isImportedNode;
   }
}
