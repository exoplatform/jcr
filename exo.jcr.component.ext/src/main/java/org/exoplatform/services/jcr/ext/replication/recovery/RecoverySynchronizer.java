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
package org.exoplatform.services.jcr.ext.replication.recovery;

import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.dataflow.ItemState;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLogImpl;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.datamodel.ItemData;
import org.exoplatform.services.jcr.ext.replication.AbstractWorkspaceDataReceiver;
import org.exoplatform.services.jcr.ext.replication.ChangesFile;
import org.exoplatform.services.jcr.ext.replication.Packet;
import org.exoplatform.services.jcr.ext.replication.PendingBinaryFile;
import org.exoplatform.services.jcr.ext.replication.ReplicationChannelManager;
import org.exoplatform.services.jcr.ext.replication.ReplicationException;
import org.exoplatform.services.jcr.impl.storage.JCRInvalidItemStateException;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RecoverySynchronizer.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public class RecoverySynchronizer
{
   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("ext.RecoverySynchronizer");

   /**
    * Definition the folder to ChangesLog.
    */
   // private File recoveryDir;
   /**
    * The FileNameFactory will be generated name of file to binary ChangesLog.
    */
   private FileNameFactory fileNameFactory;

   /**
    * The FileCleaner will delete the temporary files.
    */
   private FileCleaner fileCleaner;

   /**
    * The ReplicationChannelManager will be transmitted or receive the Packets.
    */
   private ReplicationChannelManager channelManager;

   /**
    * The own name in cluster.
    */
   private String ownName;

   /**
    * The system identification string.
    */
   private String systemId;

   /**
    * The RecoveryReader will be read the binary ChangesLog on file system.
    */
   private RecoveryReader recoveryReader;

   /**
    * The RecoveryReader will be wrote the ChangesLog to file system.
    */
   private RecoveryWriter recoveryWriter;

   /**
    * The HashMap with PendingBinaryFile.
    */
   private HashMap<String, PendingBinaryFile> mapPendingBinaryFile;

   /**
    * The ChangesLogs will be saved on ItemDataKeeper.
    */
   private ItemDataKeeper dataKeeper;

   /**
    * The list of names other participants who was initialized.
    */
   private List<String> initedParticipantsClusterList;

   /**
    * The list of names other participants who was Synchronized successful.
    */
   private List<String> successfulSynchronizedList;

   /**
    * The flag for local synchronization.
    */
   private volatile boolean localSynchronization = false;

   /**
    * RecoverySynchronizer constructor.
    * 
    * @param recoveryDir
    *          the recovery dir
    * @param fileNameFactory
    *          the FileNameFactory
    * @param fileCleaner
    *          the FileCleaner
    * @param channelManager
    *          the ReplicationChannelManager
    * @param ownName
    *          the own name
    * @param recoveryWriter
    *          the RecoveryWriter
    * @param recoveryReader
    *          the RecoveryReader
    * @param systemId
    *          the system identification string
    */
   public RecoverySynchronizer(File recoveryDir, FileNameFactory fileNameFactory, FileCleaner fileCleaner,
      ReplicationChannelManager channelManager, String ownName, RecoveryWriter recoveryWriter,
      RecoveryReader recoveryReader, String systemId)
   {
      // this.recoveryDir = recoveryDir;
      this.fileNameFactory = fileNameFactory;
      this.fileCleaner = fileCleaner;
      this.channelManager = channelManager;
      this.ownName = ownName;
      this.systemId = systemId;

      this.recoveryReader = recoveryReader;

      this.recoveryWriter = recoveryWriter;
      mapPendingBinaryFile = new HashMap<String, PendingBinaryFile>();

      successfulSynchronizedList = new ArrayList<String>();
      initedParticipantsClusterList = new ArrayList<String>();
   }

   /**
    * Will be initialized the synchronization.
    */
   public void synchronizRepository()
   {
      try
      {
         if (localSynchronization)
         {
            log.info("Synchronization init...");
            Packet packet =
               new Packet(Packet.PacketType.GET_CHANGESLOG_UP_TO_DATE, IdGenerator.generate(), ownName, Calendar
                  .getInstance());
            channelManager.sendPacket(packet);
         }
      }
      catch (Exception e)
      {
         log.error("Synchronization error", e);
      }

   }

   /**
    * send.
    * 
    * @param packet
    *          the Packet
    * @throws Exception
    *           will be generated the Exception
    */
   private void send(Packet packet) throws Exception
   {
      byte[] buffer = Packet.getAsByteArray(packet);

      if (buffer.length <= Packet.MAX_PACKET_SIZE)
      {
         channelManager.send(buffer);
      }
      else
         channelManager.sendBigPacket(buffer, packet);
   }

   /**
    * processingPacket.
    * 
    * @param packet
    *          the Packet
    * @param status
    *          before status
    * @return int 
    *           after status
    * @throws Exception
    *           will be generated the Exception
    */
   public int processingPacket(Packet packet, int status) throws Exception
   {

      int stat = status;

      switch (packet.getPacketType())
      {

         case Packet.PacketType.GET_CHANGESLOG_UP_TO_DATE :
            sendChangesLogUpDate(packet.getTimeStamp(), packet.getOwnerName(), packet.getIdentifier());
            break;

         case Packet.PacketType.BINARY_FILE_PACKET :

            PendingBinaryFile container = mapPendingBinaryFile.get(packet.getIdentifier());
            if (container == null)
            {
               container = new PendingBinaryFile();
               mapPendingBinaryFile.put(packet.getIdentifier(), container);
            }

            ChangesFile chf;
            synchronized (container)
            {
               chf = container.getChangesFile(packet.getOwnerName(), packet.getFileName());
               if (chf == null)
               {
                  chf =
                     container.addChangesFile(packet.getOwnerName(), packet.getFileName(), packet.getSystemId(), packet
                        .getTotalPacketCount());
               }
            }

            chf.write(packet.getOffset(), packet.getByteArray());

            if (chf.isStored())
            {
               if (log.isDebugEnabled())
                  log.debug("Last packet of file has been received : " + packet.getFileName());

            }

            break;

         case Packet.PacketType.ALL_BINARY_FILE_TRANSFERRED_OK :
            if (mapPendingBinaryFile.containsKey(packet.getIdentifier()))
            {
               PendingBinaryFile pbf = mapPendingBinaryFile.get(packet.getIdentifier());
               pbf.addToSuccessfulTransferCounter(packet.getSize());

               if (pbf.isSuccessfulTransfer())
               {
                  if (log.isDebugEnabled())
                     log.debug("The signal ALL_BinaryFile_transferred_OK has been received  from "
                        + packet.getOwnerName());

                  List<ChangesFile> fileDescriptorList = pbf.getSortedFilesDescriptorList();

                  if (log.isDebugEnabled())
                     log.info("fileDescriptorList.size() == pbf.getNeedTransferCounter() : "
                        + fileDescriptorList.size() + "== " + pbf.getNeedTransferCounter());

                  if (fileDescriptorList.size() == pbf.getNeedTransferCounter())
                  {
                     List<String> failList = new ArrayList<String>();

                     for (ChangesFile fileDescriptor : fileDescriptorList)
                     {
                        try
                        {
                           TransactionChangesLog transactionChangesLog =
                              recoveryReader.getChangesLog(fileDescriptor.getFile().getAbsolutePath());

                           transactionChangesLog.setSystemId(fileDescriptor.getSystemId());

                           Calendar cLogTime = fileNameFactory.getDateFromFileName(fileDescriptor.getFile().getName());

                           if (log.isDebugEnabled())
                           {
                              log.debug("Save to JCR : " + fileDescriptor.getFile().getAbsolutePath());
                              log.debug("SystemID : " + transactionChangesLog.getSystemId());
                              log.debug("list size : " + fileDescriptorList.size());
                           }

                           // dump log
                           if (log.isDebugEnabled())
                           {
                              ChangesLogIterator logIterator = transactionChangesLog.getLogIterator();
                              while (logIterator.hasNextLog())
                              {
                                 PlainChangesLog pcl = logIterator.nextLog();
                                 log.debug(pcl.dump());
                              }
                           }

                           saveChangesLog(dataKeeper, transactionChangesLog, cLogTime);

                           if (log.isDebugEnabled())
                           {
                              log.debug("After save message: the owner systemId --> "
                                 + transactionChangesLog.getSystemId());
                              log.debug("After save message: --> " + systemId);
                           }

                        }
                        catch (Exception e)
                        {
                           failList.add(fileDescriptor.getFile().getName());
                           log.error("Can't save to JCR ", e);
                        }
                     }

                     // Send file name list
                     List<String> fileNameList =
                        new ArrayList<String>(mapPendingBinaryFile.get(packet.getIdentifier()).getFileNameList());
                     if (failList.size() != 0)
                        fileNameList.removeAll(failList);

                     Packet packetFileNameList =
                        new Packet(Packet.PacketType.ALL_CHANGESLOG_SAVED_OK, packet.getIdentifier(), ownName,
                           fileNameList);
                     send(packetFileNameList);

                     log.info("The " + fileDescriptorList.size() + " changeslogs were received and "
                        + fileNameList.size() + " saved");

                  }
                  else if (log.isDebugEnabled())
                  {
                     log.debug("Do not start save : " + fileDescriptorList.size() + " of "
                        + pbf.getNeedTransferCounter());
                  }
               }
            }
            break;

         case Packet.PacketType.ALL_CHANGESLOG_SAVED_OK :
            long removeCounter = recoveryWriter.removeChangesLog(packet.getFileNameList(), packet.getOwnerName());

            if (log.isDebugEnabled())
               log.debug("Remove from file system : " + removeCounter);

            Packet removedOldChangesLogPacket =
               new Packet(Packet.PacketType.REMOVED_OLD_CHANGESLOG_COUNTER, packet.getIdentifier(), ownName);
            removedOldChangesLogPacket.setSize(removeCounter);
            channelManager.sendPacket(removedOldChangesLogPacket);

            break;

         case Packet.PacketType.REMOVED_OLD_CHANGESLOG_COUNTER :
            if (mapPendingBinaryFile.containsKey(packet.getIdentifier()) == true)
            {
               PendingBinaryFile pbf = mapPendingBinaryFile.get(packet.getIdentifier());
               pbf.setRemovedOldChangesLogCounter(pbf.getRemovedOldChangesLogCounter() + packet.getSize());

               if (pbf.isAllOldChangesLogsRemoved())
               {

                  // remove temporary files
                  for (ChangesFile fd : pbf.getSortedFilesDescriptorList())
                     fileCleaner.addFile(fd.getFile());

                  // remove PendingBinaryFile
                  mapPendingBinaryFile.remove(packet.getIdentifier());

                  // next iteration
                  if (log.isDebugEnabled())
                     log.debug("Next iteration of recovery ...");

                  synchronizRepository();
               }
            }
            else
               log.warn("Can not find the PendingBinaryFile whith id: " + packet.getIdentifier());
            break;

         case Packet.PacketType.NEED_TRANSFER_COUNTER :
            if (mapPendingBinaryFile.containsKey(packet.getIdentifier()) == false)
               mapPendingBinaryFile.put(packet.getIdentifier(), new PendingBinaryFile());

            PendingBinaryFile pbf = mapPendingBinaryFile.get(packet.getIdentifier());
            pbf.setNeedTransferCounter(pbf.getNeedTransferCounter() + packet.getSize());

            if (log.isDebugEnabled())
               log.debug("NeedTransferCounter : " + pbf.getNeedTransferCounter());
            break;

         case Packet.PacketType.SYNCHRONIZED_OK :
            if (successfulSynchronizedList.contains(packet.getOwnerName()) == false)
               successfulSynchronizedList.add(packet.getOwnerName());

            if (successfulSynchronizedList.size() == initedParticipantsClusterList.size())
            {
               stat = AbstractWorkspaceDataReceiver.NORMAL_MODE;

               localSynchronization = false;
            }
            break;
         default :
            break;
      }

      return stat;
   }

   /**
    * sendChangesLogUpDate.
    * 
    * @param timeStamp
    *          the update to this date
    * @param ownerName
    *          the member name who initialize synchronization
    * @param identifier
    *          the operation identifier
    */
   private void sendChangesLogUpDate(Calendar timeStamp, String ownerName, String identifier)
   {
      try
      {
         if (log.isDebugEnabled())
            log.debug("+++ sendChangesLogUpDate() +++ : " + Calendar.getInstance().getTime().toGMTString());

         List<String> filePathList = recoveryReader.getFilePathList(timeStamp, ownerName);

         Packet needTransferCounter = new Packet(Packet.PacketType.NEED_TRANSFER_COUNTER, identifier, ownName);
         needTransferCounter.setSize(filePathList.size());
         channelManager.sendPacket(needTransferCounter);

         if (filePathList.size() > 0)
         {
            for (String filePath : filePathList)
            {
               channelManager.sendBinaryFile(filePath, ownerName, identifier, systemId,
                  Packet.PacketType.BINARY_FILE_PACKET);
            }

            Packet endPocket = new Packet(Packet.PacketType.ALL_BINARY_FILE_TRANSFERRED_OK, identifier);
            endPocket.setOwnName(ownerName);
            endPocket.setSize(filePathList.size());
            channelManager.sendPacket(endPocket);

         }
         else
         {
            Packet synchronizedOKPacket =
               new Packet(Packet.PacketType.SYNCHRONIZED_OK, IdGenerator.generate(), ownerName);
            channelManager.sendPacket(synchronizedOKPacket);
         }

      }
      catch (Exception e)
      {
         log.error("ChangesLogs was send with error", e);
      }
   }

   /**
    * setDataKeeper.
    * 
    * @param dataKeeper
    *          the ItemDataKeeper
    */
   public void setDataKeeper(ItemDataKeeper dataKeeper)
   {
      this.dataKeeper = dataKeeper;
   }

   /**
    * updateInitedParticipantsClusterList.
    * 
    * @param list
    *          the list of initialized members
    */
   public void updateInitedParticipantsClusterList(Collection<? extends String> list)
   {
      initedParticipantsClusterList = new ArrayList<String>(list);
   }

   /**
    * localSynchronization.
    * 
    */
   public void localSynchronization()
   {
      localSynchronization = true;
   }

   /**
    * saveChangesLog.
    * 
    * @param dataManager
    *          the ItemDataKeeper
    * @param changesLog
    *          the ChangesLog with data
    * @param cLogTime
    *          the date of ChangesLog
    * @throws ReplicationException
    *           will be generated the ReplicationException
    */
   private void saveChangesLog(ItemDataKeeper dataManager, TransactionChangesLog changesLog, Calendar cLogTime)
      throws ReplicationException
   {
      try
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
               saveChangesLog(dataManager, normalizeChangesLog, cLogTime);
         }
      }
      catch (Throwable t)
      {
         throw new ReplicationException("Save error. Log time " + cLogTime.getTime(), t);
      }
   }

   /**
    * getNormalizedChangesLog.
    *
    * @param collisionID
    *          String, id of collision
    * @param state
    *          int, the state
    * @param changesLog
    *          TransactionChangesLog, the changes log
    * @return TransactionChangesLog
    *           return the normalized changes log
    */
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
}

/**
 * Counter.
 *
 */
class Counter
{

   /**
    * The count value.
    */
   int count = 0;

   /**
    * inc.
    *
    * @return int
    *           return the value of count
    */
   public int inc()
   {
      return ++count;
   }

   /**
    * clear.
    *
    */
   public void clear()
   {
      count = 0;
   }

   /**
    * getValue.
    *
    * @return int
    *           return the value of count 
    */
   public int getValue()
   {
      return count;
   }
}
