/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.exoplatform.services.jcr.impl.core.query;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.management.jmx.annotations.NameTemplate;
import org.exoplatform.management.jmx.annotations.Property;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.config.WorkspaceEntry;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.datamodel.NodeData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rpc.RPCService;

import java.io.File;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SystemSearchManager.java 13891 2008-05-05 16:02:30Z pnedonosko
 *          $
 */
@NameTemplate(@Property(key = "service", value = "SystemSearchManager"))
public class SystemSearchManager extends SearchManager
{

   /**
    * Class logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SystemSearchManager");

   /**
    * Is started flag.
    */
   private boolean isStarted = false;

   public static final String INDEX_DIR_SUFFIX = "system";

   public SystemSearchManager(ExoContainerContext ctx, WorkspaceEntry wEntry, RepositoryEntry rEntry,
      RepositoryService rService, QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
      WorkspacePersistentDataManager itemMgr, DocumentReaderService service, ConfigurationManager cfm,
      RepositoryIndexSearcherHolder indexSearcherHolder, RPCService rpcService, FileCleanerHolder cleanerHolder)
      throws RepositoryException, RepositoryConfigurationException
   {
      super(ctx, wEntry, rEntry, rService, config, nsReg, ntReg, itemMgr, null, service, cfm, indexSearcherHolder,
         rpcService, cleanerHolder);
   }

   public SystemSearchManager(ExoContainerContext ctx, WorkspaceEntry wEntry, RepositoryEntry rEntry,
      RepositoryService rService, QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
      WorkspacePersistentDataManager itemMgr, DocumentReaderService service, ConfigurationManager cfm,
      RepositoryIndexSearcherHolder indexSearcherHolder, FileCleanerHolder cleanerHolder) throws RepositoryException,
      RepositoryConfigurationException
   {
      this(ctx, wEntry, rEntry, rService, config, nsReg, ntReg, itemMgr, service, cfm, indexSearcherHolder, null,
         cleanerHolder);
   }

   @Override
   public void start()
   {
      if (!isStarted)
      {

         try
         {
            if (indexingTree == null)
            {
               NodeData indexingRootNodeData = (NodeData)itemMgr.getItemData(Constants.SYSTEM_UUID);

               indexingTree = new IndexingTree(indexingRootNodeData, null);
            }
            initializeQueryHandler();
         }
         catch (RepositoryException e)
         {
            LOG.error(e.getLocalizedMessage());
            handler = null;
            //freeBuffers();
            throw new RuntimeException(e);
         }
         catch (RepositoryConfigurationException e)
         {
            LOG.error(e.getLocalizedMessage());
            handler = null;
            //freeBuffers();
            throw new RuntimeException(e);
         }
         isStarted = true;
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected File getIndexDirectory() throws RepositoryConfigurationException
   {
      return new File(getIndexDirParam() + "_" + INDEX_DIR_SUFFIX);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected String getStorageName()
   {
      return super.getStorageName() + "_" + INDEX_DIR_SUFFIX;
   }
   
}
