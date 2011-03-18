/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.services.jcr.impl.backup.rdbms;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.jcr.impl.backup.BackupException;
import org.exoplatform.services.jcr.impl.backup.DataRestor;
import org.exoplatform.services.jcr.impl.util.io.DirectoryHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:anatoliy.bazko@gmail.com">Anatoliy Bazko</a>
 * @version $Id: DirectoryRestorer.java 34360 2009-07-22 23:58:59Z tolusha $
 */
public class DirectoryRestor implements DataRestor
{

   /**
    * Logger.
    */
   protected final static Log LOG = ExoLogger.getLogger("exo.jcr.component.core.DirectoryRestorer");

   /**
    * The list of directories with actual data.
    */
   protected final List<File> dataDirs = new ArrayList<File>();

   /**
    * The list of directories with backuped data.
    */
   protected final List<File> backupDirs = new ArrayList<File>();

   /**
    * The list of temporary directories.
    */
   private final List<File> tmpDirs = new ArrayList<File>();

   /**
    * Java temporary directory.
    */
   protected final File tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

   /**
    * The prefix for temporary directories.
    */
   private static final String PREFIX = "fsrestorer";

   /**
    * Guarantee the unique name.
    */
   private static volatile int uniqueIndex = 0;

   /**
    * Constructor DirectoryRestorer.
    * 
    * @param dataDirs
    * @param backupDirs
    */
   public DirectoryRestor(List<File> dataDirs, List<File> backupDirs)
   {
      this.dataDirs.addAll(dataDirs);
      this.backupDirs.addAll(backupDirs);
   }

   /**
    * Constructor DirectoryRestorer.
    * 
    * @param dataDir
    * @param backupDir
    */
   public DirectoryRestor(File dataDir, File backupDir)
   {
      this.dataDirs.add(dataDir);
      this.backupDirs.add(backupDir);
   }

   /**
    * {@inheritDoc}
    */
   public void clean() throws BackupException
   {
      for (File dataDir : dataDirs)
      {
         try
         {
            File tmpDir = new File(tempDir, PREFIX + (System.currentTimeMillis() + uniqueIndex++));
            DirectoryHelper.copyDirectory(dataDir, tmpDir);

            tmpDirs.add(tmpDir);

            DirectoryHelper.removeDirectory(dataDir);
         }
         catch (IOException e)
         {
            throw new BackupException(e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void restore() throws BackupException
   {
      for (int i = 0; i < backupDirs.size(); i++)
      {
         File backupDir = backupDirs.get(i);
         File dataDir = dataDirs.get(i);

         try
         {
            DirectoryHelper.uncompressDirectory(backupDir, dataDir);
         }
         catch (IOException e)
         {
            throw new BackupException(e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void commit() throws BackupException
   {
   }

   /**
    * {@inheritDoc}
    */
   public void rollback() throws BackupException
   {
      for (int i = 0; i < tmpDirs.size(); i++)
      {
         try
         {
            File tmpDir = tmpDirs.get(i);
            File dataDir = dataDirs.get(i);

            if (PrivilegedFileHelper.exists(dataDir))
            {
               DirectoryHelper.removeDirectory(dataDir);
            }

            DirectoryHelper.copyDirectory(tmpDir, dataDir);
         }
         catch (IOException e)
         {
            throw new BackupException(e);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws BackupException
   {
      for (File tmpDir : tmpDirs)
      {
         try
         {
            DirectoryHelper.removeDirectory(tmpDir);
         }
         catch (IOException e)
         {
            LOG.error("Can't remove temporary directory " + PrivilegedFileHelper.getAbsolutePath(tmpDir), e);
         }
      }

      dataDirs.clear();
      backupDirs.clear();
      tmpDirs.clear();
   }
}
