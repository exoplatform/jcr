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

   private final List<WorkerResult> responceResults;

   private boolean shouldStop = false;

   /**
    * 
    */
   public AbstractTestAgent(List<NodeInfo> nodesPath, List<WorkerResult> responceResults, CountDownLatch startSignal,
      int readValue, Random random)
   {
      this.nodesPath = nodesPath;
      this.responceResults = responceResults;
      this.random = random;
      this.startSignal = startSignal;
      this.readValue = readValue;
   }

   /**
    * Do read
    * @return
    */
   public abstract List<WorkerResult> doRead(List<NodeInfo> nodesPath);

   /**
    * Do write
    * @return
    */
   public abstract List<WorkerResult> doWrite(List<NodeInfo> nodesPath);

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
            if (random.nextInt(100) > readValue)
            {

               responceResults.addAll(doWrite(nodesPath));
            }
            else
            {
               responceResults.addAll(doRead(nodesPath));
            }
         }
      }
      catch (InterruptedException e)
      {
         // exit
      }
   }

   /**
    * @return the shouldStop
    */
   protected boolean isShouldStop()
   {
      return shouldStop;
   }

   /**
    * @param shouldStop the shouldStop to set
    */
   protected void setShouldStop(boolean shouldStop)
   {
      this.shouldStop = shouldStop;
   }

}
