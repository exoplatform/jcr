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
package org.exoplatform.services.jcr.impl.backup;

import org.exoplatform.commons.utils.PrivilegedFileHelper;
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.DataManager;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.impl.Constants;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.storage.JCRItemExistsException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.jcr.observation.ExtendedEvent;

import java.io.EOFException;
import java.io.Externalizable;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;

/**
 * Created by The eXo Platform SAS.
 *
 * Date: 24 01 2011
 * 
 * Class contains commons method for JCR restore operation.
 * - get path to full backup<br>
 * - get list of incremental backup files<br>
 * - perform incremental restore function<br>
 * 
 * @author <a href="mailto:anatoliy.bazko@exoplatform.com.ua">Anatoliy Bazko</a>
 * @version $Id: JCRRestore.java 34360 2010-11-11 11:11:11Z tolusha $
 */
public class JCRRestore
{

   private final DataManager dataManager;

   private final FileCleaner fileCleaner;

   private final File tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));

   public JCRRestore(DataManager dataManager, FileCleaner fileCleaner)
   {
      this.dataManager = dataManager;
      this.fileCleaner = fileCleaner;
   }

   /**
    * Returns file with full backup. In case of RDBMS backup 
    * it may be a directory. 
    *  
    * @param restoreDir
    * @return
    */
   public static File getFullBackupFile(File restoreDir)
   {
      Pattern p = Pattern.compile(".+\\.0");

      for (File f : PrivilegedFileHelper.listFiles(restoreDir, new FileFilter()
      {
         public boolean accept(File pathname)
         {
            Pattern p = Pattern.compile(".+\\.[0-9]+");
            Matcher m = p.matcher(pathname.getName());
            return m.matches();
         }
      }))
      {
         Matcher m = p.matcher(f.getName());
         if (m.matches())
         {
            return f;
         }
      }

      return null;
   }

   /**
    * Get list of incremental backup files.
    *  
    * @param restoreDir
    * @return list of files
    */
   public static List<File> getIncrementalFiles(File restoreDir)
   {
      ArrayList<File> list = new ArrayList<File>();

      Pattern fullBackupPattern = Pattern.compile(".+\\.0");

      for (File f : PrivilegedFileHelper.listFiles(restoreDir, new FileFilter()
      {
         public boolean accept(File pathname)
         {
            Pattern p = Pattern.compile(".+\\.[0-9]+");
            Matcher m = p.matcher(pathname.getName());
            return m.matches();
         }
      }))
      {
         if (fullBackupPattern.matcher(f.getName()).matches() == false)
         {
            list.add(f);
         }
      }

      return list;
   }

   /**
    * Perform incremental restore operation.
    * 
    * @param incrementalBackupFile
    *          incremental backup file
    * @throws FileNotFoundException
    * @throws IOException
    * @throws ClassNotFoundException
    * @throws RepositoryException
    */
   public void incrementalRestore(File incrementalBackupFile) throws FileNotFoundException, IOException,
      ClassNotFoundException, RepositoryException
   {
      ObjectInputStream ois = null;
      try
      {
         ois = new ObjectInputStream(PrivilegedFileHelper.fileInputStream(incrementalBackupFile));

         while (true)
         {
            TransactionChangesLog changesLog = readExternal(ois);
            changesLog.setSystemId(Constants.JCR_CORE_RESTORE_WORKSPACE_INITIALIZER_SYSTEM_ID); // mark changes

            ChangesLogIterator cli = changesLog.getLogIterator();
            while (cli.hasNextLog())
            {
               if (cli.nextLog().getEventType() == ExtendedEvent.LOCK)
                  cli.removeLog();
            }

            saveChangesLog(changesLog);
         }
      }
      catch (EOFException ioe)
      {
         // ok - reading all data from backup file;
      }
   }

   private void saveChangesLog(TransactionChangesLog changesLog) throws RepositoryException
   {
      try
      {
         dataManager.save(changesLog);
      }
      catch (JCRInvalidItemStateException e)
      {
         TransactionChangesLog normalizeChangesLog =
            getNormalizedChangesLog(e.getIdentifier(), e.getState(), changesLog);
         if (normalizeChangesLog != null)
            saveChangesLog(normalizeChangesLog);
         else
            throw new RepositoryException(
               "Collisions found during save of restore changes log, but caused item is not found by ID "
                  + e.getIdentifier() + ". " + e, e);
      }
      catch (JCRItemExistsException e)
      {
         TransactionChangesLog normalizeChangesLog =
            getNormalizedChangesLog(e.getIdentifier(), e.getState(), changesLog);
         if (normalizeChangesLog != null)
            saveChangesLog(normalizeChangesLog);
         else
            throw new RepositoryException(
               "Collisions found during save of restore changes log, but caused item is not found by ID "
                  + e.getIdentifier() + ". " + e, e);
      }
   }

   private TransactionChangesLog getNormalizedChangesLog(String collisionID, int state, TransactionChangesLog changesLog)
   {
      ItemState citem = changesLog.getItemState(collisionID);

      if (citem != null)
      {

         TransactionChangesLog result = new TransactionChangesLog();
         result.setSystemId(changesLog.getSystemId());

         ChangesLogIterator cli = changesLog.getLogIterator();
         while (cli.hasNextLog())
         {
            ArrayList<ItemState> normalized = new ArrayList<ItemState>();
            PlainChangesLog next = cli.nextLog();
            for (ItemState change : next.getAllStates())
            {
               if (state == change.getState())
               {
                  ItemData item = change.getData();
                  // targeted state
                  if (citem.isNode())
                  {
                     // Node... by ID and desc path
                     if (!item.getIdentifier().equals(collisionID)
                        && !item.getQPath().isDescendantOf(citem.getData().getQPath()))
                        normalized.add(change);
                  }
                  else if (!item.getIdentifier().equals(collisionID))
                  {
                     // Property... by ID
                     normalized.add(change);
                  }
               }
               else
                  // another state
                  normalized.add(change);
            }

            PlainChangesLog plog = new PlainChangesLogImpl(normalized, next.getSessionId(), next.getEventType());
            result.addLog(plog);
         }

         return result;
      }

      return null;
   }

   private TransactionChangesLog readExternal(ObjectInputStream in) throws IOException, ClassNotFoundException
   {
      int changesLogType = in.readInt();

      TransactionChangesLog transactionChangesLog = null;

      if (changesLogType == RestoreChangesLog.Type.ItemDataChangesLog_with_Streams)
      {

         // read ChangesLog
         transactionChangesLog = (TransactionChangesLog)in.readObject();

         // read FixupStream count
         int iFixupStream = in.readInt();

         ArrayList<FixupStream> listFixupStreams = new ArrayList<FixupStream>();

         for (int i = 0; i < iFixupStream; i++)
         {
            FixupStream fs = new FixupStream();
            fs.readExternal(in);
            listFixupStreams.add(fs);
         }

         // read stream data
         int iStreamCount = in.readInt();
         ArrayList<SpoolFile> listFiles = new ArrayList<SpoolFile>();

         for (int i = 0; i < iStreamCount; i++)
         {

            // read file size
            long fileSize = in.readLong();

            // read content file
            listFiles.add(getAsFile(in, fileSize));
         }

         RestoreChangesLog restoreChangesLog =
            new RestoreChangesLog(transactionChangesLog, listFixupStreams, listFiles, fileCleaner);

         restoreChangesLog.restore();

      }
      else if (changesLogType == RestoreChangesLog.Type.ItemDataChangesLog_without_Streams)
      {
         transactionChangesLog = (TransactionChangesLog)in.readObject();
      }

      return transactionChangesLog;
   }

   private SpoolFile getAsFile(ObjectInputStream ois, long fileSize) throws IOException
   {
      int bufferSize = 1024 * 8;
      byte[] buf = new byte[bufferSize];

      SpoolFile tempFile = SpoolFile.createTempFile("vdincb" + System.currentTimeMillis(), ".stmp", tempDir);
      FileOutputStream fos = PrivilegedFileHelper.fileOutputStream(tempFile);
      long readBytes = fileSize;

      while (readBytes > 0)
      {
         // long longTemp = readByte - bufferSize;
         if (readBytes >= bufferSize)
         {
            ois.readFully(buf);
            fos.write(buf);
         }
         else if (readBytes < bufferSize)
         {
            ois.readFully(buf, 0, (int)readBytes);
            fos.write(buf, 0, (int)readBytes);
         }
         readBytes -= bufferSize;
      }

      fos.flush();
      fos.close();

      return tempFile;
   }

   class RestoreChangesLog
   {
      public class Type
      {
         public static final int ItemDataChangesLog_without_Streams = 1;

         public static final int ItemDataChangesLog_with_Streams = 2;
      }

      private TransactionChangesLog itemDataChangesLog;

      private List<FixupStream> listFixupStream;

      private List<SpoolFile> listFile;

      private FileCleaner fileCleaner;

      public RestoreChangesLog(TransactionChangesLog transactionChangesLog, List<FixupStream> listFixupStreams,
         List<SpoolFile> listFiles, FileCleaner fileCleaner)
      {
         this.itemDataChangesLog = transactionChangesLog;
         this.listFixupStream = listFixupStreams;
         this.listFile = listFiles;
         this.fileCleaner = fileCleaner;
      }

      public TransactionChangesLog getItemDataChangesLog()
      {
         return itemDataChangesLog;
      }

      public void restore() throws IOException
      {
         List<ItemState> listItemState = itemDataChangesLog.getAllStates();
         for (int i = 0; i < this.listFixupStream.size(); i++)
         {
            ItemState itemState = listItemState.get(listFixupStream.get(i).getItemSateId());
            ItemData itemData = itemState.getData();

            PersistedPropertyData propertyData = (PersistedPropertyData)itemData;
            ValueData vd = (propertyData.getValues().get(listFixupStream.get(i).getValueDataId()));

            // re-init the value
            propertyData.getValues().set(listFixupStream.get(i).getValueDataId(),
               new StreamPersistedValueData(vd.getOrderNumber(), listFile.get(i)));
         }

         for (int i = 0; i < listFile.size(); i++)
            fileCleaner.addFile(listFile.get(i));
      }
   }

   class FixupStream implements Externalizable
   {
      int iItemStateId = -1;

      int iValueDataId = -1;

      public FixupStream()
      {
      }

      public FixupStream(int itemState_, int valueData_)
      {
         iItemStateId = itemState_;
         iValueDataId = valueData_;
      }

      public int getItemSateId()
      {
         return iItemStateId;
      }

      public int getValueDataId()
      {
         return iValueDataId;
      }

      public boolean compare(FixupStream fs)
      {
         boolean b = true;
         if (fs.getItemSateId() != this.getItemSateId())
            b = false;
         if (fs.getValueDataId() != this.getValueDataId())
            b = false;
         return b;
      }

      public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
      {
         iItemStateId = in.readInt();
         iValueDataId = in.readInt();
      }

      public void writeExternal(ObjectOutput out) throws IOException
      {
         out.writeInt(iItemStateId);
         out.writeInt(iValueDataId);
      }
   }
}
