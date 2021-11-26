/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
