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

import java.util.ArrayList;
import java.util.Collections;
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
public abstract class AbstractAvgResponceTimeTest
{

   private final int iterationGrowingPoll;

   private final int iterationTime;

   private final int initialSize;

   private final int readValue;

   /**
    * @param iterationGrowingPoll
    * @param iterationTime
    * @param initialSize
    */
   public AbstractAvgResponceTimeTest(int iterationGrowingPoll, int iterationTime, int initialSize, int readValue)
   {
      super();
      this.iterationGrowingPoll = iterationGrowingPoll;
      this.iterationTime = iterationTime;
      this.initialSize = initialSize;
      this.readValue = readValue;
   }

   public void testResponce() throws Exception
   {
      final List<NodeInfo> nodesPath = new ArrayList<NodeInfo>();
      //start from 1 thread
      int threadCount = initialSize;

      Random random = new Random();

      while (true)
      {

         final List<WorkerResult> responceResults = Collections.synchronizedList(new ArrayList<WorkerResult>());

         ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
         CountDownLatch startSignal = new CountDownLatch(1);
         AbstractTestAgent[] testAgents = new AbstractTestAgent[threadCount];
         //pool initialization
         for (int i = 0; i < threadCount; i++)
         {
            testAgents[i] = getAgent(nodesPath, responceResults, startSignal, readValue, random);
            threadPool.execute(testAgents[i]);

         }
         responceResults.clear();
         startSignal.countDown();//let all threads proceed

         Thread.sleep(iterationTime);

         threadPool.shutdown();
         for (int i = 0; i < testAgents.length; i++)
         {
            testAgents[i].setShouldStop(true);
         }
         //wait 10 minutes
         threadPool.awaitTermination(60 * 10, TimeUnit.SECONDS);
         dumpResults(responceResults, threadCount);
         threadCount += iterationGrowingPoll;
      }
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
   protected abstract AbstractTestAgent getAgent(List<NodeInfo> nodesPath, List<WorkerResult> responceResults,
      CountDownLatch startSignal, int readValue, Random random);

   private void dumpResults(List<WorkerResult> responceResults, int threadCount)
   {
      long sum_read = 0;
      long sum_write = 0;
      long read = 0;
      long write = 0;
      for (WorkerResult workerResult : responceResults)
      {

         if (workerResult.isRead())
         {
            read++;
            sum_read += workerResult.getResponceTime();
         }
         else
         {
            write++;
            sum_write += workerResult.getResponceTime();
         }
      }
      if ((read + write) > 0)
      {
         System.out.println(" ThreadCount= " + threadCount + " Read=" + read + " Write=" + write + " value "
            + (read * 100 / (read + write)) + " Avg read resp=" + (read > 0 ? (sum_read / read) : 0)
            + " Avg write resp=" + (write > 0 ? (sum_write / write) : 0));
      }
      responceResults.clear();
   }
}
