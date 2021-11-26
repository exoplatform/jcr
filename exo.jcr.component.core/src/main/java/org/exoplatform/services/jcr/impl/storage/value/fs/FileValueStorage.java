/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

package org.exoplatform.services.jcr.impl.storage.value.fs;

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.impl.storage.value.ValueDataResourceHolder;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.storage.value.ValueStoragePlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author Gennady Azarenkov
 * @version $Id: FileValueStorage.java 34801 2009-07-31 15:44:50Z dkatayev $
 */

public abstract class FileValueStorage extends ValueStoragePlugin
{

   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.core.FileValueStorage");

   public final static String PATH = "path";

   /**
    * Temporary directory name under storage root dir.
    */
   public static final String TEMP_DIR_NAME = "temp";

   protected File rootDir;

   protected FileCleaner cleaner;

   protected ValueDataResourceHolder resources;

   /**
    * FileValueStorage constructor.
    */
   public FileValueStorage(FileCleaner fileCleaner)
   {
      this.cleaner = fileCleaner;
   }

   /**
    * {@inheritDoc}
    */
   public void init(Properties props, ValueDataResourceHolder resources) throws IOException,
      RepositoryConfigurationException
   {
      this.resources = resources;
      prepareRootDir(props.getProperty(PATH));
   }

   /**
    * Prepare RootDir.
    * 
    * @param rootDirPath
    *          path
    * @throws IOException
    *           if error
    * @throws RepositoryConfigurationException
    *           if confog error
    */
   protected void prepareRootDir(String rootDirPath) throws IOException, RepositoryConfigurationException
   {
      this.rootDir = new File(rootDirPath);

      if (!rootDir.exists())
      {
         if (rootDir.mkdirs())
         {
            LOG.info("Value storage directory created: " + rootDir.getAbsolutePath());
            
            // create internal temp dir
            File tempDir = new File(rootDir, TEMP_DIR_NAME);
            tempDir.mkdirs();

            if (tempDir.exists() && tempDir.isDirectory())
            {
               // care about storage temp dir cleanup
               for (File tmpf : tempDir.listFiles())
                  if (!tmpf.delete())
                     LOG.warn("Storage temporary directory contains un-deletable file " + tmpf.getAbsolutePath()
                        + ". It's recommended to leave this directory for JCR External Values Storage private use.");
            }
            else
               throw new RepositoryConfigurationException("Cannot create " + TEMP_DIR_NAME
                  + " directory under External Value Storage.");
         }
         else
         {
            LOG.warn("Directory IS NOT created: " + rootDir.getAbsolutePath());
         }
      }
      else
      {
         if (!rootDir.isDirectory())
         {
            throw new RepositoryConfigurationException("File exists but is not a directory " + rootDirPath);
         }
      }
   }
}
