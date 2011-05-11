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
package org.exoplatform.services.jcr.cluster.load.webdav;

import junit.framework.TestCase;

import org.exoplatform.services.jcr.cluster.load.AbstractAvgResponseTimeTest;
import org.exoplatform.services.jcr.cluster.load.AbstractTestAgent;
import org.exoplatform.services.jcr.cluster.load.NodeInfo;
import org.exoplatform.services.jcr.cluster.load.ResultCollector;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class WebDavAvgResponseTimeTest extends TestCase
{

   /**
    * 2min default time of work of one iteration.
    */
   private static final int ITERATION_TIME = 60 * 1000;

   /**
    * How much thread will be added on the next iteration.
    */
   private static final int ITERATION_GROWING_POLL = 50;

   /**
    * Number between 0 and 100 show % how many read operations. 
    */
   private static final int READ_VALUE = 90;

   private static final String TEST_ROOT = "WebDavAvgResponseTimeTestRoot";

   public void testWebDav() throws Exception
   {
      WebDavTest test = new WebDavTest(ITERATION_GROWING_POLL, ITERATION_TIME, 50, READ_VALUE);
      test.testResponce();
   }

   private class WebDavTest extends AbstractAvgResponseTimeTest
   {
      private String iterationRoot;

      /**
       * @param iterationGrowingPoll
       * @param iterationTime
       * @param initialSize
       * @param readValue
       */
      public WebDavTest(int iterationGrowingPoll, int iterationTime, int initialSize, int readValue)
      {
         super(iterationGrowingPoll, iterationTime, initialSize, readValue);
      }

      /**
       * @see org.exoplatform.services.jcr.cluster.load.AbstractAvgResponseTimeTest#setUp()
       */
      @Override
      protected void setUp() throws Exception
      {
         // TODO Auto-generated method stub
         super.setUp();
         WebDavTestAgent setUpAgent = new WebDavTestAgent(null, null, null, null, 0, null, false);

         String testRoot = setUpAgent.createDirIfAbsent("", TEST_ROOT, new ResultCollector());
         iterationRoot = setUpAgent.createDirIfAbsent(testRoot, UUID.randomUUID().toString(), new ResultCollector());
      }

      /**
       * @see org.exoplatform.services.jcr.cluster.load.AbstractAvgResponseTimeTest#tearDown()
       */
      @Override
      protected void tearDown() throws Exception
      {
         super.tearDown();

      }

      /**
       * @see org.exoplatform.services.jcr.cluster.load.AbstractAvgResponseTimeTest#getAgent(java.util.List, java.util.List, java.util.concurrent.CountDownLatch, int, java.util.Random)
       */
      @Override
      protected AbstractTestAgent getAgent(List<NodeInfo> nodesPath, ResultCollector resultCollector,
         CountDownLatch startSignal, int readValue, Random random, boolean isReadThread)
      {
         return new WebDavTestAgent(iterationRoot, nodesPath, resultCollector, startSignal, readValue, random,
            isReadThread);
      }

   }
}
