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

import junit.framework.TestCase;

import org.exoplatform.common.http.client.HTTPResponse;
import org.exoplatform.services.jcr.cluster.JCRWebdavConnection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class AvgResponceTimeTest extends TestCase
{
   /**
    * WebDav realm
    */
   private String WEBDAV_REALM = "eXo REST services";

   /**
    * WebDav path
    */
   private String WEBDAV_DEFAULT_PATH = "/rest/jcr/repository/production/";

   /**
    * 10 sec default sampling time.
    */
   private static final int SAMPLING_TIME = 14000;

   /**
    * 2min default time of work of one iteration.
    */
   private static final int ITERATION_TIME = 60 * 1000;

   /**
    * How much thread will be added on the next iteration.
    */
   private static final int ITERATION_GROWING_POLL = 5;

   /**
    * Number between 0 and 100 show % how many read operations. 
    */
   private static final int READ_VALUE = 90;

   private static final Random random = new Random();

   private volatile static long l1FolderCount = 0;

   private static long lastCleanTime;

   /**
    * Statistic dumper task
    */
   private static final Timer STAT_TIMER = new Timer(true);

   public void testResponce() throws Exception
   {
      final List<NodeInfo> nodesPath = new ArrayList<NodeInfo>();
      //start from 1 thread
      int threadCount = 0;
      lastCleanTime = System.currentTimeMillis();

      while (true)
      {

         final List<WorkerResult> responceResults = Collections.synchronizedList(new ArrayList<WorkerResult>());
         threadCount += ITERATION_GROWING_POLL;
         ExecutorService tp = Executors.newFixedThreadPool(threadCount);
         WorkerThread[] workedThread = new WorkerThread[threadCount];
         for (int i = 0; i < threadCount; i++)
         {
            workedThread[i] = new WorkerThread(nodesPath, responceResults);
            tp.execute(workedThread[i]);

         }
         responceResults.clear();
         System.out.println("Thread started " + threadCount);
         Thread.sleep(ITERATION_TIME);

         for (int i = 0; i < workedThread.length; i++)
         {
            workedThread[i].setShoudRun(false);
         }
         tp.shutdown();
         //wait 10 minutes
         tp.awaitTermination(60 * 10, TimeUnit.SECONDS);
         dumpResults(responceResults);
         responceResults.clear();

      }
   }

   public static void dumpResults(List<WorkerResult> responceResults)
   {
      long sum_read = 0;
      long sum_write = 0;
      long read = 0;
      long write = 0;
      for (WorkerResult workerResult : responceResults)
      {

         if (workerResult.isRead)
         {
            read++;
            sum_read += workerResult.responceTime;
         }
         else
         {
            write++;
            sum_write += workerResult.responceTime;
         }
      }
      if ((read + write) > 0)
      {
         System.out.println("Read=" + read + " Write=" + write + " value " + (read * 100 / (read + write))
            + " Avg read resp=" + (read > 0 ? (sum_read / read) : 0) + " Avg write resp="
            + (write > 0 ? (sum_write / write) : 0));
         //System.out.println(responceResults);
      }
      responceResults.clear();
   }

   private class NodeInfo
   {
      private final String path;

      private final long created;

      /**
       * @param path
       * @param created
       */
      public NodeInfo(String path, long created)
      {
         super();
         this.path = path;
         this.created = created;
      }

   }

   private class WorkerResult
   {
      private final boolean isRead;

      private final long responceTime;

      /**
       * @param isRead
       * @param responceTime
       */
      public WorkerResult(boolean isRead, long responceTime)
      {
         super();
         this.isRead = isRead;
         this.responceTime = responceTime;
      }

      /**
       * @see java.lang.Object#toString()
       */
      @Override
      public String toString()
      {
         return "WorkerResult [isRead=" + isRead + ", responceTime=" + responceTime + "]";
      }

   }

   private class WorkerThread implements Runnable
   {

      private final List<WorkerResult> responceResults;

      private final List<NodeInfo> nodesPath;

      private String l1FolderName;

      private long l2FolderCount;

      private boolean shoudRun;

      /**
       * @param nodesPath 
       * @param resultsList
       */
      public WorkerThread(List<NodeInfo> nodesPath, List<WorkerResult> responceResults)
      {
         this.nodesPath = nodesPath;
         this.responceResults = responceResults;
         this.shoudRun = true;
      }

      /**
       * @see java.lang.Runnable#run()
       */
      public void run()
      {

         while (shoudRun)
         {
            if (random.nextInt(100) > READ_VALUE)
            {

               doWrite();
            }
            else
            {
               doRead();
            }
         }

      }

      /**
       * @return the shoudRun
       */
      protected boolean isShoudRun()
      {
         return shoudRun;
      }

      /**
       * @param shoudRun the shoudRun to set
       */
      protected void setShoudRun(boolean shoudRun)
      {
         this.shoudRun = shoudRun;
      }

      private JCRWebdavConnection getNewConnection()
      {
         return new JCRWebdavConnection("192.168.0.129", 80, "root", "exo", WEBDAV_REALM, WEBDAV_DEFAULT_PATH);
      }

      /**
       * Make write operation.
       */
      private void doWrite()
      {
         long start = 0;
         JCRWebdavConnection connection = null;
         try
         {
            connection = getNewConnection();

            if (l1FolderName == null || l2FolderCount == 100)
            {
               l1FolderName = "folder" + (l1FolderCount++);
               start = System.currentTimeMillis();
               connection.addDir(l1FolderName);
               l2FolderCount = 0;
               responceResults.add(new WorkerResult(false, System.currentTimeMillis() - start));
            }
            String path = l1FolderName + "/" + "node" + l2FolderCount++;
            start = System.currentTimeMillis();
            HTTPResponse response = connection.addNode(path, ("__the_data_in_nt+file__" + l2FolderCount).getBytes());

            if (response.getStatusCode() != 201)
            {
               System.out.println(Thread.currentThread().getName() + " : Can not add (response code "
                  + response.getStatusCode() + new String(response.getData()) + " ) node with path : " + path);
            }
            responceResults.add(new WorkerResult(false, System.currentTimeMillis() - start));
            nodesPath.add(new NodeInfo(path, System.currentTimeMillis()));
         }
         catch (Exception e)
         {
            System.out.println(e.getLocalizedMessage());
         }
         finally
         {
            if (connection != null)
            {
               connection.stop();
            }
         }

      }

      /**
       * Make
       */
      private void doRead()
      {
         if (nodesPath.size() > 0)
         {

            String readNodePath = null;
            while (readNodePath == null)
            {
               NodeInfo nodeInfo = nodesPath.get(random.nextInt(nodesPath.size()));
               //               if ((System.currentTimeMillis() - nodeInfo.created) > 30000)
               //               {
               readNodePath = nodeInfo.path;
               //               }

            }
            long start = System.currentTimeMillis();
            JCRWebdavConnection conn = getNewConnection();
            try
            {
               HTTPResponse response = conn.getNode(readNodePath);
               if (response.getStatusCode() != 200)
               {
                  System.out.println("Can not get (response code " + response.getStatusCode()
                     + new String(response.getData()) + " ) node with path : " + readNodePath);
               }

            }
            catch (Exception e)
            {
               System.out.println(e.getLocalizedMessage());
            }
            finally
            {
               conn.stop();
            }
            responceResults.add(new WorkerResult(true, System.currentTimeMillis() - start));
         }
      }
   }
}
