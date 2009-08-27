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

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.log.Log;

import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.core.nodetype.NodeTypeDataManager;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.core.NamespaceRegistryImpl;
import org.exoplatform.services.jcr.impl.dataflow.persistent.WorkspacePersistentDataManager;
import org.exoplatform.services.log.ExoLogger;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:Sergey.Kabashnyuk@gmail.com">Sergey Kabashnyuk</a>
 * @version $Id: SystemSearchManager.java 13891 2008-05-05 16:02:30Z pnedonosko
 *          $
 */
public class SystemSearchManager
   extends SearchManager
{

   /**
    * Class logger.
    */
   private final Log log = ExoLogger.getLogger("jcr.SystemSearchManager");

   /**
    * Is started flag.
    */
   private boolean isStarted = false;

   /**
    * ChangesLog Buffer (used for saves before start).
    */
   private List<ItemStateChangesLog> changesLogBuffer = new ArrayList<ItemStateChangesLog>();

   public static final String INDEX_DIR_SUFFIX = "system";

   public SystemSearchManager(QueryHandlerEntry config, NamespaceRegistryImpl nsReg, NodeTypeDataManager ntReg,
            WorkspacePersistentDataManager itemMgr, DocumentReaderService service, ConfigurationManager cfm)
            throws RepositoryException, RepositoryConfigurationException
   {
      super(config, nsReg, ntReg, itemMgr, null, service, cfm);
   }

   @Override
   public void onSaveItems(ItemStateChangesLog changesLog)
   {
      if (!isStarted)
      {
         changesLogBuffer.add(changesLog);
      }
      else
      {
         super.onSaveItems(changesLog);
      }
   }

   @Override
   public void start()
   {
      indexingRoot = Constants.JCR_SYSTEM_PATH;
      excludedPaths.remove(Constants.JCR_SYSTEM_PATH);
      isStarted = true;
      handler.init();
      for (ItemStateChangesLog bufferedChangesLog : changesLogBuffer)
      {
         super.onSaveItems(bufferedChangesLog);
      }
      changesLogBuffer.clear();
      changesLogBuffer = null;
   }

   @Override
   protected QueryHandlerContext createQueryHandlerContext(QueryHandler parentHandler)
            throws RepositoryConfigurationException
   {
      QueryHandlerContext context =
               new QueryHandlerContext(itemMgr, Constants.SYSTEM_UUID, nodeTypeDataManager, nsReg, parentHandler,
                        config.getIndexDir() + "_" + INDEX_DIR_SUFFIX, extractor);
      return context;
   }
}
