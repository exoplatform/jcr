/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.datamodel.QPath;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SystemSearchManager.java 13891 2008-05-05 16:02:30Z pnedonosko
 *          $
 */
public class SystemSearchManager extends SearchManager
{

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.SystemSearchManager");

   /**
    * Is started flag.
    */
   private boolean isStarted = false;

   //
   //   /**
   //    * ChangesLog Buffer (used for saves before start).
   //    */
   //   private List<ItemStateChangesLog> changesLogBuffer = new ArrayList<ItemStateChangesLog>();
   //
   //   /**
   //    * ChangesLog Buffer (used for saves before start).
   //    */
   //   private List<List<WriteCommand>> writeCommandBuffer = new ArrayList<List<WriteCommand>>();

   public static final String INDEX_DIR_SUFFIX = "system";

   public SystemSearchManager(QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
      WorkspacePersistentDataManager itemMgr, DocumentReaderService service, ConfigurationManager cfm,
      RepositoryIndexSearcherHolder indexSearcherHolder) throws RepositoryException, RepositoryConfigurationException
   {
      super(config, nsReg, ntReg, itemMgr, null, service, cfm, indexSearcherHolder);
   }

   //   @Override
   //   public void onSaveItems(ItemStateChangesLog changesLog)
   //   {
   //      if (!isStarted)
   //      {
   //         changesLogBuffer.add(changesLog);
   //      }
   //      else
   //      {
   //         super.onSaveItems(changesLog);
   //      }
   //   }

   @Override
   public void start()
   {
      if (!isStarted)
      {

         try
         {
            if (indexingTree == null)
            {
               List<QPath> excludedPaths = new ArrayList<QPath>();

               NodeData indexingRootNodeData = (NodeData)itemMgr.getItemData(Constants.SYSTEM_UUID);

               indexingTree = new IndexingTree(indexingRootNodeData, excludedPaths);
            }
            initializeQueryHandler();

         }

         catch (RepositoryException e)
         {
            log.error(e.getLocalizedMessage());
            handler = null;
            //freeBuffers();
            throw new RuntimeException(e);
         }
         catch (RepositoryConfigurationException e)
         {
            log.error(e.getLocalizedMessage());
            handler = null;
            //freeBuffers();
            throw new RuntimeException(e);
         }
         isStarted = true;
      }
      //      if (changesLogBuffer.size() > 0)
      //      {
      //         for (ItemStateChangesLog bufferedChangesLog : changesLogBuffer)
      //         {
      //            super.onSaveItems(bufferedChangesLog);
      //         }
      //
      //      }
      //
      //      if (writeCommandBuffer.size() > 0)
      //      {
      //         try
      //         {
      //            for (List<WriteCommand> bufferedWriteLog : writeCommandBuffer)
      //            {
      //               super.onSaveItems(bufferedWriteLog);
      //            }
      //         }
      //         catch (RepositoryException e)
      //         {
      //            freeBuffers();
      //            throw new RuntimeException(e);
      //
      //         }
      //      }
      //      freeBuffers();
   }

   //   /**
   //    * @see org.exoplatform.services.jcr.impl.core.query.SearchManager#initializeChangesFilter()
   //    */
   //   @Override
   //   protected void initializeChangesFilter() throws RepositoryException, RepositoryConfigurationException
   //   {
   //      Class<? extends IndexerChangesFilter> changesFilterClass = DefaultChangesFilter.class;
   //      String changesFilterClassName = config.getParameterValue(QueryHandlerParams.PARAM_CHANGES_FILTER_CLASS, null);
   //      try
   //      {
   //         if (changesFilterClassName != null)
   //         {
   //            changesFilterClass =
   //               (Class<? extends IndexerChangesFilter>)Class.forName(changesFilterClassName, true, this.getClass()
   //                  .getClassLoader());
   //         }
   //         Constructor<? extends IndexerChangesFilter> constuctor =
   //            changesFilterClass.getConstructor(SearchManager.class, QueryHandlerEntry.class, Boolean.class,
   //               IndexingTree.class);
   //         changesFilter = constuctor.newInstance(this, config, true, indexingTree);
   //      }
   //      catch (SecurityException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //      catch (IllegalArgumentException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //      catch (ClassNotFoundException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //      catch (NoSuchMethodException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //      catch (InstantiationException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //      catch (IllegalAccessException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //      catch (InvocationTargetException e)
   //      {
   //         throw new RepositoryException(e.getMessage(), e);
   //      }
   //   }

   //   private void freeBuffers()
   //   {
   //      changesLogBuffer.clear();
   //      changesLogBuffer = null;
   //      writeCommandBuffer.clear();
   //      writeCommandBuffer = null;
   //   }

   @Override
   protected QueryHandlerContext createQueryHandlerContext(QueryHandler parentHandler)
      throws RepositoryConfigurationException
   {
      QueryHandlerContext context =
         new QueryHandlerContext(itemMgr, indexingTree, nodeTypeDataManager, nsReg, parentHandler, getIndexDir() + "_"
            + INDEX_DIR_SUFFIX, extractor, true, virtualTableResolver);
      return context;
   }
   //
   //   /* (non-Javadoc)
   //    * @see org.exoplatform.services.jcr.impl.core.query.SearchManager#onSaveItems(java.util.List)
   //    */
   //   @Override
   //   public void onSaveItems(List<WriteCommand> modifications) throws RepositoryException
   //   {
   //      if (!isStarted)
   //      {
   //         writeCommandBuffer.add(modifications);
   //      }
   //      else
   //      {
   //         super.onSaveItems(modifications);
   //      }
   //
   //   }
}
