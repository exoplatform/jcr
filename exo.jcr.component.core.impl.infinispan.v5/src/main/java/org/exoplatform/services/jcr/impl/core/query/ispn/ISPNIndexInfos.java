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
package org.exoplatform.services.jcr.impl.core.query.ispn;

import org.exoplatform.services.jcr.impl.core.query.IndexerIoMode;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeHandler;
import org.exoplatform.services.jcr.impl.core.query.IndexerIoModeListener;
import org.exoplatform.services.jcr.impl.core.query.lucene.IndexInfos;
import org.exoplatform.services.jcr.impl.core.query.lucene.MultiIndex;
import org.exoplatform.services.jcr.infinispan.PrivilegedISPNCacheHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.transaction.ActionNonTxAware;
import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

import javax.transaction.TransactionManager;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 22.02.2011
 * 
 * List of indexes is stored in FS and all operations with it are wrapped by IndexInfos class. In
 * standalone mode index and so the list of indexes are managed by indexer and can't be changed 
 * externally. 
 * But in cluster environment all JCR Indexers are reading from shared file system and only one
 * cluster node is writing this index. So read-only cluster nodes should be notified when content
 * of index (actually list of index segments) is changed. 
 * This class is responsible for storing list of segments (indexes) in ISPN cache.
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: ISPNIndexInfos.java 34360 2010-11-11 11:11:11Z tolusha $
 */
@Listener
public class ISPNIndexInfos extends IndexInfos implements IndexerIoModeListener
{

   /**
    * Logger.
    */
   private final Log log = ExoLogger.getLogger("exo.jcr.component.core.impl.infinispan.v5.ISPNIndexInfos");

   private static final String INDEX_NAMES = "$names".intern();

   private static final String SYSINDEX_NAMES = "$sysNames".intern();

   /**
    * ISPN cache.
    */
   protected final Cache<Serializable, Object> cache;

   /**
    * Used to retrieve the current mode.
    */
   protected final IndexerIoModeHandler modeHandler;

   /**
    * Cache key for storing index names.
    */
   protected final IndexInfosKey namesKey;
   
   /**
    * Need to write the index info out of the current transaction 
    * due to the fact that even in auto commit mode, changes are now applied 
    * within a transaction starting from ISPN 5.1
    */
   private final ActionNonTxAware<Void, Void, RuntimeException> write =
      new ActionNonTxAware<Void, Void, RuntimeException>()
      {

         @Override
         protected TransactionManager getTransactionManager()
         {
            return cache.getAdvancedCache().getTransactionManager();
         }

         @Override
         protected Void execute(Void arg) throws RuntimeException
         {
            PrivilegedISPNCacheHelper.put(cache, namesKey, getNames());
            return null;
         }
      };
      
   /**
    * ISPNIndexInfos constructor.
    * 
    * @param wsId
    *          unique workspace identifier
    * @param cache
    *          ISPN cache
    * @param system
    *           notifies if this IndexInfos is from system search manager or not
    * @param modeHandler
    *          used to retrieve the current mode
    */
   public ISPNIndexInfos(String wsId, Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler)
   {
      this(wsId, DEFALUT_NAME, cache, system, modeHandler);
   }

   /**
    * ISPNIndexInfos constructor.
    * 
    * @param wsId
    *          unique workspace identifier
    * @param fileName
    *          name of the file where the infos are stored.
    * @param cache
    *          ISPN cache
    * @param system
    *           notifies if this IndexInfos is from system search manager or not
    * @param modeHandler
    *          used to retrieve the current mode
    */
   public ISPNIndexInfos(String wsId, String fileName, Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler)
   {
      super(fileName);
      this.cache = cache;
      this.modeHandler = modeHandler;
      this.modeHandler.addIndexerIoModeListener(this);
      this.namesKey = new IndexInfosKey(wsId + (system ? SYSINDEX_NAMES : INDEX_NAMES));

      if (modeHandler.getMode() == IndexerIoMode.READ_ONLY)
      {
         // Currently READ_ONLY is set, so new lists should be fired to multiIndex.
         cache.addListener(this);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onChangeMode(IndexerIoMode mode)
   {
      if (mode == IndexerIoMode.READ_WRITE)
      {
         // Now is read-write. Index list is actual and shouldn't be refreshed.
         // Remove listener to avoid asserting if ioMode is RO on each cache event 
         cache.removeListener(this);
         // re-read from FS current actual list.
         try
         {
            if (!multiIndex.isStopped())
            {
               super.read();
            }
         }
         catch (IOException e)
         {
            log.error("Cannot read the list of index names", e);
         }
      }
      else
      {
         // Currently READ_ONLY is set, so new lists should be fired to multiIndex.
         cache.addListener(this);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void write() throws IOException
   {
      // if READ_WRITE and is dirty, then flush. 
      if (isDirty() && modeHandler.getMode() == IndexerIoMode.READ_WRITE)
      {
         // write to FS
         super.write();

         // write to cache
         write.run();
      }
   }

   /**
    * Method will be invoked when a cache entry has been modified only in READ_ONLY mode.
    * 
    * @param event
    *          CacheEntryModifiedEvent
    */
   @CacheEntryModified
   public void cacheEntryModified(CacheEntryModifiedEvent event)
   {
      if (!event.isPre() && event.getKey().equals(namesKey))
      {
         Set<String> set = (Set<String>)event.getValue();
         if (set != null)
         {
            refreshIndexes(set);
         }
      }
   }
   


   /**
    * Update index configuration, when it changes on persistent storage 
    * 
    * @param set
    */
  protected void refreshIndexes(Set<String> set)
  {
     // do nothing if null is passed
     if (set == null)
     {
        return;
     }
     setNames(set);
     // callback multiIndex to refresh lists
     try
     {
        MultiIndex multiIndex = getMultiIndex();
        if (multiIndex != null)
        {
           multiIndex.refreshIndexList();
        }
     }
     catch (IOException e)
     {
        log.error("Failed to update indexes! " + e.getMessage(), e);
     }
  }
   
}
