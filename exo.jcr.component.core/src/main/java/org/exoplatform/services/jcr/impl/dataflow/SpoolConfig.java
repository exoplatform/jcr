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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple class wrapper. Contains all needed variables for spooling input stream.
 * 
 * @author <a href="abazko@exoplatform.com">Anatoliy Bazko</a>
 * @version $Id: SpoolConfig.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class SpoolConfig
{
   /**
    * The Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.SpoolConfig");

   /**
    *  Name of max swap size system property parameter.
    */
   private static final String  MAX_SWAP_SIZE = "exo.jcr.core.spoolConfig.swap-size";

   public FileCleaner fileCleaner;

   public File tempDirectory = new File(PropertyManager.getProperty("java.io.tmpdir"));

   public int maxBufferSize = WorkspaceDataContainer.DEF_MAXBUFFERSIZE;

   /**
    *  Accumulate swap size (K bytes).
    */
   public static AtomicLong accumulateSwapSize = new AtomicLong();

   /**
    * The max swap size.
    */
   public static final long maxSwapSize;
   static
   {
      String size = PropertyManager.getProperty(MAX_SWAP_SIZE);
      int value = 0;
      if (size != null)
      {
         try
         {
            value = Integer.valueOf(size) * 1024 * 1024 ;
         }
         catch (NumberFormatException e)
         {
            LOG.warn("The value of the property '" + MAX_SWAP_SIZE
               + "' must be an integer, the default value will be used (0).");
         }
      }
      maxSwapSize = value;
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

}