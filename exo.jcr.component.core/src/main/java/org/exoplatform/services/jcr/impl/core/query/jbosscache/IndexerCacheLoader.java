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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: IndexerCacheLoader.java 34360 2009-07-22 23:58:59Z nzamosenchuk $
 *
 */
public class IndexerCacheLoader extends AbstractWriteOnlyCacheLoader
{
   private final Log log = ExoLogger.getLogger(this.getClass().getName());

   private SearchManager searchManager;

   private SearchManager parentSearchManager;

   private QueryHandler handler;

   private QueryHandler parentHandler;
   
   private volatile IndexerIoModeHandler modeHandler;

   /**
    * @see org.jboss.cache.loader.AbstractCacheLoader#commit(java.lang.Object)
    */
   @Override
   public void commit(Object tx) throws Exception
   {
      // do nothing. Everything is done on prepare phase.
   }

   /**
    * Inject dependencies needed for CacheLoader: SearchManagers and QueryHandlers. 
    * 
    * @param searchManager
    * @param parentSearchManager
    * @param handler
    * @param parentHandler
    * @throws RepositoryConfigurationException
    */
   public void init(SearchManager searchManager, SearchManager parentSearchManager, QueryHandler handler,
      QueryHandler parentHandler) throws RepositoryConfigurationException
   {
      this.searchManager = searchManager;
      this.parentSearchManager = parentSearchManager;
      this.handler = handler;
      this.parentHandler = parentHandler;
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
            updateIndex(wrapper.getAddedNodes(), wrapper.getRemovedNodes(), wrapper.getParentAddedNodes(), wrapper
               .getParentRemovedNodes());
         }
         finally
         {
            // remove the data from the cache
            cache.removeNode(name);
         }
      }
      return null;
   }

   /**
    * @see org.exoplatform.services.jcr.impl.storage.jbosscache.AbstractWriteOnlyCacheLoader#put(org.jboss.cache.Fqn, java.util.Map)
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
      if (modeHandler != null)
      {
         modeHandler.setMode(ioMode);         
      }
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
               this.modeHandler = new IndexerIoModeHandler(cache.getRPCManager().isCoordinator() ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY);               
            }
         }
      }
      return modeHandler;
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
               parentHandler.logErrorChanges(removedNodes, addedNodes);
            }
            catch (IOException ioe)
            {
               log.warn("Exception occure when errorLog writed. Error log is not complete. " + ioe, ioe);
            }
         }
      }
   }
}
