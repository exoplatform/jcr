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
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class ResultCollector
{
   private ResultInfo readResultInfo;

   private ResultInfo writeResultInfo;

   private final DecimalFormat df = new DecimalFormat("#####.##");

   private long iterationStartTime;

   private long iterationTime;

   /**
    * 
    */
   public ResultCollector()
   {
      super();
      readResultInfo = new ResultInfo();
      writeResultInfo = new ResultInfo();
   }

   /**
    * 
    * @return return total write time
    */
   public long getTotalWriteTime()
   {
      return writeResultInfo.getSum();
   }

   /**
    * Reset result 
    */
   public synchronized void reset()
   {
      this.readResultInfo = new ResultInfo();
      this.writeResultInfo = new ResultInfo();
      this.iterationStartTime = System.currentTimeMillis();
   }

   /**
    * 
    * @return total read time.
    */
   public long getTotalReadTime()
   {
      return readResultInfo.getSum();
   }

   /**
    * Total write time
    * @return
    */
   public long getReadCount()
   {
      return readResultInfo.getResultCount();
   }

   /**
    * Total read times
    * @return
    */
   public long getWriteCount()
   {
      return writeResultInfo.getResultCount();
   }

   public void addResult(boolean isRead, long time)
   {
      if (isRead)
      {
         readResultInfo.addResult(time);
      }
      else
      {
         writeResultInfo.addResult(time);
      }
   }

   /**
    * Strt iteration
    */
   public void startIteration()
   {
      this.iterationStartTime = System.currentTimeMillis();
   }

   /**
    * Strt iteration
    */
   public void finishIteration()
   {
      this.iterationTime = System.currentTimeMillis() - iterationStartTime;
   }

   public String getStatistic()
   {
      readResultInfo.calculateStatistic();
      writeResultInfo.calculateStatistic();

      StringBuffer result = new StringBuffer();
      result.append(" TPS total= ").append(
         Math.round(((readResultInfo.getResultCount() + writeResultInfo.getResultCount()) * 1000) / iterationTime));
      result.append(" TPS read= ").append(Math.round(((readResultInfo.getResultCount()) * 1000) / iterationTime));
      result.append(" TPS write= ").append(Math.round(((writeResultInfo.getResultCount()) * 1000) / iterationTime));

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

      return result.toString();
   }

   class ResultInfo
   {
      private double maxValue;

      private double minValue;

      private double avgValue;

      private double stdDevValue;

      private List<Double> data;

      private AtomicLong sumValue;

      /**
       * @param data
       */
      public ResultInfo()
      {

         this.data = new LinkedList<Double>();
         this.sumValue = new AtomicLong(0);
      }

      public void addResult(long resultTime)
      {
         this.data.add(new Double(resultTime));
         this.sumValue.addAndGet(resultTime);
      }

      /**
       * 
       * @return sum of values
       */
      public long getSum()
      {
         return sumValue.get();
      }

      public void calculateStatistic()
      {
         final int n = data.size();
         if (n > 0)
         {
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
                  if (currValue == null)
                  {
                     currValue = new Double(0);
                  }
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