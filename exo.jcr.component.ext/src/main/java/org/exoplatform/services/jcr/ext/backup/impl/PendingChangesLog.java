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
import org.exoplatform.commons.utils.PrivilegedSystemHelper;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.dataflow.persistent.PersistedPropertyData;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.datamodel.ValueData;
import org.exoplatform.services.jcr.ext.replication.FixupStream;
import org.exoplatform.services.jcr.impl.dataflow.persistent.StreamPersistedValueData;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.impl.util.io.SpoolFile;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: PendingChangesLog.java 31768 2009-05-14 09:35:43Z pnedonosko $
 */

public class PendingChangesLog
{

   /**
    * The apache logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.PendingChangesLog");

   /**
    * The definition of ChangesLog types.
    */
   public final class Type
   {
      /**
       * CHANGESLOG_WITHOUT_STREAM. The the type fore ChangesLog without streams.
       */
      public static final int CHANGESLOG_WITHOUT_STREAM = 1;

      /**
       * CHANGESLOG_WITH_STREAM. The the type fore ChangesLog with streams.
       */
      public static final int CHANGESLOG_WITH_STREAM = 2;

      /**
       * Empty constructor.
       */
      private Type()
      {
      }
   }

   /**
    * Minimal sleep timeout.
    */
   private static final int SLEEP_TIME = 5;

   /**
    * ChangesLog with data.
    */
   private TransactionChangesLog itemDataChangesLog;

   /**
    * The list of input streams who are contains in ChangesLog.
    */
   private List<InputStream> listInputStream;

   /**
    * The list of RandomAccessFiles who ate contains in ChangesLog.
    */
   private List<RandomAccessFile> listRandomAccessFile;

   /**
    * Type of ChangesLog (CHANGESLOG_WITHOUT_STREAM or CHANGESLOG_WITH_STREAM).
    */
   private int containerType;

   /**
    * The list of FixupStreams who are indicate the location the input streams in ChangesLog.
    */
   private List<FixupStream> listFixupStream;

   // private HashMap<FixupStream, RandomAccessFile> mapFixupStream;

   /**
    * The list of Files who are contains in ChangesLog.
    */
   private List<SpoolFile> listFile;

   /**
    * The identification string for PendingChangesLog.
    */
   private String identifier;

   /**
    * The FileCleaner will delete the temporary files.
    */
   private FileCleaner fileCleaner;

   /**
    * The arrays of bytes for serialized ChangesLog without streams.
    */
   private byte[] data;

   /**
    * Temporary directory;
    */
   private final File tempDir;

