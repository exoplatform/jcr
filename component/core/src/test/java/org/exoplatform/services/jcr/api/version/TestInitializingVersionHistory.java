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
package org.exoplatform.services.jcr.api.version;

import javax.jcr.AccessDeniedException;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.core.CredentialsImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:geaz@users.sourceforge.net">Gennady Azarenkov</a>
 * @version $Id: TestInitializingVersionHistory.java 11908 2008-03-13 16:00:12Z
 *          ksm $
 */

public class TestInitializingVersionHistory
   extends JcrAPIBaseTest
{

   public void testNewVersionable() throws Exception
   {

      Node node;
      try
      {
         node = root.addNode("node-v", "mix:versionable");
         fail("AddNode ConstraintViolationException should be thrown as type is not primary!");
      }
      catch (ConstraintViolationException e)
      {
      }

      node = root.addNode("node-v", "nt:unstructured");

      node.addMixin("mix:versionable");
      assertEquals(1, node.getMixinNodeTypes().length);
      assertEquals("mix:versionable", node.getMixinNodeTypes()[0].getName());
      assertEquals("nt:unstructured", node.getPrimaryNodeType().getName());

      assertNotNull(node.getProperty("jcr:uuid").toString());

      assertTrue(node.isNodeType("mix:versionable"));

      root.save();

      node = root.getNode("node-v");

      assertNotNull(node.getProperty("jcr:isCheckedOut"));
      assertTrue(node.getProperty("jcr:isCheckedOut").getBoolean());

      assertNotNull(node.getProperty("jcr:predecessors"));
      assertEquals(1, node.getProperty("jcr:predecessors").getValues().length);

      assertNotNull(node.getProperty("jcr:versionHistory"));
      String vhRef = node.getProperty("jcr:versionHistory").getString();

      assertNotNull(node.getProperty("jcr:baseVersion"));
      String bvRef = node.getProperty("jcr:baseVersion").getString();

      assertTrue(node.isNodeType("mix:versionable"));

      VersionHistory vh = node.getVersionHistory();
      if (log.isDebugEnabled())
         log.debug(" node " + node.getUUID() + " " + vh.getProperties().getSize());
      assertEquals(vh.getUUID(), vhRef);
      assertNotNull(vh.getVersionableUUID());
      assertNotNull(vh.getNode("jcr:rootVersion"));

      Version bv = node.getBaseVersion();
      assertEquals(bv.getUUID(), bvRef);
      assertNotNull(bv.getProperty("jcr:created"));

   }

   public void testNewVersionable1() throws Exception
   {

      Node node;

      node = root.addNode("node-v1", "nt:unstructured");

      root.save();

      // another session
      SessionImpl session1 = (SessionImpl) repository.login(new CredentialsImpl("exo", "exo".toCharArray()));

      Node root1 = session1.getRootNode();
      node = root1.getNode("node-v1");
      node.addMixin("mix:versionable");

      assertNotNull(node.getProperty("jcr:versionHistory"));

      assertTrue(node.isNodeType("mix:versionable"));

      // NodeDumpVisitor v = new NodeDumpVisitor();
      // node.accept(v);
      // log.debug("X "+node.getProperty("jcr:baseVersion"));
      // log.debug("XYYY >>> "+v.getDump());
      //log.debug("XYYYYYYYYYYYYYYYY "+session1.getTransientNodesManager().dump())
      // ;

      root1.save();

      node = root1.getNode("node-v1");

      assertNotNull(node.getVersionHistory());

   }

   public void testCheckinAfterNewVersionable() throws Exception
   {
      Node node = root.addNode("node1", "nt:unstructured");

      node.addMixin("mix:versionable");

      root.save();

      Session s1 = repository.login(this.credentials, "ws");

      Node node1 = s1.getRootNode().getNode("node1");
      assertTrue(node1.getProperty("jcr:isCheckedOut").getBoolean());
      assertTrue(node1.isCheckedOut());

      node1.checkin();

      assertFalse(node1.getProperty("jcr:isCheckedOut").getBoolean());
      assertFalse(node1.isCheckedOut());

      node1.checkout();

      assertTrue(node1.getProperty("jcr:isCheckedOut").getBoolean());
      assertTrue(node1.isCheckedOut());

   }

   public void testBaseVersionAccessible() throws Exception
   {

      Node node;

      node = root.addNode("testBaseVersionAccessible", "nt:unstructured");

      node.addMixin("mix:versionable");
      session.save();
      node.checkin();

      // another session
      SessionImpl session1 = (SessionImpl) repository.login(new CredentialsImpl("__anonim", "exo".toCharArray()));
      node = (Node) session1.getItem("/testBaseVersionAccessible");
      assertEquals("1", node.getBaseVersion().getName());
      try
      {
         // [PN] 02.08.06
         // versionStorage's children should have READ permission for all
         node = (Node) session1.getItem("/jcr:system/jcr:versionStorage/" + node.getVersionHistory().getUUID() + "/1");
         // fail("AccessDeniedException should have been thrown");
      }
      catch (AccessDeniedException e)
      {
         fail("versionStorage's children should have READ permission for all");
      }
   }
}
