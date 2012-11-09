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
package org.exoplatform.services.jcr.ext.backup.impl;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Calendar;

/**
 * Created by The eXo Platform SARL Author : Alex Reshetnyak alex.reshetnyak@exoplatform.com.ua Nov
 * 20, 2007
 */
public class FileNameProducer
{
   /**
    * Logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.FileNameProducer");

   class SkipBackupLogFilter
      implements FilenameFilter
   {

      public boolean accept(File dir, String name)
      {
         return !name.endsWith(".xml");
      }
   }

   /**
    * Backup set name.
    */
   private String backupSetName;

   /**
    * Backup set directory.
    */
   private File backupSetDir;

   /**
    * Indicates is full backup or not.
    */
   private boolean isFullBackup;

   /**
    * Indicates that need to create a directory for full backup otherwise is will be the single file.
    */
   private boolean isDirectoryForFullBackup;

   /**
    * Constructor FileNameProducer.
    * 
    * @param backupSetName
    *          backup set name
    * @param backupDir
    *          backup directory
    * @param timeStamp
    *          time stamp for creation unique backup set directory 
    * @param isFullBackup
    *          indicates is full backup or not 
    * @param isDirectory
    *          indicates that need to create a directory for full backup otherwise is will be the single file
    */
   public FileNameProducer(String backupSetName, String backupDir, Calendar timeStamp, boolean isFullBackup,
            boolean isDirectory)
   {
      this.backupSetName = backupSetName;
      this.isFullBackup = isFullBackup;
      this.isDirectoryForFullBackup = isDirectory;

      this.backupSetDir = new File(backupDir);

      if (!PrivilegedFileHelper.exists(backupSetDir))
      {
         PrivilegedFileHelper.mkdirs(backupSetDir);
      }
   }

   public static File generateBackupSetDir(String repositoryName, String workspaceName, String backupDir,
            Calendar timeStamp)
   {
      FileNameProducer fileNameProducer = new FileNameProducer();
      String sTime = "-" + fileNameProducer.getStrDate(timeStamp) + "_" + fileNameProducer.getStrTime(timeStamp);
      File fBackupSetDir = new File(backupDir + File.separator + repositoryName + "_" + workspaceName + sTime);

      if (!PrivilegedFileHelper.exists(fBackupSetDir))
      {
         PrivilegedFileHelper.mkdirs(fBackupSetDir);
      }
      else
      {
         int i = 2;
         do
         {
            fBackupSetDir =
               new File(backupDir + File.separator + repositoryName + "_" + workspaceName + sTime + "_" + i++);
         }
         while (PrivilegedFileHelper.exists(fBackupSetDir));
      }

      return fBackupSetDir;
   }

   /**
    * Constructor FileNameProducer.
    * 
    * @param repositoryName
    *          repository name for creation backup set name
    * @param workspaceName
    *          workspace name for creation backup set name
    * @param backupDir
    *          backup directory
    * @param timeStamp
    *          time stamp for creation unique backup set directory 
    * @param isFullBackup
    *          indicates is full backup or not 
    * @param isDirectory
    *          indicates that need to create a directory for full backup otherwise is will be the single file
    */
   public FileNameProducer(String repositoryName, String workspaceName, String backupDir, Calendar timeStamp,
            boolean isFullBackup, boolean isDirectory)
   {
      this(repositoryName + "_" + workspaceName, backupDir, timeStamp, isFullBackup, isDirectory);
   }

   /**
    * Constructor FileNameProducer.
    * 
    * @param repositoryName
    *          repository name for creation backup set name
    * @param workspaceName
    *          workspace name for creation backup set name
    * @param backupDir
    *          backup directory
    * @param timeStamp
    *          time stamp for creation unique backup set directory 
    * @param isFullBackup
    *          indicates is full backup or not 
    */
   public FileNameProducer(String repositoryName, String workspaceName, String backupDir, Calendar timeStamp,
            boolean isFullBackup)
   {
      this(repositoryName + "_" + workspaceName, backupDir, timeStamp, isFullBackup, false);
   }

   /**
    * Empty constructor.
    */
   public FileNameProducer()
   {
   }

   /**
    * Get next file in backup set.
    * 
    * @return
    *       file
    */
   public File getNextFile()
   {
      File nextFile = null;

      try
      {
         String sNextName = generateName();

         nextFile = new File(backupSetDir.getAbsoluteFile() + File.separator + sNextName);
         if (isFullBackup && isDirectoryForFullBackup)
         {
            if (!PrivilegedFileHelper.exists(nextFile))
            {
               PrivilegedFileHelper.mkdirs(nextFile);
            }
         }
         else
         {
            PrivilegedFileHelper.createNewFile(nextFile);
         }
      }
      catch (IOException e)
      {
         LOG.error("Can nit get next file : " + e.getLocalizedMessage(), e);
      }

      return nextFile;
   }

   /**
    * Generate name for backup file (directory) based on backup set name and current time.
    */
   private String generateName()
   {

      Calendar date = Calendar.getInstance();

      String sDate = getStrDate(date);
      String sTime = getStrTime(date);

      StringBuilder fileName = new StringBuilder(backupSetName).append("-").append(sDate).append("_").append(sTime).append("."); 

      if (isFullBackup)
      {
         fileName.append("0");
      }
      else
      {
         fileName.append(getNextSufix());
      }

      return fileName.toString();
   }

   private String getNextSufix()
   {

      String[] fileList = PrivilegedFileHelper.list(backupSetDir, new SkipBackupLogFilter());

      int sufix = 0;

      for (int i = 0; i < fileList.length; i++)
      {
         String[] stringArray = fileList[i].split("[.]");

         int currentSufix = Integer.valueOf(stringArray[stringArray.length - 1]).intValue();

         if (currentSufix > sufix)
         {
            sufix = currentSufix;
         }
      }

      return String.valueOf(++sufix);
   }

   /**
    * Returns date as String in format YYYYMMDD.
    */
   private String getStrDate(Calendar c)
   {
      int m = c.get(Calendar.MONTH) + 1;
      int d = c.get(Calendar.DATE);
      return "" + c.get(Calendar.YEAR) + (m < 10 ? "0" + m : m) + (d < 10 ? "0" + d : d);
   }

   /**
    * Returns time as String in format HHMMSS.
    */
   private String getStrTime(Calendar c)
   {
      int h = c.get(Calendar.HOUR);
      int m = c.get(Calendar.MINUTE);
      int s = c.get(Calendar.SECOND);
      return "" + (h < 10 ? "0" + h : h) + (m < 10 ? "0" + m : m) + (s < 10 ? "0" + s : s);
   }

   /**
    * Get Backup set directory.
    * 
    * @return File
    *           The backup set directory
    */
   public File getBackupSetDir()
   {
      return backupSetDir;
   }

}
