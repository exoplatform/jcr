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

import java.util.HashMap;

import junit.framework.TestCase;

/**
 * Created by The eXo Platform SAS.
 * 
 * Date: 17.04.2008
 * 
 * @author <a href="mailto:peter.nedonosko@exoplatform.com.ua">Peter Nedonosko</a>
 * @version $Id: TestHashMapUse.java 34801 2009-07-31 15:44:50Z dkatayev $
 */
public class TestHashMapUse
   extends TestCase
{

   private HashMap<String, Object> cache;

   @Override
   protected void setUp() throws Exception
   {
      super.setUp();

      cache = new HashMap<String, Object>(CacheTestConstants.CACHE_SIZE + 10000);
   }

   @Override
   protected void tearDown() throws Exception
   {
      super.tearDown();

      cache.clear();
   }

   /**
    * Test if CacheManager creates and contains empty caches list. Test if we can add custome cache
    * etc.
    * 
    * @throws Exception
    */
   public void testGetCacheNames() throws Exception
   {

   }

   /**
    * Put lot of Strings into cache and getting them back for speed test.
    * 
    * @throws Exception
    */
   public void testPutGetStrings() throws Exception
   {
      final int cnt = CacheTestConstants.CACHE_SIZE;

      // put 1M
      long start = System.currentTimeMillis();
      for (int i = 1; i <= cnt; i++)
      {
         cache.put(CacheTestConstants.KEY_PREFIX + i, "value" + i);
      }
      long time = System.currentTimeMillis() - start;
      double perItem = time * 1d / cnt;
      System.out.println(getName() + "\tPut\t" + cnt + " strings in " + time + "ms. Avg " + perItem
               + "ms per one string.");

      // get 1M
      start = System.currentTimeMillis();
      for (int i = 1; i <= cnt; i++)
      {
         Object value = cache.get(CacheTestConstants.KEY_PREFIX + i);
         assertNotNull("The element '$key" + i + "' should not be null", value);
      }
      time = System.currentTimeMillis() - start;
      perItem = time * 1d / cnt;
      System.out.println(getName() + "\tGet\t" + cnt + " strings in " + time + "ms. Avg " + perItem
               + "ms per one string.");

      // check if we have all keys/values same as just write
      for (int i = 1; i <= cnt; i++)
      {
         Object value = cache.get(CacheTestConstants.KEY_PREFIX + i);
         assertNotNull("The element '$key" + i + "' should not be a null", value);
         assertEquals("The element '$key" + i + "' value should be of a String class", String.class, value.getClass());
         assertEquals("The element '$key" + i + "' value is wrong", "value" + i, (String) value);
      }
   }

   public void testPutGetRemove()
   {
      final int cnt = CacheTestConstants.CACHE_SIZE;

      // put 1M
      for (int i = 1; i <= cnt; i++)
      {
         cache.put(CacheTestConstants.KEY_PREFIX + i, "value" + i);
      }

      // get 1M
      for (int i = 1; i <= cnt; i++)
      {
         Object value = cache.get(CacheTestConstants.KEY_PREFIX + i);
         assertNotNull("The element '$key" + i + "' should not be null", value);
      }

      long start = System.currentTimeMillis();

      for (int i = cnt; i >= 1; i--)
      {
         cache.remove(CacheTestConstants.KEY_PREFIX + i);
      }

      long time = System.currentTimeMillis() - start;
      double perItem = time * 1d / cnt;
      System.out.println(getName() + "\tRemove\t" + cnt + " strings in " + time + "ms. Avg " + perItem
               + "ms per one string.");

   }

}
