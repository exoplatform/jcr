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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query.jbosscache;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.Indexer;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.storage.jbosscache.AbstractWriteOnlyCacheLoader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.CacheStatus;
import org.jboss.cache.Fqn;
import org.jboss.cache.Modification;
import org.jboss.cache.config.Configuration.CacheMode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: IndexerCacheLoader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class IndexerCacheLoader extends AbstractWriteOnlyCacheLoader
{
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.IndexerCacheLoader");

   /**
    * A map of all the indexers that has been registered
    */
   private final Map<Fqn<String>, Indexer> indexers = new HashMap<Fqn<String>, Indexer>();

   protected volatile IndexerIoModeHandler modeHandler;

   /**
    * @see org.jboss.cache.loader.AbstractCacheLoader#commit(java.lang.Object)
    */
   @Override
   public void commit(Object tx) throws Exception
   {
      // do nothing. Everything is done on prepare phase.
   }

   /**
    * This method will register a new Indexer according to the given parameters. 
    * 
    * @param searchManager
    * @param parentSearchManager
    * @param handler
    * @param parentHandler
    * @throws RepositoryConfigurationException
    */
   public void register(SearchManager searchManager, SearchManager parentSearchManager, QueryHandler handler,
      QueryHandler parentHandler) throws RepositoryConfigurationException
   {
      indexers.put(Fqn.fromElements(searchManager.getWsId()), new Indexer(searchManager, parentSearchManager, handler,
         parentHandler));
      if (LOG.isDebugEnabled())
      {
         LOG.debug("Register " + searchManager.getWsId() + " " + this + " in " + indexers);
      }
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public Object put(Fqn name, Object key, Object value) throws Exception
   {
      if (key.equals(JBossCacheIndexChangesFilter.LISTWRAPPER) && value instanceof ChangesFilterListsWrapper)
      {
         if (LOG.isDebugEnabled())
         {
            LOG.info("Received list wrapper, start indexing...");
         }
         // updating index
         ChangesFilterListsWrapper wrapper = (ChangesFilterListsWrapper)value;
         try
         {
            Indexer indexer = indexers.get(name.getParent());
            if (indexer == null)
            {
               LOG.warn("No indexer could be found for the fqn " + name.getParent());
               if (LOG.isDebugEnabled())
               {
                  LOG.debug("The current content of the map of indexers is " + indexers);
               }
            }
            else if (wrapper.withChanges())
            {
               indexer.updateIndex(wrapper.getChanges(), wrapper.getParentChanges());
            }
            else
            {
               indexer.updateIndex(wrapper.getAddedNodes(), wrapper.getRemovedNodes(), wrapper.getParentAddedNodes(),
                  wrapper.getParentRemovedNodes());
            }
         }
         finally
         {
            if (modeHandler.getMode() == IndexerIoMode.READ_WRITE)
            {
               // remove the data from the cache
               cache.removeNode(name);
            }
         }
      }
      return null;
   }

   /**
    * {@inheritDoc}
    */
   public void put(Fqn arg0, Map<Object, Object> arg1) throws Exception
   {
      // ignoring call  cacheRoot.addChild(PARAMETER_ROOT).setResident(true);
   }

   /**
    * @see org.exoplatform.services.jcr.impl.storage.jbosscache.AbstractWriteOnlyCacheLoader#put(java.util.List)
    */
   @Override
   public void put(List<Modification> modifications) throws Exception
   {
      // do nothing. Index is updated on prepare phase.
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#remove(org.jboss.cache.Fqn)
    */
   public void remove(Fqn arg0) throws Exception
   {
      // do nothing
   }

   /**
    * Switches Indexer mode from RO to RW, or from RW to RO
    * 
    * @param ioMode
    */
   void setMode(IndexerIoMode ioMode)
   {
      getModeHandler().setMode(ioMode);
   }

   /**
    * Set the mode handler
    * @param modeHandler
    */
   IndexerIoModeHandler getModeHandler()
   {
      if (modeHandler == null)
      {
         if (cache.getCacheStatus() != CacheStatus.STARTED)
         {
            throw new IllegalStateException("The cache should be started first");
         }
         synchronized (this)
         {
            if (modeHandler == null)
            {
               this.modeHandler =
                  new IndexerIoModeHandler(cache.getRPCManager().isCoordinator()
                     || cache.getConfiguration().getCacheMode() == CacheMode.LOCAL ? IndexerIoMode.READ_WRITE
                     : IndexerIoMode.READ_ONLY);
            }
         }
      }
      return modeHandler;
   }
}
