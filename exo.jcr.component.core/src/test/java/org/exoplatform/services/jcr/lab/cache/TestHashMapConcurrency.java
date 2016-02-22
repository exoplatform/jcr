/*
 * Copyright (C) 2009 eXo Platform SAS.
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
package org.exoplatform.services.jcr.lab.cache;

import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 30.09.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestHashMapConcurrency.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestHashMapConcurrency extends TestCase
{

   public void testHashMap() throws InterruptedException
   {

      final Map<String, Object> map = new HashMap<String, Object>();
      // final Map<String, Object> map = Collections.synchronizedMap(new HashMap<String, Object>());

      map.put("key-0", 0);

      Thread th1 = new Thread()
      {

         /* (non-Javadoc)
          * @see java.lang.Thread#run()
          */
         @Override
         public void run()
         {
            try
            {
               // System.out.println(">>>>> in run() waiting 3sec...");
               Thread.sleep(3000);
            }
            catch (InterruptedException e)
            {
               e.printStackTrace();
            }

            for (int i = 1; i <= 10; i++)
            {
               map.put("key-" + i, i);
               // System.out.println("put key-" + i);
            }

            // System.out.println("<<<<< run() done.");
         }
      };

      th1.start();
      synchronized (map)
      {
         // System.out.println(">>>>>>>>>> synchronized (map)...");
         // System.out.println("map.get " + map.get("key-0"));
         map.get("key-0");
         // Thread.sleep(10000);
         // System.out.println("<<<<<<<<<< synchronized (map).");
      }

   }

}
