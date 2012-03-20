package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.core.query.lucene.ChangesHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.RepositoryException;

/**
 * This class will update the indexes of the related workspace 
 */
public class Indexer
{
   private static final Log log = ExoLogger.getLogger("exo.jcr.component.core.Indexer");

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
   public void updateIndex(Set<String> addedNodes, Set<String> removedNodes, Set<String> parentAddedNodes,
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
   public void updateIndex(ChangesHolder changes, ChangesHolder parentChanges)
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