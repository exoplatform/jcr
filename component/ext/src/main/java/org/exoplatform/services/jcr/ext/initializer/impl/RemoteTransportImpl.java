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
package org.exoplatform.services.jcr.ext.initializer.impl;

import org.exoplatform.services.jcr.ext.initializer.NoMemberToSendException;
import org.exoplatform.services.jcr.ext.initializer.RemoteTransport;
import org.exoplatform.services.jcr.ext.initializer.RemoteWorkspaceInitializationException;
import org.exoplatform.services.jcr.ext.replication.ReplicationException;
import org.exoplatform.services.jcr.ext.replication.storage.InvalidChecksumException;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>
 * Date: 17.03.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: RemoteTransportImpl.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class RemoteTransportImpl implements RemoteTransport
{

   /**
    * BUFFER_SIZE. The constant for buffer size.
    */
   private static final int BUFFER_SIZE = 20 * 1024;

   /**
    * The channel manager.
    */
   private final ChannelManager channelManager;

   /**
    * The data transmitter.
    */
   private final RemoteTransmitter remoteTransmitter;

   /**
    * The data receiver.
    */
   private final RemoteReceiver remoteReceiver;

   /**
    * The temporary folder.
    */
   private final File tempDir;

   /**
    * Data source url.
    */
   private final String sourceUrl;

   /**
    * The synchronization latch.
    */
   private CountDownLatch latch;

   /**
    * RemoteTransportImpl constructor.
    * 
    */
   /**
    * RemoteTransportImpl constructor.
    * 
    * @param channelManager
    *          the channel manager.
    * @param tempDir
    *          the temporary folder.
    * @param sourceUrl
    *          data source url.
    */
   public RemoteTransportImpl(ChannelManager channelManager, File tempDir, String sourceUrl)
   {
      this.latch = new CountDownLatch(1);
      this.channelManager = channelManager;
      this.tempDir = tempDir;
      this.sourceUrl = sourceUrl;
      this.remoteTransmitter = new RemoteTransmitter(this.channelManager);
      this.remoteReceiver = new RemoteReceiver(this.tempDir, latch);
      this.channelManager.addPacketListener(remoteReceiver);
      this.channelManager.addStateListener(remoteReceiver);
   }

   /**
    * {@inheritDoc}
    */
   public void close() throws RemoteWorkspaceInitializationException
   {
      channelManager.disconnect();
   }

   /**
    * {@inheritDoc}
    */
   public File getWorkspaceData(String repositoryName, String workspaceName, String id)
      throws RemoteWorkspaceInitializationException
   {
      RemoteHttpClient client = new RemoteHttpClient(sourceUrl);
      String result = client.execute(repositoryName, workspaceName, id);

      if (result.startsWith("FAIL"))
         throw new RemoteWorkspaceInitializationException("Can not getting the remote workspace data : " + result);

      try
      {
         latch.await();
      }
      catch (InterruptedException e)
      {
         throw new RemoteWorkspaceInitializationException("The interapted : " + e.getMessage(), e);
      }

      if (remoteReceiver.getException() != null)
         throw new RemoteWorkspaceInitializationException("Can not getting the remote workspace data : "
            + remoteReceiver.getException().getMessage(), remoteReceiver.getException());

      try
      {
         remoteReceiver.getContext().getChangesFile().validate();
      }
      catch (InvalidChecksumException e)
      {
         new RemoteWorkspaceInitializationException("Can not getting the remote workspace data : " + e.getMessage(), e);
      }

      String filePath = remoteReceiver.getContext().getChangesFile().toString();

      return new File(filePath);
   }

   /**
    * {@inheritDoc}
    */
   public void init() throws RemoteWorkspaceInitializationException
   {
      try
      {
         channelManager.connect();
      }
      catch (ReplicationException e)
      {
         throw new RemoteWorkspaceInitializationException("Can not  initialize the transport : " + e.getMessage(), e);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void sendWorkspaceData(File workspaceData) throws RemoteWorkspaceInitializationException,
      NoMemberToSendException
   {

      if (!workspaceData.exists())
         throw new RemoteWorkspaceInitializationException("The file with workspace data not exists.");

      byte[] crc;
      try
      {
         crc = getCheckSum(workspaceData);

         try
         {
            if (channelManager.getOtherMembers().size() == 0)
               throw new NoMemberToSendException("No member to sent, member list : "
                  + channelManager.getOtherMembers().size());

            remoteTransmitter.sendChangesLogFile(channelManager.getOtherMembers().get(0), workspaceData, crc);
         }
         catch (IOException e)
         {
            throw new RemoteWorkspaceInitializationException(
               "Can not send the workspace data file : " + e.getMessage(), e);
         }
      }
      catch (NoSuchAlgorithmException e)
      {
         throw new RemoteWorkspaceInitializationException("Can not calculate the checksum for workspace data file : "
            + e.getMessage(), e);
      }
      catch (IOException e)
      {
         throw new RemoteWorkspaceInitializationException("Can not calculate the checksum for workspace data file : "
            + e.getMessage(), e);
      }

   }

   /**
    * {@inheritDoc}
    */
   public void sendError(String message) throws RemoteWorkspaceInitializationException, NoMemberToSendException
   {
      try
      {
         InitializationErrorPacket packet =
            new InitializationErrorPacket(InitializationErrorPacket.INITIALIZATION_ERROR_PACKET, message);
         channelManager.sendPacket(packet, channelManager.getOtherMembers().get(0));
      }
      catch (IOException e)
      {
         throw new RemoteWorkspaceInitializationException("Cannot send export data " + e.getMessage(), e);
      }
   }

   /**
    * getCheckSum.
    * 
    * @param f
    *          the File
    * @return byte[] the checksum
    * @throws NoSuchAlgorithmException
    *           will be generated the NoSuchAlgorithmException
    * @throws IOException
    *           will be generated the IOException
    */
   private byte[] getCheckSum(File f) throws NoSuchAlgorithmException, IOException
   {
      MessageDigest digest = MessageDigest.getInstance("MD5");
      InputStream in = new FileInputStream(f);

      long length = f.length();

      byte[] buff = new byte[BUFFER_SIZE];
      for (; length >= BUFFER_SIZE; length -= BUFFER_SIZE)
      {
         in.read(buff);
         digest.update(buff);
      }

      if (length > 0)
      {
         buff = new byte[(int)length];
         in.read(buff);
         digest.update(buff);
      }

      return digest.digest();
   }

}
