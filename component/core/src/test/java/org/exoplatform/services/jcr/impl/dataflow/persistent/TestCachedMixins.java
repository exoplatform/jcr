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
package org.exoplatform.services.jcr.impl.dataflow.persistent;

import org.exoplatform.services.jcr.JcrImplBaseTest;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.PropertyImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;

/**
 * Created by The eXo Platform SAS
 * 
 * 09.01.2007
 * 
 * We testing a workspace cache with mixin types. A cache work in shadow, but we try to do usecases
 * of possible wrong cache work.
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestCachedMixins.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestCachedMixins extends JcrImplBaseTest
{

   public final String TEST_NODE_NAME = "cache_test";

   protected NodeImpl testNode;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      this.testNode = (NodeImpl)session.getRootNode().addNode(TEST_NODE_NAME);

      this.session.save();
   }

   @Override
   protected void tearDown() throws Exception
   {
      this.testNode.remove();
      this.session.save();

      super.tearDown();
   }

   public void testMixinAdd() throws Exception
   {
      this.testNode.addMixin("mix:referenceable");

      this.session.save();

      String uuid = null;

      testNode = (NodeImpl)session.getRootNode().getNode(TEST_NODE_NAME);

      try
      {
         uuid = testNode.getUUID();
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         fail("Node isn't a referenceable, but must");
      }

      try
      {
         uuid = ((NodeImpl)session.getItem("/" + TEST_NODE_NAME)).getUUID();
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         fail("Node isn't a referenceable, but must. Access from Session.geItem()");
      }

      SessionImpl anotherSession = (SessionImpl)repository.login(this.credentials);
      NodeImpl anotherRoot = (NodeImpl)anotherSession.getRootNode();

      try
      {
         NodeImpl aNode = (NodeImpl)anotherRoot.getNode(TEST_NODE_NAME);
         assertEquals("UUIDs must equals", uuid, aNode.getUUID());
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         fail("Node isn't a referenceable, but must");
      }

      try
      {
         NodeImpl aNode = (NodeImpl)anotherSession.getItem("/" + TEST_NODE_NAME);
         assertEquals("UUIDs must equals. Access from Session.geItem()", uuid, aNode.getUUID());
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         fail("Node isn't a referenceable, but must. Access from Session.geItem()");
      }
   }

   public void testMixinAddRemove() throws Exception
   {
      this.testNode.addMixin("mix:referenceable");

      this.session.save();

      this.testNode.removeMixin("mix:referenceable");

      this.session.save();

      testNode = (NodeImpl)session.getRootNode().getNode(TEST_NODE_NAME);

      try
      {
         testNode.getUUID();
         fail("Node must be not referenceable, but it such.");
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         // ok
      }

      try
      {
         ((NodeImpl)session.getItem("/" + TEST_NODE_NAME)).getUUID();
         fail("Node must be not referenceable, but it such. Access from Session.geItem().");
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         // ok
      }

      SessionImpl anotherSession = (SessionImpl)repository.login(this.credentials /*
                                                                                                   * session.getCredentials
                                                                                                   * ()
                                                                                                   */);
      NodeImpl anotherRoot = (NodeImpl)anotherSession.getRootNode();

      NodeImpl aNode = (NodeImpl)anotherRoot.getNode(TEST_NODE_NAME);
      try
      {
         aNode.getUUID();
         fail("Node must be not referenceable, but it such.");
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         // ok
      }

      aNode = (NodeImpl)anotherSession.getItem("/" + TEST_NODE_NAME);
      try
      {
         aNode.getUUID();
         fail("Node must be not referenceable, but it such. Access from Session.geItem().");
      }
      catch (UnsupportedRepositoryOperationException e)
      {
         // ok
      }
   }

   public void testFewMixinAdd() throws Exception
   {

      String[] mixins = new String[]{"mix:referenceable", "mix:lockable"};

      this.testNode.addMixin(mixins[0]);
      this.session.save();

      this.testNode.addMixin(mixins[1]);
      this.session.save();

      this.testNode.lock(true, false);

      checkMixins(mixins, (NodeImpl)session.getRootNode().getNode(TEST_NODE_NAME));
      checkMixins(mixins, (NodeImpl)session.getItem("/" + TEST_NODE_NAME));

      SessionImpl anotherSession = (SessionImpl)repository.login(this.credentials /*
                                                                                                   * session.getCredentials
                                                                                                   * ()
                                                                                                   */);

      checkMixins(mixins, (NodeImpl)anotherSession.getRootNode().getNode(TEST_NODE_NAME));
      checkMixins(mixins, (NodeImpl)anotherSession.getItem("/" + TEST_NODE_NAME));
   }

   public void testFewMixinAdd_ObjectInHand() throws Exception
   {

      String[] mixins = new String[]{"mix:referenceable", "mix:lockable"};

      NodeImpl node1 = (NodeImpl)this.testNode.addNode("node-1");
      this.session.save();

      node1.addMixin(mixins[0]);
      node1.addMixin(mixins[1]);
      this.testNode.save();

      node1.lock(true, false);

      checkMixins(mixins, node1);
   }

   public void testFewMixinAddRemove() throws Exception
   {

      String[] mixins = new String[]{"mix:referenceable", "mix:lockable"};
      String[] finalMixins = new String[]{"mix:lockable"};

      this.testNode.addMixin(mixins[0]);
      this.session.save();

      this.testNode.addMixin(mixins[1]);
      this.session.save();

      this.testNode.lock(true, false);

      this.testNode.removeMixin(mixins[0]);
      this.session.save();

      checkMixins(finalMixins, (NodeImpl)session.getRootNode().getNode(TEST_NODE_NAME));
      checkMixins(finalMixins, (NodeImpl)session.getItem("/" + TEST_NODE_NAME));

      SessionImpl anotherSession = (SessionImpl)repository.login(this.credentials /*
                                                                                                   * session.getCredentials
                                                                                                   * ()
                                                                                                   */);

      checkMixins(finalMixins, (NodeImpl)anotherSession.getRootNode().getNode(TEST_NODE_NAME));
      checkMixins(finalMixins, (NodeImpl)anotherSession.getItem("/" + TEST_NODE_NAME));
   }

   public void testFewMixinAddRemove_ObjectInHand() throws Exception
   {

      String[] mixins = new String[]{"mix:referenceable", "mix:lockable"};
      String[] finalMixins = new String[]{"mix:lockable"};

      NodeImpl node1 = (NodeImpl)this.testNode.addNode("node-1");
      this.session.save();

      node1.addMixin(mixins[0]);
      node1.addMixin(mixins[1]);
      this.testNode.save();

      node1.lock(true, false);

      node1.removeMixin(mixins[0]);
      this.testNode.save();

      checkMixins(finalMixins, node1);
   }

   public void testMixinAddTransient() throws Exception
   {

      String[] mixins = new String[]{"mix:referenceable", "mix:lockable"};
      String[] finalMixins = new String[]{"mix:lockable"};

      NodeImpl node1 = (NodeImpl)this.testNode.addNode("node-1");
      this.session.save();

      node1.addMixin(mixins[0]);
      node1.addMixin(mixins[1]);

      PropertyImpl uuid = (PropertyImpl)node1.getProperty("jcr:uuid");

      try
      {
         NodeImpl sameNode1 = (NodeImpl)session.getNodeByUUID(uuid.getString());
         checkMixins(mixins, sameNode1);

         assertEquals("Nodes must be same", node1, sameNode1);
      }
      catch (RepositoryException e)
      {
         fail("Transient node must be accessible by uuid. " + e);
      }

      try
      {
         NodeImpl sameNode1 = (NodeImpl)session.getItem(node1.getPath());
         checkMixins(mixins, sameNode1);

         assertEquals("Nodes must be same", node1, sameNode1);
      }
      catch (RepositoryException e)
      {
         fail("Transient node must be accessible by path. " + e);
      }

      this.testNode.save();

      node1.removeMixin(mixins[0]);
      this.testNode.save();

      checkMixins(finalMixins, node1);
   }

}
