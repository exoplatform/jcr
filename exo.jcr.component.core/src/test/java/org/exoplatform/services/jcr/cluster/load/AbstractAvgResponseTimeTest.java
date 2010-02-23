/*
 * Copyright (C) 2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.cluster.load;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public abstract class AbstractAvgResponseTimeTest
{

   private final int iterationGrowingPoll;

   private final int iterationTime;

   private final int initialSize;

   private final int readValue;

   /**
    * 10%
    */
   private final int WARM_UP_RATIO = 10;

   /**
    * @param iterationGrowingPoll
    * @param iterationTime
    * @param initialSize
    */
   public AbstractAvgResponseTimeTest(int iterationGrowingPoll, int iterationTime, int initialSize, int readValue)
   {
      super();
      this.iterationGrowingPoll = iterationGrowingPoll;
      this.iterationTime = iterationTime;
      this.initialSize = initialSize;
      this.readValue = readValue;
   }

   public void testResponce() throws Exception
   {

      //start from 1 thread
      int threadCount = initialSize;

      Random random = new Random();

      while (true)
      {
         //test init
         setUp();

         final List<NodeInfo> nodesPath = new LinkedList<NodeInfo>();

         ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
         CountDownLatch startSignal = new CountDownLatch(1);
         AbstractTestAgent[] testAgents = new AbstractTestAgent[threadCount];
         ResultCollector resultCollector = new ResultCollector();
         //pool initialization
         for (int i = 0; i < threadCount; i++)
         {
            testAgents[i] = getAgent(nodesPath, resultCollector, startSignal, readValue, random);
            //init agent
            testAgents[i].prepare();
            threadPool.execute(testAgents[i]);

         }
         resultCollector.startIteration();
         startSignal.countDown();//let all threads proceed

         long warmUpTime = (iterationTime * WARM_UP_RATIO) / 100;
         Thread.sleep(warmUpTime);
         //reset result after warm up
         resultCollector.reset();
         Thread.sleep(iterationTime - warmUpTime);

         while (true)
         {
            long totalRead = resultCollector.getTotalReadTime();
            long totalWrite = resultCollector.getTotalWriteTime();
            long readAndWrite = totalRead + totalWrite;
            long ratio = (totalRead * 100) / readAndWrite;
            System.out.println("Ratio =" + ratio);
            if (ratio >= readValue)
            {
               break;
            }
            //only read allowed
            for (int i = 0; i < testAgents.length; i++)
            {
               testAgents[i].setBlockWrite(true);
            }
            Thread.sleep(1000);
         }

         threadPool.shutdown();
         for (int i = 0; i < testAgents.length; i++)
         {
            testAgents[i].setShouldStop(true);
         }

         //wait 10 minutes
         threadPool.awaitTermination(60 * 10, TimeUnit.SECONDS);
         resultCollector.finishIteration();
         pritResult(threadCount, resultCollector);
         threadCount += iterationGrowingPoll;
         tearDown();
      }
   }

   private void pritResult(int threadCount, ResultCollector resultCollector)
   {
      System.out.println("Threads " + threadCount + " Time read " + resultCollector.getTotalReadTime() + " Time write "
         + resultCollector.getTotalWriteTime() + "  " + resultCollector.getStatistic());
   }

   /**
    * Made some actions before iteration run
    * @throws Exception
    */
   protected void setUp() throws Exception
   {

   }

   /**
    * Made some actions after iteration finished
    * @throws Exception
    */
   protected void tearDown() throws Exception
   {

   }

   /**
    * Create new agent
    * @param nodesPath
    * @param responceResults
    * @param startSignal
    * @param READ_VALUE
    * @param random
    * @return
    */
   protected abstract AbstractTestAgent getAgent(List<NodeInfo> nodesPath, ResultCollector resultCollector,
      CountDownLatch startSignal, int readValue, Random random);

}
