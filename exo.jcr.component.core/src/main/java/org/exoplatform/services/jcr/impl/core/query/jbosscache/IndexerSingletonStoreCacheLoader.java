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
package org.exoplatform.services.jcr.impl.core.query.jbosscache;

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jboss.cache.Fqn;
import org.jboss.cache.NodeSPI;
import org.jboss.cache.loader.SingletonStoreCacheLoader;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author <a href="mailto:nikolazius@gmail.com">Nikolay Zamosenchuk</a>
 * @version $Id: IndexerSingletonStoreCacheLoader.java 1008 2009-12-11 15:14:51Z nzamosenchuk $
 *
 */
public class IndexerSingletonStoreCacheLoader extends SingletonStoreCacheLoader
{
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.IndexerSingletonStoreCacheLoader");

   /**
    * @see org.jboss.cache.loader.SingletonStoreCacheLoader#activeStatusChanged(boolean)
    */
   @Override
   protected void activeStatusChanged(boolean newActiveState) throws PushStateException
   {
      // at first change indexer mode
      setIndexerMode(newActiveState);
      // and them push states if needed
      super.activeStatusChanged(newActiveState);
   }

   @Override
   protected Callable<?> createPushStateTask()
   {
      return new Callable()
      {
         public Object call() throws Exception
         {
            final boolean debugEnabled = log.isDebugEnabled();

            if (debugEnabled)
               log.debug("start pushing in-memory state to cache cacheLoader collection");

            // merging all lists stored in memory
            Collection<NodeSPI> children = cache.getRoot().getChildren();
            for (NodeSPI wsChildren : children)
            {
               final Set<String> removedNodes = new HashSet<String>();
               final Set<String> addedNodes = new HashSet<String>();
               final Set<String> parentRemovedNodes = new HashSet<String>();
               final Set<String> parentAddedNodes = new HashSet<String>();
               Collection<NodeSPI> changes = wsChildren.getChildren();
               for (NodeSPI aChildren : changes)
               {
                  Fqn<?> fqn = aChildren.getFqn();
                  Object value = cache.get(fqn, JBossCacheIndexChangesFilter.LISTWRAPPER);
                  if (value != null && value instanceof ChangesFilterListsWrapper)
                  {
                     // get wrapper object
                     ChangesFilterListsWrapper listsWrapper = (ChangesFilterListsWrapper)value;
                     // get search manager lists
                     addedNodes.addAll(listsWrapper.getAddedNodes());
                     removedNodes.addAll(listsWrapper.getRemovedNodes());
                     // parent search manager lists
                     parentAddedNodes.addAll(listsWrapper.getParentAddedNodes());
                     parentRemovedNodes.addAll(listsWrapper.getParentAddedNodes());
                  }                  
               }
               //TODO: recover logic is here, lists are: removedNodes and addedNodes      String id = IdGenerator.generate();
               String id = IdGenerator.generate();
               cache.put(Fqn.fromRelativeElements(wsChildren.getFqn(), id), JBossCacheIndexChangesFilter.LISTWRAPPER, new ChangesFilterListsWrapper(addedNodes,
                  removedNodes, parentAddedNodes, parentRemovedNodes));
            }
            if (debugEnabled)
               log.debug("in-memory state passed to cache cacheLoader successfully");
            return null;
         }
      };
   }

   /**
    * Sets/changes indexer mode 
    * 
    * @param writeEnabled
    */
   protected void setIndexerMode(boolean writeEnabled)
   {
      // get base cache loader that is configured under SingletonStoreCacheLoader
      // if it is IndexerCacheLoader need to call setMode(ioMode)
      if (getCacheLoader() instanceof IndexerCacheLoader)
      {
         // if newActiveState is true IndexerCacheLoader is coordinator with write enabled;
         ((IndexerCacheLoader)getCacheLoader()).setMode(writeEnabled ? IndexerIoMode.READ_WRITE
            : IndexerIoMode.READ_ONLY);
         log.info("Set indexer io mode to:" + (writeEnabled ? IndexerIoMode.READ_WRITE : IndexerIoMode.READ_ONLY));
      }
   }
}
