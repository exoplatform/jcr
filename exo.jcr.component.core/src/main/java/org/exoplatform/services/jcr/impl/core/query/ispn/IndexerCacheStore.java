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
package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.QueryHandler;
import org.exoplatform.services.jcr.impl.core.query.SearchManager;
import org.exoplatform.services.jcr.impl.core.query.jbosscache.ChangesFilterListsWrapper;
import org.exoplatform.services.jcr.impl.core.query.jbosscache.JBossCacheIndexChangesFilter;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.jboss.cache.Fqn;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 23.02.2011
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: IndexerCacheStore.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class IndexerCacheStore extends AbstractCacheStore
{
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.IndexerCacheStore");

   /**
    * A map of all the indexers that has been registered.
    */
   private final Map<Fqn<String>, Indexer> indexers = new HashMap<Fqn<String>, Indexer>();

   protected volatile IndexerIoModeHandler modeHandler;

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
      if (log.isDebugEnabled())
      {
         log.debug("Register " + searchManager.getWsId() + " " + this + " in " + indexers);
      }
   }

   /**
    * @see org.jboss.cache.loader.CacheLoader#put(org.jboss.cache.Fqn, java.lang.Object, java.lang.Object)
    */
   public Object put(Fqn name, Object key, Object value) throws Exception
   {
      if (key.equals(JBossCacheIndexChangesFilter.LISTWRAPPER) && value instanceof ChangesFilterListsWrapper)
      {
         if (log.isDebugEnabled())
         {
            log.info("Received list wrapper, start indexing...");
         }
         // updating index
         ChangesFilterListsWrapper wrapper = (ChangesFilterListsWrapper)value;
         try
         {
            Indexer indexer = indexers.get(name.getParent());
            if (indexer == null)
            {
               log.warn("No indexer could be found for the fqn " + name.getParent());
               if (log.isDebugEnabled())
               {
                  log.debug("The current content of the map of indexers is " + indexers);
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
               //               cache.removeNode(name);
            }
         }
      }
      return null;
   }

   /**
    * Switches Indexer mode from RO to RW, or from RW to RO
    * 
    * @param ioMode
    */
   void setMode(IndexerIoMode ioMode)
   {
      if (modeHandler != null)
      {
         modeHandler.setMode(ioMode);
      }
   }

   /**
    * Get the mode handler.
    */
   IndexerIoModeHandler getModeHandler()
   {
      if (modeHandler == null)
      {
         if (cache.getStatus() != ComponentStatus.RUNNING)
         {
            throw new IllegalStateException("The cache should be started first");
         }
         synchronized (this)
         {
            if (modeHandler == null)
            {
               boolean isCoordinator = cache.getAdvancedCache().getRpcManager().getTransport().isCoordinator();
               this.modeHandler =
                  new IndexerIoModeHandler(isCoordinator ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY);
            }
         }
      }
      return modeHandler;
   }

   /**
    * This class will update the indexes of the related workspace 
    */
   private static class Indexer
   {

      private final SearchManager searchManager;

      private final SearchManager parentSearchManager;

      private final QueryHandler handler;

      private final QueryHandler parentHandler;

      public Indexer(SearchManager searchManager, SearchManager parentSearchManager, QueryHandler handler,
         QueryHandler parentHandler) throws RepositoryConfigurationException
      {
         this.searchManager = searchManager;
         this.parentSearchManager = parentSearchManager;
         this.handler = handler;
         this.parentHandler = parentHandler;
      }

      /**
       * Flushes lists of added/removed nodes to SearchManagers, starting indexing.
       * 
       * @param addedNodes
       * @param removedNodes
       * @param parentAddedNodes
       * @param parentRemovedNodes
       */
      protected void updateIndex(Set<String> addedNodes, Set<String> removedNodes, Set<String> parentAddedNodes,
         Set<String> parentRemovedNodes)
      {
         // pass lists to search manager 
         if (searchManager != null && (addedNodes.size() > 0 || removedNodes.size() > 0))
         {
            try
            {
               searchManager.updateIndex(removedNodes, addedNodes);
            }
            catch (RepositoryException e)
            {
               log.error("Error indexing changes " + e, e);
            }
            catch (IOException e)
            {
               log.error("Error indexing changes " + e, e);
               try
               {
                  handler.logErrorChanges(removedNodes, addedNodes);
               }
               catch (IOException ioe)
               {
                  log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
               }
            }
         }
         // pass lists to parent search manager 
         if (parentSearchManager != null && (parentAddedNodes.size() > 0 || parentRemovedNodes.size() > 0))
         {
            try
            {
               parentSearchManager.updateIndex(parentRemovedNodes, parentAddedNodes);
            }
            catch (RepositoryException e)
            {
               log.error("Error indexing changes " + e, e);
            }
            catch (IOException e)
            {
               log.error("Error indexing changes " + e, e);
               try
               {
                  parentHandler.logErrorChanges(parentRemovedNodes, parentAddedNodes);
               }
               catch (IOException ioe)
               {
                  log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
               }
            }
         }
      }

      /**
       * Flushes lists of added/removed nodes to SearchManagers, starting indexing.
       */
      protected void updateIndex(ChangesHolder changes, ChangesHolder parentChanges)
      {
         // pass lists to search manager 
         if (searchManager != null && changes != null)
         {
            try
            {
               searchManager.apply(changes);
            }
            catch (RepositoryException e)
            {
               log.error("Error indexing changes " + e, e);
            }
            catch (IOException e)
            {
               log.error("Error indexing changes " + e, e);
               try
               {
                  handler.logErrorChanges(new HashSet<String>(changes.getRemove()), new HashSet<String>(changes
                     .getAddIds()));
               }
               catch (IOException ioe)
               {
                  log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
               }
            }
         }
         // pass lists to parent search manager 
         if (parentSearchManager != null && parentChanges != null)
         {
            try
            {
               parentSearchManager.apply(parentChanges);
            }
            catch (RepositoryException e)
            {
               log.error("Error indexing changes " + e, e);
            }
            catch (IOException e)
            {
               log.error("Error indexing changes " + e, e);
               try
               {
                  parentHandler.logErrorChanges(new HashSet<String>(parentChanges.getRemove()), new HashSet<String>(
                     parentChanges.getAddIds()));
               }
               catch (IOException ioe)
               {
                  log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
               }
            }
         }
      }
   }

   @Override
   public void store(InternalCacheEntry entry) throws CacheLoaderException
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void fromStream(ObjectInput inputStream) throws CacheLoaderException
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void toStream(ObjectOutput outputStream) throws CacheLoaderException
   {
      // TODO Auto-generated method stub

   }

   @Override
   public void clear() throws CacheLoaderException
   {
      // TODO Auto-generated method stub

   }

   @Override
   public boolean remove(Object key) throws CacheLoaderException
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public InternalCacheEntry load(Object key) throws CacheLoaderException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<InternalCacheEntry> loadAll() throws CacheLoaderException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Class<? extends CacheLoaderConfig> getConfigurationClass()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   protected void purgeInternal() throws CacheLoaderException
   {
      // TODO Auto-generated method stub

   }
}
