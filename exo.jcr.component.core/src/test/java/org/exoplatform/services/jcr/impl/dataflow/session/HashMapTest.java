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
package org.exoplatform.services.jcr.impl.dataflow.session;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Created by The eXo Platform SAS
 * 
 * 14.06.2007
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: HashMapTest.java 11907 2008-03-13 15:36:21Z ksm $
 */
public class HashMapTest extends TestCase
{

   public void testStringKey()
   {

      String key1 = "111";

      Map<String, InputStream> smap = new HashMap<String, InputStream>();

      ByteArrayInputStream bais = new ByteArrayInputStream(new byte[1]);

      smap.put(key1, bais);

      assertEquals("Must be equals", bais, smap.get(new String(key1)));

   }

   // TODO
   public void _testStringKeyWeakHashMap() throws Exception
   {

      final Map<String, InputStream> smap = new WeakHashMap<String, InputStream>();

      Thread runner = new Thread()
      {

         @Override
         public void run()
         {
            String key1 = "111";
            ByteArrayInputStream bais = new ByteArrayInputStream(new byte[1]);
            smap.put(key1, bais);
         }
      };

      runner.start();
      runner.join();
      runner = null;

      System.gc();
      Thread.yield();
      Thread.sleep(15000);

      assertNull("Must be null", smap.get("111"));

   }
}
