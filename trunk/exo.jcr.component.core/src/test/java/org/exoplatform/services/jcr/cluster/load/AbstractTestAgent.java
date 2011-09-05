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

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public abstract class AbstractTestAgent implements Runnable
{

   protected final Random random;

   private final int readValue;

   private final CountDownLatch startSignal;

   private final List<NodeInfo> nodesPath;

   private final ResultCollector resultCollector;

   private boolean shouldStop = false;

   private static volatile long LAST_WRITE_START;

   private boolean blockWrite;

   private final boolean isReadThread;

   /**
    * 
    */
   public AbstractTestAgent(List<NodeInfo> nodesPath, ResultCollector resultCollector, CountDownLatch startSignal,
      int readValue, Random random, boolean isReadThread)
   {
      this.nodesPath = nodesPath;
      this.resultCollector = resultCollector;
      this.random = random;
      this.startSignal = startSignal;
      this.readValue = readValue;
      this.isReadThread = isReadThread;
   }

   /**
    * Do read
    * @return
    */
   public abstract void doRead(List<NodeInfo> nodesPath, ResultCollector resultCollector);

   /**
    * Do write
    * @return
    */
   public abstract void doWrite(List<NodeInfo> nodesPath, ResultCollector resultCollector);

   /**
    * @see java.lang.Runnable#run()
    */
   public void run()
   {
      try
      {
         startSignal.await();

         while (!shouldStop)
         {
            if (isReadThread)
            {
               if (nodesPath.size() > 1)
               {
                  doRead(nodesPath, resultCollector);
               }
               else
               {
                  Thread.sleep(100);
               }
            }
            else if (!blockWrite)
            {
               doWrite(nodesPath, resultCollector);
            }
            //
            //            long totalRead = resultCollector.getTotalReadTime();
            //            long totalWrite = resultCollector.getTotalWriteTime();
            //            long readAndWrite = totalRead + totalWrite;
            //            if (blockWrite)
            //            {
            //               doRead(nodesPath, resultCollector);
            //            }
            //            else
            //            {
            //               long ratio = 0;
            //               if (readAndWrite > 0)
            //               {
            //                  ratio = (totalRead * 100) / readAndWrite;
            //               }
            //               //prevent to match write
            //               if (System.currentTimeMillis() - LAST_WRITE_START > 500 && (nodesPath.size() < 1 || ratio > readValue))
            //               {
            //                  LAST_WRITE_START = System.currentTimeMillis();
            //                  doWrite(nodesPath, resultCollector);
            //               }
            //               else
            //               {
            //                  doRead(nodesPath, resultCollector);
            //               }
            //            }

         }
      }
      catch (InterruptedException e)
      {
         // exit
      }
   }

   /**
    * @return the blockWrite
    */
   protected boolean isBlockWrite()
   {
      return blockWrite;
   }

   /**
    * @return the shouldStop
    */
   protected boolean isShouldStop()
   {
      return shouldStop;
   }

   /**
    * Prepare agent
    */
   protected void prepare()
   {

   }

   /**
    * @param blockWrite the blockWrite to set
    */
   protected void setBlockWrite(boolean blockWrite)
   {
      this.blockWrite = blockWrite;
   }

   /**
    * @param shouldStop the shouldStop to set
    */
   protected void setShouldStop(boolean shouldStop)
   {
      this.shouldStop = shouldStop;
   }

}