   /**
    * PendingChangesLog constructor.
    * 
    * @param itemDataChangesLog
    *          ChangesLog with data
    * @param fileCleaner
    *          the FileCleaner
    * @throws IOException
    *           will be generated the IOExaption
    */
   public PendingChangesLog(TransactionChangesLog itemDataChangesLog, FileCleaner fileCleaner) throws IOException
   {
      this.itemDataChangesLog = itemDataChangesLog;
      listInputStream = new ArrayList<InputStream>();
      listFixupStream = new ArrayList<FixupStream>();
      containerType = analysisItemDataChangesLog();
      listFile = new ArrayList<SpoolFile>();
      identifier = IdGenerator.generate();
      this.fileCleaner = fileCleaner;
      this.tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));
   }

   /**
    * PendingChangesLog constructor.
    * 
    * @param itemDataChangesLog
    *          ChangesLog with data
    * @param identifier
    *          identifier to this PendingChangesLog.
    * @param type
    *          type of PendingChangesLog
    * @param fileCleaner
    *          the FileCleaner
    * @throws IOException
    *           will be generated the IOExaption
    */
   public PendingChangesLog(TransactionChangesLog itemDataChangesLog, String identifier, int type,
      FileCleaner fileCleaner) throws IOException
   {
      this.itemDataChangesLog = itemDataChangesLog;
      listInputStream = new ArrayList<InputStream>();
      listFixupStream = new ArrayList<FixupStream>();
      listRandomAccessFile = new ArrayList<RandomAccessFile>();
      listFile = new ArrayList<SpoolFile>();
      this.identifier = identifier;
      containerType = type;
      this.fileCleaner = fileCleaner;
      this.tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));
   }

   /**
    * PendingChangesLog constructor.
    * 
    * @param identifier
    *          identifier to this PendingChangesLog.
    * @param dataLength
    *          the length of binary data
    */
   public PendingChangesLog(String identifier, int dataLength)
   {
      this.identifier = identifier;
      data = new byte[dataLength];
      this.tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));
   }

   /**
    * PendingChangesLog constructor.
    * 
    * @param transactionChangesLog
    *          ChangesLog with data
    * @param listFixupStreams
    *          list of FixupStreams
    * @param listFiles
    *          list of Files
    * @param fileCleaner
    *          the FileCleaner
    */
   public PendingChangesLog(TransactionChangesLog transactionChangesLog, List<FixupStream> listFixupStreams,
      List<SpoolFile> listFiles, FileCleaner fileCleaner)
   {
      this.itemDataChangesLog = transactionChangesLog;
      this.listFixupStream = listFixupStreams;
      this.listFile = listFiles;
      this.fileCleaner = fileCleaner;
      this.tempDir = new File(PrivilegedSystemHelper.getProperty("java.io.tmpdir"));
   }

   /**
    * putData.
    * 
    * @param offset
    *          offset in 'data'
    * @param tempData
    *          piece of binary data
    */
   public void putData(int offset, byte[] tempData)
   {
      for (int i = 0; i < tempData.length; i++)
         data[i + offset] = tempData[i];
   }

   /**
    * getData.
    * 
    * @return byte[] return the binary data
    */
   public byte[] getData()
   {
      return data;
   }

   /**
    * getItemDataChangesLog.
    * 
    * @return TransactionChangesLog return the ChangesLog
    */
   public TransactionChangesLog getItemDataChangesLog()
   {
      return itemDataChangesLog;
   }

   /**
    * getInputStreams.
    * 
    * @return List return the list of input streams
    */
   public List<InputStream> getInputStreams()
   {
      return listInputStream;
   }

   /**
    * getListRandomAccessFiles.
    * 
    * @return List return the list of RandomAccessFiles
    */
   public List<RandomAccessFile> getListRandomAccessFiles()
   {
      return listRandomAccessFile;
   }

   /**
    * getListFile.
    * 
    * @return List return list of Files
    */
   public List<SpoolFile> getListFile()
   {
      return listFile;
   }

   /**
    * getFixupStreams.
    * 
    * @return List return list of FixupStreams
    */
   public List<FixupStream> getFixupStreams()
   {
      return listFixupStream;
   }

   /**
    * analysisItemDataChangesLog.
    * 
    * @return int type of ChangesLog (CHANGESLOG_WITHOUT_STREAM or CHANGESLOG_WITH_STREAM)
    * @throws IOException
    *           will be generated the IOException
    */
   private int analysisItemDataChangesLog() throws IOException
   {
      int itemDataChangesLogType = PendingChangesLog.Type.CHANGESLOG_WITHOUT_STREAM;

      int i = 0;
      for (ItemState itemState : itemDataChangesLog.getAllStates())
      {
         ItemData itemData = itemState.getData();

         if (itemData instanceof PersistedPropertyData)
         {
            PersistedPropertyData propertyData = (PersistedPropertyData)itemData;
            if ((propertyData.getValues() != null))
               for (int j = 0; j < propertyData.getValues().size(); j++)
                  if (!(propertyData.getValues().get(j).isByteArray()))
                  {
                     listFixupStream.add(new FixupStream(i, j));

                     InputStream inputStream;
                     if (itemState.isDeleted())
                        inputStream = new ByteArrayInputStream("".getBytes());
                     else
                        inputStream = propertyData.getValues().get(j).getAsStream();

                     listInputStream.add(inputStream);
                     itemDataChangesLogType = PendingChangesLog.Type.CHANGESLOG_WITH_STREAM;
                  }
         }

         i++;
      }

      return itemDataChangesLogType;
   }

   /**
    * getConteinerType.
    * 
    * @return int return the type of ChangesLog
    */
   public int getConteinerType()
   {
      return containerType;
   }

   /**
    * getIdentifier.
    * 
    * @return String return the identifier string
    */
   public String getIdentifier()
   {
      return identifier;
   }

   /**
    * getAsByteArray. Make the array of bytes from ChangesLog.
    * 
    * @param dataChangesLog
    *          the ChangesLog with data
    * @return byte[] return the serialized ChangesLog
    * @throws IOException
    *           will be generated the IOException
    */
   public static byte[] getAsByteArray(TransactionChangesLog dataChangesLog) throws IOException
   {
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(os);
      oos.writeObject(dataChangesLog);

      byte[] bArray = os.toByteArray();
      return bArray;
   }

   /**
    * getAsItemDataChangesLog. Make the ChangesLog from array of bytes.
    * 
    * @param byteArray
    *          the serialized ChangesLog
    * @return TransactionChangesLog return the deserialized ChangesLog
    * @throws IOException
    *           will be generated the IOException
    * @throws ClassNotFoundException
    *           will be generated the ClassNotFoundException
    */
   public static TransactionChangesLog getAsItemDataChangesLog(byte[] byteArray) throws IOException,
      ClassNotFoundException
   {
      ByteArrayInputStream is = new ByteArrayInputStream(byteArray);
      ObjectInputStream ois = new ObjectInputStream(is);
      TransactionChangesLog objRead = (TransactionChangesLog)ois.readObject();

      return objRead;
   }

   /**
    * getRandomAccessFile.
    * 
    * @param fs
    *          the FixupStream
    * @return RandomAccessFile return the RandomAccessFile by FixupStream
    * @throws IOException
    *           will be generated the IOException
    */
   public RandomAccessFile getRandomAccessFile(FixupStream fs) throws IOException
   {
      int i = 0;
      try
      {
         for (i = 0; i < listFixupStream.size(); i++)
            if (this.listFixupStream.get(i).compare(fs))
               return listRandomAccessFile.get(i);
      }
      catch (IndexOutOfBoundsException e)
      {
         try
         {
            Thread.sleep(SLEEP_TIME);
            return listRandomAccessFile.get(i);
         }
         catch (InterruptedException ie)
         {
            LOG.error("The interrupted exceptio : ", ie);
         }
         catch (IndexOutOfBoundsException ioobe)
         {
            if (LOG.isDebugEnabled())
            {
               LOG.info("listFixupStream.size() == " + listFixupStream.size());
               LOG.info("listRandomAccessFile.size() == " + listRandomAccessFile.size());
               LOG.info(" i == " + i);
            }
            synchronized (this)
            {
               if (listFile.size() > i)
               {
                  listFile.remove(i);
               }
               listFixupStream.remove(i);

               addNewStream(fs);

               getRandomAccessFile(fs);
            }
         }
      }
      return null;
   }

   /**
    * addNewStream.
    * 
    * @param fs
    *          the FixupStream
    * @throws IOException
    *           will be generated the IOException
    */
   public void addNewStream(FixupStream fs) throws IOException
   {
      this.getFixupStreams().add(fs);

      SpoolFile f = SpoolFile.createTempFile("tempFile" + IdGenerator.generate(), ".tmp", tempDir);

      this.getListFile().add(f);
      this.getListRandomAccessFiles().add(PrivilegedFileHelper.randomAccessFile(f, "rw"));

   }

   /**
    * Restore ChangesLog(set the InputStreams to ValueData).
    * 
    * @throws IOException
    *           will be generated the IOException
    */
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

      if (listRandomAccessFile != null)
         for (int i = 0; i < listRandomAccessFile.size(); i++)
            listRandomAccessFile.get(i).close();

      for (int i = 0; i < listFile.size(); i++)
         fileCleaner.addFile(listFile.get(i));
   }

}