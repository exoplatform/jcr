/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
import org.exoplatform.services.jcr.impl.core.query.jbosscache.RSyncJob;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsAddress;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.cache.notifications.annotation.CacheListener;
import org.jgroups.Channel;
import org.jgroups.Event;
import org.jgroups.stack.IpAddress;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Set;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jan 11, 2012  
 */
@CacheListener
public class RsyncIndexInfos extends ISPNIndexInfos
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.RsyncIndexInfos");

   private final String indexPath;

   private final String urlFormatString;

   private final String rsyncUserName;

   private final String rsyncPassword;

   public RsyncIndexInfos(String wsId, Cache<Serializable, Object> cache, boolean system,
      IndexerIoModeHandler modeHandler, String indexPath, int rsyncPort, String rsyncEntryName, String rsyncEntryPath,
      String rsyncUserName, String rsyncPassword) throws RepositoryConfigurationException
   {
      super(wsId, cache, system, modeHandler);
      this.rsyncUserName = rsyncUserName;
      this.rsyncPassword = rsyncPassword;

      String absoluteRsyncEntryPath;
      try
      {
         this.indexPath = new File(indexPath).getCanonicalPath();
         absoluteRsyncEntryPath = new File(rsyncEntryPath).getCanonicalPath();
      }
      catch (IOException e)
      {
         throw new RepositoryConfigurationException("Index path or rsyncEntry path is invalid.", e);
      }

      if (this.indexPath.startsWith(absoluteRsyncEntryPath))
      {
         // in relation to RSync Server Entry
         // i.e. absolute index path is /var/portal/data/index/repo1/ws2
         // i.e. RSync Server Entry is "index" pointing to /var/portal/data/index
         // then relative path is repo1/ws2
         // and whole url is "rsync://<addr>:<port>/<entryName>/repo1/ws2"
         String relativeIndexPath = this.indexPath.substring(absoluteRsyncEntryPath.length());

         // if client is Windows OS, need to replace all '\' with '/' used in url
         if (File.separatorChar == '\\')
         {
            relativeIndexPath = relativeIndexPath.replace(File.separatorChar, '/');
         }
         // generate ready-to-use formatter string with address variable 
         urlFormatString = "rsync://%s:" + rsyncPort + "/" + rsyncEntryName + relativeIndexPath + "/";
      }
      else
      {
         throw new RepositoryConfigurationException(
            "Invalid RSync configuration. Index must be placed in folder that is a descendant of RSync Server Entry. "
               + "Current RSync Server Entry Path is : " + absoluteRsyncEntryPath
               + " but it doesnt hold Index folder, that is : " + this.indexPath
               + ". Please fix configuration according to JCR Documentation and restart application.");
      }

   }

   /**
   * {@inheritDoc}
   */
   @Override
   protected void refreshIndexes(Set<String> set)
   {
      triggerRSyncSynchronization();
      // call super, after indexes are synchronized
      super.refreshIndexes(set);
   }

   @Override
   public void read() throws IOException
   {
      // synchronizing indexes on read access to index list for Read-Only mode
      // allowing to synchronize indexes on startup
      triggerRSyncSynchronization();
      super.read();
   }

   /**
    * Call to system RSync binary implementation, 
    */
   private void triggerRSyncSynchronization()
   {
      // Call RSync to retrieve actual index from coordinator
      if (modeHandler.getMode() == IndexerIoMode.READ_ONLY)
      {
         EmbeddedCacheManager cacheManager = cache.getCacheManager();

         if (cacheManager.getCoordinator() instanceof JGroupsAddress
            && cacheManager.getTransport() instanceof JGroupsTransport)
         {
            JGroupsTransport transport = (JGroupsTransport)cacheManager.getTransport();
            // Coordinator's address
            org.jgroups.Address jgAddress = ((JGroupsAddress)cacheManager.getCoordinator()).getJGroupsAddress();
            // if jgAddress is UUID address, not the physical one, then retrieve via channel
            if (!(jgAddress instanceof IpAddress))
            {
               // this is the only way of getting physical address. 
               Channel channel = transport.getChannel();
               jgAddress = (org.jgroups.Address)channel.down(new Event(Event.GET_PHYSICAL_ADDRESS, jgAddress));
            }
            if (jgAddress instanceof IpAddress)
            {
               String address = ((IpAddress)jgAddress).getIpAddress().getHostAddress();
               RSyncJob rSyncJob =
                  new RSyncJob(String.format(urlFormatString, address), indexPath, rsyncUserName, rsyncPassword);
               try
               {
                  // synchronizing access to RSync Job.
                  // No parallel jobs allowed
                  synchronized (this)
                  {
                     rSyncJob.execute();
                  }
               }
               catch (IOException e)
               {
                  LOG.error("Failed to retrieve index using RSYNC", e);
               }
            }
            else
            {
               LOG.error("Error triggering RSync synchronization, skipped. Unsupported Address object : "
                  + jgAddress.getClass().getName());
            }
         }
         else
         {
            LOG.error("Error triggering RSync synchronization, skipped. Unsupported Address object : "
               + cacheManager.getCoordinator().getClass().getName());
         }
      }
   }
}
