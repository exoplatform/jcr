package org.exoplatform.services.jcr.lab.infinispan;

import org.exoplatform.services.jcr.BaseStandaloneTest;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:aboughzela@exoplatform.com">Aymen Boughzela</a>
 * @version ${Revision}
 * @date 19/02/16
 */
public class TestISPNWriteLock extends BaseStandaloneTest
{

   AdvancedCache<String, Object> advancedCache;
   private static final int CHILDS_COUNT = 2;
   private static final int THREAD_COUNT = CHILDS_COUNT;
   private ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
   private AtomicInteger counter = new AtomicInteger(0);


   public void setUp() throws Exception
   {
      super.setUp();
      Cache<String, Object> cache = new DefaultCacheManager("conf/standalone/test-infinispan-config.xml").getCache();
      advancedCache = cache.getAdvancedCache();
   }

   public void testPutCache() throws Exception
   {

      advancedCache.withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.IGNORE_RETURN_VALUES, Flag.CACHE_MODE_LOCAL).put("a", new HashSet());
      for (int j = 0; j < CHILDS_COUNT; j++)
      {
         final int index = j;
         executorService.execute(new Runnable()
         {
            public void run()
            {
               try
               {
                  putCache("a", "input" + index);
               }
               catch (Exception e)
               {
                  e.printStackTrace();
                  fail("Error while adding child node: " + e.getMessage());
               }
               finally
               {
                  counter.incrementAndGet();
               }
            }
         });

      }
      do
      {
         Thread.sleep(300);
      }
      while (counter.get() < CHILDS_COUNT);

      Set<Object> foundObjects = (Set<Object>)advancedCache.get("a");
      assertEquals(CHILDS_COUNT, foundObjects == null ? 0 : foundObjects.size());
   }

   public void putCache(String key, String value) throws Exception
   {
      advancedCache.getTransactionManager().begin();
      Object existentObject = advancedCache.withFlags(Flag.FORCE_WRITE_LOCK).get(key);
      Set<Object> newSet = new HashSet<Object>();
      if (existentObject instanceof Set)
      {
         newSet.addAll((Set<Object>)existentObject);
      }
      newSet.add(value);

      advancedCache.withFlags(Flag.SKIP_REMOTE_LOOKUP, Flag.IGNORE_RETURN_VALUES, Flag.CACHE_MODE_LOCAL).put("a", newSet);
      advancedCache.getTransactionManager().commit();


   }

   public void tearDown()
   {

   }

   @Override
   protected String getRepositoryName()
   {
      return null;
   }
}