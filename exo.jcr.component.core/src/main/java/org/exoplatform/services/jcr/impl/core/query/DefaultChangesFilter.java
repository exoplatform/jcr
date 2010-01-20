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
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * @author <a href="mailto:Sergey.Kabashnyuk@exoplatform.org">Sergey Kabashnyuk</a>
 * @version $Id: exo-jboss-codetemplates.xml 34360 2009-07-22 23:58:59Z ksm $
 *
 */
public class DefaultChangesFilter extends IndexerChangesFilter
{

   /**
    * @param searchManager
    * @param parentSearchManager
    * @param config
    * @param indexingTree
    * @param parentIndexingTree
    * @param handler
    * @param parentHandler
    * @throws IOException 
    * @throws RepositoryConfigurationException 
    * @throws RepositoryException 
    */
   public DefaultChangesFilter(SearchManager searchManager, SearchManager parentSearchManager,
      QueryHandlerEntry config, IndexingTree indexingTree, IndexingTree parentIndexingTree, QueryHandler handler,
      QueryHandler parentHandler) throws IOException, RepositoryConfigurationException, RepositoryException
   {
      super(searchManager, parentSearchManager, config, indexingTree, parentIndexingTree, handler, parentHandler);
      IndexerIoModeHandler modeHandler = new IndexerIoModeHandler(IndexerIoMode.READ_WRITE);
      handler.setIndexerIoModeHandler(modeHandler);
      parentHandler.setIndexerIoModeHandler(modeHandler);

      if (!parentHandler.isInitialized())
      {
         parentHandler.init();
      }
      if (!handler.isInitialized())
      {
         handler.init();
      }
   }

   /**
    * Logger instance for this class
    */
   private static final Log log = ExoLogger.getLogger(DefaultChangesFilter.class);

   /**
    * @param removedNodes 
    * @param addedNodes 
    * @see org.exoplatform.services.jcr.impl.core.query.IndexerChangesFilter#doUpdateIndex()
    */
   @Override
   protected void doUpdateIndex(Set<String> removedNodes, Set<String> addedNodes, Set<String> parentRemovedNodes,
      Set<String> parentAddedNodes)
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
