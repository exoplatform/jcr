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

import org.exoplatform.services.jcr.config.RepositoryConfigurationException;
import org.exoplatform.services.jcr.dataflow.ChangesLogIterator;
import org.exoplatform.services.jcr.dataflow.ItemDataKeeper;
import org.exoplatform.services.jcr.dataflow.ItemStateChangesLog;
import org.exoplatform.services.jcr.dataflow.PlainChangesLog;
import org.exoplatform.services.jcr.dataflow.TransactionChangesLog;
import org.exoplatform.services.jcr.ext.replication.recovery.RecoveryManager;
import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;
import org.exoplatform.services.jcr.ext.replication.transport.MemberAddress;
import org.exoplatform.services.jcr.ext.replication.transport.PacketListener;
import org.exoplatform.services.jcr.impl.util.io.FileCleaner;
import org.exoplatform.services.jcr.util.IdGenerator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.util.HashMap;

/**
 * Created by The eXo Platform SAS.
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: AbstractWorkspaceDataReceiver.java 34445 2009-07-24 07:51:18Z dkatayev $
 */

public abstract class AbstractWorkspaceDataReceiver implements PacketListener
{

   /**
    * The apache logger.
    */
   private static Log log = ExoLogger.getLogger("ext.AbstractWorkspaceDataReceiver");

   /**
    * The definition INIT_MODE for AbstractWorkspaceDataReceiver.
    */
   public static final int INIT_MODE = -1;

   /**
    * The definition NORMAL_MODE for AbstractWorkspaceDataReceiver.
    */
   public static final int NORMAL_MODE = 0;

   /**
    * The definition RECOVERY_MODE for AbstractWorkspaceDataReceiver.
    */
   public static final int RECOVERY_MODE = 1;

   /**
    * The definition start timeout.
    */
   private static final int START_TIMEOUT = 1000;

   /**
    * The state of AbstractWorkspaceDataReceiver.
    */
   private int state;

   /**
    * System identification string.
    */
   private String systemId;

   /**
    * The ChannalManager will be transmitted the Packets.
    */
   private ChannelManager channelManager;

   /**
    * The HashMap with mapPendingBinaryFiles.
    */
   private HashMap<String, PendingBinaryFile> mapPendingBinaryFile;

   /**
    * The ChangesLogs will be saved on ItemDataKeeper.
    */
   protected ItemDataKeeper dataKeeper;

   /**
    * The FileCleaner will be deleted temporary files.
    */
   private FileCleaner fileCleaner;

   /**
    * The own name in cluster.
    */
   private String ownName;

   /**
    * The RecoveryManager will be saved ChangesLogs on FS(file system).
    */
   private RecoveryManager recoveryManager;

   /**
    * AbstractWorkspaceDataReceiver constructor.
    * 
    * @throws RepositoryConfigurationException
    *           will be generated the RepositoryConfigurationException
    */
   public AbstractWorkspaceDataReceiver() throws RepositoryConfigurationException
   {
      this.fileCleaner = new FileCleaner(ReplicationService.FILE_CLEANRE_TIMEOUT);
      mapPendingBinaryFile = new HashMap<String, PendingBinaryFile>();

      state = INIT_MODE;
   }

   /**
    * init.
    * 
    * @param channelManager
    *          the ChannelManager
    * @param systemId
    *          system identification string
    * @param ownName
    *          own name
    * @param recoveryManager
    *          the RecoveryManager
    */
   public void init(ChannelManager channelManager, String systemId, String ownName, RecoveryManager recoveryManager)
   {
      this.systemId = systemId;
      this.channelManager = channelManager;

      this.channelManager.addPacketListener(this);

      this.ownName = ownName;
      this.recoveryManager = recoveryManager;

   }

