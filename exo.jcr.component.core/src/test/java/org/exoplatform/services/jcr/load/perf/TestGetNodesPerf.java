/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.load.perf;

import org.exoplatform.services.jcr.JcrAPIBaseTest;
import org.exoplatform.services.jcr.impl.core.SessionImpl;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 2009
 *
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a> 
 * @version $Id$
 */
public class TestGetNodesPerf extends JcrAPIBaseTest
{
   private static final String testName = "testRoot";

   private static final int sessionCount = 100;

   private static final int nodesCount = 50000;

   private static final int timesCount = 5;

   private Session[] sessions = new Session[sessionCount];

   private GetNodesThread[] threads = new GetNodesThread[sessionCount];

   public void testGetNodes() throws Exception
   {
      for (int i = 0; i < sessionCount; i++)
      {
         sessions[i] = (SessionImpl)repository.login(credentials, "ws");
      }

      Node testRoot = session.getRootNode().addNode(testName);
      session.save();

      log.info("adding...");
      for (int i = 0; i < nodesCount; i++)
      {
         testRoot.addNode("_" + i + "_node");
      }
      log.info("saving...");
      session.save();

      log.info("waiting for 10 seconds...");
      Thread.sleep(10000);

      System.gc();
      Runtime rt = Runtime.getRuntime();
      long usedMemory = rt.totalMemory() - rt.freeMemory();

      log.info("getting nodes...");
      for (int k = 0; k < timesCount; k++)
      {
         for (int i = 0; i < sessionCount; i++)
         {
            threads[i] = new GetNodesThread(sessions[i]);
            threads[i].start();
         }

         outer : while (true)
         {
            for (int i = 0; i < sessionCount; i++)
            {
               if (threads[i].isAlive())
               {
                  Thread.sleep(1000);
                  continue outer;
               }
            }

            break;
         }

         log.info("Memory used: " + (rt.totalMemory() - rt.freeMemory() - usedMemory) / 1024 / 1024 + "Mb");
         log.info("waiting for 10 seconds...");
         Thread.sleep(10000);
      }
      log.info("Memory used: " + (rt.totalMemory() - rt.freeMemory() - usedMemory) / 1024 / 1024 + "Mb");
   }

   private class GetNodesThread extends Thread
   {
      private final Session curSession;

      private NodeIterator nodes;

      GetNodesThread(Session session)
      {
         this.curSession = session;
      }

      @Override
      public void run()
      {
         try
         {
            Node testRoot = curSession.getRootNode().getNode(testName);

            long startTime = System.currentTimeMillis();
            nodes = testRoot.getNodes();
            while (nodes.hasNext())
            {
               nodes.next();
            }
            log.info("Total time: " + (System.currentTimeMillis() - startTime) + "ms");

         }
         catch (Exception e)
         {
            e.printStackTrace();
         }
      }
   }

   public void _testNodeItetatorPerfomance() throws Exception
   {
      Node testRoot = session.getRootNode().getNode(testName);

      // Test iterator perfomance
      NodeIterator nodes = testRoot.getNodes();
      log.info(nodes.getSize() + " nodes");

      long startTime = System.currentTimeMillis();
      while (nodes.hasNext())
      {
         nodes.nextNode();
      }
      log.info("Iterating all nodes consumes : " + (System.currentTimeMillis() - startTime) + "ms");
   }
}
