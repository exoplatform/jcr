/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.utils.io;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Rule;
import org.junit.Test;

import com.github.javatlacati.contiperf.PerfTest;
import com.github.javatlacati.contiperf.junit.ContiPerfRule;

import org.exoplatform.services.jcr.impl.util.io.SwapFile;

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