   /**
    * The call 'start()' for information other participants.
    */
   public void start()
   {
      try
      {
         Packet memberStartedPacket = new Packet(Packet.PacketType.MEMBER_STARTED, IdGenerator.generate(), ownName);
         channelManager.sendPacket(memberStartedPacket);

         Thread.sleep(START_TIMEOUT);

         Packet initedPacket = new Packet(Packet.PacketType.INITED_IN_CLUSTER, IdGenerator.generate(), ownName);
         channelManager.sendPacket(initedPacket);
      }
      catch (Exception e)
      {
         log.error("Can't initialized AbstractWorkspaceDataReceiver", e);
      }
   }

   /**
    * receive.
    * 
    * @param itemStatechangesLog
    *          the received ChangesLog
    * @param identifier
    *          the PandingChangeLog or PendingBinaryFile identifier string
    * @throws Exception
    *           will be generated the Exception
    */
   public void receive(ItemStateChangesLog itemStatechangesLog, String identifier) throws Exception
   {
      TransactionChangesLog changesLog = (TransactionChangesLog)itemStatechangesLog;
      if (changesLog.getSystemId() == null)
      {
         throw new Exception("Invalid or same systemId " + changesLog.getSystemId());
      }
      else if (!changesLog.getSystemId().equals(this.systemId))
      {

         if (state != RECOVERY_MODE)
         {
            // dump log
            if (log.isDebugEnabled())
            {
               ChangesLogIterator logIterator = changesLog.getLogIterator();
               while (logIterator.hasNextLog())
               {
                  PlainChangesLog pcl = logIterator.nextLog();
                  log.info(pcl.dump());
               }
            }

            dataKeeper.save(changesLog);

            Packet packet = new Packet(Packet.PacketType.ADD_OK, identifier, ownName);
            channelManager.sendPacket(packet);

            if (log.isDebugEnabled())
            {
               log.info("After save message: the owner systemId --> " + changesLog.getSystemId());
               log.info("After save message: --> " + systemId);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void receive(AbstractPacket p, MemberAddress sourceAddress)
   {
      try
      {
         Packet packet = (Packet)p;
         Packet bigPacket = null;

         switch (packet.getPacketType())
         {

            case Packet.PacketType.BINARY_CHANGESLOG_PACKET :

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
                        container.addChangesFile(packet.getOwnerName(), packet.getFileName(), packet.getSystemId(),
                           packet.getTotalPacketCount());
                  }
               }

               chf.write(packet.getOffset(), packet.getByteArray());

               // save to JCR
               if (chf.isStored())
               {
                  saveChangesLog(chf, packet.getIdentifier());

                  // remove
                  if (!chf.getFile().delete())
                     fileCleaner.addFile(chf.getFile());
                  mapPendingBinaryFile.remove(packet.getIdentifier());

                  if (log.isDebugEnabled())
                     log.debug("Last packet of file has been received : " + packet.getFileName());
               }
               break;
         }

         if (bigPacket != null)
         {
            state = recoveryManager.processing(bigPacket, state);
            bigPacket = null;
         }
         else
            state = recoveryManager.processing(packet, state);

      }
      catch (Exception e)
      {
         log.error("An error in processing packet : ", e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void onError(MemberAddress sourceAddress)
   {

   }

   /**
    * getDataKeeper.
    * 
    * @return ItemDataKeeper return the dataKeeper
    */
   public ItemDataKeeper getDataKeeper()
   {
      return dataKeeper;
   }

   /**
    * saveChangesLog.
    * 
    * @param fileDescriptor
    *          the FileDescriptor
    * @param identifire
    *          the PendingBinaryFile identification string
    * @throws Exception
    *           will be generated the Exception
    */
   private void saveChangesLog(ChangesFile fileDescriptor, String identifire) throws Exception
   {
      TransactionChangesLog transactionChangesLog =
         recoveryManager.getRecoveryReader().getChangesLog(fileDescriptor.getFile().getAbsolutePath());

      if (log.isDebugEnabled())
      {
         log.debug("Save to JCR : " + fileDescriptor.getFile().getAbsolutePath());
         log.debug("SystemID : " + transactionChangesLog.getSystemId());
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

      this.receive((ItemStateChangesLog)transactionChangesLog, identifire);
   }
}
