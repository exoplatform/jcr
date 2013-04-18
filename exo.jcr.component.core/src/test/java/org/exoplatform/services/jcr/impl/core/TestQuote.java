/*
 * Copyright (C) 2013 eXo Platform SAS.
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
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.core.WorkspaceContainerFacade;
import org.exoplatform.services.jcr.dataflow.persistent.WorkspaceStorageCache;
import org.exoplatform.services.jcr.impl.backup.Backupable;
import org.exoplatform.services.jcr.impl.dataflow.persistent.CacheableWorkspaceDataManager;
import org.exoplatform.services.jcr.impl.util.ISO9075;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.jcr.ImportUUIDBehavior;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestQuote extends JcrImplBaseTest
{
   private static final String NODE_NAME = "My Node \"with a '\"";

   private static final String PROPERTY_NAME = "My Property \"with a '\"";

   private Node testRootNode;

   private Session session2;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      testRootNode = session.getRootNode().addNode("TestQuote");
      session.save();
      session2 = repository.login(credentials, "ws");
   }

   @Override
   protected void tearDown() throws Exception
   {
      if (testRootNode != null)
      {
         testRootNode.remove();
         session.save();
      }
      session2.logout();
      super.tearDown();
   }

   public void testCRUD() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      testRootNode.save();

      assertTrue(session2.itemExists("/TestQuote/" + NODE_NAME));
      assertTrue(session2.itemExists("/TestQuote/" + NODE_NAME + "/" + PROPERTY_NAME));
      Node n = (Node)session2.getItem("/TestQuote/" + NODE_NAME);
      assertTrue(n.hasProperty(PROPERTY_NAME));
      assertEquals("my value", n.getProperty(PROPERTY_NAME).getString());
      Node subNode = n.getParent().addNode(NODE_NAME + "/sub-" + NODE_NAME);
      subNode.addMixin("exo:owneable");
      subNode.setProperty(PROPERTY_NAME, "my other value");
      subNode.setProperty(PROPERTY_NAME + "2", "my other value 2");
      n.getParent().addNode(NODE_NAME + "/sub-" + NODE_NAME + "2");
      n.save();
      assertTrue(node.hasNode("sub-" + NODE_NAME));
      Node sn = node.getNode("sub-" + NODE_NAME);
      assertTrue(sn.hasProperty(PROPERTY_NAME));
      assertEquals("my other value", sn.getProperty(PROPERTY_NAME).getString());
      PropertyIterator it = sn.getProperties(PROPERTY_NAME + "|" + PROPERTY_NAME + "2|exo:owner");
      int propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }
      }
      assertEquals(3, propertyFound);
      it = sn.getProperties();
      propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }
      }
      assertEquals(3, propertyFound);
      NodeIterator nIt = node.getNodes("sub-" + NODE_NAME + "|sub-" + NODE_NAME + "2");
      int nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }
      }
      assertEquals(2, nodeFound);
      nIt = node.getNodes();
      nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }
      }
      assertEquals(2, nodeFound);

      // Clear cache
      ManageableRepository repository = repositoryService.getDefaultRepository();
      WorkspaceContainerFacade wsc = repository.getWorkspaceContainer("ws");

      CacheableWorkspaceDataManager dm =
         (CacheableWorkspaceDataManager)wsc.getComponent(CacheableWorkspaceDataManager.class);
      WorkspaceStorageCache cache = dm.getCache();
      if (cache.isEnabled() && cache instanceof Backupable)
      {
         ((Backupable)cache).clean();
      }

      it = sn.getProperties(PROPERTY_NAME + "|" + PROPERTY_NAME + "2|exo:owner");
      propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }
      }
      assertEquals(3, propertyFound);
      it = sn.getProperties();
      propertyFound = 0;
      while (it.hasNext())
      {
         Property p = it.nextProperty();
         if (p.getName().equals(PROPERTY_NAME))
         {
            assertEquals("my other value", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals(PROPERTY_NAME + "2"))
         {
            assertEquals("my other value 2", p.getString());
            propertyFound++;
         }
         else if (p.getName().equals("exo:owner"))
         {
            assertEquals("admin", p.getString());
            propertyFound++;
         }
      }
      assertEquals(3, propertyFound);
      nIt = node.getNodes("sub-" + NODE_NAME + "|sub-" + NODE_NAME + "2");
      nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }
      }
      assertEquals(2, nodeFound);
      nIt = node.getNodes();
      nodeFound = 0;
      while (nIt.hasNext())
      {
         Node no = nIt.nextNode();
         if (no.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
         else if (no.getName().equals("sub-" + NODE_NAME + "2"))
         {
            nodeFound++;
         }
      }
      assertEquals(2, nodeFound);

      Node node2 = testRootNode.addNode(NODE_NAME);
      assertTrue(node2.canAddMixin("mix:referenceable"));
      node2.addMixin("mix:referenceable");
      node2.setProperty(PROPERTY_NAME, "my value 3");
      testRootNode.save();
      assertFalse(node2.canAddMixin("mix:referenceable"));
      String uuid = node2.getUUID();
      Node n2 = session2.getNodeByUUID(uuid);
      assertTrue(n2.hasProperty(PROPERTY_NAME));
      assertEquals("my value 3", n2.getProperty(PROPERTY_NAME).getString());
      node2.setProperty(PROPERTY_NAME, "my value 4");
      testRootNode.save();
      assertEquals("my value 4", n2.getProperty(PROPERTY_NAME).getString());
      node2.setProperty(PROPERTY_NAME, (String)null);
      testRootNode.save();
      assertFalse(n2.hasProperty(PROPERTY_NAME));
      node2.remove();
      testRootNode.save();
      try
      {
         session2.getNodeByUUID(uuid);
         fail("ItemNotFoundException was expected");
      }
      catch (ItemNotFoundException e)
      {
         // expected exception
      }
   }

   public void testLock() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      testRootNode.save();
      try
      {
         node.lock(false, true);
         fail("a LockException was expected");
      }
      catch (LockException e)
      {
         // expected exception
      }
      assertTrue(node.canAddMixin("mix:lockable"));
      node.addMixin("mix:lockable");
      testRootNode.save();
      assertFalse(node.canAddMixin("mix:lockable"));
      node.lock(false, true);
      assertTrue(node.isLocked());

      Node n = (Node)session2.getItem("/TestQuote/" + NODE_NAME);
      assertTrue(n.isLocked());
      try
      {
         n.lock(false, true);
         fail("a LockException was expected");
      }
      catch (LockException e)
      {
         // expected exception
      }
      node.unlock();
      assertFalse(node.isLocked());
      n.lock(false, true);
      assertTrue(n.holdsLock());
      assertTrue(node.isLocked());
      n.unlock();
      assertFalse(node.isLocked());
      assertFalse(n.holdsLock());

      n.removeMixin("mix:lockable");
      testRootNode.save();
   }

   public void testVersioning() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      node.addNode("sub-" + NODE_NAME);
      assertTrue(node.canAddMixin("mix:versionable"));
      node.addMixin("mix:versionable");
      testRootNode.save();
      assertFalse(node.canAddMixin("mix:versionable"));
      assertTrue(node.isCheckedOut());
      node.checkin();
      assertFalse(node.isCheckedOut());
      node.checkout();
      node.setProperty(PROPERTY_NAME, "my value 2");
      testRootNode.save();
      node.checkin();
      assertFalse(node.isCheckedOut());
      node.checkout();
      node.setProperty(PROPERTY_NAME, "my value 3");
      testRootNode.save();
      node.checkin();

      Node n = (Node)session2.getItem("/TestQuote/" + NODE_NAME);
      assertFalse(n.isCheckedOut());
      node.checkout();
      assertTrue(n.isCheckedOut());
      assertTrue(n.hasProperty(PROPERTY_NAME));
      assertTrue(n.hasNode("sub-" + NODE_NAME));
      assertEquals("my value 3", n.getProperty(PROPERTY_NAME).getString());

      VersionHistory vh = node.getVersionHistory();
      Version v = vh.getRootVersion();
      v = v.getSuccessors()[0];
      assertTrue(v.hasProperty("jcr:frozenNode/" + PROPERTY_NAME));
      assertTrue(v.hasNode("jcr:frozenNode/sub-" + NODE_NAME));
      assertEquals("my value", v.getProperty("jcr:frozenNode/" + PROPERTY_NAME).getString());
      v = v.getSuccessors()[0];
      assertTrue(v.hasProperty("jcr:frozenNode/" + PROPERTY_NAME));
      assertTrue(v.hasNode("jcr:frozenNode/sub-" + NODE_NAME));
      assertEquals("my value 2", v.getProperty("jcr:frozenNode/" + PROPERTY_NAME).getString());
      v = v.getSuccessors()[0];
      assertTrue(v.hasProperty("jcr:frozenNode/" + PROPERTY_NAME));
      assertTrue(v.hasNode("jcr:frozenNode/sub-" + NODE_NAME));
      assertEquals("my value 3", v.getProperty("jcr:frozenNode/" + PROPERTY_NAME).getString());
      node.restore(vh.getRootVersion().getSuccessors()[0], true);
      node.checkout();
      assertTrue(n.hasProperty(PROPERTY_NAME));
      assertTrue(n.hasNode("sub-" + NODE_NAME));
      assertEquals("my value", n.getProperty(PROPERTY_NAME).getString());

      node.removeMixin("mix:versionable");
      testRootNode.save();
   }

   public void testImportExport() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      node.addNode("sub-" + NODE_NAME);
      testRootNode.save();
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      testRootNode.getSession().exportDocumentView(node.getPath(), out, false, false);
      Node targetNode = testRootNode.addNode("Exported-" + NODE_NAME);
      Node targetNode2 = testRootNode.addNode("ExportedBis-" + NODE_NAME);
      testRootNode.save();
      ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
      testRootNode.getSession().importXML(targetNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      testRootNode.save();
      in = new ByteArrayInputStream(out.toByteArray());
      testRootNode.getSession().getWorkspace()
         .importXML(targetNode2.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

      Node n = (Node)session2.getItem("/TestQuote/Exported-" + NODE_NAME);
      assertTrue(n.hasNode(NODE_NAME));
      assertTrue(n.hasProperty(NODE_NAME + "/" + PROPERTY_NAME));
      assertEquals("my value", n.getProperty(NODE_NAME + "/" + PROPERTY_NAME).getString());
      assertTrue(n.hasNode(NODE_NAME + "/sub-" + NODE_NAME));

      Node n2 = (Node)session2.getItem("/TestQuote/ExportedBis-" + NODE_NAME);
      assertTrue(n2.hasNode(NODE_NAME));
      assertTrue(n2.hasProperty(NODE_NAME + "/" + PROPERTY_NAME));
      assertEquals("my value", n2.getProperty(NODE_NAME + "/" + PROPERTY_NAME).getString());
      assertTrue(n2.hasNode(NODE_NAME + "/sub-" + NODE_NAME));

      out = new ByteArrayOutputStream();
      testRootNode.getSession().exportSystemView(node.getPath(), out, false, false);
      targetNode = testRootNode.addNode("Exported2-" + NODE_NAME);
      targetNode2 = testRootNode.addNode("ExportedBis2-" + NODE_NAME);
      testRootNode.save();
      in = new ByteArrayInputStream(out.toByteArray());
      testRootNode.getSession().importXML(targetNode.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
      testRootNode.save();
      in = new ByteArrayInputStream(out.toByteArray());
      testRootNode.getSession().getWorkspace()
         .importXML(targetNode2.getPath(), in, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);

      n = (Node)session2.getItem("/TestQuote/Exported2-" + NODE_NAME);
      assertTrue(n.hasNode(NODE_NAME));
      assertTrue(n.hasProperty(NODE_NAME + "/" + PROPERTY_NAME));
      assertEquals("my value", n.getProperty(NODE_NAME + "/" + PROPERTY_NAME).getString());
      assertTrue(n.hasNode(NODE_NAME + "/sub-" + NODE_NAME));

      n2 = (Node)session2.getItem("/TestQuote/ExportedBis-" + NODE_NAME);
      assertTrue(n2.hasNode(NODE_NAME));
      assertTrue(n2.hasProperty(NODE_NAME + "/" + PROPERTY_NAME));
      assertEquals("my value", n2.getProperty(NODE_NAME + "/" + PROPERTY_NAME).getString());
      assertTrue(n2.hasNode(NODE_NAME + "/sub-" + NODE_NAME));
   }

   public void testCopyMove() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      node.addNode("sub-" + NODE_NAME);
      Node node2 = testRootNode.addNode(NODE_NAME + "2");
      node2.setProperty(PROPERTY_NAME, "my value");
      node2.addNode("sub-" + NODE_NAME);
      Node targetNode = testRootNode.addNode("Target-" + NODE_NAME);
      Node targetNode2 = testRootNode.addNode("Target2-" + NODE_NAME);
      testRootNode.save();
      testRootNode.getSession().move(node.getPath(), targetNode.getPath() + "/Moved-" + NODE_NAME);
      testRootNode.save();
      testRootNode.getSession().getWorkspace()
         .move(node2.getPath(), targetNode2.getPath() + "/Moved-" + NODE_NAME + "2");

      assertFalse(session2.itemExists("/TestQuote/" + NODE_NAME));
      assertFalse(session2.itemExists("/TestQuote/" + NODE_NAME + "2"));
      Node n = (Node)session2.getItem("/TestQuote/Target-" + NODE_NAME);
      assertTrue(n.hasNode("Moved-" + NODE_NAME));
      assertTrue(n.hasProperty("Moved-" + NODE_NAME + "/" + PROPERTY_NAME));
      assertEquals("my value", n.getProperty("Moved-" + NODE_NAME + "/" + PROPERTY_NAME).getString());
      assertTrue(n.hasNode("Moved-" + NODE_NAME + "/sub-" + NODE_NAME));

      Node n2 = (Node)session2.getItem("/TestQuote/Target2-" + NODE_NAME);
      assertTrue(n2.hasNode("Moved-" + NODE_NAME + "2"));
      assertTrue(n2.hasProperty("Moved-" + NODE_NAME + "2/" + PROPERTY_NAME));
      assertEquals("my value", n2.getProperty("Moved-" + NODE_NAME + "2/" + PROPERTY_NAME).getString());
      assertTrue(n2.hasNode("Moved-" + NODE_NAME + "2/sub-" + NODE_NAME));

      session2.getWorkspace().copy(n.getNode("Moved-" + NODE_NAME).getPath(), "/TestQuote/Copied-" + NODE_NAME);
      assertTrue(n.hasNode("Moved-" + NODE_NAME));

      assertTrue(testRootNode.hasNode("Copied-" + NODE_NAME));
      assertTrue(testRootNode.hasProperty("Copied-" + NODE_NAME + "/" + PROPERTY_NAME));
      assertEquals("my value", testRootNode.getProperty("Copied-" + NODE_NAME + "/" + PROPERTY_NAME).getString());
      assertTrue(testRootNode.hasNode("Copied-" + NODE_NAME + "/sub-" + NODE_NAME));
   }

   public void testSearch() throws Exception
   {
      Node node = testRootNode.addNode(NODE_NAME);
      node.setProperty(PROPERTY_NAME, "my value");
      node.setProperty(PROPERTY_NAME + "2", "my value 2");
      Node subNode = node.addNode("sub-" + NODE_NAME);
      subNode.setProperty(PROPERTY_NAME, "my value");
      subNode.setProperty(PROPERTY_NAME + "2", "my value 3");
      testRootNode.save();

      QueryManager qm = testRootNode.getSession().getWorkspace().getQueryManager();
      Query query = qm.createQuery("select * from nt:unstructured WHERE jcr:path = '/TestQuote[%]/%'", Query.SQL);
      QueryResult result = query.execute();
      NodeIterator it = result.getNodes();
      int nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query = qm.createQuery("/jcr:root/TestQuote//element(*, nt:unstructured)", Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery(
            "select * from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%' and \""
               + PROPERTY_NAME.replaceAll("\"", "\"\"") + "\"='my value'", Query.SQL);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery("/jcr:root/TestQuote//element(*, nt:unstructured)[@" + ISO9075.encode(PROPERTY_NAME)
            + " = 'my value']", Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery("select \"" + PROPERTY_NAME.replaceAll("\"", "\"\"")
            + "\" from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%'", Query.SQL);
      result = query.execute();
      RowIterator ri = result.getRows();
      while (ri.hasNext())
      {
         Row r = ri.nextRow();
         assertEquals("my value", r.getValue(PROPERTY_NAME).getString());
      }
      assertEquals(2, ri.getSize());
      query =
         qm.createQuery("/jcr:root/TestQuote//element(*, nt:unstructured)/@" + ISO9075.encode(PROPERTY_NAME),
            Query.XPATH);
      result = query.execute();
      ri = result.getRows();
      while (ri.hasNext())
      {
         Row r = ri.nextRow();
         assertEquals("my value", r.getValue(PROPERTY_NAME).getString());
      }
      assertEquals(2, ri.getSize());
      query =
         qm.createQuery(
            "select \"" + PROPERTY_NAME.replaceAll("\"", "\"\"") + "\",\"" + PROPERTY_NAME.replaceAll("\"", "\"\"")
               + "\" from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%'", Query.SQL);
      result = query.execute();
      ri = result.getRows();
      while (ri.hasNext())
      {
         Row r = ri.nextRow();
         assertEquals("my value", r.getValue(PROPERTY_NAME).getString());
      }
      assertEquals(2, ri.getSize());
      query =
         qm.createQuery("/jcr:root/TestQuote//element(*, nt:unstructured)/(@" + ISO9075.encode(PROPERTY_NAME) + "|@"
            + ISO9075.encode(PROPERTY_NAME) + "2)", Query.XPATH);
      result = query.execute();
      ri = result.getRows();
      while (ri.hasNext())
      {
         Row r = ri.nextRow();
         assertEquals("my value", r.getValue(PROPERTY_NAME).getString());
      }
      assertEquals(2, ri.getSize());
      query =
         qm.createQuery(
            "select * from nt:unstructured WHERE jcr:path LIKE '/TestQuote/" + NODE_NAME.replaceAll("'", "''")
               + "[%]/%' and \"" + PROPERTY_NAME.replaceAll("\"", "\"\"") + "\"='my value'", Query.SQL);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(1, it.getSize());
      assertEquals(1, nodeFound);
      query =
         qm.createQuery("/jcr:root/TestQuote/" + ISO9075.encode(NODE_NAME) + "//element(*, nt:unstructured)[@"
            + ISO9075.encode(PROPERTY_NAME) + " = 'my value']", Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(1, it.getSize());
      assertEquals(1, nodeFound);
      query =
         qm.createQuery(
            "select * from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%' and \""
               + PROPERTY_NAME.replaceAll("\"", "\"\"") + "\"='my value' and \""
               + PROPERTY_NAME.replaceAll("\"", "\"\"") + "3\" IS NULL", Query.SQL);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery("/jcr:root/TestQuote//element(*, nt:unstructured)[@" + ISO9075.encode(PROPERTY_NAME)
            + " = 'my value' and not(@" + ISO9075.encode(PROPERTY_NAME + "3") + ")]", Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery(
            "select * from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%' and \""
               + PROPERTY_NAME.replaceAll("\"", "\"\"") + "\" LIKE 'my value%' ORDER BY \""
               + PROPERTY_NAME.replaceAll("\"", "\"\"") + "2\" DESC", Query.SQL);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      String firstNode = null;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (firstNode == null)
         {
            firstNode = n.getName();
         }
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      assertEquals("sub-" + NODE_NAME, firstNode);
      query =
         qm.createQuery("/jcr:root/TestQuote//element(*, nt:unstructured)[jcr:like(@" + ISO9075.encode(PROPERTY_NAME)
            + ", 'my value%')] order by @" + ISO9075.encode(PROPERTY_NAME) + "2 descending", Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      firstNode = null;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (firstNode == null)
         {
            firstNode = n.getName();
         }
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      assertEquals("sub-" + NODE_NAME, firstNode);
      query =
         qm.createQuery(
            "select * from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%' and CONTAINS(*, 'my value')"
               + " ORDER BY jcr:score DESC", Query.SQL);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery(
            "/jcr:root/TestQuote//element(*, nt:unstructured)[jcr:contains(., 'my value')] order by jcr:score() descending",
            Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery("select * from nt:unstructured WHERE jcr:path LIKE '/TestQuote[%]/%' and CONTAINS(\""
            + PROPERTY_NAME.replaceAll("\"", "\"\"") + "\", 'my value')", Query.SQL);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query.storeAsNode("/TestQuote/myQuery");
      testRootNode.save();
      qm = session2.getWorkspace().getQueryManager();
      query = qm.getQuery(session2.getRootNode().getNode("TestQuote/myQuery"));
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query =
         qm.createQuery(
            "/jcr:root/TestQuote//element(*, nt:unstructured)[jcr:contains(@" + ISO9075.encode(PROPERTY_NAME)
               + ", 'my value')]", Query.XPATH);
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
      query.storeAsNode("/TestQuote/myQuery2");
      testRootNode.save();
      qm = session2.getWorkspace().getQueryManager();
      query = qm.getQuery(session2.getRootNode().getNode("TestQuote/myQuery2"));
      result = query.execute();
      it = result.getNodes();
      nodeFound = 0;
      while (it.hasNext())
      {
         Node n = it.nextNode();
         if (n.getName().equals(NODE_NAME))
         {
            nodeFound++;
         }
         else if (n.getName().equals("sub-" + NODE_NAME))
         {
            nodeFound++;
         }
      }
      assertEquals(2, it.getSize());
      assertEquals(2, nodeFound);
   }
}
