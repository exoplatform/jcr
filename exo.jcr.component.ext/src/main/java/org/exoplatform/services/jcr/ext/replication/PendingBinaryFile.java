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
package org.exoplatform.services.jcr.ext.replication;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: PendingBinaryFile.java 111 2008-11-11 11:11:11Z rainf0x $
 */

public class PendingBinaryFile
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("exo.jcr.component.ext.PendingBinaryFile");

   /**
    * The map for FileDesctiptor per owner.
    */
   private HashMap<String, HashMap<String, ChangesFile>> mapFilePerOwner;

   /**
    * Need transfer counter.
    */
   private long needTransferCounter;

   /**
    * Successful transfer counter.
    */
   private long successfulTransferCounter;

   /**
    * Removed old ChangesLog counter.
    */
   private long removedOldChangesLogCounter;

   /**
    * Successful transfer 'flag'.
    */
   private boolean isSuccessfulTransfer;

   /**
    * Successful save 'flag'.
    */
   private boolean isSuccessfulSave;

   /**
    * PendingBinaryFile constructor.
    */
   public PendingBinaryFile()
   {
      mapFilePerOwner = new HashMap<String, HashMap<String, ChangesFile>>();
      needTransferCounter = 0;
      removedOldChangesLogCounter = 0;
      successfulTransferCounter = 0;
      isSuccessfulTransfer = false;
      isSuccessfulSave = false;
   }

   /**
    * addBinaryFile.
    * 
    * @param ownerName
    *          owner name
    * @param fileName
    *          name of file
    * @param systemId
    *          String of system identification
    * @param totalPacketCount
    *          long, the total packets count
    * @return ChangesFile
    *           return the ChangesFile 
    * @throws IOException
    *           will be generated IOException
    */
   public ChangesFile addChangesFile(String ownerName, String fileName, String systemId, long totalPacketCount)
      throws IOException
   {
      File f = PrivilegedFileHelper.createTempFile(fileName, "");
      ChangesFile fileDescriptor = new ChangesFile(f, systemId, totalPacketCount);

      HashMap<String, ChangesFile> fileMap;

      if (!mapFilePerOwner.containsKey(ownerName))
      {
         fileMap = new HashMap<String, ChangesFile>();
         mapFilePerOwner.put(ownerName, fileMap);
      }
      else
      {
         fileMap = mapFilePerOwner.get(ownerName);
      }

      fileMap.put(fileName, fileDescriptor);
      return fileDescriptor;
   }

   /**
    * getFileDescriptor.
    * 
    * @param ownName
    *          owner name
    * @param fileName
    *          name of file
    * @return FileDescriptor return the FileDescriptor
    */
   public ChangesFile getChangesFile(String ownName, String fileName)
   {
      if (mapFilePerOwner.containsKey(ownName))
      {
         HashMap<String, ChangesFile> fileMap = mapFilePerOwner.get(ownName);
         return fileMap.get(fileName);
      }
      return null;
   }

   /**
    * getSortedFilesDescriptorList.
    * 
    * @return List return the list of FileDescriptors
    */
   public List<ChangesFile> getSortedFilesDescriptorList()
   {

      ArrayList<ChangesFile> fileDescriptorhList = new ArrayList<ChangesFile>();

      for (String ownerName : mapFilePerOwner.keySet())
      {
         HashMap<String, ChangesFile> fileMap = mapFilePerOwner.get(ownerName);

         fileDescriptorhList.addAll(fileMap.values());
      }

      if (log.isDebugEnabled())
         log.debug("getSortedFilePath() : " + fileDescriptorhList.size());

      Collections.sort(fileDescriptorhList);

      if (log.isDebugEnabled())
      {
         log.debug("\n\nList has been sorted :\n");
         for (ChangesFile fd : fileDescriptorhList)
            log.debug(PrivilegedFileHelper.getAbsolutePath(fd.getFile()));
      }

      return fileDescriptorhList;
   }

   /**
    * getFileNameList.
    * 
    * @return List return the list of names of files
    */
   public List<String> getFileNameList()
   {
      ArrayList<String> list = new ArrayList<String>();

      for (String ownerName : mapFilePerOwner.keySet())
      {
         HashMap<String, ChangesFile> fileMap = mapFilePerOwner.get(ownerName);

         for (String fileName : fileMap.keySet())
            list.add(fileName);
      }

      return list;
   }

   /**
    * getNeedTransferCounter.
    * 
    * @return long return the needTransferCounter
    */
   public long getNeedTransferCounter()
   {
      return needTransferCounter;
   }

   /**
    * setNeedTransferCounter.
    * 
    * @param needTransferCounter
    *          set the needTransferCounter
    */
   public void setNeedTransferCounter(long needTransferCounter)
   {
      this.needTransferCounter = needTransferCounter;
   }

   /**
    * getRemovedOldChangesLogCounter.
    * 
    * @return long return the removedOldChangesLogCounter
    */
   public long getRemovedOldChangesLogCounter()
   {
      return removedOldChangesLogCounter;
   }

   /**
    * setRemovedOldChangesLogCounter.
    * 
    * @param needRemoveOldChangesLogCounter
    *          set the removedOldChangesLogCounter
    */
   public void setRemovedOldChangesLogCounter(long needRemoveOldChangesLogCounter)
   {
      this.removedOldChangesLogCounter = needRemoveOldChangesLogCounter;
   }

   /**
    * isAllOldChangesLogsRemoved.
    * 
    * @return boolean return 'true' if all old ChangesLogs was removed
    */
   public boolean isAllOldChangesLogsRemoved()
   {
      return (needTransferCounter == removedOldChangesLogCounter ? true : false);
   }

   /**
    * getSuccessfulTransferCounter.
    * 
    * @return long return the successfulTransferCounter
    */
   public long getSuccessfulTransferCounter()
   {
      return successfulTransferCounter;
   }

   /**
    * setSuccessfulTransferCounter.
    * 
    * @param successfulTransferCounter
    *          set the successfulTransferCounter
    */
   public void setSuccessfulTransferCounter(long successfulTransferCounter)
   {
      this.successfulTransferCounter = successfulTransferCounter;
   }

   /**
    * isSuccessfulTransfer.
    * 
    * @return boolean return 'true' if is successful transfer
    */
   public boolean isSuccessfulTransfer()
   {
      return isSuccessfulTransfer;
   }

   /**
    * addToSuccessfulTransferCounter.
    * 
    * @param count
    *          add the 'count' to successfulTransferCounter
    */
   public void addToSuccessfulTransferCounter(long count)
   {
      successfulTransferCounter += count;

      isSuccessfulTransfer = (needTransferCounter == successfulTransferCounter ? true : false);
   }

   /**
    * isSuccessfulSave.
    * 
    * @return boolean return the 'true' if successful save
    */
   public boolean isSuccessfulSave()
   {
      return isSuccessfulSave;
   }

   /**
    * setSuccessfulSave.
    * 
    * @param successfulSave
    *          set the isSuccessfulSave
    */
   public void setSuccessfulSave(boolean successfulSave)
   {
      this.isSuccessfulSave = successfulSave;
   }
}
