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
package org.exoplatform.services.jcr.lab.infinispan;

import junit.framework.TestCase;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;

import java.util.concurrent.TimeUnit;

/**
 * @author <a href="anatoliy.bazko@exoplatform.org">Anatoliy Bazko</a>
 * @version $Id: TestINSPCache.java 111 2010-11-11 11:11:11Z tolusha $
 *
 */
public class TestISPNCache extends TestCase
{

   /**
    * {@inheritDoc}
    */
   @Override
   public void setUp() throws Exception
   {
      super.setUp();
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void tearDown() throws Exception
   {
      super.tearDown();
   }

   /**
    * Test default cache and base operation.
    * 
    * @throws Exception
    */
   public void testGetCache() throws Exception
   {
      // Create cache manager
      GlobalConfiguration myGlobalConfig = new GlobalConfigurationBuilder().build();
      EmbeddedCacheManager manager = new DefaultCacheManager(myGlobalConfig);

      // Create a cache
      Configuration config = new ConfigurationBuilder().build();
      manager.defineConfiguration("cache", config);
      Cache<String, String> cache = manager.getCache("cache");

      cache.put("key", "value");
      assertTrue(cache.size() == 1);
      assertTrue(cache.containsKey("key"));

      String value = (String)cache.remove("key");
      assertTrue(value.equals("value"));
      assertTrue(cache.isEmpty());

      cache.put("key", "value");
      cache.putIfAbsent("key", "newValue");
      assertTrue("value".equals(cache.get("key")));

      cache.clear();
      assertTrue(cache.isEmpty());

      cache.put("key", "value", 2, TimeUnit.SECONDS);
      assertTrue(cache.containsKey("key"));
      Thread.sleep(2000 + 500);
      assertFalse(cache.containsKey("key"));
   }

   /**
    * Test cluster cache and base operation.
    * 
    * @throws Exception
    */
   public void testGetClusterCache() throws Exception
   {
      GlobalConfiguration myGlobalConfig = new GlobalConfigurationBuilder().clusteredDefault().build();
      // Create cache manager
      EmbeddedCacheManager manager = new DefaultCacheManager(myGlobalConfig);

      // Create a cache
      Cache<String, String> cache = manager.getCache();

      cache.put("key", "value");
      assertTrue(cache.size() == 1);
      assertTrue(cache.containsKey("key"));

      String value = (String)cache.remove("key");
      assertTrue(value.equals("value"));
      assertTrue(cache.isEmpty());

      cache.put("key", "value");
      cache.putIfAbsent("key", "newValue");
      assertTrue("value".equals(cache.get("key")));

      cache.clear();
      assertTrue(cache.isEmpty());

      cache.put("key", "value", 2, TimeUnit.SECONDS);
      assertTrue(cache.containsKey("key"));
      Thread.sleep(2000 + 500);
      assertFalse(cache.containsKey("key"));
   }

}
