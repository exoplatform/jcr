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
package org.exoplatform.services.jcr.impl.dataflow;

import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.FileCleanerHolder;
import org.exoplatform.services.jcr.storage.WorkspaceDataContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple class wrapper. Contains all needed variables for spooling input stream.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: SpoolConfig.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SpoolConfig
{
   private final static Log LOG = ExoLogger.getLogger(SpoolConfig.class);

   public FileCleaner fileCleaner;

   public File tempDirectory = new File(PropertyManager.getProperty("java.io.tmpdir"));
   
   private static final String  FORCE_CLEAN_SWAP_LIVE_TIME = "exo.jcr.spoolConfig.swap.live.time";

   public int maxBufferSize = WorkspaceDataContainer.DEF_MAXBUFFERSIZE;


   private static Map<String, SpoolConfig> spoolConfigList = new HashMap<String, SpoolConfig>();
   
   public static int liveTime = -1 ;

   static
   {
      try
      {
         liveTime = Integer.parseInt(System.getProperty(FORCE_CLEAN_SWAP_LIVE_TIME));
      }
      catch (NumberFormatException nex)
      {
         LOG.warn("Parameter {} is not a valid number, default value will be used is -1", FORCE_CLEAN_SWAP_LIVE_TIME);
      }
   }

   /**
    * SpoolConfig constructor.
    */
   public SpoolConfig(FileCleaner fileCleaner)
   {
      this.fileCleaner = fileCleaner;
   }

   public static SpoolConfig getDefaultSpoolConfig()
   {
      return new SpoolConfig(FileCleanerHolder.getDefaultFileCleaner());
   }

   public static File getSwapPath(String workspaceName)
   {
      SpoolConfig  sp = spoolConfigList.get(workspaceName);
      return ((sp != null) ? sp.tempDirectory : null);
   }

   public static void addSpoolConfig(String workspaceName, SpoolConfig spoolConfig)
   {
      spoolConfigList.put(workspaceName, spoolConfig);
   }

}