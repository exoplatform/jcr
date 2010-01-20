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
import org.exoplatform.services.jcr.datamodel.InternalQName;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.PropertyData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.dataflow.TransientNodeData;
import org.exoplatform.services.jcr.impl.dataflow.TransientPropertyData;
import org.exoplatform.services.jcr.impl.dataflow.TransientValueData;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.jcr.PropertyType;

/**
 * Created by The eXo Platform SAS
 * 
 * Date: 19.06.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestLinkedCacheMultithread.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestLinkedCacheMultithread extends JcrImplBaseTest
{

   protected static Log log = ExoLogger.getLogger("jcr.TestLinkedCacheMultithread");

   private LinkedWorkspaceStorageCacheImpl cache;

   // private LRUWorkspaceStorageCacheImpl cache;

   private NodeData rootData;

   class Reader extends Thread
   {
      final NodeData[] nodes;

      final int nodesMaxIndex;

      final Random random;

      int itemsProcessed = 0;

      volatile boolean execute = true;

      Reader(NodeData[] nodes, String name)
      {
         this.nodes = nodes;
         this.random = new Random();
         this.nodesMaxIndex = nodes.length - 1;
         super.setName(name);
      }

      public void run()
      {
         // log.info("START");
         try
         {
            while (execute)
            {
               NodeData rndNode = nodes[random.nextInt(nodesMaxIndex)];
               if (random.nextBoolean())
               {
                  // by id
                  NodeData n = (NodeData)cache.get(rndNode.getIdentifier());
                  if (n != null)
                     assertEquals(rndNode.getIdentifier(), n.getIdentifier());
               }
               else
               {
                  // by parent + name
                  NodeData n =
                     (NodeData)cache.get(rndNode.getParentIdentifier(), rndNode.getQPath().getEntries()[rndNode
                        .getQPath().getEntries().length - 1]);
                  if (n != null)
                     assertEquals(rndNode.getIdentifier(), n.getIdentifier());
               }
               itemsProcessed++;
            }
         }
         catch (Exception e)
         {
            log.error(getName() + " " + e, e);
         }
         // log.info("FINISH");
      }

      public void cancel()
      {
         this.execute = false;
      }
   }

   class Writer extends Thread
   {
      final NodeData[] parentNodes;

      final int nodesMaxIndex;

      final Random random;

      final long putTimeout;

      int itemsProcessed = 0;

      volatile boolean execute = true;

      Writer(NodeData[] parentNodes, String name, long putTimeout)
      {
         this.parentNodes = parentNodes;
         this.random = new Random();
         this.nodesMaxIndex = parentNodes.length - 1;
         this.putTimeout = putTimeout;
         super.setName(name);
      }

      public void run()
      {
         // log.info("START");
         try
         {
            while (execute)
            {
               int next = random.nextInt(nodesMaxIndex);
               NodeData rndNode = parentNodes[next];
               if (random.nextBoolean())
               {
                  // put single item
                  if (random.nextBoolean())
                  {
                     // node
                     cache.put(new TransientNodeData(QPath.makeChildPath(rndNode.getQPath(), InternalQName
                        .parse("[]childNode-" + next)), IdGenerator.generate(), 1, Constants.NT_UNSTRUCTURED,
                        new InternalQName[0], 1, IdGenerator.generate(), rndNode.getACL()));
                  }
                  else
                  {
                     TransientPropertyData pd =
                        new TransientPropertyData(QPath.makeChildPath(rndNode.getQPath(), InternalQName
                           .parse("[]property-" + next)), IdGenerator.generate(), 1, PropertyType.STRING, rndNode
                           .getIdentifier(), false, new TransientValueData("prop data"));
                     cache.put(pd);
                  }
                  itemsProcessed++;
               }
               else
               {
                  // put list of childs
                  if (random.nextBoolean())
                  {
                     // nodes
                     List<NodeData> cn = createNodesData(rndNode, 100);
                     cache.addChildNodes(rndNode, cn);
                     itemsProcessed += cn.size();
                  }
                  else
                  {
                     // properties w/o value
                     List<PropertyData> cp = createPropertiesData(rndNode, 100);
                     cache.addChildProperties(rndNode, cp);
                     itemsProcessed += cp.size();
                  }
               }

               Thread.sleep(putTimeout);
            }
         }
         catch (Exception e)
         {
            log.error(getName() + " " + e, e);
         }
         // log.info("FINISH");
      }

      public void cancel()
      {
         this.execute = false;
      }
   }

   class Remover extends Thread
   {
      final NodeData[] nodes;

      final int nodesMaxIndex;

      final Random random;

      final long putTimeout;

      int itemsProcessed = 0;

      volatile boolean execute = true;

      Remover(NodeData[] nodes, String name, long putTimeout)
      {
         this.nodes = nodes;
         this.random = new Random();
         this.nodesMaxIndex = nodes.length - 1;
         this.putTimeout = putTimeout;
         super.setName(name);
      }

      public void run()
      {
         // log.info("START");
         try
         {
            while (execute)
            {
               NodeData rndNode = nodes[random.nextInt(nodesMaxIndex)];
               if (random.nextBoolean())
               {
                  // remove child node
                  List<NodeData> cns = cache.getChildNodes(rndNode);
                  if (cns != null)
                     cache.remove(cns.get(0));
               }
               else
               {
                  // remove child property
                  List<PropertyData> cps = cache.getChildProperties(rndNode);
                  if (cps != null)
                     cache.remove(cps.get(0));
               }
               itemsProcessed++;

               Thread.sleep(putTimeout);
            }
         }
         catch (Exception e)
         {
            log.error(getName() + " " + e, e);
         }
         // log.info("FINISH");
      }

      public void cancel()
      {
         this.execute = false;
      }
   }

   class Locker extends Thread
   {

      final int timeout;

      Locker(int timeout)
      {
         super("Locker-" + timeout);
         this.timeout = timeout;
      }

      public void run()
      {
         synchronized (cache)
         {
            try
            {
               log.info("sleep...");
               Thread.sleep(timeout);
               log.info("done");
            }
            catch (InterruptedException e)
            {
               log.error(getName() + " " + e, e);
            }
         }
      }
   }

   @Override
   public void setUp() throws Exception
   {
      super.setUp();

      // cache = new LinkedWorkspaceStorageCacheImpl((WorkspaceEntry)
      // session.getContainer().getComponentInstanceOfType(WorkspaceEntry.class));
      // cache = new LRUWorkspaceStorageCacheImpl("testLoad_cache", true, 100 * 1024, 120, 5 * 60000,
      // 30000, false);
      cache =
         new LinkedWorkspaceStorageCacheImpl("testLoad_cache", true, 100 * 1024, 120, 5 * 60000, 30000, false, true, 0,
            true);

      rootData = (NodeData)((NodeImpl)root).getData();
   }

   private List<NodeData> createNodesData(NodeData parent, int count) throws Exception
   {

      List<NodeData> nodes = new ArrayList<NodeData>();

      for (int i = 1; i <= count; i++)
      {
         nodes.add(new TransientNodeData(QPath.makeChildPath(parent.getQPath(), InternalQName.parse("[]node" + i)),
            IdGenerator.generate(), 1, Constants.NT_UNSTRUCTURED, new InternalQName[0], 1, IdGenerator.generate(),
            parent.getACL()));
      }

      return nodes;
   }

   /**
    * properties w/o value.
    * 
    * @param parent
    * @param count
    * @return
    * @throws Exception
    */
   private List<PropertyData> createPropertiesData(NodeData parent, int count) throws Exception
   {

      List<PropertyData> props = new ArrayList<PropertyData>();

      for (int i = 1; i <= count; i++)
      {
         TransientPropertyData pd =
            new TransientPropertyData(QPath.makeChildPath(parent.getQPath(), InternalQName.parse("[]property-" + i)),
               IdGenerator.generate(), 1, PropertyType.STRING, parent.getIdentifier(), false, new TransientValueData(
                  "prop data"));
         props.add(pd);
      }

      return props;
   }

   private List<NodeData> prepare() throws Exception
   {
      // prepare
      final List<NodeData> nodes1 = createNodesData(rootData, 100);

      cache.put(rootData);
      for (NodeData n : nodes1)
      {
         cache.put(n);
      }
      cache.addChildNodes(rootData, nodes1); // re-put as childs

      final List<NodeData> nodes2 = createNodesData(nodes1.get(5), 250);
      cache.put(nodes1.get(5));
      for (NodeData n : nodes2)
      {
         cache.put(n);
      }
      cache.addChildNodes(rootData, nodes2); // re-put as childs

      final List<NodeData> nodes = new ArrayList<NodeData>();
      nodes.addAll(nodes1);
      nodes.addAll(nodes2);

      return nodes;
   }

   public void testDummy() throws Exception
   {
   }

   public void _testGet() throws Exception
   {

      List<NodeData> nodes = prepare();

      Set<Reader> readers = new HashSet<Reader>();
      long start = System.currentTimeMillis();
      try
      {
         // create readers
         for (int t = 1; t <= 200; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Reader r = new Reader(ns, "reader #" + t);
            readers.add(r);
            r.start();
         }
         log.info("Started");
         Thread.sleep(30 * 1000);
         log.info("Done");
      }
      finally
      {
         // join
         for (Reader r : readers)
         {
            r.cancel();
            r.join();
         }

         // debug result
         long totalRead = 0;
         for (Reader r : readers)
         {
            totalRead += r.itemsProcessed;
            // log.info(r.getName() + " " + (r.itemsProcessed));
         }

         long time = System.currentTimeMillis() - start;
         double speed = totalRead * 1d / time;
         log.info("Total read " + totalRead + ", speed " + speed + "read/sec., time " + (time / 1000d) + "sec");
      }
   }

   public void _testPut() throws Exception
   {

      // put any stuff
      List<NodeData> nodes = prepare();

      Set<Writer> writers = new HashSet<Writer>();
      try
      {
         // create readers
         for (int t = 1; t <= 100; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Writer r = new Writer(ns, "writer #" + t, 50);
            writers.add(r);
            r.start();
         }

         Thread.sleep(5 * 60 * 1000);
      }
      finally
      {
         // join

         for (Writer w : writers)
         {
            w.cancel();
            w.join();
         }

         // debug result
         for (Writer w : writers)
         {
            log.info(w.getName() + " " + (w.itemsProcessed));
         }
      }
   }

   public void _testGetPut() throws Exception
   {

      List<NodeData> nodes = prepare();

      Set<Reader> readers = new HashSet<Reader>();
      Set<Writer> writers = new HashSet<Writer>();
      try
      {
         // create readers
         for (int t = 1; t <= 10; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Reader r = new Reader(ns, "reader #" + t);
            readers.add(r);
            r.start();
         }

         // create writers
         for (int t = 1; t <= 10; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Writer w = new Writer(ns, "writer #" + t, 250);
            writers.add(w);
            w.start();
         }

         Thread.sleep(5 * 60 * 1000);
      }
      finally
      {
         // join

         for (Writer w : writers)
         {
            w.cancel();
            w.join();
         }

         for (Reader r : readers)
         {
            r.cancel();
            r.join();
         }

         // debug result
         for (Reader r : readers)
         {// cache.getSize()
            log.info(r.getName() + " " + (r.itemsProcessed));
         }

         for (Writer w : writers)
         {
            log.info(w.getName() + " " + (w.itemsProcessed));
         }
      }
   }

   public void _testGetPutRemove() throws Exception
   {

      List<NodeData> nodes = prepare();

      Set<Reader> readers = new HashSet<Reader>();
      Set<Writer> writers = new HashSet<Writer>();
      Set<Remover> removers = new HashSet<Remover>();
      long start = System.currentTimeMillis();
      try
      {
         // create readers
         for (int t = 1; t <= 100; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Reader r = new Reader(ns, "reader #" + t);
            readers.add(r);
            r.start();
         }

         // create writers
         for (int t = 1; t <= 5; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Writer w = new Writer(ns, "writer #" + t, 1000);
            writers.add(w);
            w.start();
         }

         // create removers
         for (int t = 1; t <= 5; t++)
         {
            NodeData[] ns = new NodeData[nodes.size()];
            nodes.toArray(ns);
            Remover r = new Remover(ns, "remover #" + t, 1000);
            removers.add(r);
            r.start();
         }

         log.info("Wait....");

         // Thread.sleep(50400 * 1000); // 50400sec = 14h
         Thread.sleep(20 * 1000); // 20sec.

         log.info("Stopping");
      }
      finally
      {
         // join
         for (Remover r : removers)
         {
            r.cancel();
            r.join();
         }

         for (Writer w : writers)
         {
            w.cancel();
            w.join();
         }

         for (Reader r : readers)
         {
            r.cancel();
            r.join();
         }

         // debug result
         long stop = System.currentTimeMillis() - start;
         long totalRead = 0;
         for (Reader r : readers)
         {
            totalRead += r.itemsProcessed;
            // log.info(r.getName() + " " + (r.itemsProcessed));
         }

         for (Writer w : writers)
         {
            totalRead += w.itemsProcessed;
            // log.info(w.getName() + " " + (w.itemsProcessed));
         }

         for (Remover r : removers)
         {
            totalRead += r.itemsProcessed;
            // log.info(r.getName() + " " + (r.itemsProcessed));
         }

         double speed = totalRead * 1d / stop;
         log.info("Total accessed " + totalRead + ", speed " + speed + " oper/sec., time " + (stop / 1000d) + "sec");
      }
   }

}
