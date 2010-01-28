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
package org.exoplatform.services.jcr.jbosscache;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.MappedParametrizedObjectEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.TemplateConfigurationHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Cache;
import org.jboss.cache.CacheFactory;
import org.jboss.cache.DefaultCacheFactory;
import org.jgroups.JChannelFactory;

import java.io.IOException;
import java.io.InputStream;

import javax.transaction.TransactionManager;

/**
 * Factory that creates pre-configured instances of JBossCache, without starting it.
 * Path to JBossCache configuration or template should be provided as 
 * "jbosscache-configuration" property in parameterEntry instance. If
 * transaction manager is configure in ExoJBossCacheFactory, then it
 * is injected into the cache instance. 
 * <br>
 * If parameterEntry has "jgroups-multiplexer-stack" (=true) and 
 * "jgroups-configuration" parameters then Multiplexing stack is enabled
 * in JBossCache (this is highly recommended by RH specialists).
 * 
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: ExoCacheFactoryImpl.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class ExoJBossCacheFactory<K, V>
{

   public static final String JBOSSCACHE_CONFIG = "jbosscache-configuration";

   public static final String JGROUPS_CONFIG = "jgroups-configuration";

   public static final String JGROUPS_MUX_ENABLED = "jgroups-multiplexer-stack";

   private final TemplateConfigurationHelper configurationHelper;

   private final TransactionManager transactionManager;

   private final Log log = ExoLogger.getLogger(ExoJBossCacheFactory.class);

   /**
    * Creates ExoJbossCacheFactory with provided configuration transaction managers.
    * Transaction manager will later be injected to cache instance. 
    * 
    * @param configurationManager
    * @param transactionManager
    */
   public ExoJBossCacheFactory(ConfigurationManager configurationManager, TransactionManager transactionManager)
   {
      this.configurationHelper = TemplateConfigurationHelper.createJBossCacheHelper(configurationManager);
      this.transactionManager = transactionManager;
   }

   /**
    * Creates ExoJbossCacheFactory with provided configuration manager and without transaction manager. 
    * 
    * @param configurationManager
    */
   public ExoJBossCacheFactory(ConfigurationManager configurationManager)
   {
      this.configurationHelper = TemplateConfigurationHelper.createJBossCacheHelper(configurationManager);
      this.transactionManager = null;
   }

   /**
    * Creates pre-configured instance of JBossCache, without starting it.
    * Path to JBossCache configuration or template should be provided as 
    * "jbosscache-configuration" property in parameterEntry instance. If
    * transaction manager is configure in ExoJBossCacheFactory, then it
    * is injected into the cache instance. 
    * <br>
    * If parameterEntry has "jgroups-multiplexer-stack" (=true) and 
    * "jgroups-configuration" parameters then Multiplexing stack is enabled
    * in JBossCache (this is highly recommended by RH specialists).
    * 
    * @param parameterEntry
    * @return
    * @throws RepositoryConfigurationException
    */
   public Cache<K, V> createCache(MappedParametrizedObjectEntry parameterEntry) throws RepositoryConfigurationException
   {
      // get JBossCache configuration file path
      String jBossCacheConfigurationPath = parameterEntry.getParameterValue(JBOSSCACHE_CONFIG);
      log.info("JBoss Cache configuration/template used: " + jBossCacheConfigurationPath);

      // prepare configuration
      InputStream configStream;
      try
      {
         // fill template
         configStream = configurationHelper.fillTemplate(jBossCacheConfigurationPath, parameterEntry.getParameters());
      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException(e);
      }

      // create cache
      CacheFactory<K, V> factory = new DefaultCacheFactory<K, V>();
      Cache<K, V> cache = factory.createCache(configStream, false);

      // inject transaction manager if defined
      if (transactionManager != null)
      {
         cache.getConfiguration().getRuntimeConfig().setTransactionManager(transactionManager);
      }

      // JGroups multiplexer configuration if enabled
      if (parameterEntry.getParameterBoolean(JGROUPS_MUX_ENABLED, false))
      {
         try
         {
            // Get path to JGroups configuration
            String jgroupsConfigurationFilePath = parameterEntry.getParameterValue(JGROUPS_CONFIG);
            if (jgroupsConfigurationFilePath != null)
            {
               // Create and inject multiplexer fatory
               JChannelFactory muxFactory = new JChannelFactory();
               muxFactory.setMultiplexerConfig(jgroupsConfigurationFilePath);
               cache.getConfiguration().getRuntimeConfig().setMuxChannelFactory(muxFactory);
               log.info("Multiplexer stack successfully inabled for the cache.");
            }
         }
         catch (Exception e)
         {
            // exception occurred setting mux factory
            e.printStackTrace();
            throw new RepositoryConfigurationException("Error setting multiplexer configuration.", e);
         }
      }
      return cache;
   }
}
