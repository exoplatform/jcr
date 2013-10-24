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
package org.exoplatform.services.jcr.api.version;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.impl.Constants;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestVersionableThreadSafety
{
   protected static final int TOTAL_THREADS = 3;

   // We need it as a variable since to be compatible with JUnit 4, the class must not extend TestCase so we
   // cannot extend BaseVersionTest. But as we need all the logic inside, we use it as a variable
   private BaseVersionTest test;

   private ExtendedNode vs;
   private long totalNodes;
   private Node testMultiThreading;
   private String path;
   private final AtomicInteger step = new AtomicInteger();

   @Rule
   public ContiPerfRule rule = new ContiPerfRule();

   private CyclicBarrier startSignal = new CyclicBarrier(TOTAL_THREADS);

   @Before
   public void setUp() throws Exception
   {
      test = new BaseVersionTest();
      test.setUp();
      vs = (ExtendedNode)test.getSession().getNodeByIdentifier(Constants.VERSIONSTORAGE_UUID);
      totalNodes = vs.getNodesCount();
   }

   @After
   public void tearDown() throws Exception
   {
      if (testMultiThreading != null)
      {
         Node parent = testMultiThreading.getParent();
         testMultiThreading.remove();
         parent.save();
         if (parent.getSession() != test.getSession())
         {
            parent.getSession().logout();
         }
      }
      test.tearDown();
      test = null;
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading1OnWS1() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         Session s = test.getRepository().login(test.getCredentials(), "ws1");
         testMultiThreading = s.getRootNode().addNode("testMultiThreading");
         s.save();
         path = testMultiThreading.getPath();
      }
      startSignal.await();
      boolean beforeAwait = true;
      Session session = null;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws1");
         Node testMultiThreading = (Node)session.getItem(path);
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (ItemExistsException e)
      {
         // The mixin has been added several times so it is normal to have such issue
      }
      catch (RepositoryException e)
      {
         if (beforeAwait)
            throw e;
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(1, 2))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // reload the node
         testMultiThreading.getSession().refresh(false);
         testMultiThreading = (Node)testMultiThreading.getSession().getItem(path);
         assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
         assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading2OnWS1() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         Session s = test.getRepository().login(test.getCredentials(), "ws1");
         testMultiThreading = s.getRootNode().addNode("testMultiThreading");
         testMultiThreading.addMixin("mix:referenceable");
         s.save();
         path = testMultiThreading.getPath();
      }
      startSignal.await();
      Session session = null;
      boolean beforeAwait = true;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws1");
         Node testMultiThreading = (Node)session.getItem(path);
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (RepositoryException e)
      {
         // Concurrent modifications of the property mixinTypes
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(1, 2))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // reload the node
         testMultiThreading.getSession().refresh(false);
         testMultiThreading = (Node)testMultiThreading.getSession().getItem(path);
         assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
         Set<String> mixins = new HashSet<String>();
         mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
         mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
         assertTrue(mixins.contains("mix:referenceable"));
         assertTrue(mixins.contains("mix:versionable"));
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading3OnWS1() throws Exception
   {
      Session session = null;
      boolean beforeAwait = true;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws1");
         Node testMultiThreading = session.getRootNode().addNode("testMultiThreading");
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (ItemExistsException e)
      {
         // The node has not been created so far
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(0, 1))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // load the node
         Session s = test.getRepository().login(test.getCredentials(), "ws1");
         testMultiThreading = (Node)s.getItem("/testMultiThreading");
         assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
         assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
         assertFalse(s.itemExists("/testMultiThreading[2]"));
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading4OnWS1() throws Exception
   {
      Session session = null;
      boolean beforeAwait = true;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws1");
         Node testMultiThreading = session.getRootNode().addNode("testMultiThreading");
         testMultiThreading.addMixin("mix:referenceable");
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (ItemExistsException e)
      {
         // The node has not been created so far
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(0, 1))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // load the node
         Session s = test.getRepository().login(test.getCredentials(), "ws1");
         testMultiThreading = (Node)s.getItem("/testMultiThreading");
         assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
         Set<String> mixins = new HashSet<String>();
         mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
         mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
         assertTrue(mixins.contains("mix:referenceable"));
         assertTrue(mixins.contains("mix:versionable"));
         assertFalse(s.itemExists("/testMultiThreading[2]"));
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading1OnWS() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         testMultiThreading = test.getRoot().addNode("testMultiThreading");
         test.getRoot().save();
         path = testMultiThreading.getPath();
      }
      startSignal.await();
      boolean beforeAwait = true;
      Session session = null;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws");
         Node testMultiThreading = (Node)session.getItem(path);
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (ItemExistsException e)
      {
         // The mixin has been added several times so it is normal to have such issue
      }
      catch (RepositoryException e)
      {
         if (beforeAwait)
            throw e;
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(1, 2))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // reload the node
         testMultiThreading.getSession().refresh(false);
         testMultiThreading = (Node)testMultiThreading.getSession().getItem(path);
         assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
         assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading2OnWS() throws Exception
   {
      if (step.compareAndSet(0, 1))
      {
         testMultiThreading = test.getRoot().addNode("testMultiThreading");
         testMultiThreading.addMixin("mix:referenceable");
         test.getRoot().save();
         path = testMultiThreading.getPath();
      }
      startSignal.await();
      Session session = null;
      boolean beforeAwait = true;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws");
         Node testMultiThreading = (Node)session.getItem(path);
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (RepositoryException e)
      {
         // Concurrent modifications of the property mixinTypes
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(1, 2))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // reload the node
         testMultiThreading.getSession().refresh(false);
         testMultiThreading = (Node)testMultiThreading.getSession().getItem(path);
         assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
         Set<String> mixins = new HashSet<String>();
         mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
         mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
         assertTrue(mixins.contains("mix:referenceable"));
         assertTrue(mixins.contains("mix:versionable"));
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading3OnWS() throws Exception
   {
      Session session = null;
      boolean beforeAwait = true;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws");
         Node testMultiThreading = session.getRootNode().addNode("testMultiThreading");
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (ItemExistsException e)
      {
         // The node has not been created so far
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(0, 1))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // load the node
         testMultiThreading = (Node)test.getSession().getItem("/testMultiThreading");
         assertEquals(1, testMultiThreading.getMixinNodeTypes().length);
         assertEquals("mix:versionable", testMultiThreading.getMixinNodeTypes()[0].getName());
         assertFalse(test.getSession().itemExists("/testMultiThreading[2]"));
      }
   }

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testMultiThreading4OnWS() throws Exception
   {
      Session session = null;
      boolean beforeAwait = true;
      try
      {
         session = test.getRepository().login(test.getCredentials(), "ws");
         Node testMultiThreading = session.getRootNode().addNode("testMultiThreading");
         testMultiThreading.addMixin("mix:referenceable");
         testMultiThreading.addMixin("mix:versionable");
         startSignal.await();
         beforeAwait = false;
         session.save();
      }
      catch (ItemExistsException e)
      {
         // The node has not been created so far
      }
      finally
      {
         if (beforeAwait)
         {
            try
            {
               startSignal.await();
            }
            catch (Exception e)
            {
               // ignore me
            }
         }
         if (session != null)
            session.logout();
      }
      startSignal.await();
      if (step.compareAndSet(0, 1))
      {
         assertEquals(totalNodes + 1, vs.getNodesCount());
         assertEquals(totalNodes + 1, vs.getNodes().getSize());
         // load the node
         testMultiThreading = (Node)test.getSession().getItem("/testMultiThreading");
         assertEquals(2, testMultiThreading.getMixinNodeTypes().length);
         Set<String> mixins = new HashSet<String>();
         mixins.add(testMultiThreading.getMixinNodeTypes()[0].getName());
         mixins.add(testMultiThreading.getMixinNodeTypes()[1].getName());
         assertTrue(mixins.contains("mix:referenceable"));
         assertTrue(mixins.contains("mix:versionable"));
         assertFalse(test.getSession().itemExists("/testMultiThreading[2]"));
      }
   }
}
