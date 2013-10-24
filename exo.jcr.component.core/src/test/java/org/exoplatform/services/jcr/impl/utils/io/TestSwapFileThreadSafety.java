/*
 * Copyright (C) 2013 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.utils.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.databene.contiperf.PerfTest;
import org.databene.contiperf.junit.ContiPerfRule;
import org.exoplatform.services.jcr.impl.util.io.SwapFile;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:nfilotto@exoplatform.com">Nicolas Filotto</a>
 * @version $Id$
 *
 */
public class TestSwapFileThreadSafety
{
   protected static final int TOTAL_THREADS = 20;

   @Rule
   public ContiPerfRule rule = new ContiPerfRule();

   private final AtomicInteger step = new AtomicInteger();
   private CyclicBarrier startSignal = new CyclicBarrier(TOTAL_THREADS);

   @Test
   @PerfTest(invocations = TOTAL_THREADS, threads = TOTAL_THREADS)
   public void testConcurrentAccess() throws Exception
   {
      startSignal.await();
      SwapFile sf = SwapFile.get(new File(TestSwapFile.DIR_NAME), TestSwapFile.FILE_NAME + "-testConcurrentAccess");
      int value = 1;
      if (sf.exists())
      {
         InputStream is = new FileInputStream(sf);
         value = is.read() + 1;
         is.close();
      }
      OutputStream out = new FileOutputStream(sf);
      out.write(value);
      out.close();
      sf.spoolDone();
      startSignal.await();
      if (step.compareAndSet(0, 1))
      {
         sf = SwapFile.get(new File(TestSwapFile.DIR_NAME), TestSwapFile.FILE_NAME + "-testConcurrentAccess");
         InputStream is = new FileInputStream(sf);
         value = is.read();
         is.close();
         assertEquals("We should get the total amount of threads.", TOTAL_THREADS, value);
         assertTrue("File should be deleted.", sf.delete());
      }
   }
}
