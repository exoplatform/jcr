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
package org.exoplatform.services.jcr.ext.distribution;

import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.ext.BaseStandaloneTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * @author <a href="mailto:nicolas.filotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestDataDistributionManager extends BaseStandaloneTest
{
   private DataDistributionManager manager;

   private Node parentNode;

   /**
    * @see org.exoplatform.services.jcr.ext.BaseStandaloneTest#setUp()
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      manager = (DataDistributionManager)container.getComponentInstanceOfType(DataDistributionManager.class);
      parentNode = root.addNode("TestDataDistributionManager");
      session.save();
   }

   /**
    * @see org.exoplatform.services.jcr.ext.BaseStandaloneTest#tearDown()
    */
   @Override
   protected void tearDown() throws Exception
   {
      manager = null;
      if (parentNode != null)
      {
         parentNode.remove();
         session.save();
         parentNode = null;
      }
      super.tearDown();
   }

   public void testDataDistributionModeNone() throws Exception
   {
      DataDistributionType type = manager.getDataDistributionType(DataDistributionMode.NONE);
      String dataId = "/a/a/a/a/";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      Node node = type.getOrCreateDataNode(parentNode, dataId);
      assertTrue(node.isSame(type.getOrCreateDataNode(parentNode, dataId)));
      assertEquals(1, node.getParent().getNodes().getSize());
      Node node2 = type.getDataNode(parentNode, dataId);
      assertTrue(node.isSame(node2));
      Node node3 = parentNode.getNode("a/a/a/a");
      assertTrue(node.isSame(node3));
      Node node4 = type.getOrCreateDataNode(parentNode, "a/a/a/b", "nt:folder");
      assertFalse(node.isSame(node4));
      assertTrue(node.getParent().isSame(node4.getParent()));
      assertTrue(node4.isNodeType("nt:folder"));
      assertTrue(node4.canAddMixin("mix:referenceable"));
      assertTrue(node4.canAddMixin("exo:privilegeable"));

      dataId = "b/a/a/a";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }

      Node node5 =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"));
      assertFalse(node.isSame(node5));
      assertFalse(node.getParent().isSame(node5.getParent()));
      assertTrue(node5.isNodeType("nt:folder"));
      assertFalse(node5.canAddMixin("mix:referenceable"));
      assertTrue(node5.canAddMixin("exo:privilegeable"));
      assertTrue(node5.getParent().isNodeType("nt:folder"));
      assertFalse(node5.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node5.getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node5.getParent().getParent().isNodeType("nt:folder"));
      assertFalse(node5.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node5.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node5.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertFalse(node5.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node5.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      Map<String, String[]> permissions = Collections.singletonMap("root", PermissionType.ALL);
      type.removeDataNode(parentNode, dataId);
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }

      dataId = "c/a/a/a";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      Node node6 =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertFalse(node.isSame(node6));
      assertFalse(node.getParent().isSame(node6.getParent()));
      assertTrue(node6.isNodeType("nt:folder"));
      assertFalse(node6.canAddMixin("mix:referenceable"));
      assertFalse(node6.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node6).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node6).getACL().getPermissions("root"));
      assertTrue(node6.getParent().isNodeType("nt:folder"));
      assertFalse(node6.getParent().canAddMixin("mix:referenceable"));
      assertFalse(node6.getParent().canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node6.getParent()).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node6.getParent()).getACL().getPermissions("root"));
      assertTrue(node6.getParent().getParent().isNodeType("nt:folder"));
      assertFalse(node6.getParent().getParent().canAddMixin("mix:referenceable"));
      assertFalse(node6.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node6.getParent().getParent()).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node6.getParent().getParent()).getACL().getPermissions("root"));
      assertTrue(node6.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertFalse(node6.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertFalse(node6.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node6.getParent().getParent().getParent()).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node6.getParent().getParent().getParent()).getACL().getPermissions("root"));
      type.removeDataNode(parentNode, dataId);
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
   }

   public void testDataDistributionModeReadable() throws Exception
   {
      DataDistributionType type = manager.getDataDistributionType(DataDistributionMode.READABLE);
      String dataId = "john.smith";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      Map<String, String[]> permissions = Collections.singletonMap("root", PermissionType.ALL);
      Node node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("j___/jo___/joh___/john.smith"));

      dataId = "bob";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("b___/bo___/bob"));

      dataId = "john___";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("j___/jo___/joh___/john___"));

      dataId = "joh___";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("j___/jo___/joh___/joh___"));

      dataId = "jo___";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("j___/jo___/jo____/jo___"));

      dataId = "j___";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("j___/j____/j_____/j___"));
      type.removeDataNode(parentNode, dataId);
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
   }

   public void testDataDistributionModeOptimized() throws Exception
   {
      DataDistributionType type = manager.getDataDistributionType(DataDistributionMode.OPTIMIZED);
      String dataId = "john.smith";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      Map<String, String[]> permissions = Collections.singletonMap("root", PermissionType.ALL);
      Node node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("1/2/s/john.smith"));

      dataId = "mary";
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
      node =
         type.getOrCreateDataNode(parentNode, dataId, "nt:folder", Collections.singletonList("mix:referenceable"),
            permissions);
      assertTrue(node.isNodeType("nt:folder"));
      assertFalse(node.canAddMixin("mix:referenceable"));
      assertFalse(node.canAddMixin("exo:privilegeable"));
      assertTrue(((ExtendedNode)node).getACL().hasPermissions());
      assertNotNull(((ExtendedNode)node).getACL().getPermissions("root"));
      assertFalse(node.getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().canAddMixin("exo:privilegeable"));
      assertFalse(node.getParent().getParent().getParent().isNodeType("nt:folder"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("mix:referenceable"));
      assertTrue(node.getParent().getParent().getParent().canAddMixin("exo:privilegeable"));
      assertTrue(node.getPath().endsWith("5/o/s/mary"));
      type.removeDataNode(parentNode, dataId);
      try
      {
         type.getDataNode(parentNode, dataId);
         fail("a PathNotFoundException is expected");
      }
      catch (PathNotFoundException e)
      {
         // expected behavior
      }
   }

   public void testMultiThreadingNone() throws Throwable
   {
      testMultiThreading(manager.getDataDistributionType(DataDistributionMode.NONE), "key/");
   }

   public void testMultiThreadingReadable() throws Throwable
   {
      testMultiThreading(manager.getDataDistributionType(DataDistributionMode.READABLE), "key-");
   }

   public void testMultiThreadingOptimized() throws Throwable
   {
      testMultiThreading(manager.getDataDistributionType(DataDistributionMode.OPTIMIZED), "");
   }

   public void testMultiThreading(final DataDistributionType type, final String dataIdPrefix) throws Throwable
   {
      final int totalElement = 20;
      final int totalTimes = 3;
      int reader = 8;
      int writer = 4;
      int remover = 2;
      final CountDownLatch startSignalWriter = new CountDownLatch(1);
      final CountDownLatch startSignalOthers = new CountDownLatch(1);
      final CountDownLatch doneSignal = new CountDownLatch(reader + writer + remover);
      final List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());
      for (int i = 0; i < writer; i++)
      {
         final int index = i;
         Thread thread = new Thread()
         {
            public void run()
            {
               Session session = null;
               try
               {
                  startSignalWriter.await();
                  for (int j = 0; j < totalTimes; j++)
                  {
                     session = repository.login(credentials, WS_NAME);
                     Node node = (Node)session.getItem(parentNode.getPath());
                     for (int i = 0; i < totalElement; i++)
                     {
                        try
                        {
                           Node n = type.getOrCreateDataNode(node, dataIdPrefix + i);
                           assertFalse("The path " + n.getPath() + " should not contain any indexes", n.getPath()
                              .contains("["));
                        }
                        catch (RepositoryException e)
                        {
                           // Ignore them, they could be due to deadlocks
                        }
                     }
                     if (index == 0 && j == 0)
                     {
                        // The cache is full, we can launch the others
                        startSignalOthers.countDown();
                     }
                     session.logout();
                     sleep(50);
                  }
               }
               catch (Throwable e)
               {
                  errors.add(e);
                  startSignalOthers.countDown();
               }
               finally
               {
                  doneSignal.countDown();
                  if (session != null)
                  {
                     session.logout();
                     session = null;
                  }
               }
            }
         };
         thread.start();
      }
      startSignalWriter.countDown();
      for (int i = 0; i < reader; i++)
      {
         Thread thread = new Thread()
         {
            public void run()
            {
               Session session = null;
               try
               {
                  startSignalOthers.await();
                  for (int j = 0; j < totalTimes; j++)
                  {
                     session = repository.login(credentials, WS_NAME);
                     Node node = (Node)session.getItem(parentNode.getPath());
                     for (int i = 0; i < totalElement; i++)
                     {
                        try
                        {
                           Node n = type.getDataNode(node, dataIdPrefix + i);
                           assertFalse("The path " + n.getPath() + " should not contain any indexes", n.getPath()
                              .contains("["));
                        }
                        catch (PathNotFoundException e)
                        {
                           // ignore me
                        }
                     }
                     session.logout();
                     sleep(50);
                  }
               }
               catch (Throwable e)
               {
                  errors.add(e);
               }
               finally
               {
                  doneSignal.countDown();
                  if (session != null)
                  {
                     session.logout();
                     session = null;
                  }
               }
            }
         };
         thread.start();
      }
      for (int i = 0; i < remover; i++)
      {
         Thread thread = new Thread()
         {
            public void run()
            {
               Session session = null;
               try
               {
                  startSignalOthers.await();
                  for (int j = 0; j < totalTimes; j++)
                  {
                     session = repository.login(credentials, WS_NAME);
                     Node node = (Node)session.getItem(parentNode.getPath());
                     for (int i = 0; i < totalElement; i++)
                     {
                        try
                        {
                           type.removeDataNode(node, dataIdPrefix + i);
                        }
                        catch (RepositoryException e)
                        {
                           // Ignore them, they could be due to deadlocks
                        }
                     }
                     session.logout();
                     sleep(50);
                  }
               }
               catch (Throwable e)
               {
                  errors.add(e);
               }
               finally
               {
                  doneSignal.countDown();
                  if (session != null)
                  {
                     session.logout();
                     session = null;
                  }
               }
            }
         };
         thread.start();
      }
      doneSignal.await();
      for (int i = 0; i < totalElement; i++)
      {
         type.removeDataNode(parentNode, dataIdPrefix + i);
      }
      for (int i = 0; i < totalElement; i++)
      {
         try
         {
            type.getDataNode(parentNode, dataIdPrefix + i);
            fail("The node should be removed");
         }
         catch (PathNotFoundException e)
         {
            // ignore me
         }
      }
      for (int i = 0; i < totalElement; i++)
      {
         Node n = type.getOrCreateDataNode(parentNode, dataIdPrefix + i);
         assertFalse("The path " + n.getPath() + " should not contain any indexes", n.getPath().contains("["));
      }
      if (!errors.isEmpty())
      {
         for (Throwable e : errors)
         {
            e.printStackTrace();
         }
         throw errors.get(0);
      }
   }

   public void testMigration() throws Exception
   {
      Node rootNode = session.getRootNode().addNode("testRoot");
      rootNode.addNode("a").setProperty("a", "a");
      rootNode.addNode("bob").setProperty("bob", "bob");
      rootNode.addNode("john.smith").setProperty("john.smith", "john.smith");
      rootNode.addNode("joiv").setProperty("joiv", "joiv");
      rootNode.addNode("bonjov").setProperty("bonjov", "bonjov");
      rootNode.addNode("anatoliy.bazko").setProperty("anatoliy.bazko", "anatoliy.bazko");
      rootNode.getSession().save();

      manager.getDataDistributionType(DataDistributionMode.READABLE).migrate(rootNode);

      assertTrue(rootNode.hasNode("a"));
      assertFalse(rootNode.hasNode("bob"));
      assertFalse(rootNode.hasNode("john.smith"));
      assertFalse(rootNode.hasNode("joiv"));
      assertFalse(rootNode.hasNode("bonjov"));
      assertFalse(rootNode.hasNode("anatoliy.bazko"));

      assertTrue(rootNode.hasNode("b___/bo___/bob"));
      assertTrue(rootNode.hasNode("j___/jo___/joh___/john.smith"));
      assertTrue(rootNode.hasNode("j___/jo___/joi___/joiv"));
      assertTrue(rootNode.hasNode("b___/bo___/bon___/bonjov"));
      assertTrue(rootNode.hasNode("a___/an___/ana___/anatoliy.bazko"));

      assertTrue(rootNode.getNode("a").hasProperty("a"));
      assertTrue(rootNode.getNode("b___/bo___/bob").hasProperty("bob"));
      assertTrue(rootNode.getNode("j___/jo___/joh___/john.smith").hasProperty("john.smith"));
      assertTrue(rootNode.getNode("j___/jo___/joi___/joiv").hasProperty("joiv"));
      assertTrue(rootNode.getNode("b___/bo___/bon___/bonjov").hasProperty("bonjov"));
      assertTrue(rootNode.getNode("a___/an___/ana___/anatoliy.bazko").hasProperty("anatoliy.bazko"));

      // shoud not be any changes
      manager.getDataDistributionType(DataDistributionMode.READABLE).migrate(rootNode);

      assertTrue(rootNode.hasNode("a"));
      assertFalse(rootNode.hasNode("bob"));
      assertFalse(rootNode.hasNode("john.smith"));
      assertFalse(rootNode.hasNode("joiv"));
      assertFalse(rootNode.hasNode("bonjov"));
      assertFalse(rootNode.hasNode("anatoliy.bazko"));

      assertTrue(rootNode.hasNode("b___/bo___/bob"));
      assertTrue(rootNode.hasNode("j___/jo___/joh___/john.smith"));
      assertTrue(rootNode.hasNode("j___/jo___/joi___/joiv"));
      assertTrue(rootNode.hasNode("b___/bo___/bon___/bonjov"));
      assertTrue(rootNode.hasNode("a___/an___/ana___/anatoliy.bazko"));

      assertTrue(rootNode.getNode("a").hasProperty("a"));
      assertTrue(rootNode.getNode("b___/bo___/bob").hasProperty("bob"));
      assertTrue(rootNode.getNode("j___/jo___/joh___/john.smith").hasProperty("john.smith"));
      assertTrue(rootNode.getNode("j___/jo___/joi___/joiv").hasProperty("joiv"));
      assertTrue(rootNode.getNode("b___/bo___/bon___/bonjov").hasProperty("bonjov"));
      assertTrue(rootNode.getNode("a___/an___/ana___/anatoliy.bazko").hasProperty("anatoliy.bazko"));
   }
}
