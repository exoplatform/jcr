/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
 * along with this program; if not, see&lt;http://www.gnu.org/licenses/&gt;.
 */
package org.exoplatform.services.jcr.statistics;

import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The class used to manage all the metrics such as minimum, maximum, total, times and average.
 * 
 * Created by The eXo Platform SAS
 * Author : Nicolas Filotto 
 *          nicolas.filotto@exoplatform.com
 * 30 mars 2010  
 */
public class Statistics
{

   /**
    * The global statistics on the top of the current ones
    */
   private final Statistics parent;

   /**
    * The description of the statistics
    */
   private final String description;

   /**
    * The min value of the time spent for one call
    */
   private final AtomicLong min = new AtomicLong(Long.MAX_VALUE);

   /**
    * The max value of the time spent for one call
    */
   private final AtomicLong max = new AtomicLong(-1);

   /**
    * The total time spent for all the calls
    */
   private final AtomicLong total = new AtomicLong();

   /**
    * The total amount of calls
    */
   private final AtomicLong times = new AtomicLong();

   /**
    * The {@link ThreadLocal} used to keep the initial timestamp
    */
   private final ThreadLocal<Queue<Long>> currentTime = new ThreadLocal<Queue<Long>>()
   {
      protected Queue<Long> initialValue()
      {
         return new LinkedList<Long>();
      }
   };

   /**
    * The default constructor
    * @param parent the global statistics on the top of the current ones
    * @param description the description of the statistics, to be compatible with the CSV format
    * all ',' will be replaced with ';'
    */
   public Statistics(Statistics parent, String description)
   {
      this.parent = parent;
      this.description = description.replace(',', ';');
   }

   /**
    * Start recording
    */
   public void begin()
   {
      if (parent != null)
      {
         parent.onBegin();         
      }
      onBegin();
   }

   /**
    * Store the current timestamp in the {@link ThreadLocal}
    */
   private void onBegin()
   {
      Queue<Long> q = currentTime.get();
      q.add(System.currentTimeMillis());
   }

   /**
    * Stop recording
    */
   public void end()
   {
      onEnd();
      if (parent != null)
      {
         parent.onEnd();         
      }
   }

   /**
    * Refresh the values of the metrics (min, max, total and times)
    */
   private void onEnd()
   {
      long result = System.currentTimeMillis() - currentTime.get().poll();
      times.incrementAndGet();
      if (result < min.get())
      {
         min.set(result);
      }
      if (max.get() < result)
      {
         max.set(result);
      }
      total.addAndGet(result);
   }

   /**
    * Print the description of all the metrics into the given {@link PrintWriter}
    */
   public void printHeader(PrintWriter pw)
   {
      pw.print(description);
      pw.print("-Min,");
      pw.print(description);
      pw.print("-Max,");
      pw.print(description);
      pw.print("-Total,");
      pw.print(description);
      pw.print("-Avg,");
      pw.print(description);
      pw.print("-Times");
   }

   /**
    * Print the current snapshot of the metrics and evaluate the average value
    */
   public void printData(PrintWriter pw)
   {
      long lmin = min.get();
      if (lmin == Long.MAX_VALUE)
      {
         lmin = -1;
      }
      long lmax = max.get();
      long ltotal = total.get();
      long ltimes = times.get();
      float favg = ltimes == 0 ? 0f : (float)ltotal / ltimes;
      pw.print(lmin);
      pw.print(',');
      pw.print(lmax);
      pw.print(',');
      pw.print(ltotal);
      pw.print(',');
      pw.print(favg);
      pw.print(',');
      pw.print(ltimes);
   }

   /**
    * @return The min value of the time spent for one call
    */
   public long getMin()
   {
      long lmin = min.get();
      if (lmin == Long.MAX_VALUE)
      {
         lmin = -1;
      }
      return lmin;
   }

   /**
    * @return The max value of the time spent for one call
    */
   public long getMax()
   {
      return max.get();
   }

   /**
    * @return The total time spent for all the calls
    */
   public long getTotal()
   {
      return total.get();
   }

   /**
    * @return The total amount of calls
    */
   public long getTimes()
   {
      return times.get();
   }

   /**
    * @return The average time spent for one call
    */
   public float getAvg()
   {
      long ltotal = total.get();
      long ltimes = times.get();
      float favg = ltimes == 0 ? 0f : (float)ltotal / ltimes;      
      return favg;
   }
   
   /**
    * Reset the statistics
    */
   public void reset()
   {
      min.set(Long.MAX_VALUE);
      max.set(0);
      total.set(0);
      times.set(0);
   }
}