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

import java.text.DecimalFormat;
import java.util.ArrayList;
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

   private final DecimalFormat df = new DecimalFormat("#####.##");

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
      final List<NodeInfo> nodesPath = new LinkedList<NodeInfo>();
      //start from 1 thread
      int threadCount = initialSize;

      Random random = new Random();

      while (true)
      {
         //test init
         setUp();

         final List<WorkerResult> responceResults = new LinkedList<WorkerResult>();

         ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
         CountDownLatch startSignal = new CountDownLatch(1);
         AbstractTestAgent[] testAgents = new AbstractTestAgent[threadCount];

         //pool initialization
         for (int i = 0; i < threadCount; i++)
         {
            testAgents[i] = getAgent(nodesPath, responceResults, startSignal, readValue, random);
            //init agent
            testAgents[i].prepare();
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
         dumpResults(responceResults, threadCount, iterationTime);
         threadCount += iterationGrowingPoll;
         tearDown();
      }
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
   protected abstract AbstractTestAgent getAgent(List<NodeInfo> nodesPath, List<WorkerResult> responceResults,
      CountDownLatch startSignal, int readValue, Random random);

   private void dumpResults(List<WorkerResult> responceResults, int threadCount, int iterationTime)
   {

      List<Double> readResult = new ArrayList<Double>();
      List<Double> writeResult = new ArrayList<Double>();

      for (WorkerResult workerResult : responceResults)
      {
         if (workerResult == null)
         {
            continue;
         }
         if (workerResult.isRead())
         {
            //            read++;
            //            sum_read += workerResult.getResponceTime();
            readResult.add(new Double(workerResult.getResponceTime()));
         }
         else
         {
            writeResult.add(new Double(workerResult.getResponceTime()));
         }
      }
      ResultInfo readResultInfo = new ResultInfo(readResult);
      readResultInfo.calculate();

      ResultInfo writeResultInfo = new ResultInfo(writeResult);
      writeResultInfo.calculate();

      StringBuffer result = new StringBuffer();
      result.append("ThreadCount= ").append(threadCount);
      result.append(" TPS = ").append(
         Math.round(((readResultInfo.getResultCount() + writeResultInfo.getResultCount()) * 1000) / iterationTime));
      result.append(" Total read= ").append(df.format(readResultInfo.getResultCount()));
      result.append(" Max= ").append(df.format(readResultInfo.getMaxValue()));
      result.append(" Min= ").append(df.format(readResultInfo.getMinValue()));
      result.append(" Avg= ").append(df.format(readResultInfo.getAvgValue()));
      result.append(" StdDev= ").append(df.format(readResultInfo.getStdDevValue()));
      result.append(" Total write= ").append(writeResultInfo.getResultCount());
      result.append(" Max= ").append(df.format(writeResultInfo.getMaxValue()));
      result.append(" Min= ").append(df.format(writeResultInfo.getMinValue()));
      result.append(" Avg= ").append(df.format(writeResultInfo.getAvgValue()));
      result.append(" StdDev= ").append(df.format(writeResultInfo.getStdDevValue()));

      System.out.println(result.toString());
      //      long sum_read = 0;
      //      long sum_write = 0;
      //      long read = 0;
      //      long write = 0;
      //      for (WorkerResult workerResult : responceResults)
      //      {
      //         if (workerResult == null)
      //         {
      //            continue;
      //         }
      //         if (workerResult.isRead())
      //         {
      //            read++;
      //            sum_read += workerResult.getResponceTime();
      //         }
      //         else
      //         {
      //            write++;
      //            sum_write += workerResult.getResponceTime();
      //         }
      //      }
      //      if ((read + write) > 0)
      //      {
      //         StringBuffer result = new StringBuffer();
      //         result.append("ThreadCount= ").append(threadCount);
      //         result.append(" TPS = ").append(Math.round(((read + write) * 1000) / iterationTime));
      //         if (read > 0)
      //         {
      //            result.append(" Total read = ").append(read);
      //
      //            result.append(" Avg read response = ").append((sum_read / read));
      //         }
      //         if (write > 0)
      //         {
      //            result.append(" Total write = ").append(write);
      //
      //            result.append(" Avg write response = ").append((sum_write / write));
      //         }
      //
      //         System.out.println(result.toString());
      //      }
      responceResults.clear();
   }

   public class ResultInfo
   {
      private double maxValue;

      private double minValue;

      private double avgValue;

      private double stdDevValue;

      private List<Double> data;

      /**
       * @param data
       */
      public ResultInfo(List<Double> data)
      {
         super();
         this.data = data;
      }

      private void calculate()
      {
         final int n = data.size();
         if (n < 2)
         {
            this.stdDevValue = Double.NaN;
            this.avgValue = data.get(0);
            this.maxValue = data.get(0);
            this.minValue = data.get(0);
         }
         else
         {
            this.avgValue = data.get(0);
            this.maxValue = data.get(0);
            this.minValue = data.get(0);
            double sum = 0;
            for (int i = 1; i < data.size(); i++)
            {
               Double currValue = data.get(i);
               if (currValue > maxValue)
               {
                  maxValue = currValue;
               }
               if (currValue < minValue)
               {
                  minValue = currValue;
               }
               double newavg = avgValue + (currValue - avgValue) / (i + 1);
               sum += (currValue - avgValue) * (currValue - newavg);
               this.avgValue = newavg;
            }
            // Change to ( n - 1 ) to n if you have complete data instead of a sample.
            this.stdDevValue = Math.sqrt(sum / (n - 1));
         }
      }

      /**
       * @return the stdDevValue
       */
      public double getStdDevValue()
      {
         return stdDevValue;
      }

      /**
       * @return the maxValue
       */
      public double getMaxValue()
      {
         return maxValue;
      }

      /**
       * @return the minValue
       */
      public double getMinValue()
      {
         return minValue;
      }

      /**
       * @return the avgValue
       */
      public double getAvgValue()
      {
         return avgValue;
      }

      /**
       * @return the data
       */
      public List<Double> getData()
      {
         return data;
      }

      public long getResultCount()
      {
         return data.size();
      }

   }
}
