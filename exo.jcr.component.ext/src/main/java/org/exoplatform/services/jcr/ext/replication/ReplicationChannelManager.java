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
import org.exoplatform.services.jcr.ext.replication.transport.AbstractPacket;
import org.exoplatform.services.jcr.ext.replication.transport.ChannelManager;
import org.exoplatform.services.jcr.ext.replication.transport.MemberAddress;
import org.exoplatform.services.jcr.ext.replication.transport.PacketTransformer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.GroupRequest;
import org.jgroups.blocks.MessageDispatcher;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br/>Date: 10.06.2009
 * 
 * @author <a href="mailto:alex.reshetnyak@exoplatform.com.ua">Alex Reshetnyak</a>
 * @version $Id: ReplicationChannelManager.java 111 2008-11-11 11:11:11Z rainf0x $
 */
public class ReplicationChannelManager extends ChannelManager
{

   /**
    * log. the apache logger.
    */
   private static final Log LOG = ExoLogger.getLogger("exo.jcr.component.ext.ReplicationChannelManager");

   /**
    * testChannelName. The name to JChannel. Using only testing.
    */
   private String testChannelName;

   /**
    * ReplicationChannelManager ChannelManager constructor.
    * 
    * @param channelConfig
    *          channel configuration
    * @param channelName
    *          name of channel
    */
   public ReplicationChannelManager(String channelConfig, String channelName)
   {
      super(channelConfig, channelName, 0);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public Object handle(final Message message)
   {
      if (isConnected())
      {
         try
         {
            packetsHandler.add(PacketTransformer.getAsPacket(message.getBuffer()), new MemberAddress(message.getSrc()));

            if (channel != null || channel.getView() != null)
            {
               packetsHandler.handle();
            }
            else
               LOG.warn("No members found or channel closed, queue message " + message);

            return new String("Success");
         }
         catch (IOException e)
         {
            LOG.error("Message handler error " + e, e);
            return e.getMessage();
         }
         catch (ClassNotFoundException e)
         {
            LOG.error("Message handler error " + e, e);
            return e.getMessage();
         }
      }
      else
      {
         LOG.warn("Channel is closed but message received " + message);
         return new String("Disconnected");
      }
   }

   /**
    * send.
    * 
    * @param buffer
    *          the binary data
    */
   public synchronized void send(byte[] buffer)
   {
      Message msg = new Message(null, null, buffer);
      dispatcher.castMessage(null, msg, GroupRequest.GET_NONE, 0);
   }

   /**
    * sendBigPacket.
    * 
    * @param data
    *          the binary data
    * @param packet
    *          the Packet
    * @throws Exception
    *           will be generated Exception
    */
   public synchronized void sendBigPacket(byte[] data, Packet packet) throws Exception
   {

      long totalPacketCount = this.getPacketCount(data.length, Packet.MAX_PACKET_SIZE);
      int offset = 0;

      int len;

      while ((len = data.length - offset) > 0)
      {

         int l = (len > Packet.MAX_PACKET_SIZE) ? Packet.MAX_PACKET_SIZE : (int)len;
         byte[] buf = new byte[l];
         System.arraycopy(data, offset, buf, 0, l);

         Packet bigPacket =
            new Packet(Packet.PacketType.BIG_PACKET, packet.getIdentifier(), totalPacketCount, data.length, offset, buf);

         sendPacket(bigPacket);
         offset += l;
         if (LOG.isDebugEnabled())
            LOG.debug("Send of damp --> " + bigPacket.getByteArray().length);
      }
   }

   /**
    * sendBinaryFile.
    * 
    * @param filePath
    *          full path to file
    * @param ownerName
    *          owner name
    * @param identifier
    *          the identifier String
    * @param systemId
    *          system identifications ID
    * @param packetType
    *          the packet type for first packet
    * @throws Exception
    *           will be generated the Exception
    */
   public synchronized void sendBinaryFile(String filePath, String ownerName, String identifier, String systemId,
      int packetType) throws Exception
   {

      if (LOG.isDebugEnabled())
         LOG.debug("Begin send : " + filePath);

      File f = new File(filePath);
      long packetCount = getPacketCount(PrivilegedFileHelper.length(f), Packet.MAX_PACKET_SIZE);

      FileInputStream in = PrivilegedFileHelper.fileInputStream(f);
      byte[] buf = new byte[Packet.MAX_PACKET_SIZE];
      int len;
      long offset = 0;

      // Send first packet in all cases. If InputStream is empty too.
      len = in.read(buf);
      if (len < Packet.MAX_PACKET_SIZE)
      {
         // cut buffer to original size;
         byte[] b = new byte[len];
         System.arraycopy(buf, 0, b, 0, len);
         buf = b;
      }

      Packet packet = new Packet(packetType, systemId, identifier, ownerName, f.getName(), packetCount, offset, buf);

      sendPacket(packet);
      offset += len;
      if (LOG.isDebugEnabled())
         LOG.debug("Send packet type [" + packetType + "] --> " + offset);

      while ((len = in.read(buf)) > 0)
      {
         if (len < AbstractPacket.MAX_PACKET_SIZE)
         {
            byte[] b = new byte[len];
            // cut buffer to original size;
            System.arraycopy(buf, 0, b, 0, len);
            buf = b;
         }
         packet = new Packet(packetType, systemId, identifier, ownerName, f.getName(), packetCount, offset, buf);

         sendPacket(packet);
         offset += len;

         if (LOG.isDebugEnabled())
            LOG.debug("Send packet type [" + packetType + "] --> " + offset);
      }

      in.close();
   }

   /**
    * getPacketCount.
    *
    * @param contentLength
    *          long, the content length
    * @param packetSize
    *          long, the packet size
    * @return long
    *           the total packets for this content 
    */
   private long getPacketCount(long contentLength, long packetSize)
   {
      long count = contentLength / packetSize;
      count += ((count * packetSize - contentLength) != 0) ? 1 : 0;
      return count;
   }

   /**
    * setAllowConnect.
    * 
    * @param allowConnect
    *          allow connection state(true or false)
    */
   public void setAllowConnect(boolean allowConnect)
   {
      if (!allowConnect)
         testChannelName = channelName + Math.round(Math.random() * Byte.MAX_VALUE);
      else
         testChannelName = null;
   }

   /**
    * setAllowConnect.
    * 
    * @param allowConnect
    *          allow connection state(true or false)
    * @param id
    *          channel id
    */
   public void setAllowConnect(boolean allowConnect, int id)
   {
      if (!allowConnect)
         testChannelName = channelName + id;
      else
         testChannelName = null;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void connect() throws ReplicationException
   {

      try
      {
         if (channel == null)
         {
            channel = new JChannel(channelConfig);

            channel.setOpt(Channel.AUTO_RECONNECT, Boolean.TRUE);
            channel.setOpt(Channel.AUTO_GETSTATE, Boolean.TRUE);

            dispatcher = new MessageDispatcher(channel, null, null, null);

            dispatcher.setRequestHandler(this);
            dispatcher.setMembershipListener(this);
         }
      }
      catch (ChannelException e)
      {
         throw new ReplicationException("Can't create JGroups channel", e);
      }

      LOG.info("Channel name : " + channelName);

      try
      {
         if (testChannelName == null)
            channel.connect(channelName);
         else
            channel.connect(testChannelName);

         this.state = CONNECTED;
      }
      catch (ChannelException e)
      {
         throw new ReplicationException("Can't connect to JGroups channel", e);
      }
   }
}
