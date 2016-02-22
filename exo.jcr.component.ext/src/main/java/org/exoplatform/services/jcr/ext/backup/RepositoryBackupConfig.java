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
package org.exoplatform.services.jcr.ext.backup;

import java.io.File;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date: 2010
 *
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a> 
 * @version $Id$
 */
public class RepositoryBackupConfig
{
   /**
    * The backup type.
    */
   private int backupType;

   /**
    * The repository name.
    */
   private String repository;

   /**
    * The incremental job period.
    */
   private long incrementalJobPeriod;

   /**
    * The incremental job number (the repetition numper).
    */
   private int incrementalJobNumber;

   /**
    * The backup directory.
    */
   private File backupDir;

   /**
    * Getting incremental job period.
    *
    * @return long
    *           return incremental job period.
    */
   public long getIncrementalJobPeriod()
   {
      return incrementalJobPeriod;
   }

   /**
    * Setting incremental job period.
    *
    * @param incrementalJobPeriod
    *          long, the incremental job period
    */
   public void setIncrementalJobPeriod(long incrementalJobPeriod)
   {
      this.incrementalJobPeriod = incrementalJobPeriod;
   }

   /**
    * Getting incremental job period (the repetition period).
    *
    * @return int
    *          return incremental job period 
    */
   public int getIncrementalJobNumber()
   {
      return incrementalJobNumber;
   }

   /**
    * Setting incremental job number (the repetition period).
    *
    * @param incrementalJobNumber
    *          int, the incremental job number
    *          
    */
   public void setIncrementalJobNumber(int incrementalJobNumber)
   {
      this.incrementalJobNumber = incrementalJobNumber;
   }

   /**
    * Getting the repository name.
    *
    * @return String
    *           return the repository name
    */
   public String getRepository()
   {
      return repository;
   }

   /**
    * Setting the repository name.
    *
    * @param repository
    *          String, the repository name
    */
   public void setRepository(String repository)
   {
      this.repository = repository;
   }

   /**
    * Getting backup directory.
    *
    * @return File
    *           return c 
    */
   public File getBackupDir()
   {
      return backupDir;
   }

   /**
    * Setting the backup directory.
    *
    * @param backupDir
    *          File, the backup directory
    */
   public void setBackupDir(File backupDir)
   {
      this.backupDir = backupDir;
   }

   /**
    * Getting the backup type.
    *
    * @return int
    *           return the backup type
    */
   public int getBackupType()
   {
      return backupType;
   }

   /**
    * Setting the backup type.
    *
    * @param backupType
    *          int, the backup type
    */
   public void setBackupType(int backupType)
   {
      this.backupType = backupType;
   }
}
